package com.example.leitor.model;

public class MatchResult {
    private boolean match;
    private double score;
    private Long userId;
    private String userName;
    private String userRegistration;
    private String error;

    // Construtor para sucesso
    public MatchResult(boolean match, double score, Long userId, String userName, String userRegistration) {
        this.match = match;
        this.score = score;
        this.userId = userId;
        this.userName = userName;
        this.userRegistration = userRegistration;
    }

    // Construtor para erro
    public MatchResult(boolean match, double score, String error) {
        this.match = match;
        this.score = score;
        this.error = error;
    }

    // Getters e Setters
    public boolean isMatch() {
        return match;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public String getUserRegistration() {
        return userRegistration;
    }

    public double getScore() {
        return score;
    }

    public String getError() {
        return error;
    }
}