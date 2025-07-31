package com.yy.floatserver

import android.app.AlertDialog
import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.CountDownTimer
import android.os.IBinder
import android.widget.ImageView
import android.widget.Toast
import com.yy.floatserver.server.FloatingServer
import com.yy.floatserver.utils.SettingsCompat

/**
 * Created by yy on 2020/6/13.
 * function: 悬浮窗管理
 */
class FloatManager(private val builder: FloatClient.Builder) : IFloatWindowHandler {

    private var serviceConnection: FloatServiceConnection? = null

    private var floatBinder: FloatingServer.FloatBinder? = null

    private var countDownTimer: CountDownTimer? = null

    init {
        initService()
    }

    private fun initService() {
        serviceConnection = FloatServiceConnection()
        serviceConnection?.let {
            val intent = Intent(builder.context, FloatingServer::class.java)
            builder.context?.bindService(intent, it, Service.BIND_AUTO_CREATE)
        }
    }


    override fun requestPermission() {
        SettingsCompat.manageDrawOverlays(builder.context)
        initCountDown()
    }

    override fun showFloat() {

        if (SettingsCompat.canDrawOverlays(builder.context?.applicationContext)) {
            showFloatWindow()
            builder.callback?.onPermissionResult(true)
        } else {
            if (builder.callback != null) {
                builder.callback?.onPermissionResult(false)
            } else {
                if (builder.enableDefaultPermissionDialog) {
                    showPermissionDialog()
                }
            }
        }

    }

    private fun showFloatWindow() {
        var view = builder.view
        if (view == null) {
            val imageView = ImageView(builder.context)
            imageView.setImageResource(R.mipmap.ic_launcher_round)
            view = imageView
        }

        // 添加双击监听
        val gestureDetector = android.view.GestureDetector(builder.context, object : android.view.GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: android.view.MotionEvent?): Boolean {
                // 触发拍照逻辑
                takePhoto()
                return true
            }
        })
        view.setOnTouchListener { v, event ->
            gestureDetector.onTouchEvent(event)
            false
        }

        floatBinder?.show(view)
        floatBinder?.setClazz(builder.clazz)
    }

    /**
     * 双击悬浮窗时调用，触发拍照
     */
    private fun takePhoto() {
        // 这里仅发送一个广播或回调，实际拍照逻辑建议在Activity中实现
        val intent = android.content.Intent("com.yy.floatserver.ACTION_TAKE_PHOTO")
        builder.context?.sendBroadcast(intent)
        // 你可以在MainActivity注册广播接收器，收到后启动相机并保存图片
        Toast.makeText(builder.context, "正在启动拍照...", Toast.LENGTH_SHORT).show()
    }


    private fun showPermissionDialog() {
        val builder = AlertDialog.Builder(builder.context)
        builder.setTitle("提示")
        builder.setMessage("开启悬浮窗权限，")
        builder.setIcon(R.mipmap.ic_launcher)
        builder.setCancelable(true)
        builder.setPositiveButton(
            "确定"
        ) { dialog, _ ->
            requestPermission()
            dialog.dismiss()
        }
        builder.setNegativeButton(
            "取消"
        ) { dialog, _ ->
            Toast.makeText(builder.context, "你点击了取消", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
        builder.show()
    }

    private fun initCountDown() {
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(Long.MAX_VALUE, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                if (SettingsCompat.canDrawOverlays(builder.context)) {
                    showFloatWindow()
                    countDownTimer?.cancel()
                }
            }

            override fun onFinish() {

            }
        }
        countDownTimer?.start()
    }


    override fun dismissFloat() {
        floatBinder?.hide()
        countDownTimer?.cancel()
    }

    /**
     * 释放资源
     */
    override fun releaseResource() {
        serviceConnection?.let {
            builder.context?.unbindService(it)
        }
        countDownTimer?.cancel()
    }

    inner class FloatServiceConnection : ServiceConnection {

        override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {
            floatBinder = iBinder as FloatingServer.FloatBinder
        }

        override fun onServiceDisconnected(componentName: ComponentName) {

        }
    }
}