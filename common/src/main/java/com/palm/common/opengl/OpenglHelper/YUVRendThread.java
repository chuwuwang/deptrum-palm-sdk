package com.palm.common.opengl.OpenglHelper;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ConfigurationInfo;
import android.opengl.GLES20;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 *
 */
public class YUVRendThread extends BaseOpenglRenderThread {
  private GLFrameRenderer glFrameRenderer;

  public YUVRendThread(String name, SurfaceView mSurfaceView) {
    super(name, mSurfaceView);
    TAG = YUVRendThread.class.getSimpleName();
    glFrameRenderer = new GLFrameRenderer();
    mSurfaceView.getHolder().addCallback(glFrameRenderer);
    glFrameRenderer.setResolution(640, 480);
    glFrameRenderer.setDisplayOrientation(0);
    glFrameRenderer.displayMirror(false);
    glFrameRenderer.creatBuffer(640, 480);
  }

  @Override
  protected void creatProgram() {
    if (glFrameRenderer != null) {
      glFrameRenderer.creatProgram();
    }
  }

  @Override
  protected void draw() {
    if (glFrameRenderer != null) {
      glFrameRenderer.onDrawFrame();
    }
  }

  public void update(byte[] yuv420p) {
    if (glFrameRenderer != null) {
      glFrameRenderer.update(yuv420p);
      notifyDraw();
    }
  }

  public static class GLProgram {
    private static final String VERTEX_SHADER =
        "uniform mat4 uMVPMatrix;\n"
            + "attribute vec4 vPosition;\n"
            + "attribute vec2 a_texCoord;\n"
            + "varying vec2 tc;\n"
            + "void main() {\n"
            + "gl_Position = uMVPMatrix * vPosition;\n"
            + "tc = a_texCoord;\n"
            + "}\n";
    private static final String FRAGMENT_SHADER =
        "precision mediump float;\n"
            + "uniform sampler2D tex_y;\n"
            + "uniform sampler2D tex_u;\n"
            + "uniform sampler2D tex_v;\n"
            + "varying vec2 tc;\n"
            + "void main() {\n"
            + "vec4 c = vec4((texture2D(tex_y, tc).r - 16./255.) * 1.164);\n"
            + "vec4 U = vec4(texture2D(tex_u, tc).r - 128./255.);\n"
            + "vec4 V = vec4(texture2D(tex_v, tc).r - 128./255.);\n"
            + "c += V * vec4(1.596, -0.813, 0, 0);\n"
            + "c += U * vec4(0, -0.392, 2.017, 0);\n"
            + "c.a = 1.0;\n"
            + "gl_FragColor = c;\n"
            + "}\n";

    private float[] mViewMatrix = new float[16];
    private int mVPMatrixHandle = -1;

    static float[] s0Matrix = {
        1.0f, 0.0f, 0f, 0.0f,
        0.0f, 1.0f, 0.0f, 0.0f,
        0.0f, 0.0f, 1.0f, 0.0f,
        0.0f, 0.0f, 0.0f, 1.0f,
    };

    static float[] s0MirrorMatrix = {
        -1.0f, 0.0f, 0f, 0.0f,
        0.0f, 1.0f, 0.0f, 0.0f,
        0.0f, 0.0f, 1.0f, 0.0f,
        0.0f, 0.0f, 0.0f, 1.0f,
    };

    static float[] s90Matrix = {
        0.0f, -1.0f, 0f, 0.0f,
        1.0f, 0.0f, 0.0f, 0.0f,
        0.0f, 0.0f, 1.0f, 0.0f,
        0.0f, 0.0f, 0.0f, 1.0f,
    };
    static float[] s90MirrorMatrix = {
        0.0f, -1.0f, 0f, 0.0f,
        -1.0f, 0.0f, 0.0f, 0.0f,
        0.0f, 0.0f, 1.0f, 0.0f,
        0.0f, 0.0f, 0.0f, 1.0f,
    };
    static float[] s180Matrix = {
        -1.0f, 0.0f, 0f, 0.0f,
        0.0f, -1.0f, 0.0f, 0.0f,
        0.0f, 0.0f, 1.0f, 0.0f,
        0.0f, 0.0f, 0.0f, 1.0f,
    };
    static float[] s180MirrorMatrix = {
        1.0f, 0.0f, 0f, 0.0f,
        0.0f, -1.0f, 0.0f, 0.0f,
        0.0f, 0.0f, 1.0f, 0.0f,
        0.0f, 0.0f, 0.0f, 1.0f,
    };
    static float[] s270Matrix = {
        0.0f, 1.0f, 0f, 0.0f,
        -1.0f, 0.0f, 0.0f, 0.0f,
        0.0f, 0.0f, 1.0f, 0.0f,
        0.0f, 0.0f, 0.0f, 1.0f,
    };
    static float[] s270MirrorMatrix = {
        0.0f, 1.0f, 0f, 0.0f,
        1.0f, 0.0f, 0.0f, 0.0f,
        0.0f, 0.0f, 1.0f, 0.0f,
        0.0f, 0.0f, 0.0f, 1.0f,
    };

