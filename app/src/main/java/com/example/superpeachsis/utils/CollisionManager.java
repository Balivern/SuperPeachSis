package com.example.superpeachsis.utils;

import android.graphics.Rect;

public class CollisionManager {

    public enum Side {
        NONE, TOP, BOTTOM, LEFT, RIGHT
    }

    public static boolean checkCollision(Rect a, Rect b) {
        return Rect.intersects(a, b);
    }

    public static Side getCollisionSide(Rect a, Rect b) {
        if (!Rect.intersects(a, b)) {
            return Side.NONE;
        }

        int overlapLeft = a.right - b.left;
        int overlapRight = b.right - a.left;
        int overlapTop = a.bottom - b.top;
        int overlapBottom = b.bottom - a.top;

        int minOverlap = Math.min(Math.min(overlapLeft, overlapRight), Math.min(overlapTop, overlapBottom));

        if (minOverlap == overlapTop) {
            return Side.TOP;
        } else if (minOverlap == overlapBottom) {
            return Side.BOTTOM;
        } else if (minOverlap == overlapLeft) {
            return Side.LEFT;
        } else {
            return Side.RIGHT;
        }
    }
}
