package com.palm.demo.custom;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.api.stream.bean.BBox;

import java.util.concurrent.locks.ReentrantLock;

public class DtRectRoiView extends View {
  private Paint mPaint;
  private ReentrantLock mLock = new ReentrantLock();
  private Rect mRect = new Rect();
  public final static String RECT_TAG = DtRectRoiView.class.getSimpleName();
  private int mWidgetWidth = 120;
  private int mWidgetHeight = 282;
  /**
   * ir pic output origin size:400x640
   */
  private int mSourceWidth = 480;
  private int mSourceHeight = 848;
  private int mRectLeft;
  private int mRectRight;
  private int mRectTop;
  private int mRectBottom;
  private boolean recordRealValue;

  public DtRectRoiView(Context context) {
    super(context);
    mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    mPaint.setColor(Color.GREEN);
    mPaint.setStyle(Paint.Style.STROKE);
    mPaint.setStrokeWidth(2f);
    mPaint.setAlpha(100);
  }

  public DtRectRoiView(Context context, AttributeSet attrs) {
    super(context, attrs);
    mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    mPaint.setColor(Color.GREEN);
    mPaint.setStyle(Paint.Style.STROKE);
    mPaint.setStrokeWidth(5f);
    mPaint.setAlpha(180);
  }

  public void resizeSource(int width,int  height) {
    mSourceWidth = width;
    mSourceHeight = height;
  }

  public void clearFaceRect() {
    mLock.lock();
    try {
      this.mRectLeft = 0;
      this.mRectTop = 0;
      this.mRectRight = 0;
      this.mRectBottom = 0;
    } finally {
      mLock.unlock();
    }
    postInvalidate();
  }

  public void setRect(BBox box, int tag) {
//    Log.d(RECT_TAG, "setRect ret = " + box.toString());
    if (0 == tag) {
      mPaint.setColor(Color.GREEN);
    } else if (1 == tag) {
      mPaint.setColor(Color.RED);
    }
    mLock.lock();
    try {
      if (box == null) {
        this.mRectLeft = 0;
        this.mRectTop = 0;
        this.mRectRight = 0;
        this.mRectBottom = 0;
      } else {
        double xRatio = (mWidgetWidth * 1.0 / mSourceWidth);
        double yRatio = (mWidgetHeight * 1.0 / mSourceHeight);
//        Log.d(RECT_TAG, "mWidgetWidth:" + mWidgetWidth + " mWidgetHeight:" + mWidgetHeight + " mSourceWidth:" + mSourceWidth + " mSourceHeight:" + mSourceHeight);
//        Log.d(RECT_TAG, "box.getX() : " + box.x + " box.getY():" + box.y + " mSourceWidth:" + mSourceWidth + " mSourceHeight:" + mSourceHeight);
        double x = box.x * xRatio;
        double y = box.y * yRatio;
        this.mRectLeft = (int) Math.round(x);
        this.mRectTop = (int) Math.round(y);
        this.mRectRight = (int) ((box.x + box.w) * xRatio);
        this.mRectBottom = (int) ((box.y + box.h) * yRatio);
//        Log.d(RECT_TAG, "rectLeft:" + mRectLeft + " rectTop:" + mRectTop + " rectRight:" + mRectRight + " rectBottom:" + mRectBottom);
      }
    } finally {
      mLock.unlock();
    }
    postInvalidate();
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    this.mWidgetWidth = w;
    this.mWidgetHeight = h;

  }

  @Override
  protected void onDraw(Canvas canvas) {
    if (mWidgetWidth < 0 || mWidgetHeight < 0) {
      return;
    }
    if (!recordRealValue) {
      mWidgetWidth = getWidth();
      mWidgetHeight = getHeight();
      Log.d(RECT_TAG, "onDraw mWidgetWidth:" + mWidgetWidth + " mWidgetHeight: " + mWidgetHeight);
      recordRealValue = true;
    }

    mLock.lock();
    try {
      mRect.set(mRectLeft, mRectTop, mRectRight, mRectBottom);
      canvas.drawRect(mRect, mPaint);
    } finally {
      mLock.unlock();
    }
  }
}
