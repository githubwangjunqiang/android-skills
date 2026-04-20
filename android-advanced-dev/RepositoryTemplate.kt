/**
 * Repository 数据仓库模板
 * ⚠️ 包名需替换为项目实际包名（com.xxx.app → 实际包名）
 * 负责统一管理数据的获取、缓存、持久化
 * ViewModel 不直接调用网络层，通过 Repository 获取数据
 *
 * 依赖的基础设施（需从其他 skill 引入）：
 * - HttpCallPool / getCall → 项目自定义网络层封装（参见 android-network）
 * - MMKVUtil → MMKV 存储封装（参见 android-local-storage）
 * - MviBaseViewModel / MviUiState / MviPageStatus → 页面状态管理（参见 android-mvi-compose）
 * - launchTryViewModelScope → ViewModel 协程安全启动（参见 android-utils-core/data-flow-tools.md）
 */

package com.xxx.app.feature.user

import com.xxx.app.base.baseui.MviBaseViewModel
import com.xxx.app.base.http.HttpCallPool
import com.xxx.app.base.storage.mmkv.MMKVUtil
import com.xxx.app.base.utils.toputils.show
import com.xxx.app.model.entity.UserInfoData
import com.xxx.app.model.response.UserInfoResponse
import com.xxx.app.model.response.UserListResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONObject

/**
 * 用户信息数据仓库
 * 负责统一管理用户数据的获取、缓存、持久化
 */
class UserRepository {

    /**
     * 获取用户信息
     * 策略：优先网络，失败降级 MMKV 缓存
     * @param userId 用户ID
     * @return 用户信息结果
     */
    suspend fun getUserInfo(userId: String): Result<UserInfoData> {
        return try {
            val response = HttpCallPool.get_user_info.getCall(
                UserInfoResponse::class.java,
                json = JSONObject().apply { put("user_id", userId) }
            )
            if (response.success && response.resultData?.data != null) {
                val data = response.resultData!!.data!!
                // 网络成功后同步刷新本地缓存
                MMKVUtil.put("user_$userId", data)
                Result.success(data)
            } else {
                // 网络失败时尝试回退到本地缓存
                val cached = MMKVUtil.getObject("user_$userId", UserInfoData::class.java)
                if (cached != null) Result.success(cached)
                else Result.failure(Exception(response.getErrorMsg()))
            }
        } catch (e: Exception) {
            val cached = MMKVUtil.getObject("user_$userId", UserInfoData::class.java)
            if (cached != null) Result.success(cached)
            else Result.failure(e)
        }
    }

    /**
     * 获取用户列表（分页）
     */
    suspend fun getUserList(pageNo: Int, pageSize: Int): Result<List<UserInfoData>> {
        return try {
            val response = HttpCallPool.get_user_list.getCall(
                UserListResponse::class.java,
                json = JSONObject().apply {
                    put("pageNo", pageNo)
                    put("pageSize", pageSize)
                }
            )
            if (response.success) {
                Result.success(response.resultData?.data?.list.orEmpty())
            } else {
                Result.failure(Exception(response.getErrorMsg()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// ==================== ViewModel 中使用示例 ====================

/** 用户页面 Intent 定义 */
sealed interface UserIntent {
    /** 加载用户信息 */
    data class LoadUser(val userId: String) : UserIntent
}

/** 用户页面数据模型 */
data class UserUiData(
    val userInfo: UserInfoData? = null
)

/** 用户页面一次性事件 */
sealed interface UserEffect

/**
 * 使用 Repository 的 ViewModel 示例
 */
class UserViewModel(
    private val repository: UserRepository = UserRepository()
) : MviBaseViewModel<UserIntent, UserUiData, UserEffect>(UserUiData()) {

    /** 额外暴露一个只读 StateFlow 仅用于演示业务数据派生 */
    private val _selectedUserId = MutableStateFlow<String?>(null)
    val selectedUserId: StateFlow<String?> = _selectedUserId.asStateFlow()

    /** 统一的 Intent 入口 */
    override fun handleIntent(intent: UserIntent) {
        when (intent) {
            is UserIntent.LoadUser -> loadUserInfo(intent.userId)
        }
    }

    /**
     * 加载用户信息
     * @param userId 用户ID
     */
    private fun loadUserInfo(userId: String) {
        launchTryViewModelScope {
            _selectedUserId.update { userId }
            showFullScreenLoading("正在加载用户信息...")
            repository.getUserInfo(userId)
                .onSuccess { data ->
                    showContent { old ->
                        old.copy(userInfo = data)
                    }
                }
                .onFailure { e ->
                    e.message?.show()
                    showError(e.message ?: "加载失败")
                }
        }
    }
}
