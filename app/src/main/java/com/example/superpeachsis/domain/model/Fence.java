package com.example.superpeachsis.domain.model;

import android.graphics.Bitmap;

public class Fence extends Block {
    public enum Pattern {
        CIRCLE, CROSS, LINE
    }
    private Bitmap brokenBitmap;
    private Bitmap originalBitmap;
    public boolean broken = false;
    public Pattern expectedPattern;
    public int deathAnimationTimer = 0;
    private static final int DEATH_DURATION = 60; // 1 seconde

    public void spawn(Bitmap bitmap, Bitmap brokenBitmap, int x, int y, Pattern pattern) {
        super.spawn(bitmap, x, y);
        this.originalBitmap = bitmap;
        this.brokenBitmap = brokenBitmap;
        this.broken = false;
        this.expectedPattern = pattern;
        this.deathAnimationTimer = 0;
    }

    public void breakFence() {
        this.broken = true;
        this.bitmap = brokenBitmap;
        this.deathAnimationTimer = DEATH_DURATION;
    }
    
    public void reset() {
        this.broken = false;
        this.bitmap = originalBitmap;
    }

    @Override
    public void update(int speed) {
        super.update(speed);

        if (broken && deathAnimationTimer > 0) {
            deathAnimationTimer--;
        }
    }
}
