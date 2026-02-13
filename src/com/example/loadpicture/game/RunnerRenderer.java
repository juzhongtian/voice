package com.example.loadpicture.game;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;

public class RunnerRenderer implements GLSurfaceView.Renderer {

    public interface GameHudListener {
        void onHudUpdated(int score, float speed, boolean crashed);
    }

    private static final String VERTEX_SHADER =
            "uniform mat4 uMVP;" +
            "attribute vec3 aPos;" +
            "void main(){ gl_Position = uMVP * vec4(aPos,1.0); }";

    private static final String FRAGMENT_SHADER =
            "precision mediump float;" +
            "uniform vec4 uColor;" +
            "void main(){ gl_FragColor = uColor; }";

    private final float[] projection = new float[16];
    private final float[] view = new float[16];
    private final float[] model = new float[16];
    private final float[] mvp = new float[16];
    private final float[] tempM = new float[16];

    private final List<Obstacle> obstacles = new ArrayList<Obstacle>();
    private final Random random = new Random();

    private FloatBuffer cubeBuffer;
    private int program;
    private int mvpHandle;
    private int posHandle;
    private int colorHandle;

    private int lane = 1;
    private float laneX = 0f;
    private float playerY = 0f;
    private float velocityY = 0f;
    private long lastFrameTime;
    private float spawnTimer = 0f;
    private float speed = 6f;
    private int score = 0;
    private boolean crashState = false;
    private long crashTimeMs = 0L;
    private GameHudListener hudListener;

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.08f, 0.16f, 0.32f, 1f);
        program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER);
        mvpHandle = GLES20.glGetUniformLocation(program, "uMVP");
        posHandle = GLES20.glGetAttribLocation(program, "aPos");
        colorHandle = GLES20.glGetUniformLocation(program, "uColor");

        float[] cube = {
                -0.5f, -0.5f, -0.5f, 0.5f, -0.5f, -0.5f, 0.5f, 0.5f, -0.5f,
                -0.5f, -0.5f, -0.5f, 0.5f, 0.5f, -0.5f, -0.5f, 0.5f, -0.5f,
                -0.5f, -0.5f, 0.5f, 0.5f, -0.5f, 0.5f, 0.5f, 0.5f, 0.5f,
                -0.5f, -0.5f, 0.5f, 0.5f, 0.5f, 0.5f, -0.5f, 0.5f, 0.5f,
                -0.5f, -0.5f, -0.5f, -0.5f, 0.5f, -0.5f, -0.5f, 0.5f, 0.5f,
                -0.5f, -0.5f, -0.5f, -0.5f, 0.5f, 0.5f, -0.5f, -0.5f, 0.5f,
                0.5f, -0.5f, -0.5f, 0.5f, 0.5f, -0.5f, 0.5f, 0.5f, 0.5f,
                0.5f, -0.5f, -0.5f, 0.5f, 0.5f, 0.5f, 0.5f, -0.5f, 0.5f,
                -0.5f, 0.5f, -0.5f, 0.5f, 0.5f, -0.5f, 0.5f, 0.5f, 0.5f,
                -0.5f, 0.5f, -0.5f, 0.5f, 0.5f, 0.5f, -0.5f, 0.5f, 0.5f,
                -0.5f, -0.5f, -0.5f, 0.5f, -0.5f, -0.5f, 0.5f, -0.5f, 0.5f,
                -0.5f, -0.5f, -0.5f, 0.5f, -0.5f, 0.5f, -0.5f, -0.5f, 0.5f,
        };

        ByteBuffer bb = ByteBuffer.allocateDirect(cube.length * 4);
        bb.order(ByteOrder.nativeOrder());
        cubeBuffer = bb.asFloatBuffer();
        cubeBuffer.put(cube);
        cubeBuffer.position(0);

        resetGame();
        lastFrameTime = SystemClock.uptimeMillis();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        float ratio = (float) width / Math.max(height, 1);
        Matrix.frustumM(projection, 0, -ratio, ratio, -1, 1, 2, 80);
        Matrix.setLookAtM(view, 0, 0, 5f, 10f, 0, 0, 0, 0, 1, 0);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        long now = SystemClock.uptimeMillis();
        float dt = Math.min((now - lastFrameTime) / 1000f, 0.033f);
        lastFrameTime = now;

        updatePhysics(dt, now);

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glUseProgram(program);
        GLES20.glEnableVertexAttribArray(posHandle);
        GLES20.glVertexAttribPointer(posHandle, 3, GLES20.GL_FLOAT, false, 12, cubeBuffer);

        drawRoad();
        drawPlayer();
        drawObstacles();

        if (hudListener != null) {
            hudListener.onHudUpdated(score, speed, crashState);
        }
    }

    public void setHudListener(GameHudListener listener) {
        hudListener = listener;
    }

    public void moveLane(int direction) {
        lane = Math.max(0, Math.min(2, lane + direction));
    }

    public void jump() {
        if (!crashState && playerY < 0.01f) {
            velocityY = 8f;
        }
    }

    private void updatePhysics(float dt, long now) {
        laneX += (((lane - 1) * 2.2f) - laneX) * Math.min(1f, dt * 14f);

        if (crashState) {
            if (now - crashTimeMs > 800) {
                resetGame();
            }
            return;
        }

        velocityY -= 20f * dt;
        playerY += velocityY * dt;
        if (playerY < 0f) {
            playerY = 0f;
            velocityY = 0f;
        }

        spawnTimer -= dt;
        if (spawnTimer <= 0f) {
            obstacles.add(new Obstacle(random.nextInt(3), 35f + random.nextFloat() * 15f));
            spawnTimer = Math.max(0.38f, 0.8f - score / 600f);
        }

        Iterator<Obstacle> it = obstacles.iterator();
        while (it.hasNext()) {
            Obstacle o = it.next();
            o.z -= speed * dt;

            boolean laneHit = o.lane == lane;
            boolean zHit = Math.abs(o.z) < 0.8f;
            boolean yHit = playerY < 1.1f;
            if (laneHit && zHit && yHit) {
                crashState = true;
                crashTimeMs = now;
                break;
            }

            if (o.z < -8f) {
                it.remove();
                score += 10;
            }
        }

        speed = Math.min(14.5f, speed + 0.12f * dt);
    }

    private void resetGame() {
        obstacles.clear();
        lane = 1;
        laneX = 0f;
        playerY = 0f;
        velocityY = 0f;
        speed = 6f;
        score = 0;
        crashState = false;
        crashTimeMs = 0L;
        spawnTimer = 1f;
    }

    private void drawRoad() {
        for (int i = 0; i < 3; i++) {
            float x = (i - 1) * 2.2f;
            Matrix.setIdentityM(model, 0);
            Matrix.translateM(model, 0, x, -1.1f, 0f);
            Matrix.scaleM(model, 0, 1.7f, 0.2f, 30f);
            drawCube(0.08f, 0.09f, 0.12f, 1f);
        }
    }

    private void drawPlayer() {
        Matrix.setIdentityM(model, 0);
        Matrix.translateM(model, 0, laneX, playerY, 0f);
        Matrix.scaleM(model, 0, 1f, 1f, 1f);
        drawCube(0.95f, 0.85f, 0.2f, 1f);
    }

    private void drawObstacles() {
        for (Obstacle o : obstacles) {
            float x = (o.lane - 1) * 2.2f;
            Matrix.setIdentityM(model, 0);
            Matrix.translateM(model, 0, x, 0f, o.z);
            Matrix.scaleM(model, 0, 1f, 1.2f, 1f);
            drawCube(0.9f, 0.35f, 0.65f, 1f);
        }
    }

    private void drawCube(float r, float g, float b, float a) {
        Matrix.multiplyMM(tempM, 0, view, 0, model, 0);
        Matrix.multiplyMM(mvp, 0, projection, 0, tempM, 0);
        GLES20.glUniformMatrix4fv(mvpHandle, 1, false, mvp, 0);
        GLES20.glUniform4f(colorHandle, r, g, b, a);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36);
    }

    private int createProgram(String vertexSource, String fragmentSource) {
        int vs = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
        int fs = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
        int p = GLES20.glCreateProgram();
        GLES20.glAttachShader(p, vs);
        GLES20.glAttachShader(p, fs);
        GLES20.glLinkProgram(p);

        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(p, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == 0) {
            String log = GLES20.glGetProgramInfoLog(p);
            GLES20.glDeleteProgram(p);
            throw new RuntimeException("Program link failed: " + log);
        }
        return p;
    }

    private int loadShader(int type, String code) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, code);
        GLES20.glCompileShader(shader);

        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            String log = GLES20.glGetShaderInfoLog(shader);
            GLES20.glDeleteShader(shader);
            throw new RuntimeException("Shader compile failed: " + log);
        }
        return shader;
    }

    private static class Obstacle {
        final int lane;
        float z;

        Obstacle(int lane, float z) {
            this.lane = lane;
            this.z = z;
        }
    }
}
