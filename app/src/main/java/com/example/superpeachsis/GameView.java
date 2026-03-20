package com.example.superpeachsis;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.example.superpeachsis.domain.model.Block;
import com.example.superpeachsis.domain.model.Enemy;
import com.example.superpeachsis.domain.model.Player;
import com.example.superpeachsis.domain.service.HUD;
import com.example.superpeachsis.ui.GameOverActivity;
import com.example.superpeachsis.ui.MenuActivity;
import com.example.superpeachsis.utils.Camera;
import com.example.superpeachsis.utils.CollisionManager;
import com.example.superpeachsis.utils.SpriteManager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class GameView extends SurfaceView implements SurfaceHolder.Callback, SensorEventListener {

    private GameThread thread;
    private SpriteManager spriteManager;
    private Camera camera;
    private Player player;
    private final HUD hud;

    private SensorManager sensorManager;
    private Sensor lightSensor;

    private Bitmap backgroundBitmap;
    private Bitmap groundTile;

    private static final int TILE_SIZE = 64;
    private static final int PLAYER_HEIGHT = 128;
    private static final float PARALLAX_FACTOR = 0.3f;
    private static final int POOL_SIZE = 30;
    private static final int BASE_GAME_SPEED = 10;
    private static final int ENEMY_POOL_SIZE = 10;
    private static final long TRAIL_LIFETIME = 300;
    private static final int MIN_SPAWN_DELAY = 20;
    private static final int BASE_SPAWN_DELAY = 50;
    private static final int SPAWN_RANGE = 60;

    private int screenWidth;
    private int screenHeight;
    private int gameSpeed = BASE_GAME_SPEED;
    private int tickCount = 0;

    private final String[] backgroundFiles = {
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

    private final List<Block> blockPool = new ArrayList<>();
    private final List<Bitmap> blockBitmaps = new ArrayList<>();
    private final List<Enemy> enemyPool = new ArrayList<>();
    private final List<Bitmap> sawFrames = new ArrayList<>();
    private Bitmap sawDeathBitmap;
    private final Random random = new Random();
    private int nextSpawnTick = 30;
    private int lives = 3;
    private int coins = 0;
    private boolean gameOver = false;

    private final Rect bgRect = new Rect();
    private final Rect tileRect = new Rect();
    private final Rect enemyRect = new Rect();
    private final Rect touchRect = new Rect();

    private Paint trailPaint;
    private final List<TrailPoint> trailPoints = new ArrayList<>();

    private static class TrailPoint {
        float x, y;
        long timestamp;
        TrailPoint(float x, float y) {
            this.x = x;
            this.y = y;
            this.timestamp = System.currentTimeMillis();
        }
    }

    public GameView(Context context) {
        super(context);
        getHolder().addCallback(this);
        spriteManager = SpriteManager.getInstance(context);
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        camera = new Camera(5f);
        thread = new GameThread(getHolder(), this);
        hud = new HUD(spriteManager);
        hud.setListener(() -> {
            Context ctx = getContext();
            Intent intent = new Intent(ctx, MenuActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            ctx.startActivity(intent);
        });
        setFocusable(true);
        loadSprites();
        player = new Player(200, 400, spriteManager);
        trailPaint = new Paint();
        trailPaint.setColor(Color.RED);
        trailPaint.setStrokeWidth(8f);
        trailPaint.setStyle(Paint.Style.STROKE);
        trailPaint.setStrokeJoin(Paint.Join.ROUND);
        trailPaint.setStrokeCap(Paint.Cap.ROUND);
        trailPaint.setAntiAlias(true);
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

        sawFrames.add(spriteManager.loadBitmap("enemies/saw_a.png"));
        sawFrames.add(spriteManager.loadBitmap("enemies/saw_b.png"));
        sawDeathBitmap = spriteManager.loadBitmap("enemies/saw_rest.png");

        for (int i = 0; i < ENEMY_POOL_SIZE; i++) {
            enemyPool.add(new Enemy());
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        screenWidth = getWidth();
        screenHeight = getHeight();

        if (lightSensor != null) {
            sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_GAME);
        }

        if (player != null) {
            player.setX(screenWidth / 4f);
            player.setY(screenHeight - TILE_SIZE - PLAYER_HEIGHT);
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
        sensorManager.unregisterListener(this);
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
        drawEnemies(canvas);
        drawPlayer(canvas);
        drawTrail(canvas);
        hud.draw(canvas, screenWidth, screenHeight);
    }

    private void drawBackground(Canvas canvas) {
        if (backgroundBitmap == null) {
            return;
        }
        float parallaxX = -(camera.getX() * PARALLAX_FACTOR) % screenWidth;
        bgRect.set(0, 0, screenWidth, screenHeight);

        canvas.save();
        canvas.translate(parallaxX, 0);
        canvas.drawBitmap(backgroundBitmap, null, bgRect, null);
        canvas.translate(screenWidth, 0);
        canvas.drawBitmap(backgroundBitmap, null, bgRect, null);
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
            int tileX = (int) (offsetX + (i * TILE_SIZE));
            tileRect.set(tileX, groundY, tileX + TILE_SIZE, groundY + TILE_SIZE);
            canvas.drawBitmap(groundTile, null, tileRect, null);
        }
    }

    private void drawBlocks(Canvas canvas) {
        for (int i = 0; i < blockPool.size(); i++) {
            Block block = blockPool.get(i);
            if (block.active && block.bitmap != null) {
                canvas.drawBitmap(block.bitmap, block.x, block.y, null);
            }
        }
    }

    private void drawEnemies(Canvas canvas) {
        for (int i = 0; i < enemyPool.size(); i++) {
            Enemy enemy = enemyPool.get(i);
            if (enemy.active) {
                canvas.drawBitmap(enemy.getCurrentBitmap(), enemy.x, enemy.y, null);
            }
        }
    }

    private void drawPlayer(Canvas canvas) {
        if (player != null) {
            player.draw(canvas);
        }
    }

    private void drawTrail(Canvas canvas) {
        if (trailPoints.size() < 2) return;

        for (int i = 0; i < trailPoints.size() - 1; i++) {
            TrailPoint p1 = trailPoints.get(i);
            TrailPoint p2 = trailPoints.get(i + 1);

            long age = System.currentTimeMillis() - p1.timestamp;
            int alpha = (int) (255 * (1 - (float) age / TRAIL_LIFETIME));
            if (alpha < 0) alpha = 0;

            trailPaint.setAlpha(alpha);
            canvas.drawLine(p1.x, p1.y, p2.x, p2.y, trailPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();

        if (action == MotionEvent.ACTION_MOVE || action == MotionEvent.ACTION_DOWN) {
            trailPoints.add(new TrailPoint(event.getX(), event.getY()));
            checkBlockDestruction(event.getX(), event.getY());
        }

        if (action == MotionEvent.ACTION_UP) {
            hud.onTouch(event.getX(), event.getY());
        }

        return true;
    }

    private void checkBlockDestruction(float tx, float ty) {
        for (int i = 0; i < blockPool.size(); i++) {
            Block block = blockPool.get(i);
            if (block.active && block.bitmap != null) {
                touchRect.set(block.x, block.y,
                        block.x + block.bitmap.getWidth(), block.y + block.bitmap.getHeight());
                if (touchRect.contains((int) tx, (int) ty)) {
                    block.active = false;
                    coins++;
                }
            }
        }
    }

    public HUD getHud() { return hud; }

    public void update() {
        if (gameOver) {
            return;
        }

        tickCount++;
        updateDifficulty();
        hud.update();
        if (hud.isPaused()) return;

        camera.update();
        hud.setDistance((int) (camera.getX() / 10));

        if (player != null) {
            player.update();

            int groundY = screenHeight - TILE_SIZE - PLAYER_HEIGHT;
            if (player.getY() > groundY) {
                player.setY(groundY);
                player.setVy(0);
                if (player.getState() == Player.State.JUMPING) {
                    player.setState(Player.State.RUNNING);
                }
            }

            checkBlockCollisions();
            checkEnemyCollisions();
        }

        nextSpawnTick--;
        if (nextSpawnTick <= 0) {
            spawnObstacle();
            int delay = Math.max(MIN_SPAWN_DELAY, BASE_SPAWN_DELAY - (tickCount / 300));
            nextSpawnTick = delay + random.nextInt(Math.max(10, SPAWN_RANGE - (tickCount / 400)));
        }

        for (int i = 0; i < blockPool.size(); i++) {
            blockPool.get(i).update(gameSpeed);
        }

        for (int i = 0; i < enemyPool.size(); i++) {
            enemyPool.get(i).update(gameSpeed);
        }

        long now = System.currentTimeMillis();
        Iterator<TrailPoint> it = trailPoints.iterator();
        while (it.hasNext()) {
            if (now - it.next().timestamp > TRAIL_LIFETIME) {
                it.remove();
            }
        }
    }

    private void updateDifficulty() {
        if (tickCount % 300 == 0 && tickCount > 0) {
            int level = tickCount / 300;
            gameSpeed = Math.min(BASE_GAME_SPEED + level * 2, 22);
            camera.setSpeed(5f + level * 0.8f);
        }
    }

    private void spawnObstacle() {
        int roll = random.nextInt(100);

        if (roll < 40) {
            spawnBlock();
        } else if (roll < 70) {
            spawnEnemy();
        } else if (roll < 85) {
            spawnBlock();
            spawnEnemy();
        } else {
            spawnBlock();
            spawnBlockOffset(40);
        }
    }

    private void spawnBlock() {
        for (int i = 0; i < blockPool.size(); i++) {
            Block block = blockPool.get(i);
            if (!block.active) {
                Bitmap randomBitmap = blockBitmaps.get(random.nextInt(blockBitmaps.size()));
                int blockY = screenHeight - TILE_SIZE - randomBitmap.getHeight();
                block.spawn(randomBitmap, screenWidth, blockY);
                return;
            }
        }
    }

    private void spawnBlockOffset(int offset) {
        for (int i = 0; i < blockPool.size(); i++) {
            Block block = blockPool.get(i);
            if (!block.active) {
                Bitmap randomBitmap = blockBitmaps.get(random.nextInt(blockBitmaps.size()));
                int blockY = screenHeight - TILE_SIZE - randomBitmap.getHeight();
                block.spawn(randomBitmap, screenWidth + offset, blockY);
                return;
            }
        }
    }

    private void spawnEnemy() {
        for (int i = 0; i < enemyPool.size(); i++) {
            Enemy enemy = enemyPool.get(i);
            if (!enemy.active) {
                int groundY = screenHeight - TILE_SIZE - 64;
                enemy.spawn(sawFrames, sawDeathBitmap, screenWidth, groundY);
                return;
            }
        }
    }

    private void checkBlockCollisions() {
        Rect playerRect = player.getRect();

        for (int i = 0; i < blockPool.size(); i++) {
            Block block = blockPool.get(i);
            if (!block.active) {
                continue;
            }

            CollisionManager.Side side = CollisionManager.getCollisionSide(playerRect, block.getRect());

            switch (side) {
                case TOP:
                    player.setY(block.getRect().top - playerRect.height());
                    player.setVy(0);
                    coins++;
                    block.active = false;
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

    private void checkEnemyCollisions() {
        Rect playerRect = player.getRect();

        for (int i = 0; i < enemyPool.size(); i++) {
            Enemy enemy = enemyPool.get(i);
            if (!enemy.active || enemy.isDead) {
                continue;
            }

            Bitmap enemyBmp = enemy.getCurrentBitmap();
            enemyRect.set(enemy.x, enemy.y,
                    enemy.x + enemyBmp.getWidth(), enemy.y + enemyBmp.getHeight());

            CollisionManager.Side side = CollisionManager.getCollisionSide(playerRect, enemyRect);

            switch (side) {
                case TOP:
                    player.setVy(-10f);
                    enemy.kill();
                    coins++;
                    break;
                case LEFT:
                case RIGHT:
                    loseLife();
                    enemy.kill();
                    break;
                default:
                    break;
            }
        }
    }

    private void loseLife() {
        lives--;
        hud.setLives(lives);
        if (lives <= 0) {
            gameOver = true;
            launchGameOver();
        }
    }

    private void launchGameOver() {
        post(() -> {
            Context ctx = getContext();
            if (ctx instanceof Activity && ((Activity) ctx).isFinishing()) return;
            Intent intent = new Intent(ctx, GameOverActivity.class);
            intent.putExtra(GameOverActivity.EXTRA_DISTANCE, (int) (camera.getX() / 10));
            intent.putExtra(GameOverActivity.EXTRA_COINS, coins);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            ctx.startActivity(intent);
        });
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            float lux = event.values[0];
            if (lux < 10) {
                killAllEnemies();
            }
        }
    }

    private void killAllEnemies() {
        for (int i = 0; i < enemyPool.size(); i++) {
            Enemy enemy = enemyPool.get(i);
            if (enemy.active && !enemy.isDead) {
                enemy.kill();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public int getLives() {
        return lives;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public Camera getCamera() {
        return camera;
    }

    public void triggerJump() {
        if (player != null) {
            player.jump();
        }
    }
}
