package com.example.superpeachsis.ui;

import android.app.Activity;
import android.content.Context;
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

import com.example.superpeachsis.domain.service.ScoreManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class ScoreBoardActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        setContentView(new ScoreBoardView(this));
    }

    class ScoreBoardView extends View {

        private Bitmap bgBitmap;
        private Bitmap blockMenu;
        private Bitmap characterIdle;
        private Bitmap coinBitmap;

        private Paint titlePaint;
        private Paint titleOutlinePaint;
        private Paint scorePaint;
        private Paint btnTextPaint;
        private Paint tablePaint;

        private RectF btnMenu;
        private List<Integer> topScores;

        public ScoreBoardView(Context context) {
            super(context);
            topScores = new ScoreManager(context).getTopScores();
            loadAssets();
            setupPaints();
        }

        private void loadAssets() {
            AssetManager am = getContext().getAssets();
            bgBitmap      = loadBitmap(am, "backgrounds/background_color_desert.png");
            blockMenu     = loadBitmap(am, "tiles/block_coin.png");
            characterIdle = loadBitmap(am, "characters/character_pink_idle.png");
            coinBitmap    = loadBitmap(am, "tiles/block_coin.png");
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
            titleOutlinePaint.setColor(Color.parseColor("#D2691E"));
            titleOutlinePaint.setStyle(Paint.Style.STROKE);
            titleOutlinePaint.setStrokeWidth(8f);
            titleOutlinePaint.clearShadowLayer();

            scorePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            scorePaint.setColor(Color.WHITE);
            scorePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            scorePaint.setTextAlign(Paint.Align.LEFT);
            scorePaint.setShadowLayer(4f, 2f, 2f, Color.BLACK);

            btnTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            btnTextPaint.setColor(Color.WHITE);
            btnTextPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            btnTextPaint.setTextAlign(Paint.Align.CENTER);
            btnTextPaint.setShadowLayer(4f, 2f, 2f, Color.BLACK);

            tablePaint = new Paint();
            tablePaint.setColor(Color.parseColor("#90000000"));
            tablePaint.setStyle(Paint.Style.FILL);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int w = getWidth();
            int h = getHeight();

            if (bgBitmap != null) {
                canvas.drawBitmap(bgBitmap, null, new RectF(0, 0, w, h), null);
            } else {
                canvas.drawColor(Color.parseColor("#F4A460"));
            }

            float titleSize = h * 0.10f;
            titlePaint.setTextSize(titleSize);
            titleOutlinePaint.setTextSize(titleSize);
            float titleY = h * 0.15f;
            canvas.drawText("MEILLEURS SCORES", w / 2f, titleY, titleOutlinePaint);
            canvas.drawText("MEILLEURS SCORES", w / 2f, titleY, titlePaint);

            // Table Background
            float tableW = w * 0.8f;
            float tableH = h * 0.5f;
            float tableX = (w - tableW) / 2f;
            float tableY_pos = h * 0.25f;
            RectF tableRect = new RectF(tableX, tableY_pos, tableX + tableW, tableY_pos + tableH);
            canvas.drawRoundRect(tableRect, 20f, 20f, tablePaint);

            // Table Header
            scorePaint.setTextSize(h * 0.05f);
            float headerY = tableY_pos + h * 0.08f;
            canvas.drawText("RANG", tableX + w * 0.05f, headerY, scorePaint);
            canvas.drawText("SCORE", tableX + w * 0.45f, headerY, scorePaint);

            // Divider line
            Paint linePaint = new Paint();
            linePaint.setColor(Color.WHITE);
            linePaint.setStrokeWidth(4f);
            canvas.drawLine(tableX + w * 0.02f, headerY + 10, tableX + tableW - w * 0.02f, headerY + 10, linePaint);

            // Scores
            float rowHeight = h * 0.08f;
            for (int i = 0; i < 5; i++) {
                float rowY = headerY + (i + 1) * rowHeight;
                String rank = (i + 1) + ".";
                String scoreStr = (i < topScores.size()) ? String.valueOf(topScores.get(i)) : "---";
                
                canvas.drawText(rank, tableX + w * 0.05f, rowY, scorePaint);
                canvas.drawText(scoreStr, tableX + w * 0.45f, rowY, scorePaint);
                
                if (i < topScores.size() && coinBitmap != null) {
                    float coinSize = h * 0.04f;
                    canvas.drawBitmap(coinBitmap, null, new RectF(
                            tableX + w * 0.38f, rowY - coinSize * 0.8f,
                            tableX + w * 0.38f + coinSize, rowY + coinSize * 0.2f
                    ), null);
                }
            }

            // Back Button
            float btnW  = w * 0.35f;
            float btnH  = h * 0.10f;
            float btnY  = h * 0.82f;
            btnTextPaint.setTextSize(btnH * 0.45f);
            btnMenu = new RectF(w / 2f - btnW / 2f, btnY, w / 2f + btnW / 2f, btnY + btnH);
            drawBlockButton(canvas, blockMenu, btnMenu, "RETOUR");

            // Decorative Character
            float charH = h * 0.15f;
            if (characterIdle != null) {
                canvas.drawBitmap(characterIdle, null, new RectF(
                        w * 0.8f, h * 0.75f,
                        w * 0.8f + charH, h * 0.75f + charH
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

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                float x = event.getX();
                float y = event.getY();

                if (btnMenu != null && btnMenu.contains(x, y)) {
                    finish();
                }
            }
            return true;
        }
    }
}
