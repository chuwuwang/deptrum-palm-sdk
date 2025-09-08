package com.palm.common.opengl;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.Renderer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * 渲染
 */
public class GLFrameRenderer implements Renderer {
  private static final int sFPS = 25;

  private ISimplePlayer mParentAct;
  private GLSurfaceView mTargetSurface;
  private YUVI420PGLProgram yuvi420PGLProgram = new YUVI420PGLProgram(0);
  private RGB24GLProgram rgbProg = new RGB24GLProgram(0);
  private YUV420NV12PGLProgram yuvNv12Prog = new YUV420NV12PGLProgram(0);
  private YUV420NV21PGLProgram yuvNv21Prog = new YUV420NV21PGLProgram(0);
  private BitmapGLProgram bitmapGLProgram = new BitmapGLProgram(0);

  private int mScreenWidth, mScreenHeight;
  private int mVideoWidth, mVideoHeight;
  private ByteBuffer y;
  private ByteBuffer u;
  private ByteBuffer v;
  private ByteBuffer uv;

  private ByteBuffer blackUV;
  private ByteBuffer rgb24;

  private int mDisplayDegrees;
  private boolean mNeedMirror = false;
  private long mLastFrameTime = 0;
  private long mStanderDelta;
  public final static int kYuvI420 = 0;
  public final static int kRGB24 = 1;  //rgb 3通道
  public final static int kIr = 2;  //ir 单通道
  public final static int kYuv420Nv12 = 7;
  public final static int kYuv420Nv21 = 8;
  public final static int kBitmap = 9;
  private int dataType = 0;


  public int getVideoWidth() {
    return mVideoWidth;
  }

  public int getVideoHeight() {
    return mVideoHeight;
  }

  public GLFrameRenderer(GLSurfaceView surface) {
    // mParentAct = captureCallback;
    mTargetSurface = surface;
    mDisplayDegrees = 0;
    mNeedMirror = false;
    mStanderDelta = 1000 / sFPS;
  }

  public void setResolution(int nWidth, int nHeight) {
    mScreenWidth = nWidth;
    mScreenHeight = nHeight;
  }

