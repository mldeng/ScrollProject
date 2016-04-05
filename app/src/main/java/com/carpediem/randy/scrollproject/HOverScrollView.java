package com.carpediem.randy.scrollproject;

import android.content.Context;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewParent;
import android.widget.LinearLayout;
import android.widget.OverScroller;

/**
 * Created by randy on 16-4-5.
 */
public class HOverScrollView extends LinearLayout{
    private final static int INVALID_ID = -1;
    private int mActivePointerId = INVALID_ID;
    private float mLastY=0;


    private int mSecondaryPointerId = INVALID_ID;
    private float mSecondaryLastX=0;
    private float mSecondaryLastY =0;

    private boolean mIsBeingDragged = false;

    private int mTouchSlop;
    private int mMinFlingSpeed;
    private int mMaxFlingSpeed;
    private int mOverFlingDistance;
    private int mOverScrollDistance;
    private OverScroller mScroller;
    private VelocityTracker mVelocityTracker;

    private int mScrollX = 0;
    private int mScrollY = 0;

    public HOverScrollView(Context context) {
        super(context);
        init(context);
    }

    public HOverScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public HOverScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }


    private void init(Context context) {
        ViewConfiguration configuration = ViewConfiguration.get(context);
        mTouchSlop = configuration.getScaledTouchSlop();
        mMinFlingSpeed = configuration.getScaledMinimumFlingVelocity();
        mMaxFlingSpeed = configuration.getScaledMaximumFlingVelocity();
        mOverFlingDistance = configuration.getScaledOverflingDistance();
        mOverScrollDistance = configuration.getScaledOverscrollDistance();
        mScroller = new OverScroller(context);
    }

    private void initVelocityTrackerIfNotExist() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
    }
    private void recycleVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
        }
    }
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        final int action = MotionEventCompat.getActionMasked(ev);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                int index = MotionEventCompat.getActionIndex(ev);
                float y = MotionEventCompat.getY(ev,index);
                initVelocityTrackerIfNotExist();
                mVelocityTracker.addMovement(ev);
                mLastY = y;
                mActivePointerId = MotionEventCompat.getPointerId(ev,index);
                //分两种情况，一种是初始动作，一个是界面正在滚动，down触摸停止滚动
                mIsBeingDragged = mScroller.isFinished();
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                index = MotionEventCompat.getActionIndex(ev);
                mSecondaryPointerId = MotionEventCompat.getPointerId(ev,index);
                mSecondaryLastY = MotionEventCompat.getY(ev,index);
                break;

            case MotionEvent.ACTION_MOVE:
                index = MotionEventCompat.findPointerIndex(ev,mActivePointerId);
                y = MotionEventCompat.getY(ev,index);
                final float yDiff  = Math.abs(y-mLastY);
                if (yDiff > mTouchSlop) {
                    //是滚动状态啦
                    mIsBeingDragged = true;
                    mLastY = y;
                    initVelocityTrackerIfNotExist();
                    mVelocityTracker.addMovement(ev);

                    final ViewParent parent = getParent();
                    if (parent != null) {
                        parent.requestDisallowInterceptTouchEvent(true);
                    }
                }

                break;
            case MotionEvent.ACTION_POINTER_UP:
                index = MotionEventCompat.getActionIndex(ev);
                int curId = MotionEventCompat.getPointerId(ev,index);
                if (curId == mActivePointerId) {
                    mActivePointerId = mSecondaryPointerId;
                    mLastY = mSecondaryLastY;
                    mVelocityTracker.clear();
                } else {
                    mSecondaryPointerId = INVALID_ID;
                    mSecondaryLastY = 0;
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                mIsBeingDragged = false;
                mActivePointerId = INVALID_ID;
                recycleVelocityTracker();
                break;
            default:
        }
        return mIsBeingDragged;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mScroller == null) {
            //
            return false;
        }

        initVelocityTrackerIfNotExist();
        // ScrollView中设置了offsetLocation,这里需要设置吗？
        int action = MotionEventCompat.getActionMasked(event);
        int index = -1;
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mIsBeingDragged = mScroller.isFinished();
                if (mIsBeingDragged) {

                }
                if (!mScroller.isFinished()) { //fling
                    mScroller.abortAnimation();
                }
                index  = MotionEventCompat.getActionIndex(event);
                mActivePointerId = MotionEventCompat.getPointerId(event,index);
                mLastY = MotionEventCompat.getY(event,index);

                break;
            case MotionEvent.ACTION_MOVE:
                if (mActivePointerId == INVALID_ID) {
                    break;
                }
                index = MotionEventCompat.findPointerIndex(event,mActivePointerId);
                if (index == -1) {
                    break;
                }
                float y = MotionEventCompat.getY(event,index);
                float deltaY = mLastY - y;

                if (!mIsBeingDragged && Math.abs(deltaY) > mTouchSlop) {
                    requestParentDisallowInterceptTouchEvent();
                    mIsBeingDragged  = true;
                    // 减少滑动的距离
                    if (deltaY > 0) {
                        deltaY -= mTouchSlop;
                    } else {
                        deltaY += mTouchSlop;
                    }
                }

                if (mIsBeingDragged) {
                    //直接滑动
                    Log.e("TEST",deltaY+"");
                    overScrollBy(0,(int)deltaY,0,getScrollY(),0,getScrollRange(),0,mOverScrollDistance,true);
                    mLastY = y;
                }
                if (mSecondaryPointerId != INVALID_ID) {
                    index = MotionEventCompat.findPointerIndex(event,mSecondaryPointerId);
                    mSecondaryLastY = MotionEventCompat.getY(event,index);
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                endDrag();
                break;
            case MotionEvent.ACTION_UP:
                if (mIsBeingDragged) {
                    mVelocityTracker.computeCurrentVelocity(1000,mMaxFlingSpeed);
                    int initialVelocity = (int)mVelocityTracker.getYVelocity(mActivePointerId);
                    if (Math.abs(initialVelocity) > mMinFlingSpeed) {
                        // fling
                        doFling(-initialVelocity);
                    }
                    endDrag();
                }
                break;
            default:
        }
        if (mVelocityTracker != null) {
            mVelocityTracker.addMovement(event);
        }
        return true;
    }


    private void doFling(int speed) {
        if (mScroller == null) {
            return;
        }
        Log.e("TEST","dddd");
        mScroller.fling(0,getScrollY(),0,speed,0,0,-500,10000);
        invalidate();
    }

    private void endDrag() {
        mIsBeingDragged = false;
        recycleVelocityTracker();
        mActivePointerId = INVALID_ID;
        mLastY = 0;
    }
    private void requestParentDisallowInterceptTouchEvent() {
        final ViewParent parent = getParent();
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(true);
        }
    }
    @Override
    protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
        Log.e("TEST","onOverScrolled"+scrollX);
        if (!mScroller.isFinished()) {
            int oldX = mScrollX;
            int oldY = mScrollY;
            mScrollX = scrollX;
            mScrollY = scrollY;
            onScrollChanged(mScrollX,mScrollY,oldX,oldY);

            if (clampedY) {
                mScroller.springBack(mScrollX,mScrollY,0,0,0,getScrollRange());
            }
        } else {
            // TouchEvent中的overScroll调用
            super.scrollTo(scrollX,scrollY);
        }
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            int oldX = mScrollX;
            int oldY = mScrollY;
            int x = mScroller.getCurrX();
            int y = mScroller.getCurrY();
            int range = getScrollRange();
            if (oldX != x || oldY != y) {
                overScrollBy(x-oldX,y-oldY,oldX,oldY,0,range,0,mOverFlingDistance,false);
            }
        }
    }
    private int getScrollRange() {
        int scrollRange = 0;
        if (getChildCount() >0) {
            View child = getChildAt(0);
            scrollRange= Math.max(0,child.getHeight()-(getHeight()-getPaddingBottom()-getPaddingTop()));
        }
        return 10000;
    }
}
