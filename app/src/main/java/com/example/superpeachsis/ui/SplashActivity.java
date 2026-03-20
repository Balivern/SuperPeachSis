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
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import java.io.IOException;
import java.io.InputStream;

public class SplashActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        setContentView(new SplashView(this));
    }

    class SplashView extends View {

        private static final long SPLASH_DURATION = 3200;
        private static final long LOGO_SCALE_IN = 800;
        private static final long LOGO_SETTLE = 1200;
        private static final long SHIMMER_START = 1000;
        private static final long PARTICLES_START = 600;
        private static final long FADE_OUT_START = 2600;

        private Bitmap logoBitmap;
        private Bitmap bgBitmap;
        private Bitmap groundTile;
        private Bitmap characterRun;

        private final Paint bitmapPaint;
        private final Paint overlayPaint;
        private final Paint particlePaint;
        private final Paint shimmerPaint;

        private long startTime;
        private boolean navigated = false;

        private final float[] particleX = new float[20];
        private final float[] particleY = new float[20];
        private final float[] particleVx = new float[20];
        private final float[] particleVy = new float[20];
        private final float[] particleSize = new float[20];
        private final int[] particleColor = new int[20];

        private final Runnable tickRunnable = new Runnable() {
            @Override
            public void run() {
                long elapsed = System.currentTimeMillis() - startTime;
                if (elapsed >= SPLASH_DURATION && !navigated) {
                    navigated = true;
                    startActivity(new Intent(SplashActivity.this, MenuActivity.class));
                    finish();
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    return;
                }
                invalidate();
                postDelayed(this, 16);
            }
        };

        public SplashView(Context context) {
            super(context);
            loadAssets();

            bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
            overlayPaint = new Paint();
            particlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            shimmerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            shimmerPaint.setColor(Color.WHITE);

            initParticles();

            startTime = System.currentTimeMillis();
            post(tickRunnable);
        }

        private void loadAssets() {
            AssetManager am = getContext().getAssets();
            logoBitmap = loadBitmap(am, "logo.png");
            bgBitmap = loadBitmap(am, "backgrounds/background_color_trees.png");
            groundTile = loadBitmap(am, "tiles/terrain_grass_block_top.png");
            characterRun = loadBitmap(am, "characters/character_pink_walk_a.png");
        }

        private Bitmap loadBitmap(AssetManager am, String path) {
            try (InputStream is = am.open(path)) {
                return BitmapFactory.decodeStream(is);
            } catch (IOException e) {
                return null;
            }
        }

        private void initParticles() {
            int[] colors = {
                    Color.parseColor("#FFB7A1"),
                    Color.parseColor("#FFD700"),
                    Color.parseColor("#FF8C69"),
                    Color.parseColor("#FFC0CB"),
                    Color.WHITE
            };
            for (int i = 0; i < particleX.length; i++) {
                resetParticle(i);
                particleColor[i] = colors[i % colors.length];
            }
        }

        private void resetParticle(int i) {
            particleX[i] = (float) (Math.random() * 1.2f - 0.1f);
            particleY[i] = (float) (0.3f + Math.random() * 0.4f);
            particleVx[i] = (float) (Math.random() * 0.002f - 0.001f);
            particleVy[i] = (float) (-0.001f - Math.random() * 0.003f);
            particleSize[i] = (float) (3f + Math.random() * 6f);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int w = getWidth();
            int h = getHeight();
            long elapsed = System.currentTimeMillis() - startTime;

            drawBackground(canvas, w, h);
            drawGround(canvas, w, h, elapsed);

            if (elapsed > PARTICLES_START) {
                drawParticles(canvas, w, h, elapsed);
            }

            drawLogo(canvas, w, h, elapsed);

            if (elapsed > SHIMMER_START) {
                drawShimmer(canvas, w, h, elapsed);
            }

            drawRunningCharacter(canvas, w, h, elapsed);

            if (elapsed > FADE_OUT_START) {
                float fadeOut = clamp01((float) (elapsed - FADE_OUT_START) / (SPLASH_DURATION - FADE_OUT_START));
                overlayPaint.setColor(Color.BLACK);
                overlayPaint.setAlpha((int) (255 * fadeOut));
                canvas.drawRect(0, 0, w, h, overlayPaint);
            }
        }

        private void drawBackground(Canvas canvas, int w, int h) {
            if (bgBitmap != null) {
                canvas.drawBitmap(bgBitmap, null, new RectF(0, 0, w, h), null);
            } else {
                canvas.drawColor(Color.parseColor("#FFECD2"));
            }

            overlayPaint.setColor(Color.parseColor("#40FFFFFF"));
            canvas.drawRect(0, 0, w, h, overlayPaint);
        }

        private void drawGround(Canvas canvas, int w, int h, long elapsed) {
            if (groundTile == null) return;
            int tileSize = 64;
            int groundY = h - tileSize;
            float scroll = (elapsed * 0.05f) % tileSize;
            int tilesNeeded = (w / tileSize) + 2;
            for (int i = 0; i < tilesNeeded; i++) {
                float tileX = i * tileSize - scroll;
                canvas.drawBitmap(groundTile, null,
                        new RectF(tileX, groundY, tileX + tileSize, h), null);
            }
        }

        private void drawLogo(Canvas canvas, int w, int h, long elapsed) {
            if (logoBitmap == null) return;

            float scaleProgress = clamp01((float) elapsed / LOGO_SCALE_IN);
            float scale = easeOutBack(scaleProgress);

            float settleProgress = clamp01((float) (elapsed - LOGO_SCALE_IN) / (LOGO_SETTLE - LOGO_SCALE_IN));
            float breathe = 0f;
            if (settleProgress >= 1f) {
                float breatheTime = (elapsed - LOGO_SETTLE) / 1000f;
                breathe = (float) Math.sin(breatheTime * 2.5f) * 0.015f;
            }

            float finalScale = scale * (1f + breathe);

            float logoW = w * 0.65f;
            float logoH = logoW * ((float) logoBitmap.getHeight() / logoBitmap.getWidth());
            float logoX = (w - logoW) / 2f;
            float logoY = (h - logoH) / 2f - h * 0.05f;

            int alpha = (int) (255 * clamp01(scaleProgress * 2f));
            bitmapPaint.setAlpha(alpha);

            canvas.save();
            canvas.translate(w / 2f, h / 2f - h * 0.05f);
            canvas.scale(finalScale, finalScale);
            canvas.translate(-w / 2f, -(h / 2f - h * 0.05f));
            canvas.drawBitmap(logoBitmap, null,
                    new RectF(logoX, logoY, logoX + logoW, logoY + logoH), bitmapPaint);
            canvas.restore();
        }

        private void drawShimmer(Canvas canvas, int w, int h, long elapsed) {
            float shimmerTime = (elapsed - SHIMMER_START) / 1500f;
            float shimmerX = (shimmerTime % 1f) * w * 1.4f - w * 0.2f;

            float logoW = w * 0.65f;
            float logoH = logoW * (logoBitmap != null ?
                    (float) logoBitmap.getHeight() / logoBitmap.getWidth() : 0.5f);
            float logoY = (h - logoH) / 2f - h * 0.05f;

            int shimmerAlpha = (int) (80 * (1f - Math.abs((shimmerTime % 1f) - 0.5f) * 2f));
            shimmerPaint.setAlpha(Math.max(0, shimmerAlpha));

            canvas.save();
            canvas.clipRect(shimmerX - 30, logoY, shimmerX + 30, logoY + logoH);
            canvas.drawRect(shimmerX - 30, logoY, shimmerX + 30, logoY + logoH, shimmerPaint);
            canvas.restore();
        }

        private void drawParticles(Canvas canvas, int w, int h, long elapsed) {
            float progress = clamp01((float) (elapsed - PARTICLES_START) / 500f);
            int alpha = (int) (180 * progress);

            for (int i = 0; i < particleX.length; i++) {
                particleX[i] += particleVx[i];
                particleY[i] += particleVy[i];

                if (particleY[i] < 0.1f) {
                    resetParticle(i);
                }

                float sparkle = 0.5f + 0.5f * (float) Math.sin(elapsed * 0.01f + i * 1.3f);
                int a = (int) (alpha * sparkle);

                particlePaint.setColor(particleColor[i]);
                particlePaint.setAlpha(a);

                float px = particleX[i] * w;
                float py = particleY[i] * h;
                canvas.drawCircle(px, py, particleSize[i], particlePaint);
            }
        }

        private void drawRunningCharacter(Canvas canvas, int w, int h, long elapsed) {
            if (characterRun == null) return;

            float runProgress = clamp01((float) (elapsed - 400) / 2000f);
            if (runProgress <= 0) return;

            float charH = h * 0.12f;
            float charW = charH * 0.75f;

            float startX = -charW;
            float endX = w * 0.12f;
            float charX = startX + (endX - startX) * easeOutCubic(runProgress);

            float groundY = h - 64;
            float bounce = (float) Math.abs(Math.sin(elapsed * 0.012f)) * h * 0.03f;
            float charY = groundY - charH - bounce;

            int alpha = (int) (255 * clamp01(runProgress * 3f));
            bitmapPaint.setAlpha(alpha);
            canvas.drawBitmap(characterRun, null,
                    new RectF(charX, charY, charX + charW, charY + charH), bitmapPaint);
        }

        private float clamp01(float v) {
            return Math.max(0f, Math.min(1f, v));
        }

        private float easeOutBack(float t) {
            float c1 = 1.70158f;
            float c3 = c1 + 1f;
            return 1f + c3 * (float) Math.pow(t - 1, 3) + c1 * (float) Math.pow(t - 1, 2);
        }

        private float easeOutCubic(float t) {
            return 1f - (float) Math.pow(1 - t, 3);
        }

        @Override
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            removeCallbacks(tickRunnable);
        }
    }
}
