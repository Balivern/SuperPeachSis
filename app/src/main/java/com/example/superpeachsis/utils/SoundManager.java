package com.example.superpeachsis.utils;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;

import java.io.IOException;

public class SoundManager {

    public static final String MUSIC_GAME     = "game";
    public static final String MUSIC_GAMEOVER = "gameover";

    private static SoundManager instance;

    private final Context context;
    private MediaPlayer musicPlayer;
    private final SoundPool soundPool;

    private int soundJump        = -1;
    private int soundGhostAppear = -1;
    private int soundBoxBreak    = -1;

    private SoundManager(Context context) {
        this.context = context.getApplicationContext();

        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder()
                .setMaxStreams(5)
                .setAudioAttributes(attrs)
                .build();

        soundJump        = loadSound("sounds/jump.mp3");
        soundGhostAppear = loadSound("sounds/ghostappear.mp3");
        soundBoxBreak    = loadSound("sounds/boxbreak.mp3");
    }

    public static SoundManager getInstance(Context context) {
        if (instance == null) {
            instance = new SoundManager(context);
        }
        return instance;
    }

    // ── Musique ────────────────────────────────────────────────────────────

    public void playMusic(String name) {
        stopMusic();
        try {
            AssetFileDescriptor afd = context.getAssets().openFd("music/" + name + ".mp3");
            musicPlayer = new MediaPlayer();
            musicPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
            musicPlayer.setLooping(MUSIC_GAME.equals(name));
            musicPlayer.setVolume(0.7f, 0.7f);
            musicPlayer.prepare();
            musicPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void pauseMusic() {
        if (musicPlayer != null && musicPlayer.isPlaying()) {
            musicPlayer.pause();
        }
    }

    public void resumeMusic() {
        if (musicPlayer != null && !musicPlayer.isPlaying()) {
            musicPlayer.start();
        }
    }

    public void stopMusic() {
        if (musicPlayer != null) {
            musicPlayer.stop();
            musicPlayer.release();
            musicPlayer = null;
        }
    }

    // ── Effets sonores ─────────────────────────────────────────────────────

    public void playJump()        { playSound(soundJump); }
    public void playGhostAppear() { playSound(soundGhostAppear); }
    public void playBoxBreak()    { playSound(soundBoxBreak); }

    private void playSound(int soundId) {
        if (soundId != -1) {
            soundPool.play(soundId, 1f, 1f, 1, 0, 1f);
        }
    }

    private int loadSound(String path) {
        try {
            AssetFileDescriptor afd = context.getAssets().openFd(path);
            int id = soundPool.load(afd, 1);
            afd.close();
            return id;
        } catch (IOException e) {
            return -1;
        }
    }
}
