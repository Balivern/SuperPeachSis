package com.example.superpeachsis;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.util.Log;

public class ShakeDetector implements SensorEventListener {

    private static final String TAG = "ShakeDetector";
    private static final float SHAKE_THRESHOLD = 12.0f;
    private static final int COOLDOWN_MS = 500;
    
    private OnShakeListener mListener;
    private long lastShakeTime;

    public interface OnShakeListener {
        void onShake();
    }

    public void setOnShakeListener(OnShakeListener listener) {
        this.mListener = listener;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (mListener != null) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            double acceleration = Math.sqrt(x * x + y * y + z * z);
            if (acceleration > SHAKE_THRESHOLD) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastShakeTime > COOLDOWN_MS) {
                    lastShakeTime = currentTime;
                    Log.d(TAG, "Shake detected! Acceleration: " + acceleration);
                    mListener.onShake();
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not used
    }
}
