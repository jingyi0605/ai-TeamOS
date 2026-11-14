@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.outcomego.features

import android.graphics.BitmapFactory
import android.net.Uri
import java.io.FileInputStream
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import com.outcomego.platform.AndroidUpdateState
import com.outcomego.mobile.network.ProviderSnapshot
import com.outcomego.mobile.network.ProviderState

@Composable
internal fun featureTopBarColors() = TopAppBarDefaults.topAppBarColors(
    containerColor = MaterialTheme.colorScheme.surfaceVariant,
    scrolledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
)

@Composable
internal fun featureCardColors() = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)

@Composable
internal fun FeaturePage(title: String, onBack: (() -> Unit)? = null, settingsStyle: Boolean = false, content: @Composable () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    onBack?.let {
                        IconButton(onClick = it) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = localizedText("Back", "返回"))
                        }
                    }
                },
                title = { Text(title) },
                colors = if (settingsStyle) settingsTopBarColors() else featureTopBarColors(),
            )
        },
        containerColor = if (settingsStyle) settingsCanvas() else MaterialTheme.colorScheme.surfaceVariant,
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) { content() }
    }
}

@Composable
private fun <T> LoadStateContent(
    state: FeatureLoadState<T>,
    onRetry: () -> Unit,
    emptyAction: (@Composable () -> Unit)? = null,
    content: @Composable (T) -> Unit,
) {
    when (state) {
        FeatureLoadState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        is FeatureLoadState.Failure -> StateMessage(state.message, onRetry)
        is FeatureLoadState.Empty -> Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(state.message, color = MaterialTheme.colorScheme.onSurfaceVariant)
            emptyAction?.invoke()
        }
        is FeatureLoadState.Ready -> content(state.value)
    }
}

@Composable
private fun StateMessage(message: String, onRetry: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(message, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(12.dp))
        Button(onClick = onRetry) { Text(localizedText("Retry", "重试")) }
    }
}

@Composable
private fun OperationStatus(state: FeatureOperationState) {
    when (state) {
        FeatureOperationState.Idle -> Unit
        FeatureOperationState.Pending -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
            Text(localizedText("Working…", "处理中…"), style = MaterialTheme.typography.bodySmall)
        }
        is FeatureOperationState.Succeeded -> state.message?.let { Text(it, color = MaterialTheme.colorScheme.tertiary) }
        is FeatureOperationState.Failed -> Text(state.message, color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun MessageAction.localizedLabel(): String = when (this) {
    MessageAction.Fork -> localizedText("Fork", "分支")
    MessageAction.Stop -> localizedText("Stop", "停止")
    MessageAction.Dismiss -> localizedText("Dismiss", "忽略")
    MessageAction.Approve -> localizedText("Approve", "批准")
}

@Composable
private fun RoutineSchedule.Frequency.localizedLabel(): String = when (this) {
    RoutineSchedule.Frequency.Hourly -> localizedText("Hourly", "每小时")
    RoutineSchedule.Frequency.Daily -> localizedText("Daily", "每天")
    RoutineSchedule.Frequency.Weekdays -> localizedText("Weekdays", "工作日")
    RoutineSchedule.Frequency.Weekly -> localizedText("Weekly", "每周")
    RoutineSchedule.Frequency.Monthly -> localizedText("Monthly", "每月")
    RoutineSchedule.Frequency.Interval -> localizedText("Interval", "间隔")
}

@Composable
private fun RoutineSchedule.localizedSummary(): String {
    val safeMinutes = timeMinutes.coerceIn(0, 23 * 60 + 59)
    val hour = safeMinutes / 60
    val minute = safeMinutes % 60
    val time = "%02d:%02d".format(hour, minute)
    val weekday = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        .getOrElse((dayOfWeek - 1).coerceIn(0, 6)) { "Monday" }
    val chineseWeekday = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
        .getOrElse((dayOfWeek - 1).coerceIn(0, 6)) { "周一" }
    val ordinal = when {
        dayOfMonth % 100 in 11..13 -> "${dayOfMonth}th"
        dayOfMonth % 10 == 1 -> "${dayOfMonth}st"
        dayOfMonth % 10 == 2 -> "${dayOfMonth}nd"
        dayOfMonth % 10 == 3 -> "${dayOfMonth}rd"
        else -> "${dayOfMonth}th"
    }
    val chineseUnit = when (intervalUnit) {
        RoutineSchedule.IntervalUnit.Minutes -> "分钟"
        RoutineSchedule.IntervalUnit.Hours -> "小时"
        RoutineSchedule.IntervalUnit.Days -> "天"
        RoutineSchedule.IntervalUnit.Weeks -> "周"
    }
    return when (frequency) {
        RoutineSchedule.Frequency.Hourly -> localizedText("Every hour at :%02d".format(minute), "每小时在 :%02d".format(minute))
        RoutineSchedule.Frequency.Daily -> localizedText("Daily at $time", "每天 $time")
        RoutineSchedule.Frequency.Weekdays -> localizedText("Weekdays at $time", "工作日 $time")
        RoutineSchedule.Frequency.Weekly -> localizedText("Weekly on $weekday at $time", "每周${chineseWeekday} $time")
        RoutineSchedule.Frequency.Monthly -> localizedText("Monthly on the $ordinal at $time", "每月${dayOfMonth}日 $time")
        RoutineSchedule.Frequency.Interval -> localizedText("Every $intervalAmount ${intervalUnit.label}", "每 $intervalAmount $chineseUnit")
    } + " ($timeZone)"
}

@Composable
fun RichMessageCardScreen(
    message: RichMessage,
    operation: FeatureOperationState = FeatureOperationState.Idle,
    onAction: (RichMessageAction, Map<String, String>) -> Unit,
) {
    val values = remember(message.id) { mutableStateMapOf<String, String>() }
    // A question card is the whole message, so it renders directly in the bubble.
    val questionOnly = message.isQuestion
    Card(
        Modifier.fillMaxWidth().then(if (questionOnly) Modifier else Modifier.padding(12.dp)),
        colors = CardDefaults.cardColors(containerColor = if (questionOnly) androidx.compose.ui.graphics.Color.Transparent else MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(if (questionOnly) 0.dp else 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            message.blocks.forEach { block ->
                when (block) {
                    is RichMessageBlock.Markdown -> MarkdownMessage(block.text)
                    is RichMessageBlock.Code -> CodeBlock(block.language, block.code)
                    is RichMessageBlock.Math -> CodeBlock("math", block.expression)
                    is RichMessageBlock.Mermaid -> CodeBlock("mermaid", block.source)
                    is RichMessageBlock.Table -> MessageTable(block)
                    is RichMessageBlock.Card -> {
                        Text(block.title, fontWeight = FontWeight.Bold)
                        Text(block.body)
                        block.action?.let { ActionButton(it) { onAction(it, emptyMap()) } }
                    }
                    is RichMessageBlock.Form -> {
                        Text(block.title, fontWeight = FontWeight.Bold)
                        block.fields.forEach { field ->
                            OutlinedTextField(
                                value = values[field.id] ?: field.value,
                                onValueChange = { values[field.id] = it },
                                label = { Text(field.label + if (field.required) " *" else "") },
                                visualTransformation = if (field.secret) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                            )
                            if (field.options.isNotEmpty()) {
                                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    field.options.forEach { option ->
                                        FilterChip(
                                            selected = values[field.id] == option,
                                            onClick = { values[field.id] = option },
                                            label = { Text(option) },
                                        )
                                    }
                                }
                            }
                        }
                        ActionButton(block.submit) { onAction(block.submit, values.toMap() + ("_richType" to "user-form")) }
                    }
                    is RichMessageBlock.Secret -> {
                        Text(block.prompt, fontWeight = FontWeight.Bold)
                        var value by remember(message.id, block.prompt) { mutableStateOf("") }
                        OutlinedTextField(
                            value = value,
                            onValueChange = { value = it },
                            label = { Text(localizedText("Secret", "密钥")) },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                        ActionButton(block.submit) { onAction(block.submit, mapOf("value" to value, "_richType" to "secret")) }
                    }
                    is RichMessageBlock.Approval -> {
                        Text(block.title, fontWeight = FontWeight.Bold)
                        Text(block.detail)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ActionButton(block.approve) { onAction(block.approve, emptyMap()) }
                            ActionButton(block.reject) { onAction(block.reject, emptyMap()) }
                        }
                    }
                    is RichMessageBlock.Widget -> WidgetQuestion(message, block, operation, onAction)
                }
            }
            message.actions.forEach { action -> ActionButton(action) { onAction(action, emptyMap()) } }
            OperationStatus(operation)
        }
    }
}

/**
 * Question card shared with the iOS client: a prompt, a dismiss control, and a bordered option list.
 * A single choice is submitted by tapping it. Only multi-select needs a Submit button.
 */
@Composable
private fun WidgetQuestion(
    message: RichMessage,
    block: RichMessageBlock.Widget,
    operation: FeatureOperationState,
    onAction: (RichMessageAction, Map<String, String>) -> Unit,
) {
    val responded = message.metadata["respondedValue"]
    val dismissed = message.metadata["widgetDismissed"] == "true"
    val busy = operation is FeatureOperationState.Pending
    var selected by remember(message.id, block.prompt) { mutableStateOf(emptySet<String>()) }
    val outline = MaterialTheme.colorScheme.outlineVariant
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    fun respond(value: String) = onAction(
        RichMessageAction("widget-response", value, route = "widget-response"),
        mapOf("value" to value, "_richType" to "widget-response"),
    )

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Text(block.prompt, Modifier.weight(1f), fontWeight = FontWeight.Medium)
            if (responded == null && !dismissed) {
                IconButton(
                    onClick = {
                        onAction(
                            RichMessageAction("widget-dismiss", "Dismiss", route = "widget-dismiss"),
                            mapOf("_richType" to "widget-dismiss"),
                        )
                    },
                    enabled = !busy,
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = localizedText("Dismiss", "忽略"),
                        modifier = Modifier.size(16.dp),
                        tint = muted,
                    )
                }
            }
        }
        block.helpText?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = muted) }
        val frame = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.background)
            .border(0.5.dp, outline, RoundedCornerShape(12.dp))
        if (responded != null) {
            val answers = responded.split("\n").filter { it.isNotBlank() }
                .map { value -> block.options.firstOrNull { it.value == value }?.label ?: value }
            Column(frame) {
                answers.forEachIndexed { index, answer ->
                    if (index > 0) HorizontalDivider(color = outline, thickness = 0.5.dp)
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 40.dp).padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(answer, Modifier.weight(1f))
                        Icon(Icons.Default.Check, contentDescription = localizedText("Selected", "已选择"), modifier = Modifier.size(16.dp))
                    }
                }
            }
        } else {
            Column(frame.then(if (dismissed) Modifier.alpha(0.35f) else Modifier)) {
                block.options.forEachIndexed { index, option ->
                    if (index > 0) HorizontalDivider(color = outline.copy(alpha = 0.5f), thickness = 0.5.dp)
                    val isSelected = option.value in selected
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 40.dp)
                            .clickable(enabled = !dismissed && !busy) {
                                if (block.multiSelect) {
                                    selected = if (isSelected) selected - option.value else selected + option.value
                                } else {
                                    respond(option.value)
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Box(
                            Modifier.size(20.dp).clip(RoundedCornerShape(5.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                if (index < 26) ('A' + index).toString() else (index + 1).toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = muted,
                            )
                        }
                        Spacer(Modifier.width(9.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                option.label,
                                color = if (option.style == "danger") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                            )
                            option.description?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = muted) }
                        }
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = localizedText("Selected", "已选择"), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
            if (dismissed) {
                Text(localizedText("dismissed", "已忽略"), style = MaterialTheme.typography.labelSmall, color = muted)
            } else {
                if (block.allowCustom) {
                    Text(localizedText("Or answer in the chat below", "也可以在下方输入框中回答"), style = MaterialTheme.typography.bodySmall, color = muted)
                }
                if (block.multiSelect) {
                    Button(
                        onClick = {
                            respond(block.options.map { it.value }.filter { it in selected }.joinToString("\n"))
                        },
                        enabled = selected.isNotEmpty() && !busy,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(localizedText("Submit", "提交")) }
                }
            }
        }
    }
}

