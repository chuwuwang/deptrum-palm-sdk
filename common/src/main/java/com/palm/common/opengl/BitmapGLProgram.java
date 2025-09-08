package com.palm.common.opengl;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

import com.palm.common.opengl.OpenglHelper.RGBRendThread;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

/**
 * step to use:<br>
 * 1. new GLProgram()<br>
 * 2. buildProgram()<br>
 * 3. buildTextures()<br>
 * 4. drawFrame()<br>
 */
public class BitmapGLProgram {
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

  private static final String VERTEX_SHADER1 =
      "uniform mat4 uMVPMatrix;\n"
          + "attribute vec4 vPosition;\n"
          + "attribute vec2 a_texCoord;\n"
          + "varying vec2 tc;\n"
          + "void main() {\n"
          + "gl_Position = uMVPMatrix * vPosition;\n"
          + "tc = a_texCoord;\n"
          + "}\n";

  private static final String FRAGMENT_SHADER_RGB =
      "precision mediump float;\n"
          + "uniform sampler2D tex_y;\n"
          + "varying vec2 tc;\n"
          + "vec4 color;\n"
          + "void main() {\n"
          + "color = texture2D(tex_y,tc);\n"
          + " gl_FragColor = color.bgra;\n"
          + "}\n";

  //默认的
  private static final String FRAGMENT_SHADER_GRAY =
      "precision mediump float;\n"
          + "uniform sampler2D tex_y;\n"
          + "varying vec2 tc;\n"
          + "vec4 color;\n"
          + "void main() {\n"
          + "gl_FragColor = texture2D(tex_y,tc);\n"
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

  /**
   * 顶点坐标
   * (x,y,z)
   */
  public static float[] POSITION_VERTEX = new float[]{
      0f, 0f, 0f,     //顶点坐标V0
      1f, 1f, 0f,     //顶点坐标V1
      -1f, 1f, 0f,    //顶点坐标V2
      -1f, -1f, 0f,   //顶点坐标V3
      1f, -1f, 0f     //顶点坐标V4
  };

  /**
   * 纹理坐标
   * (s,t)
   */
  private static final float[] TEX_VERTEX = {
      0.5f, 0.5f, //纹理坐标V0
      1f, 0f,     //纹理坐标V1
      0f, 0f,     //纹理坐标V2
      0f, 1.0f,   //纹理坐标V3
      1f, 1.0f    //纹理坐标V4
  };

  /**
   * 索引
   */
  private static final short[] VERTEX_INDEX = {
      0, 1, 2,  //V0,V1,V2 三个顶点组成一个三角形
      0, 2, 3,  //V0,V2,V3 三个顶点组成一个三角形
      0, 3, 4,  //V0,V3,V4 三个顶点组成一个三角形
      0, 4, 1   //V0,V4,V1 三个顶点组成一个三角形
  };


//    //定义顶点坐标
//    static float[] squareVertices = {
//            -1.0f, -1.0f,   //左下角坐标
//            1.0f, -1.0f,    //右下角坐标
//            -1.0f, 1.0f,    //左上角坐标
//            1.0f, 1.0f,     //右上角坐标
//    }; // fullscreen

  static float squareVertices[] = {-1.0f, -1.0f, 0.0f, // V1 - bottom left
      -1.0f, 1.0f, 0.0f, // V2 - top left
      1.0f, -1.0f, 0.0f, // V3 - bottom right
      1.0f, 1.0f, 0.0f // V4 - top right
  };
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
      0.0f, 1.0f, // top left (V2)
      0.0f, 0.0f, // bottom left (V1)
      1.0f, 1.0f, // top right (V4)
      1.0f, 0.0f // bottom right (V3)
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
  public BitmapGLProgram(int position) {
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
      mGLProgram = createProgram(VERTEX_SHADER1, FRAGMENT_SHADER_GRAY);
    }
    try {
      mVPMatrixHandle = GLES20.glGetUniformLocation(mGLProgram, "uMVPMatrix");
      mPositionHandle = GLES20.glGetAttribLocation(mGLProgram, "vPosition");
      RGBRendThread.ShaderUtil.checkGlError("glGetAttribLocation vPosition");
      if (mPositionHandle == -1) {
        throw new RuntimeException("Could not get attribute location for vPosition");
      }
      mCoordHandle = GLES20.glGetAttribLocation(mGLProgram, "a_texCoord");
      RGBRendThread.ShaderUtil.checkGlError("glGetAttribLocation a_texCoord");
      if (mCoordHandle == -1) {
        throw new RuntimeException("Could not get attribute location for a_texCoord");
      }
      mYhandle = GLES20.glGetUniformLocation(mGLProgram, "tex_y");
      RGBRendThread.ShaderUtil.checkGlError("glGetUniformLocation tex_y");
      if (mYhandle == -1) {
        throw new RuntimeException("Could not get uniform location for tex_y");
      }

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


  public int createTexture(Bitmap bitmap) {
    int[] texture = new int[1];
//        if (mYtid >= 0) {
//            GLES20.glDeleteTextures(1, new int[]{mYtid}, 0);
//            RGBRendThread.ShaderUtil.checkGlError("glDeleteTextures");
//        }
    if (bitmap != null && !bitmap.isRecycled()) {
      //生成纹理
      GLES20.glGenTextures(1, texture, 0);

      //生成纹理
      GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[0]);
      //设置缩小过滤为使用纹理中坐标最接近的一个像素的颜色作为需要绘制的像素颜色
      GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
      //设置放大过滤为使用纹理中坐标最接近的若干个颜色，通过加权平均算法得到需要绘制的像素颜色
      GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
      //设置环绕方向S，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
      GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
      //设置环绕方向T，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
      GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
      //根据以上指定的参数，生成一个2D纹理
      GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
      mYtid = texture[0];
//            Log.e("opengl", "mYtid:" + mYtid);
//            GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
//            // 数据如果已经被加载进OpenGL,则可以回收该bitmap
//            bitmap.recycle();
//
//            // 取消绑定纹理
//            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
      return texture[0];
    }
    return 0;
  }

