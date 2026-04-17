/**
 * 传统 View 状态页容器
 * 支持 4 种状态：Loading / Error / Empty / Content
 * 支持淡入淡出动画，支持点击重试
 * ⚠️ 包名需替换为项目实际包名
 * 例如：com.xxx.app → com.yourcompany.yourapp
 */
package com.xxx.app.base.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.animation.AlphaAnimation
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView

/**
 * 状态页容器（传统 View）
 *
 * 用法（XML）：
 * <StatusViewLayout android:id="@+id/statusView" ...>
 *     <!-- 正常内容放这里 -->
 *     <RecyclerView ... />
 * </StatusViewLayout>
 *
 * 用法（代码）：
 * statusView.showLoading()
 * statusView.showContent()
 * statusView.showEmpty("暂无数据") { retryLoad() }
 * statusView.showError("加载失败") { retryLoad() }
 */
class StatusViewLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    /** 状态枚举 */
    enum class Status { LOADING, CONTENT, ERROR, EMPTY }

    /** 当前状态 */
    private var currentStatus: Status = Status.CONTENT

    /** 状态视图缓存 */
    private var loadingView: View? = null
    private var errorView: View? = null
    private var emptyView: View? = null

    /** 状态监听器 */
    var statusListener: StatusListener? = null

    /** 动画时长（毫秒） */
    var animDuration: Long = 200L

    /** 显示加载中 */
    fun showLoading() {
        if (currentStatus == Status.LOADING) return
        hideAllStatusViews()
        if (loadingView == null) {
            loadingView = ProgressBar(context).apply {
                layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                    gravity = android.view.Gravity.CENTER
                }
            }
            addView(loadingView)
        }
        loadingView?.fadeIn()
        setContentVisible(false)
        currentStatus = Status.LOADING
        statusListener?.onStatusChanged(Status.LOADING)
    }

    /** 显示正常内容 */
    fun showContent() {
        if (currentStatus == Status.CONTENT) return
        hideAllStatusViews()
        setContentVisible(true)
        currentStatus = Status.CONTENT
        statusListener?.onStatusChanged(Status.CONTENT)
    }

    /**
     * 显示错误状态
     * @param msg 错误提示文案
     * @param onRetry 点击重试回调
     */
    fun showError(msg: String? = "加载失败，点击重试", onRetry: (() -> Unit)? = null) {
        if (currentStatus == Status.ERROR) return
        hideAllStatusViews()
        if (errorView == null) {
            errorView = createStatusView(msg ?: "加载失败，点击重试")
            addView(errorView)
        } else {
            errorView?.findViewById<TextView>(android.R.id.text1)?.text = msg
        }
        onRetry?.let { retry -> errorView?.setOnClickListener { retry() } }
        errorView?.fadeIn()
        setContentVisible(false)
        currentStatus = Status.ERROR
        statusListener?.onStatusChanged(Status.ERROR)
    }

    /**
     * 显示空数据状态
     * @param msg 空状态提示文案
     * @param onRetry 点击重试回调
     */
    fun showEmpty(msg: String? = "暂无数据", onRetry: (() -> Unit)? = null) {
        if (currentStatus == Status.EMPTY) return
        hideAllStatusViews()
        if (emptyView == null) {
            emptyView = createStatusView(msg ?: "暂无数据")
            addView(emptyView)
        } else {
            emptyView?.findViewById<TextView>(android.R.id.text1)?.text = msg
        }
        onRetry?.let { retry -> emptyView?.setOnClickListener { retry() } }
        emptyView?.fadeIn()
        setContentVisible(false)
        currentStatus = Status.EMPTY
        statusListener?.onStatusChanged(Status.EMPTY)
    }

    /** 创建状态提示视图（居中文字） */
    private fun createStatusView(msg: String): View {
        return TextView(context).apply {
            text = msg
            gravity = android.view.Gravity.CENTER
            textSize = 14f
            setTextColor(0xFF999999.toInt())
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            setPadding(32, 32, 32, 32)
        }
    }

    /** 隐藏所有状态视图 */
    private fun hideAllStatusViews() {
        loadingView?.fadeOut()
        errorView?.fadeOut()
        emptyView?.fadeOut()
    }

    /** 设置原始内容子 View 的可见性 */
    private fun setContentVisible(visible: Boolean) {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child != loadingView && child != errorView && child != emptyView) {
                child.visibility = if (visible) View.VISIBLE else View.GONE
            }
        }
    }

    /** 淡入动画 */
    private fun View.fadeIn() {
        this.visibility = View.VISIBLE
        this.startAnimation(AlphaAnimation(0f, 1f).apply { duration = animDuration })
    }

    /** 淡出动画 */
    private fun View.fadeOut() {
        if (this.visibility != View.VISIBLE) return
        this.startAnimation(AlphaAnimation(1f, 0f).apply { duration = animDuration })
        this.visibility = View.GONE
    }

    /** 状态变化监听器 */
    interface StatusListener {
        fun onStatusChanged(status: Status)
    }
}