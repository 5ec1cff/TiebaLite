package com.huanchengfly.tieba.post.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.bm.library.PhotoView;
import com.huanchengfly.tieba.post.interfaces.OnDispatchTouchEvent;
import com.huanchengfly.tieba.post.interfaces.OnPhotoErrorListener;

public class MyPhotoView extends PhotoView {
    public static final String TAG = MyPhotoView.class.getSimpleName();

    protected OnPhotoErrorListener onPhotoErrorListener;
    protected OnDispatchTouchEvent onDispatchTouchEvent;
    private int startX, startY;
    private final Handler mHandler;
    private final Runnable mLongClickRunnable = this::performLongClick;

    public MyPhotoView(Context context) {
        this(context, null, 0);
    }

    public MyPhotoView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MyPhotoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mHandler = new Handler(Looper.getMainLooper());
    }

    public OnDispatchTouchEvent getOnDispatchTouchEvent() {
        return onDispatchTouchEvent;
    }

    public void setOnDispatchTouchEvent(OnDispatchTouchEvent onDispatchTouchEvent) {
        this.onDispatchTouchEvent = onDispatchTouchEvent;
    }

    public OnPhotoErrorListener getOnPhotoErrorListener() {
        return onPhotoErrorListener;
    }

    public void setOnPhotoErrorListener(OnPhotoErrorListener onPhotoErrorListener) {
        this.onPhotoErrorListener = onPhotoErrorListener;
    }

    private boolean mIsScrollingHorizontally = false;

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (getOnDispatchTouchEvent() != null) {
            getOnDispatchTouchEvent().onDispatchTouchEvent(event);
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startX = (int) event.getX();
                startY = (int) event.getY();
                mIsScrollingHorizontally = false;
                getParent().requestDisallowInterceptTouchEvent(true);
                mHandler.postDelayed(mLongClickRunnable, 500L);
                break;
            case MotionEvent.ACTION_MOVE:
                mHandler.removeCallbacks(mLongClickRunnable);
                boolean shouldDisallowIntercept = false;
                if (mIsScrollingHorizontally || event.getPointerCount() > 1) shouldDisallowIntercept = true;
                else {
                    int endX = (int) event.getX();
                    int endY = (int) event.getY();
                    int disX = Math.abs(endX - startX);
                    int disY = Math.abs(endY - startY);
                    if (disX > disY) {
                        if (canScrollHorizontally(startX - endX)) {
                            mIsScrollingHorizontally = true;
                            shouldDisallowIntercept = true;
                        }
                    } else {
                        shouldDisallowIntercept = true;
                    }
                }
                getParent().requestDisallowInterceptTouchEvent(shouldDisallowIntercept);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mHandler.removeCallbacks(mLongClickRunnable);
                getParent().requestDisallowInterceptTouchEvent(false);
                break;
        }
        return super.dispatchTouchEvent(event);
    }

    @Override
    public void draw(Canvas canvas) {
        try {
            super.draw(canvas);
        } catch (Exception e) {
            e.printStackTrace();
            if (getOnPhotoErrorListener() != null) {
                getOnPhotoErrorListener().onError(e);
            }
        }
    }
}
