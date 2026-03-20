package com.example.superpeachsis.domain.service;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import com.example.superpeachsis.utils.SpriteManager;

public class HUD {

    public interface Listener {
        void onAbandon();
    }

    public static final int MAX_LIVES = 3;

    private static final int BLINK_DURATION = 60;
    private static final int BLINK_INTERVAL = 8;

    private final Bitmap[] digits = new Bitmap[10];
    private Bitmap heartFull;
    private Bitmap heartEmpty;
    private Bitmap blockBtn;

    private int lives    = MAX_LIVES;
    private int distance = 0;

    private int      blinkTick    = 0;
    private boolean  blinkVisible = true;
    private volatile boolean paused = false;

    private Listener listener;

    private RectF pauseButtonRect;
    private RectF btnContinuerRect;
    private RectF btnAbandonnerRect;

    private final Paint hudPaint;
    private final Paint fallbackPaint;
    private final Paint btnBgPaint;
    private final Paint btnIconPaint;
    private final Paint overlayPaint;
    private final Paint titlePaint;
    private final Paint titleOutlinePaint;
    private final Paint popupBtnTextPaint;
    private final Paint heartFallbackFull;
    private final Paint heartFallbackEmpty;

    public HUD(SpriteManager sm) {
        for (int i = 0; i <= 9; i++) {
            digits[i] = sm.getTile("hud_character_" + i);
        }
        heartFull  = sm.getTile("hud_heart");
        heartEmpty = sm.getTile("hud_heart_empty");
        blockBtn   = sm.getTile("block_exclamation");

        hudPaint = new Paint();
        hudPaint.setAlpha(220);

        fallbackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fallbackPaint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        fallbackPaint.setColor(Color.WHITE);
        fallbackPaint.setShadowLayer(3f, 1f, 1f, Color.BLACK);
        fallbackPaint.setAlpha(220);

        btnBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        btnBgPaint.setColor(Color.parseColor("#99000000"));

        btnIconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        btnIconPaint.setColor(Color.WHITE);
        btnIconPaint.setStyle(Paint.Style.FILL);

        overlayPaint = new Paint();
        overlayPaint.setColor(Color.parseColor("#BB000000"));

        titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setColor(Color.WHITE);
        titlePaint.setTypeface(android.graphics.Typeface.create(
                android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD));
        titlePaint.setTextAlign(Paint.Align.CENTER);
        titlePaint.setShadowLayer(8f, 4f, 4f, Color.parseColor("#80000000"));

        titleOutlinePaint = new Paint(titlePaint);
        titleOutlinePaint.setColor(Color.parseColor("#7B3F00"));
        titleOutlinePaint.setStyle(Paint.Style.STROKE);
        titleOutlinePaint.setStrokeWidth(8f);
        titleOutlinePaint.clearShadowLayer();

        popupBtnTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        popupBtnTextPaint.setColor(Color.WHITE);
        popupBtnTextPaint.setTypeface(android.graphics.Typeface.create(
                android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD));
        popupBtnTextPaint.setTextAlign(Paint.Align.CENTER);
        popupBtnTextPaint.setShadowLayer(4f, 2f, 2f, Color.BLACK);

        heartFallbackFull = new Paint(Paint.ANTI_ALIAS_FLAG);
        heartFallbackFull.setColor(Color.RED);
        heartFallbackFull.setStyle(Paint.Style.FILL);

        heartFallbackEmpty = new Paint(Paint.ANTI_ALIAS_FLAG);
        heartFallbackEmpty.setColor(Color.WHITE);
        heartFallbackEmpty.setStyle(Paint.Style.STROKE);
        heartFallbackEmpty.setStrokeWidth(3f);
    }

    public void setListener(Listener listener) { this.listener = listener; }

    public void setLives(int newLives) {
        if (newLives < this.lives) blinkTick = BLINK_DURATION;
        this.lives = Math.max(0, Math.min(MAX_LIVES, newLives));
    }

    public void setDistance(int distance) { this.distance = distance; }
    public int  getLives()                { return lives; }
    public int  getDistance()             { return distance; }
    public boolean isPaused()             { return paused; }

    public boolean onTouch(float x, float y) {
        if (paused) {
            if (btnContinuerRect != null && btnContinuerRect.contains(x, y)) {
                paused = false;
                return true;
            }
            if (btnAbandonnerRect != null && btnAbandonnerRect.contains(x, y)) {
                if (listener != null) listener.onAbandon();
                return true;
            }
            if (pauseButtonRect != null && pauseButtonRect.contains(x, y)) {
                paused = false;
                return true;
            }
            return true;
        }
        if (pauseButtonRect != null && pauseButtonRect.contains(x, y)) {
            paused = true;
            return true;
        }
        return false;
    }

    public void update() {
        if (blinkTick > 0) {
            blinkTick--;
            blinkVisible = (blinkTick / BLINK_INTERVAL) % 2 == 0;
        } else {
            blinkVisible = true;
        }
    }

