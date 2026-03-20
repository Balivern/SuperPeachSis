package com.example.superpeachsis.domain.model;

import android.graphics.Bitmap;

public class Block {
    public Bitmap bitmap;
    public int x, y;
    public boolean active; // Pour savoir si le bloc est visible ou recyclable

    public Block() {
        this.active = false; // Inactif au départ
    }

    public void spawn(Bitmap bitmap, int x, int y) {
        this.bitmap = bitmap;
        this.x = x;
        this.y = y;
        this.active = true;
    }

    public void update(int speed) {
        if (active) {
            x -= speed; // Le bloc se déplace vers la gauche
            if (x + (bitmap != null ? bitmap.getWidth() : 0) < 0) {
                active = false; // Devient recyclable quand il sort de l'écran
            }
        }
    }
}