    static float[] squareVertices = {
        -1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f,
    }; // fullscreen
    static float[] squareVertices1 = {
        -1.0f, 0.0f, 0.0f, 0.0f, -1.0f, 1.0f, 0.0f, 1.0f,
    }; // left-top
    static float[] squareVertices2 = {
        0.0f, -1.0f, 1.0f, -1.0f, 0.0f, 0.0f, 1.0f, 0.0f,
    }; // right-bottom
    static float[] squareVertices3 = {
        -1.0f, -1.0f, 0.0f, -1.0f, -1.0f, 0.0f, 0.0f, 0.0f,
    }; // left-bottom
    static float[] squareVertices4 = {
        0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f,
    }; // right-top
    private static float[] coordVertices = {
        0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f,
    }; // whole-texture

    public final int mWinPosition;
    private int mGLProgram;

    private int mGLTextureI;
    private int mGLTextureII;
    private int mGLTextureIII;

    private int mGLIndexI;
    private int mGLIndexII;
    private int mGLTIndexIII;

    private float[] mGLVertices;

    private int mPositionHandle = -1, mCoordHandle = -1;
    private int mYhandle = -1, mUhandle = -1, mVhandle = -1;
    private int mYtid = -1, mUtid = -1, mVtid = -1;

    private ByteBuffer mVerticeBuffer;
    private ByteBuffer mCoordBuffer;

    private int mVideoWidth = -1;
    private int mVideoHeight = -1;

    private boolean isProgBuilt = false;

    /**
     * position can only be 0~4:<br>
     * fullscreen => 0<br>
     * left-top => 1<br>
     * right-top => 2<br>
     * left-bottom => 3<br>
     * right-bottom => 4
     */
    public GLProgram(int position) {
      if (position < 0 || position > 4) {
        throw new RuntimeException("Index can only be 0 to 4");
      }
      mWinPosition = position;
      setup();
    }

    /**
     * prepared for later use
     */
    private void setup() {
      switch (mWinPosition) {
        case 1:
          mGLVertices = squareVertices1;
          mGLTextureI = GLES20.GL_TEXTURE0;
          mGLTextureII = GLES20.GL_TEXTURE1;
          mGLTextureIII = GLES20.GL_TEXTURE2;
          mGLIndexI = 0;
          mGLIndexII = 1;
          mGLTIndexIII = 2;
          break;
        case 2:
          mGLVertices = squareVertices2;
          mGLTextureI = GLES20.GL_TEXTURE3;
          mGLTextureII = GLES20.GL_TEXTURE4;
          mGLTextureIII = GLES20.GL_TEXTURE5;
          mGLIndexI = 3;
          mGLIndexII = 4;
          mGLTIndexIII = 5;
          break;
        case 3:
          mGLVertices = squareVertices3;
          mGLTextureI = GLES20.GL_TEXTURE6;
          mGLTextureII = GLES20.GL_TEXTURE7;
          mGLTextureIII = GLES20.GL_TEXTURE8;
          mGLIndexI = 6;
          mGLIndexII = 7;
          mGLTIndexIII = 8;
          break;
        case 4:
          mGLVertices = squareVertices4;
          mGLTextureI = GLES20.GL_TEXTURE9;
          mGLTextureII = GLES20.GL_TEXTURE10;
          mGLTextureIII = GLES20.GL_TEXTURE11;
          mGLIndexI = 9;
          mGLIndexII = 10;
          mGLTIndexIII = 11;
          break;
        case 0:
        default:
          mGLVertices = squareVertices;
          mGLTextureI = GLES20.GL_TEXTURE0;
          mGLTextureII = GLES20.GL_TEXTURE1;
          mGLTextureIII = GLES20.GL_TEXTURE2;
          mGLIndexI = 0;
          mGLIndexII = 1;
          mGLTIndexIII = 2;
          break;
      }
    }

    public boolean isProgramBuilt() {
      return isProgBuilt;
    }

