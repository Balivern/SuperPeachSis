package com.example.superpeachsis.domain.model;

import android.graphics.Bitmap;
import android.graphics.Rect;

public class Block {
    public Bitmap bitmap;
    public int x, y;
    public boolean active;
    private final Rect rect = new Rect();

    public Block() {
        this.active = false;
    }

    public Rect getRect() {
        int w = bitmap != null ? bitmap.getWidth() : 0;
        int h = bitmap != null ? bitmap.getHeight() : 0;
        rect.set(x, y, x + w, y + h);
        return rect;
    }

    public void spawn(Bitmap bitmap, int x, int y) {
        this.bitmap = bitmap;
        this.x = x;
        this.y = y;
        this.active = true;
    }

    public void update(int speed) {
        if (active) {
            x -= speed;
            if (x + (bitmap != null ? bitmap.getWidth() : 0) < 0) {
                active = false;
            }
        }
    }
}
