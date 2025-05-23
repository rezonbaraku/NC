import java.sql.Timestamp;

public class Auction {
    private int auctionId;
    private String itemName;
    private String itemDescription;
    private double startingPrice;
    private double currentPrice;
    private String sellerUsername;
    private int auctionType; // 1 for timed, 2 for ongoing until no more bids
    private Timestamp startTime;
    private Timestamp endTime;
    private String status; // active or closed
    private String highestBidder;
    
    public Auction() {}
    
    // Getters and setters
    public int getAuctionId() {
        return auctionId;
    }
    
    public void setAuctionId(int auctionId) {
        this.auctionId = auctionId;
    }
    
    public String getItemName() {
        return itemName;
    }
    
    public void setItemName(String itemName) {
        this.itemName = itemName;
    }
    
    public String getItemDescription() {
        return itemDescription;
    }
    
    public void setItemDescription(String itemDescription) {
        this.itemDescription = itemDescription;
    }
    
    public double getStartingPrice() {
        return startingPrice;
    }
    
    public void setStartingPrice(double startingPrice) {
        this.startingPrice = startingPrice;
    }
    
    public double getCurrentPrice() {
        return currentPrice;
    }
    
    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }
    
    public String getSellerUsername() {
        return sellerUsername;
    }
    
    public void setSellerUsername(String sellerUsername) {
        this.sellerUsername = sellerUsername;
    }
    
    public int getAuctionType() {
        return auctionType;
    }
    
    public void setAuctionType(int auctionType) {
        this.auctionType = auctionType;
    }
    
    public Timestamp getStartTime() {
        return startTime;
    }
    
    public void setStartTime(Timestamp startTime) {
        this.startTime = startTime;
    }
    
    public Timestamp getEndTime() {
        return endTime;
    }
    
    public void setEndTime(Timestamp endTime) {
        this.endTime = endTime;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getHighestBidder() {
        return highestBidder;
    }
    
    public void setHighestBidder(String highestBidder) {
        this.highestBidder = highestBidder;
    }
    
    @Override
    public String toString() {
        return "Auction ID: " + auctionId + 
               ", Item: " + itemName + 
               ", Description: " + itemDescription + 
               ", Starting Price: " + startingPrice + 
               ", Current Price: " + currentPrice + 
               ", Seller: " + sellerUsername;
    }
}