    public void draw(Canvas canvas, int screenW, int screenH) {
        float pad       = screenH * 0.025f;
        float heartSize = screenH * 0.085f;
        float digitH    = heartSize * 0.90f;
        float digitW    = digitH * 0.72f;

        float x = pad;
        float y = pad;

        for (int i = 0; i < MAX_LIVES; i++) {
            boolean isFull     = i < lives;
            boolean isBlinking = isFull && (i == lives - 1) && blinkTick > 0;
            boolean render     = !isBlinking || blinkVisible;

            if (render) {
                Bitmap heart = isFull ? heartFull : heartEmpty;
                if (heart != null) {
                    canvas.drawBitmap(heart, null,
                            new RectF(x, y, x + heartSize, y + heartSize), hudPaint);
                } else {
                    float cx = x + heartSize / 2f;
                    float cy = y + heartSize / 2f;
                    float r  = heartSize / 2f - 2f;
                    canvas.drawCircle(cx, cy, r, isFull ? heartFallbackFull : heartFallbackEmpty);
                }
            }
            x += heartSize + pad * 0.5f;
        }

        float btnSize = heartSize * 1.1f;
        float btnX    = screenW - pad - btnSize;
        pauseButtonRect = new RectF(btnX, y, btnX + btnSize, y + btnSize);
        drawPauseButton(canvas, pauseButtonRect);

        float distRight = btnX - pad;
        float digitTop  = y + (btnSize - digitH) / 2f;
        float distWidth = String.valueOf(distance).length() * (digitW + 1f);
        drawNumber(canvas, distance, distRight - distWidth, digitTop, digitW, digitH);

        if (paused) {
            drawPausePopup(canvas, screenW, screenH);
        }
    }

    private void drawPausePopup(Canvas canvas, int screenW, int screenH) {
        canvas.drawRect(0, 0, screenW, screenH, overlayPaint);

        float titleSize = screenH * 0.09f;
        titlePaint.setTextSize(titleSize);
        titleOutlinePaint.setTextSize(titleSize);
        float titleY = screenH * 0.30f;
        canvas.drawText("PAUSE", screenW / 2f, titleY, titleOutlinePaint);
        canvas.drawText("PAUSE", screenW / 2f, titleY, titlePaint);

        float btnW  = screenW * 0.32f;
        float btnH  = screenH * 0.14f;
        float btnY  = screenH * 0.50f;
        float gap   = screenW * 0.06f;
        float cx    = screenW / 2f;

        popupBtnTextPaint.setTextSize(btnH * 0.38f);

        btnContinuerRect  = new RectF(cx - gap / 2f - btnW, btnY, cx - gap / 2f, btnY + btnH);
        btnAbandonnerRect = new RectF(cx + gap / 2f, btnY, cx + gap / 2f + btnW, btnY + btnH);

        drawBlockButton(canvas, btnContinuerRect,  "CONTINUER");
        drawBlockButton(canvas, btnAbandonnerRect, "ABANDONNER");
    }

    private void drawBlockButton(Canvas canvas, RectF rect, String label) {
        if (blockBtn != null) {
            float tileSize = rect.height();
            int numTiles = (int) Math.ceil(rect.width() / tileSize);
            for (int i = 0; i < numTiles; i++) {
                float left  = rect.left + i * tileSize;
                float right = Math.min(left + tileSize, rect.right);
                canvas.drawBitmap(blockBtn, null,
                        new RectF(left, rect.top, right, rect.bottom), hudPaint);
            }
        } else {
            Paint fallback = new Paint();
            fallback.setColor(Color.parseColor("#C84B11"));
            canvas.drawRoundRect(rect, 14f, 14f, fallback);
        }
        float textY = rect.centerY() + popupBtnTextPaint.getTextSize() * 0.35f;
        canvas.drawText(label, rect.centerX(), textY, popupBtnTextPaint);
    }

    private void drawPauseButton(Canvas canvas, RectF rect) {
        float cx = rect.centerX();
        float cy = rect.centerY();
        float r  = Math.min(rect.width(), rect.height()) / 2f;

        canvas.drawCircle(cx, cy, r, btnBgPaint);

        if (paused) {
            float tw = r * 0.65f;
            float th = r * 0.75f;
            Path triangle = new Path();
            triangle.moveTo(cx - tw * 0.4f, cy - th / 2f);
            triangle.lineTo(cx + tw * 0.6f, cy);
            triangle.lineTo(cx - tw * 0.4f, cy + th / 2f);
            triangle.close();
            canvas.drawPath(triangle, btnIconPaint);
        } else {
            float barW   = r * 0.22f;
            float barH   = r * 0.75f;
            float gap    = r * 0.20f;
            float barTop = cy - barH / 2f;
            canvas.drawRect(cx - gap / 2f - barW, barTop, cx - gap / 2f, barTop + barH, btnIconPaint);
            canvas.drawRect(cx + gap / 2f, barTop, cx + gap / 2f + barW, barTop + barH, btnIconPaint);
        }
    }

    private void drawNumber(Canvas canvas, int number, float startX, float startY,
                            float digitW, float digitH) {
        String str = String.valueOf(number);
        float x = startX;
        for (char c : str.toCharArray()) {
            int d = c - '0';
            if (d >= 0 && d <= 9 && digits[d] != null) {
                canvas.drawBitmap(digits[d], null,
                        new RectF(x, startY, x + digitW, startY + digitH), hudPaint);
            } else {
                fallbackPaint.setTextSize(digitH);
                canvas.drawText(String.valueOf(c), x, startY + digitH, fallbackPaint);
            }
            x += digitW + 1f;
        }
    }
}
