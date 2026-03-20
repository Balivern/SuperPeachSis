package com.example.superpeachsis.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.example.superpeachsis.MainActivity;
import com.example.superpeachsis.domain.service.ScoreManager;

import java.io.IOException;
import java.io.InputStream;

public class GameOverActivity extends Activity {

    public static final String EXTRA_DISTANCE = "distance";
    public static final String EXTRA_COINS    = "coins";

    private GameOverView gameOverView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        int distance = 0;
        int coins = 0;
        if (getIntent() != null) {
            distance = getIntent().getIntExtra(EXTRA_DISTANCE, 0);
            coins    = getIntent().getIntExtra(EXTRA_COINS, 0);
        }
        int score = distance + coins;

        // Save score defensively
        try {
            new ScoreManager(this).saveScore(score);
        } catch (Exception e) {
            e.printStackTrace();
        }

        gameOverView = new GameOverView(this, distance, coins, score);
        setContentView(gameOverView);
    }

    @Override
    protected void onDestroy() {
        if (gameOverView != null) {
            gameOverView.cleanup();
        }
        super.onDestroy();
    }

    class GameOverView extends View {

        private final int distance;
        private final int coins;
        private final int score;
        private final int bestScore;

        private Bitmap bgBitmap;
        private Bitmap characterHit;
        private Bitmap blockRejouer;
        private Bitmap blockMenu;

        private Paint titlePaint;
        private Paint titleOutlinePaint;
        private Paint scorePaint;
        private Paint btnTextPaint;
        private Paint loadingPaint;
        private Paint overlayPaint;

        private RectF btnRejouer;
        private RectF btnMenu;

        private volatile boolean assetsLoaded = false;
        private boolean navigating = false;
        private final Handler mainHandler = new Handler(Looper.getMainLooper());

        public GameOverView(Context context, int distance, int coins, int score) {
            super(context);
            this.distance  = distance;
            this.coins     = coins;
            this.score     = score;
            
            int bScore = 0;
            try {
                bScore = new ScoreManager(context).getBestScore();
            } catch (Exception ignored) {}
            this.bestScore = bScore;

            setupPaints();
            loadAssetsAsync();
        }

        private void setupPaints() {
            titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            titlePaint.setColor(Color.WHITE);
            titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            titlePaint.setTextAlign(Paint.Align.CENTER);
            titlePaint.setShadowLayer(10f, 5f, 5f, Color.parseColor("#80000000"));

            titleOutlinePaint = new Paint(titlePaint);
            titleOutlinePaint.setColor(Color.parseColor("#8B0000"));
            titleOutlinePaint.setStyle(Paint.Style.STROKE);
            titleOutlinePaint.setStrokeWidth(10f);
            titleOutlinePaint.clearShadowLayer();

            scorePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            scorePaint.setColor(Color.WHITE);
            scorePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            scorePaint.setTextAlign(Paint.Align.CENTER);
            scorePaint.setShadowLayer(6f, 3f, 3f, Color.BLACK);

            btnTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            btnTextPaint.setColor(Color.WHITE);
            btnTextPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            btnTextPaint.setTextAlign(Paint.Align.CENTER);
            btnTextPaint.setShadowLayer(5f, 2f, 2f, Color.BLACK);

            loadingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            loadingPaint.setColor(Color.WHITE);
            loadingPaint.setTextSize(60f);
            loadingPaint.setTextAlign(Paint.Align.CENTER);
            
            overlayPaint = new Paint();
            overlayPaint.setColor(Color.parseColor("#99000000"));
        }

        private void loadAssetsAsync() {
            new Thread(() -> {
                AssetManager am = getContext().getAssets();
                bgBitmap      = safeLoadBitmap(am, "backgrounds/background_color_hills.png");
                characterHit  = safeLoadBitmap(am, "characters/character_pink_hit.png");
                blockRejouer  = safeLoadBitmap(am, "tiles/block_exclamation.png");
                blockMenu     = safeLoadBitmap(am, "tiles/block_coin.png");
                
                assetsLoaded = true;
                mainHandler.post(this::invalidate);
            }).start();
        }

        private Bitmap safeLoadBitmap(AssetManager am, String path) {
            try (InputStream is = am.open(path)) {
                return BitmapFactory.decodeStream(is);
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int w = getWidth();
            int h = getHeight();
            if (w == 0 || h == 0) return;

            if (!assetsLoaded || navigating) {
                canvas.drawColor(Color.BLACK);
                canvas.drawText("Chargement...", w / 2f, h / 2f + 20, loadingPaint);
                return;
            }

            // Draw Background
            if (bgBitmap != null) {
                canvas.drawBitmap(bgBitmap, null, new RectF(0, 0, w, h), null);
            } else {
                canvas.drawColor(Color.parseColor("#1a1a2e"));
            }

            canvas.drawRect(0, 0, w, h, overlayPaint);

            // Draw Title
            float titleSize = h * 0.12f;
            titlePaint.setTextSize(titleSize);
            titleOutlinePaint.setTextSize(titleSize);
            float titleY = h * 0.18f;
            canvas.drawText("GAME OVER", w / 2f, titleY, titleOutlinePaint);
            canvas.drawText("GAME OVER", w / 2f, titleY, titlePaint);

            // Draw Character
            float charH   = h * 0.18f;
            float charCx  = w / 2f;
            float charTop = h * 0.22f;
            if (characterHit != null) {
                canvas.drawBitmap(characterHit, null, new RectF(
                        charCx - charH / 2f, charTop,
                        charCx + charH / 2f, charTop + charH
                ), null);
            }

            // Draw Scores
            float scoreSize = h * 0.045f;
            scorePaint.setTextSize(scoreSize);
            float lineH = scoreSize * 1.5f;
            float scoreY = h * 0.48f;

            canvas.drawText("Distance : " + distance + " m",  w / 2f, scoreY,          scorePaint);
            canvas.drawText("Pièces   : " + coins,            w / 2f, scoreY + lineH,   scorePaint);
            canvas.drawText("Score Total : " + score,         w / 2f, scoreY + lineH * 2.2f, scorePaint);
            
            scorePaint.setColor(Color.YELLOW);
            canvas.drawText("Meilleur : " + bestScore,        w / 2f, scoreY + lineH * 3.4f, scorePaint);
            scorePaint.setColor(Color.WHITE);

            // Draw Buttons
            float btnW  = w * 0.38f;
            float btnH  = h * 0.11f;
            float btnY  = h * 0.75f;
            float gap   = w * 0.08f;

            btnTextPaint.setTextSize(btnH * 0.35f);

            btnRejouer = new RectF(w / 2f - btnW - gap / 2f, btnY, w / 2f - gap / 2f, btnY + btnH);
            btnMenu    = new RectF(w / 2f + gap / 2f,        btnY, w / 2f + btnW + gap / 2f, btnY + btnH);

            drawBlockButton(canvas, blockRejouer, btnRejouer, "REJOUER");
            drawBlockButton(canvas, blockMenu,    btnMenu,    "MENU");
        }

        private void drawBlockButton(Canvas canvas, Bitmap blockBmp, RectF rect, String label) {
            if (blockBmp != null) {
                float tileSize = rect.height();
                int numTiles = (int) Math.ceil(rect.width() / tileSize);
                for (int i = 0; i < numTiles; i++) {
                    float left  = rect.left + i * tileSize;
                    float right = Math.min(left + tileSize, rect.right);
                    canvas.drawBitmap(blockBmp, null, new RectF(left, rect.top, right, rect.bottom), null);
                }
            } else {
                Paint fallback = new Paint(Paint.ANTI_ALIAS_FLAG);
                fallback.setColor(Color.parseColor("#C84B11"));
                canvas.drawRoundRect(rect, 20f, 20f, fallback);
            }

            float textY = rect.centerY() + btnTextPaint.getTextSize() * 0.35f;
            canvas.drawText(label, rect.centerX(), textY, btnTextPaint);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_UP && !navigating) {
                float x = event.getX();
                float y = event.getY();

                if (btnRejouer != null && btnRejouer.contains(x, y)) {
                    startNavigation(MainActivity.class);
                    return true;
                } else if (btnMenu != null && btnMenu.contains(x, y)) {
                    startNavigation(MenuActivity.class);
                    return true;
                }
            }
            return true;
        }

        private void startNavigation(Class<?> activityClass) {
            navigating = true;
            invalidate();
            mainHandler.postDelayed(() -> {
                Context context = getContext();
                if (context instanceof Activity) {
                    Activity activity = (Activity) context;
                    if (!activity.isFinishing()) {
                        Intent intent = new Intent(activity, activityClass);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        activity.startActivity(intent);
                        activity.finish();
                    }
                }
            }, 100);
        }

        public void cleanup() {
            // Clean up bitmaps if needed, though GC handles it, it's safer
            assetsLoaded = false;
        }
    }
}
