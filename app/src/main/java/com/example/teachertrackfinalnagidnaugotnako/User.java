package com.example.teachertrackfinalnagidnaugotnako;

public class User {
    private int id;
    private String idNumber;
    private String password;
    private String birthdate;
    private String userType;
    private String createdAt;

    // Constructors
    public User() {}

    public User(String idNumber, String password, String birthdate, String userType) {
        this.idNumber = idNumber;
        this.password = password;
        this.birthdate = birthdate;
        this.userType = userType;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getIdNumber() { return idNumber; }
    public void setIdNumber(String idNumber) { this.idNumber = idNumber; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getBirthdate() { return birthdate; }
    public void setBirthdate(String birthdate) { this.birthdate = birthdate; }

    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}