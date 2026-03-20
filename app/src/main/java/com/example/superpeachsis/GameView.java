package com.example.superpeachsis;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.example.superpeachsis.domain.model.Block;
import com.example.superpeachsis.utils.SpriteManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
        thread = new GameThread(getHolder(), this);
        setFocusable(true);
        loadSprites();
    }

    private void loadSprites() {
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
        thread.setRunning(true);
        thread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
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
        backgroundBitmap = spriteManager.loadBitmap("backgrounds/" + randomBgFile);

        if (backgroundBitmap != null) {
            Bitmap bg = Bitmap.createScaledBitmap(backgroundBitmap, getWidth(), getHeight(), true);
            canvas.drawBitmap(bg, 0, 0, null);
        }

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
}
