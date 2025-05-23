import java.io.*;
import java.net.*;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.*;

public class AuctionServer {
    private static final int PORT = 8888;
    private ServerSocket serverSocket;
    private boolean running;
    private DatabaseManager dbManager;
    private Map<String, ClientHandler> connectedClients;
    private ExecutorService threadPool;
    private ScheduledExecutorService timerService;
    
    // Map to keep track of auction timers
    private Map<Integer, ScheduledFuture<?>> auctionTimers;
    private Map<Integer, ScheduledFuture<?>> goingOnceTimers;
    private Map<Integer, ScheduledFuture<?>> goingTwiceTimers;
    
    public AuctionServer() {
        dbManager = new DatabaseManager();
        connectedClients = new ConcurrentHashMap<>();
        threadPool = Executors.newCachedThreadPool();
        timerService = Executors.newScheduledThreadPool(10);
        auctionTimers = new ConcurrentHashMap<>();
        goingOnceTimers = new ConcurrentHashMap<>();
        goingTwiceTimers = new ConcurrentHashMap<>();
        running = true;
    }
    
    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("Auction Server started on port " + PORT);
            
            while (running) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress().getHostAddress());
                ClientHandler handler = new ClientHandler(clientSocket);
                threadPool.execute(handler);
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        } finally {
            stop();
        }
    }
    
    public void stop() {
        running = false;
        threadPool.shutdown();
        timerService.shutdown();
        dbManager.close();
        
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing server socket: " + e.getMessage());
        }
        
        System.out.println("Server stopped");
    }
    
    // Notify all participants in an auction
    private void notifyAuctionParticipants(int auctionId, String message) {
        List<String> participants = dbManager.getAuctionParticipants(auctionId);
        Auction auction = dbManager.getAuction(auctionId);
        
        if (auction != null) {
            // Also notify the seller
            participants.add(auction.getSellerUsername());
            
            for (String username : participants) {
                ClientHandler handler = connectedClients.get(username);
                if (handler != null) {
                    handler.sendMessage(message);
                }
            }
        }
    }
    
    
    
 // Method to schedule auction close for timed auctions
    private void scheduleAuctionClose(int auctionId, long durationMillis) {
        ScheduledFuture<?> future = timerService.schedule(() -> {
            Auction auction = dbManager.getAuction(auctionId);
            if (auction != null && "active".equals(auction.getStatus())) {
                Bid highestBid = dbManager.getHighestBid(auctionId);
                String highestBidder = highestBid != null ? highestBid.getUsername() : null;
                double finalPrice = highestBid != null ? highestBid.getAmount() : auction.getStartingPrice();
                
                // Close the auction
                dbManager.closeAuction(auctionId, finalPrice, highestBidder);
                
                // Notify participants
                String message = "SOLD " + auctionId + " " + auction.getItemName() + " " + finalPrice + " " + 
                                 (highestBidder != null ? highestBidder : "No bidders");
                notifyAuctionParticipants(auctionId, message);
                
                // Remove auction timer
                auctionTimers.remove(auctionId);
            }
        }, durationMillis, TimeUnit.MILLISECONDS);
        
        auctionTimers.put(auctionId, future);
    }
    
    // Method to handle ongoing auction bidding process
    private void handleBid(int auctionId, String bidder, double amount) {
        Auction auction = dbManager.getAuction(auctionId);
        
        if (auction == null || !"active".equals(auction.getStatus())) {
            return;
        }
        
        // For auction type 2 (ongoing until no more bids)
        if (auction.getAuctionType() == 2) {
            // Cancel any existing timers for this auction
            cancelAuctionTimers(auctionId);
            
            // Schedule "going once" timer (30 seconds)
            goingOnceTimers.put(auctionId, timerService.schedule(() -> {
                String message = "GOING_ONCE " + auctionId + " " + auction.getItemName() + " " + amount;
                notifyAuctionParticipants(auctionId, message);
                
                // Schedule "going twice" timer (5 seconds after "going once")
                goingTwiceTimers.put(auctionId, timerService.schedule(() -> {
                    String goingTwiceMessage = "GOING_TWICE " + auctionId + " " + auction.getItemName() + " " + amount;
                    notifyAuctionParticipants(auctionId, goingTwiceMessage);
                    
                    // Schedule "sold" message (5 seconds after "going twice")
                    auctionTimers.put(auctionId, timerService.schedule(() -> {
                        // Close the auction
                        dbManager.closeAuction(auctionId, amount, bidder);
                        
                        ClientHandler bidderHandler = connectedClients.get(bidder);
                        String bidderIp = bidderHandler != null ? bidderHandler.getIpAddress() : "unknown";
                        
                        String soldMessage = "SOLD " + auctionId + " " + auction.getItemName() + " " + amount + " " + bidder + " " + bidderIp;
                        notifyAuctionParticipants(auctionId, soldMessage);
                        
                        // Remove all timers for this auction
                        cancelAuctionTimers(auctionId);
                    }, 5, TimeUnit.SECONDS));
                }, 5, TimeUnit.SECONDS));
            }, 30, TimeUnit.SECONDS));
        }
        
        // For auction type 1, there's already a fixed timer set when the auction was created
    }
    
    private void cancelAuctionTimers(int auctionId) {
        // Cancel "going once" timer
        ScheduledFuture<?> goingOnceTimer = goingOnceTimers.remove(auctionId);
        if (goingOnceTimer != null) {
            goingOnceTimer.cancel(false);
        }
        
        // Cancel "going twice" timer
        ScheduledFuture<?> goingTwiceTimer = goingTwiceTimers.remove(auctionId);
        if (goingTwiceTimer != null) {
            goingTwiceTimer.cancel(false);
        }
        
        // Cancel auction close timer
        ScheduledFuture<?> auctionTimer = auctionTimers.remove(auctionId);
        if (auctionTimer != null) {
            auctionTimer.cancel(false);
        }
    }
    
    // Inner class to handle client connections
    private class ClientHandler implements Runnable {
        private Socket clientSocket;
        private BufferedReader in;
        private PrintWriter out;
        private String username;
        private String ipAddress;
        
        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
            this.ipAddress = socket.getInetAddress().getHostAddress();
            
            try {
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintWriter(clientSocket.getOutputStream(), true);
            } catch (IOException e) {
                System.err.println("Error creating client handler: " + e.getMessage());
            }
        }
        
        public String getIpAddress() {
            return ipAddress;
        }
        
        public void sendMessage(String message) {
            out.println(message);
        }
        
        @Override
        public void run() {
            try {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    processCommand(inputLine);
                }
            } catch (IOException e) {
                System.err.println("Error handling client: " + e.getMessage());
            } finally {
                // Handle client disconnection
                if (username != null) {
                    connectedClients.remove(username);
                    System.out.println("Client disconnected: " + username);
                }
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    System.err.println("Error closing client socket: " + e.getMessage());
                }
            }
        }
        
        private void processCommand(String input) {
            String[] tokens = input.split(" ", 2);
            String command = tokens[0];
            
            try {
                switch (command) {
                    case "REGISTER":
                        handleRegister(tokens[1]);
                        break;
                    case "CONNECT":
                        handleConnect(tokens[1]);
                        break;
                    case "ADVERTISE":
                        handleAdvertise(tokens[1]);
                        break;
                    case "LIST_AUCTIONS":
                        handleListAuctions();
                        break;
                    case "JOIN_AUCTION":
                        handleJoinAuction(tokens[1]);
                        break;
                    case "BID":
                        handleBidCommand(tokens[1]);
                        break;
                    case "CHECK_BID":
                        handleCheckBid(tokens[1]);
                        break;
                    case "WITHDRAW":
                        handleWithdraw(tokens[1]);
                        break;
                    case "DISCONNECT":
                        handleDisconnect();
                        break;
                    default:
                        sendMessage("ERROR Unknown command: " + command);
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                sendMessage("ERROR Invalid command format");
            } catch (Exception e) {
                sendMessage("ERROR " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        private void handleRegister(String args) {
            String[] tokens = args.split(" ", 7);
            if (tokens.length < 7) {
                sendMessage("ERROR Invalid registration format");
                return;
            }
            
            String username = tokens[0];
            String password = tokens[1];
            String name = tokens[2];
            String surname = tokens[3];
            String idNumber = tokens[4];
            String phone = tokens[5];
            String email = tokens[6];
            
            boolean success = dbManager.registerUser(username, password, name, surname, idNumber, phone, email, ipAddress);
            
            if (success) {
                sendMessage("SUCCESS Registration successful");
            } else {
                sendMessage("ERROR Username already exists");
            }
        }
        
        private void handleConnect(String args) {
            String[] tokens = args.split(" ", 2);
            if (tokens.length < 2) {
                sendMessage("ERROR Invalid login format");
                return;
            }
            
            String username = tokens[0];
            String password = tokens[1];
            
            User user = dbManager.getUser(username);
            
            if (user == null) {
                sendMessage("ERROR User not found");
                return;
            }
            
            if (!user.getPassword().equals(password)) {
                sendMessage("ERROR Invalid password");
                return;
            }
            
            if (!user.getIpAddress().equals(ipAddress)) {
                sendMessage("ERROR IP address does not match registration IP");
                return;
            }
            
            this.username = username;
            connectedClients.put(username, this);
            sendMessage("WELCOME Welcome to the Auction System, " + user.getName() + "!");
        }
        
        private void handleAdvertise(String args) {
            if (username == null) {
                sendMessage("ERROR You must be connected to advertise an item");
                return;
            }
            
            // Split using the pipe delimiter
            String[] parts = args.split("\\|");
            
            if (parts.length < 4) {
                sendMessage("ERROR Invalid advertise format. Need at least 4 parameters.");
                return;
            }
            
            String itemName = parts[0].trim(); // Trim to remove any extra spaces
            String itemDescription = parts[1].trim();
            
            double startingPrice;
            try {
                startingPrice = Double.parseDouble(parts[2].trim());
                if (startingPrice <= 0) {
                    sendMessage("ERROR Starting price must be greater than zero");
                    return;
                }
            } catch (NumberFormatException e) {
                sendMessage("ERROR Invalid starting price: " + parts[2] + ". Must be a valid number.");
                return;
            }
            
            String auctionTypeStr = parts[3].trim();
            int auctionType;
            Timestamp endTime = null;
            
            try {
                auctionType = Integer.parseInt(auctionTypeStr);
                
                if (auctionType == 1) {
                    // For timed auction, we need duration
                    if (parts.length < 5) {
                        sendMessage("ERROR Duration required for timed auction");
                        return;
                    }
                    
                    try {
                        long durationMinutes = Long.parseLong(parts[4].trim());
                        
                        // ADD THIS VALIDATION - Check if duration is positive
                        if (durationMinutes <= 0) {
                            sendMessage("ERROR Duration must be greater than zero minutes");
                            return;
                        }
                        
                        // ADD THIS VALIDATION - Check if duration is reasonable (not too long)
                        if (durationMinutes > 10080) { // More than 1 week (7 * 24 * 60 minutes)
                            sendMessage("ERROR Duration cannot exceed 1 week (10080 minutes)");
                            return;
                        }
                        
                        // ADD THIS VALIDATION - Check if duration is not too short
                        if (durationMinutes < 1) {
                            sendMessage("ERROR Duration must be at least 1 minute");
                            return;
                        }
                        
                        // Calculate end time
                        Calendar calendar = Calendar.getInstance();
                        calendar.add(Calendar.MINUTE, (int) durationMinutes);
                        endTime = new Timestamp(calendar.getTimeInMillis());
                        
                        // Create the auction and send response
                        int auctionId = dbManager.createAuction(itemName, itemDescription, startingPrice, username, auctionType, endTime);
                        
                        if (auctionId > 0) {
                            // Schedule auction close
                            long durationMillis = durationMinutes * 60 * 1000;
                            scheduleAuctionClose(auctionId, durationMillis);
                            
                            sendMessage("SUCCESS Auction created with ID: " + auctionId + " (Duration: " + durationMinutes + " minutes)");
                        } else {
                            sendMessage("ERROR Failed to create auction");
                        }
                        
                    } catch (NumberFormatException e) {
                        sendMessage("ERROR Invalid duration: " + parts[4] + ". Must be a valid number.");
                        return;
                    }
                } else if (auctionType == 2) {
                    // Ongoing auction, no end time needed
                    int auctionId = dbManager.createAuction(itemName, itemDescription, startingPrice, username, auctionType, null);
                    
                    if (auctionId > 0) {
                        sendMessage("SUCCESS Auction created with ID: " + auctionId + " (Ongoing until no more bids)");
                    } else {
                        sendMessage("ERROR Failed to create auction");
                    }
                } else {
                    sendMessage("ERROR Invalid auction type (1 for timed, 2 for ongoing)");
                    return;
                }
            } catch (NumberFormatException e) {
                sendMessage("ERROR Invalid auction type: " + auctionTypeStr + ". Must be 1 or 2.");
                return;
            }
        }
        private void handleListAuctions() {
            List<Auction> auctions = dbManager.getActiveAuctions();
            
            if (auctions.isEmpty()) {
                sendMessage("AUCTIONS No active auctions");
                return;
            }
            
            StringBuilder response = new StringBuilder("AUCTIONS ");
            for (Auction auction : auctions) {
                User seller = dbManager.getUser(auction.getSellerUsername());
                String sellerIp = seller != null ? seller.getIpAddress() : "unknown";
                
                Bid highestBid = dbManager.getHighestBid(auction.getAuctionId());
                double highestBidAmount = highestBid != null ? highestBid.getAmount() : auction.getStartingPrice();
                
                response.append(auction.getAuctionId()).append("|")
                        .append(auction.getItemName()).append("|")
                        .append(auction.getItemDescription()).append("|")
                        .append(auction.getStartingPrice()).append("|")
                        .append(highestBidAmount).append("|")
                        .append(auction.getSellerUsername()).append("|")
                        .append(sellerIp).append(";");
            }
            
            sendMessage(response.toString());
        }
        
        private void handleJoinAuction(String args) {
            if (username == null) {
                sendMessage("ERROR You must be connected to join an auction");
                return;
            }
            
            int auctionId;
            try {
                auctionId = Integer.parseInt(args);
            } catch (NumberFormatException e) {
                sendMessage("ERROR Invalid auction ID format");
                return;
            }
            
            // Check if auction exists first
            Auction auction = dbManager.getAuction(auctionId);
            if (auction == null) {
                sendMessage("ERROR Auction with ID " + auctionId + " does not exist");
                return;
            }
            
            if (!"active".equals(auction.getStatus())) {
                sendMessage("ERROR Auction " + auctionId + " is no longer active");
                return;
            }
            
            boolean success = dbManager.joinAuction(auctionId, username);
            
            if (success) {
                sendMessage("SUCCESS You have joined auction #" + auctionId + " (" + auction.getItemName() + ")");
            } else {
                sendMessage("ERROR Failed to join auction (you may already be registered)");
            }
        }
        
        private void handleBidCommand(String args) {
            if (username == null) {
                sendMessage("ERROR! You must be connected to place a bid");
                return;
            }
            
            String[] tokens = args.split(" ", 2);
            if (tokens.length < 2) {
                sendMessage("ERROR!  Invalid bid format");
                return;
            }
            
            int auctionId;
            double amount;
            
            try {
                auctionId = Integer.parseInt(tokens[0]);
                amount = Double.parseDouble(tokens[1]);
            } catch (NumberFormatException e) {
                sendMessage("ERROR!  Invalid bid amount or the auction ID");
                return;
            }
            
            boolean success = dbManager.placeBid(auctionId, username, amount);
            
            if (success) {
                sendMessage("SUCCESS!  Your bid is placed successfully!");
                
                // Notify all participants about the new bid
                Auction auction = dbManager.getAuction(auctionId);
                if (auction != null) {
                    String message = "BID_UPDATE " + auctionId + " " + auction.getItemName() + " " + amount + " " + username + " " + ipAddress;
                    notifyAuctionParticipants(auctionId, message);
                    
                    // Handle the bid for auction type 2 (reset timers)
                    handleBid(auctionId, username, amount);
                }
            } else {
                sendMessage("ERROR! Failed to place bid (you may not be registered for this auction or bid amount is too low)");
            }
        }
        
        private void handleCheckBid(String args) {
            int auctionId;
            try {
                auctionId = Integer.parseInt(args);
            } catch (NumberFormatException e) {
                sendMessage("ERROR Invalid auction ID");
                return;
            }
            
            Bid highestBid = dbManager.getHighestBid(auctionId);
            
            if (highestBid != null) {
                sendMessage("BID_STATUS " + auctionId + " " + highestBid.getAmount() + " " + highestBid.getBidTime());
            } else {
                Auction auction = dbManager.getAuction(auctionId);
                if (auction != null) {
                    sendMessage("BID_STATUS " + auctionId + " " + auction.getStartingPrice() + " (starting price, no bids yet)");
                } else {
                    sendMessage("ERROR Auction not found");
                }
            }
        }
        
        private void handleWithdraw(String args) {
            if (username == null) {
                sendMessage("ERROR You must be connected to withdraw from an auction");
                return;
            }
            
            int auctionId;
            try {
                auctionId = Integer.parseInt(args);
            } catch (NumberFormatException e) {
                sendMessage("ERROR Invalid auction ID");
                return;
            }
            
            boolean success = dbManager.withdrawFromAuction(auctionId, username);
            
            if (success) {
                sendMessage("SUCCESS You have withdrawn from auction #" + auctionId);
            } else {
                sendMessage("ERROR Failed to withdraw (you may be the highest bidder)");
            }
        }
        
        private void handleDisconnect() {
            if (username == null) {
                sendMessage("ERROR You are not connected");
                return;
            }
            
            // Check if user is highest bidder in any active auction
            if (dbManager.isUserHighestBidder(username)) {
                sendMessage("ERROR Cannot disconnect while you are the highest bidder in an active auction");
                return;
            }
            
  
            dbManager.removeUserFromAllAuctions(username);
            
            // Disconnect the user
            connectedClients.remove(username);
            sendMessage("GOODBYE Goodbye!");
            username = null;
            
            // Close the connection
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing client socket: " + e.getMessage());
            }
        }
       
    }
    
    public static void main(String[] args) {
        AuctionServer server = new AuctionServer();
        server.start();
    }
}


