package com.plus.camera.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

import java.util.ArrayList;

public class HorizontalAverageLayout extends FrameLayout {
    public HorizontalAverageLayout(Context context) {
        this(context, null);
    }

    public HorizontalAverageLayout(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HorizontalAverageLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public HorizontalAverageLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        autoLayout();
    }

    private void autoLayout() {
        int totalCount = getChildCount();
        if (totalCount == 0) return;

        ArrayList<View> visibleChildren = new ArrayList<>();
        for (int i = 0; i < totalCount; i++) {
            View view = getChildAt(i);
            if (view.getVisibility() != GONE) {
                visibleChildren.add(view);
            }
        }

        averageLayoutItems(visibleChildren);
    }

//    // method 1.
//    private void averageLayoutItems(ArrayList<View> children) {
//        int count = children.size();
//
//        int totalWidth = getWidth() - getPaddingStart() - getPaddingEnd();
//        int averageWidth = totalWidth / (count + 1);
//
//        for (int i = 0; i < count; i++) {
//            int centerX = averageWidth * (i + 1);
//            View view = children.get(i);
//
//            int left = centerX - view.getWidth() / 2;
//            int top = view.getTop();
//            int right = left + view.getWidth();
//            int bottom = view.getBottom();
//            view.layout(left, top, right, bottom);
//        }
//    }

    // method 2.
    private void averageLayoutItems(ArrayList<View> children) {
        int count = children.size();

        int totalWidth = getWidth() - getPaddingStart() - getPaddingEnd();
        int averageWidth = totalWidth / count;

        for (int i = 0; i < count; i++) {
            int centerX = averageWidth * i + averageWidth / 2;
            View view = children.get(i);
            int left = centerX - view.getWidth() / 2;
            int top = view.getTop();
            int right = left + view.getWidth();
            int bottom = view.getBottom();
            view.layout(left, top, right, bottom);
        }
    }
}
