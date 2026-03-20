package com.example.superpeachsis.utils;

public class Camera {

    private float x;
    private float speed;
    private final float baseSpeed;

    public Camera(float baseSpeed) {
        this.baseSpeed = baseSpeed;
        this.speed = baseSpeed;
        this.x = 0;
    }

    public void update() {
        x += speed;
    }

    public float getX() {
        return x;
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public void accelerate(float increment) {
        this.speed += increment;
    }

    public void reset() {
        this.x = 0;
        this.speed = baseSpeed;
    }
}
