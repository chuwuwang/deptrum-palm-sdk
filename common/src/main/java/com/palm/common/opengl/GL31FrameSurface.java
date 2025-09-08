package com.palm.common.opengl;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewGroup;

/**
 * @author BiCheng
 * @date 2023/8/1  19:21
 * @description:
 **/
public class GL31FrameSurface extends GLSurfaceView {
  private GL31FrameRenderer mRenderer;

  public GL31FrameSurface(Context context) {
    super(context);
    init();
  }

  public GL31FrameSurface(Context context, AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  private synchronized void init() {
    if (mRenderer == null) {
      setEGLContextClientVersion(3);
      mRenderer = new GL31FrameRenderer(this);
      this.setRenderer(mRenderer);
      setRenderMode(RENDERMODE_WHEN_DIRTY);
    } else {
      if (this.getCurrentRenderer() == null) {
        this.setRenderer(mRenderer);
        setRenderMode(RENDERMODE_WHEN_DIRTY);
      }
    }
  }

  public void setDisplay(int w, int h) {
    ViewGroup.LayoutParams layoutParams = getLayoutParams();
    if (layoutParams.width == w && layoutParams.height == h) {
      return;
    }
    Log.d("GLFrameSurface", "layoutParams.width:" + layoutParams.width + ",layoutParams.height:" + layoutParams.height);
    layoutParams.width = w;
    layoutParams.height = h;
    setLayoutParams(layoutParams);
  }

  public GLFrameRenderer getCurrentRenderer() {
    return mRenderer;
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
    // Utils.LOGD("surface onAttachedToWindow()");
    super.onAttachedToWindow();
    init();
    // setRenderMode() only takes effectd after SurfaceView attached to window!
    // note that on this mode, surface will not render util GLSurfaceView.requestRender() is
    // called, it's good and efficient -v-
    // “WHEN_DIRTY”是由用户调用requestRenderer()绘制
    // Utils.LOGD("surface setRenderMode RENDERMODE_WHEN_DIRTY");
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);

  }

  public interface ICallback {
    void onSizeChanged(int w, int h);
  }

  // 标志是否相应触摸事件
  private volatile boolean mEnableTouchEvent;

  public void enableTouchEvent(boolean enable) {
    mEnableTouchEvent = enable;
  }

  @Override
  public boolean dispatchTouchEvent(MotionEvent event) {

    if (!mEnableTouchEvent) {
      getParent().requestDisallowInterceptTouchEvent(false);
      return super.dispatchTouchEvent(event);
    }
    if (event.getX() < 0 || event.getY() < 0 ||
        event.getX() > this.getWidth() || event.getY() > this.getHeight()) {
      getParent().requestDisallowInterceptTouchEvent(false);
      return super.dispatchTouchEvent(event);
    }
    // 需要内部控件处理该事件,请求上层viewGroup不拦截
    getParent().requestDisallowInterceptTouchEvent(true);
    return super.dispatchTouchEvent(event);
  }

  private float oldDistance;
  private float oldSingleX;
  private float oldSingleY;

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    if (!mEnableTouchEvent) {
      return false;
    }
    switch (event.getAction() & MotionEvent.ACTION_MASK) {
      case MotionEvent.ACTION_DOWN:
        Log.d("LBC_", "ACTION_DOWN ");
        if (event.getPointerCount() == 1) {
          oldSingleX = event.getX();
          oldSingleY = event.getY();
          mRenderer.initRotateParam();
        }
        break;
      case MotionEvent.ACTION_POINTER_DOWN:
        Log.d("LBC_", "ACTION_POINTER_DOWN ");
        if (event.getPointerCount() == 2) {
          oldDistance = calculateDistance(event);//计算距离
//          Log.d("LBC_", "oldDistance = " + oldDistance);
        }
        break;
      case MotionEvent.ACTION_MOVE:
        Log.d("LBC_", "ACTION_MOVE ");
        if (event.getPointerCount() == 1) {
          mRenderer.setRotate(event.getX() - oldSingleX, event.getY() - oldSingleY);
        }
        if (event.getPointerCount() == 2) {
          float currentDistance = calculateDistance(event);
          float scale = currentDistance / oldDistance;
          oldDistance = currentDistance;
//          Log.d("LBC_", "scale = " + scale);
          mRenderer.setScale(scale);
        }
        break;
      case MotionEvent.ACTION_UP:
        Log.d("LBC_", "ACTION_UP ");
        break;
      case MotionEvent.ACTION_POINTER_UP:
        Log.d("LBC_", "ACTION_POINTER_UP");
        break;
      case MotionEvent.ACTION_CANCEL:
        Log.d("LBC_", "ACTION_CANCEL ");
        break;
      default:
    }
    // 返回true表示当前view消费事件
    return true;
  }

  private float calculateDistance(MotionEvent motionEvent) {
    float x1 = motionEvent.getX(0);//第一个点x坐标
    float x2 = motionEvent.getX(1);//第二个点x坐标
    float y1 = motionEvent.getY(0);//第一个点y坐标
    float y2 = motionEvent.getY(1);//第二个点y坐标
    return (float) Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
  }
}