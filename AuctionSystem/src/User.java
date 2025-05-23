public class User {
    private String username;
    private String password;
    private String name;
    private String surname;
    private String idNumber;
    private String phone;
    private String email;
    private String ipAddress;
    
    public User() {}
    
    public User(String username, String password, String name, String surname, String idNumber, String phone, String email, String ipAddress) {
        this.username = username;
        this.password = password;
        this.name = name;
        this.surname = surname;
        this.idNumber = idNumber;
        this.phone = phone;
        this.email = email;
        this.ipAddress = ipAddress;
    }
    
    // Getters and setters
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getSurname() {
        return surname;
    }
    
    public void setSurname(String surname) {
        this.surname = surname;
    }
    
    public String getIdNumber() {
        return idNumber;
    }
    
    public void setIdNumber(String idNumber) {
        this.idNumber = idNumber;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
}