package com.example.superpeachsis;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.List;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {

    private GameThread thread;
    private SpriteManager spriteManager;

    private List<Bitmap> walkFrames;
    private Bitmap idleFrame;
    private Bitmap backgroundBitmap;

    private int frameIndex = 0;
    private int frameTick = 0;
    private static final int TICKS_PER_FRAME = 12;

    private int playerX = 200;
    private int playerY = 400;

    public GameView(Context context) {
        super(context);
        getHolder().addCallback(this);
        spriteManager = SpriteManager.getInstance(context);
        thread = new GameThread(getHolder(), this);
        setFocusable(true);
        loadSprites();
    }

    private void loadSprites() {
        walkFrames = spriteManager.getCharacterFrames("pink", "walk_a", "walk_b");
        idleFrame = spriteManager.loadBitmap("characters/character_pink_idle.png");
        backgroundBitmap = spriteManager.getBackground("background_color_trees");
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        thread.setRunning(true);
        thread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        boolean retry = true;
        while (retry) {
            try {
                thread.setRunning(false);
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            retry = false;
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (canvas == null) {
            return;
        }

        canvas.drawColor(Color.parseColor("#5C94FC"));

        if (backgroundBitmap != null) {
            Bitmap bg = Bitmap.createScaledBitmap(backgroundBitmap, getWidth(), getHeight(), true);
            canvas.drawBitmap(bg, 0, 0, null);
        }

        Bitmap currentFrame = getCurrentFrame();
        if (currentFrame != null) {
            canvas.drawBitmap(currentFrame, playerX, playerY, null);
        }
    }

    private Bitmap getCurrentFrame() {
        if (walkFrames != null && !walkFrames.isEmpty()) {
            return walkFrames.get(frameIndex);
        }
        return idleFrame;
    }

    public void update() {
        frameTick++;
        if (frameTick >= TICKS_PER_FRAME) {
            frameTick = 0;
            if (walkFrames != null && !walkFrames.isEmpty()) {
                frameIndex = (frameIndex + 1) % walkFrames.size();
            }
        }
    }
}