    public void buildProgram() {
      if (mGLProgram <= 0) {
        mGLProgram = createProgram(VERTEX_SHADER, FRAGMENT_SHADER);
      }
      /*
       * get handle for "vPosition" and "a_texCoord"
       */
      try {
        mPositionHandle = GLES20.glGetAttribLocation(mGLProgram, "vPosition");
        checkGlError("glGetAttribLocation vPosition");
        if (mPositionHandle == -1) {
          throw new RuntimeException("Could not get attribute location for vPosition");
        }
        mCoordHandle = GLES20.glGetAttribLocation(mGLProgram, "a_texCoord");
        checkGlError("glGetAttribLocation a_texCoord");
        if (mCoordHandle == -1) {
          throw new RuntimeException("Could not get attribute location for a_texCoord");
        }
        /*
         * get uniform location for y/u/v, we pass data through these uniforms
         */
        mYhandle = GLES20.glGetUniformLocation(mGLProgram, "tex_y");
        checkGlError("glGetUniformLocation tex_y");
        if (mYhandle == -1) {
          throw new RuntimeException("Could not get uniform location for tex_y");
        }
        mUhandle = GLES20.glGetUniformLocation(mGLProgram, "tex_u");
        checkGlError("glGetUniformLocation tex_u");
        if (mUhandle == -1) {
          throw new RuntimeException("Could not get uniform location for tex_u");
        }
        mVhandle = GLES20.glGetUniformLocation(mGLProgram, "tex_v");
        checkGlError("glGetUniformLocation tex_v");
        if (mVhandle == -1) {
          throw new RuntimeException("Could not get uniform location for tex_v");
        }
        mVPMatrixHandle = GLES20.glGetUniformLocation(mGLProgram, "uMVPMatrix");
        isProgBuilt = true;
      } catch (RuntimeException e) {
        e.printStackTrace();
      }
    }

    public void releaseProgram() {
      GLES20.glUseProgram(0);
      if (mGLProgram >= 0) {
        GLES20.glDeleteProgram(mGLProgram);
      }
      mGLProgram = -1;
      isProgBuilt = false;
    }