  public void buildTextures(Bitmap bitmap, int width, int height) {
    boolean videoSizeChanged = (width != mVideoWidth || height != mVideoHeight);
    if (videoSizeChanged) {
      mVideoWidth = width;
      mVideoHeight = height;
    }

    if (mYtid < 0 || videoSizeChanged) {
      if (mYtid >= 0) {
        GLES20.glDeleteTextures(1, new int[]{mYtid}, 0);
        RGBRendThread.ShaderUtil.checkGlError("glDeleteTextures");
      }
      int[] textures = new int[1];
      GLES20.glGenTextures(1, textures, 0);
      RGBRendThread.ShaderUtil.checkGlError("glGenTextures");
      mYtid = textures[0];
      Log.e("opengl", "buildTextures mYtid:" + mYtid);
    }
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mYtid);
    RGBRendThread.ShaderUtil.checkGlError("glBindTexture");

    //根据以上指定的参数，生成一个2D纹理
    GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
    RGBRendThread.ShaderUtil.checkGlError("glTexImage2D");
    GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
    GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
    Log.e("opengl", "buildTextures--bitmap");
    // Clean up
//        bitmap.recycle();
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
//        GLES20.glUniform1i(mYhandle, mGLIndexI);

    //        GLES20.glActiveTexture(mGLTextureII);

    //        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mUtid);
    //        GLES20.glUniform1i(mUhandle, mGLIndexII);
    //
    //        GLES20.glActiveTexture(mGLTextureIII);
    //        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mVtid);
    //        GLES20.glUniform1i(mVhandle, mGLTIndexIII);

//        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
//        GLES20.glFinish();
    GLES20.glDrawElements(GLES20.GL_TRIANGLES, VERTEX_INDEX.length, GLES20.GL_UNSIGNED_SHORT, mVertexIndexBuffer);

    GLES20.glDisableVertexAttribArray(mPositionHandle);
    GLES20.glDisableVertexAttribArray(mCoordHandle);
    Log.e("opengl", "draw--bitmap");

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

  ShortBuffer mVertexIndexBuffer;

  /**
   * these two buffers are used for holding vertices, screen vertices and texture vertices.
   */
  void createBuffers(float[] vert) {
    mVerticeBuffer = ByteBuffer.allocateDirect(vert.length * 4);
    mVerticeBuffer.order(ByteOrder.nativeOrder());
    mVerticeBuffer.asFloatBuffer().put(vert);
    mVerticeBuffer.position(0);

    if (mCoordBuffer == null) {
      mCoordBuffer = ByteBuffer.allocateDirect(TEX_VERTEX.length * 4);
      mCoordBuffer.order(ByteOrder.nativeOrder());
      mCoordBuffer.asFloatBuffer().put(coordVertices);
      mCoordBuffer.position(0);
    }

    mVertexIndexBuffer = ByteBuffer.allocateDirect(VERTEX_INDEX.length * 2)
        .order(ByteOrder.nativeOrder())
        .asShortBuffer()
        .put(VERTEX_INDEX);
    mVertexIndexBuffer.position(0);
  }

  private void checkGlError(String op) {
    int error;
    while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
      //            throw new RuntimeException(op + ": glError " + error);
    }
  }
}
