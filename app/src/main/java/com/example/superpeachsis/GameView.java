package com.example.superpeachsis;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.example.superpeachsis.utils.Camera;
import com.example.superpeachsis.domain.model.Player;
import com.example.superpeachsis.utils.SpriteManager;

import java.util.List;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {

    private GameThread thread;
    private SpriteManager spriteManager;
    private Camera camera;
    private Player player;

    private Bitmap backgroundBitmap;
    private Bitmap groundTile;

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
        // Initialize player position. Y will be updated once screenHeight is known.
        player = new Player(200, 400, spriteManager);
    }

    private void loadSprites() {
        backgroundBitmap = spriteManager.getBackground("background_color_trees");
        groundTile = spriteManager.getTile("terrain_grass_block_top");
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        screenWidth = getWidth();
        screenHeight = getHeight();

        // Adjust player Y to be on the ground
        if (player != null) {
            player.setY(screenHeight - TILE_SIZE - 64); // Assuming 64 is player height approximately
        }

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
        if (player != null) {
            player.draw(canvas);
        }
    }

    public void update() {
        camera.update();

        if (player != null) {
            player.update();

            // Ground collision (simple for now)
            int groundY = screenHeight - TILE_SIZE - 64; // assuming player height 64
            if (player.getY() > groundY) {
                player.setY(groundY);
                player.setVy(0);
            }
        }
    }

    public Camera getCamera() {
        return camera;
    }
}
