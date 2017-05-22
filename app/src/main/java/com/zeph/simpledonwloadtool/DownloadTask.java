package com.zeph.simpledonwloadtool;


import android.os.AsyncTask;
import android.os.Build.VERSION_CODES;
import android.os.Environment;
import android.support.annotation.RequiresApi;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@RequiresApi(api = VERSION_CODES.CUPCAKE)
public class DownloadTask extends AsyncTask<String, Integer, Integer> {

  public static final int TYPE_SUCCESS = 0;
  public static final int TYPE_FAILED = 1;
  public static final int TYPE_PAUSE = 2;
  public static final int TYPE_CANCELED = 3;

  private DownloadListener listener;
  private boolean isCanceled = false;
  private boolean isPause = false;
  private int lastProgress;

  public DownloadTask(DownloadListener listener) {
    this.listener = listener;
  }

  @Override
  protected Integer doInBackground(String... params) {
    InputStream is = null; // 实例并初始化 输入流is
    RandomAccessFile saveFile = null;
    File file = null;
    try {
      long downloadedLength = 0; // 初始化变量 下载长度
      String downloadUrl = params[0];
      String fileName = downloadUrl.substring(downloadUrl.lastIndexOf("/"));
      String directory = Environment
          .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
      file = new File(fileName + directory);
      if (file.exists()) {
        // 如果文件存在则文件的长度等于下载长度
        downloadedLength = file.length();
      }
      long contentLength = getContentLength(downloadUrl);
      if (contentLength == 0) {
        return TYPE_FAILED;
      } else if (contentLength == downloadedLength) {
        return TYPE_SUCCESS;
      }
      OkHttpClient client = new OkHttpClient();
      Request request = new Request.Builder().addHeader("RANGE", "byte=" + downloadedLength + "-")
          .url(downloadUrl).build();
      Response response = client.newCall(request).execute();
      if (response != null) {
        is = response.body().byteStream();
        saveFile = new RandomAccessFile(file, "rw");
        saveFile.seek(downloadedLength); // 跳过已下载字节
        byte[] b = new byte[1024];
        int total = 0;
        int len;
        while ((len = is.read(b)) != -1) {
          if (isCanceled) {
            return TYPE_CANCELED;
          } else if (isPause) {
            return TYPE_PAUSE;
          } else {
            total += len;
            saveFile.write(b, 0, len);
            // 计算已下载的百分比
            int progress = (int) ((total + downloadedLength) * 100 / contentLength);
            publishProgress(progress);
          }
        }
        response.body().close();
        return TYPE_SUCCESS;
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      try {
        if (is != null) {
          is.close();
        }
        if (saveFile != null) {
          saveFile.close();
        }
        if (isCanceled && file != null) {
          file.delete();
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return TYPE_FAILED;
  }

  @Override
  protected void onProgressUpdate(Integer... values) {
    int progress = values[0];
    if (progress > lastProgress){
      listener.onProgress(progress);
      lastProgress = progress;
    }
  }

  @Override
  protected void onPostExecute(Integer integer) {
    switch (integer){
      case TYPE_SUCCESS:
        listener.onSuccess();
        break;
      case TYPE_FAILED:
        listener.onFailed();
        break;
      case TYPE_PAUSE:
        listener.onPaused();
        break;
      case TYPE_CANCELED:
        listener.onCanceled();
      default:
        break;
    }
  }

  public void pauseDownload(){
    isPause = true;
  }

  public void cancelDownload(){
    isCanceled = true;
  }

  private long getContentLength(String downloadUrl) throws IOException {
    OkHttpClient client = new OkHttpClient();
    Request request = new Request.Builder().url(downloadUrl).build();
    Response response = client.newCall(request).execute();
    if (response != null && response.isSuccessful()) {
      long contentLength = response.body().contentLength();
      response.body().close();
      return contentLength;
    }
    return 0;
  }
}
