package com.palm.common.opengl;

import android.graphics.Bitmap;
import android.util.Log;

public class GLDisplay {
  private boolean mIsDisplayStarted;
  private GLFrameRenderer render;

  /**
   * 预览渲染
   *
   * @param view               需要渲染的视图
   * @param displayOrientation 预览方向
   * @param displayMirror      预览镜像
   * @param data               预览的图像数据
   * @param renderWidth        预览宽
   * @param renderHeight       预览高
   * @param dataType           图像数据类型 0:yuv420_I420p 1:rgb24 2:ir 7:yuv420_nv12 8:yuv420_nv21 10:pointsCloud
   * @return void
   */
  public synchronized void render(
      GLFrameSurface view,
      int displayOrientation,
      boolean displayMirror,
      byte[] data,
      int renderWidth,
      int renderHeight,
      int dataType) {
    if (view == null) {
      return;
    }
    render = view.getCurrentRenderer();
    render.setResolution(renderWidth, renderHeight);
    if (view != null && render != null) {
      if (!mIsDisplayStarted || render.getVideoWidth() != renderWidth || render.getVideoHeight() != renderHeight) {
        render.update(renderWidth, renderHeight, dataType);
        if (renderHeight > 0 && renderWidth > 0) {
          view.post(() -> {
            int width = view.getWidth();
            view.setDisplay(width, width * renderHeight / renderWidth);
          });
        }
        view.onResume();
        mIsDisplayStarted = true;
      }
      render.setDisplayOrientation(displayOrientation);
      render.displayMirror(displayMirror);
      render.update(data, renderWidth, renderHeight, dataType);
    }
  }

  /**
   * 预览渲染
   *
   * @param view               需要渲染的视图
   * @param displayOrientation 预览方向
   * @param displayMirror      预览镜像
   * @param displayOrientation 预览方向
   * @param data               预览的图像数据
   * @param renderWidth        预览宽
   * @param renderHeight       预览高
   * @param dataType           图像数据类型 0:yuv420_I420 p 1:rgb24 7:yuv420_nv12 8:yuv420_nv21
   * @param roi                图像中的手掌roi
   * @return void
   */
  public synchronized void render(
      GLFrameSurface view,
      int displayOrientation,
      boolean displayMirror,
      byte[] data,
      int renderWidth,
      int renderHeight,
      int dataType,
      int[] roi) {
    if (view == null) {
      return;
    }
    render = view.getCurrentRenderer();
    render.setResolution(renderWidth, renderHeight);
    if (view != null && render != null) {
      if (!mIsDisplayStarted || render.getVideoWidth() != renderWidth || render.getVideoHeight() != renderHeight) {
        render.update(renderWidth, renderHeight, dataType);
        view.onResume();
        mIsDisplayStarted = true;
      }
      render.setDisplayOrientation(displayOrientation);
      render.displayMirror(displayMirror);
      render.update(data, renderWidth, renderHeight, dataType,roi);
    }
  }

  /**
   * 预览渲染
   *
   * @param view               需要渲染的视图
   * @param displayOrientation 预览方向
   * @param displayMirror      预览镜像
   * @param data               预览的图像数据
   * @param renderWidth        预览宽
   * @param renderHeight       预览高
   * @param dataType           图像数据类型 0:yuv420_I420p 1:rgb24 2:ir 7:yuv420_nv12 8:yuv420_nv21 10:pointsCloud
   * @return void
   */
  public synchronized void render(
      GL31FrameSurface view,
      int displayOrientation,
      boolean displayMirror,
      byte[] data,
      int renderWidth,
      int renderHeight,
      int dataType) {
    if (view == null) {
      return;
    }
    render = view.getCurrentRenderer();
    render.setResolution(renderWidth, renderHeight);
    if (view != null && render != null) {
      if (!mIsDisplayStarted || render.getVideoWidth() != renderWidth || render.getVideoHeight() != renderHeight) {
        render.update(renderWidth, renderHeight, dataType);
        if (renderHeight > 0 && renderWidth > 0) {
          view.post(() -> {
            view.setDisplay(renderWidth, renderHeight);
          });
        }
        view.onResume();
        mIsDisplayStarted = true;
      }
      render.setDisplayOrientation(displayOrientation);
      render.displayMirror(displayMirror);
      render.update(data, renderWidth, renderHeight, dataType);
    }
  }


  public synchronized void renderBitmap(
      GLFrameSurface view,
      int displayOrientation,
      boolean displayMirror,
      Bitmap bitmap,
      int renderWidth,
      int renderHeight) {
    Log.e("opengl", renderWidth + "x" + renderHeight);
    render = view.getCurrentRenderer();
    render.setResolution(renderWidth, renderHeight);
    if (view != null && render != null) {
      if (!mIsDisplayStarted) {
        render.update(renderWidth, renderHeight, 9);
        view.onResume();
        mIsDisplayStarted = true;
      }
      render.setDisplayOrientation(displayOrientation);
      render.displayMirror(displayMirror);
      render.update(bitmap);
    }
  }

  /**
   * 释放预览
   *
   * @return void
   */
  public void release() {
    if (render != null) {
      if (render instanceof GL31FrameRenderer) {
        ((GL31FrameRenderer) render).release();
      } else {
        render.release();
      }
      this.mIsDisplayStarted = false;
    }
  }
}