  @Override
  public void onSurfaceCreated(GL10 gl, EGLConfig config) {
    // Utils.LOGD("GLFrameRenderer :: onSurfaceCreated");
    if (!yuvi420PGLProgram.isProgramBuilt()) {
      yuvi420PGLProgram.buildProgram();
    }
    if (!rgbProg.isProgramBuilt()) {
      rgbProg.buildProgram();
    }
    if (!yuvNv12Prog.isProgramBuilt()) {
      yuvNv12Prog.buildProgram();
    }
    if (!yuvNv21Prog.isProgramBuilt()) {
      yuvNv21Prog.buildProgram();
    }
    if (!bitmapGLProgram.isProgramBuilt()) {
      bitmapGLProgram.buildProgram();
    }

    // 初始化顶点数据和索引数据
    float vertices[] = {
        0.0f, 0.0f, 0.0f,  // 左上角
        0.0f, 0.0f, 0.0f,  // 左下角
        0.0f, 0.0f, 0.0f,  // 右下角
        0.0f, 0.0f, 0.0f,  // 右上角
    };
    short indices[] = {
        0, 1, 1, 2, 2, 3, 3, 0, // 绘制矩形边框线
    };
    vertexBuffer = ByteBuffer.allocateDirect(vertices.length * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .put(vertices);
    vertexBuffer.position(0);
    indexBuffer = ByteBuffer.allocateDirect(indices.length * 2)
        .order(ByteOrder.nativeOrder())
        .asShortBuffer()
        .put(indices);
    indexBuffer.position(0);

    // 编译顶点着色器和片段着色器
    String vertexShaderCode = "attribute vec4 aPosition;\n" +
        "void main() {\n" +
        "  gl_Position = aPosition;\n" +
        "}";
    int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
    String fragmentShaderCode = "precision mediump float;\n" +
        "void main() {\n" +
        "  gl_FragColor = vec4(1.0, 1.0, 0.0, 1.0);\n" +
        "}";
    int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

    // 创建OpenGL ES程序对象
    mProgram = GLES20.glCreateProgram();
    GLES20.glAttachShader(mProgram, vertexShader);
    GLES20.glAttachShader(mProgram, fragmentShader);
    GLES20.glLinkProgram(mProgram);
  }

  @Override
  public void onSurfaceChanged(GL10 gl, int width, int height) {
    // Utils.LOGD("GLFrameRenderer :: onSurfaceChanged");
    GLES20.glViewport(0, 0, width, height);
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
        case kYuvI420: {
          // 绿帧则不渲染
          if (y != null) {
            if (isNullFrame(y, u, v)) {
              return;
            } else {
              y.position(0);
              u.position(0);
              v.position(0);
              yuvi420PGLProgram.buildTextures(y, u, v, mVideoWidth, mVideoHeight);
              GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
              GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
              yuvi420PGLProgram.drawFrame();
            }
          }
          break;
        }
        case kYuv420Nv12:
          y.position(0);
          uv.position(0);
          yuvNv12Prog.setType(dataType);
          yuvNv12Prog.buildTextures(y, uv, mVideoWidth, mVideoHeight);
          GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
          GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
          yuvNv12Prog.drawFrame();
          break;
        case kYuv420Nv21:
          y.position(0);
          uv.position(0);
          yuvNv21Prog.setType(dataType);
          yuvNv21Prog.buildTextures(y, uv, mVideoWidth, mVideoHeight);
          GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
          GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
          yuvNv21Prog.drawFrame();
          break;
        case kRGB24: {
          if (rgb24 != null) {
            rgb24.position(0);
            rgbProg.buildTextures(rgb24, mVideoWidth, mVideoHeight, false);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            rgbProg.drawFrame();
          }
          break;
        }
        case kIr: {
          if (rgb24 != null) {
            rgb24.position(0);
            rgbProg.buildTexturesSingle(rgb24, mVideoWidth, mVideoHeight);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            rgbProg.drawFrame();
          }
          break;
        }
        case kBitmap: {
          if (bitmap != null) {
//                        bitmapGLProgram.buildTextures(bitmap, mVideoWidth, mVideoHeight);
            bitmapGLProgram.createTexture(bitmap);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            bitmapGLProgram.drawFrame();
          }
          break;
        }
        default:
      }
    }

