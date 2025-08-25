package com.mcp.uiautomator.core

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * 调试日志管理器
 * 提供详细的日志记录和状态监控功能
 */
class DebugLogger(private val context: Context) {
    
    companion object {
        private const val TAG = "DebugLogger"
        private const val LOG_FILE_NAME = "ui_automator_debug.log"
        private const val MAX_LOG_SIZE = 1024 * 1024 // 1MB
        private const val MAX_LOG_FILES = 5
    }
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    private var logFile: File? = null
    private var isDebugMode = false
    
    init {
        setupLogFile()
    }
    
    /**
     * 设置日志文件
     */
    private fun setupLogFile() {
        try {
            val logDir = File(context.filesDir, "logs")
            if (!logDir.exists()) {
                logDir.mkdirs()
            }
            
            logFile = File(logDir, LOG_FILE_NAME)
            
            // 检查日志文件大小，如果超过限制则轮转
            if (logFile!!.exists() && logFile!!.length() > MAX_LOG_SIZE) {
                rotateLogFiles()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to setup log file", e)
        }
    }
    
    /**
     * 轮转日志文件
     */
    private fun rotateLogFiles() {
        try {
            val logDir = logFile!!.parentFile!!
            
            // 删除最老的日志文件
            for (i in MAX_LOG_FILES downTo 1) {
                val oldFile = File(logDir, "$LOG_FILE_NAME.$i")
                if (oldFile.exists()) {
                    oldFile.delete()
                }
            }
            
            // 重命名当前日志文件
            for (i in (MAX_LOG_FILES - 1) downTo 1) {
                val oldFile = File(logDir, "$LOG_FILE_NAME.$i")
                val newFile = File(logDir, "$LOG_FILE_NAME.${i + 1}")
                if (oldFile.exists()) {
                    oldFile.renameTo(newFile)
                }
            }
            
            val backupFile = File(logDir, "$LOG_FILE_NAME.1")
            logFile!!.renameTo(backupFile)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to rotate log files", e)
        }
    }
    
    /**
     * 启用调试模式
     */
    fun enableDebugMode() {
        isDebugMode = true
        log("DEBUG", "Debug mode enabled")
    }
    
    /**
     * 禁用调试模式
     */
    fun disableDebugMode() {
        log("DEBUG", "Debug mode disabled")
        isDebugMode = false
    }
    
    /**
     * 记录信息日志
     */
    fun info(message: String, vararg args: Any?) {
        log("INFO", message.format(*args))
    }
    
    /**
     * 记录警告日志
     */
    fun warn(message: String, vararg args: Any?) {
        log("WARN", message.format(*args))
    }
    
    /**
     * 记录错误日志
     */
    fun error(message: String, throwable: Throwable? = null, vararg args: Any?) {
        val formattedMessage = message.format(*args)
        log("ERROR", formattedMessage)
        
        if (throwable != null) {
            log("ERROR", "Exception: ${throwable.javaClass.simpleName}: ${throwable.message}")
            log("ERROR", "Stack trace: ${throwable.stackTraceToString()}")
        }
    }
    
    /**
     * 记录调试日志
     */
    fun debug(message: String, vararg args: Any?) {
        if (isDebugMode) {
            log("DEBUG", message.format(*args))
        }
    }
    
    /**
     * 记录服务状态
     */
    fun logServiceStatus(status: String, details: String? = null) {
        val message = "Service Status: $status"
        log("STATUS", message)
        if (details != null) {
            log("STATUS", "Details: $details")
        }
    }
    
    /**
     * 记录API调用
     */
    fun logApiCall(method: String, endpoint: String, requestData: String? = null, responseCode: Int? = null) {
        val message = "API Call: $method $endpoint"
        log("API", message)
        
        if (requestData != null) {
            log("API", "Request: $requestData")
        }
        
        if (responseCode != null) {
            log("API", "Response: $responseCode")
        }
    }
    
    /**
     * 记录UI操作
     */
    fun logUIOperation(operation: String, selector: String, result: String? = null) {
        val message = "UI Operation: $operation - Selector: $selector"
        log("UI", message)
        
        if (result != null) {
            log("UI", "Result: $result")
        }
    }
    
    /**
     * 写入日志到文件
     */
    private fun log(level: String, message: String) {
        val timestamp = dateFormat.format(Date())
        val logEntry = "[$timestamp] [$level] $message"
        
        // 同时输出到Logcat和文件
        when (level) {
            "ERROR" -> Log.e(TAG, message)
            "WARN" -> Log.w(TAG, message)
            "INFO" -> Log.i(TAG, message)
            "DEBUG" -> Log.d(TAG, message)
            else -> Log.v(TAG, message)
        }
        
        // 写入文件
        writeToFile(logEntry)
    }
    
    /**
     * 写入日志到文件
     */
    private fun writeToFile(logEntry: String) {
        try {
            logFile?.let { file ->
                FileWriter(file, true).use { writer ->
                    writer.append(logEntry).append("\n")
                    writer.flush()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write to log file", e)
        }
    }
    
    /**
     * 获取日志文件内容
     */
    fun getLogContent(maxLines: Int = 100): String {
        return try {
            logFile?.let { file ->
                if (file.exists()) {
                    val content = file.readText()
                    val contentLines = content.split("\n")
                    if (contentLines.size > maxLines) {
                        contentLines.takeLast(maxLines).joinToString("\n")
                    } else {
                        content
                    }
                } else {
                    "日志文件不存在"
                }
            } ?: "日志文件未初始化"
        } catch (e: Exception) {
            "读取日志文件失败: ${e.message}"
        }
    }
    
    /**
     * 清除日志文件
     */
    fun clearLogs() {
        try {
            logFile?.delete()
            setupLogFile()
            log("INFO", "Logs cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear logs", e)
        }
    }
    
    /**
     * 获取日志文件路径
     */
    fun getLogFilePath(): String? {
        return logFile?.absolutePath
    }
    
    /**
     * 检查是否启用调试模式
     */
    fun isDebugModeEnabled(): Boolean = isDebugMode
}
