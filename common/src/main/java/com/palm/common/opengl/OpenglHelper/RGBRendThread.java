package com.palm.common.opengl.OpenglHelper;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.opengl.GLES20;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class RGBRendThread extends BaseOpenglRenderThread implements SurfaceHolder.Callback {
  public RGBRendThread(String name, SurfaceView mSurfaceView) {
    super(name, mSurfaceView);
    TAG = RGBRendThread.class.getSimpleName();
    setEGLContextClientVersion(2);
    mStanderDelta = 1000 / sFPS;
  }

  @Override
  protected void creatProgram() {
    GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
    mGLGraphics = new GLGraphics();
    if (!mGLGraphics.isProgramBuilt()) {
      mGLGraphics.buildProgram();
    }
    GLES20.glEnable(GLES20.GL_DEPTH_TEST);
    GLES20.glEnable(GLES20.GL_CULL_FACE);
  }

  @Override
  protected void draw() {
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
      GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
      GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
      if (mBufferImage != null || mIntBuffer != null) {

        if (mIntBuffer != null) {
          mIntBuffer.position(0);
          mGLGraphics.buildTextures(mIntBuffer, mFrameWidth, mFrameHeight, hasAlpha);
        } else if (mBufferImage != null) {
          mBufferImage.position(0);
          mGLGraphics.buildTextures(mBufferImage, mFrameWidth, mFrameHeight, hasAlpha);
        }
      }
      mGLGraphics.draw();
    }
  }

  @Override
  public void surfaceCreated(SurfaceHolder holder) {
    super.surfaceCreated(holder);
  }

  @Override
  public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    super.surfaceChanged(holder, format, width, height);
    GLES20.glViewport(0, 0, width, height);
  }

  public void update(ByteBuffer buffer, int width, int height) {
    if (mGLGraphics == null) {
      return;
    }
    mFrameWidth = width;
    mFrameHeight = height;
    mGLGraphics.setVertexData(null);
    mBufferImage = buffer;
    notifyDraw();
  }

  public void update(int[] colors, int width, int height) {
    if (mGLGraphics == null) {
      return;
    }
    if (mIntBuffer == null) {
      mIntBuffer = ByteBuffer.allocateDirect(640 * 480 * 4).asIntBuffer();
    }
    mFrameWidth = width;
    mFrameHeight = height;
    mGLGraphics.setVertexData(null);
    mIntBuffer.clear();
    mIntBuffer.put(colors);
    notifyDraw();
  }

  private GLGraphics mGLGraphics;
  private ByteBuffer mBufferImage;
  private IntBuffer mIntBuffer;
  private boolean bWork = true;
  private int mFrameWidth;
  private int mFrameHeight;
  private long mLastFrameTime = 0;
  private long mStanderDelta;
  private static final int sFPS = 25;

  public static class ShaderUtil {
    public static int loadShader(int shaderType, String source) {
      int shader = GLES20.glCreateShader(shaderType);
      if (shader != 0) {
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);
        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
          Log.e("ES20_ERROR", "Could not compile shader " + shaderType + ":");
          Log.e("ES20_ERROR", GLES20.glGetShaderInfoLog(shader));
          GLES20.glDeleteShader(shader);
          shader = 0;
        }
      }
      return shader;
    }

    public static int createProgram(String vertexSource, String fragmentSource) {
      int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
      if (vertexShader == 0) {
        return 0;
      }

      int pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
      if (pixelShader == 0) {
        return 0;
      }

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
          Log.e("ES20_ERROR", "Could not link program: ");
          Log.e("ES20_ERROR", GLES20.glGetProgramInfoLog(program));
          GLES20.glDeleteProgram(program);
          program = 0;
        }
      }
      return program;
    }

    public static void checkGlError(String op) {
      int error;
      while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
        Log.e("ES20_ERROR", op + ": glError " + error);
        throw new RuntimeException(op + ": glError " + error);
      }
    }

    public static String loadFromAssetsFile(String fname, Resources r) {
      String result = null;
      try {
        InputStream in = r.getAssets().open(fname);
        int ch = 0;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while ((ch = in.read()) != -1) {
          baos.write(ch);
        }
        byte[] buff = baos.toByteArray();
        baos.close();
        in.close();
        result = new String(buff, "UTF-8");
        result = result.replaceAll("\\r\\n", "\n");
      } catch (Exception e) {
        e.printStackTrace();
      }
      return result;
    }
  }

  public static class GLGraphics {

    private static float[] squareVertices = {-1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f};

    private static float[] coordVertices = {0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f};

    private static final String VERTEX_SHADER =
        "attribute vec4 vPosition;\n"
            + "attribute vec2 a_texCoord;\n"
            + "varying vec2 tc;\n"
            + "void main() {\n"
            + "gl_Position = vPosition;\n"
            + "tc = a_texCoord;\n"
            + "}\n";

    private static final String FRAGMENT_SHADER =
        "precision mediump float;\n"
            + "uniform sampler2D tex_y;\n"
            + "varying vec2 tc;\n"
            + "void main() {\n"
            + "gl_FragColor = texture2D(tex_y,tc);\n"
            + "}\n";

    private FloatBuffer mVertexBuffer;
    private FloatBuffer mColorBuffer;
    private ByteBuffer coordbuffer;
    private ByteBuffer verticebuffer;

    private int mVertexCount = -1;
    private int mProgram = 0;
    private int mTexture = 0;
    private int mIndex = 0;
    private int mPositionHandle = -1, mCoordHandle = -1;
    private int yhandle = -1;
    private int ytid = -1;
    private int mGraphWidth = -1;
    private int mGraphHeight = -1;

    private boolean isProgBuilt = false;

    public GLGraphics() {
      mTexture = GLES20.GL_TEXTURE0;

      createBuffers();
    }

    private void createBuffers() {
      verticebuffer = ByteBuffer.allocateDirect(squareVertices.length * 4);
      verticebuffer.order(ByteOrder.nativeOrder());
      verticebuffer.asFloatBuffer().put(squareVertices);
      verticebuffer.position(0);

      coordbuffer = ByteBuffer.allocateDirect(coordVertices.length * 4);
      coordbuffer.order(ByteOrder.nativeOrder());
      coordbuffer.asFloatBuffer().put(coordVertices);
      coordbuffer.position(0);
    }

    public void setVertexData(float[] vertices) {
      if (vertices != null) {
        mVertexCount = vertices.length / 3;
        ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length * 4);
        vbb.order(ByteOrder.nativeOrder());
        mVertexBuffer = vbb.asFloatBuffer();
        mVertexBuffer.put(vertices);
        mVertexBuffer.position(0);

        float colors[] = new float[mVertexCount * 4];
        for (int i = 0; i < colors.length; i++) {
          if (i % 4 == 3) {
            colors[i] = 0;
          } else {
            colors[i] = 1;
          }
        }

        ByteBuffer cbb = ByteBuffer.allocateDirect(colors.length * 4);
        cbb.order(ByteOrder.nativeOrder());
        mColorBuffer = cbb.asFloatBuffer();
        mColorBuffer.put(colors);
        mColorBuffer.position(0);
      }
    }

    @SuppressLint("NewApi")
    public void draw() {
      GLES20.glUseProgram(mProgram);
      ShaderUtil.checkGlError("glUseProgram");
      GLES20.glVertexAttribPointer(mPositionHandle, 2, GLES20.GL_FLOAT, false, 8, verticebuffer);
      ShaderUtil.checkGlError("glVertexAttribPointer mPositionHandle");
      GLES20.glEnableVertexAttribArray(mPositionHandle);
      GLES20.glVertexAttribPointer(mCoordHandle, 2, GLES20.GL_FLOAT, false, 8, coordbuffer);
      ShaderUtil.checkGlError("glVertexAttribPointer maTextureHandle");
      GLES20.glEnableVertexAttribArray(mCoordHandle);

      // bind textures
      GLES20.glActiveTexture(mTexture);
      GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, ytid);
      GLES20.glUniform1i(yhandle, mIndex);
      GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
      GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

      GLES20.glDisableVertexAttribArray(mPositionHandle);
      GLES20.glDisableVertexAttribArray(mCoordHandle);
    }

    public boolean isProgramBuilt() {
      return isProgBuilt;
    }

    @SuppressLint("NewApi")
    public void buildProgram() {
      if (mProgram <= 0) {
        mProgram = ShaderUtil.createProgram(VERTEX_SHADER, FRAGMENT_SHADER);
      }
      mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
      ShaderUtil.checkGlError("glGetAttribLocation vPosition");
      if (mPositionHandle == -1) {
        throw new RuntimeException("Could not get attribute location for vPosition");
      }
      mCoordHandle = GLES20.glGetAttribLocation(mProgram, "a_texCoord");
      ShaderUtil.checkGlError("glGetAttribLocation a_texCoord");
      if (mCoordHandle == -1) {
        throw new RuntimeException("Could not get attribute location for a_texCoord");
      }
      yhandle = GLES20.glGetUniformLocation(mProgram, "tex_y");
      ShaderUtil.checkGlError("glGetUniformLocation tex_y");
      if (yhandle == -1) {
        throw new RuntimeException("Could not get uniform location for tex_y");
      }

      isProgBuilt = true;
    }

    @SuppressLint("NewApi")
    public void buildTextures(Buffer rgbBuffer, int width, int height, boolean hasAlpha) {
      boolean videoSizeChanged = (width != mGraphWidth || height != mGraphHeight);
      if (videoSizeChanged) {
        mGraphWidth = width;
        mGraphHeight = height;
      }

      if (ytid < 0 || videoSizeChanged) {
        if (ytid >= 0) {
          GLES20.glDeleteTextures(1, new int[]{ytid}, 0);
          ShaderUtil.checkGlError("glDeleteTextures");
        }
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        ShaderUtil.checkGlError("glGenTextures");
        ytid = textures[0];
      }
      GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, ytid);
      ShaderUtil.checkGlError("glBindTexture");

      GLES20.glTexImage2D(
          GLES20.GL_TEXTURE_2D,
          0,
          hasAlpha ? GLES20.GL_RGBA : GLES20.GL_RGB,
          mGraphWidth,
          mGraphHeight,
          0,
          hasAlpha ? GLES20.GL_RGBA : GLES20.GL_RGB,
          GLES20.GL_UNSIGNED_BYTE,
          rgbBuffer);
      ShaderUtil.checkGlError("glTexImage2D");
      GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
      GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
      GLES20.glTexParameteri(
          GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
      GLES20.glTexParameteri(
          GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
    }

    public void buildTextures(Buffer rgbBuffer, int width, int height) {
      buildTextures(rgbBuffer, width, height, false);
    }
  }

  public static class DecodePanel {

    private MediaCodec mCodec;

    public DecodePanel() {
    }

    public void initDecoder(Surface surface, int width, int height) {
      // init decoder
      try {
        mCodec = MediaCodec.createDecoderByType("video/avc");
        MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", width, height);
        mCodec.configure(mediaFormat, surface, null, 0);
        mCodec.start();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    public void stopDecoder() {
      // stop decoder
      if (mCodec != null) {
        mCodec.stop();
        mCodec.release();
        mCodec = null;
      }
    }

    public void paint(ByteBuffer bufferImage, long timeStamp) {
      // queue inputbuffer
      if (bufferImage != null) {
        try {
          int inputBufferIndex = mCodec.dequeueInputBuffer(1000);
          if (inputBufferIndex >= 0) {
            ByteBuffer[] inputBuffers = mCodec.getInputBuffers();
            ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
            inputBuffer.clear();
            inputBuffer.put(bufferImage);
            mCodec.queueInputBuffer(inputBufferIndex, 0, bufferImage.capacity(), timeStamp, 0);
          }
        } catch (Exception e) {

        }
      }

      // release outputbuffer
      try {
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        int outputBufferIndex = mCodec.dequeueOutputBuffer(info, 1000);
        if (outputBufferIndex >= 0) {
          mCodec.releaseOutputBuffer(outputBufferIndex, info.size != 0);
        }
      } catch (Exception e) {

      }
    }
  }
}
