package com.example.autoclickdemo

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Point
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class AutoClickService : AccessibilityService() {
    interface ClickListener {
        fun onFinish()
    }

    private lateinit var listener: ClickListener
    private val points = mutableListOf<Point>()

    companion object {
        var instance: AutoClickService? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 这个方法可以用来处理你感兴趣的辅助事件，例如屏幕内容变化等
        event?.let {
            when (event.eventType) {
                AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                    // 处理视图点击事件
                    Log.d("AutoClickService", "View clicked")
                }

                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                    // 处理窗口内容变化事件
                    Log.d("AutoClickService", "Window content changed")
                }

                else -> {
                    // 其他事件不处理
                }
            }
        }
    }

    override fun onInterrupt() {
        // 当系统中断辅助服务时调用此方法，可以处理清理操作
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        // 服务连接时可以初始化一些参数
        val config = serviceInfo
        config.flags = AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE
        serviceInfo = config
    }

    fun saveClickPoint(points: List<Point>, listener: ClickListener) {
        this.points.clear()
        this.points.addAll(points)
        this.listener = listener
        //todo：先执行第一个，等待窗口有没有变化，窗口变化结束执行第二个
        performClick(points.first().x, points.first().y)
        this.points.removeAt(0)
    }

    /**
     * 执行点击操作的核心方法
     * @param x 点击的横坐标
     * @param y 点击的纵坐标
     */
    fun performClick(x: Int, y: Int) {
        Log.d("AutoClickService", "Click coordinates: ($x, $y)")
        val config = serviceInfo
        if (config.flags and AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE != 0) {
            Log.d("AutoClickService", "Touch exploration mode is enabled")
        } else {
            Log.d("AutoClickService", "Touch exploration mode is not enabled")
        }

        val path = Path().apply {
            moveTo(x.toFloat(), y.toFloat())
        }

        val gestureDescription = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100)) // 0ms开始，持续100ms
            .build()

        val dispatcherResult =
            dispatchGesture(gestureDescription, object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    super.onCompleted(gestureDescription)
                    Log.d("AutoClickService", "Click at ($x, $y) completed")
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    super.onCancelled(gestureDescription)
                    Log.e("AutoClickService", "Click at ($x, $y) cancelled")
                }
            }, null)

        Log.e("AutoClickService", "dispatcherResult is $dispatcherResult")
    }
}

