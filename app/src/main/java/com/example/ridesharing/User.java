package com.example.ridesharing;

public class User {
    private String uid;
    private String fullName;
    private String email;
    private String phone;
    private String studentId;
    private String profileImageUrl;
    private long createdAt;
    private boolean isVerified;
    private String userType; // "student", "faculty", etc.

    // Default constructor required for Firebase
    public User() {}

    public User(String uid, String fullName, String email, String phone, String studentId) {
        this.uid = uid;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.studentId = studentId;
        this.createdAt = System.currentTimeMillis();
        this.isVerified = false;
        this.userType = "student";
    }

    // Google Sign-in constructor
    public User(String uid, String fullName, String email) {
        this.uid = uid;
        this.fullName = fullName;
        this.email = email;
        this.createdAt = System.currentTimeMillis();
        this.isVerified = true; // Google users are pre-verified
        this.userType = "student";
    }

    // Getters and Setters
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public boolean isVerified() { return isVerified; }
    public void setVerified(boolean verified) { isVerified = verified; }

    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }
}