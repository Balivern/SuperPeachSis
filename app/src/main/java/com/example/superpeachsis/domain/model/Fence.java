package com.example.superpeachsis.domain.model;

import android.graphics.Bitmap;

public class Fence extends Block {
    public enum Pattern {
        CIRCLE, CROSS, LINE
    }

    private Bitmap brokenBitmap;
    private Bitmap originalBitmap;
    public boolean broken = false;
    public Pattern expectedPattern;

    public void spawn(Bitmap bitmap, Bitmap brokenBitmap, int x, int y, int drawWidth, int drawHeight, Pattern pattern) {
        super.spawn(bitmap, x, y, drawWidth, drawHeight);
        this.originalBitmap = bitmap;
        this.brokenBitmap = brokenBitmap;
        this.broken = false;
        this.expectedPattern = pattern;
    }

    public void breakFence() {
        this.broken = true;
        this.bitmap = brokenBitmap;
    }

    public void reset() {
        this.broken = false;
        this.bitmap = originalBitmap;
    }
}
