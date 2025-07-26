package model.logic.state;

public class GameState {
    private boolean paused = false;
    private boolean gameOver = false;
    private boolean gameStarted = false;
    private int currentTime = 0;
    private int coins = 20; // Give player 20 coins for testing

    public GameState() {
        // Initialize with default values
    }

    // Game flow state
    public boolean isPaused() {
        return paused || gameOver;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public void setGameOver(boolean gameOver) {
        this.gameOver = gameOver;
        if (gameOver) this.paused = true;
    }

    public boolean isGameStarted() {
        return gameStarted;
    }

    public void setGameStarted(boolean gameStarted) {
        this.gameStarted = gameStarted;
    }

    // Time tracking
    public int getCurrentTime() {
        return currentTime;
    }

    public void setCurrentTime(int currentTime) {
        this.currentTime = currentTime;
    }

    // Economy
    public int getCoins() {
        return coins;
    }

    public void addCoins(int amount) {
        this.coins += amount;
    }

    public void subtractCoins(int amount) {
        this.coins -= amount;
        if (this.coins < 0) this.coins = 0;
    }
}
