package com.palm.demo.activity;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.api.stream.BaseDeviceImpl;
import com.api.stream.Device;
import com.api.stream.Frame;
import com.api.stream.Frames;
import com.api.stream.ICapturePalmCallback;
import com.api.stream.IDevice;
import com.api.stream.IOpenCallback;
import com.api.stream.IStream;
import com.api.stream.StreamType;
import com.api.stream.bean.BBox;
import com.api.stream.bean.CaptureFrame;
import com.api.stream.bean.ClientPalmOutput;
import com.api.stream.bean.DeviceInfo;
import com.api.stream.bean.ExtraFrameInfo;
import com.api.stream.bean.ExtractOutput;
import com.api.stream.bean.ImageInstance;
import com.api.stream.enumclass.Hint;
import com.api.stream.manager.DtUsbDevice;
import com.api.stream.manager.DtUsbManager;
import com.api.stream.manager.UsbMapTable;
import com.api.stream.veinshine.IVeinshine;
import com.palm.common.opengl.GLDisplay;
import com.palm.common.opengl.GLFrameSurface;
import com.palm.demo.R;
import com.palm.demo.custom.DtRectRoiView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author BiCheng
 * @date 2024/3/15  17:56
 * @description:
 **/
public class ExampleActivity extends AppCompatActivity {

  private final String TAG = getClass().getSimpleName();

  private GLFrameSurface mGLIrView, mGLRgbView;

  GLDisplay rgbDisPlay, irDisPlay;

  private Button mBtnOpen, mBtnEnable, mBtnCapture, mBtnCaptureOnce, mBtnStopCapture;
  private Button mBtnCreatePalmClient, mBtnRegisterToServer, mBtnDeleteId, mBtnQueryFromServer;
  private TextView mTvDeviceInfo;
  private Switch mSwitchStartStream;
  private Spinner mSpinnerStreamMode;
  private ExecutorService deviceThread1 = Executors.newSingleThreadExecutor();
  private ExecutorService workServices = Executors.newSingleThreadExecutor();
  private volatile IDevice mDevice = null;
  private Handler mainHandler;
  private ArrayAdapter<StreamType> mAdapterStreamType;
  private final List<StreamType> mListStreamType = new ArrayList<>();
  private StreamType currentStreamType = StreamType.INVALID_STREAM_TYPE;
  private volatile boolean mIsRunning;
  private volatile boolean mIsOpenCamera;

  private byte[] rgbFrameData1 = null;
  private byte[] irFrameData1 = null;
  private ExtraFrameInfo irFrameExtraInfo = null;
  private ExtraFrameInfo rgbFrameExtraInfo = null;
  private int irFrameW1;
  private int irFrameH1;
  private int rgbFrameW1;
  private int rgbFrameH1;

  public DtRectRoiView mRectRoiRgbView;
  public DtRectRoiView mRectRoiIrView;

  protected Bitmap mRgbBitmap, mIrBitmap, mDepthBitmap;
  private ImageView rgbImage, irImage, depthImage;
  private TextView tvResult;


  private volatile EnableAlgorithmStatus algoStatus = EnableAlgorithmStatus.DISABLE;

  public static enum EnableAlgorithmStatus {
    DISABLE, ENABLE, INITIALIZING;
  }

