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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        int distance = getIntent().getIntExtra(EXTRA_DISTANCE, 0);
        int coins    = getIntent().getIntExtra(EXTRA_COINS, 0);
        int score    = distance + coins;

        new ScoreManager(this).saveScore(score);

        setContentView(new GameOverView(this, distance, coins, score));
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

        private RectF btnRejouer;
        private RectF btnMenu;

        public GameOverView(Context context, int distance, int coins, int score) {
            super(context);
            this.distance  = distance;
            this.coins     = coins;
            this.score     = score;
            this.bestScore = new ScoreManager(context).getBestScore();
            loadAssets();
            setupPaints();
        }

        private void loadAssets() {
            AssetManager am = getContext().getAssets();
            bgBitmap      = loadBitmap(am, "backgrounds/background_color_hills.png");
            characterHit  = loadBitmap(am, "characters/character_pink_hit.png");
            blockRejouer  = loadBitmap(am, "tiles/block_exclamation.png");
            blockMenu     = loadBitmap(am, "tiles/block_coin.png");
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
            titleOutlinePaint.setColor(Color.parseColor("#8B0000"));
            titleOutlinePaint.setStyle(Paint.Style.STROKE);
            titleOutlinePaint.setStrokeWidth(8f);
            titleOutlinePaint.clearShadowLayer();

            scorePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            scorePaint.setColor(Color.WHITE);
            scorePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            scorePaint.setTextAlign(Paint.Align.CENTER);
            scorePaint.setShadowLayer(4f, 2f, 2f, Color.BLACK);

            btnTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            btnTextPaint.setColor(Color.WHITE);
            btnTextPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            btnTextPaint.setTextAlign(Paint.Align.CENTER);
            btnTextPaint.setShadowLayer(4f, 2f, 2f, Color.BLACK);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int w = getWidth();
            int h = getHeight();

            if (bgBitmap != null) {
                canvas.drawBitmap(bgBitmap, null, new RectF(0, 0, w, h), null);
            } else {
                canvas.drawColor(Color.parseColor("#1a1a2e"));
            }

            Paint overlay = new Paint();
            overlay.setColor(Color.parseColor("#80000000"));
            canvas.drawRect(0, 0, w, h, overlay);

            float titleSize = h * 0.10f;
            titlePaint.setTextSize(titleSize);
            titleOutlinePaint.setTextSize(titleSize);
            float titleY = h * 0.20f;
            canvas.drawText("GAME OVER", w / 2f, titleY, titleOutlinePaint);
            canvas.drawText("GAME OVER", w / 2f, titleY, titlePaint);

            float charH   = h * 0.16f;
            float charCx  = w / 2f;
            float charTop = h * 0.24f;
            if (characterHit != null) {
                canvas.drawBitmap(characterHit, null, new RectF(
                        charCx - charH / 2f, charTop,
                        charCx + charH / 2f, charTop + charH
                ), null);
            }

            float scoreSize = h * 0.040f;
            scorePaint.setTextSize(scoreSize);
            float lineH = scoreSize * 1.6f;
            float scoreY = h * 0.46f;

            canvas.drawText("Distance : " + distance + " m",  w / 2f, scoreY,          scorePaint);
            canvas.drawText("Pièces   : " + coins,            w / 2f, scoreY + lineH,   scorePaint);
            canvas.drawText("Score    : " + score,            w / 2f, scoreY + lineH * 2, scorePaint);
            canvas.drawText("Meilleur : " + bestScore,        w / 2f, scoreY + lineH * 3, scorePaint);

            float btnW  = w * 0.42f;
            float btnH  = h * 0.12f;
            float btnY  = h * 0.72f;
            float gap   = w * 0.06f;

            btnTextPaint.setTextSize(btnH * 0.40f);

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
                Paint fallback = new Paint();
                fallback.setColor(Color.parseColor("#C84B11"));
                canvas.drawRoundRect(rect, 14f, 14f, fallback);
            }

            float textY = rect.centerY() + btnTextPaint.getTextSize() * 0.35f;
            canvas.drawText(label, rect.centerX(), textY, btnTextPaint);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                float x = event.getX();
                float y = event.getY();

                if (btnRejouer != null && btnRejouer.contains(x, y)) {
                    Intent intent = new Intent(GameOverActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                } else if (btnMenu != null && btnMenu.contains(x, y)) {
                    Intent intent = new Intent(GameOverActivity.this, MenuActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                }
            }
            return true;
        }
    }
}