    /**
     * build a set of textures, one for R, one for G, and one for B.
     */
    public void buildTextures(Buffer y, Buffer u, Buffer v, int width, int height) {
      boolean videoSizeChanged = (width != mVideoWidth || height != mVideoHeight);
      if (videoSizeChanged) {
        mVideoWidth = width;
        mVideoHeight = height;
      }

      // building texture for Y data
      if (mYtid < 0 || videoSizeChanged) {
        if (mYtid >= 0) {
          GLES20.glDeleteTextures(1, new int[]{mYtid}, 0);
          checkGlError("glDeleteTextures");
        }
        // GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1);
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        checkGlError("glGenTextures");
        mYtid = textures[0];
      }
      GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mYtid);
      checkGlError("glBindTexture");
      GLES20.glTexImage2D(
          GLES20.GL_TEXTURE_2D,
          0,
          GLES20.GL_LUMINANCE,
          mVideoWidth,
          mVideoHeight,
          0,
          GLES20.GL_LUMINANCE,
          GLES20.GL_UNSIGNED_BYTE,
          y);
      checkGlError("glTexImage2D");
      GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
      GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
      GLES20.glTexParameteri(
          GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
      GLES20.glTexParameteri(
          GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

      // building texture for U data
      if (mUtid < 0 || videoSizeChanged) {
        if (mUtid >= 0) {
          GLES20.glDeleteTextures(1, new int[]{mUtid}, 0);
          checkGlError("glDeleteTextures");
        }
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        checkGlError("glGenTextures");
        mUtid = textures[0];
      }
      GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mUtid);
      GLES20.glTexImage2D(
          GLES20.GL_TEXTURE_2D,
          0,
          GLES20.GL_LUMINANCE,
          mVideoWidth / 2,
          mVideoHeight / 2,
          0,
          GLES20.GL_LUMINANCE,
          GLES20.GL_UNSIGNED_BYTE,
          u);
      GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
      GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
      GLES20.glTexParameteri(
          GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
      GLES20.glTexParameteri(
          GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

      // building texture for V data
      if (mVtid < 0 || videoSizeChanged) {
        if (mVtid >= 0) {
          GLES20.glDeleteTextures(1, new int[]{mVtid}, 0);
          checkGlError("glDeleteTextures");
        }
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        checkGlError("glGenTextures");
        mVtid = textures[0];
      }
      GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mVtid);
      GLES20.glTexImage2D(
          GLES20.GL_TEXTURE_2D,
          0,
          GLES20.GL_LUMINANCE,
          mVideoWidth / 2,
          mVideoHeight / 2,
          0,
          GLES20.GL_LUMINANCE,
          GLES20.GL_UNSIGNED_BYTE,
          v);
      GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
      GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
      GLES20.glTexParameteri(
          GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
      GLES20.glTexParameteri(
          GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
    }

    public void setDisplayOrientation(int degrees, boolean needMirror) {
      if (degrees == 0) {
        if (needMirror) {
          mViewMatrix = s0MirrorMatrix;
        } else {
          mViewMatrix = s0Matrix;
        }
      } else if (degrees == 90) {
        if (needMirror) {
          mViewMatrix = s90MirrorMatrix;
        } else {
          mViewMatrix = s90Matrix;
        }
      } else if (degrees == 180) {
        if (needMirror) {
          mViewMatrix = s180MirrorMatrix;
        } else {
          mViewMatrix = s180Matrix;
        }
      } else if (degrees == 270) {
        if (needMirror) {
          mViewMatrix = s270MirrorMatrix;
        } else {
          mViewMatrix = s270Matrix;
        }
      } else {
      }
    }

    /**
     * render the frame the YUV data will be converted to RGB by shader.
     */
    public void drawFrame() {
      if (null == mVerticeBuffer) return;

      GLES20.glUseProgram(mGLProgram);
      checkGlError("glUseProgram");

      GLES20.glUniformMatrix4fv(mVPMatrixHandle, 1, false, mViewMatrix, 0);

      GLES20.glVertexAttribPointer(mPositionHandle, 2, GLES20.GL_FLOAT, false, 8, mVerticeBuffer);
      checkGlError("glVertexAttribPointer mPositionHandle");
      GLES20.glEnableVertexAttribArray(mPositionHandle);

      GLES20.glVertexAttribPointer(mCoordHandle, 2, GLES20.GL_FLOAT, false, 8, mCoordBuffer);
      checkGlError("glVertexAttribPointer maTextureHandle");
      GLES20.glEnableVertexAttribArray(mCoordHandle);

      // bind textures
      GLES20.glActiveTexture(mGLTextureI);
      GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mYtid);
      GLES20.glUniform1i(mYhandle, mGLIndexI);

      GLES20.glActiveTexture(mGLTextureII);
      GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mUtid);
      GLES20.glUniform1i(mUhandle, mGLIndexII);

      GLES20.glActiveTexture(mGLTextureIII);
      GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mVtid);
      GLES20.glUniform1i(mVhandle, mGLTIndexIII);

      GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
      GLES20.glFinish();

      GLES20.glDisableVertexAttribArray(mPositionHandle);
      GLES20.glDisableVertexAttribArray(mCoordHandle);
    }

