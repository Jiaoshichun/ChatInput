package com.heng.chatinput.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import java.io.File;
import java.io.IOException;

public final class StorageUtils {
  private static final String TAG = "StorageUtils";

  private StorageUtils() {
  }

  /**
   * 当且仅当有扩展卡才返回true，反之返回false。
   */
  public static boolean hasExternalStorage() {
    try {
      return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
          || !isExternalStorageRemovable();
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * 当且仅当扩展卡没有被移除才返回true，反之返回false。
   */
  @TargetApi(Build.VERSION_CODES.GINGERBREAD) public static boolean isExternalStorageRemovable() {
    return !(Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD)
        || Environment.isExternalStorageRemovable();
  }

  /**
   * 根据{@link Context}返回缓存目录
   *
   * @param context 上下文引用
   */
  public static File getCacheDir(Context context) {
    File cacheDir = null;
    if (hasExternalStorage()) {
      cacheDir = getExternalCacheDir(context);
    }
    if (cacheDir == null) {
      cacheDir = context.getCacheDir();
    }
    if (cacheDir == null) {
      String cacheDirPath = "/data/data/" + context.getPackageName() + "/cache/";
      Log.w(TAG, "Can't define system cache directory! '%s' will be used." + cacheDirPath);
      cacheDir = new File(cacheDirPath);
    }
    return cacheDir;
  }

  /**
   * 根据{@link Context}与备用目录返回扩展缓存目录，防止小米等手机锁定目录
   *
   * @param context 上下文引用
   * @param alternateDir 备用目录
   */
  public static File getExternalCacheDir(Context context, String alternateDir) {
    File cacheDir = getExternalCacheDir(context);
    if (cacheDir == null) {
      if (!TextUtils.isEmpty(alternateDir)) {
        cacheDir = new File(Environment.getExternalStorageDirectory(), alternateDir);
        if (!cacheDir.exists()) {
          if (!cacheDir.mkdirs()) {
            Log.w(TAG, "Unable to create custom directory");
            return null;
          }
        }
        try {
          new File(cacheDir, ".nomedia").createNewFile();
        } catch (IOException e) {
          Log.w(TAG, "Can't create \".nomedia\" file in application external cache directory");
        }
      }
    }
    return cacheDir;
  }

  public static File getExternalCacheDir(Context context) {
    File dataDir = new File(new File(Environment.getExternalStorageDirectory(), "Android"), "data");
    File cacheDir = new File(new File(dataDir, context.getPackageName()), "cache");
    if (!cacheDir.exists()) {
      if (!cacheDir.mkdirs()) {
        Log.w(TAG, "Unable to create external cache directory");
        return null;
      }
      try {
        new File(cacheDir, ".nomedia").createNewFile();
      } catch (IOException e) {
        Log.w(TAG, "Can't create \".nomedia\" file in application external cache directory");
      }
    }
    return cacheDir;
  }
}

