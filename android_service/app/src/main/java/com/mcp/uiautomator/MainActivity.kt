package com.mcp.uiautomator

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.IBinder
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.mcp.uiautomator.core.DebugLogger
import com.mcp.uiautomator.databinding.ActivityMainBinding
import com.mcp.uiautomator.service.UIAutomatorService

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private var uiAutomatorService: UIAutomatorService? = null
    private var serviceBound = false
    private lateinit var debugLogger: DebugLogger
    private val handler = Handler(Looper.getMainLooper())
    private var isLogUpdateRunning = false
    
    companion object {
        private const val PREF_NAME = "ui_automator_prefs"
        private const val KEY_LAST_PORT = "last_port"
        private const val DEFAULT_PORT = "8080"
    }
    
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as UIAutomatorService.LocalBinder
            uiAutomatorService = binder.getService()
            serviceBound = true
            debugLogger.info("Service connected successfully")
            updateUI()
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            uiAutomatorService = null
            serviceBound = false
            debugLogger.warn("Service disconnected")
            updateUI()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // 初始化调试日志器
        debugLogger = DebugLogger(this)
        debugLogger.info("MainActivity created")
        
        setupUI()
        checkPermissions()
        loadLastPort()
    }
    
    override fun onStart() {
        super.onStart()
        debugLogger.info("MainActivity started")
        
        // 绑定服务
        Intent(this, UIAutomatorService::class.java).also { intent ->
            try {
                bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
                debugLogger.info("Attempting to bind service")
            } catch (e: Exception) {
                debugLogger.error("Failed to bind service", e)
                Toast.makeText(this, "服务绑定失败: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    override fun onStop() {
        super.onStop()
        debugLogger.info("MainActivity stopped")
        
        if (serviceBound) {
            try {
                unbindService(serviceConnection)
                serviceBound = false
                debugLogger.info("Service unbound")
            } catch (e: Exception) {
                debugLogger.error("Failed to unbind service", e)
            }
        }
        
        // 停止日志更新
        stopLogUpdate()
    }
    
    private fun setupUI() {
        binding.apply {
            // 启动服务按钮
            buttonStartService.setOnClickListener {
                startServiceWithCurrentPort()
            }
            
            // 端口输入框回车监听
            editTextPort.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE || 
                    actionId == EditorInfo.IME_ACTION_GO) {
                    startServiceWithCurrentPort()
                    hideKeyboard()
                    return@setOnEditorActionListener true
                }
                false
            }
            
            // 停止服务按钮
            buttonStopService.setOnClickListener {
                debugLogger.info("User clicked stop service button")
                stopUIAutomatorService()
            }
            
            // 打开浏览器按钮
            buttonOpenBrowser.setOnClickListener {
                debugLogger.info("User clicked open browser button")
                openServerInBrowser()
            }
            
            // 权限设置按钮
            buttonPermissions.setOnClickListener {
                debugLogger.info("User clicked permissions button")
                requestOverlayPermission()
            }
            
            // 刷新状态按钮
            buttonRefresh.setOnClickListener {
                debugLogger.info("User clicked refresh button")
                updateUI()
            }
            
            // 调试模式开关
            switchDebugMode.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    debugLogger.enableDebugMode()
                    debugLogger.info("Debug mode enabled by user")
                    startLogUpdate()
                    Toast.makeText(this@MainActivity, "调试模式已启用", Toast.LENGTH_SHORT).show()
                } else {
                    debugLogger.disableDebugMode()
                    debugLogger.info("Debug mode disabled by user")
                    stopLogUpdate()
                    Toast.makeText(this@MainActivity, "调试模式已禁用", Toast.LENGTH_SHORT).show()
                }
                updateDebugUI()
            }
            
            // 清除日志按钮
            buttonClearLogs.setOnClickListener {
                debugLogger.info("User clicked clear logs button")
                showClearLogsDialog()
            }
            
            // 导出日志按钮
            buttonExportLogs.setOnClickListener {
                debugLogger.info("User clicked export logs button")
                exportLogs()
            }
            
            // 设置默认端口（会在loadLastPort中加载上次保存的端口）
            editTextPort.setText(DEFAULT_PORT)
        }
        
        updateUI()
        updateDebugUI()
    }
    
    private fun checkPermissions() {
        debugLogger.info("Checking permissions")
        
        // 检查系统警告窗权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                debugLogger.warn("Overlay permission not granted")
                showPermissionDialog()
            } else {
                debugLogger.info("Overlay permission already granted")
            }
        }
    }
    
    private fun showPermissionDialog() {
        debugLogger.info("Showing permission dialog")
        AlertDialog.Builder(this)
            .setTitle("权限需求")
            .setMessage("UI自动化服务需要系统警告窗权限才能正常工作。请在设置中授予权限。")
            .setPositiveButton("去设置") { _, _ ->
                debugLogger.info("User chose to go to settings")
                requestOverlayPermission()
            }
            .setNegativeButton("稍后") { dialog, _ ->
                debugLogger.info("User chose to skip permission for now")
                dialog.dismiss()
            }
            .show()
    }
    
    private fun requestOverlayPermission() {
        debugLogger.info("Requesting overlay permission")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            try {
                startActivity(intent)
            } catch (e: Exception) {
                debugLogger.error("Failed to open overlay permission settings", e)
                Toast.makeText(this, "无法打开权限设置: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun startUIAutomatorService(port: Int) {
        debugLogger.info("Starting UI Automator service on port $port")
        
        if (!serviceBound || uiAutomatorService == null) {
            debugLogger.warn("Service not bound, starting service first")
            
            // 如果服务未绑定，先启动服务
            val intent = Intent(this, UIAutomatorService::class.java).apply {
                action = UIAutomatorService.ACTION_START_SERVER
                putExtra(UIAutomatorService.EXTRA_PORT, port)
            }
            
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ContextCompat.startForegroundService(this, intent)
                } else {
                    startService(intent)
                }
                debugLogger.info("Service start intent sent")
                Toast.makeText(this, "正在启动服务...", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                debugLogger.error("Failed to start service", e)
                Toast.makeText(this, "启动服务失败: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            debugLogger.info("Service already bound, starting server directly")
            uiAutomatorService?.startServer(port)
            updateUI()
        }
    }
    
    private fun stopUIAutomatorService() {
        debugLogger.info("Stopping UI Automator service")
        uiAutomatorService?.stopServer()
        updateUI()
    }
    
    private fun openServerInBrowser() {
        val serverUrl = uiAutomatorService?.getServerUrl()
        if (serverUrl != null) {
            debugLogger.info("Opening server URL in browser: $serverUrl")
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(serverUrl))
            try {
                startActivity(intent)
            } catch (e: Exception) {
                debugLogger.error("Failed to open browser", e)
                Toast.makeText(this, "无法打开浏览器: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            debugLogger.warn("Cannot open browser: service not running")
            Toast.makeText(this, "服务未启动", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun updateUI() {
        binding.apply {
            val isRunning = uiAutomatorService?.isRunning() ?: false
            val serverUrl = uiAutomatorService?.getServerUrl()
            
            debugLogger.debug("Updating UI - Service running: $isRunning, URL: $serverUrl")
            
            // 更新状态文本
            if (isRunning) {
                textViewStatus.text = "服务状态: 运行中"
                textViewStatus.setTextColor(ContextCompat.getColor(this@MainActivity, android.R.color.holo_green_dark))
                textViewUrl.text = "服务地址: $serverUrl"
                textViewUrl.visibility = android.view.View.VISIBLE
            } else {
                textViewStatus.text = "服务状态: 已停止"
                textViewStatus.setTextColor(ContextCompat.getColor(this@MainActivity, android.R.color.holo_red_dark))
                textViewUrl.visibility = android.view.View.GONE
            }
            
            // 更新按钮状态
            buttonStartService.isEnabled = !isRunning
            buttonStopService.isEnabled = isRunning
            buttonOpenBrowser.isEnabled = isRunning
            
            // 更新权限状态
            val hasOverlayPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(this@MainActivity)
            } else {
                true
            }
            
            textViewPermissions.text = if (hasOverlayPermission) {
                "权限状态: 已授权"
            } else {
                "权限状态: 需要系统警告窗权限"
            }
            textViewPermissions.setTextColor(
                ContextCompat.getColor(
                    this@MainActivity,
                    if (hasOverlayPermission) android.R.color.holo_green_dark 
                    else android.R.color.holo_orange_dark
                )
            )
            
            // 显示使用说明
            textViewInstructions.text = """
                使用说明:
                1. 点击"启动服务"开始HTTP服务器
                2. 使用显示的URL地址进行API调用
                3. 支持的API端点请访问服务器根路径查看
                4. 可使用adb端口转发: adb forward tcp:8080 tcp:8080
                5. 启用调试模式查看详细日志
                
                示例命令:
                curl http://设备IP:8080/ui/dump
                curl http://设备IP:8080/health
            """.trimIndent()
        }
    }
    
    private fun updateDebugUI() {
        binding.apply {
            val isDebugEnabled = debugLogger.isDebugModeEnabled()
            
            // 更新调试模式开关状态
            switchDebugMode.isChecked = isDebugEnabled
            
            // 更新调试相关按钮状态
            buttonClearLogs.isEnabled = true
            buttonExportLogs.isEnabled = true
            
            // 更新日志显示区域
            if (isDebugEnabled) {
                scrollViewLogs.visibility = android.view.View.VISIBLE
                textViewLogs.text = debugLogger.getLogContent(50)
            } else {
                scrollViewLogs.visibility = android.view.View.GONE
            }
        }
    }
    
    private fun startLogUpdate() {
        if (!isLogUpdateRunning) {
            isLogUpdateRunning = true
            debugLogger.info("Starting log update task")
            
            val logUpdateRunnable = object : Runnable {
                override fun run() {
                    if (isLogUpdateRunning && debugLogger.isDebugModeEnabled()) {
                        binding.textViewLogs.text = debugLogger.getLogContent(50)
                        handler.postDelayed(this, 2000) // 每2秒更新一次
                    }
                }
            }
            
            handler.post(logUpdateRunnable)
        }
    }
    
    private fun stopLogUpdate() {
        isLogUpdateRunning = false
        debugLogger.info("Stopping log update task")
    }
    
    private fun showClearLogsDialog() {
        AlertDialog.Builder(this)
            .setTitle("清除日志")
            .setMessage("确定要清除所有日志记录吗？此操作不可撤销。")
            .setPositiveButton("清除") { _, _ ->
                debugLogger.clearLogs()
                debugLogger.info("Logs cleared by user")
                updateDebugUI()
                Toast.makeText(this, "日志已清除", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
    
    private fun exportLogs() {
        try {
            val logPath = debugLogger.getLogFilePath()
            if (logPath != null) {
                debugLogger.info("Exporting logs from: $logPath")
                Toast.makeText(this, "日志文件路径: $logPath", Toast.LENGTH_LONG).show()
            } else {
                debugLogger.warn("Log file path is null")
                Toast.makeText(this, "日志文件路径不可用", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            debugLogger.error("Failed to export logs", e)
            Toast.makeText(this, "导出日志失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    /**
     * 启动服务并保存端口号
     */
    private fun startServiceWithCurrentPort() {
        val portText = binding.editTextPort.text.toString()
        val port = if (portText.isNotEmpty()) {
            portText.toIntOrNull() ?: 8080
        } else {
            8080
        }
        
        // 验证端口号范围
        if (port < 1024 || port > 65535) {
            Toast.makeText(this@MainActivity, "端口号必须在1024-65535之间", Toast.LENGTH_SHORT).show()
            debugLogger.warn("Invalid port number: $port")
            return
        }
        
        // 保存端口号
        savePort(port.toString())
        
        debugLogger.info("User clicked start service button, port: $port")
        startUIAutomatorService(port)
    }
    
    /**
     * 保存端口号到SharedPreferences
     */
    private fun savePort(port: String) {
        try {
            val sharedPrefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            sharedPrefs.edit().putString(KEY_LAST_PORT, port).apply()
            debugLogger.info("Port saved: $port")
        } catch (e: Exception) {
            debugLogger.error("Failed to save port", e)
        }
    }
    
    /**
     * 加载上次保存的端口号
     */
    private fun loadLastPort() {
        try {
            val sharedPrefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val lastPort = sharedPrefs.getString(KEY_LAST_PORT, DEFAULT_PORT) ?: DEFAULT_PORT
            binding.editTextPort.setText(lastPort)
            debugLogger.info("Last port loaded: $lastPort")
        } catch (e: Exception) {
            debugLogger.error("Failed to load last port", e)
            binding.editTextPort.setText(DEFAULT_PORT)
        }
    }
    
    /**
     * 隐藏键盘
     */
    private fun hideKeyboard() {
        try {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(binding.editTextPort.windowToken, 0)
            debugLogger.debug("Keyboard hidden")
        } catch (e: Exception) {
            debugLogger.error("Failed to hide keyboard", e)
        }
    }
}
