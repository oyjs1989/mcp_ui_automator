package com.mcp.uiautomator.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.mcp.uiautomator.MainActivity
import com.mcp.uiautomator.R
import com.mcp.uiautomator.core.DebugLogger
import com.mcp.uiautomator.server.HttpServer
import kotlinx.coroutines.*
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.*

/**
 * UI Automator 后台服务
 * 运行HTTP服务器并提供前台服务通知
 */
class UIAutomatorService : Service() {
    
    companion object {
        private const val TAG = "UIAutomatorService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "ui_automator_channel"
        private const val DEFAULT_PORT = 8080
        
        const val ACTION_START_SERVER = "com.mcp.uiautomator.START_SERVER"
        const val ACTION_STOP_SERVER = "com.mcp.uiautomator.STOP_SERVER"
        const val EXTRA_PORT = "port"
    }
    
    private var httpServer: HttpServer? = null
    private var serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isServerRunning = false
    private var currentPort = DEFAULT_PORT
    private lateinit var debugLogger: DebugLogger
    
    // Binder for local service binding
    inner class LocalBinder : Binder() {
        fun getService(): UIAutomatorService = this@UIAutomatorService
    }
    
    private val binder = LocalBinder()
    
    override fun onCreate() {
        super.onCreate()
        debugLogger = DebugLogger(this)
        debugLogger.info("UIAutomatorService created")
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        debugLogger.info("onStartCommand called with action: ${intent?.action}")
        
        when (intent?.action) {
            ACTION_START_SERVER -> {
                val port = intent.getIntExtra(EXTRA_PORT, DEFAULT_PORT)
                debugLogger.info("Received START_SERVER action with port: $port")
                startServer(port)
            }
            ACTION_STOP_SERVER -> {
                debugLogger.info("Received STOP_SERVER action")
                stopServer()
                stopSelf()
            }
            else -> {
                debugLogger.warn("Unknown action: ${intent?.action}")
            }
        }
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder {
        debugLogger.info("Service bound")
        return binder
    }
    
    override fun onDestroy() {
        super.onDestroy()
        debugLogger.info("UIAutomatorService destroyed")
        stopServer()
        serviceScope.cancel()
    }
    
    /**
     * 启动HTTP服务器
     */
    fun startServer(port: Int = DEFAULT_PORT) {
        // 验证端口号
        if (port < 1024 || port > 65535) {
            debugLogger.error("Invalid port number: $port, must be between 1024-65535")
            return
        }
        
        if (isServerRunning) {
            debugLogger.warn("Server is already running on port $currentPort")
            return
        }
        
        debugLogger.info("Starting HTTP server on port $port")
        currentPort = port
        
        serviceScope.launch {
            try {
                debugLogger.info("Creating HttpServer instance")
                httpServer = HttpServer(this@UIAutomatorService, port)
                
                debugLogger.info("Starting HttpServer")
                httpServer?.start()
                
                isServerRunning = true
                debugLogger.logServiceStatus("RUNNING", "Server started successfully on port $port")
                
                withContext(Dispatchers.Main) {
                    startForeground(NOTIFICATION_ID, createNotification())
                    debugLogger.info("Foreground service started with notification")
                }
            } catch (e: Exception) {
                debugLogger.error("Failed to start server", e)
                isServerRunning = false
                debugLogger.logServiceStatus("FAILED", "Server failed to start: ${e.message}")
                
                withContext(Dispatchers.Main) {
                    // 可以发送广播通知启动失败
                }
            }
        }
    }
    
    /**
     * 停止HTTP服务器
     */
    @RequiresApi(Build.VERSION_CODES.N)
    fun stopServer() {
        if (!isServerRunning) {
            debugLogger.info("Server is not running, nothing to stop")
            return
        }
        
        debugLogger.info("Stopping HTTP server")
        
        try {
            httpServer?.stop()
            httpServer = null
            isServerRunning = false
            
            stopForeground(STOP_FOREGROUND_REMOVE)
            debugLogger.logServiceStatus("STOPPED", "Server stopped successfully")
        } catch (e: Exception) {
            debugLogger.error("Error stopping server", e)
        }
    }
    
    /**
     * 获取服务器状态
     */
    fun isRunning(): Boolean {
        debugLogger.debug("Checking server status: $isServerRunning")
        return isServerRunning
    }
    
    /**
     * 获取当前端口
     */
    fun getCurrentPort(): Int = currentPort
    
    /**
     * 获取服务器URL
     */
    fun getServerUrl(): String? {
        if (!isServerRunning) {
            debugLogger.debug("Server not running, cannot get URL")
            return null
        }
        
        val ipAddress = getLocalIpAddress()
        val url = if (ipAddress != null) {
            "http://$ipAddress:$currentPort"
        } else {
            "http://localhost:$currentPort"
        }
        
        debugLogger.debug("Server URL: $url")
        return url
    }
    
    /**
     * 获取调试日志器
     */
    fun getDebugLogger(): DebugLogger = debugLogger
    
    /**
     * 创建通知渠道
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "UI Automator Service"
            val descriptionText = "UI自动化服务运行状态"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            debugLogger.info("Notification channel created")
        }
    }
    
    /**
     * 创建前台服务通知
     */
    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val stopIntent = Intent(this, UIAutomatorService::class.java).apply {
            action = ACTION_STOP_SERVER
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val serverUrl = getServerUrl() ?: "http://localhost:$currentPort"
        
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("UI Automator 服务运行中")
            .setContentText("服务地址: $serverUrl")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "停止服务", stopPendingIntent)
            .setOngoing(true)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        }
        
        debugLogger.debug("Notification created with URL: $serverUrl")
        return builder.build()
    }
    
    /**
     * 获取本地IP地址
     */
    private fun getLocalIpAddress(): String? {
        try {
            debugLogger.debug("Getting local IP address")
            
            // 优先使用WiFi IP
            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifiInfo = wifiManager.connectionInfo
            if (wifiInfo != null && wifiManager.isWifiEnabled) {
                val ipAddress = wifiInfo.ipAddress
                if (ipAddress != 0) {
                    val ip = String.format(
                        Locale.getDefault(),
                        "%d.%d.%d.%d",
                        ipAddress and 0xff,
                        ipAddress shr 8 and 0xff,
                        ipAddress shr 16 and 0xff,
                        ipAddress shr 24 and 0xff
                    )
                    debugLogger.debug("Found WiFi IP: $ip")
                    return ip
                }
            }
            
            // 备选：遍历网络接口
            val networkInterfaces = NetworkInterface.getNetworkInterfaces()
            for (networkInterface in Collections.list(networkInterfaces)) {
                val inetAddresses = networkInterface.inetAddresses
                for (inetAddress in Collections.list(inetAddresses)) {
                    if (!inetAddress.isLoopbackAddress && inetAddress is InetAddress && 
                        inetAddress.hostAddress?.contains(":") != true) {
                        val ip = inetAddress.hostAddress
                        debugLogger.debug("Found network IP: $ip")
                        return ip
                    }
                }
            }
            
            debugLogger.warn("No suitable IP address found")
            return null
        } catch (e: Exception) {
            debugLogger.error("Failed to get local IP address", e)
            return null
        }
    }
}
