package com.example.superpeachsis.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.example.superpeachsis.MainActivity;

import java.io.IOException;
import java.io.InputStream;

public class MenuActivity extends Activity {

    private MenuView menuView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        menuView = new MenuView(this);
        setContentView(menuView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (menuView != null) {
            menuView.resetNavigation();
        }
    }

    class MenuView extends View {

        private Bitmap bgBitmap;
        private Bitmap blockJouer;
        private Bitmap blockScores;
        private Bitmap characterIdle;

        private Paint titlePaint;
        private Paint titleOutlinePaint;
        private Paint btnTextPaint;

        private RectF btnJouer;
        private RectF btnScores;

        private float bounceOffset = 0f;
        private float bounceVelocity = BOUNCE_SPEED;
        private static final float BOUNCE_SPEED = 2.5f;
        private static final float BOUNCE_MAX   = 18f;

        private final Runnable animRunnable = new Runnable() {
            @Override
            public void run() {
                bounceOffset += bounceVelocity;
                if (bounceOffset >= BOUNCE_MAX)  bounceVelocity = -BOUNCE_SPEED;
                if (bounceOffset <= 0f)          bounceVelocity =  BOUNCE_SPEED;
                invalidate();
                postDelayed(this, 16);
            }
        };

        void resetNavigation() {
            navigating = false;
        }

        private boolean assetsLoaded = false;
        private Paint loadingPaint;

        public MenuView(Context context) {
            super(context);
            setupPaints();
            new Thread(() -> {
                loadAssets();
                assetsLoaded = true;
                post(this::invalidate);
            }).start();
            post(animRunnable);
        }

        private void loadAssets() {
            AssetManager am = getContext().getAssets();
            bgBitmap      = loadBitmap(am, "backgrounds/background_color_trees.png");
            blockJouer    = loadBitmap(am, "tiles/block_exclamation.png");
            blockScores   = loadBitmap(am, "tiles/block_coin.png");
            characterIdle = loadBitmap(am, "characters/character_pink_idle.png");
        }

        private Bitmap loadBitmap(AssetManager am, String path) {
            try (InputStream is = am.open(path)) {
                return BitmapFactory.decodeStream(is);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        private void setupPaints() {
            titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            titlePaint.setColor(Color.WHITE);
            titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            titlePaint.setTextAlign(Paint.Align.CENTER);
            titlePaint.setShadowLayer(8f, 4f, 4f, Color.parseColor("#80000000"));

            titleOutlinePaint = new Paint(titlePaint);
            titleOutlinePaint.setColor(Color.parseColor("#7B3F00"));
            titleOutlinePaint.setStyle(Paint.Style.STROKE);
            titleOutlinePaint.setStrokeWidth(8f);
            titleOutlinePaint.clearShadowLayer();

            btnTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            btnTextPaint.setColor(Color.WHITE);
            btnTextPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            btnTextPaint.setTextAlign(Paint.Align.CENTER);
            btnTextPaint.setShadowLayer(4f, 2f, 2f, Color.BLACK);

            loadingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            loadingPaint.setColor(Color.WHITE);
            loadingPaint.setTextSize(50f);
            loadingPaint.setTextAlign(Paint.Align.CENTER);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int w = getWidth();
            int h = getHeight();

            if (!assetsLoaded || navigating) {
                canvas.drawColor(Color.BLACK);
                canvas.drawText("Chargement...", w / 2f, h / 2f, loadingPaint);
                return;
            }

            if (bgBitmap != null) {
                canvas.drawBitmap(bgBitmap, null, new RectF(0, 0, w, h), null);
            } else {
                canvas.drawColor(Color.parseColor("#5C94FC"));
            }

            float titleSize = h * 0.085f;
            titlePaint.setTextSize(titleSize);
            titleOutlinePaint.setTextSize(titleSize);
            float titleY = h * 0.22f;

            canvas.drawText("SuperPeachSis", w / 2f, titleY, titleOutlinePaint);
            canvas.drawText("SuperPeachSis", w / 2f, titleY, titlePaint);

            float btnW = w * 0.55f;
            float btnH = h * 0.13f;
            float btnCx = w / 2f;
            btnTextPaint.setTextSize(btnH * 0.42f);

            btnJouer  = new RectF(btnCx - btnW / 2f, h * 0.40f, btnCx + btnW / 2f, h * 0.40f + btnH);
            btnScores = new RectF(btnCx - btnW / 2f, h * 0.58f, btnCx + btnW / 2f, h * 0.58f + btnH);

            drawBlockButton(canvas, blockJouer,  btnJouer,  "JOUER");
            drawBlockButton(canvas, blockScores, btnScores, "SCORES");

            float charH     = h * 0.18f;
            float charCx    = w * 0.15f;
            float charBottom = h * 0.52f - bounceOffset;

            if (characterIdle != null) {
                canvas.drawBitmap(characterIdle, null, new RectF(
                        charCx - charH / 2f,
                        charBottom - charH,
                        charCx + charH / 2f,
                        charBottom
                ), null);
            }
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
                Paint fallback = new Paint();
                fallback.setColor(Color.parseColor("#C84B11"));
                canvas.drawRoundRect(rect, 14f, 14f, fallback);
            }

            float textY = rect.centerY() + btnTextPaint.getTextSize() * 0.35f;
            canvas.drawText(label, rect.centerX(), textY, btnTextPaint);
        }

        private boolean navigating = false;

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_UP && !navigating) {
                float x = event.getX();
                float y = event.getY();

                if (btnJouer != null && btnJouer.contains(x, y)) {
                    navigating = true;
                    invalidate();
                    postDelayed(() -> {
                        Intent intent = new Intent(MenuActivity.this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }, 50);
                } else if (btnScores != null && btnScores.contains(x, y)) {
                    showScoresDialog();
                }
            }
            return true;
        }

        private void showScoresDialog() {
            SharedPreferences prefs = getSharedPreferences("superpeachsis_scores", Context.MODE_PRIVATE);
            int bestScore = prefs.getInt("best_score", 0);

            new AlertDialog.Builder(MenuActivity.this)
                    .setTitle("Meilleurs scores")
                    .setMessage("Meilleur score : " + bestScore)
                    .setPositiveButton("OK", null)
                    .show();
        }

        @Override
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            removeCallbacks(animRunnable);
        }
    }
}
