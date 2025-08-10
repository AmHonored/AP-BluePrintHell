package service;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class AudioManager {
    private static MediaPlayer backgroundMusicPlayer;
    private static MediaPlayer menuMusicPlayer;
    private static double volume = 0.7;
    private static final double MUSIC_VOLUME_FACTOR = 0.5;
    
    private static final Map<String, MediaPlayer> sfxPlayers = new HashMap<>();
    
    private static final String RES_BUTTON_CLICK = "/sounds/button_click.mp3";
    private static final String RES_CONNECTION_SUCCESS = "/sounds/connection_success.mp3";
    private static final String RES_LEVEL_COMPLETE = "/sounds/level_complete.mp3";
    private static final String RES_PACKET_DAMAGE = "/sounds/packet_damage.mp3";
    private static final String RES_SHOP_PURCHASE = "/sounds/shop_purchase.mp3";
    private static final String RES_BG_MUSIC = "/sounds/background_music.mp3";
    private static final String RES_MENU_MUSIC = "/sounds/menu_music.mp3";
    
    private static MediaPlayer createPlayer(String resourcePath, boolean loop, double volumeFactor) {
        URL url = AudioManager.class.getResource(resourcePath);
        if (url == null) return null;
        Media media = new Media(url.toExternalForm());
        MediaPlayer player = new MediaPlayer(media);
        player.setVolume(volume * volumeFactor);
        if (loop) {
            player.setCycleCount(MediaPlayer.INDEFINITE);
        }
        return player;
    }

    private static MediaPlayer getOrCreateSfx(String resourcePath) {
        MediaPlayer player = sfxPlayers.get(resourcePath);
        if (player == null) {
            player = createPlayer(resourcePath, false, 1.0);
            if (player != null) sfxPlayers.put(resourcePath, player);
        }
        return player;
    }

    private static void playEffect(String resourcePath) {
        try {
            MediaPlayer player = getOrCreateSfx(resourcePath);
            if (player != null) {
                player.stop();
                player.seek(Duration.ZERO);
                player.play();
            }
        } catch (Exception e) {
            System.err.println("Error playing sound: " + resourcePath + ": " + e.getMessage());
        }
    }

    public static void playBackgroundMusic() {
        try {
            stopBackgroundMusic();
            stopMenuMusic();
            
            if (backgroundMusicPlayer == null) {
                backgroundMusicPlayer = createPlayer(RES_BG_MUSIC, true, MUSIC_VOLUME_FACTOR);
            } else {
                backgroundMusicPlayer.setVolume(volume * MUSIC_VOLUME_FACTOR);
                backgroundMusicPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            }
            if (backgroundMusicPlayer != null) {
                backgroundMusicPlayer.play();
            }
        } catch (Exception e) {
        }
    }
    
    private static void stopBackgroundMusic() {
        if (backgroundMusicPlayer != null) {
            backgroundMusicPlayer.stop();
            backgroundMusicPlayer.dispose();
            backgroundMusicPlayer = null;
        }
    }
    
    public static void playMenuMusic() {
        try {
            stopBackgroundMusic();
            stopMenuMusic();
            if (menuMusicPlayer == null) {
                menuMusicPlayer = createPlayer(RES_MENU_MUSIC, true, MUSIC_VOLUME_FACTOR);
            } else {
                menuMusicPlayer.setVolume(volume * MUSIC_VOLUME_FACTOR);
                menuMusicPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            }
            if (menuMusicPlayer != null) {
                menuMusicPlayer.play();
            }
        } catch (Exception e) {
        }
    }
    
    public static void stopMenuMusic() {
        if (menuMusicPlayer != null) {
            menuMusicPlayer.stop();
        }
    }

    public static void playButtonClick() {
        playEffect(RES_BUTTON_CLICK);
    }
    
    public static void playConnectionSuccess() {
        playEffect(RES_CONNECTION_SUCCESS);
    }

    public static void playLevelComplete() {
        stopBackgroundMusic();
        playEffect(RES_LEVEL_COMPLETE);
    }
    
    public static void playPacketDamage() {
        playEffect(RES_PACKET_DAMAGE);
    }
    
    public static void playShopPurchase() {
        playEffect(RES_SHOP_PURCHASE);
    }
    
    public static void setVolume(double newVolume) {
        volume = Math.max(0.0, Math.min(1.0, newVolume));
        
        if (backgroundMusicPlayer != null) {
            backgroundMusicPlayer.setVolume(volume * MUSIC_VOLUME_FACTOR);
        }
        if (menuMusicPlayer != null) {
            menuMusicPlayer.setVolume(volume * MUSIC_VOLUME_FACTOR);
        }
        for (MediaPlayer p : sfxPlayers.values()) {
            if (p != null) p.setVolume(volume);
        }
    }

    public static void cleanup() {
        stopBackgroundMusic();
        stopMenuMusic();
        
        if (menuMusicPlayer != null) {
            menuMusicPlayer.dispose();
            menuMusicPlayer = null;
        }
        for (MediaPlayer p : sfxPlayers.values()) {
            try { if (p != null) p.dispose(); } catch (Exception ignored) {}
        }
        sfxPlayers.clear();
    }
    
}