    // 绑定顶点缓冲区
    int positionHandle = GLES20.glGetAttribLocation(mProgram, "aPosition");
    GLES20.glEnableVertexAttribArray(positionHandle);
    GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);

    // 启用深度测试
    GLES20.glEnable(GLES20.GL_DEPTH_TEST);

    // 绘制矩形
    GLES20.glUseProgram(mProgram);
    GLES20.glLineWidth(1);
    GLES20.glDrawElements(GLES20.GL_LINES, 8, GLES20.GL_UNSIGNED_SHORT, indexBuffer);

    // 禁用深度测试和顶点属性指针
    GLES20.glDisable(GLES20.GL_DEPTH_TEST);
    GLES20.glDisableVertexAttribArray(positionHandle);
  }

  public void clear() {
    switch (dataType) {
      case 0: {
        if (y == null || u == null || v == null) {
          return;
        }
        int size = this.mVideoWidth * this.mVideoHeight;
        byte[] b = new byte[size];
        initBlackUV();
        synchronized (this) {
          y.clear();
          u.clear();
          v.clear();
          y.put(b);
          blackUV.position(0);
          u.put(blackUV);
          blackUV.position(0);
          v.put(blackUV);
        }
        mTargetSurface.requestRender();
        break;
      }
      case 2:
      case 3:
      case 1: {
        rgb24.clear();
        rgb24.put(new byte[rgb24.capacity()]);
        mTargetSurface.requestRender();
        break;
      }

    }
  }

  /**
   * this method will be called from native code, it happens when the video is about to play or the
   * video size changes.
   */
  public void update(int w, int h, int dataType) {
    this.dataType = dataType;
    // Utils.LOGD("INIT E");
    if (w > 0 && h > 0) {

      if (mScreenWidth > 0 && mScreenHeight > 0) {
        float f1 = 1f * mScreenHeight / mScreenWidth;
        float f2 = 1f * h / w;
        if (f1 == f2) {
          yuvi420PGLProgram.createBuffers(YUVI420PGLProgram.squareVertices);
          rgbProg.createBuffers(RGB24GLProgram.squareVertices);
          yuvNv12Prog.createBuffers(YUV420NV12PGLProgram.squareVertices);
          yuvNv21Prog.createBuffers(YUV420NV21PGLProgram.squareVertices);
          bitmapGLProgram.createBuffers(BitmapGLProgram.POSITION_VERTEX);
        } else if (f1 < f2) {
          float widScale = f1 / f2;
          yuvi420PGLProgram.createBuffers(
              new float[]{
                  -widScale, -1.0f, widScale, -1.0f, -widScale, 1.0f, widScale, 1.0f,
              });
          rgbProg.createBuffers(
              new float[]{
                  -widScale, -1.0f, widScale, -1.0f, -widScale, 1.0f, widScale, 1.0f,
              });
          yuvNv12Prog.createBuffers(
              new float[]{
                  -widScale, -1.0f, widScale, -1.0f, -widScale, 1.0f, widScale, 1.0f,
              });
          yuvNv21Prog.createBuffers(
              new float[]{
                  -widScale, -1.0f, widScale, -1.0f, -widScale, 1.0f, widScale, 1.0f,
              });
          bitmapGLProgram.createBuffers(
              new float[]{
                  -widScale, -1.0f, widScale, -1.0f, -widScale, 1.0f, widScale, 1.0f,
              });
        } else {
          float heightScale = f2 / f1;
          yuvi420PGLProgram.createBuffers(
              new float[]{
                  -1.0f, -heightScale, 1.0f, -heightScale, -1.0f, heightScale, 1.0f, heightScale,
              });
          rgbProg.createBuffers(
              new float[]{
                  -1.0f, -heightScale, 1.0f, -heightScale, -1.0f, heightScale, 1.0f, heightScale,
              });
          yuvNv12Prog.createBuffers(
              new float[]{
                  -1.0f, -heightScale, 1.0f, -heightScale, -1.0f, heightScale, 1.0f, heightScale,
              });
          yuvNv21Prog.createBuffers(
              new float[]{
                  -1.0f, -heightScale, 1.0f, -heightScale, -1.0f, heightScale, 1.0f, heightScale,
              });
          bitmapGLProgram.createBuffers(
              new float[]{
                  -1.0f, -heightScale, 1.0f, -heightScale, -1.0f, heightScale, 1.0f, heightScale,
              });
        }
      }

      if (w != mVideoWidth || h != mVideoHeight) {
        this.mVideoWidth = w;
        this.mVideoHeight = h;
        int yarraySize = w * h;
        if (dataType == 0) {
          int uvarraySize = yarraySize / 4;
          synchronized (this) {
            y = ByteBuffer.allocate(yarraySize);
            u = ByteBuffer.allocate(uvarraySize);
            v = ByteBuffer.allocate(uvarraySize);
          }
        }
        if (dataType == kYuv420Nv12 || dataType == kYuv420Nv21) {
          int uvarraySize = yarraySize / 2;
          synchronized (this) {
            y = ByteBuffer.allocate(yarraySize);
            uv = ByteBuffer.allocate(uvarraySize);
          }
        } else if (dataType == kRGB24) {
          synchronized (this) {
            rgb24 = ByteBuffer.allocate(yarraySize * 3);
          }
        } else if (dataType == kIr) {
          synchronized (this) {
            rgb24 = ByteBuffer.allocate(yarraySize);
          }
        }
      }
    }

    // mParentAct.onPlayStart();
    // Utils.LOGD("INIT X");
  }

  /**
   * this method will be called from native code, it's used for passing yuv data to me.
   */
  public void update(byte[] ydata, byte[] udata, byte[] vdata) {
    synchronized (this) {
      y.clear();
      u.clear();
      v.clear();
      y.put(ydata, 0, ydata.length);
      u.put(udata, 0, udata.length);
      v.put(vdata, 0, vdata.length);
    }
    // request to render
    mTargetSurface.requestRender();
  }


  public Bitmap bitmap;

  public void update(Bitmap bitmap) {
    this.bitmap = bitmap;
    // request to render
    mTargetSurface.requestRender();
  }


  /**
   * @param dataType 0:yuv420p 1:rgb24
   */
  public void update(byte[] data, int dataType) {
    this.dataType = dataType;
    int size = this.mVideoWidth * this.mVideoHeight;
    synchronized (this) {
      switch (this.dataType) {
        case kYuvI420: {
          y.clear();
          u.clear();
          v.clear();
          y.put(data, 0, size);
          u.put(data, size, size / 4);
          v.put(data, size * 5 / 4, size / 4);
          break;
        }
        case kYuv420Nv12:
        case kYuv420Nv21:
          y.clear();
          uv.clear();
          y.put(data, 0, size);
          uv.put(data, size, size / 2);
          break;
        case kRGB24: {
          if (rgb24 == null) {
            rgb24 = ByteBuffer.allocate(size * 3);
          }
          rgb24.clear();
          rgb24.put(data, 0, size * 3);
          break;
        }
        case kIr: {
          if (rgb24 == null) {
            rgb24 = ByteBuffer.allocate(size);
          }
          rgb24.clear();
          rgb24.put(data, 0, size);
          break;
        }
      }
    }
    // request to render
    mTargetSurface.requestRender();
  }


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
        case kYuvI420: {
          y.clear();
          u.clear();
          v.clear();
          y.put(data, 0, size);
          u.put(data, size, size / 4);
          v.put(data, size * 5 / 4, size / 4);
          break;
        }
        case kYuv420Nv12:
        case kYuv420Nv21:
          y.clear();
          uv.clear();
          y.put(data, 0, size);
          uv.put(data, size, size / 2);
          break;
        case kRGB24: {
          if (rgb24 == null || isSizeChanged) {
            rgb24 = ByteBuffer.allocate(size * 3);
          }
          rgb24.clear();
          rgb24.put(data, 0, size * 3);
          break;
        }
        case kIr: {
          if (rgb24 == null || isSizeChanged) {
            rgb24 = ByteBuffer.allocate(size);
          }
          rgb24.clear();
          rgb24.put(data, 0, size);
          break;
        }
      }
    }
    // request to render
    mTargetSurface.requestRender();
  }

  /**
   * @param dataType 0:yuv420p 1:rgb24
   */
  public void update(byte[] data, int width, int height, int dataType, int[] roi) {
    this.dataType = dataType;
    int size = width * height;
    boolean isSizeChanged = this.mVideoWidth * this.mVideoHeight != size;
    if (isSizeChanged) {
      mVideoWidth = width;
      mVideoHeight = height;
    }
    synchronized (this) {
      switch (this.dataType) {
        case kYuvI420: {
          y.clear();
          u.clear();
          v.clear();
          y.put(data, 0, size);
          u.put(data, size, size / 4);
          v.put(data, size * 5 / 4, size / 4);
          break;
        }
        case kYuv420Nv12:
        case kYuv420Nv21:
          y.clear();
          uv.clear();
          y.put(data, 0, size);
          uv.put(data, size, size / 2);
          break;
        case kRGB24: {
          if (rgb24 == null) {
            rgb24 = ByteBuffer.allocate(size * 3);
          }
          rgb24.clear();
          rgb24.put(data, 0, size * 3);
          break;
        }
        case kIr: {
          if (rgb24 == null) {
            rgb24 = ByteBuffer.allocate(size);
          }
          rgb24.clear();
          rgb24.put(data, 0, size);
          break;
        }
      }
    }

    if (null != roi) {
      transformRoi(roi, width, height);
    }
    // request to render
    mTargetSurface.requestRender();
  }

  private void transformRoi(int[] roi, int width, int height) {
    if (null == roi || roi.length != 4) {
      return;
    }
    int halfWidth = width / 2;
    int halfHeight = height / 2;
    float p1X = (float) (roi[0] - halfWidth) / halfWidth;
    float p1Y = (float) (halfHeight - roi[1]) / halfHeight;
    float p2X = (float) (roi[0] - halfWidth) / halfWidth;
    float p2Y = (float) (halfHeight - roi[1] - roi[3]) / halfHeight;
    float p3X = (float) (roi[0] + roi[2] - halfWidth) / halfWidth;
    float p3Y = (float) (halfHeight - roi[1]) / halfHeight;
    float p4X = (float) (roi[0] + roi[2] - halfWidth) / halfWidth;
    float p4Y = (float) (halfHeight - roi[1] - roi[3]) / halfHeight;

    float[] vertices = new float[]{
        p1X, p1Y, 0.0f,  // 左上角
        p2X, p2Y, 0.0f,  // 左下角
        p4X, p4Y, 0.0f,  // 右下角
        p3X, p3Y, 0.0f,  // 右上角
    };

    if (vertexBuffer != null) {
      vertexBuffer.clear();
      vertexBuffer.put(vertices);
      vertexBuffer.position(0);
    }
  }

  private int mProgram;
  private FloatBuffer vertexBuffer;
  private ShortBuffer indexBuffer;


  private int loadShader(int shaderType, String source) {
    int shader = GLES20.glCreateShader(shaderType);
    if (shader != 0) {
      GLES20.glShaderSource(shader, source);
      GLES20.glCompileShader(shader);
      int[] compiled = new int[1];
      GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
      if (compiled[0] == 0) {
        GLES20.glDeleteShader(shader);
        shader = 0;
      }
    }
    return shader;
  }

  /**
   * 取图像四角与中心判定
   * */
  private boolean isNullFrame(ByteBuffer y, ByteBuffer u, ByteBuffer v) {
    switch (dataType) {
      case kYuvI420: {
        int size = mVideoWidth * mVideoHeight;
        if (y == null
            || u == null
            || v == null
            || y.capacity() < size
            || u.capacity() < size / 4
            || v.capacity() < size / 4) {
          return true;
        }
        int ypos = y.position();
        int upos = u.position();
        int vpos = v.position();
        int[] points = {0, mVideoWidth - 2, size / 2, size - mScreenWidth + 2, size - 2};
        int flag = 2; // 最少两点为绿则判断为绿帧

        for (int i = 0; i < 5; i++) {
          if (y.get(points[i]) == 0 && u.get(points[i] / 4) == 0 && v.get(points[i] / 4) == 0) {
            flag--;
            if (flag <= 0) {
              y.position(ypos);
              u.position(upos);
              v.position(vpos);
              return true;
            }
          }
        }
        y.position(ypos);
        u.position(upos);
        v.position(vpos);
        return false;
      }
      case 1: {
        break;
      }
    }
    return false;
  }

  private void initBlackUV() {
    int size = mVideoWidth * mVideoHeight;
    if (blackUV == null || blackUV.capacity() < size / 4) {
      blackUV = ByteBuffer.allocate(size / 4);
      blackUV.position(0);
      for (int i = 0; i < size / 4; i++) {
        blackUV.put((byte) 128);
      }
      blackUV.position(0);
    } else {
      blackUV.position(0);
    }
  }

  /**
   * this method will be called from native code, it's used for passing play state to activity.
   */
  public void updateState(int state) {
    // Utils.LOGD("updateState E = " + state);
    if (mParentAct != null) {
      mParentAct.onReceiveState(state);
    }
    // Utils.LOGD("updateState X");
  }

  public void setDisplayOrientation(int displayOrientation) {
    mDisplayDegrees = displayOrientation;
    rgbProg.setDisplayOrientation(displayOrientation, mNeedMirror);
    yuvNv12Prog.setDisplayOrientation(displayOrientation, mNeedMirror);
    yuvNv21Prog.setDisplayOrientation(displayOrientation, mNeedMirror);
    yuvi420PGLProgram.setDisplayOrientation(displayOrientation, mNeedMirror);
    bitmapGLProgram.setDisplayOrientation(displayOrientation, mNeedMirror);
  }

  public void displayMirror(boolean mirror) {
    mNeedMirror = mirror;
    rgbProg.setDisplayOrientation(mDisplayDegrees, mNeedMirror);
    yuvNv12Prog.setDisplayOrientation(mDisplayDegrees, mNeedMirror);
    yuvNv21Prog.setDisplayOrientation(mDisplayDegrees, mNeedMirror);
    yuvi420PGLProgram.setDisplayOrientation(mDisplayDegrees, mNeedMirror);
    bitmapGLProgram.setDisplayOrientation(mDisplayDegrees, mNeedMirror);
  }

  public void release() {
    rgbProg.releaseProgram();
    yuvNv12Prog.releaseProgram();
    yuvNv21Prog.releaseProgram();
    yuvi420PGLProgram.releaseProgram();
    bitmapGLProgram.releaseProgram();
  }
}
