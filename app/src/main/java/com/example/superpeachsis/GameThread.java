package com.example.superpeachsis;

import android.graphics.Canvas;
import android.view.SurfaceHolder;

public class GameThread extends Thread {

    private static final int TARGET_FPS = 60;
    private static final long FRAME_DURATION_NS = 1_000_000_000L / TARGET_FPS;

    private final SurfaceHolder surfaceHolder;
    private final GameView gameView;
    private volatile boolean running;

    public GameThread(SurfaceHolder surfaceHolder, GameView gameView) {
        super();
        this.surfaceHolder = surfaceHolder;
        this.gameView = gameView;
    }

    @Override
    public void run() {
        long nextFrameTime = System.nanoTime();

        while (running) {
            Canvas canvas = null;

            try {
                canvas = surfaceHolder.lockCanvas();
                if (canvas != null) {
                    synchronized (surfaceHolder) {
                        gameView.update();
                        gameView.draw(canvas);
                    }
                }
            } finally {
                if (canvas != null) {
                    try {
                        surfaceHolder.unlockCanvasAndPost(canvas);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            nextFrameTime += FRAME_DURATION_NS;
            long sleepNs = nextFrameTime - System.nanoTime();

            if (sleepNs > 0) {
                try {
                    Thread.sleep(sleepNs / 1_000_000, (int) (sleepNs % 1_000_000));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            } else {
                nextFrameTime = System.nanoTime();
            }
        }
    }

    public void setRunning(boolean isRunning) {
        running = isRunning;
    }
}
