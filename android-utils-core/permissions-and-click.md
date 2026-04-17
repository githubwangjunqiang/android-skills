# 权限管理规范

本文件是 android-utils-core 技能的附属参考文件，包含 Compose 运行时权限请求规范。

---

## 一、权限请求规范

### 1.1 单权限请求

```kotlin
val launcher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
) { granted ->
    if (granted) {
        // 权限授予，继续操作
    } else {
        // 权限拒绝，提示用户
        if (ActivityCompat.shouldShowRequestPermissionRationale(context, permission)) {
            "需要权限才能继续".show()
        } else {
            // 用户勾选"不再询问"，引导去设置页
            context.openAppSettings()
        }
    }
}

// 触发请求
launcher.launch(Manifest.permission.CAMERA)
```

### 1.2 多权限请求

```kotlin
val multiLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions()
) { perms ->
    val allGranted = perms.values.all { it }
    if (allGranted) {
        // 全部授予
    } else {
        // 部分拒绝，检查具体权限
        val denied = perms.filter { !it.value }.keys
        "以下权限被拒绝: ${denied.joinToString()}".show()
    }
}

// 触发请求
multiLauncher.launch(arrayOf(
    Manifest.permission.CAMERA,
    Manifest.permission.READ_MEDIA_IMAGES
))
```

### 1.3 跳转系统权限设置页

```kotlin
/**
 * 跳转应用权限设置页
 * ⚠️ 包名需替换为项目实际包名
 */
fun Context.openAppSettings() {
    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    })
}
```

---

## 二、权限请求时机规范

| 场景 | 时机 | 说明 |
|------|------|------|
| 相机/录音 | 用户点击拍照/录音按钮时 | 上下文相关请求，用户更容易理解 |
| 存储/媒体 | 首次保存/读取文件时 | 避免启动时就请求 |
| 定位 | 用户点击地图/导航时 | 上下文相关请求 |

**禁止**：在 App 启动时批量请求所有权限。

---

> **防连续点击等通用工具由 AI 自行实现，无需规范文件。**