@Composable
private fun CodeBlock(language: String?, code: String) {
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(12.dp)) {
        language?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) }
        Text(code, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun MessageTable(table: RichMessageBlock.Table) {
    Column(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
        Row(Modifier.background(MaterialTheme.colorScheme.surface), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            table.headers.forEach { Text(it, Modifier.padding(8.dp), fontWeight = FontWeight.Bold) }
        }
        table.rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) { row.forEach { Text(it, Modifier.padding(8.dp)) } }
        }
    }
}

@Composable
private fun ActionButton(action: RichMessageAction, onClick: () -> Unit) {
    when (action.kind) {
        RichMessageAction.Kind.Primary -> Button(onClick = onClick, enabled = action.enabled) { Text(action.label) }
        RichMessageAction.Kind.Secondary -> OutlinedButton(onClick = onClick, enabled = action.enabled) { Text(action.label) }
        RichMessageAction.Kind.Destructive -> Button(onClick = onClick, enabled = action.enabled, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text(action.label) }
    }
}

@Composable
fun AttachmentGalleryScreen(
    state: FeatureLoadState<List<Attachment>>,
    operation: FeatureOperationState = FeatureOperationState.Idle,
    onRetry: () -> Unit,
    onAttachmentAction: (Attachment, AttachmentAction) -> Unit,
    onUpload: () -> Unit = {},
    onBack: (() -> Unit)? = null,
) {
    FeaturePage(localizedText("Attachments", "附件"), onBack = onBack) {
        LoadStateContent(state, onRetry) { attachments ->
            LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Button(onClick = onUpload) { Text(localizedText("Add attachment", "添加附件")) } }
                items(attachments, key = { it.id }) { attachment ->
                    Card(Modifier.fillMaxWidth(), colors = featureCardColors()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(attachment.name, Modifier.weight(1f), fontWeight = FontWeight.Medium)
                                Text(formatBytes(attachment.sizeBytes), style = MaterialTheme.typography.labelSmall)
                            }
                            Text(attachment.mimeType, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(attachment.state.label(), style = MaterialTheme.typography.labelMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (attachment.state == AttachmentState.Available) {
                                    TextButton(onClick = { onAttachmentAction(attachment, AttachmentAction.Preview) }) { Text(localizedText("Preview", "预览")) }
                                    TextButton(onClick = { onAttachmentAction(attachment, AttachmentAction.Download) }) { Text(localizedText("Download", "下载")) }
                                    TextButton(onClick = { onAttachmentAction(attachment, AttachmentAction.Save) }) { Text(localizedText("Save", "保存")) }
                                    TextButton(onClick = { onAttachmentAction(attachment, AttachmentAction.Share) }) { Text(localizedText("Share", "分享")) }
                                    TextButton(onClick = { onAttachmentAction(attachment, AttachmentAction.Forward) }) { Text(localizedText("Forward", "转发")) }
                                }
                                if (attachment.state == AttachmentState.Failed) TextButton(onClick = { onAttachmentAction(attachment, AttachmentAction.Retry) }) { Text(localizedText("Retry", "重试")) }
                            }
                        }
                    }
                }
                item { OperationStatus(operation) }
            }
        }
    }
}

/**
 * Decodes a local image off the main thread, downsampled to roughly [maxSizePx] on its longest
 * edge. Full-size synchronous decodes during composition dropped frames while scrolling chats.
 */
@Composable
fun rememberDecodedBitmap(path: String?, maxSizePx: Int): ImageBitmap? {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(null, path, maxSizePx) {
        value = path?.let { file ->
            withContext(Dispatchers.IO) {
                runCatching {
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    openBitmapStream(context, file).use { BitmapFactory.decodeStream(it, null, bounds) }
                    var sample = 1
                    while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSizePx) sample *= 2
                    openBitmapStream(context, file).use {
                        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
                    }
                        ?.asImageBitmap()
                }.getOrNull()
            }
        }
    }
    return bitmap
}

internal fun openBitmapStream(context: android.content.Context, path: String) =
    Uri.parse(path).let { uri ->
        if (uri.scheme == "content") {
            context.contentResolver.openInputStream(uri)
                ?: error("The image could not be opened.")
        } else {
            FileInputStream(if (uri.scheme == "file") uri.path ?: path else path)
        }
    }

@Composable
fun ChatAttachmentStrip(
    attachments: List<Attachment>,
    onPreview: (Attachment, AttachmentAction) -> Unit,
    onDownload: (Attachment, AttachmentAction) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        attachments.forEach { attachment ->
            val localPath = attachment.localUri
            if (attachment.isImage) {
                LaunchedEffect(attachment.id, localPath) {
                    if (localPath == null) onDownload(attachment, AttachmentAction.Download)
                }
                val bitmap = rememberDecodedBitmap(localPath, maxSizePx = 1024)
                Card(
                    Modifier.fillMaxWidth().clickable { onPreview(attachment, AttachmentAction.Preview) },
                ) {
                    if (bitmap != null) {
                        Image(
                            bitmap,
                            contentDescription = attachment.alt ?: attachment.name,
                            modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                        )
                    } else {
                        Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(Modifier.size(24.dp))
                        }
                    }
                }
            } else {
                // As on macOS and iOS, the whole card opens the viewer; downloading starts from there.
                FileAttachmentCard(attachment) { onPreview(attachment, AttachmentAction.Preview) }
            }
        }
    }
}

/** A file's compact size as the other clients show it: "658B", "38kB", "1.2MB". */
fun formatAttachmentSize(bytes: Long): String {
    if (bytes < 0) return ""
    val units = arrayOf("B", "kB", "MB", "GB")
    var value = bytes.toDouble()
    var unit = 0
    while (value >= 1024 && unit < units.lastIndex) {
        value /= 1024
        unit += 1
    }
    val text = if (unit == 0 || value >= 10) "%.0f".format(java.util.Locale.ROOT, value) else "%.1f".format(java.util.Locale.ROOT, value)
    return text + units[unit]
}

private enum class FileGlyphVariant { Pdf, Docx, Ppt, Spreadsheet, Generic, Zip, Image, Video, Audio, Font, Unknown }

private val glyphExtensions: Map<String, FileGlyphVariant> = buildMap {
    listOf("doc", "docx", "odt", "pages", "rtf").forEach { put(it, FileGlyphVariant.Docx) }
    listOf("ppt", "pptx", "odp", "key").forEach { put(it, FileGlyphVariant.Ppt) }
    listOf("xls", "xlsx", "csv", "tsv", "ods", "numbers").forEach { put(it, FileGlyphVariant.Spreadsheet) }
    listOf("zip", "gz", "tgz", "tar", "7z", "rar").forEach { put(it, FileGlyphVariant.Zip) }
    listOf("md", "markdown", "txt", "log", "html", "xml", "yaml", "yml", "json", "js", "ts", "py", "swift", "kt").forEach { put(it, FileGlyphVariant.Generic) }
    listOf("png", "jpg", "jpeg", "gif", "webp", "heic", "svg").forEach { put(it, FileGlyphVariant.Image) }
    listOf("mp4", "mov", "m4v", "webm", "mkv").forEach { put(it, FileGlyphVariant.Video) }
    listOf("mp3", "m4a", "wav", "aac", "ogg", "flac").forEach { put(it, FileGlyphVariant.Audio) }
    listOf("otf", "ttf", "woff", "woff2").forEach { put(it, FileGlyphVariant.Font) }
    put("pdf", FileGlyphVariant.Pdf)
}

