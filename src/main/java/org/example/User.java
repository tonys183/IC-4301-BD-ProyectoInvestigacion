package org.example;

public class User {
    private String id;
    private String wholeName;
    private String email;
    private String telephoneNumber;
    public User() {}

    public User(String id, String wholeName) {
        this.id = id;
        this.wholeName = wholeName;
    }

    public User(String id, String wholeName, String email) {
        this.id = id;
        this.wholeName = wholeName;
        this.email = email;
    }

    public User(String id, String wholeName, String email, String telephoneNumber) {
        this.id = id;
        this.wholeName = wholeName;
        this.email = email;
        this.telephoneNumber = telephoneNumber;
    }

    public String getId() { return id; }
    public String getTelephoneNumber() {
        return telephoneNumber;
    }
    public String getEmail() { return email; }
    public String getWholeName() { return wholeName; }

    public void setTelephoneNumber(String telephoneNumber) {
        this.telephoneNumber = telephoneNumber;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public void setWholeName(String wholeName) {
        this.wholeName = wholeName;
    }
    public void setId(String id) { this.id = id; }


    @Override
    public String toString() {
        return "[" + id + "] " + wholeName + " - " + email;
    }
}