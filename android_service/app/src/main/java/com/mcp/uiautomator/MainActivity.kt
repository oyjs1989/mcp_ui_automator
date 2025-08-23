package com.mcp.uiautomator

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.mcp.uiautomator.databinding.ActivityMainBinding
import com.mcp.uiautomator.service.UIAutomatorService

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private var uiAutomatorService: UIAutomatorService? = null
    private var serviceBound = false
    
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as UIAutomatorService.LocalBinder
            uiAutomatorService = binder.getService()
            serviceBound = true
            updateUI()
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            uiAutomatorService = null
            serviceBound = false
            updateUI()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        checkPermissions()
    }
    
    override fun onStart() {
        super.onStart()
        // 绑定服务
        Intent(this, UIAutomatorService::class.java).also { intent ->
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }
    
    override fun onStop() {
        super.onStop()
        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
        }
    }
    
    private fun setupUI() {
        binding.apply {
            // 启动服务按钮
            buttonStartService.setOnClickListener {
                val port = editTextPort.text.toString().toIntOrNull() ?: 8080
                startUIAutomatorService(port)
            }
            
            // 停止服务按钮
            buttonStopService.setOnClickListener {
                stopUIAutomatorService()
            }
            
            // 打开浏览器按钮
            buttonOpenBrowser.setOnClickListener {
                openServerInBrowser()
            }
            
            // 权限设置按钮
            buttonPermissions.setOnClickListener {
                requestOverlayPermission()
            }
            
            // 刷新状态按钮
            buttonRefresh.setOnClickListener {
                updateUI()
            }
            
            // 设置默认端口
            editTextPort.setText("8080")
        }
        
        updateUI()
    }
    
    private fun checkPermissions() {
        // 检查系统警告窗权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                showPermissionDialog()
            }
        }
    }
    
    private fun showPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("权限需求")
            .setMessage("UI自动化服务需要系统警告窗权限才能正常工作。请在设置中授予权限。")
            .setPositiveButton("去设置") { _, _ ->
                requestOverlayPermission()
            }
            .setNegativeButton("稍后") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
    
    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }
    
    private fun startUIAutomatorService(port: Int) {
        if (!serviceBound || uiAutomatorService == null) {
            // 如果服务未绑定，先启动服务
            val intent = Intent(this, UIAutomatorService::class.java).apply {
                action = UIAutomatorService.ACTION_START_SERVER
                putExtra(UIAutomatorService.EXTRA_PORT, port)
            }
            ContextCompat.startForegroundService(this, intent)
            Toast.makeText(this, "正在启动服务...", Toast.LENGTH_SHORT).show()
        } else {
            uiAutomatorService?.startServer(port)
            updateUI()
        }
    }
    
    private fun stopUIAutomatorService() {
        uiAutomatorService?.stopServer()
        updateUI()
    }
    
    private fun openServerInBrowser() {
        val serverUrl = uiAutomatorService?.getServerUrl()
        if (serverUrl != null) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(serverUrl))
            try {
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "无法打开浏览器: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, "服务未启动", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun updateUI() {
        binding.apply {
            val isRunning = uiAutomatorService?.isRunning() ?: false
            val serverUrl = uiAutomatorService?.getServerUrl()
            
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
                
                示例命令:
                curl http://设备IP:8080/ui/dump
                curl http://设备IP:8080/health
            """.trimIndent()
        }
    }
}
