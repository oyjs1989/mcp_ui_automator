package com.mcp.uiautomator.server

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.mcp.uiautomator.core.UIAutomatorHelper
import com.mcp.uiautomator.model.*
import io.ktor.http.*
import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.*

/**
 * HTTP服务器
 * 暴露UI Automator功能的REST API
 */
class HttpServer(
    private val context: Context,
    private val port: Int = 8080
) {
    companion object {
        private const val TAG = "HttpServer"
    }
    
    private var server: NettyApplicationEngine? = null
    private val uiAutomatorHelper = UIAutomatorHelper(context)
    private val gson = Gson()
    
    /**
     * 启动HTTP服务器
     */
    suspend fun start() {
        try {
            server = embeddedServer(Netty, port = port) {
                configureRouting()
            }
            
            server?.start(wait = false)
            Log.i(TAG, "HTTP Server started on port $port")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start HTTP server", e)
            throw e
        }
    }
    
    /**
     * 停止HTTP服务器
     */
    fun stop() {
        server?.stop(1000, 2000)
        server = null
        Log.i(TAG, "HTTP Server stopped")
    }
    
    /**
     * 配置路由
     */
    private fun Application.configureRouting() {
        install(ContentNegotiation) {
            gson {
                setPrettyPrinting()
                setLenient()
            }
        }
        
        routing {
            // UI信息获取
            get("/ui/dump") {
                try {
                    val pageSource = uiAutomatorHelper.getPageSource()
                    call.respond(HttpStatusCode.OK, pageSource)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to get page source", e)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ActionResponse(
                            success = false,
                            message = "Failed to get page source: ${e.message}",
                            errorCode = ErrorCodes.SERVICE_ERROR
                        )
                    )
                }
            }
            
            // 获取XML格式的UI层次结构
            get("/ui/dump/xml") {
                try {
                    val xmlDump = android.app.UiAutomation().rootUiNode.toString()
                    call.respondText(xmlDump, ContentType.Application.Xml)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to get XML dump", e)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ActionResponse(
                            success = false,
                            message = "Failed to get XML dump: ${e.message}",
                            errorCode = ErrorCodes.SERVICE_ERROR
                        )
                    )
                }
            }
            
            // 点击元素
            post("/ui/click") {
                try {
                    val request = call.receive<ClickRequest>()
                    val response = uiAutomatorHelper.clickElement(request.selector)
                    call.respond(HttpStatusCode.OK, response)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to process click request", e)
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ActionResponse(
                            success = false,
                            message = "Failed to process click request: ${e.message}",
                            errorCode = ErrorCodes.SERVICE_ERROR
                        )
                    )
                }
            }
            
            // 输入文本
            post("/ui/input") {
                try {
                    val request = call.receive<InputRequest>()
                    val response = uiAutomatorHelper.inputText(request)
                    call.respond(HttpStatusCode.OK, response)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to process input request", e)
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ActionResponse(
                            success = false,
                            message = "Failed to process input request: ${e.message}",
                            errorCode = ErrorCodes.SERVICE_ERROR
                        )
                    )
                }
            }
            
            // 滚动操作
            post("/ui/scroll") {
                try {
                    val request = call.receive<ScrollRequest>()
                    val response = uiAutomatorHelper.scroll(request)
                    call.respond(HttpStatusCode.OK, response)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to process scroll request", e)
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ActionResponse(
                            success = false,
                            message = "Failed to process scroll request: ${e.message}",
                            errorCode = ErrorCodes.SERVICE_ERROR
                        )
                    )
                }
            }
            
            // 等待元素
            post("/ui/wait") {
                try {
                    val request = call.receive<WaitRequest>()
                    val response = uiAutomatorHelper.waitForElement(request)
                    call.respond(HttpStatusCode.OK, response)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to process wait request", e)
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ActionResponse(
                            success = false,
                            message = "Failed to process wait request: ${e.message}",
                            errorCode = ErrorCodes.SERVICE_ERROR
                        )
                    )
                }
            }
            
            // 设备操作
            post("/device/back") {
                try {
                    val response = uiAutomatorHelper.pressBack()
                    call.respond(HttpStatusCode.OK, response)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to press back key", e)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ActionResponse(
                            success = false,
                            message = "Failed to press back key: ${e.message}",
                            errorCode = ErrorCodes.SERVICE_ERROR
                        )
                    )
                }
            }
            
            post("/device/home") {
                try {
                    val response = uiAutomatorHelper.pressHome()
                    call.respond(HttpStatusCode.OK, response)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to press home key", e)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ActionResponse(
                            success = false,
                            message = "Failed to press home key: ${e.message}",
                            errorCode = ErrorCodes.SERVICE_ERROR
                        )
                    )
                }
            }
            
            post("/device/recent") {
                try {
                    val response = uiAutomatorHelper.pressRecentApps()
                    call.respond(HttpStatusCode.OK, response)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to press recent apps key", e)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ActionResponse(
                            success = false,
                            message = "Failed to press recent apps key: ${e.message}",
                            errorCode = ErrorCodes.SERVICE_ERROR
                        )
                    )
                }
            }
            
            get("/device/info") {
                try {
                    val deviceInfo = uiAutomatorHelper.getDeviceInfo()
                    call.respond(HttpStatusCode.OK, deviceInfo)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to get device info", e)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ActionResponse(
                            success = false,
                            message = "Failed to get device info: ${e.message}",
                            errorCode = ErrorCodes.SERVICE_ERROR
                        )
                    )
                }
            }
            
            // 健康检查端点
            get("/health") {
                call.respond(
                    HttpStatusCode.OK, 
                    mapOf(
                        "status" to "healthy",
                        "timestamp" to System.currentTimeMillis(),
                        "version" to "1.0.0"
                    )
                )
            }
            
            // 根路径 - API文档
            get("/") {
                val apiDoc = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <title>UI Automator MCP Service</title>
                        <style>
                            body { font-family: Arial, sans-serif; margin: 40px; }
                            .endpoint { background: #f5f5f5; padding: 10px; margin: 10px 0; }
                            .method { color: #007acc; font-weight: bold; }
                        </style>
                    </head>
                    <body>
                        <h1>UI Automator MCP Service API</h1>
                        <div class="endpoint">
                            <div class="method">GET</div> /ui/dump - 获取UI树结构
                        </div>
                        <div class="endpoint">
                            <div class="method">GET</div> /ui/dump/xml - 获取XML格式UI结构
                        </div>
                        <div class="endpoint">
                            <div class="method">POST</div> /ui/click - 点击元素
                        </div>
                        <div class="endpoint">
                            <div class="method">POST</div> /ui/input - 输入文本
                        </div>
                        <div class="endpoint">
                            <div class="method">POST</div> /ui/scroll - 滚动操作
                        </div>
                        <div class="endpoint">
                            <div class="method">POST</div> /ui/wait - 等待元素
                        </div>
                        <div class="endpoint">
                            <div class="method">POST</div> /device/back - 按返回键
                        </div>
                        <div class="endpoint">
                            <div class="method">POST</div> /device/home - 按Home键
                        </div>
                        <div class="endpoint">
                            <div class="method">POST</div> /device/recent - 按最近任务键
                        </div>
                        <div class="endpoint">
                            <div class="method">GET</div> /device/info - 获取设备信息
                        </div>
                        <div class="endpoint">
                            <div class="method">GET</div> /health - 健康检查
                        </div>
                    </body>
                    </html>
                """.trimIndent()
                
                call.respondText(apiDoc, ContentType.Text.Html)
            }
        }
    }
}
