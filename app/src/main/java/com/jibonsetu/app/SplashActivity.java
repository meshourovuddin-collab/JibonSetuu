package com.jibonsetu.app;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

public class SplashActivity extends Activity {

    ImageView ivLogo;
    TextView tvAppName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_splash);

        ivLogo = findViewById(R.id.ivLogo);
        tvAppName = findViewById(R.id.tvAppName);

        Animation logoAnimation =
                AnimationUtils.loadAnimation(
                        this,
                        R.anim.logo_animation
                );

        Animation textAnimation =
                AnimationUtils.loadAnimation(
                        this,
                        R.anim.text_animation
                );

        ivLogo.startAnimation(logoAnimation);

        logoAnimation.setAnimationListener(
                new Animation.AnimationListener() {

                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {

                        tvAppName.setVisibility(
                                android.view.View.VISIBLE
                        );

                        tvAppName.startAnimation(textAnimation);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }
                }
        );

        new android.os.Handler().postDelayed(() -> {

            Intent intent = new Intent(
                    SplashActivity.this,
                    MainActivity.class
            );

            startActivity(intent);
            finish();

        }, 3500);
    }
}