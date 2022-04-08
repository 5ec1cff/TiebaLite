package com.huanchengfly.tieba.post.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import com.bm.library.PhotoView;
import com.huanchengfly.tieba.post.interfaces.OnDispatchTouchEvent;
import com.huanchengfly.tieba.post.interfaces.OnPhotoErrorListener;

public class MyPhotoView extends PhotoView {
    public static final String TAG = MyPhotoView.class.getSimpleName();

    protected OnPhotoErrorListener onPhotoErrorListener;
    protected OnDispatchTouchEvent onDispatchTouchEvent;
    private int startX;

    public MyPhotoView(Context context) {
        super(context);
    }

    public MyPhotoView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MyPhotoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public OnDispatchTouchEvent getOnDispatchTouchEvent() {
        return onDispatchTouchEvent;
    }

    public MyPhotoView setOnDispatchTouchEvent(OnDispatchTouchEvent onDispatchTouchEvent) {
        this.onDispatchTouchEvent = onDispatchTouchEvent;
        return this;
    }

    public OnPhotoErrorListener getOnPhotoErrorListener() {
        return onPhotoErrorListener;
    }

    public MyPhotoView setOnPhotoErrorListener(OnPhotoErrorListener onPhotoErrorListener) {
        this.onPhotoErrorListener = onPhotoErrorListener;
        return this;
    }

    int shouldDisallowIntercept = 0;

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (getOnDispatchTouchEvent() != null) {
            getOnDispatchTouchEvent().onDispatchTouchEvent(event);
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startX = (int) event.getX();
                getParent().requestDisallowInterceptTouchEvent(true);
                shouldDisallowIntercept = 0;
                break;
            case MotionEvent.ACTION_MOVE:
                int endX = (int) event.getX();
                if (shouldDisallowIntercept == 0) {
                    if (event.getPointerCount() > 1) shouldDisallowIntercept = 1;
                    else shouldDisallowIntercept = canScrollHorizontally(startX - endX) ? 1 : 2;
                }
                Log.d("MyPhotoView", "shouldDisallowIntercept=" + shouldDisallowIntercept);
                getParent().requestDisallowInterceptTouchEvent(shouldDisallowIntercept == 1);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
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
