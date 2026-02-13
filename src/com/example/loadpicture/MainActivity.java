package com.example.loadpicture;

import java.util.Locale;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.example.loadpicture.game.GameSurfaceView;
import com.example.loadpicture.game.RunnerRenderer;

public class MainActivity extends Activity implements RunnerRenderer.GameHudListener {

    private GameSurfaceView gameSurfaceView;
    private TextView hudView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FrameLayout root = new FrameLayout(this);

        gameSurfaceView = new GameSurfaceView(this);
        root.addView(gameSurfaceView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        hudView = new TextView(this);
        hudView.setTextColor(Color.WHITE);
        hudView.setShadowLayer(6f, 2f, 2f, Color.BLACK);
        hudView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        hudView.setPadding(24, 24, 24, 24);
        hudView.setGravity(Gravity.START);
        hudView.setText("Score: 0\nSpeed: 0.0");

        FrameLayout.LayoutParams hudParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.START);
        root.addView(hudView, hudParams);

        setContentView(root);

        gameSurfaceView.setHudListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        gameSurfaceView.onResume();
    }

    @Override
    protected void onPause() {
        gameSurfaceView.onPause();
        super.onPause();
    }

    @Override
    public void onHudUpdated(final int score, final float speed, final boolean crashed) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                StringBuilder builder = new StringBuilder();
                builder.append(String.format(Locale.US, "Score: %d", score));
                builder.append('\n');
                builder.append(String.format(Locale.US, "Speed: %.1f", speed));
                if (crashed) {
                    builder.append("\nCrash! Auto restart...");
                }
                hudView.setText(builder.toString());
            }
        });
    }
}
