@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.outcomego.features

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.outcomego.android.data.AndroidNetworkDetails
import com.outcomego.android.data.AndroidNetworkStatusLoader
import com.outcomego.mobile.network.ProviderSnapshot
import com.outcomego.mobile.network.ProviderState
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun NetworkSharingDetailsScreen(
    provider: ProviderSnapshot,
    language: AppSettings.Language,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val loader = remember(context) { AndroidNetworkStatusLoader(context) }
    var details by remember { mutableStateOf<AndroidNetworkDetails?>(null) }
    var loading by remember { mutableStateOf(true) }
    var lastError by remember { mutableStateOf<String?>(null) }
    var qualityDueAt by remember { mutableStateOf(0L) }
    val refreshScope = rememberCoroutineScope()

    suspend fun refresh(forceQuality: Boolean = false) {
        loading = details == null
        val qualityDue = forceQuality || System.currentTimeMillis() >= qualityDueAt
        runCatching { loader.load(includeQuality = qualityDue) }
            .onSuccess { next ->
                val previous = details
                details = if (!qualityDue && previous != null) {
                    next.copy(
                        latencyMs = previous.latencyMs,
                        downloadMbps = previous.downloadMbps,
                        uploadMbps = previous.uploadMbps,
                    )
                } else {
                    qualityDueAt = System.currentTimeMillis() + 60_000
                    next
                }
                lastError = next.error
            }
            .onFailure { lastError = it.message ?: textFor(language, "Could not load network details.", "无法加载网络详情。") }
        loading = false
    }

    LaunchedEffect(provider.state) {
        refresh(forceQuality = true)
        while (isActive) {
            delay(5_000)
            refresh()
        }
    }

    val title = textFor(language, "Cloud computer egress", "云端电脑出口")
    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = textFor(language, "Back", "返回"))
                    }
                },
                title = { Text(title) },
                actions = {
                    IconButton(onClick = { refreshScope.launch { refresh(forceQuality = true) } }) {
                        Icon(Icons.Default.Refresh, contentDescription = textFor(language, "Refresh", "刷新"))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp),
        ) {
            item {
                StatusCard(
                    details = details,
                    provider = provider,
                    language = language,
                    loading = loading,
                )
            }
            item {
                DetailCard(
                    title = textFor(language, "Proxy egress", "代理出口"),
                    rows = listOf(
                        textFor(language, "Address", "地址") to (details?.egressIp ?: "—"),
                        textFor(language, "Location", "位置") to (details?.egressLocation ?: "—"),
                        textFor(language, "Active streams", "活动连接") to (details?.activeConnections?.toString() ?: "—"),
                    ),
                )
            }
            item {
                DetailCard(
                    title = textFor(language, "Traffic", "流量"),
                    rows = listOf(
                        textFor(language, "Sent", "发送") to formatBytes(details?.bytesSent ?: 0),
                        textFor(language, "Received", "接收") to formatBytes(details?.bytesReceived ?: 0),
                    ),
                )
            }
            item {
                DetailCard(
                    title = textFor(language, "Speed", "速度"),
                    rows = listOf(
                        textFor(language, "Latency", "延迟") to (details?.latencyMs?.let { "$it ms" } ?: "—"),
                        textFor(language, "Download", "下载") to (details?.downloadMbps?.let { "%.1f Mbps".format(it) } ?: "—"),
                        textFor(language, "Upload", "上传") to (details?.uploadMbps?.let { "%.1f Mbps".format(it) } ?: "—"),
                    ),
                )
            }
            lastError?.takeIf(String::isNotBlank)?.let { message ->
                item { Text(message, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
            }
            item {
                Button(
                    onClick = { refreshScope.launch { refresh(forceQuality = true) } },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(textFor(language, "Refresh status", "刷新状态"))
                }
            }
        }
    }
}

@Composable
private fun StatusCard(
    details: AndroidNetworkDetails?,
    provider: ProviderSnapshot,
    language: AppSettings.Language,
    loading: Boolean,
) {
    Card(colors = featureCardColors()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(textFor(language, "Connection status", "连接状态"), style = MaterialTheme.typography.titleMedium)
            if (loading && details == null) {
                CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
            }
            StatusLine(
                textFor(language, "This phone provider", "本机提供者"),
                providerStateLabel(provider.state, language),
            )
            StatusLine(
                textFor(language, "Cloud computer route", "云端电脑路由"),
                when {
                    details?.connectedToThisPhone == true ->
                        textFor(language, "Connected and usable", "已连接且可用")
                    details?.routeSelectedToThisPhone == true ->
                        textFor(language, "Selected, but egress unavailable", "已选择本机，但出口不可用")
                    details?.computerState == "server" -> textFor(language, "Using server connection", "正在使用服务器连接")
                    else -> details?.computerState ?: textFor(language, "Unavailable", "不可用")
                },
            )
        }
    }
}

@Composable
private fun DetailCard(title: String, rows: List<Pair<String, String>>) {
    Card(colors = featureCardColors()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            rows.forEach { (label, value) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun StatusLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 14.sp)
    }
}

private fun providerStateLabel(
    state: ProviderState,
    language: AppSettings.Language,
): String = when (state) {
    ProviderState.READY -> textFor(language, "Ready", "已就绪")
    ProviderState.ENROLLING -> textFor(language, "Registering", "注册中")
    ProviderState.CONNECTING, ProviderState.RECONNECTING -> textFor(language, "Connecting", "连接中")
    ProviderState.UNAVAILABLE -> textFor(language, "Unavailable", "不可用")
    ProviderState.REVOKED -> textFor(language, "Authorization revoked", "授权已撤销")
    ProviderState.FAILED -> textFor(language, "Needs attention", "需要处理")
    ProviderState.STOPPED -> textFor(language, "Stopped", "已停止")
}

private fun textFor(
    language: AppSettings.Language,
    english: String,
    chinese: String,
): String = if (language == AppSettings.Language.Chinese) chinese else english

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val units = arrayOf("KiB", "MiB", "GiB")
    var value = bytes.toDouble()
    var unit = 0
    while (value >= 1024 && unit < units.lastIndex) {
        value /= 1024
        unit++
    }
    return "%.1f %s".format(value, units[unit])
}
