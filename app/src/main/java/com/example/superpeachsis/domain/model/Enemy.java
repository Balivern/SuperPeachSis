package com.example.superpeachsis.domain.model;

import android.graphics.Bitmap;
import android.graphics.Rect;

import java.util.List;

public class Enemy {
    private List<Bitmap> walkFrames;
    public int deathAnimationTimer = 0;
    private static final int DEATH_DURATION = 60;
    private Bitmap deathBitmap;
    public int x, y;
    public int drawWidth, drawHeight;
    public boolean active;
    public boolean isDead;
    private final Rect rect = new Rect();

    private int frameIndex = 0;
    private int frameTick = 0;
    private static final int TICKS_PER_FRAME = 6;

    public Enemy() {
        this.active = false;
    }

    public void spawn(List<Bitmap> walkFrames, Bitmap deathBitmap, int x, int y, int drawSize) {
        this.walkFrames = walkFrames;
        this.deathBitmap = deathBitmap;
        this.x = x;
        this.y = y;
        this.drawWidth = drawSize;
        this.drawHeight = drawSize;
        this.active = true;
        this.isDead = false;
        this.frameIndex = 0;
    }

    public void update(int speed) {
        if (!active) return;
        x -= speed;

        if (!isDead && walkFrames != null) {
            frameTick++;
            if (frameTick >= TICKS_PER_FRAME) {
                frameTick = 0;
                frameIndex = (frameIndex + 1) % walkFrames.size();
            }
        }

        if (isDead && deathAnimationTimer > 0) {
            deathAnimationTimer--;
            if (deathAnimationTimer == 0) {
                active = false;
            }
        }

        if (x + drawWidth < 0) active = false;
    }

    public Rect getRect() {
        rect.set(x, y, x + drawWidth, y + drawHeight);
        return rect;
    }

    public Bitmap getCurrentBitmap() {
        if (isDead) return deathBitmap;
        return walkFrames.get(frameIndex);
    }

    public void kill() {
        if (!this.isDead) {
            this.isDead = true;
            this.deathAnimationTimer = DEATH_DURATION;
        }
    }
}