private fun glyphVariant(fileName: String): FileGlyphVariant {
    val dot = fileName.lastIndexOf('.')
    return glyphExtensions[if (dot > 0) fileName.substring(dot + 1).lowercase() else ""] ?: FileGlyphVariant.Unknown
}

@Composable
private fun FileAttachmentCard(attachment: Attachment, onOpen: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.background)
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .clickable(onClickLabel = localizedText("Open ${attachment.name}", "打开 ${attachment.name}"), onClick = onOpen)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FileGlyph(attachment.name)
        Column(Modifier.weight(1f)) {
            Text(
                attachment.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                formatAttachmentSize(attachment.sizeBytes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        if (attachment.state == AttachmentState.Downloading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
    }
}

/** A 32x40 paper icon: lines of ink and a colored band by file type, as on macOS. */
@Composable
fun FileGlyph(fileName: String, modifier: Modifier = Modifier) {
    val variant = glyphVariant(fileName)
    val dark = MaterialTheme.colorScheme.background.luminance() < .45f
    val ink = if (dark) Color(0x3DF5F5F5) else Color(0x2E1C1C1C)
    val mark = if (dark) Color(0x80F5F5F5) else Color(0x851C1C1C)
    val stripe = when (variant) {
        FileGlyphVariant.Pdf -> if (dark) Color(0xFFFC2C3E) else Color(0xFFFB2B3E)
        FileGlyphVariant.Docx -> if (dark) Color(0xFF116DD2) else Color(0xFF1680F3)
        FileGlyphVariant.Ppt -> if (dark) Color(0xFFCD5309) else Color(0xFFEE620D)
        FileGlyphVariant.Spreadsheet -> if (dark) Color(0xFF0E9F5A) else Color(0xFF10AB61)
        else -> null
    }
    val paper = RoundedCornerShape(6.dp)
    fun Modifier.line(width: Int) = this.width(width.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(ink)
    Column(
        modifier
            .size(width = 32.dp, height = 40.dp)
            .clip(paper)
            .background(if (dark) Color(0xFF2C2C2C) else Color.White)
            .border(0.5.dp, ink, paper),
    ) {
        val symbol = when (variant) {
            FileGlyphVariant.Image -> Icons.Default.Image
            FileGlyphVariant.Video -> Icons.Default.Movie
            FileGlyphVariant.Audio -> Icons.Default.MusicNote
            FileGlyphVariant.Font -> Icons.Default.TextFields
            else -> null
        }
        when {
            symbol != null -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Icon(symbol, contentDescription = null, tint = mark, modifier = Modifier.size(16.dp))
            }
            variant == FileGlyphVariant.Unknown -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("?", fontSize = 17.sp, color = mark)
            }
            variant == FileGlyphVariant.Zip -> Column(
                Modifier.weight(1f).fillMaxWidth().padding(top = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                repeat(5) { row ->
                    Box(Modifier.offset(x = if (row % 2 == 0) (-2.5).dp else 2.5.dp).size(width = 5.dp, height = 3.dp).background(ink))
                }
            }
            else -> Column(
                Modifier.weight(1f).fillMaxWidth().padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Box(Modifier.line(24))
                if (stripe != null) {
                    Row(Modifier.height(11.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Box(Modifier.line(11))
                            Box(Modifier.line(11))
                        }
                        Box(Modifier.size(11.dp).clip(RoundedCornerShape(2.dp)).background(ink))
                    }
                } else {
                    Box(Modifier.line(20))
                    Box(Modifier.line(24))
                    Box(Modifier.line(16))
                }
            }
        }
        if (stripe != null) Box(Modifier.fillMaxWidth().height(12.dp).background(stripe))
    }
}

enum class AttachmentAction { Preview, Download, Share, Save, Forward, Retry }

@Composable
private fun AttachmentState.label() = when (this) {
    AttachmentState.Queued -> localizedText("Queued", "排队中")
    AttachmentState.Uploading -> localizedText("Uploading…", "正在上传…")
    AttachmentState.Available -> localizedText("Ready", "就绪")
    AttachmentState.Downloading -> localizedText("Downloading…", "正在下载…")
    AttachmentState.Failed -> localizedText("Transfer failed", "传输失败")
}

private fun formatBytes(value: Long): String = when {
    value < 1_024 -> "$value B"
    value < 1_024 * 1_024 -> "${value / 1_024} KB"
    else -> "${value / (1_024 * 1_024)} MB"
}

@Composable
fun ReactionsAndActionsRow(
    state: ReactionState,
    availableEmojis: List<String> = listOf("👍", "❤️", "🎉", "👀"),
    onReaction: (String) -> Unit,
    onAction: (MessageAction) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            state.reactions.forEach { reaction ->
                AssistChip(onClick = { onReaction(reaction.emoji) }, label = { Text("${reaction.emoji} ${reaction.count}") })
            }
            availableEmojis.forEach { emoji ->
                AssistChip(onClick = { onReaction(emoji) }, label = { Text(emoji) })
            }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            MessageAction.entries.forEach { action -> TextButton(onClick = { onAction(action) }) { Text(action.localizedLabel()) } }
        }
        OperationStatus(state.operation)
    }
}

