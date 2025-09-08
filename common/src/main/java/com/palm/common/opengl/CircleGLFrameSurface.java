package com.palm.common.opengl;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Outline;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewOutlineProvider;

public class CircleGLFrameSurface extends GLSurfaceView {

  private ICallback _cb = null;
  private GLFrameRenderer mRenderer;
  // 圆角半径
  private int radius = 0;

  public CircleGLFrameSurface(Context context) {
    this(context, null);
    init();
  }

  @SuppressLint("NewApi")
  public CircleGLFrameSurface(Context context, AttributeSet attrs) {
    super(context, attrs);
    setOutlineProvider(new ViewOutlineProvider() {
      @Override
      public void getOutline(View view, Outline outline) {
        Rect rect = new Rect(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
        outline.setRoundRect(rect, radius);
      }
    });
    setClipToOutline(true);
    init();
  }

  public GLFrameRenderer getCurrentRenderer() {
    return mRenderer;
  }

  private synchronized void init() {
    SurfaceHolder surfaceHolder = this.getHolder();
    this.setZOrderMediaOverlay(true);
    surfaceHolder.setFormat(PixelFormat.TRANSLUCENT);
    if (mRenderer == null) {
      setEGLContextClientVersion(2);
      mRenderer = new GLFrameRenderer(this);
      this.setRenderer(mRenderer);
      setRenderMode(RENDERMODE_WHEN_DIRTY);
    } else {
      if (getCurrentRenderer() == null) {
        this.setRenderer(mRenderer);
        setRenderMode(RENDERMODE_WHEN_DIRTY);
      }
    }


  }

  @SuppressLint("NewApi")
  public void turnRound() {
    invalidateOutline();
  }

  public int getRadius() {
    return radius;
  }

  public void setRadius(int radius) {
    this.radius = radius;
  }


  public void setCallback(ICallback cb) {
    _cb = cb;
  }

  @Override
  public void setRenderer(Renderer renderer) {
    super.setRenderer(renderer);
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
  }

  @Override
  protected void onAttachedToWindow() {

    init();
    super.onAttachedToWindow();
    // setRenderMode() only takes effectd after SurfaceView attached to window!
    // note that on this mode, surface will not render util GLSurfaceView.requestRender() is
    // called, it's good and efficient -v-
    // “WHEN_DIRTY”是由用户调用requestRenderer()绘制
    // Utils.LOGD("surface setRenderMode RENDERMODE_WHEN_DIRTY");
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);

    if (w > 0 && h > 0) {
      if (null != _cb) _cb.onSizeChanged(w, h);
    }
  }

  public interface ICallback {
    void onSizeChanged(int w, int h);
  }
}
