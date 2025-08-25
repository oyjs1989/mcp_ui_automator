package com.mcp.uiautomator.server

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.mcp.uiautomator.core.DebugLogger
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
    private val debugLogger = DebugLogger(context)
    
    init {
        debugLogger.info("HttpServer initialized for port $port")
    }
    
    /**
     * 启动HTTP服务器
     */
    suspend fun start() {
        try {
            debugLogger.info("Starting HTTP server on port $port")
            
            server = embeddedServer(Netty, port = port) {
                configureRouting()
            }
            
            server?.start(wait = false)
            debugLogger.logServiceStatus("HTTP_SERVER_STARTED", "Server listening on port $port")
        } catch (e: Exception) {
            debugLogger.error("Failed to start HTTP server", e)
            throw e
        }
    }
    
    /**
     * 停止HTTP服务器
     */
    fun stop() {
        try {
            debugLogger.info("Stopping HTTP server")
            server?.stop(1000, 2000)
            server = null
            debugLogger.logServiceStatus("HTTP_SERVER_STOPPED", "Server stopped")
        } catch (e: Exception) {
            debugLogger.error("Error stopping HTTP server", e)
        }
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
                    debugLogger.logApiCall("GET", "/ui/dump")
                    val pageSource = uiAutomatorHelper.getPageSource()
                    debugLogger.logApiCall("GET", "/ui/dump", null, 200)
                    call.respond(HttpStatusCode.OK, pageSource)
                } catch (e: Exception) {
                    debugLogger.error("Failed to get page source", e)
                    debugLogger.logApiCall("GET", "/ui/dump", null, 500)
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
                    debugLogger.logApiCall("GET", "/ui/dump/xml")
                    // 使用UiDevice获取页面源码，然后转换为XML格式
                    val pageSource = uiAutomatorHelper.getPageSource()
                    val xmlDump = convertToXml(pageSource)
                    debugLogger.logApiCall("GET", "/ui/dump/xml", null, 200)
                    call.respondText(xmlDump, ContentType.Application.Xml)
                } catch (e: Exception) {
                    debugLogger.error("Failed to get XML dump", e)
                    debugLogger.logApiCall("GET", "/ui/dump/xml", null, 500)
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
                    debugLogger.logApiCall("POST", "/ui/click", gson.toJson(request))
                    debugLogger.logUIOperation("CLICK", gson.toJson(request.selector))
                    
                    val response = uiAutomatorHelper.clickElement(request.selector)
                    debugLogger.logApiCall("POST", "/ui/click", null, 200)
                    debugLogger.logUIOperation("CLICK", gson.toJson(request.selector), "Success: ${response.success}")
                    call.respond(HttpStatusCode.OK, response)
                } catch (e: Exception) {
                    debugLogger.error("Failed to process click request", e)
                    debugLogger.logApiCall("POST", "/ui/click", null, 400)
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
                    debugLogger.logApiCall("POST", "/ui/input", gson.toJson(request))
                    debugLogger.logUIOperation("INPUT", gson.toJson(request.selector), "Text: ${request.text}")
                    
                    val response = uiAutomatorHelper.inputText(request)
                    debugLogger.logApiCall("POST", "/ui/input", null, 200)
                    debugLogger.logUIOperation("INPUT", gson.toJson(request.selector), "Success: ${response.success}")
                    call.respond(HttpStatusCode.OK, response)
                } catch (e: Exception) {
                    debugLogger.error("Failed to process input request", e)
                    debugLogger.logApiCall("POST", "/ui/input", null, 400)
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
                    debugLogger.logApiCall("POST", "/ui/scroll", gson.toJson(request))
                    debugLogger.logUIOperation("SCROLL", request.direction)
                    
                    val response = uiAutomatorHelper.scroll(request)
                    debugLogger.logApiCall("POST", "/ui/scroll", null, 200)
                    debugLogger.logUIOperation("SCROLL", request.direction, "Success: ${response.success}")
                    call.respond(HttpStatusCode.OK, response)
                } catch (e: Exception) {
                    debugLogger.error("Failed to process scroll request", e)
                    debugLogger.logApiCall("POST", "/ui/scroll", null, 400)
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
                    debugLogger.logApiCall("POST", "/ui/wait", gson.toJson(request))
                    debugLogger.logUIOperation("WAIT", gson.toJson(request.selector), "Timeout: ${request.timeout}ms")
                    
                    val response = uiAutomatorHelper.waitForElement(request)
                    debugLogger.logApiCall("POST", "/ui/wait", null, 200)
                    debugLogger.logUIOperation("WAIT", gson.toJson(request.selector), "Success: ${response.success}")
                    call.respond(HttpStatusCode.OK, response)
                } catch (e: Exception) {
                    debugLogger.error("Failed to process wait request", e)
                    debugLogger.logApiCall("POST", "/ui/wait", null, 400)
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
                    debugLogger.logApiCall("POST", "/device/back")
                    debugLogger.logUIOperation("DEVICE_BACK", "Back button")
                    
                    val response = uiAutomatorHelper.pressBack()
                    debugLogger.logApiCall("POST", "/device/back", null, 200)
                    debugLogger.logUIOperation("DEVICE_BACK", "Back button", "Success: ${response.success}")
                    call.respond(HttpStatusCode.OK, response)
                } catch (e: Exception) {
                    debugLogger.error("Failed to press back key", e)
                    debugLogger.logApiCall("POST", "/device/back", null, 500)
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
                    debugLogger.logApiCall("POST", "/device/home")
                    debugLogger.logUIOperation("DEVICE_HOME", "Home button")
                    
                    val response = uiAutomatorHelper.pressHome()
                    debugLogger.logApiCall("POST", "/device/home", null, 200)
                    debugLogger.logUIOperation("DEVICE_HOME", "Home button", "Success: ${response.success}")
                    call.respond(HttpStatusCode.OK, response)
                } catch (e: Exception) {
                    debugLogger.error("Failed to press home key", e)
                    debugLogger.logApiCall("POST", "/device/home", null, 500)
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
                    debugLogger.logApiCall("POST", "/device/recent")
                    debugLogger.logUIOperation("DEVICE_RECENT", "Recent apps button")
                    
                    val response = uiAutomatorHelper.pressRecentApps()
                    debugLogger.logApiCall("POST", "/device/recent", null, 200)
                    debugLogger.logUIOperation("DEVICE_RECENT", "Recent apps button", "Success: ${response.success}")
                    call.respond(HttpStatusCode.OK, response)
                } catch (e: Exception) {
                    debugLogger.error("Failed to press recent apps key", e)
                    debugLogger.logApiCall("POST", "/device/recent", null, 500)
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
                    debugLogger.logApiCall("GET", "/device/info")
                    val deviceInfo = uiAutomatorHelper.getDeviceInfo()
                    debugLogger.logApiCall("GET", "/device/info", null, 200)
                    call.respond(HttpStatusCode.OK, deviceInfo)
                } catch (e: Exception) {
                    debugLogger.error("Failed to get device info", e)
                    debugLogger.logApiCall("GET", "/device/info", null, 500)
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
                debugLogger.logApiCall("GET", "/health")
                debugLogger.logApiCall("GET", "/health", null, 200)
                call.respond(
                    HttpStatusCode.OK, 
                    mapOf(
                        "status" to "healthy",
                        "timestamp" to System.currentTimeMillis(),
                        "version" to "1.0.0"
                    )
                )
            }
            
            // 调试日志端点
            get("/debug/logs") {
                try {
                    debugLogger.logApiCall("GET", "/debug/logs")
                    val logContent = debugLogger.getLogContent(100)
                    debugLogger.logApiCall("GET", "/debug/logs", null, 200)
                    call.respondText(logContent, ContentType.Text.Plain)
                } catch (e: Exception) {
                    debugLogger.error("Failed to get debug logs", e)
                    debugLogger.logApiCall("GET", "/debug/logs", null, 500)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ActionResponse(
                            success = false,
                            message = "Failed to get debug logs: ${e.message}",
                            errorCode = ErrorCodes.SERVICE_ERROR
                        )
                    )
                }
            }
            
            // 根路径 - API文档
            get("/") {
                debugLogger.logApiCall("GET", "/")
                debugLogger.logApiCall("GET", "/", null, 200)
                
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
                        <div class="endpoint">
                            <div class="method">GET</div> /debug/logs - 获取调试日志
                        </div>
                    </body>
                    </html>
                """.trimIndent()
                
                call.respondText(apiDoc, ContentType.Text.Html)
            }
        }
    }
    
    /**
     * 将PageSource转换为XML格式
     */
    private fun convertToXml(pageSource: PageSource): String {
        return buildString {
            appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
            appendLine("<ui-hierarchy>")
            appendLine("  <screen-info>")
            appendLine("    <package-name>${pageSource.packageName}</package-name>")
            appendLine("    <timestamp>${pageSource.timestamp}</timestamp>")
            pageSource.screenSize?.let { screen ->
                appendLine("    <screen-size>")
                appendLine("      <width>${screen.width}</width>")
                appendLine("      <height>${screen.height}</height>")
                appendLine("    </screen-size>")
            }
            appendLine("  </screen-info>")
            appendLine("  <root>")
            appendElementToXml(pageSource.root, this, 2)
            appendLine("  </root>")
            appendLine("</ui-hierarchy>")
        }
    }
    
    /**
     * 递归将UIElement转换为XML
     */
    private fun appendElementToXml(element: UIElement, builder: StringBuilder, indent: Int) {
        val indentStr = " ".repeat(indent)
        builder.appendLine("$indentStr<element>")
        builder.appendLine("$indentStr  <class-name>${element.className}</class-name>")
        if (element.resourceId.isNotEmpty()) {
            builder.appendLine("$indentStr  <resource-id>${element.resourceId}</resource-id>")
        }
        if (element.text.isNotEmpty()) {
            builder.appendLine("$indentStr  <text>${element.text}</text>")
        }
        if (element.contentDesc.isNotEmpty()) {
            builder.appendLine("$indentStr  <content-desc>${element.contentDesc}</content-desc>")
        }
        element.bounds?.let { bounds ->
            builder.appendLine("$indentStr  <bounds>")
            builder.appendLine("$indentStr    <left>${bounds.left}</left>")
            builder.appendLine("$indentStr    <top>${bounds.top}</top>")
            builder.appendLine("$indentStr    <right>${bounds.right}</right>")
            builder.appendLine("$indentStr    <bottom>${bounds.bottom}</bottom>")
            builder.appendLine("$indentStr  </bounds>")
        }
        builder.appendLine("$indentStr  <clickable>${element.clickable}</clickable>")
        builder.appendLine("$indentStr  <scrollable>${element.scrollable}</scrollable>")
        builder.appendLine("$indentStr  <enabled>${element.enabled}</enabled>")
        
        if (element.children.isNotEmpty()) {
            builder.appendLine("$indentStr  <children>")
            element.children.forEach { child ->
                appendElementToXml(child, builder, indent + 2)
            }
            builder.appendLine("$indentStr  </children>")
        }
        
        builder.appendLine("$indentStr</element>")
    }
}
