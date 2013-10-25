package com.avenwu.rotateview;
/*
 * Copyright (C) 2013 Chaobin Wu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

/**
 * @author chaobin
 * @date 10/23/13.
 */
public class RotationView extends View {
    private final String TAG = "RotationView";
    private final int REFRESH_INTERVAL = 100;
    private final int ROTATING_SPEED = 5;
    private Paint mPaint;
    private Context mContext;
    private Bitmap mRotateBackground;
    private Matrix mMatrix;
    private int mViewHeight;
    private int mViewWidth;
    private int mRotatedDegree;
    private int mBitmapWidth;
    private int mBitmapHeight;
    private int mBitmapResourceId;
    private RefreshProgressRunnable mRefreshRunnable;
    private RotateWorker mRotateWorker;
    private BitmapFactory.Options mOptions;
    private boolean mAutoRotate;
    private boolean mDetached;
    private boolean mRotating;
    private boolean mRotateable = true;
    private float mMaxProgress = 100.0f;


    public RotationView(Context context) {
        this(context, null);
    }

    public RotationView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RotationView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RotationView, defStyle, 0);
        mBitmapResourceId = a.getResourceId(R.styleable.RotationView_rotateBackground, android.R.drawable.sym_def_app_icon);
        mAutoRotate = a.getBoolean(R.styleable.RotationView_autoRefresh, false);
        a.recycle();
        intitData(context);
    }

    private void intitData(Context context) {
        mContext = context;
        mMatrix = new Matrix();
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setFilterBitmap(true);
        mRefreshRunnable = new RefreshProgressRunnable();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mRotateBackground == null) {
            mOptions = new BitmapFactory.Options();
            mOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeResource(mContext.getResources(), mBitmapResourceId, mOptions);
            int imageWidth = mOptions.outWidth;
            int imageHeight = mOptions.outHeight;
            Log.d(TAG, "imageHeight = " + imageHeight + ", imageWidth =" + imageWidth);
            int inSampleSize = 1;
            if (imageHeight > mViewHeight || imageWidth > mViewWidth) {
                final int heightRatio = Math.round(imageHeight / (float) mViewHeight);
                final int widthRatio = Math.round(imageWidth / (float) mViewWidth);
                inSampleSize = Math.max(heightRatio, widthRatio);
                Log.d(TAG, "heightRatio =" + heightRatio + ", widthRatio =" + widthRatio + ", inSampleSize =" + inSampleSize);
            }
            mOptions.inSampleSize = inSampleSize;
            mOptions.inJustDecodeBounds = false;
            mRotateBackground = BitmapFactory.decodeResource(mContext.getResources(), mBitmapResourceId, mOptions);
            mBitmapWidth = mOptions.outWidth;
            mBitmapHeight = mOptions.outHeight;
            if (mAutoRotate && mRotateable) startAnimate();
        }
        Log.d(TAG, "RotationView = " + mRotatedDegree);
        canvas.translate(Math.abs((mViewWidth - mBitmapWidth) / 2), Math.abs((mViewHeight - mBitmapHeight) / 2));
        mMatrix.setRotate(mRotatedDegree, mBitmapWidth / 2, mBitmapHeight / 2);
        canvas.drawBitmap(mRotateBackground, mMatrix, mPaint);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mViewHeight = View.MeasureSpec.getSize(heightMeasureSpec);
        mViewWidth = View.MeasureSpec.getSize(widthMeasureSpec);
        Log.d(TAG, "mViewHeight =" + mViewHeight + ", mViewWidth =" + mViewWidth);
    }

    private class RefreshProgressRunnable implements Runnable {
        public void run() {
            synchronized (RotationView.this) {
                mRotatedDegree += ROTATING_SPEED;
                if (mRotatedDegree > 360) mRotatedDegree -= 360;
                invalidate();
            }
        }
    }

    private class RotateWorker extends Thread {
        private boolean cancelled;

        @Override
        public void run() {
            while (!cancelled && !mDetached && getVisibility() == View.VISIBLE) {
                Log.d(TAG, "post refresh request");
                post(mRefreshRunnable);
                try {
                    Thread.sleep(REFRESH_INTERVAL);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        public void cancel() {
            try {
                cancelled = true;
                this.interrupt();
                Log.d(TAG, "cancel RotateWorker " + this.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void startAnimate() {
        if (mRotateWorker != null) mRotateWorker.cancel();
        mRotateable = true;
        mRotating = true;
        mRotateWorker = new RotateWorker();
        mRotateWorker.start();
    }

    public void stopAnimate() {
        if (mRotateWorker != null) mRotateWorker.cancel();
        mRotateWorker = null;
        mRotating = false;
    }

    public void resetAnimate() {
        mRotatedDegree = 0;
        if (mRotateWorker != null) mRotateWorker.cancel();
        invalidate();
    }

    public void setProgress(int progress) {
        mRotatedDegree = (int) ((progress + 0.5) / mMaxProgress * 360);
        startAnimate();
    }

    /**
     * start/stop to animate according to current state
     */
    public void toggle() {
        if (!mRotating) startAnimate();
        else stopAnimate();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Log.d(TAG, "detached window");
        mDetached = true;
        stopAnimate();
        if (mRefreshRunnable != null) removeCallbacks(mRefreshRunnable);
    }
}
