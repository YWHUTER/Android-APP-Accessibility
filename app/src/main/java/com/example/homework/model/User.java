package com.example.homework.model;

public class User {
    private long id;
    private String name;
    private String username;
    private String password;
    private String major;
    private String interests;
    private String bio;
    private String studentIdImageUrl;
    private String avatarImageUrl;
    private long verifiedAt;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

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

    public String getMajor() {
        return major;
    }

    public void setMajor(String major) {
        this.major = major;
    }

    public String getInterests() {
        return interests;
    }

    public void setInterests(String interests) {
        this.interests = interests;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getStudentIdImageUrl() {
        return studentIdImageUrl;
    }

    public void setStudentIdImageUrl(String studentIdImageUrl) {
        this.studentIdImageUrl = studentIdImageUrl;
    }

    public String getAvatarImageUrl() {
        return avatarImageUrl;
    }

    public void setAvatarImageUrl(String avatarImageUrl) {
        this.avatarImageUrl = avatarImageUrl;
    }

    public long getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(long verifiedAt) {
        this.verifiedAt = verifiedAt;
    }
}
