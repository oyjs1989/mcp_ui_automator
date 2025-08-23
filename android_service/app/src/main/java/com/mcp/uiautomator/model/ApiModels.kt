package com.mcp.uiautomator.model

import com.google.gson.annotations.SerializedName

/**
 * 元素选择器
 * 支持多种定位方式
 */
data class ElementSelector(
    @SerializedName("resource_id")
    val resourceId: String? = null,
    
    @SerializedName("text")
    val text: String? = null,
    
    @SerializedName("text_contains")
    val textContains: String? = null,
    
    @SerializedName("class_name")
    val className: String? = null,
    
    @SerializedName("content_desc")
    val contentDesc: String? = null,
    
    @SerializedName("index")
    val index: Int? = null,
    
    @SerializedName("bounds")
    val bounds: Bounds? = null
) {
    /**
     * 检查选择器是否有效（至少包含一个定位条件）
     */
    fun isValid(): Boolean {
        return resourceId != null || 
               text != null || 
               textContains != null ||
               className != null ||
               contentDesc != null ||
               index != null ||
               bounds != null
    }
}

/**
 * 点击请求
 */
data class ClickRequest(
    @SerializedName("selector")
    val selector: ElementSelector
)

/**
 * 输入文本请求
 */
data class InputRequest(
    @SerializedName("selector")
    val selector: ElementSelector,
    
    @SerializedName("text")
    val text: String,
    
    @SerializedName("clear_first")
    val clearFirst: Boolean = true
)

/**
 * 滚动请求
 */
data class ScrollRequest(
    @SerializedName("direction")
    val direction: String, // up, down, left, right
    
    @SerializedName("steps")
    val steps: Int = 1,
    
    @SerializedName("selector")
    val selector: ElementSelector? = null // 可选，指定滚动的容器
)

/**
 * 等待请求
 */
data class WaitRequest(
    @SerializedName("selector")
    val selector: ElementSelector,
    
    @SerializedName("timeout")
    val timeout: Long = 5000, // 毫秒
    
    @SerializedName("condition")
    val condition: String = "visible" // visible, gone, clickable
)

/**
 * 操作响应
 */
data class ActionResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("message")
    val message: String = "",
    
    @SerializedName("timestamp")
    val timestamp: Long = System.currentTimeMillis(),
    
    @SerializedName("element_found")
    val elementFound: Boolean = false,
    
    @SerializedName("error_code")
    val errorCode: String? = null
)

/**
 * 设备信息
 */
data class DeviceInfo(
    @SerializedName("screen_size")
    val screenSize: ScreenSize,
    
    @SerializedName("api_level")
    val apiLevel: Int,
    
    @SerializedName("manufacturer")
    val manufacturer: String,
    
    @SerializedName("model")
    val model: String,
    
    @SerializedName("version")
    val version: String
)

data class ScreenSize(
    @SerializedName("width")
    val width: Int,
    
    @SerializedName("height")
    val height: Int
)

/**
 * 错误代码枚举
 */
object ErrorCodes {
    const val ELEMENT_NOT_FOUND = "ELEMENT_NOT_FOUND"
    const val TIMEOUT = "TIMEOUT"
    const val INVALID_SELECTOR = "INVALID_SELECTOR"
    const val OPERATION_FAILED = "OPERATION_FAILED"
    const val INVALID_DIRECTION = "INVALID_DIRECTION"
    const val SERVICE_ERROR = "SERVICE_ERROR"
}
