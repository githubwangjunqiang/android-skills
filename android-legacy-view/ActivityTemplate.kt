/**
 * Activity 启动模板
 * ⚠️ 包名需替换为项目实际包名（com.xxx.app → 实际包名）
 * ⚠️ BaseVMActivity 需替换为项目实际基类
 *
 * 推荐在 companion object 中提供 startActivity() 方法，统一管理参数和启动逻辑
 */

package com.xxx.app.base.activity

import android.content.Context
import android.content.Intent
import com.xxx.app.base.baseui.BaseVMActivity
import com.xxx.app.ui.vm.SettingVm

/**
 * Activity 启动模板 - 基础版（无参数）
 */
class SettingActivity : BaseVMActivity<SettingVm>() {

    companion object {
        /**
         * 启动此界面
         */
        fun startActivity(context: Context) {
            context.startActivity(Intent(context, SettingActivity::class.java).apply {
                // 默认模板：按页面场景决定是否保留这些 Flags
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            })
        }
    }
}

/**
 * Activity 启动模板 - 带参数版
 */
class UserDetailActivity : BaseVMActivity<UserDetailVm>() {

    companion object {
        private const val EXTRA_USER_ID = "extra_user_id"
        private const val EXTRA_USER_NAME = "extra_user_name"

        /**
         * 启动此界面
         * @param userId 用户ID
         * @param userName 用户名
         */
        fun startActivity(context: Context, userId: String, userName: String) {
            context.startActivity(Intent(context, UserDetailActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(EXTRA_USER_ID, userId)
                putExtra(EXTRA_USER_NAME, userName)
            })
        }
    }

    /** 在 onCreate 或 initView 中获取参数 */
    private fun getIntentData() {
        val userId = intent.getStringExtra(EXTRA_USER_ID)
        val userName = intent.getStringExtra(EXTRA_USER_NAME)
        // 使用参数...
    }
}

/**
 * Activity 启动模板 - 带可选参数版
 */
class EditActivity : BaseVMActivity<EditVm>() {

    companion object {
        private const val EXTRA_ITEM_ID = "extra_item_id"
        private const val EXTRA_MODE = "extra_mode"

        /** 启动此界面（新建模式） */
        fun startActivity(context: Context) {
            startActivity(context, null, "create")
        }

        /**
         * 启动此界面（编辑模式）
         * @param itemId 要编辑的项目ID
         */
        fun startActivity(context: Context, itemId: String?) {
            startActivity(context, itemId, "edit")
        }

        /**
         * 启动此界面（完整参数）
         */
        fun startActivity(context: Context, itemId: String?, mode: String) {
            context.startActivity(Intent(context, EditActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                itemId?.let { putExtra(EXTRA_ITEM_ID, it) }
                putExtra(EXTRA_MODE, mode)
            })
        }
    }
}

// ==================== 使用规范 ====================

/**
 * 1. 新建 Activity 时推荐同步创建 startActivity() 方法
 * 2. 启动 Activity 时优先使用此方法，而非直接 context.startActivity(Intent(...))
 * 3. Intent Flags 为默认模板，具体按页面场景裁剪
 * 4. 参数扩展：通过添加方法参数和 putExtra 实现数据传递
 * 5. 参数名常量：使用 private const val EXTRA_XXX 定义
 */