@Composable
fun RoutinesScreen(
    state: FeatureLoadState<List<Routine>>,
    onRetry: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (Routine) -> Unit,
    onToggle: (Routine) -> Unit,
    onRun: (Routine) -> Unit,
    onBack: () -> Unit,
) {
    val haptic = LocalAppHaptics.current
    FeaturePage(localizedText("Routines", "例行任务"), onBack = onBack) {
        LoadStateContent(state, onRetry, emptyAction = { FilledTonalButton(onClick = onAdd) { Text(localizedText("Add routine", "添加例行任务")) } }) { routines ->
            LazyColumn(
                Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 14.dp, bottom = 28.dp),
            ) {
                item {
                    FilledTonalButton(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
                        Text(localizedText("Add routine", "添加例行任务"))
                    }
                }
                items(routines, key = { it.id }) { routine ->
                    Card(Modifier.fillMaxWidth(), colors = featureCardColors(), shape = MaterialTheme.shapes.medium) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(routine.name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Switch(checked = routine.enabled, onCheckedChange = { haptic.play(HapticEffect.Selection); onToggle(routine) })
                            }
                            Text(routine.schedule.localizedSummary(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(routine.prompt, maxLines = 3, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                            routine.nextRunAt?.let {
                                Text(localizedText("Next: $it", "下次运行：$it"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilledTonalButton(onClick = { onEdit(routine) }) { Text(localizedText("Edit", "编辑")) }
                                TextButton(onClick = { haptic.play(HapticEffect.Light); onRun(routine) }) { Text(localizedText("Run now", "立即运行")) }
                            }
                            if (routine.executions.isNotEmpty()) {
                                HorizontalDivider()
                                Text(localizedText("Recent runs", "最近运行记录"), fontWeight = FontWeight.Medium)
                                routine.executions.take(3).forEach { execution -> Text("${execution.status} · ${execution.createdAt}", style = MaterialTheme.typography.bodySmall) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RoutineEditorScreen(
    routine: Routine,
    onChange: (Routine) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    onDelete: (() -> Unit)? = null,
    operation: FeatureOperationState = FeatureOperationState.Idle,
) {
    val haptic = LocalAppHaptics.current
    var confirmDelete by remember { mutableStateOf(false) }
    FeaturePage(if (routine.id.isBlank()) localizedText("New routine", "新建例行任务") else localizedText("Edit routine", "编辑例行任务"), onBack = onBack) {
        LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                OutlinedTextField(routine.name, { onChange(routine.copy(name = it)) }, label = { Text(localizedText("Routine name", "例行任务名称")) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(routine.prompt, { onChange(routine.copy(prompt = it)) }, label = { Text(localizedText("Instructions", "说明")) }, modifier = Modifier.fillMaxWidth().height(130.dp))
            }
            item {
                Text(localizedText("Frequency", "频率"), fontWeight = FontWeight.Bold)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RoutineSchedule.Frequency.entries.forEach { frequency ->
                        FilterChip(
                            selected = routine.schedule.frequency == frequency,
                            onClick = { onChange(routine.copy(schedule = routine.schedule.copy(frequency = frequency))) },
                            label = { Text(frequency.localizedLabel()) },
                        )
                    }
                }
            }
            item {
                OutlinedTextField(routine.schedule.timeZone, { onChange(routine.copy(schedule = routine.schedule.copy(timeZone = it))) }, label = { Text(localizedText("Time zone", "时区")) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(routine.schedule.timeMinutes.toString(), { it.toIntOrNull()?.let { minutes -> onChange(routine.copy(schedule = routine.schedule.copy(timeMinutes = minutes))) } }, label = { Text(localizedText("Time of day (minutes after midnight)", "时间（距午夜的分钟数）")) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                if (routine.schedule.frequency == RoutineSchedule.Frequency.Weekly) {
                    OutlinedTextField(routine.schedule.dayOfWeek.toString(), { it.toIntOrNull()?.let { day -> onChange(routine.copy(schedule = routine.schedule.copy(dayOfWeek = day))) } }, label = { Text(localizedText("Day of week (1–7)", "星期几（1–7）")) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
                if (routine.schedule.frequency == RoutineSchedule.Frequency.Monthly) {
                    OutlinedTextField(routine.schedule.dayOfMonth.toString(), { it.toIntOrNull()?.let { day -> onChange(routine.copy(schedule = routine.schedule.copy(dayOfMonth = day))) } }, label = { Text(localizedText("Day of month (1–31)", "每月第几天（1–31）")) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
                if (routine.schedule.frequency == RoutineSchedule.Frequency.Interval) {
                    OutlinedTextField(routine.schedule.intervalAmount.toString(), { it.toIntOrNull()?.let { amount -> onChange(routine.copy(schedule = routine.schedule.copy(intervalAmount = amount.coerceAtLeast(1)))) } }, label = { Text(localizedText("Every", "每隔")) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { haptic.play(HapticEffect.Light); onSave() }, enabled = routine.name.isNotBlank() && routine.prompt.isNotBlank()) { Text(localizedText("Save", "保存")) }
                    onDelete?.let { TextButton(onClick = { confirmDelete = true }) { Text(localizedText("Delete", "删除"), color = MaterialTheme.colorScheme.error) } }
                }
                OperationStatus(operation)
            }
        }
        if (confirmDelete) {
            AlertDialog(
                onDismissRequest = { confirmDelete = false },
                title = { Text(localizedText("Delete routine?", "删除例行任务？")) },
                text = { Text(localizedText("This removes the routine and its schedule from the server.", "这会从服务器删除例行任务及其计划。")) },
                confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete?.invoke() }) { Text(localizedText("Delete", "删除"), color = MaterialTheme.colorScheme.error) } },
                dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(localizedText("Cancel", "取消")) } },
            )
        }
    }
}

@Composable
fun AccountMemoryScreen(
    state: FeatureLoadState<AccountMemoryState>,
    onRetry: () -> Unit,
    onEditMemory: (MemoryItem) -> Unit,
    onClearMemory: () -> Unit,
    onReset: () -> Unit,
    onSignOut: () -> Unit,
    onBack: () -> Unit,
) {
    var pendingAction by remember { mutableStateOf<String?>(null) }
    FeaturePage(localizedText("Account and memory", "账户与记忆"), onBack = onBack) {
        LoadStateContent(state, onRetry) { account ->
            LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Text(account.profile.displayName, style = MaterialTheme.typography.titleLarge)
                    Text(account.profile.server, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(if (account.profile.connected) localizedText("Connected", "已连接") else localizedText("Offline", "离线"))
                    Text(localizedText("Version ${account.profile.version}", "版本 ${account.profile.version}"), style = MaterialTheme.typography.labelSmall)
                }
                item { Text(localizedText("Memory", "记忆"), style = MaterialTheme.typography.titleMedium) }
                if (account.memories.isEmpty()) item { Text(localizedText("No saved memories yet.", "还没有保存的记忆。"), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                items(account.memories, key = { it.id }) { memory ->
                    Card(Modifier.fillMaxWidth().clickable { onEditMemory(memory) }, colors = featureCardColors()) {
                        Column(Modifier.padding(12.dp)) { Text(memory.title, fontWeight = FontWeight.Medium); Text(memory.content, maxLines = 3); Text(memory.updatedAt, style = MaterialTheme.typography.labelSmall) }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { pendingAction = "clear" }) { Text(localizedText("Clear memory", "清除记忆")) }
                        TextButton(onClick = { pendingAction = "reset" }) {
                            Text(localizedText("Reset local data", "重置本地数据"), color = MaterialTheme.colorScheme.error)
                        }
                    }
                    Button(onClick = { pendingAction = "sign-out" }) { Text(localizedText("Sign out", "退出登录")) }
                }
            }
        }
        pendingAction?.let { action ->
            val title = when (action) { "clear" -> localizedText("Clear memory?", "清除记忆？"); "reset" -> localizedText("Reset local data?", "重置本地数据？"); else -> localizedText("Sign out?", "退出登录？") }
            val detail = when (action) { "clear" -> localizedText("Saved memories will be removed from the server.", "服务器将删除已保存的记忆。"); "reset" -> localizedText("Local drafts and feature state will be cleared.", "本地草稿和功能状态将被清除。"); else -> localizedText("You will need to sign in again to use this server.", "你需要重新登录才能使用此服务器。") }
            AlertDialog(
                onDismissRequest = { pendingAction = null },
                title = { Text(title) },
                text = { Text(detail) },
                confirmButton = {
                    TextButton(onClick = {
                        when (action) { "clear" -> onClearMemory(); "reset" -> onReset(); else -> onSignOut() }
                        pendingAction = null
                    }) { Text(localizedText("Continue", "继续"), color = if (action == "sign-out") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary) }
                },
                dismissButton = { TextButton(onClick = { pendingAction = null }) { Text(localizedText("Cancel", "取消")) } },
            )
        }
    }
}

@Composable
fun SettingsScreen(
    settings: AppSettings,
    accountName: String,
    server: String,
    networkSharing: ProviderSnapshot,
    onToggleNetworkSharing: (Boolean) -> Unit,
    onNetworkSharingDetails: () -> Unit,
    androidUpdateState: AndroidUpdateState,
    onCheckForAndroidUpdate: () -> Unit,
    onDownloadAndroidUpdate: () -> Unit,
    onInstallAndroidUpdate: () -> Unit,
    onChange: (AppSettings) -> Unit,
    onSignOut: () -> Unit,
    onBack: () -> Unit,
    onProviders: () -> Unit,
    onRealtimeVoice: () -> Unit,
    onWebProviders: () -> Unit,
    onOutbox: () -> Unit,
    onAccount: () -> Unit,
    onPlugins: () -> Unit,
    onSidebar: () -> Unit,
) {
    val haptic = LocalAppHaptics.current
    var confirmSignOut by remember { mutableStateOf(false) }
    val language = settings.language
    val text: (String, String) -> String = { english, chinese ->
        if (language == AppSettings.Language.Chinese) chinese else english
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = settingsTopBarColors(),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = text("Back", "返回"))
                    }
                },
                title = {
                    Column {
                        Text(text("Settings", "设置"))
                        Text(
                            text("Application preferences", "应用偏好"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        },
        containerColor = settingsCanvas(),
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize()
                .pointerInput(Unit) {
                    // Mirror the way settings opens: dragging it back to the left closes it.
                    val trigger = 72.dp.toPx()
                    var total = 0f
                    var fired = false
                    detectHorizontalDragGestures(onDragStart = { total = 0f; fired = false }) { _, delta ->
                        total += delta
                        if (!fired && total <= -trigger) { fired = true; onBack() }
                    }
                }
                .padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 10.dp, bottom = 32.dp),
        ) {
            item {
                SettingsGroup {
                    Row(Modifier.padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(44.dp).background(MaterialTheme.colorScheme.primary, androidx.compose.foundation.shape.CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                accountName.take(1).uppercase().ifBlank { "O" },
                                color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(accountName.ifBlank { "OutcomeGo" }, style = MaterialTheme.typography.bodyLarge)
                            Text(server, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text("Connected", "已连接"), fontSize = 13.sp, color = MaterialTheme.colorScheme.tertiary)
                        }
                    }
                }
            }

            item { SettingsSectionTitle(text("Updates", "更新")) }
            item {
                SettingsGroup {
                    AndroidUpdateRow(
                        state = androidUpdateState,
                        text = text,
                        onCheck = onCheckForAndroidUpdate,
                        onDownload = onDownloadAndroidUpdate,
                        onInstall = onInstallAndroidUpdate,
                    )
                }
            }

            item { SettingsSectionTitle(text("Appearance", "外观")) }
            item {
                SettingsGroup {
                    SettingsRow(text("Theme", "主题"), "", inline = false) {
                        SettingsSegmented(
                            choices = AppSettings.Appearance.entries,
                            selected = settings.appearance,
                            label = { choice ->
                                when (choice) {
                                    AppSettings.Appearance.System -> text("System", "系统")
                                    AppSettings.Appearance.Light -> text("Light", "浅色")
                                    AppSettings.Appearance.Dark -> text("Dark", "深色")
                                }
                            },
                            onSelected = { onChange(settings.copy(appearance = it)) },
                        )
                    }
                    SettingsDivider()
                    SettingsRow(text("Accent", "强调色"), "", inline = false) {
                        SettingsSegmented(
                            choices = AppSettings.Accent.entries,
                            selected = settings.accent,
                            label = { choice ->
                                when (choice) {
                                    AppSettings.Accent.Black -> text("Black", "黑色")
                                    AppSettings.Accent.Blue -> text("Blue", "蓝色")
                                    AppSettings.Accent.Purple -> text("Purple", "紫色")
                                    AppSettings.Accent.Green -> text("Green", "绿色")
                                }
                            },
                            onSelected = { onChange(settings.copy(accent = it)) },
                        )
                    }
                }
            }

            item { SettingsSectionTitle(text("Language", "语言")) }
            item {
                SettingsGroup {
                    SettingsRow(text("Interface language", "界面语言"), "", inline = false) {
                        SettingsSegmented(
                            choices = AppSettings.Language.entries,
                            selected = language,
                            label = { choice -> if (choice == AppSettings.Language.Chinese) "中文" else "English" },
                            onSelected = { onChange(settings.copy(language = it)) },
                        )
                    }
                }
            }

            item { SettingsSectionTitle(text("Preferences", "偏好设置")) }
            item {
                SettingsGroup {
                    SettingsRow(text("Time zone", "时区"), text("Used for routines and timestamps", "用于例行任务和时间戳"), inline = false) {
                        SettingsTextField(settings.timeZone, { onChange(settings.copy(timeZone = it)) }, "Asia/Shanghai")
                    }
                    SettingsDivider()
                    SettingsRow(text("App haptics", "应用触感反馈"), text("Vibrate for important interactions", "重要交互时提供震动反馈")) {
                        Switch(checked = settings.haptics, onCheckedChange = { onChange(settings.copy(haptics = it)); if (it) haptic.play(HapticEffect.Selection) })
                    }
                }
            }

            item { SettingsSectionTitle(text("Notifications", "通知")) }
            item {
                SettingsGroup {
                    val enabled = settings.notifications.messages ||
                        settings.notifications.mentions ||
                        settings.notifications.routineRuns
                    SettingsToggleRow(
                        text("Notifications", "通知"),
                        text("Notify about activity from this server", "接收此服务器的活动通知"),
                        enabled,
                    ) {
                        onChange(settings.copy(notifications = NotificationPreferences(it, it, it)))
                    }
                }
            }

            item { SettingsSectionTitle(text("Connections", "连接")) }
            item {
                SettingsGroup {
                    val sharing = networkSharing.state != ProviderState.STOPPED
                    val status = when (networkSharing.state) {
                        ProviderState.READY -> text("Sharing is active", "共享已开启")
                        ProviderState.CONNECTING, ProviderState.ENROLLING, ProviderState.RECONNECTING -> text("Connecting…", "正在连接…")
                        ProviderState.UNAVAILABLE -> text("Temporarily unavailable", "暂时不可用")
                        ProviderState.REVOKED -> text("Authorization revoked", "授权已撤销")
                        ProviderState.FAILED -> text("Needs attention", "需要处理")
                        else -> text("Allow this device to provide public network access", "允许此设备提供公共网络出口")
                    }
                    SettingsToggleRow(
                        text("Network sharing", "网络共享"),
                        status,
                        sharing,
                        onToggleNetworkSharing,
                    )
                    SettingsDivider()
                    SettingsActionRow(
                        text("Cloud computer egress", "云端电脑出口"),
                        text("View route, proxy address, traffic, and speed", "查看路由、代理地址、流量和速度"),
                        onNetworkSharingDetails,
                    )
                    SettingsDivider()
                    SettingsActionRow(text("Message outbox", "消息发件箱"), text("Retry messages that could not be delivered", "重试无法投递的消息"), onOutbox)
                    SettingsDivider()
                    SettingsActionRow(text("Account and memory", "账户与记忆"), text("Review account data and saved memories", "查看账户数据和已保存的记忆"), onAccount)
                    SettingsDivider()
                    SettingsActionRow(text("Plugins", "插件"), text("Manage installed connections and tools", "管理已安装的连接和工具"), onPlugins)
                    SettingsDivider()
                    SettingsActionRow(text("Organize conversations", "整理会话"), text("Rename sections and manage hidden conversations", "重命名分区并管理隐藏会话"), onSidebar)
                    SettingsDivider()
                    SettingsActionRow(text("AI model", "AI 模型"), text("Choose the provider and model used by bots", "选择 Bot 使用的服务商和模型"), onProviders)
                    SettingsDivider()
                    SettingsActionRow(text("Realtime voice", "实时语音"), text("Configure speech providers for realtime conversations", "配置实时对话的语音服务商"), onRealtimeVoice)
                    SettingsDivider()
                    SettingsActionRow(text("Web providers", "网络服务商"), text("Choose search and page-fetch services", "选择搜索和网页抓取服务"), onWebProviders)
                }
            }

            item {
                SettingsGroup {
                    Box(Modifier.fillMaxWidth().clickable { confirmSignOut = true }.padding(vertical = 15.dp), contentAlignment = Alignment.Center) {
                        Text(text("Sign out", "退出登录"), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            title = { Text(text("Sign out?", "退出登录？")) },
            text = { Text(text("You will need to sign in again to use this server.", "你需要重新登录才能使用此服务器。")) },
            confirmButton = {
                TextButton(onClick = { confirmSignOut = false; onSignOut() }) {
                    Text(text("Sign out", "退出登录"), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmSignOut = false }) { Text(text("Cancel", "取消")) } },
        )
    }
}

@Composable
private fun AndroidUpdateRow(
    state: AndroidUpdateState,
    text: (String, String) -> String,
    onCheck: () -> Unit,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
) {
    val (title, description, action, enabled, onClick) = when (state) {
        AndroidUpdateState.Idle -> UpdateRowContent(
            text("Android app", "Android 应用"),
            text("Check GitHub for a signed update.", "检查 GitHub 上是否有已签名更新。"),
            text("Check for updates", "检查更新"),
            true,
            onCheck,
        )
        AndroidUpdateState.Checking -> UpdateRowContent(
            text("Checking for updates", "正在检查更新"),
            text("Verifying the signed GitHub release manifest.", "正在验证 GitHub 已签名发布清单。"),
            text("Checking…", "检查中…"),
            false,
            {},
        )
        is AndroidUpdateState.Available -> UpdateRowContent(
            text("Update available", "有可用更新"),
            text("OutcomeGo ${state.manifest.version} is ready to download.", "OutcomeGo ${state.manifest.version} 已准备下载。"),
            text("Download", "下载"),
            true,
            onDownload,
        )
        is AndroidUpdateState.Downloading -> UpdateRowContent(
            text("Downloading update", "正在下载更新"),
            text("The APK is verified before it can be installed.", "APK 会在安装前完成验证。"),
            state.progress?.let { "$it%" } ?: text("Downloading…", "下载中…"),
            false,
            {},
        )
        is AndroidUpdateState.Ready -> UpdateRowContent(
            text("Update ready", "更新已准备好"),
            text("Install OutcomeGo ${state.manifest.version}.", "安装 OutcomeGo ${state.manifest.version}。"),
            text("Install", "安装"),
            true,
            onInstall,
        )
        is AndroidUpdateState.NeedsInstallPermission -> UpdateRowContent(
            text("Allow APK installs", "允许安装 APK"),
            text("Allow installs from OutcomeGo, then tap Install again.", "允许 OutcomeGo 安装应用，然后再次点击安装。"),
            text("Allow installs", "允许安装"),
            true,
            onInstall,
        )
        is AndroidUpdateState.Installing -> UpdateRowContent(
            text("Installing update", "正在安装更新"),
            text("Android will finish the installation in its system installer.", "Android 将在系统安装器中完成安装。"),
            text("Installing…", "安装中…"),
            false,
            {},
        )
        is AndroidUpdateState.UpToDate -> UpdateRowContent(
            text("You're up to date", "已是最新版本"),
            text("Installed version ${state.currentVersion}.", "当前版本 ${state.currentVersion}。"),
            text("Check again", "再次检查"),
            true,
            onCheck,
        )
        is AndroidUpdateState.Error -> UpdateRowContent(
            text("Update check failed", "更新检查失败"),
            state.message,
            text("Retry", "重试"),
            true,
            onCheck,
        )
    }

    SettingsRow(title, description, inline = false) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
            if (state is AndroidUpdateState.Downloading) {
                LinearProgressIndicator(
                    progress = { (state.progress ?: 0) / 100f },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
            }
            Button(onClick = onClick, enabled = enabled) {
                if (state is AndroidUpdateState.Checking || state is AndroidUpdateState.Installing) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text(action)
            }
        }
    }
}

private data class UpdateRowContent(
    val title: String,
    val description: String,
    val action: String,
    val enabled: Boolean,
    val onClick: () -> Unit,
)

/** iOS-style settings palette: grey canvas, white outlined cards. */
@Composable
private fun settingsDark() = MaterialTheme.colorScheme.background.luminance() < 0.5f

@Composable
private fun settingsCanvas() = if (settingsDark()) Color(0xFF1C1C1C) else Color(0xFFF2F2F7)

@Composable
private fun settingsCard() = if (settingsDark()) Color(0xFF272629) else Color.White

@Composable
private fun settingsBorder() = if (settingsDark()) Color(0xFF333235) else Color(0xFFE4E4E8)

@Composable
private fun settingsTopBarColors() = TopAppBarDefaults.topAppBarColors(
    containerColor = settingsCanvas(),
    scrolledContainerColor = settingsCanvas(),
)

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        title,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 18.dp, top = 18.dp, end = 18.dp),
    )
}

@Composable
private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(settingsCard()).border(1.dp, settingsBorder(), shape),
        content = content,
    )
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(Modifier.padding(start = 18.dp), thickness = 0.5.dp, color = settingsBorder())
}

@Composable
private fun SettingsRowText(title: String, description: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        if (description.isNotBlank()) {
            Text(description, fontSize = 13.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingsRow(title: String, description: String, inline: Boolean = true, content: @Composable () -> Unit) {
    val rowModifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp)
    if (!inline) {
        Column(rowModifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingsRowText(title, description)
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) { content() }
        }
    } else {
        Row(rowModifier, horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            SettingsRowText(title, description, Modifier.weight(1f))
            content()
        }
    }
}

@Composable
private fun SettingsToggleRow(title: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val haptic = LocalAppHaptics.current
    SettingsRow(title, description) {
        Switch(checked = checked, onCheckedChange = { haptic.play(HapticEffect.Selection); onCheckedChange(it) })
    }
}

/** A tappable navigation row with a trailing chevron, like an iOS NavigationLink. */
@Composable
private fun SettingsActionRow(title: String, description: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsRowText(title, description, Modifier.weight(1f))
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun <T> SettingsSegmented(choices: List<T>, selected: T, label: (T) -> String, onSelected: (T) -> Unit) {
    val haptic = LocalAppHaptics.current
    val track = RoundedCornerShape(12.dp)
    val trackColor = if (settingsDark()) Color(0xFF3A393D) else Color(0xFFEFEFF4)
    Row(
        Modifier.fillMaxWidth().clip(track).background(trackColor).padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        choices.forEach { choice ->
            val active = choice == selected
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(9.dp))
                    .background(if (active) settingsCard() else Color.Transparent)
                    .clickable { if (!active) haptic.play(HapticEffect.Selection); onSelected(choice) }.padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label(choice),
                    fontSize = 14.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SettingsTextField(value: String, onChange: (String) -> Unit, placeholder: String, secret: Boolean = false) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)) },
        visualTransformation = if (secret) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = settingsCanvas(),
            unfocusedContainerColor = settingsCanvas(),
            unfocusedBorderColor = Color.Transparent,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun <T> ChoiceChips(choices: List<T>, selected: T, label: (T) -> String, onSelected: (T) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        choices.forEach { choice ->
            FilterChip(
                selected = choice == selected,
                onClick = { onSelected(choice) },
                label = { Text(label(choice)) },
            )
        }
    }
}

@Composable
private fun <T> ChoiceRow(title: String, choices: List<T>, selected: T, onSelected: (T) -> Unit, label: (T) -> String) {
    Column { Text(title, style = MaterialTheme.typography.labelLarge); choices.forEach { choice -> Row(verticalAlignment = Alignment.CenterVertically) { RadioButton(selected = choice == selected, onClick = { onSelected(choice) }); Text(label(choice)) } } }
}

@Composable
private fun NotificationToggle(label: String, enabled: Boolean, onChange: (Boolean) -> Unit) {
    val haptic = LocalAppHaptics.current
    Row(verticalAlignment = Alignment.CenterVertically) { Text(label, modifier = Modifier.weight(1f)); Switch(checked = enabled, onCheckedChange = { haptic.play(HapticEffect.Selection); onChange(it) }) }
}

@Composable
fun ProviderSettingsScreen(
    state: FeatureLoadState<List<ProviderConnection>>,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    language: AppSettings.Language,
    onConnect: (ProviderConnection, String?) -> Unit,
    onDisconnect: (ProviderConnection) -> Unit,
    onRemove: (ProviderConnection) -> Unit,
    loadPresets: suspend () -> Result<List<ProviderPreset>>,
    onAdd: suspend (ProviderPreset?, String, String, ProviderProtocol, String?) -> Result<String>,
    onModelChange: (ProviderConnection, String) -> Unit,
    onReasoningChange: (ProviderConnection, String) -> Unit,
) {
    var pendingDisconnect by remember { mutableStateOf<ProviderConnection?>(null) }
    var pendingRemove by remember { mutableStateOf<ProviderConnection?>(null) }
    var adding by remember { mutableStateOf(false) }
    val text: (String, String) -> String = { english, chinese -> if (language == AppSettings.Language.Chinese) chinese else english }
    FeaturePage(text("AI model", "AI 模型"), onBack = onBack, settingsStyle = true) {
        LoadStateContent(state, onRetry) { providers ->
            LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 14.dp, bottom = 28.dp)) {
                item { SettingsSectionTitle(text("Choose the provider and model used by bots.", "选择 Bot 使用的服务商和模型。")) }
                items(providers, key = { it.id }) { provider ->
                    SettingsGroup {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ProviderIcon(provider.icon)
                                Spacer(Modifier.width(10.dp))
                                Text(provider.name, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                                Text(
                                    if (provider.connected) text("Connected", "已连接") else text("Disconnected", "未连接"),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (provider.connected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            provider.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                            if (!provider.connected) {
                                if (provider.authMode == AuthMode.ApiKey) {
                                    var apiKey by remember(provider.id) { mutableStateOf("") }
                                    OutlinedTextField(apiKey, { apiKey = it }, label = { Text(if (provider.apiKeySaved) text("Replace API key", "替换 API 密钥") else text("API key", "API 密钥")) }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), singleLine = true)
                                    Button(onClick = { onConnect(provider.copy(apiKeySaved = true), apiKey.takeIf { it.isNotBlank() }); apiKey = "" }, enabled = apiKey.isNotBlank() || provider.apiKeySaved) { Text(text("Save and connect", "保存并连接")) }
                                } else {
                                    Button(onClick = { onConnect(provider, null) }) { Text(if (provider.authMode == AuthMode.OAuth) text("Connect with OAuth", "使用 OAuth 连接") else text("Connect", "连接")) }
                                }
                            }
                            else {
                                SettingsRow(text("Model", "模型"), text("Used by this provider", "此服务商使用的模型"), inline = false) {
                                    ChoiceChips(provider.models, provider.selectedModel ?: provider.models.firstOrNull().orEmpty(), { it }) { onModelChange(provider, it) }
                                }
                                SettingsRow(text("Reasoning", "推理强度"), text("How much effort the model uses", "模型使用的推理强度"), inline = false) {
                                    ChoiceChips(provider.reasoningLevels, provider.selectedReasoning ?: provider.reasoningLevels.firstOrNull().orEmpty(), { it }) { onReasoningChange(provider, it) }
                                }
                                Row {
                                    if (provider.custom) TextButton(onClick = { pendingRemove = provider }) { Text(text("Remove", "移除"), color = MaterialTheme.colorScheme.error) }
                                    TextButton(onClick = { pendingDisconnect = provider }) { Text(text("Disconnect", "断开连接"), color = MaterialTheme.colorScheme.error) }
                                }
                            }
                        }
                    }
                }
                item {
                    OutlinedButton(onClick = { adding = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(text("Add provider", "添加服务商"))
                    }
                }
            }
        }
        pendingDisconnect?.let { provider ->
            AlertDialog(
                onDismissRequest = { pendingDisconnect = null },
                title = { Text(text("Disconnect ${provider.name}?", "断开 ${provider.name}？")) },
                text = { Text(text("The server will remove this provider connection and its selected model.", "服务器将移除此服务商连接及其已选模型。")) },
                confirmButton = { TextButton(onClick = { pendingDisconnect = null; onDisconnect(provider) }) { Text(text("Disconnect", "断开连接"), color = MaterialTheme.colorScheme.error) } },
                dismissButton = { TextButton(onClick = { pendingDisconnect = null }) { Text(text("Cancel", "取消")) } },
            )
        }
        pendingRemove?.let { provider ->
            AlertDialog(
                onDismissRequest = { pendingRemove = null },
                title = { Text(text("Remove ${provider.name}?", "移除 ${provider.name}？")) },
                text = { Text(text("Its address and key are deleted from your server.", "它的地址和密钥会从你的服务器上删除。")) },
                confirmButton = { TextButton(onClick = { pendingRemove = null; onRemove(provider) }) { Text(text("Remove", "移除"), color = MaterialTheme.colorScheme.error) } },
                dismissButton = { TextButton(onClick = { pendingRemove = null }) { Text(text("Cancel", "取消")) } },
            )
        }
        if (adding) AddProviderSheet(text = text, loadPresets = loadPresets, onAdd = onAdd, onDismiss = { adding = false })
    }
}

/**
 * A vendor mark from the provider icon set (scripts/icons/export-provider-icons.ts). Names ending in
 * "-color" keep their colours; the rest are black on transparent and take the text colour.
 */
@Composable
private fun ProviderIcon(name: String?, size: androidx.compose.ui.unit.Dp = 20.dp) {
    val context = LocalContext.current
    val id = remember(name) { name?.let { context.resources.getIdentifier("provider_" + it.replace('-', '_'), "drawable", context.packageName) } ?: 0 }
    if (id == 0) {
        Spacer(Modifier.size(size))
        return
    }
    androidx.compose.foundation.Image(
        painter = painterResource(id),
        contentDescription = null,
        modifier = Modifier.size(size),
        contentScale = ContentScale.Fit,
        colorFilter = if (name!!.endsWith("-color")) null else ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
    )
}

private val providerGroups = listOf(
    "vendor" to ("Model makers" to "模型厂商"),
    "cloud" to ("Cloud platforms" to "云平台"),
    "relay" to ("Hosts and gateways" to "托管与网关"),
    "local" to ("On this server" to "在此服务器上"),
)

/** Adds a provider over a standard protocol. The server keeps the key and removes the provider again if the key is refused. */
@Composable
private fun AddProviderSheet(
    text: (String, String) -> String,
    loadPresets: suspend () -> Result<List<ProviderPreset>>,
    onAdd: suspend (ProviderPreset?, String, String, ProviderProtocol, String?) -> Result<String>,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    var presets by remember { mutableStateOf<List<ProviderPreset>>(emptyList()) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var preset by remember { mutableStateOf<ProviderPreset?>(null) }
    var protocol by remember { mutableStateOf(ProviderProtocol.OpenAiChat) }
    var name by remember { mutableStateOf("") }
    var baseUrl by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }
    var failure by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var serviceMenu by remember { mutableStateOf(false) }
    var protocolMenu by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        loadPresets().onSuccess { presets = it }.onFailure { loadError = it.message ?: text("Could not load the provider list", "无法加载服务商列表") }
    }
    // The key field is shown unless the server never takes one; an optional key may stay empty.
    val showsKey = preset?.noKey != true
    val withoutKey = !showsKey || (preset?.keyOptional == true && apiKey.isBlank())
    val protocols = preset?.protocols ?: ProviderProtocol.values().toList()
    val ready = name.isNotBlank() && baseUrl.isNotBlank() && (withoutKey || apiKey.isNotBlank())
    fun pick(next: ProviderPreset?) {
        preset = next
        failure = null
        if (next != null) {
            protocol = next.defaultProtocol
            name = next.name
            baseUrl = next.baseUrl(protocol)
        }
    }
    ModalBottomSheet(onDismissRequest = { if (!busy) onDismiss() }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 20.dp).imePadding().navigationBarsPadding().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text("Add provider", "添加服务商"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(
                text("Connect any service that speaks OpenAI Chat, OpenAI Responses or Anthropic Messages. The key stays on your server.", "连接任何支持 OpenAI Chat、OpenAI Responses 或 Anthropic Messages 的服务。密钥只保存在你的服务器上。"),
                fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Box {
                OutlinedButton(onClick = { serviceMenu = true }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                    ProviderIcon(preset?.icon ?: protocol.genericIcon, 18.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(preset?.name ?: text("Custom endpoint", "自定义地址"), Modifier.weight(1f))
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null)
                }
                DropdownMenu(expanded = serviceMenu, onDismissRequest = { serviceMenu = false }, modifier = Modifier.heightIn(max = 420.dp)) {
                    DropdownMenuItem(
                        leadingIcon = { ProviderIcon(protocol.genericIcon, 18.dp) },
                        text = { Text(text("Custom endpoint", "自定义地址")) },
                        onClick = { serviceMenu = false; pick(null) },
                    )
                    providerGroups.forEach { (group, titles) ->
                        val members = presets.filter { it.group == group }
                        if (members.isNotEmpty()) {
                            Text(text(titles.first, titles.second), Modifier.padding(horizontal = 16.dp, vertical = 6.dp), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            members.forEach { member -> DropdownMenuItem(leadingIcon = { ProviderIcon(member.icon, 18.dp) }, text = { Text(member.name) }, onClick = { serviceMenu = false; pick(member) }) }
                        }
                    }
                }
            }
            loadError?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
            Box {
                OutlinedButton(onClick = { protocolMenu = true }, enabled = !busy && protocols.size > 1, modifier = Modifier.fillMaxWidth()) {
                    Text(protocol.label, Modifier.weight(1f))
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null)
                }
                DropdownMenu(expanded = protocolMenu, onDismissRequest = { protocolMenu = false }) {
                    protocols.forEach { option ->
                        DropdownMenuItem(text = { Text(option.label) }, onClick = {
                            protocolMenu = false
                            protocol = option
                            // A vendor documents an address per protocol; a typed address stays as it is.
                            preset?.takeIf { it.endpoints.containsKey(option.wire) }?.let { baseUrl = it.baseUrl(option) }
                        })
                    }
                }
            }
            OutlinedTextField(name, { name = it }, label = { Text(text("Name", "名称")) }, enabled = !busy, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                baseUrl, { baseUrl = it }, label = { Text(text("API base URL", "API 基础地址")) }, enabled = !busy, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri), textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                modifier = Modifier.fillMaxWidth(),
            )
            if (showsKey) {
                OutlinedTextField(
                    apiKey, { apiKey = it }, label = { Text(if (preset?.keyOptional == true) text("API key (optional)", "API 密钥（可选）") else text("API key", "API 密钥")) }, enabled = !busy, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                )
                preset?.keysUrl?.let { url -> TextButton(onClick = { uriHandler.openUri(url) }) { Text(text("Get a key", "获取密钥")) } }
            }
            if (preset?.noKey == true || preset?.keyOptional == true) preset?.note?.let { Text(it, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            failure?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDismiss, enabled = !busy) { Text(text("Cancel", "取消")) }
                Button(
                    enabled = ready && !busy,
                    onClick = {
                        busy = true
                        failure = null
                        scope.launch {
                            onAdd(preset, name.trim(), baseUrl.trim(), protocol, if (withoutKey) null else apiKey.trim())
                                .onSuccess { onDismiss() }
                                .onFailure { failure = it.message ?: text("Could not add this provider", "无法添加此服务商") }
                            busy = false
                        }
                    },
                ) {
                    if (busy) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text(if (busy) text("Checking the key…", "正在检查密钥…") else text("Add", "添加"))
                }
            }
        }
    }
}

/** Display-only mirror of the server's per-provider defaults, shown as placeholders for blank fields. */
private data class VoiceDefaults(val sttModel: String, val ttsModel: String, val voice: String, val language: String)

private val volcengineDefaults = VoiceDefaults("bigmodel", "", "zh_female_vv_uranus_bigtts", "zh-CN")
private val deepgramDefaults = VoiceDefaults("nova-3", "aura-2-thalia-en", "alloy", "en-US")

@Composable
fun VoiceSettingsScreen(
    state: FeatureLoadState<VoiceSettings>,
    language: AppSettings.Language,
    onRetry: () -> Unit,
    onSave: (VoiceSettings, String?, String?, Boolean, Boolean) -> Unit,
    onBack: () -> Unit,
) {
    val text: (String, String) -> String = { english, chinese -> if (language == AppSettings.Language.Chinese) chinese else english }
    var draft by remember { mutableStateOf<VoiceSettings?>(null) }
    var apiKey by remember { mutableStateOf("") }
    var removeKey by remember { mutableStateOf(false) }
    var advancedOpen by remember { mutableStateOf(false) }
    LaunchedEffect(state) { if (state is FeatureLoadState.Ready) draft = state.value }
    FeaturePage(text("Realtime voice", "实时语音"), onBack, settingsStyle = true) {
        LoadStateContent(state, onRetry) { loaded ->
            val value = draft ?: loaded
            val volcengine = value.defaultProvider == "volcengine"
            val provider = if (volcengine) value.volcengine else value.deepgram
            val providerName = if (volcengine) "Volcengine" else "Deepgram"
            val defaults = if (volcengine) volcengineDefaults else deepgramDefaults
            val changed = value != loaded || apiKey.isNotBlank() || removeKey
            fun update(next: VoiceSettings) { draft = next }
            fun updateProvider(change: (VoiceProviderSettings) -> VoiceProviderSettings) {
                update(if (volcengine) value.copy(volcengine = change(value.volcengine)) else value.copy(deepgram = change(value.deepgram)))
            }
            LazyColumn(
                Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 10.dp, bottom = 32.dp),
            ) {
                item {
                    SettingsGroup {
                        SettingsToggleRow(text("Enable realtime voice", "启用实时语音"), text("Allow authenticated speech sessions from macOS and Android", "允许 macOS 和 Android 建立认证语音会话"), value.enabled) { update(value.copy(enabled = it)) }
                    }
                }
                item { SettingsSectionTitle(text("Provider", "服务商")) }
                item {
                    SettingsGroup {
                        Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            SettingsSegmented(listOf("volcengine", "deepgram"), value.defaultProvider, { if (it == "volcengine") "Volcengine" else "Deepgram" }) {
                                update(value.copy(defaultProvider = it))
                                apiKey = ""
                                removeKey = false
                            }
                            VoiceStatus(
                                ok = provider.configured,
                                label = if (provider.configured) text("$providerName is configured.", "$providerName 已配置。") else text("$providerName needs an API key.", "$providerName 需要 API 密钥。"),
                            )
                        }
                        SettingsDivider()
                        Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(text("$providerName API key", "$providerName API 密钥"), fontWeight = FontWeight.Medium)
                            Text(
                                when {
                                    removeKey -> text("The saved key will be deleted when you save.", "保存时将删除已保存的密钥。")
                                    provider.configured -> text("Enter a new key to replace the saved key.", "输入新密钥以替换已保存的密钥。")
                                    else -> text("The key is stored encrypted on the server.", "密钥将加密存储在服务器上。")
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            SettingsTextField(
                                value = apiKey,
                                onChange = { apiKey = it; if (it.isNotBlank()) removeKey = false },
                                placeholder = if (provider.configured) "••••${provider.apiKeySuffix.orEmpty()}" else text("API key", "API 密钥"),
                                secret = true,
                            )
                            if (provider.configured) {
                                TextButton(onClick = { removeKey = !removeKey; if (removeKey) apiKey = "" }, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp)) {
                                    Text(if (removeKey) text("Undo deletion", "撤销删除") else text("Delete key", "删除密钥"), color = if (removeKey) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
                item {
                    SettingsGroup {
                        Row(
                            Modifier.fillMaxWidth().clickable { advancedOpen = !advancedOpen }.padding(horizontal = 18.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(text("Advanced settings", "高级设置"), fontWeight = FontWeight.Medium)
                                Text(text("Provider endpoints, resource IDs, models, and speech defaults", "服务商端点、资源 ID、模型和语音默认值"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(if (advancedOpen) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (advancedOpen) {
                    item { SettingsSectionTitle(text("$providerName connection", "$providerName 连接")) }
                    item {
                        SettingsGroup {
                            if (volcengine) {
                                VoiceField(text("Base URL", "基础 URL"), provider.baseUrl.orEmpty(), "https://openspeech.bytedance.com") { v -> updateProvider { it.copy(baseUrl = v) } }
                                SettingsDivider()
                                VoiceField(text("STT WebSocket URL", "STT WebSocket URL"), provider.sttUrl.orEmpty(), text("Optional provider-specific URL", "可选，服务商专用 URL")) { v -> updateProvider { it.copy(sttUrl = v) } }
                                SettingsDivider()
                                VoiceField(text("TTS WebSocket URL", "TTS WebSocket URL"), provider.ttsUrl.orEmpty(), text("Optional provider-specific URL", "可选，服务商专用 URL")) { v -> updateProvider { it.copy(ttsUrl = v) } }
                                SettingsDivider()
                                VoiceField(text("STT resource ID", "STT 资源 ID"), provider.sttResourceId.orEmpty(), "volc.seedasr.sauc.duration") { v -> updateProvider { it.copy(sttResourceId = v) } }
                                SettingsDivider()
                                VoiceField(text("TTS resource ID", "TTS 资源 ID"), provider.ttsResourceId.orEmpty(), "seed-tts-2.0") { v -> updateProvider { it.copy(ttsResourceId = v) } }
                            } else {
                                VoiceField(text("Base URL", "基础 URL"), provider.baseUrl.orEmpty(), "https://api.deepgram.com") { v -> updateProvider { it.copy(baseUrl = v) } }
                            }
                        }
                    }
                    item { SettingsSectionTitle(text("Speech defaults", "语音默认值")) }
                    item {
                        SettingsGroup {
                            val automatic = text("Automatic", "自动")
                            VoiceField(text("STT model", "语音识别模型"), value.sttModel, defaults.sttModel.ifBlank { automatic }) { update(value.copy(sttModel = it)) }
                            SettingsDivider()
                            VoiceField(text("TTS model", "语音合成模型"), value.ttsModel, defaults.ttsModel.ifBlank { automatic }) { update(value.copy(ttsModel = it)) }
                            SettingsDivider()
                            VoiceField(text("TTS voice", "语音"), value.voice, defaults.voice.ifBlank { automatic }) { update(value.copy(voice = it)) }
                            SettingsDivider()
                            VoiceField(text("Language", "语言"), value.language, defaults.language.ifBlank { automatic }) { update(value.copy(language = it)) }
                            SettingsDivider()
                            VoiceField(
                                text("Narration model", "旁白模型"),
                                value.narrationModel,
                                text("Same as the bot", "与 Bot 相同"),
                                hint = text("Speaks short progress updates while the agent works. Use a fast model as provider/model. Leave empty to use the bot's own model.", "Agent 工作时朗读简短的进度更新。请使用 provider/model 格式的快速模型，留空则使用 Bot 自己的模型。"),
                            ) { update(value.copy(narrationModel = it)) }
                        }
                    }
                    item { Text(text("Leave fields empty to use the recommended values.", "字段留空将使用推荐值。"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 6.dp)) }
                }
                item {
                    Button(
                        onClick = {
                            onSave(value, if (volcengine) apiKey.takeIf { it.isNotBlank() } else null, if (volcengine) null else apiKey.takeIf { it.isNotBlank() }, volcengine && removeKey, !volcengine && removeKey)
                            apiKey = ""
                            removeKey = false
                        },
                        enabled = changed,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(50.dp).padding(top = 0.dp),
                    ) { Text(text("Save settings", "保存设置")) }
                }
            }
        }
    }
}

@Composable
private fun VoiceStatus(ok: Boolean, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(8.dp).clip(RoundedCornerShape(50)).background(if (ok) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error))
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun VoiceField(title: String, value: String, placeholder: String, hint: String? = null, onChange: (String) -> Unit) {
    Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontWeight = FontWeight.Medium)
        hint?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        SettingsTextField(value, onChange, placeholder)
    }
}

@Composable
fun WebProviderSettingsScreen(
    state: FeatureLoadState<WebProvidersState>,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    language: AppSettings.Language,
    onSelect: (tool: String, provider: WebProvider) -> Unit,
    onSaveKey: (tool: String, provider: WebProvider, key: String) -> Unit,
    onRemoveKey: (tool: String, provider: WebProvider) -> Unit,
    onCheck: (tool: String, provider: WebProvider) -> Unit,
) {
    val text: (String, String) -> String = { english, chinese -> if (language == AppSettings.Language.Chinese) chinese else english }
    FeaturePage(text("Web providers", "网络服务商"), onBack = onBack, settingsStyle = true) {
        LoadStateContent(state, onRetry) { providers ->
            LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 14.dp, bottom = 28.dp)) {
                item { WebProviderTool(text("Search", "搜索"), text("The service bots use to find information on the web.", "Bot 用于在网络上查找信息的服务。"), "search", providers.search, language, onSelect, onSaveKey, onRemoveKey, onCheck) }
                item { WebProviderTool(text("Fetch", "抓取"), text("How bots read a web page without opening their browser.", "Bot 不打开浏览器时读取网页的方式。"), "fetch", providers.fetch, language, onSelect, onSaveKey, onRemoveKey, onCheck) }
            }
        }
    }
}

@Composable
private fun WebProviderTool(
    title: String,
    description: String,
    tool: String,
    providers: List<WebProvider>,
    language: AppSettings.Language,
    onSelect: (String, WebProvider) -> Unit,
    onSaveKey: (String, WebProvider, String) -> Unit,
    onRemoveKey: (String, WebProvider) -> Unit,
    onCheck: (String, WebProvider) -> Unit,
) {
    val text: (String, String) -> String = { english, chinese -> if (language == AppSettings.Language.Chinese) chinese else english }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SettingsSectionTitle(title)
        Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        providers.forEach { provider ->
            var key by remember(provider.id) { mutableStateOf("") }
            var confirmRemove by remember(provider.id) { mutableStateOf(false) }
            SettingsGroup {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = provider.selected, onClick = { if (provider.ready || !provider.requiresApiKey) onSelect(tool, provider) })
                        Column(Modifier.weight(1f)) { Text(provider.name, fontWeight = FontWeight.Medium); Text(provider.description, style = MaterialTheme.typography.bodySmall) }
                        Text(if (provider.check?.passed == true) text("Checked", "已检查") else if (provider.requiresApiKey && !provider.ready) text("Add API key", "添加 API 密钥") else text("Not checked", "未检查"), style = MaterialTheme.typography.labelSmall)
                    }
                    if (provider.requiresApiKey) {
                        OutlinedTextField(key, { key = it }, label = { Text(if (provider.secretSaved) text("Replace API key", "替换 API 密钥") else text("API key", "API 密钥")) }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), singleLine = true)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Button(onClick = { onSaveKey(tool, provider, key); key = "" }, enabled = key.isNotBlank()) { Text(text("Save", "保存")) }
                            if (provider.secretSaved && !provider.selected) TextButton(onClick = { confirmRemove = true }) { Text(text("Remove key", "删除密钥")) }
                            TextButton(onClick = { onCheck(tool, provider) }, enabled = provider.ready || key.isNotBlank()) { Text(text("Check", "检查")) }
                        }
                    }
                    provider.check?.let { check -> Text(check.message, color = if (check.passed) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error) }
                    if (confirmRemove) {
                        AlertDialog(
                            onDismissRequest = { confirmRemove = false },
                            title = { Text(text("Remove API key?", "删除 API 密钥？")) },
                            text = { Text(text("The saved key for ${provider.name} will be removed from the server.", "服务器将删除 ${provider.name} 的已保存密钥。")) },
                            confirmButton = { TextButton(onClick = { confirmRemove = false; onRemoveKey(tool, provider) }) { Text(text("Remove", "删除"), color = MaterialTheme.colorScheme.error) } },
                            dismissButton = { TextButton(onClick = { confirmRemove = false }) { Text(text("Cancel", "取消")) } },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PluginsScreen(
    state: FeatureLoadState<List<Plugin>>,
    onRetry: () -> Unit,
    onInstall: (Plugin) -> Unit,
    onUpdate: (Plugin) -> Unit,
    onRollback: (Plugin) -> Unit,
    onAuthorize: (Plugin) -> Unit,
    onCancelAuthorization: (Plugin) -> Unit,
    onConfigure: (Plugin, JsonObject) -> Unit,
    onTestTool: (Plugin) -> Unit,
    onBack: () -> Unit,
) {
    FeaturePage(localizedText("Plugins", "插件"), onBack = onBack) {
        LoadStateContent(state, onRetry, emptyAction = { Text(localizedText("No plugins available.", "没有可用插件。")) }) { plugins ->
            var filter by remember { mutableStateOf("") }
            val authorizationPending = plugins.any { it.authorizationUrl != null }
            LaunchedEffect(authorizationPending) {
                while (authorizationPending) {
                    delay(2_000)
                    onRetry()
                }
            }
            Column(Modifier.fillMaxSize().padding(12.dp)) {
                OutlinedTextField(filter, { filter = it }, label = { Text(localizedText("Filter plugins", "筛选插件")) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(8.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(plugins.filter { filter.isBlank() || it.name.contains(filter, true) || it.description.contains(filter, true) }, key = { "${it.id}:${it.connectionId ?: "plugin"}" }) { plugin ->
                        var confirmRollback by remember(plugin.id, plugin.connectionId) { mutableStateOf(false) }
                        var configure by remember(plugin.id, plugin.connectionId) { mutableStateOf(false) }
                        var configuration by remember(plugin.id, plugin.connectionId) { mutableStateOf("{}") }
                        Card(Modifier.fillMaxWidth(), colors = featureCardColors()) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) { Text(plugin.name, Modifier.weight(1f), fontWeight = FontWeight.Bold); Text(plugin.installedVersion ?: "Not installed", style = MaterialTheme.typography.labelSmall) }
                                Text(plugin.description)
                                plugin.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (plugin.installedVersion == null) Button(onClick = { onInstall(plugin) }, enabled = !plugin.busy) { Text(localizedText("Install", "安装")) }
                                    else {
                                        if (plugin.latestVersion != null && plugin.latestVersion != plugin.installedVersion) TextButton(onClick = { onUpdate(plugin) }, enabled = !plugin.busy) { Text(localizedText("Update", "更新")) }
                                        TextButton(onClick = { confirmRollback = true }, enabled = !plugin.busy) { Text(localizedText("Rollback", "回滚")) }
                                        TextButton(onClick = { configure = true }, enabled = !plugin.busy) { Text(localizedText("Configure", "配置")) }
                                        TextButton(onClick = { onTestTool(plugin) }, enabled = !plugin.busy) { Text(localizedText("Test tool", "测试工具")) }
                                    }
                                    if (plugin.requiresOAuth && !plugin.configured && plugin.authorizationUrl == null) TextButton(onClick = { onAuthorize(plugin) }, enabled = !plugin.busy) { Text(localizedText("Authorize", "授权")) }
                                    if (plugin.authorizationUrl != null) {
                                        Text(localizedText("Waiting for browser sign-in", "等待浏览器登录"), style = MaterialTheme.typography.labelSmall)
                                        TextButton(onClick = { onCancelAuthorization(plugin) }, enabled = !plugin.busy) { Text(localizedText("Cancel", "取消")) }
                                    }
                                }
                                if (plugin.busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            }
                        }
                        if (configure) {
                            AlertDialog(
                                onDismissRequest = { configure = false },
                                title = { Text(localizedText("Configure ${plugin.name}", "配置 ${plugin.name}")) },
                                text = {
                                    OutlinedTextField(
                                        value = configuration,
                                        onValueChange = { configuration = it },
                                        label = { Text(localizedText("Configuration JSON", "配置 JSON")) },
                                        modifier = Modifier.fillMaxWidth(),
                                        minLines = 5,
                                    )
                                },
                                confirmButton = {
                                    TextButton(onClick = {
                                        runCatching { Json.parseToJsonElement(configuration).jsonObject }
                                            .onSuccess { onConfigure(plugin, it); configure = false }
                                    }) { Text(localizedText("Save", "保存")) }
                                },
                                dismissButton = { TextButton(onClick = { configure = false }) { Text(localizedText("Cancel", "取消")) } },
                            )
                        }
                        if (confirmRollback) {
                            AlertDialog(
                                onDismissRequest = { confirmRollback = false },
                                title = { Text(localizedText("Roll back ${plugin.name}?", "回滚 ${plugin.name}？")) },
                                text = { Text(localizedText("The server will restore the previous installed plugin version.", "服务器将恢复之前安装的插件版本。")) },
                                confirmButton = { TextButton(onClick = { confirmRollback = false; onRollback(plugin) }) { Text(localizedText("Roll back", "回滚"), color = MaterialTheme.colorScheme.error) } },
                                dismissButton = { TextButton(onClick = { confirmRollback = false }) { Text(localizedText("Cancel", "取消")) } },
                            )
                        }
                    }
                }
            }
        }
    }
}
