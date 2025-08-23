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
import androidx.core.app.NotificationCompat
import com.mcp.uiautomator.MainActivity
import com.mcp.uiautomator.R
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
    
    // Binder for local service binding
    inner class LocalBinder : Binder() {
        fun getService(): UIAutomatorService = this@UIAutomatorService
    }
    
    private val binder = LocalBinder()
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "UIAutomatorService created")
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SERVER -> {
                val port = intent.getIntExtra(EXTRA_PORT, DEFAULT_PORT)
                startServer(port)
            }
            ACTION_STOP_SERVER -> {
                stopServer()
                stopSelf()
            }
        }
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder {
        return binder
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopServer()
        serviceScope.cancel()
        Log.d(TAG, "UIAutomatorService destroyed")
    }
    
    /**
     * 启动HTTP服务器
     */
    fun startServer(port: Int = DEFAULT_PORT) {
        if (isServerRunning) {
            Log.w(TAG, "Server is already running")
            return
        }
        
        currentPort = port
        
        serviceScope.launch {
            try {
                httpServer = HttpServer(this@UIAutomatorService, port)
                httpServer?.start()
                
                isServerRunning = true
                
                withContext(Dispatchers.Main) {
                    startForeground(NOTIFICATION_ID, createNotification())
                    Log.i(TAG, "HTTP Server started on port $port")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start server", e)
                isServerRunning = false
                withContext(Dispatchers.Main) {
                    // 可以发送广播通知启动失败
                }
            }
        }
    }
    
    /**
     * 停止HTTP服务器
     */
    fun stopServer() {
        if (!isServerRunning) {
            return
        }
        
        httpServer?.stop()
        httpServer = null
        isServerRunning = false
        
        stopForeground(STOP_FOREGROUND_REMOVE)
        Log.i(TAG, "HTTP Server stopped")
    }
    
    /**
     * 获取服务器状态
     */
    fun isRunning(): Boolean = isServerRunning
    
    /**
     * 获取当前端口
     */
    fun getCurrentPort(): Int = currentPort
    
    /**
     * 获取服务器URL
     */
    fun getServerUrl(): String? {
        if (!isServerRunning) return null
        
        val ipAddress = getLocalIpAddress()
        return if (ipAddress != null) {
            "http://$ipAddress:$currentPort"
        } else {
            "http://localhost:$currentPort"
        }
    }
    
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
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("UI Automator 服务运行中")
            .setContentText("服务地址: $serverUrl")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "停止服务", stopPendingIntent)
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }
    
    /**
     * 获取本地IP地址
     */
    private fun getLocalIpAddress(): String? {
        try {
            // 优先使用WiFi IP
            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifiInfo = wifiManager.connectionInfo
            if (wifiInfo != null && wifiManager.isWifiEnabled) {
                val ipAddress = wifiInfo.ipAddress
                if (ipAddress != 0) {
                    return String.format(
                        Locale.getDefault(),
                        "%d.%d.%d.%d",
                        ipAddress and 0xff,
                        ipAddress shr 8 and 0xff,
                        ipAddress shr 16 and 0xff,
                        ipAddress shr 24 and 0xff
                    )
                }
            }
            
            // 备选：遍历网络接口
            val networkInterfaces = NetworkInterface.getNetworkInterfaces()
            for (networkInterface in Collections.list(networkInterfaces)) {
                val inetAddresses = networkInterface.inetAddresses
                for (inetAddress in Collections.list(inetAddresses)) {
                    if (!inetAddress.isLoopbackAddress && inetAddress is InetAddress && 
                        !inetAddress.hostAddress?.contains(":") == true) {
                        return inetAddress.hostAddress
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get local IP address", e)
        }
        return null
    }
}
