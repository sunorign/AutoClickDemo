package com.example.autoclickdemo

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Point
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class AutoClickService : AccessibilityService() {
    interface ClickListener {
        fun onFinish()
    }

    private lateinit var listener: ClickListener
    private val points = mutableListOf<Point>()
    private var isWaitingForWindowChange = false // 标识是否在等待窗口变化
    private var lastContentChangeTimestamp: Long = 0
    private val stabilityCheckDelay = 250L // 500毫秒的窗口稳定检查
    private val handler = Handler(Looper.getMainLooper())

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
        event?.let {
            when (event.eventType) {
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                    if (isWaitingForWindowChange) {
                        // 每次窗口内容变化，记录最新的变化时间戳
                        lastContentChangeTimestamp = System.currentTimeMillis()

                        // 取消之前的延迟任务（如果有）
                        handler.removeCallbacksAndMessages(null)

                        // 延迟执行检查，如果在指定时间内没有新的窗口变化，认为窗口稳定
                        handler.postDelayed({
                            val now = System.currentTimeMillis()
                            if (now - lastContentChangeTimestamp >= stabilityCheckDelay) {
                                Log.d("AutoClickService", "Window content stabilized, performing next click")
                                isWaitingForWindowChange = false
                                performNextClick() // 执行下一个点击
                            }
                        }, stabilityCheckDelay)
                    }
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
        if (points.isNotEmpty()) {
            performNextClick() // 执行第一个点击
        }
    }

    /**
     * 执行点击操作的核心方法
     * @param x 点击的横坐标
     * @param y 点击的纵坐标
     */
    private fun performClick(x: Int, y: Int) {
        handler.post {
            Log.d("AutoClickService", "Click coordinates: ($x, $y)")

            val path = Path().apply {
                moveTo(x.toFloat(), y.toFloat())
            }

            val gestureDescription = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, 100)) // 0ms开始，持续100ms
                .build()

            isWaitingForWindowChange = true // 标记等待窗口变化

            dispatchGesture(gestureDescription, object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    super.onCompleted(gestureDescription)
                    Log.d("AutoClickService", "Click at ($x, $y) completed")
                    // 点击完成后，等待窗口变化完成
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    super.onCancelled(gestureDescription)
                    Log.e("AutoClickService", "Click at ($x, $y) cancelled")
                    isWaitingForWindowChange = false
                    handler.removeCallbacksAndMessages(null)
                }
            }, null)
        }
    }

    /**
     * 执行下一个点击操作
     */
    private fun performNextClick() {
        if (points.isNotEmpty()) {
            val nextPoint = points.removeAt(0) // 先获取并移除第一个点
            performClick(nextPoint.x, nextPoint.y)
        } else {
            // 所有点击点都执行完毕，通知回调
            listener.onFinish()
        }
    }
}





