package service;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import java.io.File;
import java.net.URL;

public class AudioManager {
    private static MediaPlayer backgroundMusicPlayer;
    private static MediaPlayer menuMusicPlayer;
    private static double volume = 0.7;
    private static boolean soundEnabled = true;
    
    // Sound effect players
    private static MediaPlayer buttonClickPlayer;
    private static MediaPlayer connectionSuccessPlayer;
    private static MediaPlayer levelCompletePlayer;
    private static MediaPlayer packetDamagePlayer;
    private static MediaPlayer shopPurchasePlayer;
    
    static {
        initializeSoundEffects();
    }
    
    /**
     * Initialize all sound effect players
     */
    private static void initializeSoundEffects() {
        try {
            // Initialize button click sound
            URL buttonClickUrl = AudioManager.class.getResource("/sounds/button_click.mp3");
            if (buttonClickUrl != null) {
                Media buttonClickMedia = new Media(buttonClickUrl.toExternalForm());
                buttonClickPlayer = new MediaPlayer(buttonClickMedia);
                buttonClickPlayer.setVolume(volume);
            }
            
            // Initialize connection success sound
            URL connectionSuccessUrl = AudioManager.class.getResource("/sounds/connection_success.mp3");
            if (connectionSuccessUrl != null) {
                Media connectionSuccessMedia = new Media(connectionSuccessUrl.toExternalForm());
                connectionSuccessPlayer = new MediaPlayer(connectionSuccessMedia);
                connectionSuccessPlayer.setVolume(volume);
            }
            
            // Initialize level complete sound
            URL levelCompleteUrl = AudioManager.class.getResource("/sounds/level_complete.mp3");
            if (levelCompleteUrl != null) {
                Media levelCompleteMedia = new Media(levelCompleteUrl.toExternalForm());
                levelCompletePlayer = new MediaPlayer(levelCompleteMedia);
                levelCompletePlayer.setVolume(volume);
            }
            
            // Initialize packet damage sound
            URL packetDamageUrl = AudioManager.class.getResource("/sounds/packet_damage.mp3");
            if (packetDamageUrl != null) {
                Media packetDamageMedia = new Media(packetDamageUrl.toExternalForm());
                packetDamagePlayer = new MediaPlayer(packetDamageMedia);
                packetDamagePlayer.setVolume(volume);
            }
            
            // Initialize shop purchase sound
            URL shopPurchaseUrl = AudioManager.class.getResource("/sounds/shop_purchase.mp3");
            if (shopPurchaseUrl != null) {
                Media shopPurchaseMedia = new Media(shopPurchaseUrl.toExternalForm());
                shopPurchasePlayer = new MediaPlayer(shopPurchaseMedia);
                shopPurchasePlayer.setVolume(volume);
            }
            
            // Initialize menu music
            URL menuMusicUrl = AudioManager.class.getResource("/sounds/menu_music.mp3");
            if (menuMusicUrl != null) {
                Media menuMusic = new Media(menuMusicUrl.toExternalForm());
                menuMusicPlayer = new MediaPlayer(menuMusic);
                menuMusicPlayer.setVolume(volume * 0.5); // Menu music at half volume
                menuMusicPlayer.setCycleCount(MediaPlayer.INDEFINITE); // Loop indefinitely
            }
            
        } catch (Exception e) {
            System.err.println("Error initializing sound effects: " + e.getMessage());
        }
    }
    
    /**
     * Play background music for game levels
     */
    public static void playBackgroundMusic() {
        if (!soundEnabled) return;
        
        try {
            // Stop any existing music
            stopBackgroundMusic();
            stopMenuMusic();
            
            URL backgroundMusicUrl = AudioManager.class.getResource("/sounds/background_music.mp3");
            if (backgroundMusicUrl != null) {
                Media backgroundMusic = new Media(backgroundMusicUrl.toExternalForm());
                backgroundMusicPlayer = new MediaPlayer(backgroundMusic);
                backgroundMusicPlayer.setVolume(volume * 0.5); // Background music at half volume
                backgroundMusicPlayer.setCycleCount(MediaPlayer.INDEFINITE); // Loop indefinitely
                backgroundMusicPlayer.play();
                System.out.println("AUDIO: Playing background music");
            }
        } catch (Exception e) {
            System.err.println("Error playing background music: " + e.getMessage());
        }
    }
    
    /**
     * Stop background music
     */
    public static void stopBackgroundMusic() {
        if (backgroundMusicPlayer != null) {
            backgroundMusicPlayer.stop();
            backgroundMusicPlayer.dispose();
            backgroundMusicPlayer = null;
            System.out.println("AUDIO: Stopped background music");
        }
    }
    
    /**
     * Play menu music
     */
    public static void playMenuMusic() {
        if (!soundEnabled) return;
        
        try {
            // Stop any existing music
            stopBackgroundMusic();
            stopMenuMusic();
            
            if (menuMusicPlayer != null) {
                menuMusicPlayer.play();
                System.out.println("AUDIO: Playing menu music");
            }
        } catch (Exception e) {
            System.err.println("Error playing menu music: " + e.getMessage());
        }
    }
    
