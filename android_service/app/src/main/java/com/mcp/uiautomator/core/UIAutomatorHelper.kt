package com.mcp.uiautomator.core

import android.app.UiAutomation
import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.util.Log
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import com.google.gson.Gson
import com.mcp.uiautomator.model.*

/**
 * UI Automator 功能封装
 * 提供UI信息获取和交互操作
 */
class UIAutomatorHelper(private val context: Context) {
    
    companion object {
        private const val TAG = "UIAutomatorHelper"
        private const val DEFAULT_TIMEOUT = 5000L
    }
    
    private val uiDevice: UiDevice by lazy {
        UiDevice.getInstance()
    }
    
    private val debugLogger = DebugLogger(context)
    private val gson = Gson()
    
    init {
        debugLogger.info("UIAutomatorHelper initialized")
    }
    
    /**
     * 获取当前屏幕的UI树
     */
    fun getPageSource(): PageSource {
        debugLogger.debug("Getting page source")
        return try {
            val rootNode = uiDevice.findObject(UiSelector())
            val rootElement = parseUIElement(rootNode)
            
            val currentPackage = uiDevice.currentPackageName ?: ""
            val screenBounds = Bounds(0, 0, uiDevice.displayWidth, uiDevice.displayHeight)
            
            debugLogger.debug("Page source retrieved - Package: $currentPackage, Screen: ${uiDevice.displayWidth}x${uiDevice.displayHeight}")
            
            PageSource(
                root = rootElement,
                timestamp = System.currentTimeMillis(),
                packageName = currentPackage,
                screenSize = screenBounds
            )
        } catch (e: Exception) {
            debugLogger.error("Failed to get page source", e)
            // 返回一个空的页面源
            PageSource(
                root = UIElement(className = "android.widget.FrameLayout"),
                packageName = uiDevice.currentPackageName ?: ""
            )
        }
    }
    
    /**
     * 点击元素
     */
    fun clickElement(selector: ElementSelector): ActionResponse {
        debugLogger.debug("Attempting to click element with selector: ${gson.toJson(selector)}")
        
        if (!selector.isValid()) {
            debugLogger.warn("Invalid selector provided: ${gson.toJson(selector)}")
            return ActionResponse(
                success = false,
                message = "Invalid selector",
                errorCode = ErrorCodes.INVALID_SELECTOR
            )
        }
        
        return try {
            val element = findElement(selector)
            if (element != null) {
                debugLogger.debug("Element found, attempting click")
                element.click()
                debugLogger.debug("Element clicked successfully")
                ActionResponse(
                    success = true,
                    message = "Element clicked successfully",
                    elementFound = true
                )
            } else {
                debugLogger.warn("Element not found with selector: ${gson.toJson(selector)}")
                ActionResponse(
                    success = false,
                    message = "Element not found",
                    elementFound = false,
                    errorCode = ErrorCodes.ELEMENT_NOT_FOUND
                )
            }
        } catch (e: Exception) {
            debugLogger.error("Failed to click element", e)
            ActionResponse(
                success = false,
                message = "Click operation failed: ${e.message}",
                errorCode = ErrorCodes.OPERATION_FAILED
            )
        }
    }
    
    /**
     * 输入文本
     */
    fun inputText(request: InputRequest): ActionResponse {
        debugLogger.debug("Attempting to input text: '${request.text}' with selector: ${gson.toJson(request.selector)}")
        
        if (!request.selector.isValid()) {
            debugLogger.warn("Invalid selector provided: ${gson.toJson(request.selector)}")
            return ActionResponse(
                success = false,
                message = "Invalid selector",
                errorCode = ErrorCodes.INVALID_SELECTOR
            )
        }
        
        return try {
            val element = findElement(request.selector)
            if (element != null) {
                debugLogger.debug("Element found, attempting text input")
                if (request.clearFirst) {
                    debugLogger.debug("Clearing element first")
                    element.clear()
                }
                element.setText(request.text)
                debugLogger.debug("Text input completed successfully")
                ActionResponse(
                    success = true,
                    message = "Text input successfully",
                    elementFound = true
                )
            } else {
                debugLogger.warn("Element not found with selector: ${gson.toJson(request.selector)}")
                ActionResponse(
                    success = false,
                    message = "Element not found",
                    elementFound = false,
                    errorCode = ErrorCodes.ELEMENT_NOT_FOUND
                )
            }
        } catch (e: Exception) {
            debugLogger.error("Failed to input text", e)
            ActionResponse(
                success = false,
                message = "Input operation failed: ${e.message}",
                errorCode = ErrorCodes.OPERATION_FAILED
            )
        }
    }
    
