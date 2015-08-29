package com.example.myronlg.windowsstyleloadingdemo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by myron.lg on 2015/7/9.
 */
public class LoadingView extends View {

    /**
     * The cy of every point is the same.
     */
    private float cy;

    /**
     * The radius of every point is the same.
     */
    private float radius;

    /**
     * Used in animation.
     */
    private long startMillis = -1;
    private long lastMills = -1;

    /**
     * Used to make translation more smooth
     */
    private Interpolator enterInterpolator, exitInterpolator;


    /**
     * The moving velocity of the point which is not entering or exiting
     */
    private float v;

    /**
     * The number of points
     */
    private int pointNum;

    private HandlerThread workerThread;
    private Handler workerHandler;



    private long enterDuration = 600;

    private long moveDuration = 1800;

    private long exitDuraion = 600;

    private long cycle;

    private float enterDistance;
    private float exitDistance;
    private float moveVelocity;

    private float offsetBetweenPoint;
    private List<LoadingPoint> points;

    private float uniformCxOffset;

    public LoadingView(Context context) {
        super(context);
        init();
    }

    public LoadingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public LoadingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }


    private void init() {
        pointNum = 4;

        enterInterpolator = new DecelerateInterpolator(1.6F);
        exitInterpolator = new AccelerateInterpolator(1.2F);

        cycle = enterDuration + moveDuration + exitDuraion + enterDuration * (pointNum - 1);

        points = new ArrayList<>(pointNum);
        for (int i = 0; i < pointNum; i++) {
            LoadingPoint point = new LoadingPoint(i);
            points.add(point);
        }

        workerThread = new HandlerThread("workerThread");
        workerThread.start();
        workerHandler = new Handler(workerThread.getLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(Message message) {
                updateDrawParams();
                postInvalidate();
                return true;
            }
        });
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        initSize();
    }

    private void initSize() {
        cy = getMeasuredHeight() / 2;
        radius = getMeasuredHeight() / 3;

        enterDistance = getMeasuredWidth() * 0.425F;
        float moveDistance = getMeasuredWidth() * 0.15F;
        exitDistance = getMeasuredWidth() * 0.425F;

        moveVelocity = moveDistance / moveDuration;

        uniformCxOffset = - moveDistance * 0.5F;

        offsetBetweenPoint = moveDistance / 3;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        for (int i = 0; i < points.size(); i++){
            points.get(i).draw(canvas);
        }
        workerHandler.sendEmptyMessage(0);//update draw params on worker thread
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        workerThread.quit();
    }

    private void updateDrawParams() {
        long currentMillis = System.currentTimeMillis();
        if (startMillis == -1) {
            startMillis = currentMillis;
        }

        if (lastMills == -1) {
            lastMills = currentMillis;
        } else {
            long timeDelta = currentMillis - lastMills;
            if (timeDelta < 16) {
                try {
                    Thread.sleep(16 - timeDelta);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        //acquire current millis again, because this thread may sleep for not invalidating too frequently
        currentMillis = System.currentTimeMillis();

        long passMills = (currentMillis - startMillis) % cycle;

        for (int i = 0; i < points.size(); i++){
            points.get(i).update(passMills);
        }

        lastMills = currentMillis;
    }

    private class LoadingPoint {

        private int index;
        private float translateX;
        private boolean visible;

        private float cx;
        private Paint paint;

        public LoadingPoint(int i) {
            index = i;
            translateX = i * offsetBetweenPoint;
            paint = new Paint();
            paint.setDither(true);
            paint.setAntiAlias(true);
            paint.setColor(Color.parseColor("#455A64"));
            paint.setAlpha(0);
        }

        public void update(long passMills) {
            //做时间偏移
            passMills = passMills - index * enterDuration;

            //还没有出现
            if (passMills < 0) {
                visible = false;
                return;
            }
            visible = true;

            float enterX = 0;
            float exitX  = 0;

            if (passMills < enterDuration) {
                //enter
                float enterFraction = ((float) passMills) / enterDuration;
                float interpolatedEnterFraction = enterInterpolator.getInterpolation(enterFraction);
                enterX = interpolatedEnterFraction * enterDistance;

                exitX = 0;

                paint.setAlpha((int) (255 * interpolatedEnterFraction));
            } else if (passMills < enterDuration + moveDuration) {
                enterX = enterDistance;

                exitX = 0;

                paint.setAlpha(255);
            } else {
                enterX = enterDistance;

                float exitFraction = ((float) (passMills - enterDuration - moveDuration)) / exitDuraion;
                float interpolatedExitFraction = exitInterpolator.getInterpolation(exitFraction);
                exitX = interpolatedExitFraction * exitDistance;

                paint.setAlpha((int) (255 * (1 - interpolatedExitFraction)));
            }

            //move
            float moveX = passMills * moveVelocity;

            cx = enterX + moveX + exitX;
        }

        public void draw(Canvas canvas) {
            if (visible) {
                canvas.save();
                //做位置偏移
                canvas.translate(translateX + uniformCxOffset, 0);
                canvas.drawCircle(cx, cy, radius, paint);
                canvas.restore();
            }
        }
    }
}
