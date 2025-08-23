package com.mcp.uiautomator.model

import com.google.gson.annotations.SerializedName

/**
 * UI元素数据模型
 * 对应JSON Schema中的UIElement定义
 */
data class UIElement(
    @SerializedName("resource_id")
    val resourceId: String = "",
    
    @SerializedName("text")
    val text: String = "",
    
    @SerializedName("class_name")
    val className: String = "",
    
    @SerializedName("content_desc")
    val contentDesc: String = "",
    
    @SerializedName("bounds")
    val bounds: Bounds = Bounds(),
    
    @SerializedName("clickable")
    val clickable: Boolean = false,
    
    @SerializedName("scrollable")
    val scrollable: Boolean = false,
    
    @SerializedName("checkable")
    val checkable: Boolean = false,
    
    @SerializedName("checked")
    val checked: Boolean = false,
    
    @SerializedName("enabled")
    val enabled: Boolean = true,
    
    @SerializedName("focused")
    val focused: Boolean = false,
    
    @SerializedName("children")
    val children: List<UIElement> = emptyList()
)

/**
 * 元素边界坐标
 */
data class Bounds(
    @SerializedName("left")
    val left: Int = 0,
    
    @SerializedName("top")
    val top: Int = 0,
    
    @SerializedName("right")
    val right: Int = 0,
    
    @SerializedName("bottom")
    val bottom: Int = 0
) {
    /**
     * 获取中心点坐标
     */
    fun centerX(): Int = (left + right) / 2
    fun centerY(): Int = (top + bottom) / 2
    
    /**
     * 获取宽度和高度
     */
    fun width(): Int = right - left
    fun height(): Int = bottom - top
}

/**
 * UI页面源数据
 */
data class PageSource(
    @SerializedName("root")
    val root: UIElement,
    
    @SerializedName("timestamp")
    val timestamp: Long = System.currentTimeMillis(),
    
    @SerializedName("package_name")
    val packageName: String = "",
    
    @SerializedName("activity")
    val activity: String = "",
    
    @SerializedName("screen_size")
    val screenSize: Bounds = Bounds()
)
