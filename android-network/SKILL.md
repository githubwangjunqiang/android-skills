---
name: android-network
description: 网络请求架构。OkHttp 扩展函数、请求规范、响应处理、全局错误拦截。
---

# OkHttp 网络请求架构规范

**本 skill 描述的是项目内自定义 OkHttp 调用链。**  
若项目已有统一网络封装，应优先兼容项目现状；以下示例默认基于当前 skills 的 MVI 体系。

---

## 一、核心架构概览

```
┌─────────────────────────────────────────────────────────────┐
│  ViewModel                                                  │
│  ├─ launchTryViewModelScope {                               │
│  │    HttpCallPool.post_login.postCall(Data::class.java) {  │
│  │        put("key", value)   // 构建 JSON body             │
│  │    }                                                      │
│  │    .loadSuccess { data -> showContent { ... } }          │
│  │    .loadErrorForMsg { msg -> showError(msg) }            │
│  │  }                                                        │
└─────────────────────────────────────────────────────────────┘
          │
          ▼
┌─────────────────────────────────────────────────────────────┐
│  BaseHttpManager (String.postCall/getCall/deleteCall)       │
│  ├─ 自动切换到 IO 线程                                       │
│  ├─ 自动注入基础参数                                         │
│  ├─ 自动注入请求头                                           │
│  ├─ 执行请求 → OkhttpCallDefinite                            │
│  ├─ 解析响应 → Gson                                          │
│  ├─ 异常处理 → OkhttpException                               │
│  └─ 全局拦截 → HttpErrorManager                              │
└─────────────────────────────────────────────────────────────┘
```

---

## 二、请求方式速查

| 方法 | 用途 | 返回类型 |
|------|------|----------|
| `String.postCall(clazz)` | POST JSON 请求 | `BaseCallReturnData<T>` |
| `String.getCall(clazz)` | GET 请求 | `BaseCallReturnData<T>` |
| `String.deleteCall(clazz)` | DELETE 请求 | `BaseCallReturnData<T>` |
| `String.postCall(headers, body)` | 简单 POST（无解析） | `Triple<Boolean, String, Exception?>` |
| `String.getCall(headers, params)` | 简单 GET（无解析） | `Triple<Boolean, String, Exception?>` |

---

## 三、完整请求示例

### 3.1 POST 请求（最常用）

```kotlin
// ⚠️ 包名需替换为项目实际包名
import com.xxx.app.base.baseui.MviBaseViewModel
import com.xxx.app.base.http.manager.BaseHttpManager.postCall
import com.xxx.app.manager.https.HttpCallPool

/** 登录页 Intent */
sealed interface LoginIntent {
    /** 提交登录 */
    data class Submit(val phone: String, val password: String) : LoginIntent
}

/** 登录页数据 */
data class LoginData(
    val phone: String = "",
    val token: String = ""
)

/** 登录页一次性事件 */
sealed interface LoginEffect {
    /** 登录成功后跳转首页 */
    data object NavigateHome : LoginEffect
}

class LoginViewModel : MviBaseViewModel<LoginIntent, LoginData, LoginEffect>(LoginData()) {

    override fun handleIntent(intent: LoginIntent) {
        when (intent) {
            is LoginIntent.Submit -> login(intent.phone, intent.password)
        }
    }

    private fun login(phone: String, password: String) {
        launchTryViewModelScope {
            showDialogLoading("正在登录...")

            HttpCallPool.post_login.postCall(LoginResponse::class.java) {
                // lambda 内构建 JSON body
                put("account", phone)
                put("password", password)
                put("type", "ACCOUNT")
            }.apply {
                loadSuccess { response ->
                    showContent { old -> old.copy(phone = phone, token = response.data.token) }
                    sendEffect(LoginEffect.NavigateHome)
                }
                loadErrorForMsg { errorMsg ->
                    showError(errorMsg)
                }
            }
        }
    }
}
```

### 3.2 GET 请求

```kotlin
import com.xxx.app.base.http.manager.BaseHttpManager.getCall

private fun fetchUserInfo(userId: String) {
    launchTryViewModelScope {
        showFullScreenLoading("正在加载用户信息...")

        HttpCallPool.get_user_info.getCall(UserInfoResponse::class.java) {
            put("userId", userId)
        }.apply {
            loadSuccess { response ->
                showContent { old -> old.copy(userInfo = response.data) }
            }
            loadErrorForMsg { errorMsg ->
                showError(errorMsg)
            }
        }
    }
}
```

### 3.3 DELETE 请求

```kotlin
import com.xxx.app.base.http.manager.BaseHttpManager.deleteCall

private fun deleteUser(userId: String) {
    launchTryViewModelScope {
        HttpCallPool.delete_user.deleteCall(BaseResponse::class.java) {
            put("userId", userId)
        }.apply {
            loadSuccess {
                showContent { old -> old.copy(deleteSuccess = true) }
            }
            loadErrorForMsg { errorMsg ->
                showError(errorMsg)
            }
        }
    }
}
```

---

## 四、响应处理链式调用

`BaseCallReturnData<T>` 提供三种链式处理方法：

