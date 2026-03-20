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
import com.example.superpeachsis.domain.model.Fence;
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
    private static final int BASE_GAME_SPEED = 6;
    private static final int ENEMY_POOL_SIZE = 5;

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
    private final List<Fence> fencePool = new ArrayList<>();
    private Bitmap fenceBitmap;
    private Bitmap fenceBrokenBitmap;
    private final List<Enemy> enemyPool = new ArrayList<>();

    // Drawing mode state
    private boolean drawingMode = false;
    private Fence targetFence = null;
    private final List<TrailPoint> currentStroke = new ArrayList<>();
    private final List<List<TrailPoint>> allStrokes = new ArrayList<>();
    private Paint drawingPaint;
    private Paint overlayPaint;
    private boolean drawingFailure = false;

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

        drawingPaint = new Paint();
        drawingPaint.setColor(Color.WHITE);
        drawingPaint.setStrokeWidth(12f);
        drawingPaint.setStyle(Paint.Style.STROKE);
        drawingPaint.setStrokeJoin(Paint.Join.ROUND);
        drawingPaint.setStrokeCap(Paint.Cap.ROUND);
        drawingPaint.setAntiAlias(true);

        overlayPaint = new Paint();
        overlayPaint.setColor(Color.BLACK);
        overlayPaint.setAlpha(40); // Very light overlay
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

        fenceBitmap = spriteManager.loadBitmap("tiles/fence.png");
        fenceBrokenBitmap = spriteManager.loadBitmap("tiles/fence_broken.png");
        for (int i = 0; i < 5; i++) {
            fencePool.add(new Fence());
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
        drawFences(canvas);
        drawEnemies(canvas);
        drawPlayer(canvas);

        if (drawingMode) {
            drawDrawingOverlay(canvas);
        }

        hud.draw(canvas, screenWidth, screenHeight);
    }

    private void drawDrawingOverlay(Canvas canvas) {
        canvas.drawRect(0, 0, screenWidth, screenHeight, overlayPaint);

        // Draw pattern to recognize hint
        Paint hintPaint = new Paint();
        hintPaint.setColor(Color.WHITE);
        hintPaint.setTextSize(60);
        hintPaint.setTextAlign(Paint.Align.CENTER);
        if (targetFence != null) {
            canvas.drawText("Dessinez : " + targetFence.expectedPattern.name(), screenWidth / 2f, 150, hintPaint);
        }

        drawingPaint.setColor(drawingFailure ? Color.RED : Color.WHITE);

        // Draw current stroke
        drawStroke(canvas, currentStroke);

        // Draw previous strokes
        for (List<TrailPoint> stroke : allStrokes) {
            drawStroke(canvas, stroke);
        }
    }

    private void drawStroke(Canvas canvas, List<TrailPoint> stroke) {
        if (stroke.size() < 2) return;
        for (int i = 0; i < stroke.size() - 1; i++) {
            TrailPoint p1 = stroke.get(i);
            TrailPoint p2 = stroke.get(i + 1);
            canvas.drawLine(p1.x, p1.y, p2.x, p2.y, drawingPaint);
        }
    }

    private void drawFences(Canvas canvas) {
        for (int i = 0; i < fencePool.size(); i++) {
            Fence fence = fencePool.get(i);
            if (fence.active && fence.bitmap != null) {
                canvas.drawBitmap(fence.bitmap, fence.x, fence.y, null);
            }
        }
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

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        float x = event.getX();
        float y = event.getY();

        if (drawingMode) {
            handleDrawingInput(action, x, y);
            return true;
        }

        if (action == MotionEvent.ACTION_UP) {
            hud.onTouch(x, y);
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
    private void handleDrawingInput(int action, float x, float y) {
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                currentStroke.clear();
                currentStroke.add(new TrailPoint(x, y));
                drawingFailure = false;
                break;
            case MotionEvent.ACTION_MOVE:
                currentStroke.add(new TrailPoint(x, y));
                break;
            case MotionEvent.ACTION_UP:
                allStrokes.add(new ArrayList<>(currentStroke));
                checkPattern();
                currentStroke.clear();
                break;
        }
    }

    private void checkPattern() {
        if (targetFence == null) return;

        boolean success = false;
        switch (targetFence.expectedPattern) {
            case CIRCLE:
                success = isCircle(allStrokes.get(allStrokes.size() - 1));
                break;
            case LINE:
                success = isHorizontalLine(allStrokes.get(allStrokes.size() - 1));
                break;
            case CROSS:
                if (allStrokes.size() >= 2) {
                    success = isCross(allStrokes.get(allStrokes.size() - 2), allStrokes.get(allStrokes.size() - 1));
                }
                break;
        }

        if (success) {
            targetFence.breakFence();
            drawingMode = false;
            targetFence = null;
            allStrokes.clear();
        } else {
            // For circle and line, if failed on one stroke, it's a failure.
            // For cross, we might need to wait for the second stroke.
            if (targetFence.expectedPattern != Fence.Pattern.CROSS || allStrokes.size() >= 2) {
                drawingFailure = true;
                allStrokes.clear();
            }
        }
    }

    private boolean isCircle(List<TrailPoint> points) {
        if (points.size() < 10) return false;
        TrailPoint start = points.get(0);
        TrailPoint end = points.get(points.size() - 1);
        float dist = (float) Math.hypot(start.x - end.x, start.y - end.y);

        // End near start
        if (dist > 200) return false;

        float minX = Float.MAX_VALUE, maxX = Float.MIN_VALUE;
        float minY = Float.MAX_VALUE, maxY = Float.MIN_VALUE;
        for (TrailPoint p : points) {
            if (p.x < minX) minX = p.x;
            if (p.x > maxX) maxX = p.x;
            if (p.y < minY) minY = p.y;
            if (p.y > maxY) maxY = p.y;
        }

        float width = maxX - minX;
        float height = maxY - minY;

        // Significant size
        if (width < 100 || height < 100) return false;

        // Aspect ratio
        float ratio = width / height;
        return ratio > 0.5f && ratio < 2.0f;
    }

    private boolean isHorizontalLine(List<TrailPoint> points) {
        if (points.size() < 5) return false;
        float minX = Float.MAX_VALUE, maxX = Float.MIN_VALUE;
        float minY = Float.MAX_VALUE, maxY = Float.MIN_VALUE;
        for (TrailPoint p : points) {
            if (p.x < minX) minX = p.x;
            if (p.x > maxX) maxX = p.x;
            if (p.y < minY) minY = p.y;
            if (p.y > maxY) maxY = p.y;
        }
        float width = maxX - minX;
        float height = maxY - minY;

        return width > 200 && height < 100;
    }

    private boolean isCross(List<TrailPoint> s1, List<TrailPoint> s2) {
        if (s1.size() < 5 || s2.size() < 5) return false;

        // Simple intersection check of bounding boxes first
        Rect r1 = getBoundingBox(s1);
        Rect r2 = getBoundingBox(s2);

        if (!Rect.intersects(r1, r2)) return false;

        // Check if both are somewhat "diagonal" or just "straight" lines
        // For simplicity, if they intersect and have significant size, we call it a cross
        return r1.width() > 100 && r1.height() > 100 && r2.width() > 100 && r2.height() > 100;
    }

    private Rect getBoundingBox(List<TrailPoint> points) {
        float minX = Float.MAX_VALUE, maxX = Float.MIN_VALUE;
        float minY = Float.MAX_VALUE, maxY = Float.MIN_VALUE;
        for (TrailPoint p : points) {
            if (p.x < minX) minX = p.x;
            if (p.x > maxX) maxX = p.x;
            if (p.y < minY) minY = p.y;
            if (p.y > maxY) maxY = p.y;
        }
        return new Rect((int)minX, (int)minY, (int)maxX, (int)maxY);
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
            checkFenceCollisions();
            checkEnemyCollisions();
        }

        // Deactivate drawing mode if target is gone or broken
        if (drawingMode && targetFence != null) {
            if (!targetFence.active || targetFence.broken || targetFence.x + TILE_SIZE < player.getX()) {
                drawingMode = false;
                targetFence = null;
                allStrokes.clear();
            }
        }

        nextSpawnTick--;
        if (nextSpawnTick <= 0) {
            spawnObstacle();
            int delay = Math.max(MIN_SPAWN_DELAY, BASE_SPAWN_DELAY - (tickCount / 300));
            nextSpawnTick = delay + random.nextInt(Math.max(10, SPAWN_RANGE - (tickCount / 400)));
            int spawnType = random.nextInt(3);
            if (spawnType == 0) {
                spawnBlock();
            } else if (spawnType == 1) {
                spawnFence();
            } else {
                spawnEnemy();
            }
            nextSpawnTick = 60 + random.nextInt(120);
        }

        for (int i = 0; i < blockPool.size(); i++) {
            blockPool.get(i).update(gameSpeed);
        }

        for (int i = 0; i < fencePool.size(); i++) {
            fencePool.get(i).update(gameSpeed);
        }

        for (int i = 0; i < enemyPool.size(); i++) {
            enemyPool.get(i).update(gameSpeed);
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

    private void spawnFence() {
        for (int i = 0; i < fencePool.size(); i++) {
            Fence fence = fencePool.get(i);
            if (!fence.active) {
                int fenceY = screenHeight - TILE_SIZE - fenceBitmap.getHeight();
                Fence.Pattern pattern = Fence.Pattern.values()[random.nextInt(Fence.Pattern.values().length)];
                fence.spawn(fenceBitmap, fenceBrokenBitmap, screenWidth, fenceY, pattern);
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

    private void checkFenceCollisions() {
        Rect playerRect = player.getRect();

        for (int i = 0; i < fencePool.size(); i++) {
            Fence fence = fencePool.get(i);
            if (!fence.active || fence.broken) {
                continue;
            }

            Rect fenceRect = fence.getRect();

            // Activate drawing mode if approaching
            if (playerRect.right < fenceRect.left && fenceRect.left - playerRect.right < 600) {
                if (!drawingMode) {
                    startDrawingMode(fence);
                }
            }

            CollisionManager.Side side = CollisionManager.getCollisionSide(playerRect, fenceRect);
            if (side == CollisionManager.Side.LEFT || side == CollisionManager.Side.RIGHT) {
                loseLife();
                fence.active = false;
                drawingMode = false;
            } else if (side == CollisionManager.Side.TOP) {
                player.setY(fenceRect.top - playerRect.height());
                player.setVy(0);
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

    private void startDrawingMode(Fence fence) {
        drawingMode = true;
        targetFence = fence;
        allStrokes.clear();
        currentStroke.clear();
        drawingFailure = false;
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