    /**
     * 滚动操作
     */
    fun scroll(request: ScrollRequest): ActionResponse {
        debugLogger.debug("Attempting to scroll ${request.direction} with steps: ${request.steps}")
        
        val direction = when (request.direction.lowercase()) {
            "up" -> Direction.UP
            "down" -> Direction.DOWN
            "left" -> Direction.LEFT
            "right" -> Direction.RIGHT
            else -> {
                debugLogger.warn("Invalid scroll direction: ${request.direction}")
                return ActionResponse(
                    success = false,
                    message = "Invalid scroll direction: ${request.direction}",
                    errorCode = ErrorCodes.INVALID_DIRECTION
                )
            }
        }
        
        return try {
            val success = if (request.selector != null && request.selector.isValid()) {
                // 滚动指定元素
                debugLogger.debug("Scrolling specific element with selector: ${gson.toJson(request.selector)}")
                val element = findElement(request.selector)
                if (element != null) {
                    element.scroll(direction, 1.0f, request.steps)
                } else {
                    debugLogger.warn("Scroll container not found with selector: ${gson.toJson(request.selector)}")
                    return ActionResponse(
                        success = false,
                        message = "Scroll container not found",
                        elementFound = false,
                        errorCode = ErrorCodes.ELEMENT_NOT_FOUND
                    )
                }
            } else {
                // 滚动整个屏幕
                debugLogger.debug("Scrolling entire screen")
                when (direction) {
                    Direction.UP -> uiDevice.swipe(
                        uiDevice.displayWidth / 2, uiDevice.displayHeight * 2 / 3,
                        uiDevice.displayWidth / 2, uiDevice.displayHeight / 3,
                        10
                    )
                    Direction.DOWN -> uiDevice.swipe(
                        uiDevice.displayWidth / 2, uiDevice.displayHeight / 3,
                        uiDevice.displayWidth / 2, uiDevice.displayHeight * 2 / 3,
                        10
                    )
                    Direction.LEFT -> uiDevice.swipe(
                        uiDevice.displayWidth * 2 / 3, uiDevice.displayHeight / 2,
                        uiDevice.displayWidth / 3, uiDevice.displayHeight / 2,
                        10
                    )
                    Direction.RIGHT -> uiDevice.swipe(
                        uiDevice.displayWidth / 3, uiDevice.displayHeight / 2,
                        uiDevice.displayWidth * 2 / 3, uiDevice.displayHeight / 2,
                        10
                    )
                }
                true
            }
            
            debugLogger.debug("Scroll operation completed with success: $success")
            ActionResponse(
                success = success,
                message = if (success) "Scroll completed" else "Scroll failed"
            )
        } catch (e: Exception) {
            debugLogger.error("Failed to scroll", e)
            ActionResponse(
                success = false,
                message = "Scroll operation failed: ${e.message}",
                errorCode = ErrorCodes.OPERATION_FAILED
            )
        }
    }
    
    /**
     * 等待元素
     */
    fun waitForElement(request: WaitRequest): ActionResponse {
        debugLogger.debug("Waiting for element with selector: ${gson.toJson(request.selector)}, condition: ${request.condition}, timeout: ${request.timeout}ms")
        
        if (!request.selector.isValid()) {
            debugLogger.warn("Invalid selector provided: ${gson.toJson(request.selector)}")
            return ActionResponse(
                success = false,
                message = "Invalid selector",
                errorCode = ErrorCodes.INVALID_SELECTOR
            )
        }
        
        return try {
            val bySelector = createBySelector(request.selector)
            val success = when (request.condition.lowercase()) {
                "visible" -> {
                    debugLogger.debug("Waiting for element to be visible")
                    uiDevice.wait(Until.hasObject(bySelector), request.timeout)
                }
                "gone" -> {
                    debugLogger.debug("Waiting for element to be gone")
                    uiDevice.wait(Until.gone(bySelector), request.timeout)
                }
                "clickable" -> {
                    debugLogger.debug("Waiting for element to be clickable")
                    val element = uiDevice.wait(Until.findObject(bySelector), request.timeout)
                    element?.isClickable == true
                }
                else -> {
                    debugLogger.warn("Invalid wait condition: ${request.condition}")
                    false
                }
            }
            
            debugLogger.debug("Wait operation completed with success: $success")
            ActionResponse(
                success = success,
                message = if (success) "Wait condition met" else "Wait timeout",
                elementFound = success,
                errorCode = if (success) null else ErrorCodes.TIMEOUT
            )
        } catch (e: Exception) {
            debugLogger.error("Failed to wait for element", e)
            ActionResponse(
                success = false,
                message = "Wait operation failed: ${e.message}",
                errorCode = ErrorCodes.OPERATION_FAILED
            )
        }
    }
    
    /**
     * 设备操作 - 返回键
     */
    fun pressBack(): ActionResponse {
        debugLogger.debug("Pressing back key")
        return try {
            val success = uiDevice.pressBack()
            debugLogger.debug("Back key press result: $success")
            ActionResponse(
                success = success,
                message = if (success) "Back key pressed" else "Back key press failed"
            )
        } catch (e: Exception) {
            debugLogger.error("Back key operation failed", e)
            ActionResponse(
                success = false,
                message = "Back key operation failed: ${e.message}",
                errorCode = ErrorCodes.OPERATION_FAILED
            )
        }
    }
    
