package com.palm.common.utils;

import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import java.lang.reflect.Method;

public class ScreenUtils {
  /**
   * 获取状态栏高度
   *
   * @return
   */
  public static int getStatusBarHeight(Context context) {
    Resources resources = context.getResources();
    int resourceId = resources.getIdentifier("status_bar_height", "dimen", "android");
    int height = resources.getDimensionPixelSize(resourceId);
    return height;
  }

  // 获取底部导航的高度
  public static int getBottomStatusHeight(Context context) {
    int totalHeight = getDpi(context);
    int contentHeight = getScreenHeight(context);
    return totalHeight - contentHeight;
  }

  // 获取屏幕原始尺寸高度，包括虚拟功能键高度
  public static int getDpi(Context context) {
    int dpi = 0;
    WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    Display display = windowManager.getDefaultDisplay();
    DisplayMetrics displayMetrics = new DisplayMetrics();
    @SuppressWarnings("rawtypes")
    Class c;
    try {
      c = Class.forName("android.view.Display");
      @SuppressWarnings("unchecked")
      Method method = c.getMethod("getRealMetrics", DisplayMetrics.class);
      method.invoke(display, displayMetrics);
      dpi = displayMetrics.heightPixels;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return dpi;
  }

  // 获取屏幕高度 不包含虚拟按键=
  public static int getScreenHeight(Context context) {
    DisplayMetrics dm = context.getResources().getDisplayMetrics();
    return dm.heightPixels;
  }

  public static boolean isLand(Context context) {
    DisplayMetrics dm = context.getResources().getDisplayMetrics();
    if (dm.widthPixels > dm.heightPixels) {
      return true;
    }
    return false;
  }

  // 获取屏幕宽度 不包含虚拟按键=
  public static int getScreenWidth(Context context) {
    DisplayMetrics dm = context.getResources().getDisplayMetrics();
    return dm.widthPixels;
  }
}
