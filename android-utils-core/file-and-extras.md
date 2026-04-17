# 剪切板与文件工具

本文件是 android-utils-core 技能的附属参考文件，包含项目封装的剪切板和文件工具。

---

## 一、剪切板工具

```kotlin
import android.content.ClipData
import android.content.ClipboardManager

/**
 * 复制文本到系统剪切板
 * 用法："要复制的内容".copyToClipboard()
 * ⚠️ 包名需替换为项目实际包名
 */
fun String.copyToClipboard(showToast: Boolean = true) {
    val clipboard = ContextProvider.get()
        .getSystemService(android.content.Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("text", this)
    clipboard.setPrimaryClip(clip)
    if (showToast) "已复制到剪切板".show()
}
```

---

## 二、文件工具

```kotlin
import java.io.File

/**
 * 文件操作工具（使用 ContextProvider 获取路径）
 * ⚠️ 包名需替换为项目实际包名
 */
object FileUtils {
    /** 获取应用私有缓存目录下的子目录 */
    fun getCacheDir(subDir: String): File {
        val dir = File(ContextProvider.get().cacheDir, subDir)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /** 获取应用私有文件目录下的子目录 */
    fun getFilesDir(subDir: String): File {
        val dir = File(ContextProvider.get().filesDir, subDir)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /** 删除文件或目录（递归） */
    fun delete(file: File): Boolean {
        return if (file.isDirectory) file.deleteRecursively() else file.delete()
    }

    /** 计算目录大小（字节） */
    fun calculateSize(dir: File): Long {
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    /** 格式化文件大小 */
    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024))
            else -> String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024))
        }
    }
}
```

---

> **其他通用工具**（MD5/SHA加密、ZIP压缩、日期计算、键盘管理、设备品牌识别等）由 AI 自行实现，无需规范文件。