package com.example;

// define a class to store game data
public class RoundData {
    int totalMines;
    int totalClicks;
    int gameTime;
    boolean userWon;

    public RoundData(int totalMines, int totalClicks, int gameTime, boolean userWon) {
        this.totalMines = totalMines;
        this.totalClicks = totalClicks;
        this.gameTime = gameTime;
        this.userWon = userWon;
    }

    // Override toString method to display game data as a string
    public String toString() {
        String result = "Total Mines: " + totalMines + ", Total Clicks: " + totalClicks + ", Game Time: " + gameTime + " seconds, ";
        result += userWon ? "User Won" : "PC Won";
        return result;
    }
}