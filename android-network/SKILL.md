---
name: android-network
description: 网络请求架构。OkHttp 扩展函数、请求规范、响应处理、全局错误拦截。
---

# OkHttp 网络请求架构规范

**禁止使用 Retrofit**，使用项目封装的 OkHttp 扩展函数。

---

## 一、核心架构概览

```
┌─────────────────────────────────────────────────────────────┐
│  ViewModel                                                  │
│  ├─ launchTryViewModelScope {                              │
│  │    HttpCallPool.xxx_url.postCall(Data::class.java) {    │  ← String 扩展函数
│  │        put("key", value)  // 构建 JSON body             │
│  │    }                                                     │
│  │    .loadSuccess { data -> showContent() }                │  ← 链式处理
│  │    .loadErrorForMsg { msg -> showError(msg) }            │
│  │  }                                                       │
└─────────────────────────────────────────────────────────────┘
          │
          ▼
┌─────────────────────────────────────────────────────────────┐
│  BaseHttpManager (String.postCall/getCall/deleteCall)       │
│  ├─ 自动切换到 IO 线程                                       │
│  ├─ 自动注入基础参数                                         │
│  ├─ 自动注入请求头                                          │
│  ├─ 执行请求 → OkhttpCallDefinite                           │
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
import com.xxx.app.base.http.manager.BaseHttpManager.postCall
import com.xxx.app.base.http.result.BaseCallReturnData
import com.xxx.app.manager.https.HttpCallPool

class UserViewModel : BaseViewModel() {

    fun login(phone: String, password: String) {
        launchTryViewModelScope {
            showLoadingDialog()
            
            HttpCallPool.post_login_url.postCall(LoginResponse::class.java) {
                // lambda 内构建 JSON body
                put("account", phone)
                put("password", password)
                put("type", "ACCOUNT")
            }.apply {
                loadSuccess { response ->
                    showContent()
                    // response.data 是解析后的对象
                    handleLoginSuccess(response.data)
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

fun fetchUserInfo(userId: String) {
    launchTryViewModelScope {
        showLoadingDialog()
        
        HttpCallPool.get_user_info.getCall(UserInfoResponse::class.java) {
            put("userId", userId)  // 添加到 URL query 参数
        }.apply {
            loadSuccess { response ->
                showContent()
                updateUserInfo(response.data)
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

fun deleteUser(userId: String) {
    launchTryViewModelScope {
        HttpCallPool.delete_user_url.deleteCall(BaseResponse::class.java) {
            put("userId", userId)
        }.apply {
            loadSuccess { showContent() }
            loadErrorForMsg { errorMsg -> errorMsg.show() }
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
    updateUI(response.data)
}

result.loadErrorForMsg { errorMsg ->
    // 直接显示错误消息（已包含服务器返回的 message）
    errorMsg.show()
    showErrorLayout(errorMsg)
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
    
    // ==================== API 定义 ====================
    
    /** 用户登录 */
    val post_login: String
        get() = "$baseUrl/login/third"
    
    /** 用户信息 */
    val get_user_info: String
        get() = "$baseUrl/queryMyself"
    
    /** 首页列表 */
    val get_home_list: String
        get() = "$baseUrl/home/list"
    
    /** 动态参数 URL */
    fun get_roomInfo(roomId: String): String {
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
        
        // 初始化网络管理器
        BaseHttpManager.initManager(
            loadHeaderParameter = { url, body, way -> 
                // 自定义请求头（如 token）
                JSONObject().apply {
                    put("Authorization", "Bearer ${loadToken()}")
                }
            },
            loadBasicParameters = { url ->
                // 基础入参（如设备信息）
                JSONObject().apply {
                    put("deviceId", loadDeviceId())
                    put("version", BuildConfig.VERSION_NAME)
                }
            },
            mErrorListener = { e ->
                // 全局错误拦截（401、被踢等）
                HttpErrorManager.handError(e)
            }
        )
        
        // 启动全局错误监听
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
    "data": { ... }
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
// writeTimeout = 60s
// retryOnConnectionFailure = true
```

---

## 十、拦截器扩展

添加请求结果拦截器（用于日志、监控）：

```kotlin
BaseHttpManager.addResultInterceptor(object : IResultInterceptor {
    override suspend fun intercept(
        url: String,
        startTime: Long,
        callHeader: String?,
        callParams: String?,
        returnValue: String?,
        error: Exception?
    ) {
        // 记录请求日志
        Log.d("HTTP", "url=$url, time=${System.currentTimeMillis() - startTime}ms")
    }
})
```

---

## 十一、注意事项

| 事项 | 说明 |
|------|------|
| **禁止 Retrofit** | 项目已有 OkHttp 封装，Retrofit 注解增加学习成本 |
| **请求在 IO 线程** | 扩展函数自动切换 `Dispatchers.IO` |
| **协程取消安全** | 请求中协程取消会抛 `CancellationException`，`loadError` 会跳过 |
| **全局拦截优先** | 401/被踢由全局处理，业务层 `loadError` 不会触发 |
| **服务端错误拦截** | 调用 `interceptServiceError()` 后 `loadError` 不触发服务端错误 |