package com.palm.common.opengl;

import android.opengl.GLES31;
import android.opengl.GLSurfaceView;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * @author BiCheng
 * @date 2023/8/1  19:38
 * @description:
 **/
public class GL31FrameRenderer extends GLFrameRenderer {

  private static final int sFPS = 25;

  private GLSurfaceView mTargetSurface;
  private PointCloudGLProgram pointCloudGLProgram = new PointCloudGLProgram(0);


  private int mScreenWidth, mScreenHeight;
  private int mVideoWidth, mVideoHeight;
  private ByteBuffer pointCloudBuffer;

  private int mDisplayDegrees;
  private boolean mNeedMirror = false;
  private long mLastFrameTime = 0;
  private long mStanderDelta;
  public final static int kPointCloud = 10;
  private int dataType = 0;

  public GL31FrameRenderer(GLSurfaceView surface) {
    super(surface);
    mTargetSurface = surface;
    mDisplayDegrees = 0;
    mNeedMirror = false;
    mStanderDelta = 1000 / sFPS;
  }

  @Override
  public int getVideoWidth() {
    return mVideoWidth;
  }

  @Override
  public int getVideoHeight() {
    return mVideoHeight;
  }

  @Override
  public void setResolution(int nWidth, int nHeight) {
    mScreenWidth = nWidth;
    mScreenHeight = nHeight;
  }

  @Override
  public void onSurfaceCreated(GL10 gl, EGLConfig config) {
    // Utils.LOGD("GLFrameRenderer :: onSurfaceCreated");
    if (!pointCloudGLProgram.isProgramBuilt()) {
      pointCloudGLProgram.buildProgram();
    }
  }

  @Override
  public void onDrawFrame(GL10 gl) {
    synchronized (this) {
      if (mLastFrameTime == 0) {
        mLastFrameTime = System.currentTimeMillis();
      } else {
        long currentTime = System.currentTimeMillis();
        long delta = currentTime - mLastFrameTime;
        if (delta < mStanderDelta) {
          try {
            Thread.sleep(mStanderDelta - delta);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
        mLastFrameTime = currentTime;
      }
      switch (dataType) {

        case kPointCloud: {
          GLES31.glClear(GLES31.GL_COLOR_BUFFER_BIT | GLES31.GL_DEPTH_BUFFER_BIT);
          GLES31.glClearColor(0.8f, 0.8f, 0.8f, 1.0f);
          GLES31.glDepthFunc(GLES31.GL_LESS);
          pointCloudGLProgram.drawAxis();
//          pointCloudGLProgram.drawGrid();
          if (pointCloudBuffer == null) {
            return;
          }
          pointCloudGLProgram.drawPointsCloud(pointCloudBuffer);
          break;
        }
        default:
      }
    }
  }

  /**
   * it happens when the video is about to play or the video size changes.
   */
  @Override
  public void update(int w, int h, int dataType) {
    this.dataType = dataType;
    // Utils.LOGD("INIT E");
    if (w > 0 && h > 0) {

      if (mScreenWidth > 0 && mScreenHeight > 0) {
        pointCloudGLProgram.createCoordinateAxis();
      }

      if (w != mVideoWidth || h != mVideoHeight) {
        this.mVideoWidth = w;
        this.mVideoHeight = h;
      }
    }
  }


  @Override
  public void update(byte[] data, int width, int height, int dataType) {
    this.dataType = dataType;
    int size = width * height;
    boolean isSizeChanged = this.mVideoWidth * this.mVideoHeight != size;
    if (isSizeChanged) {
      mVideoWidth = width;
      mVideoHeight = height;
    }
    synchronized (this) {
      switch (this.dataType) {
        case kPointCloud: {
          pointCloudBuffer = ByteBuffer.allocate(data.length);
          pointCloudBuffer.order(ByteOrder.nativeOrder());
          pointCloudBuffer.clear();
          pointCloudBuffer.put(data, 0, data.length);
          pointCloudBuffer.position(0);
          break;
        }
      }
    }
    // request to render
    mTargetSurface.requestRender();
  }


  @Override
  public void setDisplayOrientation(int displayOrientation) {
    mDisplayDegrees = displayOrientation;
    pointCloudGLProgram.setDisplayOrientation(displayOrientation, mNeedMirror);
  }

  @Override
  public void displayMirror(boolean mirror) {
    mNeedMirror = mirror;
    pointCloudGLProgram.setDisplayOrientation(mDisplayDegrees, mNeedMirror);
  }

  @Override
  public void release() {
    pointCloudGLProgram.releaseProgram();
  }

  public void setScale(float scale) {
    pointCloudGLProgram.setScale(scale);
  }

  public void setRotate(float xDiff, float yDiff) {
    pointCloudGLProgram.rotate(xDiff, yDiff);
  }

  public void initRotateParam() {
    pointCloudGLProgram.initRotateParam();
  }
}