    /**
     * Stop menu music
     */
    public static void stopMenuMusic() {
        if (menuMusicPlayer != null) {
            menuMusicPlayer.stop();
            System.out.println("AUDIO: Stopped menu music");
        }
    }
    
    /**
     * Play button click sound effect
     */
    public static void playButtonClick() {
        if (!soundEnabled || buttonClickPlayer == null) return;
        
        try {
            buttonClickPlayer.stop();
            buttonClickPlayer.seek(Duration.ZERO);
            buttonClickPlayer.play();
            System.out.println("AUDIO: Playing button click sound");
        } catch (Exception e) {
            System.err.println("Error playing button click sound: " + e.getMessage());
        }
    }
    
    /**
     * Play connection success sound effect
     */
    public static void playConnectionSuccess() {
        if (!soundEnabled || connectionSuccessPlayer == null) return;
        
        try {
            connectionSuccessPlayer.stop();
            connectionSuccessPlayer.seek(Duration.ZERO);
            connectionSuccessPlayer.play();
            System.out.println("AUDIO: Playing connection success sound");
        } catch (Exception e) {
            System.err.println("Error playing connection success sound: " + e.getMessage());
        }
    }
    
    /**
     * Play level complete sound effect and stop background music
     */
    public static void playLevelComplete() {
        if (!soundEnabled) return;
        
        // Stop background music first
        stopBackgroundMusic();
        
        // Play level complete sound
        if (levelCompletePlayer != null) {
            try {
                levelCompletePlayer.stop();
                levelCompletePlayer.seek(Duration.ZERO);
                levelCompletePlayer.play();
                System.out.println("AUDIO: Playing level complete sound");
            } catch (Exception e) {
                System.err.println("Error playing level complete sound: " + e.getMessage());
            }
        }
    }
    
    /**
     * Play packet damage sound effect
     */
    public static void playPacketDamage() {
        if (!soundEnabled || packetDamagePlayer == null) return;
        
        try {
            packetDamagePlayer.stop();
            packetDamagePlayer.seek(Duration.ZERO);
            packetDamagePlayer.play();
            System.out.println("AUDIO: Playing packet damage sound");
        } catch (Exception e) {
            System.err.println("Error playing packet damage sound: " + e.getMessage());
        }
    }
    
    /**
     * Play shop purchase sound effect
     */
    public static void playShopPurchase() {
        if (!soundEnabled || shopPurchasePlayer == null) return;
        
        try {
            shopPurchasePlayer.stop();
            shopPurchasePlayer.seek(Duration.ZERO);
            shopPurchasePlayer.play();
            System.out.println("AUDIO: Playing shop purchase sound");
        } catch (Exception e) {
            System.err.println("Error playing shop purchase sound: " + e.getMessage());
        }
    }
    
    /**
     * Set the volume for all sounds (0.0 to 1.0)
     */
    public static void setVolume(double newVolume) {
        volume = Math.max(0.0, Math.min(1.0, newVolume));
        
        // Update all player volumes
        if (backgroundMusicPlayer != null) {
            backgroundMusicPlayer.setVolume(volume * 0.5);
        }
        if (menuMusicPlayer != null) {
            menuMusicPlayer.setVolume(volume * 0.5);
        }
        if (buttonClickPlayer != null) {
            buttonClickPlayer.setVolume(volume);
        }
        if (connectionSuccessPlayer != null) {
            connectionSuccessPlayer.setVolume(volume);
        }
        if (levelCompletePlayer != null) {
            levelCompletePlayer.setVolume(volume);
        }
        if (packetDamagePlayer != null) {
            packetDamagePlayer.setVolume(volume);
        }
        if (shopPurchasePlayer != null) {
            shopPurchasePlayer.setVolume(volume);
        }
    }
    
    /**
     * Enable or disable all sounds
     */
    public static void setSoundEnabled(boolean enabled) {
        soundEnabled = enabled;
        if (!enabled) {
            stopBackgroundMusic();
        }
    }
    
    /**
     * Get current volume
     */
    public static double getVolume() {
        return volume;
    }
    
    /**
     * Check if sound is enabled
     */
    public static boolean isSoundEnabled() {
        return soundEnabled;
    }
    
    /**
     * Clean up all media players
     */
    public static void cleanup() {
        stopBackgroundMusic();
        stopMenuMusic();
        
        if (menuMusicPlayer != null) {
            menuMusicPlayer.dispose();
            menuMusicPlayer = null;
        }
        if (buttonClickPlayer != null) {
            buttonClickPlayer.dispose();
            buttonClickPlayer = null;
        }
        if (connectionSuccessPlayer != null) {
            connectionSuccessPlayer.dispose();
            connectionSuccessPlayer = null;
        }
        if (levelCompletePlayer != null) {
            levelCompletePlayer.dispose();
            levelCompletePlayer = null;
        }
        if (packetDamagePlayer != null) {
            packetDamagePlayer.dispose();
            packetDamagePlayer = null;
        }
        if (shopPurchasePlayer != null) {
            shopPurchasePlayer.dispose();
            shopPurchasePlayer = null;
        }
    }
    
    /**
     * Play collision sound effect (legacy method for compatibility)
     */
    public static void playCollision() {
        playPacketDamage();
    }
}
