

import java.sql.Timestamp;

public class Bid {
    private int bidId;
    private int auctionId;
    private String username;
    private double amount;
    private Timestamp bidTime;
    
    public Bid() {}
    
    public Bid(int bidId, int auctionId, String username, double amount, Timestamp bidTime) {
        this.bidId = bidId;
        this.auctionId = auctionId;
        this.username = username;
        this.amount = amount;
        this.bidTime = bidTime;
    }
    
    // Getters and setters
    public int getBidId() {
        return bidId;
    }
    
    public void setBidId(int bidId) {
        this.bidId = bidId;
    }
    
    public int getAuctionId() {
        return auctionId;
    }
    
    public void setAuctionId(int auctionId) {
        this.auctionId = auctionId;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public double getAmount() {
        return amount;
    }
    
    public void setAmount(double amount) {
        this.amount = amount;
    }
    
    public Timestamp getBidTime() {
        return bidTime;
    }
    
    public void setBidTime(Timestamp bidTime) {
        this.bidTime = bidTime;
    }
}