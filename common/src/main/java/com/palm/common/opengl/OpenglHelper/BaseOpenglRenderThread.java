package com.palm.common.opengl.OpenglHelper;

import android.opengl.EGL14;
import android.opengl.EGLExt;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

/**
 * @author
 */
public abstract class BaseOpenglRenderThread extends Thread implements SurfaceHolder.Callback {
  protected String TAG = BaseOpenglRenderThread.class.getSimpleName();

  public BaseOpenglRenderThread(String name, SurfaceView mSurfaceView) {
    super(name);
    this.mSurfaceView = mSurfaceView;
    this.mSurfaceView.getHolder().addCallback(this);
  }

  @Override
  public void run() {
    super.run();
    isRunning = true;
    synchronized (this) {
      while (!isSurfaceValid) {
        try {
          wait();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
    if (!isRunning) {
      return;
    }
    // 初始化环境
    prepareEgl();
    creatProgram();
    // 开始绘制
    while (isRunning) {
      synchronized (mRendLock) {
        try {
          mRendLock.wait();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        if (!isRunning) {
          break;
        }
        // TODO绘制
        draw();
        mEGL10.eglSwapBuffers(mEGLDisplay, mEGLSurface);
      }
    }
    releaseEgl();
  }

  protected abstract void creatProgram();

  protected abstract void draw();

  protected void notifyDraw() {
    synchronized (mRendLock) {
      mRendLock.notifyAll();
    }
  }

  /**
   * 初始化egl opengl环境
   *
   * @throws
   */
  private void prepareEgl() {
    mEGL10 = (EGL10) EGLContext.getEGL();
    checkSurfaceView();
    mEGLDisplay = mEGL10.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);

    if (EGL10.EGL_NO_DISPLAY == mEGLDisplay) {
      throw new RuntimeException("eglGetDisplay faile");
    }
    int[] major_minor = new int[2];
    if (!mEGL10.eglInitialize(mEGLDisplay, major_minor)) {
      throw new RuntimeException("eglInitialize faile");
    }
    int attribs[] =
        filterConfigSpec(
            new int[]{
                EGL10.EGL_RED_SIZE, 8,
                EGL10.EGL_GREEN_SIZE, 8,
                EGL10.EGL_BLUE_SIZE, 8,
                EGL10.EGL_ALPHA_SIZE, hasAlpha ? 8 : 0,
                EGL10.EGL_DEPTH_SIZE, 0,
                EGL10.EGL_STENCIL_SIZE, 0,
                EGL10.EGL_NONE
            });

    EGLConfig[] eglConfigs = new EGLConfig[1];
    int[] num_config = new int[1];
    if (!mEGL10.eglChooseConfig(mEGLDisplay, attribs, eglConfigs, 1, num_config)) {
      throw new RuntimeException("eglChooseConfig faile");
    }
    EGLConfig eglConfig = eglConfigs[0];
    // 4、配置opengl上下文环境
    int contextAttribs[] = {0x3098, 2, EGL10.EGL_NONE};
    mEGLContext =
        mEGL10.eglCreateContext(mEGLDisplay, eglConfig, EGL10.EGL_NO_CONTEXT, contextAttribs);
    if (mEGLContext == null) {
      throw new RuntimeException("eglCreateContext faile");
    }
    mEGLSurface =
        mEGL10.eglCreateWindowSurface(mEGLDisplay, eglConfig, mSurfaceView.getHolder(), null);
    if (mEGLSurface == null || mEGLSurface == EGL10.EGL_NO_SURFACE) {
      int error = mEGL10.eglGetError();
      if (error == EGL10.EGL_BAD_NATIVE_WINDOW) {
        Log.e("EglHelper", "createWindowSurface returned EGL_BAD_NATIVE_WINDOW.");
      }
    }
    if (!mEGL10.eglMakeCurrent(mEGLDisplay, mEGLSurface, mEGLSurface, mEGLContext)) {
      throw new RuntimeException("eglMakeCurrent faile");
    }
  }

  /**
   * 释放资源
   */
  private void releaseEgl() {
    if (mEGL10 != null && mEGLDisplay != null) {
      mEGL10.eglMakeCurrent(
          mEGLDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
      if (mEGLSurface != null) {
        mEGL10.eglDestroySurface(mEGLDisplay, mEGLSurface);
      }
      if (mEGLContext != null) {
        mEGL10.eglDestroyContext(mEGLDisplay, mEGLContext);
      }
      mEGL10.eglTerminate(mEGLDisplay);
    }
  }

  private int[] filterConfigSpec(int[] configSpec) {
    if (mEGLContextClientVersion != 2 && mEGLContextClientVersion != 3) {
      return configSpec;
    }

    int len = configSpec.length;
    int[] newConfigSpec = new int[len + 2];
    System.arraycopy(configSpec, 0, newConfigSpec, 0, len - 1);
    newConfigSpec[len - 1] = EGL10.EGL_RENDERABLE_TYPE;
    if (mEGLContextClientVersion == 2) {
      newConfigSpec[len] = EGL14.EGL_OPENGL_ES2_BIT; /* EGL_OPENGL_ES2_BIT */
    } else {
      newConfigSpec[len] = EGLExt.EGL_OPENGL_ES3_BIT_KHR; /* EGL_OPENGL_ES3_BIT_KHR */
    }
    newConfigSpec[len + 1] = EGL10.EGL_NONE;
    return newConfigSpec;
  }

  public void setEGLContextClientVersion(int version) {
    mEGLContextClientVersion = version;
  }

  private void checkSurfaceView() {
    if (mSurfaceView == null) {
      throw new RuntimeException("surfaceview can not be null");
    }
  }

  @Override
  public void surfaceCreated(SurfaceHolder holder) {
    isSurfaceValid = true;
    synchronized (this) {
      notifyAll();
    }
  }

  @Override
  public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
  }

  @Override
  public void surfaceDestroyed(SurfaceHolder holder) {
  }

  private int mEGLContextClientVersion;
  /**
   * 渲染目标
   */
  private SurfaceView mSurfaceView;

  private EGL10 mEGL10;

  private EGLSurface mEGLSurface;

  private EGLDisplay mEGLDisplay;

  private EGLContext mEGLContext;

  private EGLConfig mEGLConfig;

  private volatile boolean isRunning = false;

  public void setRunning(boolean running) {
    isRunning = running;
    synchronized (this) {
      notifyAll();
    }
    synchronized (mRendLock) {
      mRendLock.notifyAll();
    }
  }

  private Object mRendLock = new Object();

  private volatile boolean isSurfaceValid = false;

  protected boolean hasAlpha = false;

  public void setHasAlpha(boolean hasAlpha) {
    this.hasAlpha = hasAlpha;
  }
}
