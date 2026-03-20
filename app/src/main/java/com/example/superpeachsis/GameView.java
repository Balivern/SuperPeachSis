package com.example.superpeachsis;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.example.superpeachsis.utils.Camera;
import com.example.superpeachsis.utils.CollisionManager;
import com.example.superpeachsis.domain.model.Player;
import com.example.superpeachsis.domain.model.Block;
import com.example.superpeachsis.utils.SpriteManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {

    private GameThread thread;
    private SpriteManager spriteManager;
    private Camera camera;
    private Player player;

    private Bitmap backgroundBitmap;
    private Bitmap groundTile;

    private static final int TILE_SIZE = 64;
    private static final int PLAYER_HEIGHT = 128;
    private static final float PARALLAX_FACTOR = 0.3f;

    private int screenWidth;
    private int screenHeight;

    private String[] backgroundFiles = {
            "background_clouds",
            "background_solid_sky",
            "background_fade_hills",
            "background_fade_trees",
            "background_color_hills",
            "background_color_trees",
            "background_fade_desert",
            "background_color_desert",
            "background_fade_mushrooms",
            "background_color_mushrooms"
    };

    private List<Block> blockPool = new ArrayList<>();
    private List<Bitmap> blockBitmaps = new ArrayList<>();
    private Random random = new Random();
    private int nextSpawnTick = 0;
    private final int POOL_SIZE = 10;
    private final int GAME_SPEED = 10;
    private int lives = 3;
    private boolean gameOver = false;

    public GameView(Context context) {
        super(context);
        getHolder().addCallback(this);
        spriteManager = SpriteManager.getInstance(context);
        camera = new Camera(3f);
        thread = new GameThread(getHolder(), this);
        setFocusable(true);
        loadSprites();
        player = new Player(200, 400, spriteManager);
    }

    private void loadSprites() {
        String randomBg = backgroundFiles[random.nextInt(backgroundFiles.length)];
        backgroundBitmap = spriteManager.getBackground(randomBg);
        groundTile = spriteManager.getTile("terrain_grass_block_top");

        blockBitmaps.add(spriteManager.loadBitmap("tiles/block_red.png"));
        blockBitmaps.add(spriteManager.loadBitmap("tiles/block_blue.png"));
        blockBitmaps.add(spriteManager.loadBitmap("tiles/block_green.png"));
        blockBitmaps.add(spriteManager.loadBitmap("tiles/block_yellow.png"));

        for (int i = 0; i < POOL_SIZE; i++) {
            blockPool.add(new Block());
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        screenWidth = getWidth();
        screenHeight = getHeight();

        if (player != null) {
            player.setY(screenHeight - TILE_SIZE - 64);
        }

        if (thread.getState() == Thread.State.TERMINATED) {
            thread = new GameThread(getHolder(), this);
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
        drawBlocks(canvas);
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

    private void drawBlocks(Canvas canvas) {
        for (Block block : blockPool) {
            if (block.active && block.bitmap != null) {
                canvas.drawBitmap(block.bitmap, block.x, block.y, null);
            }
        }
    }

    private void drawPlayer(Canvas canvas) {
        if (player != null) {
            player.draw(canvas);
        }
    }

    public void update() {
        if (gameOver) {
            return;
        }

        camera.update();

        if (player != null) {
            player.update();

            int groundY = screenHeight - TILE_SIZE - player.getRect().height();
            if (player.getY() > groundY) {
                player.setY(groundY);
                player.setVy(0);
            }

            checkBlockCollisions();
        }

        nextSpawnTick--;
        if (nextSpawnTick <= 0) {
            spawnBlock();
            nextSpawnTick = 60 + random.nextInt(120);
        }

        for (Block block : blockPool) {
            block.update(GAME_SPEED);
        }
    }

    private void checkBlockCollisions() {
        android.graphics.Rect playerRect = player.getRect();

        for (Block block : blockPool) {
            if (!block.active) {
                continue;
            }

            CollisionManager.Side side = CollisionManager.getCollisionSide(playerRect, block.getRect());

            switch (side) {
                case TOP:
                    player.setY(block.getRect().top - playerRect.height());
                    player.setVy(0);
                    break;
                case LEFT:
                case RIGHT:
                    loseLife();
                    block.active = false;
                    break;
                default:
                    break;
            }
        }
    }

    private void loseLife() {
        lives--;
        if (lives <= 0) {
            gameOver = true;
        }
    }

    public int getLives() {
        return lives;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    private void spawnBlock() {
        for (Block block : blockPool) {
            if (!block.active) {
                Bitmap randomBitmap = blockBitmaps.get(random.nextInt(blockBitmaps.size()));
                int blockY = screenHeight - TILE_SIZE - randomBitmap.getHeight();
                block.spawn(randomBitmap, getWidth(), blockY);
                break;
            }
        }
    }

    public Camera getCamera() {
        return camera;
    }
}
