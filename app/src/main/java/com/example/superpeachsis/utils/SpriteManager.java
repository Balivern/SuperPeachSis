package com.example.superpeachsis.utils;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpriteManager {

    private static SpriteManager instance;
    private final AssetManager assetManager;
    private final Map<String, Bitmap> cache = new HashMap<>();

    private SpriteManager(Context context) {
        this.assetManager = context.getAssets();
    }

    public static SpriteManager getInstance(Context context) {
        if (instance == null) {
            instance = new SpriteManager(context.getApplicationContext());
        }
        return instance;
    }

    public Bitmap loadBitmap(String path) {
        if (cache.containsKey(path)) {
            return cache.get(path);
        }
        try (InputStream is = assetManager.open(path)) {
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            if (bitmap != null) {
                cache.put(path, bitmap);
            }
            return bitmap;
        } catch (IOException e) {
            return null;
        }
    }

    public Bitmap loadBitmapScaled(String path, int width, int height) {
        String key = path + "_" + width + "x" + height;
        if (cache.containsKey(key)) {
            return cache.get(key);
        }
        Bitmap original = loadBitmap(path);
        if (original == null) {
            return null;
        }
        Bitmap scaled = Bitmap.createScaledBitmap(original, width, height, true);
        cache.put(key, scaled);
        return scaled;
    }

    public List<Bitmap> getCharacterFrames(String color, String... actions) {
        List<Bitmap> frames = new ArrayList<>();
        for (String action : actions) {
            Bitmap bmp = loadBitmap("characters/character_" + color + "_" + action + ".png");
            if (bmp != null) {
                frames.add(bmp);
            }
        }
        return frames;
    }

    public Bitmap getTile(String name) {
        return loadBitmap("tiles/" + name + ".png");
    }

    public Bitmap getEnemy(String name) {
        return loadBitmap("enemies/" + name + ".png");
    }

    public Bitmap getBackground(String name) {
        return loadBitmap("backgrounds/" + name + ".png");
    }

    public void clearCache() {
        for (Bitmap bitmap : cache.values()) {
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
        }
        cache.clear();
    }
}
