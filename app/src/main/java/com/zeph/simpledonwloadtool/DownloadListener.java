package com.zeph.simpledonwloadtool;


public interface DownloadListener {

  void onProgress(int progress); // 进度
  void onSuccess(); // 成功
  void onFailed(); // 失败
  void onPaused(); // 暂停
  void onCanceled(); // 取消



}
