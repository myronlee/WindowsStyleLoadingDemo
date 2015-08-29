package com.example.myronlg.windowsstyleloadingdemo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.security.auth.login.LoginException;

/**
 * Created by myron.lg on 2015/7/9.
 */
public class LoadingDrawable extends ShapeDrawable {

    /**
     * A list that contains the cx of each point.
     */
    private List<Float> cx;

    /**
     * The cy of every point is the same.
     */
    private float cy;

    /**
     * The radius of every point is the same.
     */
    private float radius;

    /**
     * The paints that used to draw each point.
     */
    private List<Paint> paints;

    /**
     * The length that point transfer to enter and exit.
     */
    private float dx;

    private List<Float> dxs;

    /**
     * The offset between each point.
     */
    private float offset;

    /**
     * Used in animation.
     */
    private long startMillis = -1;

    /**
     * Used to make translation more smooth
     */
    private Interpolator enterInterpolator, exitInterpolator;

    /**
     * The time one point enter or exit
     */
    private long duration;

    /**
     * The moving velocity of the point which is not entering or exiting
     */
    private float v = 0.04F;

    /**
     * The number of points
     */
    private int pointNum;
    private float cxOffest;

    private boolean inited = false;


    private void init(){
        pointNum = 4;
        cx = new ArrayList<>(Collections.nCopies(pointNum, 0.0F));

        Paint paint = new Paint();
        paint.setDither(true);
        paint.setAntiAlias(true);
        paint.setColor(Color.parseColor("#00BCD4"));
//        paint.setAlpha(0);

        paints = new ArrayList<>();
        for (int i = 0; i < pointNum; i++) {
            paints.add(new Paint(paint));
        }

        enterInterpolator = new DecelerateInterpolator(1.5F);
        exitInterpolator = new AccelerateInterpolator(2.0F);

        duration = 600;

        int width = getIntrinsicWidth();
        int height = getIntrinsicHeight();
        cy = height / 2;
        radius = height / 3;
        dx = width / 3;

        cxOffest = (width - 2 * dx - v * duration * 3) * 0.5F;

        offset = radius * 3;

//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                while (true) {
//                    updateDrawParams();
//                }
//            }
//        }).start();
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);


        if (!inited){
            init();
            inited = true;
        }

//        canvas.drawRect(200, 0, 600, 1000, paints.get(0));

        for (int i = 0; i < cx.size(); i++) {
            Log.e("", i + "  " + cx.get(i));
            canvas.drawCircle(cx.get(i), cy, radius, paints.get(i));
        }
    }

    //Thread + Handler , update draw parameters on background thread, invalidate on ui thread
    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            invalidateSelf();
            return true;
        }
    });

    private void updateDrawParams(){
        long currentMillis = System.currentTimeMillis();
        if (startMillis == -1){
            startMillis = currentMillis;
        }

        // (pointNum * 2 * duration) is a cycle
        long passMills = (currentMillis - startMillis) % (pointNum * 2 * duration);

        // updateDrawParams each point's cx and alpha
        for (int i = 0; i < cx.size(); i++) {
            long step = passMills / duration;

            float animationFraction = (passMills % duration) * 1.0F / duration;


            float enterX = 0;
            float transX = 0;
            float exitX = 0;


            if (step < 4) { // entering half
                if (i < step) {

                    enterX = dx - i*offset + i*duration*v;
                    transX = (passMills - (i + 1) * duration) * v;
                    exitX = 0;
                    paints.get(i).setAlpha(255);
                } else if (i == step) {
                    float interpolatedFraction = enterInterpolator.getInterpolation(animationFraction);

                    enterX = interpolatedFraction*dx - i*offset + i*duration*v;
                    transX = 0;
                    exitX = 0;
                    paints.get(i).setAlpha((int) (255*interpolatedFraction));
                } else {

                    enterX = 0;
                    transX = 0;
                    exitX =0;
                    paints.get(i).setAlpha(0);
                }
            } else { // exiting half
                if (i < step-4){

                    enterX = dx - i*offset + i*duration*v;
                    transX = (passMills - (i + 1) * duration) * v;
                    exitX = dx;
                    paints.get(i).setAlpha(0);
                } else if (i == step-4){
                    float interpolatedFraction = exitInterpolator.getInterpolation(animationFraction);

                    enterX = dx - i*offset + i*duration*v;
                    transX = (passMills - (i + 1) * duration) * v;
                    exitX = interpolatedFraction * dx;
                    paints.get(i).setAlpha((int) (255*(1-interpolatedFraction)));
                } else {

                    enterX = dx - i*offset + i*duration*v;
                    transX = (passMills - (i + 1) * duration) * v;
                    exitX = 0;
                    paints.get(i).setAlpha(255);
                }
            }
            cx.set(i, cxOffest + enterX + transX + exitX);
        }
        handler.sendEmptyMessage(0);
    }

    @Override
    public void setAlpha(int i) {

    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {

    }

    @Override
    public int getOpacity() {
        return 0;
    }
}
