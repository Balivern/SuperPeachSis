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

    private List<Bitmap> walkFrames;
    private Bitmap idleFrame;
    private Bitmap backgroundBitmap;
    private Bitmap groundTile;

    private static final int TILE_SIZE = 64;
    private static final float PARALLAX_FACTOR = 0.3f;

    private int screenWidth;
    private int screenHeight;
    private int playerX = 200;
    private int playerY = 400;
    private String[] backgroundFiles = {
            "background_clouds.png",
            "background_solid_sky.png",
            "background_fade_hills.png",
            "background_fade_trees.png",
            "background_color_hills.png",
            "background_color_trees.png",
            "background_fade_desert.png",
            "background_color_desert.png",
            "background_fade_mushrooms.png",
            "background_color_mushrooms.png"
    };
    private String randomBgFile;

    private List<Block> blockPool = new ArrayList<>();
    private List<Bitmap> blockBitmaps = new ArrayList<>();
    private Random random = new Random();
    private int nextSpawnTick = 0;
    private final int POOL_SIZE = 10; // Nombre de blocs pré-alloués
    private final int GAME_SPEED = 10; // Vitesse de défilement

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
        walkFrames = spriteManager.getCharacterFrames("pink", "walk_a", "walk_b");
        idleFrame = spriteManager.loadBitmap("characters/character_pink_idle.png");

        // Choisir un fond aléatoire
        randomBgFile = backgroundFiles[random.nextInt(backgroundFiles.length)];
        backgroundBitmap = spriteManager.getBackground(randomBgFile);

        // Charger quelques variantes de blocs
        blockBitmaps.add(spriteManager.loadBitmap("tiles/block_red.png"));
        blockBitmaps.add(spriteManager.loadBitmap("tiles/block_blue.png"));
        blockBitmaps.add(spriteManager.loadBitmap("tiles/block_green.png"));
        blockBitmaps.add(spriteManager.loadBitmap("tiles/block_yellow.png"));

        // Initialiser le pool de blocs (Recyclage)
        for (int i = 0; i < POOL_SIZE; i++) {
            blockPool.add(new Block());
        }
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
        if (backgroundBitmap != null) {
            // Redimensionner le fond une seule fois pour toute la partie
            backgroundBitmap = Bitmap.createScaledBitmap(backgroundBitmap, width, height, true);
        }
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
        backgroundBitmap = spriteManager.loadBitmap("backgrounds/" + randomBgFile);

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
        Bitmap currentFrame = getCurrentFrame();
        if (currentFrame != null) {
            canvas.drawBitmap(currentFrame, playerX, playerY, null);
        }

        for (Block block : blockPool) {
            if (block.active && block.bitmap != null) {
                canvas.drawBitmap(block.bitmap, block.x, block.y, null);
            }
        }
    }

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

        // 1. Gérer le spawn aléatoire
        nextSpawnTick--;
        if (nextSpawnTick <= 0) {
            spawnBlock();
            // Définit le prochain spawn entre 1 et 3 secondes (60-180 ticks à 60 FPS)
            nextSpawnTick = 60 + random.nextInt(120);
        }

        // 2. Mettre à jour les blocs actifs
        for (Block block : blockPool) {
            block.update(GAME_SPEED);
        }
    }

    private void spawnBlock() {
        for (Block block : blockPool) {
            if (!block.active) {
                Bitmap randomBitmap = blockBitmaps.get(random.nextInt(blockBitmaps.size()));
                // On le place à droite de l'écran, au niveau du sol (playerY)
                block.spawn(randomBitmap, getWidth(), playerY);
                break; // On n'en active qu'un seul à la fois
            }
        }
    }

    public Camera getCamera() {
        return camera;
    }
}