| 方法 | 说明 | 触发条件 |
|------|------|----------|
| `loadSuccess { data -> }` | 成功回调 | `success == true` 且 `resultData != null` |
| `loadError { e -> }` | 异常回调 | `success == false`（接收 Exception） |
| `loadErrorForMsg { msg -> }` | 错误文案回调 | `success == false`（直接显示错误消息） |

### 4.1 推荐用法

```kotlin
result.loadSuccess { response ->
    // 成功处理，response.data 已解析
    showContent { old -> old.copy(userInfo = response.data) }
}

result.loadErrorForMsg { errorMsg ->
    // 直接显示错误消息（已包含服务器返回的 message）
    showError(errorMsg)
}
```

### 4.2 获取额外信息

```kotlin
result.getErrorMsg()      // 获取错误文案
result.getErrorCode()     // 获取服务端返回的 code
result.isServiceError()   // 是否服务端返回的异常（而非网络异常）
```

---

## 五、URL 常量池定义

在 `HttpCallPool` 中集中定义所有 API 地址：

```kotlin
// ⚠️ 包名需替换为项目实际包名
package com.xxx.app.manager.https

object HttpCallPool {
    // 后端返回值常量
    const val HTTP_RETURN_CODE = "code"
    const val HTTP_RETURN_CODE_SUCCESS = 200
    const val HTTP_RETURN_MSG = "message"

    // 全局错误码
    const val HTTP_CODE_401 = 401       // 需要登录
    const val HTTP_CODE_10039 = 10039   // 被踢下线

    // 基础域名（从 BuildConfig 获取）
    var baseUrl = BuildConfig.BaseUrl

    /** 用户登录 */
    val post_login: String
        get() = "$baseUrl/login/third"

    /** 用户信息 */
    val get_user_info: String
        get() = "$baseUrl/queryMyself"

    /** 删除用户 */
    val delete_user: String
        get() = "$baseUrl/user/delete"

    /** 用户列表 */
    val get_user_list: String
        get() = "$baseUrl/user/list"

    /** 首页列表 */
    val get_home_list: String
        get() = "$baseUrl/home/list"

    /** 动态参数 URL */
    fun getRoomInfo(roomId: String): String {
        return "$baseUrl/call/roomInfo/$roomId"
    }
}
```

---

## 六、全局错误处理

### 6.1 初始化（Application）

```kotlin
class App : Application() {
    override fun onCreate() {
        super.onCreate()

        BaseHttpManager.initManager(
            loadHeaderParameter = { _, _, _ ->
                JSONObject().apply {
                    put("Authorization", "Bearer ${loadToken()}")
                }
            },
            loadBasicParameters = {
                JSONObject().apply {
                    put("deviceId", loadDeviceId())
                    put("version", BuildConfig.VERSION_NAME)
                }
            },
            mErrorListener = { e ->
                // 全局错误拦截（401、被踢等）
                HttpErrorManager.handleError(e)
            }
        )

        HttpErrorManager.initContext()
    }
}
```

### 6.2 HttpErrorManager 功能

| 功能 | 说明 |
|------|------|
| 401 拦截 | 自动跳转登录页 |
| 被踢下线（10039） | 强制登出 + Toast 提示 |
| Flow 防抖 | `sample(2000)` 防止重复弹窗 |

---

## 七、响应模型规范

### 7.1 标准响应格式

后端返回格式约定：
```json
{
    "code": 200,
    "message": "success",
    "data": { }
}
```

### 7.2 响应类定义模板

```kotlin
/**
 * 用户信息响应
 * ⚠️ 包名需替换为项目实际包名
 */
package com.xxx.app.model.response

data class UserInfoResponse(
    val code: Int = 0,
    val message: String = "",
    val data: UserData? = null
) {
    val isSuccess: Boolean get() = code == 200
}

data class UserData(
    val userId: String = "",
    val username: String = "",
    val avatar: String = "",
    val token: String = ""
)
```

---

## 八、异常类型分类

`OkhttpException.ErrorType` 定义了 6 种异常：

| 类型 | 说明 | 处理建议 |
|------|------|----------|
| `errorCreateBody` | 构建 body 失败 | 检查参数序列化 |
| `errorHttpNoSuccessful` | HTTP 状态码非 2xx | 检查服务器状态 |
| `errorOkhttpExecute` | 网络执行异常（无网络、超时等） | 提示网络异常 |
| `errorServiceCode` | 服务端 code 非 200 | 显示服务端 message |
| `errorServiceCodeParse` | code 解析异常 | 检查响应格式 |
| `errorParse` | Gson 解析异常 | 检查实体类定义 |

---

## 九、OkHttp 配置

```kotlin
// OkhttpClientProvider 提供全局单例
OkhttpClientProvider.okhttpManager

// 超时配置：
// connectTimeout = 30s
// readTimeout = 60s
```

---

## 十、注意事项

- 本 skill 假定项目已经有统一的 OkHttp 封装；不要在业务层再次重复造网络基础设施
- 若项目仍存在旧版 `BaseViewModel` / `showErrorLayout()` 写法，应优先向当前 MVI 基准靠拢
- 用户可见错误文案应最终资源化，文档中的硬编码仅用于示例说明
