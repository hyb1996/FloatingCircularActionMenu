package com.stardust.floatingcircularactionmenu;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PointF;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.ScaleAnimation;
import android.widget.FrameLayout;

/**
 * Created by Stardust on 2017/9/25.
 */

public class CircularActionMenu extends FrameLayout {

    public interface OnStateChangeListener {
        void onExpanding(CircularActionMenu menu);

        void onExpanded(CircularActionMenu menu);

        void onCollapsing(CircularActionMenu menu);

        void onCollapsed(CircularActionMenu menu);

        void onMeasured(CircularActionMenu menu);
    }

    private PointF[] mItemExpandedPositionOffsets;
    private OnStateChangeListener mOnStateChangeListener;
    private boolean mExpanded;
    private boolean mExpanding = false;
    private boolean mCollapsing = false;
    private float mRadius = 200;
    private float mAngle = (float) Math.toRadians(90);
    private long mDuration = 200;
    private int mExpandedHeight = -1;
    private int mExpandedWidth = -1;
    private final Interpolator mInterpolator = new FastOutSlowInInterpolator();


    public CircularActionMenu(@NonNull Context context) {
        super(context);
        init(null);
    }

    public CircularActionMenu(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public CircularActionMenu(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        if (attrs == null)
            return;
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.CircularActionMenu);
        mRadius = a.getDimensionPixelSize(R.styleable.CircularActionMenu_cam_radius, (int) mRadius);
        int angleInDegree = a.getInt(R.styleable.CircularActionMenu_cam_angle, 0);
        if (angleInDegree != 0) {
            mAngle = (float) Math.toRadians(angleInDegree);
        }
    }

    public float getRadius() {
        return mRadius;
    }

    public void setRadius(float radius) {
        mRadius = radius;
    }

    public float getAngle() {
        return mAngle;
    }

    public void setAngle(float angle) {
        mAngle = angle;
    }

    public void expand(int direction) {
        mExpanding = true;
        Animator.AnimatorListener listener = new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mExpanding = false;
                mExpanded = true;
                if (mOnStateChangeListener != null) {
                    mOnStateChangeListener.onExpanded(CircularActionMenu.this);
                }
            }
        };
        ScaleAnimation scaleAnimation = createScaleAnimation(0, 1);
        direction = (direction == Gravity.RIGHT ? 1 : -1);
        for (int i = 0; i < getItemCount(); i++) {
            View item = getItemAt(i);
            item.setVisibility(VISIBLE);
            item.animate()
                    .translationXBy(direction * mItemExpandedPositionOffsets[i].x)
                    .translationYBy(direction * mItemExpandedPositionOffsets[i].y)
                    .setListener(listener)
                    .setDuration(mDuration)
                    .start();
            item.startAnimation(scaleAnimation);
        }
        if (mOnStateChangeListener != null) {
            mOnStateChangeListener.onExpanding(this);
        }
    }

    private ScaleAnimation createScaleAnimation(float fromScale, float toScale) {
        ScaleAnimation scaleAnimation = new ScaleAnimation(fromScale, toScale, fromScale, toScale, Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0.5f);
        scaleAnimation.setDuration(mDuration);
        scaleAnimation.setFillAfter(true);
        scaleAnimation.setInterpolator(mInterpolator);
        return scaleAnimation;
    }

    public View getItemAt(int i) {
        return getChildAt(i);
    }

    public void collapse() {
        Animator.AnimatorListener listener = new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mCollapsing = false;
                mExpanded = false;
                for (int i = 0; i < getItemCount(); i++) {
                    getItemAt(i).setVisibility(GONE);
                }
                if (mOnStateChangeListener != null) {
                    mOnStateChangeListener.onCollapsed(CircularActionMenu.this);
                }
            }
        };
        mCollapsing = true;
        ScaleAnimation scaleAnimation = createScaleAnimation(1, 0);
        for (int i = 0; i < getItemCount(); i++) {
            View item = getItemAt(i);
            item.animate()
                    .translationX(0)
                    .translationY(0)
                    .setListener(listener)
                    .setDuration(mDuration)
                    .setInterpolator(mInterpolator)
                    .start();
            item.startAnimation(scaleAnimation);
        }
        if (mOnStateChangeListener != null) {
            mOnStateChangeListener.onCollapsing(this);
        }
    }

    public void setOnStateChangeListener(OnStateChangeListener onStateChangeListener) {
        mOnStateChangeListener = onStateChangeListener;
    }

    public boolean isExpanded() {
        return mExpanded;
    }

    public boolean isExpanding() {
        return mExpanding;
    }

    public boolean isCollapsing() {
        return mCollapsing;
    }


    public int getItemCount() {
        return getChildCount();
    }

    private void calcExpandedPositions() {
        mItemExpandedPositionOffsets = new PointF[getItemCount()];
        double averageAngle = mAngle / (getItemCount() - 1);
        for (int i = 0; i < getItemCount(); i++) {
            double angle = -mAngle / 2 + i * averageAngle;
            mItemExpandedPositionOffsets[i] = new PointF((float) (mRadius * Math.cos(angle)),
                    (float) (mRadius * Math.sin(angle)));
        }
    }

    private void calcExpandedSize() {
        int maxX = 0;
        int maxY = 0;
        int minY = Integer.MAX_VALUE;
        for (int i = 0; i < getItemCount(); i++) {
            View item = getItemAt(i);
            int x = (int) (mItemExpandedPositionOffsets[i].x + item.getMeasuredWidth());
            int y = (int) (mItemExpandedPositionOffsets[i].y - item.getMeasuredHeight());
            maxX = Math.max(x, maxX);
            // FIXME: 2017/9/26 这样算出来的高度略大
            maxY = Math.max((int) (mItemExpandedPositionOffsets[i].y + item.getMeasuredHeight()), maxY);
            minY = Math.min((int) (mItemExpandedPositionOffsets[i].y - item.getMeasuredHeight()), minY);
        }
        mExpandedWidth = maxX;
        mExpandedHeight = maxY - minY;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        measureChildren(widthMeasureSpec, heightMeasureSpec);
        calcExpandedPositions();
        if (mExpandedHeight == -1 || mExpandedWidth == -1) {
            calcExpandedSize();
        }
        setMeasuredDimension(mExpandedWidth, mExpandedHeight);
        if (mOnStateChangeListener != null) {
            mOnStateChangeListener.onMeasured(this);
        }
    }

    @Override
    protected void measureChildren(int widthMeasureSpec, int heightMeasureSpec) {
        for (int i = 0; i < getChildCount(); ++i) {
            final View child = getChildAt(i);
            measureChild(child, widthMeasureSpec, heightMeasureSpec);
        }
    }

    public int getExpandedHeight() {
        return mExpandedHeight;
    }

    public int getExpandedWidth() {
        return mExpandedWidth;
    }

}
