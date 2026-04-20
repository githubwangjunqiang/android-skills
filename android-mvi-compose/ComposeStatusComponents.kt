/**
 * Compose 状态页组件 - Loading、Error、Empty、LoadingDialog
 * ⚠️ 包名需替换为项目实际包名
 * 例如：com.xxx.app → com.yourcompany.yourapp
 */
package com.xxx.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.shape.RoundedCornerShape

/**
 * 全屏加载状态页
 * @param text 可选加载文案，为空时仅显示进度条
 */
@Composable
fun LoadingScreen(
    modifier: Modifier = Modifier,
    text: String? = null
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            if (!text.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 空数据状态页
 * 整个区域可点击重试 + 提供明显的重新加载按钮
 *
 * @param msg 自定义提示文案
 * @param iconRes 空状态图标资源ID（需在项目中定义，如 R.drawable.ic_empty_default）
 * @param onRetry 重试回调（整个区域和按钮均触发）
 */
@Composable
fun EmptyScreen(
    modifier: Modifier = Modifier,
    msg: String? = null,
    @DrawableRes iconRes: Int,
    onRetry: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (onRetry != null) Modifier.clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onRetry
                ) else Modifier
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = "空数据状态图",
            modifier = Modifier.size(120.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = msg ?: "暂无数据",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "点击页面重新加载",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        if (onRetry != null) {
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedButton(onClick = onRetry) {
                Text("重新加载")
            }
        }
    }
}

/**
 * 错误状态页
 * 整个区域可点击重试 + 提供明显的重新加载按钮
 *
 * @param msg 错误信息
 * @param iconRes 错误图标资源ID（需在项目中定义，如 R.drawable.ic_error_default）
 * @param onRetry 重试回调
 */
@Composable
fun ErrorScreen(
    modifier: Modifier = Modifier,
    msg: String? = null,
    @DrawableRes iconRes: Int,
    onRetry: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (onRetry != null) Modifier.clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onRetry
                ) else Modifier
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = "错误状态图",
            modifier = Modifier.size(120.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = msg ?: "加载失败，请重试",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "点击页面重新加载",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        if (onRetry != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onRetry) {
                Text("重新加载")
            }
        }
    }
}

/**
 * 加载中弹窗（半透明遮罩 + 居中 loading）
 * 不遮挡底层内容，适用于提交操作
 */
@Composable
fun LoadingDialog(
    msg: String? = null,
    onDismiss: (() -> Unit)? = null
) {
    Dialog(
        onDismissRequest = { onDismiss?.invoke() },
        properties = DialogProperties(
            dismissOnBackPress = onDismiss != null,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                if (!msg.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = msg, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
