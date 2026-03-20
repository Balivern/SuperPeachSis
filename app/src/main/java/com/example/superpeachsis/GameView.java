package com.example.superpeachsis;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.example.superpeachsis.domain.model.Player;
import com.example.superpeachsis.utils.SpriteManager;

import java.util.List;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {

    private GameThread thread;
    private SpriteManager spriteManager;
    private Player player;

    private Bitmap backgroundBitmap;

    public GameView(Context context) {
        super(context);
        getHolder().addCallback(this);
        spriteManager = SpriteManager.getInstance(context);
        thread = new GameThread(getHolder(), this);
        setFocusable(true);
        loadSprites();
        player = new Player(200, 400, spriteManager);
    }

    private void loadSprites() {
        backgroundBitmap = spriteManager.getBackground("background_color_trees");
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        thread.setRunning(true);
        thread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        boolean retry = true;
        while (retry) {
            try {
                thread.setRunning(false);
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            retry = false;
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (canvas == null) {
            return;
        }

        canvas.drawColor(Color.parseColor("#5C94FC"));

        if (backgroundBitmap != null) {
            Bitmap bg = Bitmap.createScaledBitmap(backgroundBitmap, getWidth(), getHeight(), true);
            canvas.drawBitmap(bg, 0, 0, null);
        }

        if (player != null) {
            player.draw(canvas);
        }
    }

    public void update() {
        if (player != null) {
            player.update();
        }
    }
}
