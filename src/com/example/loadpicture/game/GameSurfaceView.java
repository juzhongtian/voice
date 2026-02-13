package com.example.loadpicture.game;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;

public class GameSurfaceView extends GLSurfaceView {

    private static final float SWIPE_THRESHOLD = 80f;

    private final RunnerRenderer renderer;
    private float downX;
    private float downY;

    public GameSurfaceView(Context context) {
        super(context);
        setEGLContextClientVersion(2);
        renderer = new RunnerRenderer();
        setRenderer(renderer);
        setRenderMode(RENDERMODE_CONTINUOUSLY);
    }

    public void setHudListener(RunnerRenderer.GameHudListener listener) {
        renderer.setHudListener(listener);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getX();
                downY = event.getY();
                return true;
            case MotionEvent.ACTION_UP:
                handleTouchAction(event.getX(), event.getY());
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    private void handleTouchAction(float upX, float upY) {
        float dx = upX - downX;
        float dy = upY - downY;

        if (Math.abs(dx) > Math.abs(dy) && Math.abs(dx) > SWIPE_THRESHOLD) {
            renderer.moveLane(dx > 0 ? 1 : -1);
            return;
        }

        if (upY < getHeight() * 0.45f || downY < getHeight() * 0.45f) {
            renderer.jump();
        }
    }
}
