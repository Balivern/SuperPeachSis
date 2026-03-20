package com.example.superpeachsis.domain.model;

import android.graphics.Bitmap;
import android.graphics.Rect;

public class Block {
    public Bitmap bitmap;
    public int x, y;
    public int drawWidth, drawHeight;
    public boolean active;
    private final Rect rect = new Rect();

    public Block() {
        this.active = false;
    }

    public Rect getRect() {
        rect.set(x, y, x + drawWidth, y + drawHeight);
        return rect;
    }

    public void spawn(Bitmap bitmap, int x, int y, int drawWidth, int drawHeight) {
        this.bitmap = bitmap;
        this.x = x;
        this.y = y;
        this.drawWidth = drawWidth;
        this.drawHeight = drawHeight;
        this.active = true;
    }

    public void spawn(Bitmap bitmap, int x, int y) {
        int w = bitmap != null ? bitmap.getWidth() : 0;
        int h = bitmap != null ? bitmap.getHeight() : 0;
        spawn(bitmap, x, y, w, h);
    }

    public void update(int speed) {
        if (active) {
            x -= speed;
            if (x + drawWidth < 0) {
                active = false;
            }
        }
    }
}
