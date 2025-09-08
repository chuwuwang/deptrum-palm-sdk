package com.palm.common.opengl;

import android.opengl.GLES31;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayList;

/**
 * @author BiCheng
 * @date 2023/7/7  10:15
 * @description:
 **/
public class PointCloudGLProgram {

  private static final String VERTEX_SHADER =
      "#version 310 es\n"
          + "precision highp float;\n"
          + "uniform mat4 u_mvp_matrix;\n"
          + "layout (location = 0) in vec3 in_position;\n"
          + "layout (location = 1) in vec3 in_color;\n"
          + "uniform float u_point_size;\n"
          + "out vec3 point_color;\n"
          + "void main()\n"
          + "{\n"
          + "    point_color = in_color;\n"
          + "    gl_Position = u_mvp_matrix*vec4(in_position,1.0);\n"
          + "    gl_PointSize = 1.0f;\n"
          + "}\n";

  private static final String FRAGMENT_SHADER =
      "#version 310 es\n"
          + "precision highp float;\n"
          + "in vec3 point_color;\n"
          + "out vec4 color;\n"
          + "void main() {\n"
          + "    color=vec4(point_color.rgb,1.0);\n"
          + "}\n";

  static float[] s0Matrix = {
//      -0.528972f,  0.0540315f,  0.0865947f,  0.0783476f,
//      -0.0540315f,  0.63584f,  -0.0361512f,  -0.0327082f,
//      0.388272f,  0.162094f,  0.112944f,  0.102187f,
//      0f,  0f,  -168.421f,  800f
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


//  private static final ArrayList<PointsXYZRGBA> mGrid = new ArrayList<>();
  private static final ArrayList<PointsXYZRGBA> mAxisXyzRgba = new ArrayList<>();
  private static final ArrayList<PointsXYZRGBA> mArrowXyzRgba = new ArrayList<>();

  //坐标轴箭头三角形高度值
  private static final float kArrowHalfHeight = 35;
  //坐标轴长度
  private static final int kAxisLength = 1000;
  //坐标系单位 mm
  private static float units = 1.0f;
  //网格灰度值
  private static final int kMeshGrayColor = 160;
  //网格每格大小
  private static final float kMeshCellSize = 1000.0f;
  //网格每个方向上数量/2的值
  private static final int kMeshRepetitionPerside = 20;

  private float[] mViewMatrix = new float[16];
  private float[] mLastMatrix = new float[16];
  private int mVPMatrixHandle = -1;
  private int mUPointSizeHandle = -1;
  public final int mWinPosition;
  private int mGLProgram;

  private int mGLTextureI;
  private int mGLTextureII;
  private int mGLTextureIII;

  private int mGLIndexI;
  private int mGLIndexII;
  private int mGLTIndexIII;

  private float[] mGLVertices;

  private int mPositionHandle = 0, mCoordHandle = 1;
  private int vaoPoint = -1, vboPoint;

  private ByteBuffer mAxisBuffer;
  private ByteBuffer mArrowBuffer;

  private boolean isProgBuilt = false;

  /**
   * position can only be 0~4:<br>
   * fullscreen => 0<br>
   * left-top => 1<br>
   * right-top => 2<br>
   * left-bottom => 3<br>
   * right-bottom => 4
   */
  public PointCloudGLProgram(int position) {
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
        mGLTextureI = GLES31.GL_TEXTURE0;
        mGLTextureII = GLES31.GL_TEXTURE1;
        mGLTextureIII = GLES31.GL_TEXTURE2;
        mGLIndexI = 0;
        mGLIndexII = 1;
        mGLTIndexIII = 2;
        break;
      case 2:
        mGLVertices = squareVertices2;
        mGLTextureI = GLES31.GL_TEXTURE3;
        mGLTextureII = GLES31.GL_TEXTURE4;
        mGLTextureIII = GLES31.GL_TEXTURE5;
        mGLIndexI = 3;
        mGLIndexII = 4;
        mGLTIndexIII = 5;
        break;
      case 3:
        mGLVertices = squareVertices3;
        mGLTextureI = GLES31.GL_TEXTURE6;
        mGLTextureII = GLES31.GL_TEXTURE7;
        mGLTextureIII = GLES31.GL_TEXTURE8;
        mGLIndexI = 6;
        mGLIndexII = 7;
        mGLTIndexIII = 8;
        break;
      case 4:
        mGLVertices = squareVertices4;
        mGLTextureI = GLES31.GL_TEXTURE9;
        mGLTextureII = GLES31.GL_TEXTURE10;
        mGLTextureIII = GLES31.GL_TEXTURE11;
        mGLIndexI = 9;
        mGLIndexII = 10;
        mGLTIndexIII = 11;
        break;
      case 0:
      default:
        mGLVertices = squareVertices;
        mGLTextureI = GLES31.GL_TEXTURE0;
        mGLTextureII = GLES31.GL_TEXTURE1;
        mGLTextureIII = GLES31.GL_TEXTURE2;
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
     * get handle for "u_mvp_matrix" and "u_point_size"
     */
    try {
      mVPMatrixHandle = GLES31.glGetUniformLocation(mGLProgram, "u_mvp_matrix");
//      mUPointSizeHandle = GLES31.glGetUniformLocation(mGLProgram, "u_point_size");
      if (mVPMatrixHandle < 0 /*|| mUPointSizeHandle < 0*/) {
        throw new RuntimeException("Could not get uniform location for mVPMatrixHandle or mUPointSizeHandle");
      }
      IntBuffer vaoBuffer = IntBuffer.allocate(1);
      GLES31.glGenVertexArrays(1, vaoBuffer);
      IntBuffer vboBuffer = IntBuffer.allocate(1);
      GLES31.glGenBuffers(1, vboBuffer);
      vaoPoint = vaoBuffer.get();
      vboPoint = vboBuffer.get();
      GLES31.glBindVertexArray(vaoPoint);
      GLES31.glBindBuffer(GLES31.GL_ARRAY_BUFFER, vboPoint);

      isProgBuilt = true;

    } catch (RuntimeException e) {
      e.printStackTrace();
    }
  }

  public void releaseProgram() {
    GLES31.glUseProgram(0);
    if (mGLProgram >= 0) {
      GLES31.glDeleteProgram(mGLProgram);
    }
    mGLProgram = -1;
    isProgBuilt = false;
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

  public void initRotateParam() {
    mLastMatrix = mViewMatrix.clone();
  }

  public void setScale(float scale) {
    Matrix.scaleM(mViewMatrix, 0, scale, scale, scale);
  }

  public void rotate(float xDiff, float yDiff) {
    //角度弧度转换 /53.7
    final float kDistanceToAngle = 3;
    float distance = (float) Math.sqrt(xDiff * xDiff + yDiff * yDiff);
    float angle = distance / kDistanceToAngle;
    float[] result = new float[4];
    Matrix.multiplyMV(result, 0, mLastMatrix, 0, new float[]{yDiff, -xDiff, 0f, 1.0f}, 0);
    Matrix.rotateM(mViewMatrix, 0, mLastMatrix, 0, angle, result[0], result[1], result[2]);
  }


  public void draw(Buffer xyzRgbBuffer, int pointsNum, int pointSize, int mode) {
    GLES31.glUseProgram(mGLProgram);
    checkGlError("glUseProgram");
    GLES31.glBindVertexArray(vaoPoint);

    GLES31.glBufferData(GLES31.GL_ARRAY_BUFFER,
        pointsNum * 16,
        xyzRgbBuffer,
        GLES31.GL_DYNAMIC_DRAW);


    GLES31.glUniformMatrix4fv(mVPMatrixHandle, 1, false, mViewMatrix, 0);

    GLES31.glVertexAttribPointer(mPositionHandle, 3, GLES31.GL_FLOAT, false, 16, 0);
    checkGlError("glVertexAttribPointer mPositionHandle");
    GLES31.glEnableVertexAttribArray(mPositionHandle);

    GLES31.glVertexAttribPointer(mCoordHandle, 3, GLES31.GL_UNSIGNED_BYTE, true, 16, 12);
    checkGlError("glVertexAttribPointer maTextureHandle");
    GLES31.glEnableVertexAttribArray(mCoordHandle);

    GLES31.glUniform1f(mUPointSizeHandle, pointSize);


    GLES31.glDrawArrays(mode, 0, pointsNum);
    checkGlError("glDrawArrays");
    GLES31.glFinish();

    GLES31.glDisableVertexAttribArray(mPositionHandle);
    GLES31.glDisableVertexAttribArray(mCoordHandle);
  }

  /**
   * create program and load shaders, fragment shader is very important.
   */
  public int createProgram(String vertexSource, String fragmentSource) {
    // create shaders
    int vertexShader = loadShader(GLES31.GL_VERTEX_SHADER, vertexSource);
    int pixelShader = loadShader(GLES31.GL_FRAGMENT_SHADER, fragmentSource);
    // just check

    int program = GLES31.glCreateProgram();
    if (program != 0) {
      GLES31.glAttachShader(program, vertexShader);
      checkGlError("glAttachShader");
      GLES31.glAttachShader(program, pixelShader);
      checkGlError("glAttachShader");
      GLES31.glLinkProgram(program);
      int[] linkStatus = new int[1];
      GLES31.glGetProgramiv(program, GLES31.GL_LINK_STATUS, linkStatus, 0);
      if (linkStatus[0] != GLES31.GL_TRUE) {
        GLES31.glDeleteProgram(program);
        program = 0;
      }
    }
    return program;
  }

  /**
   * create shader with given source.
   */
  private int loadShader(int shaderType, String source) {
    int shader = GLES31.glCreateShader(shaderType);
    if (shader != 0) {
      GLES31.glShaderSource(shader, source);
      GLES31.glCompileShader(shader);
      int[] compiled = new int[1];
      GLES31.glGetShaderiv(shader, GLES31.GL_COMPILE_STATUS, compiled, 0);
      if (compiled[0] == 0) {
        Log.e("PointCloudGL", GLES31.glGetShaderInfoLog(shader));
        GLES31.glDeleteShader(shader);
        shader = 0;
      }
    } else {
      throw new RuntimeException("shader is 0");
    }
    return shader;
  }

  /**
   * create 3D Coordinate axis
   */
  void createCoordinateAxis() {
    initAxisArrowBuffer(kAxisLength / units, kArrowHalfHeight / units);
  }

  private void checkGlError(String op) {
    int error;
    while ((error = GLES31.glGetError()) != GLES31.GL_NO_ERROR) {
      throw new RuntimeException(op + ": glError " + error);
    }
  }

  /**
   * draw grid
   */
  public void drawGrid() {
  }

  /**
   * draw axis
   */
  public void drawAxis() {
    if (mAxisBuffer == null || mArrowBuffer == null) {
      return;
    }

    draw(mAxisBuffer, mAxisXyzRgba.size(), 16, GLES31.GL_LINES);
    draw(mArrowBuffer, mArrowXyzRgba.size(), 16, GLES31.GL_TRIANGLES);
  }

  public void drawPointsCloud(ByteBuffer pointCloudBuffer) {
    if (pointCloudBuffer.capacity() != 0 && pointCloudBuffer.capacity() % 16 != 0) {
      return;
    }
    draw(pointCloudBuffer, pointCloudBuffer.capacity() / 16, 16, GLES31.GL_POINTS);
  }

  private synchronized void initAxisArrowBuffer(float length, float arrow_height) {
    Log.e("LBC_", "initAxisArrowBuffer()");
    float[] axis = {
        -length, 0, 0, 255, 0, 0,
        length, 0, 0, 255, 0, 0,  // x
        0, -length, 0, 0, 255, 0,
        0, length, 0, 0, 255, 0,  // y
        0, 0, -length, 0, 0, 255,
        0, 0, length, 0, 0, 255,  // z
    };
    mAxisXyzRgba.clear();
    for (int i = 0; i < 6; ++i) {
      PointsXYZRGBA pointsXyzRgba = new PointsXYZRGBA();
      pointsXyzRgba.x = axis[i * 6];
      pointsXyzRgba.y = axis[i * 6 + 1];
      pointsXyzRgba.z = axis[i * 6 + 2];
      pointsXyzRgba.r = (byte) (axis[i * 6 + 3]);
      pointsXyzRgba.g = (byte) (axis[i * 6 + 4]);
      pointsXyzRgba.b = (byte) (axis[i * 6 + 5]);
      mAxisXyzRgba.add(pointsXyzRgba);
    }
    // draw arrow
    int kArrowSize = 18;  //每个坐标轴2个箭头三角形，每个三角形3个点
    //需要kArrowSize个点，每个点需要6个变量，xyzrgb
    float[] arrow = {
        // XOY
        length /*+ center.x*/,
        0,
        0,
        255,
        0,
        0,
        length /*+ center.x*/ - 2 * arrow_height,
        arrow_height,
        0,
        255,
        0,
        0,
        length /*+ center.x*/ - 2 * arrow_height,
        -arrow_height,
        0,
        255,
        0,
        0,
        // XOZ
        length /*+ center.x*/,
        0,
        0,
        255,
        0,
        0,
        length /*+ center.x*/ - 2 * arrow_height,
        0,
        arrow_height,
        255,
        0,
        0,
        length /*+ center.x*/ - 2 * arrow_height,
        0,
        -arrow_height,
        255,
        0,
        0,
        // YOX
        0,
        length /*+ center.x*/,
        0,
        0,
        255,
        0,
        arrow_height,
        length /*+ center.x*/ - 2 * arrow_height,
        0,
        0,
        255,
        0,
        -arrow_height,
        length /*+ center.x*/ - 2 * arrow_height,
        0,
        0,
        255,
        0,
        // YOZ
        0,
        length /*+ center.x*/,
        0,
        0,
        255,
        0,
        0,
        length /*+ center.x*/ - 2 * arrow_height,
        arrow_height,
        0,
        255,
        0,
        0,
        length /*+ center.x*/ - 2 * arrow_height,
        -arrow_height,
        0,
        255,
        0,
        // ZOX
        0,
        0,
        length /*+ center.x*/,
        0,
        0,
        255,
        arrow_height,
        0,
        length /*+ center.x*/ - 2 * arrow_height,
        0,
        0,
        255,
        -arrow_height,
        0,
        length /*+ center.x*/ - 2 * arrow_height,
        0,
        0,
        255,
        // ZOY
        0,
        0,
        length /*+ center.x*/,
        0,
        0,
        255,
        0,
        arrow_height,
        length /*+ center.x*/ - 2 * arrow_height,
        0,
        0,
        255,
        0,
        -arrow_height,
        length /*+ center.x*/ - 2 * arrow_height,
        0,
        0,
        255};
    mArrowXyzRgba.clear();
    for (int i = 0; i < kArrowSize; ++i) {
      PointsXYZRGBA pointsXyzRgba = new PointsXYZRGBA();
      pointsXyzRgba.x = arrow[i * 6];
      pointsXyzRgba.y = arrow[i * 6 + 1];
      pointsXyzRgba.z = arrow[i * 6 + 2];
      pointsXyzRgba.r = (byte) (arrow[i * 6 + 3]);
      pointsXyzRgba.g = (byte) (arrow[i * 6 + 4]);
      pointsXyzRgba.b = (byte) (arrow[i * 6 + 5]);
      mArrowXyzRgba.add(pointsXyzRgba);
    }

    if (mAxisBuffer == null) {
      mAxisBuffer = ByteBuffer.allocate(mAxisXyzRgba.size() * 16);
      mAxisBuffer.order(ByteOrder.nativeOrder());
    }
    for (PointsXYZRGBA pointsXyxRgba : mAxisXyzRgba) {
      mAxisBuffer.putFloat(pointsXyxRgba.x);
      mAxisBuffer.putFloat(pointsXyxRgba.y);
      mAxisBuffer.putFloat(pointsXyxRgba.z);
      mAxisBuffer.put(pointsXyxRgba.r);
      mAxisBuffer.put(pointsXyxRgba.g);
      mAxisBuffer.put(pointsXyxRgba.b);
      mAxisBuffer.put(pointsXyxRgba.a);
    }
    mAxisBuffer.position(0);

    if (mArrowBuffer == null) {
      mArrowBuffer = ByteBuffer.allocate(mArrowXyzRgba.size() * 16);
      mArrowBuffer.order(ByteOrder.nativeOrder());
    }
    for (PointsXYZRGBA pointsXyxRgba : mArrowXyzRgba) {
      mArrowBuffer.putFloat(pointsXyxRgba.x);
      mArrowBuffer.putFloat(pointsXyxRgba.y);
      mArrowBuffer.putFloat(pointsXyxRgba.z);
      mArrowBuffer.put(pointsXyxRgba.r);
      mArrowBuffer.put(pointsXyxRgba.g);
      mArrowBuffer.put(pointsXyxRgba.b);
      mArrowBuffer.put(pointsXyxRgba.a);
    }
    mArrowBuffer.position(0);
  }

}