    /**
     * create program and load shaders, fragment shader is very important.
     */
    public int createProgram(String vertexSource, String fragmentSource) {
      // create shaders
      int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
      int pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
      // just check

      int program = GLES20.glCreateProgram();
      if (program != 0) {
        GLES20.glAttachShader(program, vertexShader);
        checkGlError("glAttachShader");
        GLES20.glAttachShader(program, pixelShader);
        checkGlError("glAttachShader");
        GLES20.glLinkProgram(program);
        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] != GLES20.GL_TRUE) {
          GLES20.glDeleteProgram(program);
          program = 0;
        }
      }
      return program;
    }

    /**
     * create shader with given source.
     */
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
     * these two buffers are used for holding vertices, screen vertices and texture vertices.
     */
    void createBuffers(float[] vert) {
      mVerticeBuffer = ByteBuffer.allocateDirect(vert.length * 4);
      mVerticeBuffer.order(ByteOrder.nativeOrder());
      mVerticeBuffer.asFloatBuffer().put(vert);
      mVerticeBuffer.position(0);

      if (mCoordBuffer == null) {
        mCoordBuffer = ByteBuffer.allocateDirect(coordVertices.length * 4);
        mCoordBuffer.order(ByteOrder.nativeOrder());
        mCoordBuffer.asFloatBuffer().put(coordVertices);
        mCoordBuffer.position(0);
      }
    }

    private void checkGlError(String op) {
      int error;
      while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
        //            throw new RuntimeException(op + ": glError " + error);
      }
    }
  }

  public static class GLFrameRenderer implements SurfaceHolder.Callback {
    private static final String TAG = GLFrameRenderer.class.getSimpleName();
    private static final int sFPS = 25;

    private GLProgram prog = new GLProgram(0);
    private int mScreenWidth, mScreenHeight;
    private int mVideoWidth, mVideoHeight;
    private ByteBuffer y;
    private ByteBuffer u;
    private ByteBuffer v;

    private int mDisplayDegrees;
    private boolean mNeedMirror = false;
    private long mLastFrameTime = 0;
    private long mStanderDelta;

    public GLFrameRenderer() {
      // mParentAct = captureCallback;
      mDisplayDegrees = 0;
      mNeedMirror = false;
      mStanderDelta = 1000 / sFPS;
    }

    public void setResolution(int nWidth, int nHeight) {
      mScreenWidth = nWidth;
      mScreenHeight = nHeight;
    }

    public void onDrawFrame() {
      synchronized (this) {
        if (mLastFrameTime == 0) {
          mLastFrameTime = System.currentTimeMillis();
        } else {
          long currentTime = System.currentTimeMillis();
          long delta = currentTime - mLastFrameTime;
          if (delta < mStanderDelta) {
            try {
              sleep(mStanderDelta - delta);
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
          }
          mLastFrameTime = currentTime;
        }

        if (y != null) {
          // reset position, have to be done
          y.position(0);
          u.position(0);
          v.position(0);
          prog.buildTextures(y, u, v, mVideoWidth, mVideoHeight);
          GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
          GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
          prog.drawFrame();
        }
      }
    }

    /**
     * this method will be called from native code, it happens when the video is about to play or
     * the video size changes.
     */
    public void creatBuffer(int w, int h) {
      if (mScreenWidth > 0 && mScreenHeight > 0) {
        float f1 = 1f * mScreenHeight / mScreenWidth;
        float f2 = 1f * h / w;
        if (f1 == f2) {
          prog.createBuffers(GLProgram.squareVertices);
        } else if (f1 < f2) {
          float widScale = f1 / f2;
          prog.createBuffers(
              new float[]{
                  -widScale, -1.0f, widScale, -1.0f, -widScale, 1.0f, widScale, 1.0f,
              });
        } else {
          float heightScale = f2 / f1;
          prog.createBuffers(
              new float[]{
                  -1.0f, -heightScale, 1.0f, -heightScale, -1.0f, heightScale, 1.0f, heightScale,
              });
        }
      }

      if (w != mVideoWidth && h != mVideoHeight) {
        this.mVideoWidth = w;
        this.mVideoHeight = h;
        int yarraySize = w * h;
        int uvarraySize = yarraySize / 4;
        synchronized (this) {
          y = ByteBuffer.allocate(yarraySize);
          byte[] yInit = new byte[yarraySize];
          Arrays.fill(yInit, (byte) 0);
          y.put(yInit);

          byte[] uvInit = new byte[uvarraySize];
          Arrays.fill(uvInit, (byte) 128);

          u = ByteBuffer.allocate(uvarraySize);
          u.put(uvInit);

          v = ByteBuffer.allocate(uvarraySize);
          v.put(uvInit);
        }
      }
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
    }

    public void update(byte[] yuv420p) {
      synchronized (this) {
        y.clear();
        u.clear();
        v.clear();
        int size = this.mVideoWidth * this.mVideoHeight;
        y.put(yuv420p, 0, size);
        u.put(yuv420p, size, size / 4);
        v.put(yuv420p, size * 5 / 4, size / 4);
      }
    }

    public void setDisplayOrientation(int displayOrientation) {
      mDisplayDegrees = displayOrientation;
      prog.setDisplayOrientation(displayOrientation, mNeedMirror);
    }

    public void displayMirror(boolean mirror) {
      mNeedMirror = mirror;
      prog.setDisplayOrientation(mDisplayDegrees, mNeedMirror);
    }

    public void release() {
      prog.releaseProgram();
    }

    public void creatProgram() {
      Log.e(TAG, "GLFrameRenderer :: onSurfaceCreated");
      if (!prog.isProgramBuilt()) {
        prog.buildProgram();
      }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
      GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }
  }

  public static class GLES20Support {

    public static boolean detectOpenGLES20(Context context) {
      ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
      ConfigurationInfo info = am.getDeviceConfigurationInfo();
      return (info.reqGlEsVersion >= 0x20000);
    }

    public static Dialog getNoSupportGLES20Dialog(final Activity activity) {
      AlertDialog.Builder b = new AlertDialog.Builder(activity);
      b.setCancelable(false);
      b.setTitle("no opengl");
      b.setMessage("not support opengl");
      b.setNegativeButton(
          "exit",
          new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
              activity.finish();
            }
          });
      return b.create();
    }
  }
}
