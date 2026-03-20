package com.example.superpeachsis;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.example.superpeachsis.utils.Camera;
import com.example.superpeachsis.utils.SpriteManager;

import java.util.List;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {

    private GameThread thread;
    private SpriteManager spriteManager;
    private Camera camera;

    private List<Bitmap> walkFrames;
    private Bitmap idleFrame;
    private Bitmap backgroundBitmap;
    private Bitmap groundTile;

    private int frameIndex = 0;
    private int frameTick = 0;
    private static final int TICKS_PER_FRAME = 12;
    private static final int TILE_SIZE = 64;
    private static final float PARALLAX_FACTOR = 0.3f;

    private int screenWidth;
    private int screenHeight;

    public GameView(Context context) {
        super(context);
        getHolder().addCallback(this);
        spriteManager = SpriteManager.getInstance(context);
        camera = new Camera(3f);
        thread = new GameThread(getHolder(), this);
        setFocusable(true);
        loadSprites();
    }

    private void loadSprites() {
        walkFrames = spriteManager.getCharacterFrames("pink", "walk_a", "walk_b");
        idleFrame = spriteManager.loadBitmap("characters/character_pink_idle.png");
        backgroundBitmap = spriteManager.getBackground("background_color_trees");
        groundTile = spriteManager.getTile("terrain_grass_block_top");
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        screenWidth = getWidth();
        screenHeight = getHeight();
        thread.setRunning(true);
        thread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        screenWidth = width;
        screenHeight = height;
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
        drawBackground(canvas);
        drawGround(canvas);
        drawPlayer(canvas);
    }

    private void drawBackground(Canvas canvas) {
        if (backgroundBitmap == null) {
            return;
        }
        float parallaxX = -(camera.getX() * PARALLAX_FACTOR) % screenWidth;
        Rect dst = new Rect(0, 0, screenWidth, screenHeight);

        canvas.save();
        canvas.translate(parallaxX, 0);
        canvas.drawBitmap(backgroundBitmap, null, dst, null);
        canvas.translate(screenWidth, 0);
        canvas.drawBitmap(backgroundBitmap, null, dst, null);
        canvas.restore();
    }

    private void drawGround(Canvas canvas) {
        if (groundTile == null) {
            return;
        }
        int groundY = screenHeight - TILE_SIZE;
        float offsetX = -(camera.getX() % TILE_SIZE);
        int tilesNeeded = (screenWidth / TILE_SIZE) + 2;

        for (int i = 0; i < tilesNeeded; i++) {
            float tileX = offsetX + (i * TILE_SIZE);
            canvas.drawBitmap(groundTile, null,
                    new Rect((int) tileX, groundY, (int) tileX + TILE_SIZE, groundY + TILE_SIZE),
                    null);
        }
    }

    private void drawPlayer(Canvas canvas) {
        Bitmap currentFrame = getCurrentFrame();
        if (currentFrame == null) {
            return;
        }
        int playerScreenX = screenWidth / 4;
        int playerY = screenHeight - TILE_SIZE - currentFrame.getHeight();
        canvas.drawBitmap(currentFrame, playerScreenX, playerY, null);
    }

    private Bitmap getCurrentFrame() {
        if (walkFrames != null && !walkFrames.isEmpty()) {
            return walkFrames.get(frameIndex);
        }
        return idleFrame;
    }

    public void update() {
        camera.update();

        frameTick++;
        if (frameTick >= TICKS_PER_FRAME) {
            frameTick = 0;
            if (walkFrames != null && !walkFrames.isEmpty()) {
                frameIndex = (frameIndex + 1) % walkFrames.size();
            }
        }
    }

    public Camera getCamera() {
        return camera;
    }
}
