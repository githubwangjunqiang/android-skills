# 图片加载与日志系统

本文件是 android-utils-core 技能的附属参考文件，包含 Glide 图片加载封装和文件日志系统。

---

## 一、图片加载规范

项目统一使用 Glide，已封装扩展函数。

### 1.1 View 图片加载

```kotlin
/**
 * ImageView 图片加载扩展（项目统一封装）
 * @param url 图片地址（String URL / Uri / Int 资源ID）
 * @param isCircle 是否圆形裁剪
 * @param roundedCorners 圆角半径（dp）
 * @param placeholder 占位图资源ID
 * @param error 错误图资源ID
 * ⚠️ 包名需替换为项目实际包名
 */
fun ImageView.loadImage(
    url: Any?,
    isCircle: Boolean = false,
    roundedCorners: Int = 0,
    placeholder: Int = 0,    // 替换为 R.drawable.img_placeholder
    error: Int = 0,          // 替换为 R.drawable.img_error
    skipMemoryCache: Boolean = false,
    diskCacheStrategy: DiskCacheStrategy = DiskCacheStrategy.AUTOMATIC
) {
    if (url == null) return
    val options = RequestOptions()
        .skipMemoryCache(skipMemoryCache)
        .diskCacheStrategy(diskCacheStrategy)
    if (placeholder != 0) options.placeholder(placeholder)
    if (error != 0) options.error(error)

    val builder = when (url) {
        is Uri -> Glide.with(context).load(url)
        is Int -> Glide.with(context).load(url)
        else -> Glide.with(context).load(url.toString())
    }
    when {
        isCircle -> builder.apply(RequestOptions.bitmapTransform(CircleCrop()))
        roundedCorners > 0 -> builder.transform(CenterCrop(), RoundedCorners(roundedCorners.dpToPx()))
        else -> builder.apply(options)
    }.into(this)
}

// 用法：
// imageView.loadImage("https://...", isCircle = true)
// imageView.loadImage(url, roundedCorners = 12, placeholder = R.drawable.img_placeholder)
```

### 1.2 Compose 图片加载

```kotlin
/**
 * Compose 通用图片组件（封装 GlideImage）
 * ⚠️ 包名需替换为项目实际包名
 */
@Composable
fun AppImage(
    model: Any?,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    isCircle: Boolean = false,
    roundedCorners: Int = 0,
    placeholder: Int = 0,
    error: Int = 0,
    contentScale: ContentScale = ContentScale.Crop
) {
    GlideImage(
        model = model,
        contentDescription = contentDescription,
        modifier = modifier.then(
            when {
                isCircle -> Modifier.clip(CircleShape)
                roundedCorners > 0 -> Modifier.clip(RoundedCornerShape(roundedCorners.dp))
                else -> Modifier
            }
        ),
        contentScale = contentScale
    ) {
        if (placeholder != 0) it.placeholder(placeholder)
        if (error != 0) it.error(error)
        it.diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
    }
}

// 用法：
// AppImage(model = avatarUrl, modifier = Modifier.size(48.dp), isCircle = true)
// AppImage(model = url, modifier = Modifier.size(80.dp), roundedCorners = 12)
```

---

## 二、日志系统（项目特有）

### 2.1 日志工具函数

```kotlin
/** 日志开关：Release 关闭 Logcat，文件日志始终写入 */
private val LOG_ENABLED: Boolean get() = BuildConfig.DEBUG

/** Debug 日志：用法 "请求成功".logd() */
fun String?.logd(tag: String = LOG_TAG) {
    if (this.isNullOrEmpty()) return
    if (LOG_ENABLED) Log.d(tag, this)
    LogManager.log(message = "[$tag] $this", type = "debug")
}

/** Error 日志（字符串）：用法 "解析失败".loge() */
fun String?.loge(tag: String = LOG_TAG) {
    if (this.isNullOrEmpty()) return
    if (LOG_ENABLED) Log.e(tag, this)
    LogManager.log(message = "[ERROR][$tag] $this", type = "error")
}

/** Error 日志（异常）：用法 exception.loge() */
fun Exception?.loge(tag: String = LOG_TAG) {
    if (this == null) return
    val msg = this.stackTraceToString()
    if (LOG_ENABLED) Log.e(tag, msg)
    LogManager.log(message = "[EXCEPTION][$tag] $msg", type = "error")
}

/** HTTP 请求日志：用法 "POST /api/user".logHttp() */
fun String?.logHttp(tag: String = "HTTP") {
    if (this.isNullOrEmpty()) return
    if (LOG_ENABLED) splitAndLog(tag)
    LogManager.log(message = "[$tag] $this", type = "http")
}
```

### 2.2 文件日志管理器

按小时轮转，单类型目录上限 100MB，异步非阻塞写入：

```kotlin
/**
 * 异步文件日志管理器
 * ⚠️ 包名需替换为项目实际包名
 */
object LogManager {
    private data class LogEntry(val message: String, val type: String)
    private val logChannel = Channel<LogEntry>(Channel.UNLIMITED)
    private lateinit var baseLogDir: String
    private var isInitialized = false
    private var writeCount = 0
    private const val MAX_DIR_SIZE = 100L * 1024 * 1024  // 100MB
    private const val CLEAN_CHECK_INTERVAL = 100
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** 初始化：Application.onCreate() 中调用 */
    fun init(context: Context, appSpecificBasePath: String) {
        if (isInitialized) return
        baseLogDir = appSpecificBasePath  // 如 "${filesDir}/logs"
        isInitialized = true
        scope.launch {
            for (entry in logChannel) {
                writeLogToFile(entry)
            }
        }
    }

    /** 写入日志（非阻塞） */
    fun log(message: String, type: String = "default") {
        if (!isInitialized) return
        logChannel.trySend(LogEntry(message, type))
    }

    private fun writeLogToFile(entry: LogEntry) {
        try {
            val now = Date()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val hourFormat = SimpleDateFormat("HH", Locale.getDefault())
            val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

            // 路径：baseLogDir/type/yyyy-MM-dd/HH.log
            val logDir = File(baseLogDir, "${entry.type}/${dateFormat.format(now)}")
            if (!logDir.exists()) logDir.mkdirs()

            val logFile = File(logDir, "${hourFormat.format(now)}.log")
            val logLine = "[${timeFormat.format(now)}] ${entry.message}\n"
            logFile.appendText(logLine)

            writeCount++
            if (writeCount >= CLEAN_CHECK_INTERVAL) {
                writeCount = 0
                checkAndCleanLogDirectory(entry.type)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkAndCleanLogDirectory(type: String) {
        val typeDir = File(baseLogDir, type)
        if (!typeDir.exists()) return
        val totalSize = typeDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        if (totalSize <= MAX_DIR_SIZE) return

        // 删除最旧的日期目录
        val dateDirs = typeDir.listFiles()?.filter { it.isDirectory }?.sortedBy { it.name } ?: return
        for (dir in dateDirs) {
            dir.deleteRecursively()
            if (typeDir.walkTopDown().filter { it.isFile }.sumOf { it.length() } <= MAX_DIR_SIZE * 0.8) break
        }
    }
}
```

---

> **Bitmap 下载、图片缓存策略等通用内容由 AI 自行实现。**