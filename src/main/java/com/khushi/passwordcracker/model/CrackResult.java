package com.khushi.passwordcracker.model;

public class CrackResult {

    private String password;
    private long attempts;
    private long timeTaken;
    private boolean success;

    public CrackResult(String password, long attempts, long timeTaken, boolean success) {
        this.password = password;
        this.attempts = attempts;
        this.timeTaken = timeTaken;
        this.success = success;
    }

    public String getPassword() {
        return password;
    }

    public long getAttempts() {
        return attempts;
    }

    public long getTimeTaken() {
        return timeTaken;
    }

    public boolean isSuccess() {
        return success;
    }

    @Override
    public String toString() {
        return "Password : " + password +
                "\nAttempts : " + attempts +
                "\nTime Taken : " + timeTaken + " ms" +
                "\nSuccess : " + success;
    }
}