  private volatile boolean mIsCreatePalmClient;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_example);
    checkPermission();
    initView();
    rgbDisPlay = new GLDisplay();
    irDisPlay = new GLDisplay();
    mainHandler = new Handler();
  }

  /**
   * check permission
   */
  private void checkPermission() {
    if ((ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        || (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        || (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)) {
      ActivityCompat.requestPermissions(this, new String[]{
          Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE,
          Manifest.permission.CAMERA}, 10);
    }
  }

  private void initView() {
    mGLRgbView = findViewById(R.id.gl_rgb);
    mGLIrView = findViewById(R.id.gl_ir);
    mGLIrView.post(() -> {
      mGLRgbView.setDisplay(mGLRgbView.getWidth(), mGLRgbView.getWidth() * 1024 / 720);
      mGLIrView.setDisplay(mGLIrView.getWidth(), mGLIrView.getWidth() * 1024 / 720);
    });

    mAdapterStreamType = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, mListStreamType);

    mBtnOpen = findViewById(R.id.btn_open);
    mBtnEnable = findViewById(R.id.btn_enable);
    mBtnCapture = findViewById(R.id.btn_capture);
    mBtnCaptureOnce = findViewById(R.id.btn_capture_once);
    mBtnStopCapture = findViewById(R.id.btn_stop_capture);

    mBtnCreatePalmClient = findViewById(R.id.btn_create_palm_client);
    mBtnQueryFromServer = findViewById(R.id.btn_query);
    mBtnDeleteId = findViewById(R.id.btn_delete);
    mBtnRegisterToServer = findViewById(R.id.btn_register);

    mTvDeviceInfo = findViewById(R.id.tv_device_info);
    mSwitchStartStream = findViewById(R.id.switch_stream);
    mSpinnerStreamMode = findViewById(R.id.spinner_stream_mode);
    mSpinnerStreamMode.setAdapter(mAdapterStreamType);
    rgbImage = findViewById(R.id.rgb_image);
    irImage = findViewById(R.id.ir_image);
    depthImage = findViewById(R.id.depth_image);
    mRectRoiRgbView = findViewById(R.id.rv_rectRgbPicView);
    mRectRoiRgbView.resizeSource(720, 1024);
    mRectRoiIrView = findViewById(R.id.rv_rectIrPicView);
    mRectRoiIrView.resizeSource(720, 1024);

    initListener();
  }

  private void initListener() {
    mBtnOpen.setOnClickListener(view -> openDevice());
    mSwitchStartStream.setOnCheckedChangeListener((buttonView, isChecked) -> {
      if (!mIsOpenCamera && isChecked) {
        showToast("未打开相机");
        mSwitchStartStream.setChecked(false);
        return;
      }
      if (mSpinnerStreamMode != null) {
        mSpinnerStreamMode.setEnabled(!isChecked);
      }
      if (!isChecked && mIsRunning) {
        mIsRunning = false;
        mainHandler.postDelayed(this::clearFrame, 200);
        return;
      }
      if (mIsOpenCamera && isChecked) {
        startStream();
      }

    });
    mSpinnerStreamMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        currentStreamType = mListStreamType.get(position);
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {

      }
    });
    mBtnEnable.setOnClickListener(view -> enableDimPalm());
    mBtnCaptureOnce.setOnClickListener(view -> captureOnce());
    mBtnCapture.setOnClickListener(view -> capture());
    mBtnStopCapture.setOnClickListener(view -> stopCapture());
    mTvDeviceInfo.setOnLongClickListener(view -> writeLicense());

    mBtnCreatePalmClient.setOnClickListener(view -> showCreatePalmClientDialog());
    mBtnQueryFromServer.setOnClickListener(view -> showQueryDialog());
    mBtnDeleteId.setOnClickListener(view -> showDeleteDialog());
    mBtnRegisterToServer.setOnClickListener(view -> showRegisterDialog());
  }

  private void showCreatePalmClientDialog() {
    if (!mIsOpenCamera) {
      showToast("未打开相机!");
      return;
    }
    if (algoStatus == EnableAlgorithmStatus.DISABLE) {
      showToast("算法未初始化!");
      return;
    }
    // 创建 AlertDialog.Builder
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    View customView = LayoutInflater.from(this).inflate(R.layout.dialog_create_palm_client, null);
    EditText edCompanyId = customView.findViewById(R.id.ed_company_id);
    EditText edSn = customView.findViewById(R.id.ed_sn);
    EditText edIp = customView.findViewById(R.id.ed_ip);
    EditText edPort = customView.findViewById(R.id.ed_port);
    EditText edHostName = customView.findViewById(R.id.ed_host_name);
    if (mDevice != null) {
      edSn.setText(((IVeinshine) mDevice).getDeviceInfo().serial_num);
    }
    builder.setView(customView);
    builder.setPositiveButton("创建", (dialog, which) -> {
    });
    builder.setNegativeButton("取消", (dialog, which) -> {
    });

    // 创建并显示 AlertDialog
    AlertDialog alertDialog = builder.create();
    alertDialog.setOnShowListener(dialog ->
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
          // 处理确定按钮的点击事件
          String companyId = edCompanyId.getText().toString();
          String sn = edSn.getText().toString();
          String ip = edIp.getText().toString();
          String port = edPort.getText().toString();
          String hostname = edHostName.getText().toString();
          if (TextUtils.isEmpty(companyId)
              || TextUtils.isEmpty(ip)
              || TextUtils.isEmpty(port)
              || TextUtils.isEmpty(sn)) {
            Toast.makeText(this, "companyId | sn | ip | port不能为空！", Toast.LENGTH_SHORT).show();
            return;
          }
          if (mDevice != null) {
            mIsCreatePalmClient = ((IVeinshine) mDevice).createPalmClient(companyId, sn, ip, port, hostname);
            showToast("createPalmClient " + (mIsCreatePalmClient ? "success" : "fail"));
          }
          alertDialog.dismiss();
        }));
    alertDialog.setCanceledOnTouchOutside(false);
    alertDialog.show();
  }

  private void showQueryDialog() {
    if (!mIsOpenCamera) {
      showToast("未打开相机!");
      return;
    }
    if (algoStatus == EnableAlgorithmStatus.DISABLE) {
      showToast("算法未初始化!");
      return;
    }
    if (!mIsCreatePalmClient) {
      showToast("PalmClient未创建!");
      return;
    }
    // 创建 AlertDialog.Builder
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    View customView = LayoutInflater.from(this).inflate(R.layout.dialog_regiter_palm_pic_path, null);
    tvResult = customView.findViewById(R.id.tv_result);
    EditText edRgbPath = customView.findViewById(R.id.ed_rgb_path);
    EditText edIrPath = customView.findViewById(R.id.ed_ir_path);
    builder.setView(customView);
    builder.setPositiveButton("查询", (dialog, which) -> {
    });
    builder.setNegativeButton("取消", (dialog, which) -> {
    });

    // 创建并显示 AlertDialog
    AlertDialog alertDialog = builder.create();
    alertDialog.setOnShowListener(dialog ->
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
          // 处理确定按钮的点击事件
          tvResult.setText("");
          String rgbPath = edRgbPath.getText().toString();
          String irPath = edIrPath.getText().toString();
          if (mDevice != null) {
            // 不是单IR模组需要检查RGB图片路径是否正常
            if (((IVeinshine) mDevice).getDeviceInfo().pid != 0x2009) {
              if (TextUtils.isEmpty(rgbPath)) {
                Toast.makeText(this, "请输入rgbPath的正确路径！", Toast.LENGTH_SHORT).show();
                return;
              }
              File rgbFile = new File(rgbPath);
              if (!rgbFile.exists()) {
                Toast.makeText(ExampleActivity.this, "该rgbPath路径下未发现图片", Toast.LENGTH_SHORT).show();
                Log.i("LBC", "该rgbPath路径下未发现图片");
                return;
              }
            }
          }
          if (TextUtils.isEmpty(irPath)) {
            Toast.makeText(this, "请输入irPath的正确路径！", Toast.LENGTH_SHORT).show();
            return;
          }
          File irFile = new File(irPath);
          if (!irFile.exists()) {
            Toast.makeText(ExampleActivity.this, "该irPath路径下未发现图片", Toast.LENGTH_SHORT).show();
            Log.i("LBC", "该rgbPath路径下未发现图片");
            return;
          }

          doQueryFromServer(rgbPath, irPath);
        }));
    alertDialog.setCanceledOnTouchOutside(false);
    alertDialog.show();
  }

  private void doQueryFromServer(String rgbPath, @NonNull String irPath) {
    // 子线程处理,这里演示用图片提取的特征值再去云服务查询
    new Thread(() -> {
      BitmapFactory.Options options = new BitmapFactory.Options();
      options.inJustDecodeBounds = false;

      Bitmap bitmap;
      ImageInstance rgbImageInstance = null;
      if (!TextUtils.isEmpty(rgbPath)) {
        bitmap = BitmapFactory.decodeFile(rgbPath, options);
        int rgbImageWidth = options.outWidth;
        int rgbImageHeight = options.outHeight;
        //    saveBitmap(bitmap, "/sdcard/palm/java_rgbSrc_bitmap.jpg")
        byte[] rgbData = convertJpegDataToRgb888(bitmap, rgbImageWidth, rgbImageHeight);
        rgbImageInstance =
            new ImageInstance(rgbImageWidth, rgbImageHeight, rgbData, ImageInstance.ImageFormat.IMG_3C8BIT);
      }
      bitmap = BitmapFactory.decodeFile(irPath, options);
      int irImageWidth = options.outWidth;
      int irImageHeight = options.outHeight;
      //    saveBitmap(bitmap, "/sdcard/palm/java_irSrc_bitmap.jpg")
      byte[] irData = convertJpegDataToGray(bitmap, irImageWidth, irImageHeight);

      ImageInstance irImageInstance =
          new ImageInstance(irImageWidth, irImageHeight, irData, ImageInstance.ImageFormat.IMG_1C8BIT);

      if (mDevice != null) {
        // 从图像提取特征值
        ExtractOutput extractOutput =
            ((IVeinshine) mDevice).extractPalmFeaturesFromImg(rgbImageInstance, irImageInstance);
        if (extractOutput == null || extractOutput.result != 0) {
          runOnUiThread(() -> {
            if (tvResult != null) {
              tvResult.setText("从图片中提取特征值失败,无法云服务查询");
            }
          });
          return;
        }

        ClientPalmOutput clientPalmOutput =
            ((IVeinshine) mDevice).queryFeatureIdFromServer(
                rgbImageInstance, extractOutput.rgbFeature,
                irImageInstance, extractOutput.irFeature);
        runOnUiThread(() -> {
          if (tvResult != null) {
            tvResult.setText(clientPalmOutput.toString());
          }
        });

      }

    }).start();
  }

  private void showDeleteDialog() {
    if (!mIsOpenCamera) {
      showToast("未打开相机!");
      return;
    }
    if (algoStatus == EnableAlgorithmStatus.DISABLE) {
      showToast("算法未初始化!");
      return;
    }
    if (!mIsCreatePalmClient) {
      showToast("PalmClient未创建!");
      return;
    }
    // 创建 AlertDialog.Builder
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    View customView = LayoutInflater.from(this).inflate(R.layout.dialog_delete_id, null);
    tvResult = customView.findViewById(R.id.tv_result);
    EditText edFeatureId = customView.findViewById(R.id.ed_feature_id);
    builder.setView(customView);
    builder.setPositiveButton("删除", (dialog, which) -> {
    });
    builder.setNegativeButton("取消", (dialog, which) -> {
    });

    // 创建并显示 AlertDialog
    AlertDialog alertDialog = builder.create();
    alertDialog.setOnShowListener(dialog ->
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
          // 处理确定按钮的点击事件
          tvResult.setText("");
          int featureId = Integer.parseInt(edFeatureId.getText().toString());
          if (featureId <= 0) {
            Toast.makeText(this, "请输入正确的FeatureId！", Toast.LENGTH_SHORT).show();
            Log.i("LBC", "请输入正确的FeatureId");
            return;
          }
          if (mDevice != null) {
            int ret = ((IVeinshine) mDevice).deleteId(featureId);
            runOnUiThread(() -> {
              if (tvResult != null) {
                tvResult.setText("delete [" + featureId + "] " + (ret == 0 ? "success" : "fail"));
              }
            });
          }

        }));
    alertDialog.setCanceledOnTouchOutside(false);
    alertDialog.show();
  }

  private void showRegisterDialog() {
    if (!mIsOpenCamera) {
      showToast("未打开相机!");
      return;
    }
    if (algoStatus == EnableAlgorithmStatus.DISABLE) {
      showToast("算法未初始化!");
      return;
    }
    if (!mIsCreatePalmClient) {
      showToast("PalmClient未创建!");
      return;
    }
    // 创建 AlertDialog.Builder
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    View customView = LayoutInflater.from(this).inflate(R.layout.dialog_regiter_palm_pic_path, null);
    tvResult = customView.findViewById(R.id.tv_result);
    EditText edRgbPath = customView.findViewById(R.id.ed_rgb_path);
    EditText edIrPath = customView.findViewById(R.id.ed_ir_path);
    builder.setView(customView);
    builder.setPositiveButton("注册", (dialog, which) -> {
    });
    builder.setNegativeButton("取消", (dialog, which) -> {
    });

    // 创建并显示 AlertDialog
    AlertDialog alertDialog = builder.create();
    alertDialog.setOnShowListener(dialog ->
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
          // 处理确定按钮的点击事件
          tvResult.setText("");
          String rgbPath = edRgbPath.getText().toString();
          String irPath = edIrPath.getText().toString();
          if (mDevice != null) {
            // 不是单IR模组需要检查RGB图片路径是否正常
            if (((IVeinshine) mDevice).getDeviceInfo().pid != 0x2009) {
              if (TextUtils.isEmpty(rgbPath)) {
                Toast.makeText(this, "请输入rgbPath的正确路径！", Toast.LENGTH_SHORT).show();
                return;
              }
              File rgbFile = new File(rgbPath);
              if (!rgbFile.exists()) {
                Toast.makeText(ExampleActivity.this, "该rgbPath路径下未发现图片", Toast.LENGTH_SHORT).show();
                Log.i("LBC", "该rgbPath路径下未发现图片");
                return;
              }
            }
          }
          if (TextUtils.isEmpty(irPath)) {
            Toast.makeText(this, "请输入irPath的正确路径！", Toast.LENGTH_SHORT).show();
            return;
          }
          File irFile = new File(irPath);
          if (!irFile.exists()) {
            Toast.makeText(ExampleActivity.this, "该irPath路径下未发现图片", Toast.LENGTH_SHORT).show();
            Log.i("LBC", "该rgbPath路径下未发现图片");
            return;
          }
          doRegisterToServer(rgbPath, irPath);

        }));
    alertDialog.setCanceledOnTouchOutside(false);
    alertDialog.show();
  }

  private void doRegisterToServer(String rgbPath, @NonNull String irPath) {
    // 子线程处理,这里演示用图片提取的特征值再去注册到云服务
    new Thread(() -> {
      BitmapFactory.Options options = new BitmapFactory.Options();
      options.inJustDecodeBounds = false;

      Bitmap bitmap;
      ImageInstance rgbImageInstance = null;
      if (!TextUtils.isEmpty(rgbPath)) {
        bitmap = BitmapFactory.decodeFile(rgbPath, options);
        int rgbImageWidth = options.outWidth;
        int rgbImageHeight = options.outHeight;
        //    saveBitmap(bitmap, "/sdcard/palm/java_rgbSrc_bitmap.jpg")
        byte[] rgbData = convertJpegDataToRgb888(bitmap, rgbImageWidth, rgbImageHeight);
        rgbImageInstance =
            new ImageInstance(rgbImageWidth, rgbImageHeight, rgbData, ImageInstance.ImageFormat.IMG_3C8BIT);
      }
      bitmap = BitmapFactory.decodeFile(irPath, options);
      int irImageWidth = options.outWidth;
      int irImageHeight = options.outHeight;
      //    saveBitmap(bitmap, "/sdcard/palm/java_irSrc_bitmap.jpg")
      byte[] irData = convertJpegDataToGray(bitmap, irImageWidth, irImageHeight);

      ImageInstance irImageInstance =
          new ImageInstance(irImageWidth, irImageHeight, irData, ImageInstance.ImageFormat.IMG_1C8BIT);

      if (mDevice != null) {
        // 从图像提取特征值
        ExtractOutput extractOutput =
            ((IVeinshine) mDevice).extractPalmFeaturesFromImg(rgbImageInstance, irImageInstance);
        if (extractOutput == null || extractOutput.result != 0) {
          runOnUiThread(() -> {
            if (tvResult != null) {
              tvResult.setText("从图片中提取特征值失败,无法注册到云服务");
            }
          });
          return;
        }
        Log.d("LBC", extractOutput.toString());

        ClientPalmOutput clientPalmOutput =
            ((IVeinshine) mDevice).registerToServer(
                rgbImageInstance, extractOutput.rgbFeature,
                irImageInstance, extractOutput.irFeature);
        runOnUiThread(() -> {
          if (tvResult != null) {
            tvResult.setText(clientPalmOutput.toString());
          }
        });

      }

    }).start();
  }

  private byte[] convertJpegDataToRgb888(Bitmap bitmap, int width, int height) {
    int[] pixelsData = new int[width * height];
    bitmap.getPixels(pixelsData, 0, width, 0, 0, width, height);

    // 创建一个字节数组来存储RGB888格式的数据
    byte[] imageData = new byte[pixelsData.length * 3]; // 一个像素占3个字节，分别代表红、绿、蓝通道

    // 将RGB888格式的数据转换为字节数组
    for (int i = 0; i < pixelsData.length; i++) {
      int pixelValue = pixelsData[i];
      imageData[i * 3 + 2] = (byte) ((pixelValue >> 16) & 0xFF); // 红色通道
      imageData[i * 3 + 1] = (byte) ((pixelValue >> 8) & 0xFF); // 绿色通道
      imageData[i * 3] = (byte) (pixelValue & 0xFF); // 蓝色通道
    }
    return imageData;
  }

  private byte[] convertJpegDataToGray(Bitmap bitmap, int width, int height) {
    int[] grayData = new int[width * height];
    bitmap.getPixels(grayData, 0, width, 0, 0, width, height);

    byte[] grayByteArray = new byte[width * height];
    for (int i = 0; i < grayData.length; i++) {
      int grayValue = (int) (0.299 * ((grayData[i] >> 16) & 0xFF)
          + 0.587 * ((grayData[i] >> 8) & 0xFF)
          + 0.114 * (grayData[i] & 0xFF));
      grayByteArray[i] = (byte) grayValue;
    }

    return grayByteArray;
  }


  private boolean writeLicense() {
    File file = new File(getExternalCacheDir() + File.separator + "license.bin");
    if (!file.exists()) {
      Toast.makeText(ExampleActivity.this, "license文件不存在", Toast.LENGTH_SHORT).show();
      return true;
    }
    try {
      FileInputStream fis = new FileInputStream(file);
      int available = fis.available();
      byte[] buffer = new byte[available];
      fis.read(buffer);
      fis.close();

      // 处理二进制数据
      String content = new String(buffer, StandardCharsets.ISO_8859_1);
      File file1 = new File(getExternalCacheDir() + File.separator + "out1.bin");
      FileOutputStream outputStream = new FileOutputStream(file1);
      outputStream.write(content.getBytes(StandardCharsets.ISO_8859_1));
      outputStream.flush();
      outputStream.close();
      Log.e("LBC", "license:" + content);
      if (mDevice != null && mIsOpenCamera) {
        int ret = ((IVeinshine) mDevice).writeLicense(content);
        Toast.makeText(ExampleActivity.this,
            "license烧录" + (ret == 0 ? "成功" : "失败"), Toast.LENGTH_SHORT).show();
        Log.e("LBC", "readLicense:" + ((IVeinshine) mDevice).readLicense());
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return true;
  }

  private void showToast(String string) {
    if (Looper.myLooper() == Looper.getMainLooper()) {
      Toast.makeText(ExampleActivity.this, string, Toast.LENGTH_SHORT).show();
    } else {
      runOnUiThread(() -> Toast.makeText(ExampleActivity.this, string, Toast.LENGTH_SHORT).show());
    }
  }

  private void stopCapture() {
    if (algoStatus != EnableAlgorithmStatus.ENABLE) {
      showToast("请先使能算法！");
      return;
    }
    if (mDevice != null) {
      IVeinshine veinshine = (IVeinshine) mDevice;
      int ret = veinshine.stopPalmCapture();
      Log.e("LBC", "stopPalmCapture ret:" + ret);
      if (ret != 0) {
        showToast("停止抓拍指令失败, ret:" + ret);
      } else {
        showToast("停止抓拍指令成功");
      }
      mainHandler.post(() -> {
        rgbImage.setWillNotDraw(true);
        hideRect(mRectRoiRgbView);
        irImage.setWillNotDraw(true);
        hideRect(mRectRoiIrView);
      });
    }
  }

  private void capture() {
    if (algoStatus != EnableAlgorithmStatus.ENABLE) {
      showToast("请先使能算法！");
      return;
    }
    if (mDevice != null) {
      IVeinshine veinshine = (IVeinshine) mDevice;
      int ret = veinshine.capturePalm(mCapturePalmCallback, 15000, false);
      Log.e("LBC", "capturePalm ret:" + ret);
      if (ret != 0) {
        showToast("持续抓拍指令失败, ret:" + ret);
      }
    }
  }

  private void captureOnce() {
    if (algoStatus != EnableAlgorithmStatus.ENABLE) {
      showToast("请先使能算法！");
      return;
    }
    if (mDevice != null) {
      IVeinshine veinshine = (IVeinshine) mDevice;
      int ret = veinshine.capturePalmOnce(mCapturePalmCallback, 15000, false);
      Log.e("LBC", "capturePalmOnce ret:" + ret);
      if (ret != 0) {
        showToast("抓拍一次指令失败, ret:" + ret);
      }
    }
  }

  private String path = "";

  private void enableDimPalm() {
    if (algoStatus == EnableAlgorithmStatus.ENABLE) {
      showToast("算法已使能!");
      return;
    }
    if (algoStatus == EnableAlgorithmStatus.INITIALIZING) {
      showToast("算法正在初始化!");
      return;
    }
    showPathDialog();
  }

  private void showPathDialog() {
    final EditText inputEdit = new EditText(ExampleActivity.this);
    // 可以放在sdcard的私有目录下或者其他有权限的目录下,这里演示放在sdcard自定义目录下
    inputEdit.setText("/sdcard/models_palm/models/");
    AlertDialog.Builder builder = new AlertDialog.Builder(ExampleActivity.this);
    builder.setTitle("Model Path:")
//        .setIcon(android.R.drawable.ic_dialog_info)
        .setView(inputEdit)
        .setNegativeButton("取消", null);
    builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        path = inputEdit.getText().toString();
        if (TextUtils.isEmpty(path)) {
          showToast("请输入确路径!");
          return;
        }
        workServices.execute(() -> {
          if (algoStatus != EnableAlgorithmStatus.DISABLE) {
            return;
          }
          if (mDevice != null) {
            IVeinshine veinshine = (IVeinshine) mDevice;
            algoStatus = EnableAlgorithmStatus.INITIALIZING;
            int ret = veinshine.enableDimPalm(path);
            if (ret == 0) {
              algoStatus = EnableAlgorithmStatus.ENABLE;
              showToast("算法使能成功");
              Log.e(TAG, "算法版本:" + veinshine.getAlgorithmVersion());
            } else {
              showToast("算法使能失败,ret:" + ret);
              algoStatus = EnableAlgorithmStatus.DISABLE;
            }
          }
        });
      }
    });
    builder.show();
  }


  protected void showRgbImage(ImageView image, byte[] data, int cols, int rows) {
    buildRgbBitmap(data, cols, rows);
    if (mRgbBitmap != null) {
      image.setWillNotDraw(false);
      image.setImageBitmap(mRgbBitmap);
    }
  }

  protected void showIrImage(ImageView image, byte[] data, int cols, int rows) {
    buildIrBitmap(data, cols, rows);
    if (mIrBitmap != null) {
      image.setWillNotDraw(false);
      image.setImageBitmap(mIrBitmap);
    }
  }

  protected void showRect(DtRectRoiView picView, CaptureFrame frame) {
    BBox box = new BBox(0, 0, 0, 0);
//    Log.i(TAG, "frame.palmRectX = " + frame.palmRectX);
//    Log.i(TAG, "frame.palmRectY = " + frame.palmRectY);
//    Log.i(TAG, "frame.palmRectW = " + frame.palmRectW);
//    Log.i(TAG, "frame.palmRectH = " + frame.palmRectH);
    box.x = frame.palmRectX;
    box.y = frame.palmRectY;
    box.w = frame.palmRectW;
    box.h = frame.palmRectH;
    picView.setRect(box, 0);
  }

  protected void hideRect(DtRectRoiView picView) {
    BBox box = new BBox(0, 0, 0, 0);
    picView.setRect(box, 0);
  }

  protected void buildRgbBitmap(byte[] data, int width, int height) {
    try {
      // RGBA 数组
      byte[] Bits = new byte[data.length / 3 * 4];
      int i;
      for (i = 0; i < data.length / 3; i++) {
        // 原理：4个字节表示一个灰度，则BGR  = 灰度值，最后一个Alpha = 0xff;
        Bits[i * 4] = data[i * 3 + 2];
        Bits[i * 4 + 1] = data[i * 3 + 1];
        Bits[i * 4 + 2] = data[i * 3];
        Bits[i * 4 + 3] = -1; // 0xff
      }
      // Bitmap.Config.ARGB_8888 表示：图像模式为8位
      if (mRgbBitmap == null) {
        Log.i(TAG, "buildRgbBitmap createBitmap");
        mRgbBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        mRectRoiRgbView.resizeSource(width, height);
      } else {
        if (mRgbBitmap.getHeight() != height || mRgbBitmap.getWidth() != width) {
          mRgbBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
          mRectRoiRgbView.resizeSource(width, height);
        }
      }
      Buffer buffer = ByteBuffer.wrap(Bits);
      buffer.rewind();
      mRgbBitmap.copyPixelsFromBuffer(buffer);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  protected void buildIrBitmap(byte[] data, int width, int height) {
    try {
      byte[] Bits = new byte[data.length * 4]; // RGBA 数组
      int i;
      for (i = 0; i < data.length; i++) {
        // 原理：4个字节表示一个灰度，则RGB  = 灰度值，最后一个Alpha = 0xff;
        Bits[i * 4] = Bits[i * 4 + 1] = Bits[i * 4 + 2] = data[i];
        Bits[i * 4 + 3] = -1; // 0xff
      }
      // Bitmap.Config.ARGB_8888 表示：图像模式为8位
      if (mIrBitmap == null) {
        mIrBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        mRectRoiIrView.resizeSource(width, height);
      } else {
        if (mIrBitmap.getHeight() != height || mIrBitmap.getWidth() != width) {
          mIrBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
          mRectRoiIrView.resizeSource(width, height);
        }
      }
      mIrBitmap.copyPixelsFromBuffer(ByteBuffer.wrap(Bits));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  protected byte[] raw16ToRaw8(byte[] inputData) {
    byte[] outputData = new byte[inputData.length / 2];
    for (int i = 0; i < outputData.length; i++) {
      if (inputData[2 * i + 1] > 0) {
        outputData[i] = (byte) (255);
      } else {
        outputData[i] = inputData[2 * i];
      }
    }
    return outputData;
  }


  protected void convert8bit(byte[] data, int width, int height) {
    try {
      // RGBA 数组
      byte[] Bits = new byte[data.length * 4];
      int i;
      for (i = 0; i < data.length; i++) {
        // 原理：4个字节表示一个灰度，则RGB  = 灰度值，最后一个Alpha = 0xff;
        Bits[i * 4] = Bits[i * 4 + 1] = Bits[i * 4 + 2] = data[i];
        Bits[i * 4 + 3] = -1; // 0xff
      }
      // Bitmap.Config.ARGB_8888 表示：图像模式为8位
      if (mDepthBitmap == null) {
        mDepthBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
      }
      mDepthBitmap.copyPixelsFromBuffer(ByteBuffer.wrap(Bits));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  private ICapturePalmCallback mCapturePalmCallback = new ICapturePalmCallback() {
    @Override
    public void onCaptureFrame(CaptureFrame frame) {
      Log.e(TAG, "onCaptureFrame()");
      if (frame.stageScores != null) {
        if (frame.stageScores.isEmpty()) {
          Log.e(TAG, "test---- mCapturePalmCallback hashMap is empty ");
        } else {
          for (Map.Entry<Integer, Float> entry : frame.stageScores.entrySet()) {
            Log.i(TAG, "test mCapturePalmCallback score ---- " + entry.getKey() + ",  " + entry.getValue());
          }
        }
      } else {
        Log.e(TAG, "test---- mCapturePalmCallback hashMap is null ");
      }
      if (frame != null) {
        mainHandler.post(new Runnable() {
          @Override
          public void run() {
            if (frame.rgbData != null) {
              showRgbImage(rgbImage, frame.rgbData, frame.rgbCols, frame.rgbRows);
              showRect(mRectRoiRgbView, frame);
            } else {
              rgbImage.setWillNotDraw(true);
              hideRect(mRectRoiRgbView);
            }
            if (frame.irData != null) {
              showIrImage(irImage, frame.irData, frame.irCols, frame.irRows);
              showRect(mRectRoiIrView, frame);
            } else {
              irImage.setWillNotDraw(true);
              hideRect(mRectRoiIrView);
            }
          }
        });
      }

    }

    @Override
    public void onCapturePalmHint(Hint hint, HashMap<Integer, Float> hashMap) {

      if (hashMap != null) {
        if (hashMap.isEmpty()) {
          Log.e(TAG, "test---- onCapturePalmHint hashMap is empty ");
        } else {
          for (Map.Entry<Integer, Float> entry : hashMap.entrySet()) {
            Log.i(TAG, "test onCapturePalmHint score ---- " + entry.getKey() + ",  " + entry.getValue());
          }
        }
      } else {
        Log.e(TAG, "test---- onCapturePalmHint hashMap is null ");
      }


//      Log.e(TAG, "onCapturePalmHint()");
      if (hint == Hint.NO_PALM_DETECTED) {
        mainHandler.post(() -> {
          rgbImage.setWillNotDraw(true);
          hideRect(mRectRoiRgbView);
//          faceHintTextView.setText(hint.value);
          irImage.setWillNotDraw(true);
          hideRect(mRectRoiIrView);
        });
      }
      if (hint == Hint.TIMEOUT) {
        runOnUiThread(() -> Toast.makeText(ExampleActivity.this, "抓拍超时", Toast.LENGTH_SHORT).show());
      }
    }
  };

  private void openDevice() {
    if (mIsOpenCamera) {
      Toast.makeText(ExampleActivity.this, "相机已打开", Toast.LENGTH_SHORT).show();
      return;
    }
    Device.create(ExampleActivity.this, new Device.DeviceListener() {
      @Override
      public void onDeviceCreatedSuccess(IDevice device, int deviceIndex, Map<Long, IDevice> runningDevice, UsbMapTable.DeviceType deviceType) {
        mDevice = device;
        Log.i(TAG, "onDeviceCreate, deviceType:" + deviceType + " deviceIndex:" + deviceIndex);
        deviceThread1.execute(() -> open(device, deviceIndex, runningDevice, deviceType));
      }

      @Override
      public void onDeviceCreateFailed(IDevice device) {
        Log.e(TAG, "test---- onDeviceCreateFailed:");
      }

      @Override
      public void onDeviceDestroy(IDevice device) {
        Log.i(TAG, "onDeviceDestroy()");
        if (mDevice != null) {
          mDevice = null;
        }
        mIsOpenCamera = false;
        algoStatus = EnableAlgorithmStatus.DISABLE;
        mIsCreatePalmClient = false;
        mIsRunning = false;
        rgbFrameData1 = null;
        irFrameData1 = null;
        if (mListStreamType.size() > 0) {
          mListStreamType.clear();
          mainHandler.post(() -> {
            mAdapterStreamType.notifyDataSetChanged();
          });
          mainHandler.postDelayed(() -> clearFrame(), 200);
        }

      }
    }, new DtUsbManager.DeviceStateListener() {
      @Override
      public void onDevicePermissionGranted(DtUsbDevice dtUsbDevice) {

      }

      @Override
      public void onDevicePermissionDenied(DtUsbDevice dtUsbDevice) {

      }

      @Override
      public void onAttached(DtUsbDevice dtUsbDevice) {
        Log.e(TAG, "onAttached()");

      }

      @Override
      public void onDetached(DtUsbDevice dtUsbDevice) {
        Log.e(TAG, "onDetached()");

        mIsOpenCamera = false;
        algoStatus = EnableAlgorithmStatus.DISABLE;
        mIsCreatePalmClient = false;
        mainHandler.post(() -> {
          if (mTvDeviceInfo != null) {
            mTvDeviceInfo.setText("Device Detached!");
          }
          if (mSwitchStartStream != null) {
            mSwitchStartStream.setChecked(false);
          }
        });

      }
    });
  }

  private void startStream() {
    if (mDevice != null) {
      Thread thread = generateStreamThread(mDevice, 1);
      thread.start();
    }
  }

  private Thread generateStreamThread(final IDevice device, int deviceIndex) {
    Thread streamThread = new Thread(() -> {
      if (currentStreamType != StreamType.INVALID_STREAM_TYPE) {
        IStream stream = device.createStream(currentStreamType);
        if (stream == null) {
          mainHandler.post(() -> Toast.makeText(ExampleActivity.this, "创建流失败", Toast.LENGTH_SHORT).show());
          return;
        }
        Frames frames = stream.allocateFrames();
        int ret = stream.start();
        if (ret == 0) {
          mIsRunning = true;
        } else {
          mIsRunning = false;
          return;
        }
        while (mIsRunning) {
          int res = stream.getFrames(frames, 2000);
          if (res != 0) {
            Log.e(TAG, "get getFrame code: " + Integer.toHexString(res) + "deviceIndex:" + deviceIndex);
            continue;
          }

          Frame frame1 = frames.getFrame(0);
          Frame frame2 = frames.getFrame(1);

          //渲染图像帧
          onDrawFrame(frame1, frame2, deviceIndex);

        }
        if (mDevice != null) {
          stream.stop();
          device.destroyStream(stream);
        }
        Log.e("LBC", "getFrame thread exit");
      }
    });
    return streamThread;
  }

  private void onDrawFrame(Frame frame1, Frame frame2, int deviceIndex) {
    if (frame1 != null) {
      switch (frame1.getFrameType()) {
        case RGB_FRAME:
          if (deviceIndex == 1) {
            rgbFrameW1 = frame1.getWidth();
            rgbFrameH1 = frame1.getHeight();
            rgbFrameData1 = frame1.getRawData();
            rgbFrameExtraInfo = frame1.getExtraInfo();
          }
          break;
        case IR_FRAME:
          if (deviceIndex == 1) {
            irFrameW1 = frame1.getWidth();
            irFrameH1 = frame1.getHeight();
            irFrameData1 = frame1.getRawData();
            irFrameExtraInfo = frame1.getExtraInfo();
          }
          break;
        default:
      }
    }
    if (frame2 != null) {
      switch (frame2.getFrameType()) {
        case RGB_FRAME:
          if (deviceIndex == 1) {
            rgbFrameW1 = frame2.getWidth();
            rgbFrameH1 = frame2.getHeight();
            rgbFrameData1 = frame2.getRawData();
            rgbFrameExtraInfo = frame2.getExtraInfo();
          }
          break;
        case IR_FRAME:
          if (deviceIndex == 1) {
            irFrameW1 = frame2.getWidth();
            irFrameH1 = frame2.getHeight();
            irFrameData1 = frame2.getRawData();
            irFrameExtraInfo = frame2.getExtraInfo();
          }
          break;
        default:
      }
    }
    if (deviceIndex == 1) {
      if (null != irFrameData1 && null != irDisPlay && null != mGLIrView) {
        irDisPlay.render(mGLIrView,
            0,
            false,
            irFrameData1,
            irFrameW1,
            irFrameH1,
            2,
            new int[]{irFrameExtraInfo.palmRoi[0],
                irFrameExtraInfo.palmRoi[1],
                irFrameExtraInfo.palmRoi[2],
                irFrameExtraInfo.palmRoi[3]});
      }
      if (null != rgbFrameData1 && null != rgbDisPlay && null != mGLRgbView) {
        rgbDisPlay.render(mGLRgbView,
            0,
            false,
            rgbFrameData1,
            rgbFrameW1,
            rgbFrameH1,
            1,
            new int[]{rgbFrameExtraInfo.palmRoi[0],
                rgbFrameExtraInfo.palmRoi[1],
                rgbFrameExtraInfo.palmRoi[2],
                rgbFrameExtraInfo.palmRoi[3]});
      }
    }

  }

  private void open(IDevice device, int deviceIndex, Map<Long, IDevice> mRunningDevice, UsbMapTable.DeviceType deviceType) {
    device.open(new IOpenCallback() {
      @Override
      public void onDownloadPrepare() {
      }

      @Override
      public void onDownloadProgress(int progress) {

      }

      @Override
      public void onDownloadSuccess() {
      }

      @Override
      public void onOpenSuccess() {
        mIsOpenCamera = true;
        DeviceInfo deviceInfo = ((IVeinshine) device).getDeviceInfo();
        Log.d("LBC", "固件版本号:" + deviceInfo.firmware_version);
        mainHandler.post(() -> {
          if (mTvDeviceInfo != null) {
            mTvDeviceInfo.setText("DeviceName:" + deviceInfo.device_name);
          }
        });

        if (deviceIndex == 1) {
          Log.e("LBC", "mDevice1");
//          mDevice = device;

          List<StreamType> deviceSupportStreamTypeList = device.getDeviceSupportStreamType();
          if (deviceSupportStreamTypeList.size() > 0) {
            mListStreamType.clear();
            mListStreamType.addAll(deviceSupportStreamTypeList);
            mainHandler.post(() -> mAdapterStreamType.notifyDataSetChanged());
          }
        }
      }

      @Override
      public void onOpenFail(int errorCode) {
        if (mDevice != null) {
          mDevice.close();
        }
        mainHandler.post(() -> Toast.makeText(ExampleActivity.this, "open device error:" + errorCode, Toast.LENGTH_SHORT).show());
      }
    });
  }


  private void clearFrame() {
    Log.e(TAG, "-------cleanFrame()------");
    if (null != rgbDisPlay && null != mGLRgbView) {
      rgbDisPlay.render(mGLRgbView, 0, false, new byte[rgbFrameW1 * rgbFrameH1 * 3],
          rgbFrameW1, rgbFrameH1, 1);
    }
    if (null != irDisPlay && null != mGLIrView) {
      irDisPlay.render(mGLIrView, 0, false, new byte[irFrameW1 * irFrameH1],
          irFrameW1, irFrameH1, 2);
    }
    rgbFrameData1 = null;
    irFrameData1 = null;
  }

  @Override
  public void onBackPressed() {

  }


  @Override
  protected void onDestroy() {
    super.onDestroy();
    mGLRgbView.onPause();
    mGLIrView.onPause();
    rgbDisPlay.release();
    irDisPlay.release();
    rgbDisPlay = null;
    irDisPlay = null;

  }

}
