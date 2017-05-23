package com.zeph.simpledonwloadtool;

import android.Manifest.permission;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

  private EditText mURL;

  private DownloadService.DownloadBinder downloadBinder;

  private ServiceConnection connection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
      downloadBinder = (DownloadService.DownloadBinder) service;
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }
  };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    // 初始化控件
    mURL = (EditText) findViewById(R.id.edit_text);
    Button start_btn = (Button) findViewById(R.id.btn_start);
    Button pause_btn = (Button) findViewById(R.id.btn_pause);
    Button cancel_btn = (Button) findViewById(R.id.btn_cancel);
    start_btn.setOnClickListener(this);
    pause_btn.setOnClickListener(this);
    cancel_btn.setOnClickListener(this);
    Intent intent = new Intent(this, DownloadService.class);
    startService(intent); //启动服务
    bindService(intent, connection, BIND_AUTO_CREATE); //绑定服务
    if (ContextCompat.checkSelfPermission(MainActivity.this, permission.WRITE_EXTERNAL_STORAGE)
        != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(MainActivity.this, new String[]{
          permission.WRITE_EXTERNAL_STORAGE}, 1);
    }
  }

  @Override
  public void onClick(View v) {
    if (downloadBinder == null) {
      return;
    }
    switch (v.getId()) {
      case R.id.btn_start:
        String url = mURL.getEditableText().toString();
        Toast.makeText(this, url, Toast.LENGTH_SHORT).show();
        if (url.indexOf("/") < 0) {
          Toast.makeText(this, "你输入的地址有误请重新输入", Toast.LENGTH_SHORT).show();
          break;
        }else {
          downloadBinder.startDownload(url);
          break;
        }
      case R.id.btn_pause:
        downloadBinder.pauseDownload();
        break;
      case R.id.btn_cancel:
        downloadBinder.cancelDownload();
        break;
      default:
        break;
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
      @NonNull int[] grantResults) {
    switch (requestCode){
      case 1:
        if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED){
          Toast.makeText(this, "拒绝权限将无法使用程序", Toast.LENGTH_SHORT).show();
          finish();
        }
        break;
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    unbindService(connection);
  }
}
