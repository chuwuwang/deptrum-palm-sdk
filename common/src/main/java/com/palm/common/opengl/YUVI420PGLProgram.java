package com.palm.common.opengl;

import android.opengl.GLES20;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * step to use:<br>
 * 1. new GLProgram()<br>
 * 2. buildProgram()<br>
 * 3. buildTextures()<br>
 * 4. drawFrame()<br>
 */
public class YUVI420PGLProgram {
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
  public YUVI420PGLProgram(int position) {
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
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

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
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

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
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
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
