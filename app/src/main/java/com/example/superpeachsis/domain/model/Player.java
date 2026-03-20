package com.example.superpeachsis.domain.model;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import com.example.superpeachsis.utils.SpriteManager;
import java.util.List;

public class Player {

    public enum State {
        IDLE, RUNNING, JUMPING, DUCKING
    }

    private float x, y;
    private float vx, vy;
    private State state;

    private List<Bitmap> walkFrames;
    private Bitmap idleFrame;
    private Bitmap jumpFrame;
    private Bitmap duckFrame;

    private int frameIndex = 0;
    private long lastFrameTime = 0;
    private static final long FRAME_DURATION_MS = 200;

    private static final float GRAVITY = 0.8f;
    private static final float MAX_VY = 15f;
    private int drawHeight = 128;
    private int drawWidth = 128;

    public Player(float x, float y, SpriteManager spriteManager) {
        this.x = x;
        this.y = y;
        this.vx = 0;
        this.vy = 0;
        this.state = State.RUNNING;
        loadSprites(spriteManager);
        lastFrameTime = System.currentTimeMillis();
    }

    private void loadSprites(SpriteManager spriteManager) {
        walkFrames = spriteManager.getCharacterFrames("pink", "walk_a", "walk_b");
        idleFrame = spriteManager.loadBitmap("characters/character_pink_idle.png");
        jumpFrame = spriteManager.loadBitmap("characters/character_pink_jump.png");
        duckFrame = spriteManager.loadBitmap("characters/character_pink_duck.png");
    }

    public void update() {
        vy += GRAVITY;
        if (vy > MAX_VY) {
            vy = MAX_VY;
        }

        y += vy;

        if (state == State.RUNNING) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastFrameTime >= FRAME_DURATION_MS) {
                frameIndex = (frameIndex + 1) % walkFrames.size();
                lastFrameTime = currentTime;
            }
        }
    }

    public void draw(Canvas canvas) {
        Bitmap currentFrame = getCurrentFrame();
        if (currentFrame != null) {
            drawWidth = (int) (currentFrame.getWidth() * ((float) drawHeight / currentFrame.getHeight()));
            Rect dst = new Rect((int) x, (int) y, (int) x + drawWidth, (int) y + drawHeight);
            canvas.drawBitmap(currentFrame, null, dst, null);
        }
    }

    private Bitmap getCurrentFrame() {
        switch (state) {
            case RUNNING:
                if (walkFrames != null && !walkFrames.isEmpty()) {
                    return walkFrames.get(frameIndex);
                }
                return idleFrame;
            case JUMPING:
                return jumpFrame != null ? jumpFrame : idleFrame;
            case DUCKING:
                return duckFrame != null ? duckFrame : idleFrame;
            default:
                return idleFrame;
        }
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public void setX(float x) { this.x = x; }
    public void setY(float y) { this.y = y; }
    public void setVy(float vy) { this.vy = vy; }
    public int getDrawHeight() { return drawHeight; }
    public State getState() { return state; }
    public void setState(State state) { this.state = state; }
}
