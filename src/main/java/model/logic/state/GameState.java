package model.logic.state;

public class GameState {
    private boolean paused = false;
    private boolean gameOver = false;
    private boolean gameStarted = false;
    private int currentTime = 0;
    private int coins = 20; 
    private boolean levelCompleted = false;

    public GameState() {
    }

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

    public int getCurrentTime() {
        return currentTime;
    }

    public void setCurrentTime(int currentTime) {
        this.currentTime = currentTime;
    }

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

    public boolean isLevelCompleted() {
        return levelCompleted;
    }

    public void setLevelCompleted(boolean levelCompleted) {
        this.levelCompleted = levelCompleted;
        if (levelCompleted) this.paused = true;
    }
}
