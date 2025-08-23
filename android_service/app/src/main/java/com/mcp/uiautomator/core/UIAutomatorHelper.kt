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
    
    /**
     * 获取当前屏幕的UI树
     */
    fun getPageSource(): PageSource {
        return try {
            val rootNode = uiDevice.findObject(UiSelector())
            val rootElement = parseUIElement(rootNode)
            
            val currentPackage = uiDevice.currentPackageName ?: ""
            val screenBounds = Bounds(0, 0, uiDevice.displayWidth, uiDevice.displayHeight)
            
            PageSource(
                root = rootElement,
                timestamp = System.currentTimeMillis(),
                packageName = currentPackage,
                screenSize = screenBounds
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get page source", e)
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
        if (!selector.isValid()) {
            return ActionResponse(
                success = false,
                message = "Invalid selector",
                errorCode = ErrorCodes.INVALID_SELECTOR
            )
        }
        
        return try {
            val element = findElement(selector)
            if (element != null) {
                val success = element.click()
                ActionResponse(
                    success = success,
                    message = if (success) "Element clicked successfully" else "Click failed",
                    elementFound = true
                )
            } else {
                ActionResponse(
                    success = false,
                    message = "Element not found",
                    elementFound = false,
                    errorCode = ErrorCodes.ELEMENT_NOT_FOUND
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to click element", e)
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
        if (!request.selector.isValid()) {
            return ActionResponse(
                success = false,
                message = "Invalid selector",
                errorCode = ErrorCodes.INVALID_SELECTOR
            )
        }
        
        return try {
            val element = findElement(request.selector)
            if (element != null) {
                if (request.clearFirst) {
                    element.clear()
                }
                val success = element.setText(request.text)
                ActionResponse(
                    success = success,
                    message = if (success) "Text input successfully" else "Text input failed",
                    elementFound = true
                )
            } else {
                ActionResponse(
                    success = false,
                    message = "Element not found",
                    elementFound = false,
                    errorCode = ErrorCodes.ELEMENT_NOT_FOUND
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to input text", e)
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
        val direction = when (request.direction.lowercase()) {
            "up" -> Direction.UP
            "down" -> Direction.DOWN
            "left" -> Direction.LEFT
            "right" -> Direction.RIGHT
            else -> {
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
                val element = findElement(request.selector)
                if (element != null) {
                    element.scroll(direction, 1.0f, request.steps)
                } else {
                    return ActionResponse(
                        success = false,
                        message = "Scroll container not found",
                        elementFound = false,
                        errorCode = ErrorCodes.ELEMENT_NOT_FOUND
                    )
                }
            } else {
                // 滚动整个屏幕
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
            
            ActionResponse(
                success = success,
                message = if (success) "Scroll completed" else "Scroll failed"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to scroll", e)
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
        if (!request.selector.isValid()) {
            return ActionResponse(
                success = false,
                message = "Invalid selector",
                errorCode = ErrorCodes.INVALID_SELECTOR
            )
        }
        
        return try {
            val bySelector = createBySelector(request.selector)
            val success = when (request.condition.lowercase()) {
                "visible" -> uiDevice.wait(Until.hasObject(bySelector), request.timeout)
                "gone" -> uiDevice.wait(Until.gone(bySelector), request.timeout)
                "clickable" -> {
                    val element = uiDevice.wait(Until.findObject(bySelector), request.timeout)
                    element?.isClickable == true
                }
                else -> false
            }
            
            ActionResponse(
                success = success,
                message = if (success) "Wait condition met" else "Wait timeout",
                elementFound = success,
                errorCode = if (success) null else ErrorCodes.TIMEOUT
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to wait for element", e)
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
        return try {
            val success = uiDevice.pressBack()
            ActionResponse(
                success = success,
                message = if (success) "Back key pressed" else "Back key press failed"
            )
        } catch (e: Exception) {
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
        return try {
            val success = uiDevice.pressHome()
            ActionResponse(
                success = success,
                message = if (success) "Home key pressed" else "Home key press failed"
            )
        } catch (e: Exception) {
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
        return try {
            val success = uiDevice.pressRecentApps()
            ActionResponse(
                success = success,
                message = if (success) "Recent apps key pressed" else "Recent apps key press failed"
            )
        } catch (e: Exception) {
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
        return DeviceInfo(
            screenSize = ScreenSize(uiDevice.displayWidth, uiDevice.displayHeight),
            apiLevel = Build.VERSION.SDK_INT,
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            version = Build.VERSION.RELEASE
        )
    }
    
    /**
     * 根据选择器查找元素
     */
    private fun findElement(selector: ElementSelector): UiObject2? {
        return try {
            val bySelector = createBySelector(selector)
            uiDevice.findObject(bySelector)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to find element with selector: $selector", e)
            null
        }
    }
    
    /**
     * 创建 BySelector
     */
    private fun createBySelector(selector: ElementSelector): androidx.test.uiautomator.BySelector {
        var bySelector = By.clazz(".*") // 默认匹配所有类
        
        selector.resourceId?.let {
            bySelector = bySelector.res(it)
        }
        
        selector.text?.let {
            bySelector = bySelector.text(it)
        }
        
        selector.textContains?.let {
            bySelector = bySelector.textContains(it)
        }
        
        selector.className?.let {
            bySelector = bySelector.clazz(it)
        }
        
        selector.contentDesc?.let {
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
                UIElement(
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
            } else {
                UIElement(className = "unknown")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse UI element", e)
            UIElement(className = "error")
        }
    }
}