    /**
     * 设备操作 - Home键
     */
    fun pressHome(): ActionResponse {
        debugLogger.debug("Pressing home key")
        return try {
            val success = uiDevice.pressHome()
            debugLogger.debug("Home key press result: $success")
            ActionResponse(
                success = success,
                message = if (success) "Home key pressed" else "Home key press failed"
            )
        } catch (e: Exception) {
            debugLogger.error("Home key operation failed", e)
            ActionResponse(
                success = false,
                message = "Home key operation failed: ${e.message}",
                errorCode = ErrorCodes.OPERATION_FAILED
            )
        }
    }
    
    /**
     * 设备操作 - 最近任务键
     */
    fun pressRecentApps(): ActionResponse {
        debugLogger.debug("Pressing recent apps key")
        return try {
            val success = uiDevice.pressRecentApps()
            debugLogger.debug("Recent apps key press result: $success")
            ActionResponse(
                success = success,
                message = if (success) "Recent apps key pressed" else "Recent apps key press failed"
            )
        } catch (e: Exception) {
            debugLogger.error("Recent apps key operation failed", e)
            ActionResponse(
                success = false,
                message = "Recent apps key operation failed: ${e.message}",
                errorCode = ErrorCodes.OPERATION_FAILED
            )
        }
    }
    
    /**
     * 获取设备信息
     */
    fun getDeviceInfo(): DeviceInfo {
        debugLogger.debug("Getting device info")
        val deviceInfo = DeviceInfo(
            screenSize = ScreenSize(uiDevice.displayWidth, uiDevice.displayHeight),
            apiLevel = Build.VERSION.SDK_INT,
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            version = Build.VERSION.RELEASE
        )
        debugLogger.debug("Device info: ${deviceInfo.manufacturer} ${deviceInfo.model} (API ${deviceInfo.apiLevel})")
        return deviceInfo
    }
    
    /**
     * 根据选择器查找元素
     */
    private fun findElement(selector: ElementSelector): UiObject2? {
        return try {
            debugLogger.debug("Finding element with selector: ${gson.toJson(selector)}")
            val bySelector = createBySelector(selector)
            val element = uiDevice.findObject(bySelector)
            if (element != null) {
                debugLogger.debug("Element found successfully")
            } else {
                debugLogger.debug("Element not found")
            }
            element
        } catch (e: Exception) {
            debugLogger.error("Failed to find element with selector: ${gson.toJson(selector)}", e)
            null
        }
    }
    
    /**
     * 创建 BySelector
     */
    private fun createBySelector(selector: ElementSelector): androidx.test.uiautomator.BySelector {
        debugLogger.debug("Creating BySelector from: ${gson.toJson(selector)}")
        var bySelector = By.clazz(".*") // 默认匹配所有类
        
        selector.resourceId?.let {
            debugLogger.debug("Adding resource ID: $it")
            bySelector = bySelector.res(it)
        }
        
        selector.text?.let {
            debugLogger.debug("Adding text: $it")
            bySelector = bySelector.text(it)
        }
        
        selector.textContains?.let {
            debugLogger.debug("Adding text contains: $it")
            bySelector = bySelector.textContains(it)
        }
        
        selector.className?.let {
            debugLogger.debug("Adding class name: $it")
            bySelector = bySelector.clazz(it)
        }
        
        selector.contentDesc?.let {
            debugLogger.debug("Adding content description: $it")
            bySelector = bySelector.desc(it)
        }
        
        return bySelector
    }
    
    /**
     * 解析UI元素（递归）
     */
    private fun parseUIElement(uiObject: Any?): UIElement {
        // 这里需要根据实际的UI Automator API来实现
        // 由于UI Automator的API复杂性，这是一个简化版本
        return try {
            if (uiObject is UiObject2) {
                val bounds = uiObject.visibleBounds
                val element = UIElement(
                    resourceId = uiObject.resourceName ?: "",
                    text = uiObject.text ?: "",
                    className = uiObject.className ?: "",
                    contentDesc = uiObject.contentDescription ?: "",
                    bounds = Bounds(bounds.left, bounds.top, bounds.right, bounds.bottom),
                    clickable = uiObject.isClickable,
                    scrollable = uiObject.isScrollable,
                    checkable = uiObject.isCheckable,
                    checked = uiObject.isChecked,
                    enabled = uiObject.isEnabled,
                    focused = uiObject.isFocused,
                    children = uiObject.children.map { parseUIElement(it) }
                )
                debugLogger.debug("Parsed UI element: ${element.className} (${element.children.size} children)")
                element
            } else {
                debugLogger.debug("Unknown UI object type: ${uiObject?.javaClass?.simpleName}")
                UIElement(className = "unknown")
            }
        } catch (e: Exception) {
            debugLogger.error("Failed to parse UI element", e)
            UIElement(className = "error")
        }
    }
}
