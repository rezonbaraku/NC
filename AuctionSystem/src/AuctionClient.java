import java.io.*;
import java.net.*;
import java.util.*;

public class AuctionClient {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 8888;
    
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private BufferedReader consoleIn;
    private boolean connected;
    private String username;
    
    public AuctionClient() {
        consoleIn = new BufferedReader(new InputStreamReader(System.in));
        connected = false;
    }
    
    public void connect() {
        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            
            // Start a separate thread to handle server messages
            new Thread(this::receiveMessages).start();
            
            System.out.println("Connected to Auction Server");
            showMainMenu();
            
            String input;
            while ((input = consoleIn.readLine()) != null) {
                processUserInput(input);
            }
        } catch (IOException e) {
            System.err.println("Error connecting to server: " + e.getMessage());
        } finally {
            disconnect();
        }
    }
    
    private void receiveMessages() {
        try {
            String message;
            while (socket.isConnected() && (message = in.readLine()) != null) {
                processServerMessage(message);
            }
        } catch (IOException e) {
            if (connected) {
                System.err.println("Connection to server lost: " + e.getMessage());
                connected = false;
            }
        }
    }
    
    private void processServerMessage(String message) {
        String[] tokens = message.split(" ", 2);
        String command = tokens[0];
        String content = tokens.length > 1 ? tokens[1] : "";
        
        switch (command) {
            case "SUCCESS":
                System.out.println("Success: " + content);
                break;
            case "ERROR":
                System.out.println("Error: " + content);
                break;
            case "WELCOME":
                connected = true;
                System.out.println(content);
                showConnectedMenu();
                break;
            case "AUCTIONS":
                displayAuctions(content);
                break;
            case "BID_UPDATE":
                System.out.println("New bid: " + content);
                break;
            case "BID_STATUS":
                System.out.println("Highest bid: " + content);
                break;
            case "GOING_ONCE":
                System.out.println("GOING ONCE: " + content);
                break;
            case "GOING_TWICE":
                System.out.println("GOING TWICE: " + content);
                break;
            case "SOLD":
                System.out.println("SOLD: " + content);
                break;
            case "GOODBYE":
                connected = false;
                System.out.println(content);
                showMainMenu();
                break;
            default:
                System.out.println("Server: " + message);
        }
    }
    
    private void displayAuctions(String auctionsData) {
        if (auctionsData.equals("No active auctions")) {
            System.out.println("No active auctions available");
            return;
        }
        
        System.out.println("\n===== ACTIVE AUCTIONS =====");
        System.out.printf("%-5s | %-20s | %-30s | %-10s | %-10s | %-15s | %-15s\n", 
                          "ID", "Item", "Description", "Start Price", "Current Price", "Seller", "Seller IP");
        System.out.println("--------------------------------------------------------------------------------------------------------------------------------");
        
        String[] auctions = auctionsData.split(";");
        for (String auction : auctions) {
            String[] fields = auction.split("\\|");
            if (fields.length >= 7) {
                System.out.printf("%-5s | %-20s | %-30s | %-10s | %-10s | %-15s | %-15s\n", 
                                 fields[0], fields[1], fields[2], fields[3], fields[4], fields[5], fields[6]);
            }
        }
        System.out.println("=========================\n");
    }
    
    private void processUserInput(String input) {
        try {
            int choice = Integer.parseInt(input);
            
            if (connected) {
                processConnectedMenuChoice(choice);
            } else {
                processMainMenuChoice(choice);
            }
        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid number");
        } catch (IOException e) {
            System.err.println("Error processing input: " + e.getMessage());
        }
    }
    
    private void processMainMenuChoice(int choice) throws IOException {
        switch (choice) {
            case 1:
                register();
                break;
            case 2:
                login();
                break;
            case 3:
                listAuctions();
                break;
            case 4:
                System.out.println("You must be logged in to perform this action");
                break;
            case 5:
                System.out.println("You must be logged in to perform this action");
                break;
            case 6:
                System.out.println("You must be logged in to perform this action");
                break;
            case 7:
                System.out.println("You must be logged in to perform this action");
                break;
            case 8:
                System.out.println("You must be logged in to perform this action");
                break;
            case 9:
                disconnect();
                System.exit(0);
                break;
            default:
                System.out.println("Invalid choice. Please try again");
                showMainMenu();
        }
    }
    
    private void processConnectedMenuChoice(int choice) throws IOException {
        switch (choice) {
            case 1:
                advertiseItem();
                break;
            case 2:
                listAuctions();
                break;
            case 3:
                joinAuction();
                break;
            case 4:
                placeBid();
                break;
            case 5:
                checkHighestBid();
                break;
            case 6:
                withdrawFromAuction();
                break;
            case 7:
                out.println("DISCONNECT");
                break;
            default:
                System.out.println("Invalid choice. Please try again");
                showConnectedMenu();
        }
    }
    
    private void register() throws IOException {
        System.out.println("\n===== REGISTRATION =====");
        System.out.print("Username: ");
        String username = consoleIn.readLine();
        
        System.out.print("Password: ");
        String password = consoleIn.readLine();
        
        System.out.print("Name: ");
        String name = consoleIn.readLine();
        
        System.out.print("Surname: ");
        String surname = consoleIn.readLine();
        
        System.out.print("ID Number: ");
        String idNumber = consoleIn.readLine();
        
        System.out.print("Phone: ");
        String phone = consoleIn.readLine();
        
        System.out.print("Email: ");
        String email = consoleIn.readLine();
        
        out.println("REGISTER " + username + " " + password + " " + name + " " + surname + " " + idNumber + " " + phone + " " + email);
        this.username = username;
    }
    
    private void login() throws IOException {
        System.out.println("\n===== LOGIN =====");
        System.out.print("Username: ");
        String username = consoleIn.readLine();
        
        System.out.print("Password: ");
        String password = consoleIn.readLine();
        
        out.println("CONNECT " + username + " " + password);
        this.username = username;
    }
    
    private void listAuctions() {
        out.println("LIST_AUCTIONS");
    }
    
    private void advertiseItem() throws IOException {
        System.out.println("\n===== ADVERTISE ITEM =====");
        System.out.print("Item Name: ");
        String itemName = consoleIn.readLine();
        
        System.out.print("Item Description: ");
        String itemDescription = consoleIn.readLine();
        
        System.out.print("Starting Price: ");
        String startingPriceStr = consoleIn.readLine();
        
        System.out.println("Auction Type (1 for timed, 2 for ongoing until no more bids): ");
        String auctionType = consoleIn.readLine();
        
        if (auctionType.equals("1")) {
            System.out.print("Duration (minutes): ");
            String duration = consoleIn.readLine();
            
            
            out.println("ADVERTISE " + itemName + "|" + itemDescription + "|" + startingPriceStr + "|" + auctionType + "|" + duration);
        } else if (auctionType.equals("2")) {
            out.println("ADVERTISE " + itemName + "|" + itemDescription + "|" + startingPriceStr + "|" + auctionType);
        } else {
            System.out.println("Invalid auction type. Please enter 1 or 2.");
        }
    }
    
    private void joinAuction() throws IOException {
        System.out.println("\n===== JOIN AUCTION =====");
        System.out.print("Auction ID: ");
        String auctionId = consoleIn.readLine();
        
        out.println("JOIN_AUCTION " + auctionId);
    }
    
    private void placeBid() throws IOException {
        System.out.println("\n===== PLACE BID =====");
        System.out.print("Auction ID: ");
        String auctionId = consoleIn.readLine();
        
        System.out.print("Bid Amount: ");
        String bidAmount = consoleIn.readLine();
        
        out.println("BID " + auctionId + " " + bidAmount);
    }
    
    private void checkHighestBid() throws IOException {
        System.out.println("\n===== CHECK HIGHEST BID =====");
        System.out.print("Auction ID: ");
        String auctionId = consoleIn.readLine();
        
        out.println("CHECK_BID " + auctionId);
    }
    
    private void withdrawFromAuction() throws IOException {
        System.out.println("\n===== WITHDRAW FROM AUCTION =====");
        System.out.print("Auction ID: ");
        String auctionId = consoleIn.readLine();
        
        out.println("WITHDRAW " + auctionId);
    }
    
    private void showMainMenu() {
        System.out.println("\n===== AUCTION SYSTEM MENU =====");
        System.out.println("1. Register");
        System.out.println("2. Login");
        System.out.println("3. List Active Auctions");
        System.out.println("4. Place Item for Auction");
        System.out.println("5. Join Auction");
        System.out.println("6. Place Bid");
        System.out.println("7. Check Highest Bid");
        System.out.println("8. Withdraw from Auction");
        System.out.println("9. Exit");
        System.out.print("Enter your choice: ");
    }
    
    private void showConnectedMenu() {
        System.out.println("\n===== AUCTION SYSTEM MENU (Logged in as " + username + ") =====");
        System.out.println("1. Place Item for Auction");
        System.out.println("2. List Active Auctions");
        System.out.println("3. Join Auction");
        System.out.println("4. Place Bid");
        System.out.println("5. Check Highest Bid");
        System.out.println("6. Withdraw from Auction");
        System.out.println("7. Disconnect");
        System.out.print("Enter your choice: ");
    }
    
    private void disconnect() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
            System.out.println("Disconnected from server");
        } catch (IOException e) {
            System.err.println("Error disconnecting: " + e.getMessage());
        }
    }
    
    public static void main(String[] args) {
        AuctionClient client = new AuctionClient();
        client.connect();
    }
}