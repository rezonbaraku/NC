import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private Connection conn;
    
    // Prepared statements for database operations
    private PreparedStatement getUserStmt;
    private PreparedStatement registerUserStmt;
    private PreparedStatement createAuctionStmt;
    private PreparedStatement getActiveAuctionsStmt;
    private PreparedStatement joinAuctionStmt;
    private PreparedStatement checkUserInAuctionStmt;
    private PreparedStatement placeBidStmt;
    private PreparedStatement getHighestBidStmt;
    private PreparedStatement withdrawFromAuctionStmt;
    private PreparedStatement checkUserHighestBidderStmt;
    private PreparedStatement getUserAuctionsStmt;
    private PreparedStatement getUserRegisteredAuctionsStmt;
    private PreparedStatement getAuctionByIdStmt;
    private PreparedStatement getAuctionParticipantsStmt;
    private PreparedStatement closeAuctionStmt;
    
    public DatabaseManager() {
        try {
            // Connect to SQLite database
            conn = DriverManager.getConnection("jdbc:sqlite:auction_system.db");
            
            // Create tables if they don't exist
            Statement stmt = conn.createStatement();
            
            // Users table
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS users (" +
                "username TEXT PRIMARY KEY, " +
                "password TEXT NOT NULL, " +
                "name TEXT NOT NULL, " +
                "surname TEXT NOT NULL, " +
                "id_number TEXT NOT NULL, " +
                "phone TEXT NOT NULL, " +
                "email TEXT NOT NULL, " +
                "ip_address TEXT NOT NULL)"
            );
            
            // Auctions table
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS auctions (" +
                "auction_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "item_name TEXT NOT NULL, " +
                "item_description TEXT NOT NULL, " +
                "starting_price REAL NOT NULL, " +
                "current_price REAL NOT NULL, " +
                "seller_username TEXT NOT NULL, " +
                "auction_type INTEGER NOT NULL, " +
                "start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "end_time TIMESTAMP, " +
                "status TEXT DEFAULT 'active', " +
                "highest_bidder TEXT, " +
                "FOREIGN KEY (seller_username) REFERENCES users(username))"
            );
            
            // Auction participants table
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS auction_participants (" +
                "auction_id INTEGER NOT NULL, " +
                "username TEXT NOT NULL, " +
                "PRIMARY KEY (auction_id, username), " +
                "FOREIGN KEY (auction_id) REFERENCES auctions(auction_id), " +
                "FOREIGN KEY (username) REFERENCES users(username))"
            );
            
            // Bids table
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS bids (" +
                "bid_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "auction_id INTEGER NOT NULL, " +
                "username TEXT NOT NULL, " +
                "amount REAL NOT NULL, " +
                "bid_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (auction_id) REFERENCES auctions(auction_id), " +
                "FOREIGN KEY (username) REFERENCES users(username))"
            );
            
            // Prepare statements
            prepareStatements();
            
        } catch (SQLException e) {
            System.err.println("Database initialization error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void prepareStatements() throws SQLException {
        // Users
        getUserStmt = conn.prepareStatement("SELECT * FROM users WHERE username = ?");
        registerUserStmt = conn.prepareStatement("INSERT INTO users (username, password, name, surname, id_number, phone, email, ip_address) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
        
        // Auctions
        createAuctionStmt = conn.prepareStatement("INSERT INTO auctions (item_name, item_description, starting_price, current_price, seller_username, auction_type, end_time) VALUES (?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
        getActiveAuctionsStmt = conn.prepareStatement("SELECT * FROM auctions WHERE status = 'active'");
        getAuctionByIdStmt = conn.prepareStatement("SELECT * FROM auctions WHERE auction_id = ?");
        getUserAuctionsStmt = conn.prepareStatement("SELECT * FROM auctions WHERE seller_username = ? AND status = 'active'");
        getUserRegisteredAuctionsStmt = conn.prepareStatement("SELECT a.* FROM auctions a JOIN auction_participants p ON a.auction_id = p.auction_id WHERE p.username = ? AND a.status = 'active'");
        closeAuctionStmt = conn.prepareStatement("UPDATE auctions SET status = 'closed', current_price = ?, highest_bidder = ? WHERE auction_id = ?");
        
        // Auction participants
        joinAuctionStmt = conn.prepareStatement("INSERT INTO auction_participants (auction_id, username) VALUES (?, ?)");
        checkUserInAuctionStmt = conn.prepareStatement("SELECT * FROM auction_participants WHERE auction_id = ? AND username = ?");
        withdrawFromAuctionStmt = conn.prepareStatement("DELETE FROM auction_participants WHERE auction_id = ? AND username = ?");
        getAuctionParticipantsStmt = conn.prepareStatement("SELECT username FROM auction_participants WHERE auction_id = ?");
        
        // Bids
        placeBidStmt = conn.prepareStatement("INSERT INTO bids (auction_id, username, amount) VALUES (?, ?, ?)");
        getHighestBidStmt = conn.prepareStatement("SELECT b.amount, b.username, b.bid_time FROM bids b WHERE b.auction_id = ? ORDER BY b.amount DESC LIMIT 1");
        checkUserHighestBidderStmt = conn.prepareStatement("SELECT COUNT(*) FROM auctions WHERE highest_bidder = ? AND status = 'active'");
    }
    
    public boolean registerUser(String username, String password, String name, String surname, String idNumber, String phone, String email, String ipAddress) {
        try {
            // Check if username already exists
            getUserStmt.setString(1, username);
            ResultSet rs = getUserStmt.executeQuery();
            if (rs.next()) {
                return false; // User already exists
            }
            
            // Register new user
            registerUserStmt.setString(1, username);
            registerUserStmt.setString(2, password);
            registerUserStmt.setString(3, name);
            registerUserStmt.setString(4, surname);
            registerUserStmt.setString(5, idNumber);
            registerUserStmt.setString(6, phone);
            registerUserStmt.setString(7, email);
            registerUserStmt.setString(8, ipAddress);
            registerUserStmt.executeUpdate();
            
            return true;
        } catch (SQLException e) {
            System.err.println("Error registering user: " + e.getMessage());
            return false;
        }
    }
    
    public User getUser(String username) {
        try {
            getUserStmt.setString(1, username);
            ResultSet rs = getUserStmt.executeQuery();
            
            if (rs.next()) {
                User user = new User();
                user.setUsername(rs.getString("username"));
                user.setPassword(rs.getString("password"));
                user.setName(rs.getString("name"));
                user.setSurname(rs.getString("surname"));
                user.setIdNumber(rs.getString("id_number"));
                user.setPhone(rs.getString("phone"));
                user.setEmail(rs.getString("email"));
                user.setIpAddress(rs.getString("ip_address"));
                return user;
            }
        } catch (SQLException e) {
            System.err.println("Error getting user: " + e.getMessage());
        }
        
        return null;
    }
    
    public int createAuction(String itemName, String itemDescription, double startingPrice, String sellerUsername, int auctionType, Timestamp endTime) {
        try {
            createAuctionStmt.setString(1, itemName);
            createAuctionStmt.setString(2, itemDescription);
            createAuctionStmt.setDouble(3, startingPrice);
            createAuctionStmt.setDouble(4, startingPrice); // Current price starts at starting price
            createAuctionStmt.setString(5, sellerUsername);
            createAuctionStmt.setInt(6, auctionType);
            createAuctionStmt.setTimestamp(7, endTime);
            
            int affectedRows = createAuctionStmt.executeUpdate();
            
            if (affectedRows > 0) {
                ResultSet rs = createAuctionStmt.getGeneratedKeys();
                if (rs.next()) {
                    return rs.getInt(1); // Return the generated auction ID
                }
            }
        } catch (SQLException e) {
            System.err.println("Error creating auction: " + e.getMessage());
        }
        
        return -1;
    }
    
    public List<Auction> getActiveAuctions() {
        List<Auction> auctions = new ArrayList<>();
        
        try {
            ResultSet rs = getActiveAuctionsStmt.executeQuery();
            
            while (rs.next()) {
                Auction auction = new Auction();
                auction.setAuctionId(rs.getInt("auction_id"));
                auction.setItemName(rs.getString("item_name"));
                auction.setItemDescription(rs.getString("item_description"));
                auction.setStartingPrice(rs.getDouble("starting_price"));
                auction.setCurrentPrice(rs.getDouble("current_price"));
                auction.setSellerUsername(rs.getString("seller_username"));
                auction.setAuctionType(rs.getInt("auction_type"));
                auction.setStartTime(rs.getTimestamp("start_time"));
                auction.setEndTime(rs.getTimestamp("end_time"));
                auction.setStatus(rs.getString("status"));
                auction.setHighestBidder(rs.getString("highest_bidder"));
                
                auctions.add(auction);
            }
        } catch (SQLException e) {
            System.err.println("Error getting active auctions: " + e.getMessage());
        }
        
        return auctions;
    }
    
    public boolean joinAuction(int auctionId, String username) {
        try {
            // First check if auction exists and is active
            getAuctionByIdStmt.setInt(1, auctionId);
            ResultSet auctionResult = getAuctionByIdStmt.executeQuery();
            
            if (!auctionResult.next()) {
                return false; // Auction doesn't exist
            }
            
            if (!"active".equals(auctionResult.getString("status"))) {
                return false; // Auction is not active
            }
            
            // Check if user is already in the auction
            checkUserInAuctionStmt.setInt(1, auctionId);
            checkUserInAuctionStmt.setString(2, username);
            ResultSet rs = checkUserInAuctionStmt.executeQuery();
            
            if (rs.next()) {
                return false; // User already in auction
            }
            
            // Add user to auction
            joinAuctionStmt.setInt(1, auctionId);
            joinAuctionStmt.setString(2, username);
            joinAuctionStmt.executeUpdate();
            
            return true;
        } catch (SQLException e) {
            System.err.println("Error joining auction: " + e.getMessage());
            return false;
        }
    }
    
    
    
    public boolean placeBid(int auctionId, String username, double amount) {
        System.out.println("DEBUG: Attempting to place bid for auction " + auctionId + " by user " + username + " amount " + amount);
        
        try {
            conn.setAutoCommit(false);
            
            // Check if user is registered for the auction
            System.out.println("DEBUG: Checking if user is registered for auction");
            checkUserInAuctionStmt.setInt(1, auctionId);
            checkUserInAuctionStmt.setString(2, username);
            ResultSet rs = checkUserInAuctionStmt.executeQuery();
            
            if (!rs.next()) {
                System.out.println("DEBUG: User not registered for auction - rolling back");
                conn.rollback();
                conn.setAutoCommit(true);
                return false; // User not registered for this auction
            }
            
            // Get the current highest bid
            System.out.println("DEBUG: Getting current highest bid");
            getHighestBidStmt.setInt(1, auctionId);
            rs = getHighestBidStmt.executeQuery();
            
            double currentHighestBid = 0;
            if (rs.next()) {
                currentHighestBid = rs.getDouble("amount");
                System.out.println("DEBUG: Current highest bid: " + currentHighestBid);
            } else {
                System.out.println("DEBUG: No previous bids found");
            }
            
            // Get auction details
            System.out.println("DEBUG: Getting auction details");
            getAuctionByIdStmt.setInt(1, auctionId);
            rs = getAuctionByIdStmt.executeQuery();
            
            if (!rs.next()) {
                System.out.println("DEBUG: Auction not found - rolling back");
                conn.rollback();
                conn.setAutoCommit(true);
                return false; // Auction not found
            }
            
            String auctionStatus = rs.getString("status");
            System.out.println("DEBUG: Auction status: " + auctionStatus);
            
            if (!auctionStatus.equals("active")) {
                System.out.println("DEBUG: Auction not active - rolling back");
                conn.rollback();
                conn.setAutoCommit(true);
                return false; // Auction not active
            }
            
            double startingPrice = rs.getDouble("starting_price");
            System.out.println("DEBUG: Starting price: " + startingPrice);
            
            // Check if bid is higher than current highest bid and starting price
            if (amount <= currentHighestBid || amount < startingPrice) {
                System.out.println("DEBUG: Bid too low (amount: " + amount + ", currentHighest: " + currentHighestBid + ", startingPrice: " + startingPrice + ") - rolling back");
                conn.rollback();
                conn.setAutoCommit(true);
                return false; // Bid too low
            }
            
            // Place the bid
            System.out.println("DEBUG: Placing bid in database");
            placeBidStmt.setInt(1, auctionId);
            placeBidStmt.setString(2, username);
            placeBidStmt.setDouble(3, amount);
            placeBidStmt.executeUpdate();
            
            // Update the auction's current price and highest bidder
            System.out.println("DEBUG: Updating auction current price and highest bidder");
            PreparedStatement updateAuctionStmt = conn.prepareStatement(
                "UPDATE auctions SET current_price = ?, highest_bidder = ? WHERE auction_id = ?"
            );
            updateAuctionStmt.setDouble(1, amount);
            updateAuctionStmt.setString(2, username);
            updateAuctionStmt.setInt(3, auctionId);
            updateAuctionStmt.executeUpdate();
            
            System.out.println("DEBUG: Committing transaction");
            conn.commit();
            conn.setAutoCommit(true);
            return true;
        } catch (SQLException e) {
            System.out.println("DEBUG: SQLException occurred - rolling back");
            try {
                conn.rollback();
                conn.setAutoCommit(true);
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
            System.err.println("Error placing bid: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    public Bid getHighestBid(int auctionId) {
        try {
            getHighestBidStmt.setInt(1, auctionId);
            ResultSet rs = getHighestBidStmt.executeQuery();
            
            if (rs.next()) {
                Bid bid = new Bid();
                bid.setAmount(rs.getDouble("amount"));
                bid.setUsername(rs.getString("username"));
                bid.setBidTime(rs.getTimestamp("bid_time"));
                return bid;
            }
        } catch (SQLException e) {
            System.err.println("Error getting highest bid: " + e.getMessage());
        }
        
        return null;
    }
    
    public boolean withdrawFromAuction(int auctionId, String username) {
        try {
            // Check if user is the highest bidder
            getAuctionByIdStmt.setInt(1, auctionId);
            ResultSet rs = getAuctionByIdStmt.executeQuery();
            
            if (rs.next() && username.equals(rs.getString("highest_bidder"))) {
                return false; // User is highest bidder, cannot withdraw
            }
            
            // Withdraw user from auction
            withdrawFromAuctionStmt.setInt(1, auctionId);
            withdrawFromAuctionStmt.setString(2, username);
            int rowsAffected = withdrawFromAuctionStmt.executeUpdate();
            
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error withdrawing from auction: " + e.getMessage());
            return false;
        }
    }
    
    public boolean isUserHighestBidder(String username) {
        try {
            checkUserHighestBidderStmt.setString(1, username);
            ResultSet rs = checkUserHighestBidderStmt.executeQuery();
            
            if (rs.next() && rs.getInt(1) > 0) {
                return true; // User is highest bidder in at least one active auction
            }
        } catch (SQLException e) {
            System.err.println("Error checking if user is highest bidder: " + e.getMessage());
        }
        
        return false;
    }
    
    public void removeUserFromAllAuctions(String username) {
        try {
            PreparedStatement removeUserStmt = conn.prepareStatement(
                "DELETE FROM auction_participants WHERE username = ?"
            );
            removeUserStmt.setString(1, username);
            removeUserStmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error removing user from auctions: " + e.getMessage());
        }
    }
    
    
    public Auction getAuction(int auctionId) {
        try {
            System.out.println("DEBUG: Looking for auction ID: " + auctionId);
            getAuctionByIdStmt.setInt(1, auctionId);
            ResultSet rs = getAuctionByIdStmt.executeQuery();
            
            if (rs.next()) {
                System.out.println("DEBUG: Found auction, status: " + rs.getString("status"));
                Auction auction = new Auction();
                auction.setAuctionId(rs.getInt("auction_id"));
                auction.setItemName(rs.getString("item_name"));
                auction.setItemDescription(rs.getString("item_description"));
                auction.setStartingPrice(rs.getDouble("starting_price"));
                auction.setCurrentPrice(rs.getDouble("current_price"));
                auction.setSellerUsername(rs.getString("seller_username"));
                auction.setAuctionType(rs.getInt("auction_type"));
                auction.setStartTime(rs.getTimestamp("start_time"));
                auction.setEndTime(rs.getTimestamp("end_time"));
                auction.setStatus(rs.getString("status"));
                auction.setHighestBidder(rs.getString("highest_bidder"));
                
                return auction;
            } else {
                System.out.println("DEBUG: No auction found with ID: " + auctionId);
            }
        } catch (SQLException e) {
            System.err.println("Error getting auction: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
//    public Auction getAuction(int auctionId) {
//        try {
//            getAuctionByIdStmt.setInt(1, auctionId);
//            ResultSet rs = getAuctionByIdStmt.executeQuery();
//            
//            if (rs.next()) {
//                Auction auction = new Auction();
//                auction.setAuctionId(rs.getInt("auction_id"));
//                auction.setItemName(rs.getString("item_name"));
//                auction.setItemDescription(rs.getString("item_description"));
//                auction.setStartingPrice(rs.getDouble("starting_price"));
//                auction.setCurrentPrice(rs.getDouble("current_price"));
//                auction.setSellerUsername(rs.getString("seller_username"));
//                auction.setAuctionType(rs.getInt("auction_type"));
//                auction.setStartTime(rs.getTimestamp("start_time"));
//                auction.setEndTime(rs.getTimestamp("end_time"));
//                auction.setStatus(rs.getString("status"));
//                auction.setHighestBidder(rs.getString("highest_bidder"));
//                
//                return auction;
//            }
//        } catch (SQLException e) {
//            System.err.println("Error getting auction: " + e.getMessage());
//        }
//        
//        return null;
//    }
    
    public List<String> getAuctionParticipants(int auctionId) {
        List<String> participants = new ArrayList<>();
        
        try {
            getAuctionParticipantsStmt.setInt(1, auctionId);
            ResultSet rs = getAuctionParticipantsStmt.executeQuery();
            
            while (rs.next()) {
                participants.add(rs.getString("username"));
            }
        } catch (SQLException e) {
            System.err.println("Error getting auction participants: " + e.getMessage());
        }
        
        return participants;
    }
    
    public boolean closeAuction(int auctionId, double finalPrice, String highestBidder) {
        try {
            closeAuctionStmt.setDouble(1, finalPrice);
            closeAuctionStmt.setString(2, highestBidder);
            closeAuctionStmt.setInt(3, auctionId);
            
            int rowsAffected = closeAuctionStmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error closing auction: " + e.getMessage());
            return false;
        }
    }
    
    public void close() {
        try {
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException e) {
            System.err.println("Error closing database connection: " + e.getMessage());
        }
    }
}