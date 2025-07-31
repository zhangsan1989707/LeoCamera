package com.yy.perfectfloatwindow

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.yy.floatserver.FloatClient
import com.yy.floatserver.FloatHelper
import com.yy.floatserver.IFloatPermissionCallback
import kotlinx.android.synthetic.main.activity_main.*
import java.text.SimpleDateFormat
import java.util.*

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File

class MainActivity : AppCompatActivity() {

    private var floatHelper: FloatHelper? = null
    private lateinit var ivIcon: ImageView
    private lateinit var tvContent: TextView
    private var countDownTimer: CountDownTimer? = null

    private val ACTION_TAKE_PHOTO = "com.yy.floatserver.ACTION_TAKE_PHOTO"
    private val REQUEST_CODE_CAMERA = 1001
    private val REQUEST_CODE_PERMISSIONS = 1002
    private var photoUri: Uri? = null
    private var photoFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var view = View.inflate(this, R.layout.float_view, null)

        ivIcon = view.findViewById(R.id.ivIcon)
        tvContent = view.findViewById(R.id.tvContent)

        // 注册拍照广播
        registerReceiver(takePhotoReceiver, IntentFilter(ACTION_TAKE_PHOTO))

        //定义悬浮窗助手
        floatHelper = FloatClient.Builder()
            .with(this)
            .addView(view)
            //是否需要展示默认权限提示弹窗，建议使用自己的项目中弹窗样式（默认开启）
            .enableDefaultPermissionDialog(true)
            .setClickTarget(MainActivity::class.java)
            .addPermissionCallback(object : IFloatPermissionCallback {
                override fun onPermissionResult(granted: Boolean) {
                    //（建议使用addPermissionCallback回调中添加自己的弹窗）
                    Toast.makeText(this@MainActivity, "granted -> $granted", Toast.LENGTH_SHORT)
                        .show()
                    if (!granted) {
                        //申请权限
                        floatHelper?.requestPermission()
                    }
                }
            })
            .build()

        btnShow.setOnClickListener {
            //开启悬浮窗
            floatHelper?.show()
        }

        btnClose.setOnClickListener {
            //隐藏悬浮窗
            floatHelper?.dismiss()
        }

        btnJump.setOnClickListener {
            startActivity(Intent(this, SecondActivity::class.java))
        }

        initCountDown()
    }

    // 拍照广播接收器
    private val takePhotoReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_CODE_PERMISSIONS)
            } else {
                startCamera()
            }
        }
    }

    // 启动相机
    private fun startCamera() {
        val photoDir = getExternalFilesDir("Pictures")
        if (photoDir != null && !photoDir.exists()) {
            photoDir.mkdirs()
        }
        val fileName = "IMG_" + System.currentTimeMillis() + ".jpg"
        photoFile = File(photoDir, fileName)
        photoUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(this, "$packageName.fileprovider", photoFile!!)
        } else {
            Uri.fromFile(photoFile)
        }
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        startActivityForResult(intent, REQUEST_CODE_CAMERA)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                startCamera()
            } else {
                Toast.makeText(this, "需要相机和存储权限", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_CAMERA && resultCode == Activity.RESULT_OK) {
            Toast.makeText(this, "照片已保存: ${photoFile?.absolutePath}", Toast.LENGTH_LONG).show()
        }
    }

    private fun initCountDown() {
        countDownTimer = object : CountDownTimer(Long.MAX_VALUE, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                //更新悬浮窗内容（这里根据自己的业务进行扩展）
                tvContent.text = getLeftTime(millisUntilFinished)
            }

            override fun onFinish() {

            }
        }
        countDownTimer?.start()
    }

    fun getLeftTime(time: Long): String {
        val formatter = SimpleDateFormat("HH:mm:ss")
        formatter.timeZone = TimeZone.getTimeZone("GMT+00:00")
        return formatter.format(time)
    }


    override fun onDestroy() {
        super.onDestroy()
        floatHelper?.release()
        unregisterReceiver(takePhotoReceiver)
    }
}
