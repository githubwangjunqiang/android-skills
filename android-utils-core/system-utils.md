# 其他工具函数

本文件是 android-utils-core 技能的附属参考文件，包含项目特有的工具函数。

---

## 一、tryResumeYourself 引用说明

**定义位置**：[data-flow-tools.md](data-flow-tools.md) 第 183 行

用于将第三方 SDK 回调转为协程挂起函数，安全恢复挂起协程：

```kotlin
// 用法示例
suspend fun getUserInfo(userId: String): UserInfo? = suspendCancellableCoroutine { continuation ->
    SdkManager.getUserInfo(userId, object : Callback<UserInfo> {
        override fun onSuccess(data: UserInfo?) {
            continuation.tryResumeYourself(data)
        }
        override fun onError(code: Int, msg: String?) {
            continuation.tryResumeYourself(null)
        }
    })
}
```

> **完整实现见 data-flow-tools.md**

---

## 二、设备 Android ID

```kotlin
import android.provider.Settings

/**
 * 获取设备 Android ID
 * ⚠️ 包名需替换为项目实际包名
 */
fun loadAndroidId(): String {
    return Settings.Secure.getString(
        ContextProvider.get().contentResolver,
        Settings.Secure.ANDROID_ID
    )
}
```

---

> **其他通用工具**（枚举查找、超时协程、正则验证、软键盘监听、RecyclerView 平滑滚动、地图唤起等）不在本 skill 中展开，按项目实际工具层补充。