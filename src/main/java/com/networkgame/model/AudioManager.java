package com.networkgame.model;

import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class AudioManager {
    private static AudioManager instance;
    private MediaPlayer backgroundMusicPlayer;
    private Map<SoundType, AudioClip> soundEffects = new HashMap<>();
    private double volume = 0.5;
    private boolean isMuted = false;
    private static final double BG_MUSIC_RATIO = 0.5;
    
    public enum SoundType { BACKGROUND_MUSIC, CONNECTION_SUCCESS, LEVEL_COMPLETE, 
                            PACKET_DAMAGE, BUTTON_CLICK, SHOP_PURCHASE, PACKET_LOSS }
    
    private static final Map<SoundType, String> SOUND_FILES = Map.of(
        SoundType.BACKGROUND_MUSIC, "background_music.mp3",
        SoundType.CONNECTION_SUCCESS, "connection_success.mp3",
        SoundType.LEVEL_COMPLETE, "level_complete.mp3",
        SoundType.PACKET_DAMAGE, "packet_damage.mp3",
        SoundType.BUTTON_CLICK, "button_click.mp3",
        SoundType.SHOP_PURCHASE, "shop_purchase.mp3",
        SoundType.PACKET_LOSS, "packet_loss.mp3"
    );
    
    private AudioManager() { loadSounds(); }
    
    public static AudioManager getInstance() {
        if (instance == null) instance = new AudioManager();
        return instance;
    }
    
    private void loadSounds() {
        try {
            URL musicUrl = getClass().getResource("/sounds/" + SOUND_FILES.get(SoundType.BACKGROUND_MUSIC));
            if (musicUrl != null) {
                backgroundMusicPlayer = new MediaPlayer(new Media(musicUrl.toString()));
                backgroundMusicPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                backgroundMusicPlayer.setVolume(volume * BG_MUSIC_RATIO);
            }
            
            for (SoundType type : SoundType.values()) {
                if (type == SoundType.BACKGROUND_MUSIC) continue;
                URL url = getClass().getResource("/sounds/" + SOUND_FILES.get(type));
                if (url != null) {
                    AudioClip clip = new AudioClip(url.toString());
                    clip.setVolume(volume);
                    soundEffects.put(type, clip);
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading sounds: " + e.getMessage());
        }
    }
    
    public void playBackgroundMusic() {
        if (backgroundMusicPlayer != null && !isMuted) backgroundMusicPlayer.play();
    }
    
    public void stopBackgroundMusic() {
        if (backgroundMusicPlayer != null) backgroundMusicPlayer.stop();
    }
    
    public void pauseBackgroundMusic() {
        if (backgroundMusicPlayer != null) backgroundMusicPlayer.pause();
    }
    
    public void playSoundEffect(SoundType type) {
        AudioClip clip = soundEffects.get(type);
        if (clip != null && !isMuted) clip.play();
    }
    
    public void setVolume(double volume) {
        this.volume = Math.max(0, Math.min(1, volume));
        if (backgroundMusicPlayer != null) backgroundMusicPlayer.setVolume(this.volume * BG_MUSIC_RATIO);
        for (AudioClip clip : soundEffects.values()) clip.setVolume(this.volume);
    }
    
    public double getVolume() { return volume; }
    
    public void setMuted(boolean muted) {
        this.isMuted = muted;
        if (isMuted) pauseBackgroundMusic(); else playBackgroundMusic();
    }
    
    public boolean isMuted() { return isMuted; }
} 