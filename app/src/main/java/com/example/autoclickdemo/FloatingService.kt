package com.example.autoclickdemo

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.Gravity.LEFT
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi

class FloatingService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var overlayView: View
    private val clickPoints = mutableListOf<Point>() // 用来存储点击点
    private var isAddingPoint = false // 标志位模式标志
    private val pointViews = mutableListOf<View>() // 保存所有的标志位视图


    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    @SuppressLint("InternalInsetResource")
    private fun getStatusBarHeight(): Int {
        var result = 0
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = resources.getDimensionPixelSize(resourceId)
        }
        return result
    }

    // 获取屏幕密度
    private fun getScreenDensity(): Float {
        val metrics = resources.displayMetrics
        return metrics.density
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("ClickableViewAccessibility", "RtlHardcoded")
    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // 初始化悬浮窗按钮
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_window, null)
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, // 确保按钮可以点击
            PixelFormat.TRANSLUCENT
        )
        layoutParams.gravity = Gravity.TOP or LEFT
        windowManager.addView(floatingView, layoutParams)

        // 实现拖拽
        floatingView.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var touchX = 0f
            private var touchY = 0f

            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        // 记录初始位置和触摸点
                        initialX = layoutParams.x
                        initialY = layoutParams.y
                        touchX = event.rawX
                        touchY = event.rawY
                        return true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        // 更新悬浮窗的位置
                        layoutParams.x = initialX + (event.rawX - touchX).toInt()
                        layoutParams.y = initialY + (event.rawY - touchY).toInt()

                        // 更新视图位置
                        windowManager.updateViewLayout(floatingView, layoutParams)
                        return true
                    }
                }
                return false
            }
        })


        // 初始化透明层，设置灰色半透明背景，但不添加到WindowManager
        overlayView = View(this).apply {
            setBackgroundColor(Color.parseColor("#80000000")) // 灰色背景 (80 表示 50% 透明度)
        }
        val overlayParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        overlayParams.gravity = Gravity.TOP or LEFT
        overlayView.layoutParams = overlayParams

        // 捕获透明区域的点击事件，记录点击点
        overlayView.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN && isAddingPoint) {
                val overlayLocation = IntArray(2)
                v.getLocationOnScreen(overlayLocation) // 获取 overlayView 在屏幕上的位置

                // 计算点击坐标相对于整个屏幕的位置
                val x = event.rawX.toInt()
                val y = event.rawY.toInt()

                addClickPoint(x, y)

                // 退出标志位模式并移除透明覆盖层
                isAddingPoint = false
                windowManager.removeView(overlayView)
                return@setOnTouchListener true
            }
            false
        }

        setupButtons()

        TaskReceiver.setClickTask(object : TaskReceiver.ClickTask {
            override fun onClick() {
                executeClickEvent()
            }
        })
    }

    @SuppressLint("RtlHardcoded")
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createLabel(x: Int, y: Int) {
        // 创建一个TextView来显示序号
        val pointView = TextView(this).apply {
            text = clickPoints.size.toString() // 显示序号
            setTextColor(Color.WHITE) // 设置文字颜色
            setBackgroundColor(Color.RED) // 设置背景为红色
            gravity = Gravity.CENTER
            textSize = 16f
            layoutParams = ViewGroup.LayoutParams(50, 50) // 这里设置的是TextView的内部尺寸，不是位置
        }

        // 定义点击点的布局参数
        val pointParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )
        pointParams.gravity = Gravity.TOP or LEFT
        pointParams.x = x - (pointView.measuredWidth / 2) // x 是屏幕坐标，需要减去视图宽度的一半以居中
        pointParams.y = y - (pointView.measuredHeight / 2) - getStatusBarHeight() // 同上，对于y坐标
        pointView.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )

        // 将TextView添加到WindowManager以显示序号
        windowManager.addView(pointView, pointParams)
        // 保存标志位视图
        pointViews.add(pointView)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun addClickPoint(x: Int, y: Int) {
        // 保存点击点
        clickPoints.add(Point(x, y))
        createLabel(x, y)
        // 移除透明层，恢复按钮可点击
        windowManager.removeView(overlayView)
    }

    private fun startTask() {
        if (clickPoints.isEmpty()) {
            Toast.makeText(this, "没有点击点，请先添加点击点", Toast.LENGTH_SHORT).show()
            return
        }
        // 隐藏悬浮窗口
        floatingView.visibility = View.GONE

        // 确保在任务开始时透明层已被移除
        if (overlayView.parent != null) {
            windowManager.removeView(overlayView)
        }
        // 取消定时任务，立即执行点击任务
        Constant.alarmExecute = false
        val alarmManager = App.context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(App.context, TaskReceiver::class.java)
        val pendingIntent =
            PendingIntent.getBroadcast(
                App.context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        alarmManager.cancel(pendingIntent)
        executeClickEvent()
    }

    private fun executeClickEvent() {
        if (clickPoints.isEmpty()) {
            Toast.makeText(this, "无点击点，任务执行完毕", Toast.LENGTH_SHORT).show()
        }
        for (view in pointViews) {
            view.visibility = View.GONE
        }
        AutoClickService.instance?.saveClickPoint(clickPoints,
            object : AutoClickService.ClickListener {
                override fun onFinish() {
                    for (view in pointViews) {
                        view.visibility = View.VISIBLE
                    }
                }
            })
    }

    private fun setupButtons() {
        val startButton = floatingView.findViewById<Button>(R.id.btn_start)
        val addPointButton = floatingView.findViewById<Button>(R.id.btn_add_point)
        val setTimeButton = floatingView.findViewById<Button>(R.id.btn_set_time)
        val clearButton = floatingView.findViewById<Button>(R.id.btn_clear)

        // 开始任务按钮
        startButton.setOnClickListener {
            startTask()
        }

        // 增加点击点按钮
        addPointButton.setOnClickListener {
            // 进入设置标志位模式
            isAddingPoint = true
            if (overlayView.parent == null) {
                windowManager.addView(overlayView, overlayView.layoutParams) // 重新添加透明层
            }
        }

        // 设置任务开始时间按钮
        setTimeButton.setOnClickListener {
            showTimePickerDialog()
        }

        // 清除最后一个点击点
        clearButton.setOnClickListener {
            if (clickPoints.isNotEmpty()) {
                // 移除最后一个点击点
                clickPoints.removeLast()

                // 移除最后一个标志位视图
                val lastPointView = pointViews.removeLast()
                windowManager.removeView(lastPointView)
            } else {
                Toast.makeText(this, "没有点击点可清除", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showTimePickerDialog() {
        val intent = Intent(App.context, TransparentActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)  // 在新的任务栈中启动
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        windowManager.removeView(floatingView)
        windowManager.removeView(overlayView)
    }
}




