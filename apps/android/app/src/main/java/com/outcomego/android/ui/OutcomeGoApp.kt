@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.outcomego.android.ui

import android.Manifest
import android.os.Build
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.webkit.WebView
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.core.animateFloat
import com.outcomego.android.data.ChannelActivity
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.scaleOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.navigation.NavBackStackEntry
import com.outcomego.features.featureCardColors
import com.outcomego.features.featureTopBarColors
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.flow.drop
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Surface
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.outcomego.features.AndroidHaptics
import com.outcomego.features.HapticEffect
import com.outcomego.features.HapticPolicy
import com.outcomego.features.LocalAppHaptics
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.zIndex
import com.outcomego.android.data.RoutineEvent
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextOverflow
import kotlin.math.roundToInt
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.outcomego.android.AppViewModel
import com.outcomego.android.data.ChatMessage
import com.outcomego.android.data.ChatRow
import com.outcomego.android.data.toChatRows
import com.outcomego.android.data.Conversation
import com.outcomego.android.data.SearchResult
import com.outcomego.android.data.SidebarPreferences
import com.outcomego.android.data.SidebarSection
import com.outcomego.android.data.PendingSend
import com.outcomego.features.Attachment
import com.outcomego.features.AccountMemoryScreen
import com.outcomego.features.FeatureOperationState
import com.outcomego.features.FeatureLoadState
import com.outcomego.features.MessageAction
import com.outcomego.features.Plugin
import com.outcomego.features.ReactionState
import com.outcomego.features.ReactionsAndActionsRow
import com.outcomego.features.RichMessageCardScreen
import com.outcomego.features.isQuestion
import com.outcomego.features.ChatAttachmentStrip
import com.outcomego.features.FileGlyph
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.FileDownload
import com.outcomego.features.DocumentPreview
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.filled.Download
import com.outcomego.features.formatAttachmentSize
import com.outcomego.features.rememberDecodedBitmap
import com.outcomego.features.MarkdownMessage
import com.outcomego.features.PluginsScreen
import com.outcomego.features.ProviderSettingsScreen
import com.outcomego.features.RoutineEditorScreen
import com.outcomego.features.RoutinesScreen
import com.outcomego.features.SettingsScreen
import com.outcomego.features.NetworkSharingDetailsScreen
import com.outcomego.features.WebProviderSettingsScreen
import com.outcomego.features.VoiceSettingsScreen
import com.outcomego.features.LiveVoiceView
import com.outcomego.features.LocalAppLanguage
import com.outcomego.features.localizedText
import com.outcomego.features.AppSettings
import com.outcomego.platform.SharePayload
import com.outcomego.platform.VoiceRecordingState
import com.outcomego.platform.AndroidVncWebViewRenderer
import com.outcomego.platform.ComputerVncGrant
import com.outcomego.platform.VncRendererState
import com.outcomego.platform.NativeAttachmentPreview
import com.outcomego.platform.AndroidUpdateState
import com.outcomego.platform.AndroidNetworkProvider
import java.net.URI
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val LOGIN = "login"
private const val DIRECTORY = "directory"
private const val CHAT = "chat/{conversationId}"
private const val BOT_DETAILS = "bot/{conversationId}/details"
private const val PROFILE = "conversation/{conversationId}/profile"
private const val CREATE = "conversation/create"
private const val COMPUTER = "bot/{conversationId}/computer"
private const val MARKETPLACE = "marketplace"
private const val ROUTINES = "routines"
private const val ROUTINE_EDITOR = "routines/editor"
private const val SETTINGS = "settings"
private const val NETWORK_SHARING_DETAILS = "network-sharing-details"
private const val PROVIDERS = "providers"
private const val WEB_PROVIDERS = "web-providers"
private const val REALTIME_VOICE = "realtime-voice"
private const val PLUGINS = "plugins"
private const val OUTBOX = "outbox"
private const val SHARE = "share"
private const val ACCOUNT = "account"
private const val SIDEBAR = "sidebar"
private const val MESSAGE_SEARCH = "message-search"
private const val NAVIGATION_TRANSITION_DURATION_MILLIS = 280

private enum class SignInStep { WELCOME, ENDPOINT, CREDENTIALS }

private val NavigationSpec = tween<androidx.compose.ui.unit.IntOffset>(NAVIGATION_TRANSITION_DURATION_MILLIS, easing = FastOutSlowInEasing)

/** Settings is a left-edge panel: it opens from the left and closes back to the left. */
/** Microphone is required for a call; Bluetooth is only needed to route audio to a headset (Android 12+). */
private fun liveVoicePermissionRequest(): Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        arrayOf(Manifest.permission.RECORD_AUDIO)
    }

private fun AnimatedContentTransitionScope<NavBackStackEntry>.isSettingsOpening() =
    initialState.destination.route == DIRECTORY && targetState.destination.route == SETTINGS

private fun AnimatedContentTransitionScope<NavBackStackEntry>.isSettingsClosing() =
    initialState.destination.route == SETTINGS && targetState.destination.route == DIRECTORY

/** [forward] means content travels right-to-left (the normal push direction). */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.slideNav(forward: Boolean) = slideIntoContainer(
    if (forward) AnimatedContentTransitionScope.SlideDirection.Left else AnimatedContentTransitionScope.SlideDirection.Right,
    animationSpec = NavigationSpec,
)

private fun AnimatedContentTransitionScope<NavBackStackEntry>.slideNavOut(forward: Boolean) = slideOutOfContainer(
    if (forward) AnimatedContentTransitionScope.SlideDirection.Left else AnimatedContentTransitionScope.SlideDirection.Right,
    animationSpec = NavigationSpec,
)

@Composable
fun OutcomeGoApp(
    appViewModel: AppViewModel = viewModel(),
    androidUpdateState: AndroidUpdateState = AndroidUpdateState.Idle,
    onCheckForAndroidUpdate: () -> Unit = {},
    onDownloadAndroidUpdate: () -> Unit = {},
    onInstallAndroidUpdate: () -> Unit = {},
) {
    val state by appViewModel.state.collectAsStateWithLifecycle()
    val networkSharing by AndroidNetworkProvider.snapshot.collectAsStateWithLifecycle()
    val voiceState by appViewModel.voiceState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val context = LocalContext.current
    val microphonePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) appViewModel.startVoice()
    }
    var pendingLiveVoiceConversationId by remember { mutableStateOf<String?>(null) }
    // A group call is started from a member picker; the choice waits here while permissions are asked.
    var pendingLiveVoiceMembers by remember { mutableStateOf<Pair<List<String>, String?>>(emptyList<String>() to null) }
    // Headset routing needs BLUETOOTH_CONNECT on Android 12+. It is optional: a denial still
    // starts the call, which then plays through the phone.
    val liveVoicePermissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        val conversationId = pendingLiveVoiceConversationId
        val (members, speaker) = pendingLiveVoiceMembers
        pendingLiveVoiceConversationId = null
        pendingLiveVoiceMembers = emptyList<String>() to null
        if (results[Manifest.permission.RECORD_AUDIO] == true && conversationId != null) {
            appViewModel.startLiveVoice(conversationId, members, speaker)
        }
    }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptics = remember(view, context, lifecycleOwner) {
        AndroidHaptics(
            view = view,
            enabled = { HapticPolicy.isEnabled(context) },
            active = { lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) },
        )
    }
    // Failures arrive asynchronously. Each new failure plays one error cue, never a repeat for an old one.
    val failedMessages = state.messages.count { it.delivery == com.outcomego.android.data.Delivery.FAILED }
    var seenFailedMessages by remember { mutableStateOf(failedMessages) }
    LaunchedEffect(failedMessages) {
        if (failedMessages > seenFailedMessages) haptics.play(HapticEffect.Error)
        seenFailedMessages = failedMessages
    }
    // Sign-in ends when loading drops. A restored session never passes through loading.
    var wasLoading by remember { mutableStateOf(false) }
    LaunchedEffect(state.loading) {
        if (wasLoading && !state.loading) {
            haptics.play(if (state.session != null) HapticEffect.Success else HapticEffect.Error)
        }
        wasLoading = state.loading
    }
    val operation = state.features.operation
    LaunchedEffect(operation) {
        if (operation is FeatureOperationState.Failed) haptics.play(HapticEffect.Error)
    }

    LaunchedEffect(state.session) {
        if (
            state.session != null &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(state.pendingExternalUrl) {
        state.pendingExternalUrl?.let { url ->
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            appViewModel.consumeExternalUrl()
        }
    }

    val shareActionLabel = if (state.features.settings.language == AppSettings.Language.Chinese) "分享" else "Share"
    LaunchedEffect(state.pendingOutgoingShare) {
        state.pendingOutgoingShare?.let { share ->
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = share.mimeType.ifBlank { "application/octet-stream" }
                putExtra(Intent.EXTRA_STREAM, NativeAttachmentPreview.contentUri(context, share.uri))
                putExtra(Intent.EXTRA_TEXT, share.fileName)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "$shareActionLabel ${share.fileName}"))
            appViewModel.consumeOutgoingShare()
        }
    }

    LaunchedEffect(state.pendingLink, state.pendingVoiceShortcut, state.session) {
        if (state.session == null) return@LaunchedEffect
        val target = state.pendingLink
        if (target != null) {
            navController.navigate("chat/${target.conversationId}") { launchSingleTop = true }
            appViewModel.openConversation(target.conversationId, target.messageId)
            appViewModel.consumeDeepLink()
            if (state.pendingVoiceShortcut) {
                appViewModel.consumeVoiceShortcut()
                microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
            }
        } else if (navController.currentDestination?.route == LOGIN) {
            navController.navigate(DIRECTORY) { popUpTo(LOGIN) { inclusive = true } }
        }
    }
    LaunchedEffect(state.pendingShare != null, state.session, state.restoringSession) {
        if (state.session == null || state.restoringSession || state.pendingShare == null) return@LaunchedEffect
        if (navController.currentDestination?.route != SHARE) {
            navController.navigate(SHARE) { launchSingleTop = true }
        }
    }
    LaunchedEffect(state.pendingSystemAction, state.pendingSystemIdentifier, state.session) {
        val action = state.pendingSystemAction ?: return@LaunchedEffect
        if (state.session == null) return@LaunchedEffect
        when {
            action == "run-routine" -> {
                state.pendingSystemIdentifier?.let(appViewModel::runRoutineById)
                navController.navigate(ROUTINES)
            }
            action == "send-bot-task" -> {
                val conversation = state.conversations.firstOrNull { it.botId == state.pendingSystemIdentifier }
                if (conversation != null) {
                    appViewModel.openConversation(conversation.id)
                    state.pendingSystemPayload?.takeIf(String::isNotBlank)?.let(appViewModel::updateDraft)
                    navController.navigate("chat/${conversation.id}")
                } else {
                    navController.navigate(DIRECTORY)
                }
            }
            action == "find-messages" -> navController.navigate(MESSAGE_SEARCH)
            action == "open-bot" -> {
                val conversation = state.conversations.firstOrNull { it.botId == state.pendingSystemIdentifier }
                if (conversation != null) navController.navigate("bot/${conversation.id}/details")
                else navController.navigate(DIRECTORY)
            }
            action.contains("routine", ignoreCase = true) -> navController.navigate(ROUTINES)
            action.contains("plugin", ignoreCase = true) -> navController.navigate(PLUGINS)
            action.contains("settings", ignoreCase = true) -> navController.navigate(SETTINGS)
            else -> state.pendingSystemIdentifier?.let { identifier ->
                appViewModel.openConversation(identifier)
                navController.navigate("chat/$identifier")
            }
                ?: appViewModel.acceptShare(SharePayload(action, null, "text/plain"))
        }
        appViewModel.consumeSystemAction()
    }

    // Navigation Compose owns the back callback here so Android predictive-back gestures
    // can seek through the same slide transition used by normal navigation.
    if (state.restoringSession) {
        // Hold the first frame until the saved session is known. The NavHost start
        // destination is fixed at first composition, so it must not be built earlier.
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        return
    }
    CompositionLocalProvider(LocalAppLanguage provides state.features.settings.language, LocalAppHaptics provides haptics) {
        Box(Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = if (state.session != null) DIRECTORY else LOGIN,
            enterTransition = { slideNav(forward = !isSettingsOpening()) },
            exitTransition = { slideNavOut(forward = !isSettingsOpening()) },
            popEnterTransition = { slideNav(forward = isSettingsClosing()) },
            popExitTransition = { slideNavOut(forward = isSettingsClosing()) },
        ) {
        composable(LOGIN) {
            SignInScreen(
                loading = state.loading,
                checkingServer = state.authChecking,
                validatedServer = state.validatedServer,
                error = state.error,
                onCheckServer = appViewModel::checkServer,
                onClearValidation = appViewModel::clearServerValidation,
                onContinue = appViewModel::signIn,
                onPreview = appViewModel::signInPreview,
            )
        }
        composable(DIRECTORY) {
            DirectoryScreen(
                accountName = state.session?.accountName.orEmpty(),
                rows = state.conversations,
                searchResults = state.searchResults,
                searchLoading = state.searchLoading,
                searchError = state.searchError,
                sidebar = state.sidebar,
                onOpen = { conversation ->
                    appViewModel.openConversation(conversation.id)
                    navController.navigate("chat/${conversation.id}")
                },
                onRoutines = { navController.navigate(CREATE) },
                onOutbox = { navController.navigate(OUTBOX) },
                onSettings = { navController.navigate(SETTINGS) { launchSingleTop = true } },
                onSearch = appViewModel::search,
                onClearSearch = appViewModel::clearSearch,
                onOpenSearchResult = { result ->
                    result.channelId?.let { conversationId ->
                        appViewModel.openConversation(conversationId, result.messageId)
                        navController.navigate("chat/$conversationId")
                    }
                },
                onBotDetails = { conversation ->
                    if (conversation.isGroup) {
                        navController.navigate("conversation/${conversation.id}/profile")
                    } else {
                        navController.navigate("bot/${conversation.id}/details")
                    }
                },
                onTogglePin = appViewModel::togglePinned,
                onMoveToSection = { conversationId, sectionId ->
                    appViewModel.moveToSection(conversationId, sectionId)
                },
                onToggleSection = appViewModel::toggleSection,
            )
        }
        composable(BOT_DETAILS, arguments = listOf(navArgument("conversationId") { type = NavType.StringType })) { entry ->
            val id = entry.arguments?.getString("conversationId").orEmpty()
            val conversation = state.conversations.firstOrNull { it.id == id }
            val shareLabel = localizedText("Share bot", "分享机器人")
            BotDetailsScreen(
                conversation = conversation,
                onBack = { navController.popBackStack() },
                onOpenChat = {
                    appViewModel.openConversation(id)
                    navController.navigate("chat/$id")
                },
                onComputer = { navController.navigate("bot/$id/computer") },
                onRoutines = { navController.navigate(ROUTINES) },
                onEdit = { navController.navigate("conversation/$id/profile") },
                onHide = { appViewModel.setConversationHidden(it, true); navController.popBackStack() },
                onDelete = { appViewModel.deleteConversation(it); navController.navigate(DIRECTORY) { popUpTo(DIRECTORY) { inclusive = true } } },
                onDuplicate = { appViewModel.duplicateBot(it) },
                onNotifications = { row, enabled -> appViewModel.setBotNotifications(row, enabled) },
                onShare = {
                    conversation?.let {
                        context.startActivity(
                            Intent.createChooser(
                                Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, it.title)
                                },
                                shareLabel,
                            ),
                        )
                    }
                },
            )
        }
        composable(CREATE) {
            CreateConversationScreen(
                bots = state.conversations.filter { it.isBot },
                onCreateBot = { name, description, instructions, icon, color ->
                    appViewModel.createBot(name, description, instructions, icon, color)
                    navController.popBackStack()
                },
                onCreateGroup = { name, botIds ->
                    appViewModel.createGroup(name, botIds)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable(PROFILE, arguments = listOf(navArgument("conversationId") { type = NavType.StringType })) { entry ->
            val id = entry.arguments?.getString("conversationId").orEmpty()
            val conversation = state.conversations.firstOrNull { it.id == id }
            ConversationProfileScreen(
                conversation = conversation,
                bots = state.conversations.filter { it.isBot },
                onSaveBot = { current, name, description, instructions, voice, icon, color ->
                    appViewModel.updateBot(current, name, description, instructions, voice, icon, color)
                    navController.popBackStack()
                },
                onSaveGroup = { current, name, description, botIds ->
                    appViewModel.updateGroup(current, name, description, botIds)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable(COMPUTER, arguments = listOf(navArgument("conversationId") { type = NavType.StringType })) { entry ->
            val id = entry.arguments?.getString("conversationId").orEmpty()
            val conversation = state.conversations.firstOrNull { it.id == id }
            ComputerViewerScreen(
                title = conversation?.title ?: localizedText("Cloud computer", "云电脑"),
                botId = conversation?.botId.orEmpty(),
                server = state.session?.server,
                requestGrant = appViewModel::requestComputerGrant,
                requestStatus = appViewModel::computerStatus,
                setTakeover = appViewModel::setComputerTakeover,
                setPaused = appViewModel::setComputerPaused,
                sendAction = appViewModel::sendComputerAction,
                sendClipboard = appViewModel::sendComputerClipboard,
                onBack = { navController.popBackStack() },
            )
        }
        composable(CHAT, arguments = listOf(navArgument("conversationId") { type = NavType.StringType })) { entry ->
            val id = entry.arguments?.getString("conversationId").orEmpty()
            val conversation = state.conversations.firstOrNull { it.id == id }
            LaunchedEffect(id) { if (state.activeConversationId != id) appViewModel.openConversation(id) }
            ChatScreen(
                conversationId = id,
                accountName = state.session?.accountName.orEmpty(),
                title = conversation?.title ?: localizedText("Conversation", "对话"),
                messages = state.messages,
                targetMessageId = state.activeMessageId,
                draft = state.draft,
                replyingTo = state.replyingTo,
                onReply = appViewModel::startReply,
                onCancelReply = appViewModel::cancelReply,
                onOpenRoutine = { navController.navigate(ROUTINES) },
                error = state.error,
                onBack = { navController.popBackStack() },
                onBotDetails = {
                    if (conversation?.isGroup == true) {
                        navController.navigate("conversation/$id/profile")
                    } else {
                        navController.navigate("bot/$id/details")
                    }
                },
                onDraftChange = appViewModel::updateDraft,
                onSend = appViewModel::sendDraft,
                onRetry = appViewModel::retryMessage,
                voiceState = voiceState,
                onStartVoice = { microphonePermission.launch(Manifest.permission.RECORD_AUDIO) },
                onStopVoice = appViewModel::stopVoice,
                onRetryVoice = appViewModel::retryVoice,
                onCancelVoice = appViewModel::cancelVoice,
                liveVoice = state.liveVoice,
                onStartLiveVoice = { members, speaker ->
                    val recordGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                    val bluetoothMissing = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
                    if (recordGranted && !bluetoothMissing) {
                        appViewModel.startLiveVoice(id, members, speaker)
                    } else {
                        pendingLiveVoiceConversationId = id
                        pendingLiveVoiceMembers = members to speaker
                        liveVoicePermissions.launch(liveVoicePermissionRequest())
                    }
                },
                groupMembers = if (conversation?.isGroup == true) {
                    val memberIds = conversation.memberBotIds.toSet()
                    state.conversations.filter { it.isBot && it.botId in memberIds }
                } else {
                    emptyList()
                },
                onStopLiveVoice = appViewModel::stopLiveVoice,
                onMuteLiveVoiceMicrophone = appViewModel::setLiveVoiceMicrophoneMuted,
                onMuteLiveVoiceSpeaker = appViewModel::setLiveVoiceSpeakerMuted,
                operation = state.features.operation,
                pendingAttachments = state.pendingAttachments,
                hasMoreHistory = state.hasMoreHistory,
                historyLoading = state.historyLoading,
                conversations = state.conversations,
                onLoadOlder = appViewModel::loadOlderHistory,
                onUpload = appViewModel::uploadChatAttachment,
                onDownload = appViewModel::downloadChatAttachment,
                onSaveAttachment = appViewModel::saveChatAttachment,
                onShareAttachment = appViewModel::shareChatAttachment,
                onForwardAttachment = appViewModel::forwardChatAttachment,
                onAction = appViewModel::submitChatAction,
                onReaction = appViewModel::reactToChatMessage,
                onMessageAction = appViewModel::mutateChatMessage,
                onContext = appViewModel::loadMessageContext,
                threadContext = state.threadContext,
                threadContextTruncated = state.threadContextTruncated,
                activity = state.activity,
                bot = conversation,
            )
        }
        composable(ROUTINES) {
            RoutinesScreen(
                state = state.features.routines,
                onRetry = appViewModel::loadFeatureData,
                onAdd = { appViewModel.startRoutineEditor(); navController.navigate(ROUTINE_EDITOR) },
                onEdit = { appViewModel.startRoutineEditor(it); navController.navigate(ROUTINE_EDITOR) },
                onToggle = appViewModel::toggleRoutine,
                onRun = appViewModel::runRoutine,
                onBack = { navController.popBackStack() },
            )
        }
        composable(ROUTINE_EDITOR) {
            val draft = state.features.routineDraft
            // Keep the last draft on screen while the pop animation plays; the ViewModel
            // clears it after a successful save or delete.
            var shown by remember { mutableStateOf(draft) }
            if (draft != null) shown = draft
            LaunchedEffect(draft == null) {
                if (draft == null && navController.currentDestination?.route == ROUTINE_EDITOR) {
                    navController.popBackStack()
                }
            }
            shown?.let { routine ->
                RoutineEditorScreen(
                    routine = routine,
                    onChange = appViewModel::updateRoutineDraft,
                    onSave = appViewModel::saveRoutine,
                    onBack = { navController.popBackStack() },
                    onDelete = routine.id.takeIf(String::isNotBlank)?.let { { appViewModel.deleteRoutine() } },
                    operation = state.features.operation,
                )
            }
        }
        composable(SETTINGS) {
            SettingsScreen(
                settings = state.features.settings,
                accountName = state.session?.accountName.orEmpty(),
                server = state.session?.server.orEmpty(),
                networkSharing = networkSharing,
                onToggleNetworkSharing = { enabled ->
                    if (enabled) AndroidNetworkProvider.start(context, allowSharing = true)
                    else AndroidNetworkProvider.stop(context)
                },
                onNetworkSharingDetails = { navController.navigate(NETWORK_SHARING_DETAILS) },
                androidUpdateState = androidUpdateState,
                onCheckForAndroidUpdate = onCheckForAndroidUpdate,
                onDownloadAndroidUpdate = onDownloadAndroidUpdate,
                onInstallAndroidUpdate = onInstallAndroidUpdate,
                onChange = appViewModel::saveSettings,
                onSignOut = {
                    appViewModel.signOut()
                    navController.navigate(LOGIN) { popUpTo(navController.graph.id) { inclusive = true } }
                },
                onBack = { navController.popBackStack() },
                onProviders = { navController.navigate(PROVIDERS) },
                onRealtimeVoice = { navController.navigate(REALTIME_VOICE) },
                onWebProviders = { navController.navigate(WEB_PROVIDERS) },
                onOutbox = { navController.navigate(OUTBOX) },
                onAccount = { navController.navigate(ACCOUNT) },
                onPlugins = { navController.navigate(PLUGINS) },
                onSidebar = { navController.navigate(SIDEBAR) },
            )
        }
        composable(NETWORK_SHARING_DETAILS) {
            NetworkSharingDetailsScreen(
                provider = networkSharing,
                language = state.features.settings.language,
                onBack = { navController.popBackStack() },
            )
        }
        composable(OUTBOX) {
            OutboxScreen(
                items = state.outbox,
                onRetry = appViewModel::retryQueuedMessage,
                onDiscard = appViewModel::discardQueuedMessage,
                onBack = { navController.popBackStack() },
            )
        }
        composable(ACCOUNT) {
            AccountMemoryScreen(
                state = state.features.account,
                onRetry = appViewModel::loadFeatureData,
                onEditMemory = {},
                onClearMemory = appViewModel::clearMemory,
                onReset = appViewModel::resetLocalData,
                onSignOut = {
                    appViewModel.signOut()
                    navController.navigate(LOGIN) { popUpTo(navController.graph.id) { inclusive = true } }
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable(SIDEBAR) {
            SidebarManagementScreen(
                sidebar = state.sidebar,
                hidden = state.hiddenConversations,
                onRename = appViewModel::renameSection,
                onDeleteSection = appViewModel::deleteSection,
                onUnhide = { appViewModel.setConversationHidden(it, false) },
                onDeleteConversation = appViewModel::deleteConversation,
                onBack = { navController.popBackStack() },
            )
        }
        composable(MESSAGE_SEARCH) {
            MessageSearchScreen(
                results = state.searchResults,
                loading = state.searchLoading,
                error = state.searchError,
                conversations = state.conversations,
                accountName = state.session?.accountName.orEmpty(),
                onSearch = { query -> appViewModel.search(query, "messages") },
                onOpen = { result ->
                    result.channelId?.let { channelId ->
                        appViewModel.openConversation(channelId, result.messageId)
                        navController.navigate("chat/$channelId")
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable(PROVIDERS) {
            ProviderSettingsScreen(
                state = state.features.providers,
                onRetry = appViewModel::loadFeatureData,
                onBack = { navController.popBackStack() },
                language = state.features.settings.language,
                onConnect = appViewModel::connectProvider,
                onDisconnect = appViewModel::disconnectProvider,
                onRemove = appViewModel::removeProvider,
                loadPresets = appViewModel::loadProviderPresets,
                onAdd = appViewModel::addProvider,
                onModelChange = { provider, model -> appViewModel.connectProvider(provider.copy(selectedModel = model)) },
                onReasoningChange = { provider, reasoning -> appViewModel.connectProvider(provider.copy(selectedReasoning = reasoning)) },
            )
        }
        composable(WEB_PROVIDERS) {
            WebProviderSettingsScreen(
                state = state.features.webProviders,
                onRetry = appViewModel::loadFeatureData,
                onBack = { navController.popBackStack() },
                language = state.features.settings.language,
                onSelect = appViewModel::selectWebProvider,
                onSaveKey = { tool, provider, key -> appViewModel.saveWebProvider(tool, provider, key) },
                onRemoveKey = { tool, provider -> appViewModel.saveWebProvider(tool, provider, "") },
                onCheck = appViewModel::checkWebProvider,
            )
        }
        composable(REALTIME_VOICE) {
            VoiceSettingsScreen(
                state = state.features.voiceSettings,
                language = state.features.settings.language,
                onRetry = appViewModel::loadFeatureData,
                onSave = appViewModel::saveVoiceSettings,
                onBack = { navController.popBackStack() },
            )
        }
        composable(PLUGINS) {
            PluginsScreen(
                state = state.features.plugins,
                onRetry = appViewModel::loadFeatureData,
                onInstall = appViewModel::installPlugin,
                onUpdate = appViewModel::updatePlugin,
                onRollback = appViewModel::rollbackPlugin,
                onAuthorize = appViewModel::authorizePlugin,
                onCancelAuthorization = appViewModel::cancelPluginAuthorization,
                onConfigure = appViewModel::configurePlugin,
                onTestTool = appViewModel::testPluginTool,
                onBack = { navController.popBackStack() },
            )
        }
        composable(SHARE) {
            val activity = LocalContext.current as? ComponentActivity
            ShareTargetScreen(
                share = state.pendingShare,
                rows = state.conversations,
                sidebar = state.sidebar,
                error = state.error,
                onSelect = appViewModel::selectShareTarget,
                onStay = { conversation ->
                    navController.navigate("chat/${conversation.id}") { popUpTo(SHARE) { inclusive = true } }
                },
                onReturn = { conversation ->
                    // Land on the chat first, so reopening OutcomeGo later shows where the share went.
                    navController.navigate("chat/${conversation.id}") { popUpTo(SHARE) { inclusive = true } }
                    activity?.moveTaskToBack(true)
                },
                onCancel = {
                    appViewModel.cancelShare()
                    navController.popBackStack()
                },
            )
        }
        composable(MARKETPLACE) {
            MarketplaceScreen(
                state = state.features.plugins,
                onBack = { navController.popBackStack() },
                onOpenExtensions = { navController.navigate(PLUGINS) },
            )
        }
        }
        if (
            state.liveVoice.state != com.outcomego.platform.AndroidLiveVoiceState.Idle &&
            state.liveVoiceConversationId != null
        ) {
            val callConversation = state.conversations.firstOrNull { it.id == state.liveVoiceConversationId }
            LiveVoiceView(
                snapshot = state.liveVoice,
                bot = callConversation,
                participants = if (callConversation?.isGroup == true) {
                    val memberIds = callConversation.memberBotIds.toSet()
                    state.conversations.filter { it.isBot && it.botId in memberIds }
                } else {
                    emptyList()
                },
                defaultSpeakerId = state.liveVoiceDefaultSpeakerId,
                onStop = appViewModel::stopLiveVoice,
                onMuteMicrophone = appViewModel::setLiveVoiceMicrophoneMuted,
                onMuteSpeaker = appViewModel::setLiveVoiceSpeakerMuted,
                modifier = Modifier.fillMaxSize().zIndex(20f),
            )
        }
        }
    }
}

@Composable
private fun ShareTargetScreen(
    share: SharePayload?,
    rows: List<Conversation>,
    sidebar: SidebarPreferences,
    error: String?,
    onSelect: (String) -> Boolean,
    onStay: (Conversation) -> Unit,
    onReturn: (Conversation) -> Unit,
    onCancel: () -> Unit,
) {
    // Choosing a bot clears the pending share, so keep showing what was received.
    var received by remember { mutableStateOf(share) }
    LaunchedEffect(share) { if (share != null) received = share }
    var sentTo by remember { mutableStateOf<Conversation?>(null) }
    val grouped = remember(rows, sidebar) { groupConversations(rows, sidebar) }
    val ordered = remember(grouped) {
        grouped.pinned + grouped.sections.flatMap { it.conversations } + grouped.unassigned
    }
    BackHandler(enabled = sentTo == null, onBack = onCancel)

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = localizedText("Cancel", "取消"))
                    }
                },
                title = { Text(localizedText("Share to OutcomeGo", "分享到 OutcomeGo")) },
                colors = featureTopBarColors(),
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).testTag("share-target-list"),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item(key = "received") {
                received?.let { ShareReceivedCard(it) }
            }
            if (error != null) {
                item(key = "error") {
                    Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
            item(key = "choose") {
                Text(
                    localizedText("Send to", "发送给"),
                    Modifier.padding(start = 4.dp, top = 16.dp, bottom = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(ordered, key = { "share-${it.id}" }) { row ->
                Row(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .testTag("share-target-${row.id}")
                        .clickable { if (onSelect(row.id)) sentTo = row }
                        .padding(horizontal = 4.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BotAvatar(row, size = 46.dp, showWorking = true)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(row.title, fontWeight = FontWeight.Medium, maxLines = 1)
                        if (row.subtitle.isNotBlank()) {
                            Text(
                                row.subtitle,
                                maxLines = 1,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }

    sentTo?.let { target ->
        AlertDialog(
            onDismissRequest = { onStay(target) },
            title = { Text(localizedText("Shared to ${target.title}", "已分享给 ${target.title}")) },
            text = {
                Text(
                    localizedText(
                        "Stay in OutcomeGo to see the conversation, or go back to the app you shared from?",
                        "要留在 OutcomeGo 查看对话,还是回到分享来源的上一个应用?",
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = { onStay(target) }, modifier = Modifier.testTag("share-stay")) {
                    Text(localizedText("Stay in OutcomeGo", "留在 OutcomeGo"))
                }
            },
            dismissButton = {
                TextButton(onClick = { onReturn(target) }, modifier = Modifier.testTag("share-return")) {
                    Text(localizedText("Back to previous app", "回到上一个应用"))
                }
            },
        )
    }
}

@Composable
private fun ShareReceivedCard(share: SharePayload) {
    val context = LocalContext.current
    val streams = share.streams.ifEmpty { listOfNotNull(share.stream) }
    val names = remember(streams) { streams.map { shareStreamLabel(context, it) } }
    Card(
        Modifier.fillMaxWidth().testTag("share-received"),
        colors = featureCardColors(),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                localizedText("Received content", "收到的内容"),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            share.text?.takeIf(String::isNotBlank)?.let {
                Text(it.trim(), maxLines = 6, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
            names.take(5).forEach { name ->
                Text("📎 $name", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
            if (names.size > 5) {
                Text(
                    localizedText("and ${names.size - 5} more", "另外 ${names.size - 5} 个"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun shareStreamLabel(context: android.content.Context, uri: Uri): String =
    runCatching {
        context.contentResolver
            .query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
    }.getOrNull()?.takeIf(String::isNotBlank) ?: uri.lastPathSegment ?: uri.toString()

@Composable
private fun OutboxScreen(
    items: List<PendingSend>,
    onRetry: (String) -> Unit,
    onDiscard: (String) -> Unit,
    onBack: () -> Unit,
) {
    val haptic = LocalAppHaptics.current
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = localizedText("Back", "返回"))
                    }
                },
                title = { Text(localizedText("Outbox", "发件箱")) },
                colors = featureTopBarColors(),
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
    ) { padding ->
        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(localizedText("All messages have been delivered.", "所有消息都已发送。"), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp),
            ) {
                items(items, key = PendingSend::id) { item ->
                    Card(Modifier.fillMaxWidth(), colors = featureCardColors()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(item.content.ifBlank { "Attachment-only message" }, fontWeight = FontWeight.Medium)
                            Text(localizedText("Conversation: ${item.channelId}", "对话：${item.channelId}"), style = MaterialTheme.typography.labelSmall)
                            if (item.attachments.isNotEmpty()) {
                                Text(
                                    item.attachments.joinToString(", ") { attachment ->
                                        "${attachment.fileName} (${attachment.mimeType})"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            Text(
                                item.failure?.let { localizedText("Failed: $it", "失败：$it") } ?: localizedText("Queued for delivery", "已排队等待发送"),
                                color = if (item.failure == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { haptic.play(HapticEffect.Light); onRetry(item.id) }) { Text(localizedText("Retry", "重试")) }
                                TextButton(onClick = { haptic.play(HapticEffect.Warning); onDiscard(item.id) }) {
                                    Text(localizedText("Discard", "丢弃"), color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SidebarManagementScreen(
    sidebar: SidebarPreferences,
    hidden: List<Conversation>,
    onRename: (String, String) -> Unit,
    onDeleteSection: (String) -> Unit,
    onUnhide: (Conversation) -> Unit,
    onDeleteConversation: (Conversation) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, localizedText("Back", "返回")) } },
                title = { Text(localizedText("Organize conversations", "整理对话")) },
                colors = featureTopBarColors(),
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp),
        ) {
            item { Text(localizedText("Sections", "分区"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
            if (sidebar.sections.isEmpty()) {
                item { Text(localizedText("No custom sections yet.", "还没有自定义分区。"), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(sidebar.sections, key = { "section:${it.id}" }) { section ->
                var name by remember(section.id, section.name) { mutableStateOf(section.name) }
                Card(Modifier.fillMaxWidth(), colors = featureCardColors()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(name, { name = it }, label = { Text(localizedText("Section name", "分区名称")) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { onRename(section.id, name) }, enabled = name.trim().isNotEmpty()) { Text(localizedText("Save", "保存")) }
                        TextButton(onClick = { onDeleteSection(section.id) }) {
                            Text(localizedText("Delete", "删除"), color = MaterialTheme.colorScheme.error)
                        }
                        }
                    }
                }
            }
            item {
                Spacer(Modifier.height(8.dp))
                Text(localizedText("Hidden conversations", "隐藏的对话"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            if (hidden.isEmpty()) {
                item { Text(localizedText("No hidden conversations.", "没有隐藏的对话。"), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(hidden, key = { "hidden:${it.id}" }) { conversation ->
                Card(Modifier.fillMaxWidth(), colors = featureCardColors()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(conversation.title, fontWeight = FontWeight.Medium)
                            Text(conversation.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = { onUnhide(conversation) }) { Text(localizedText("Unhide", "取消隐藏")) }
                        TextButton(onClick = { onDeleteConversation(conversation) }) {
                            Text(localizedText("Delete", "删除"), color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageSearchScreen(
    results: List<SearchResult>,
    loading: Boolean,
    error: String?,
    conversations: List<Conversation>,
    accountName: String,
    onSearch: (String) -> Unit,
    onOpen: (SearchResult) -> Unit,
    onBack: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    LaunchedEffect(query) { onSearch(query) }
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, localizedText("Back", "返回")) } },
                title = { Text(localizedText("Find messages", "查找消息")) },
                colors = featureTopBarColors(),
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            OutlinedTextField(
                query,
                { query = it },
                label = { Text(localizedText("Search messages", "搜索消息")) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(Modifier.height(12.dp))
            if (loading) {
                CircularProgressIndicator()
            } else if (error != null) {
                Text(error, color = MaterialTheme.colorScheme.error)
            } else if (query.isBlank()) {
                Text(localizedText("Enter a phrase to search messages.", "输入要搜索的内容。"), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                SearchResultsList(
                    results = results,
                    loading = loading,
                    error = error,
                    conversations = conversations,
                    accountName = accountName,
                    onSelect = onOpen,
                )
            }
        }
    }
}

@Composable
private fun SignInScreen(
    loading: Boolean,
    checkingServer: Boolean,
    validatedServer: String?,
    error: String?,
    onCheckServer: (String) -> Unit,
    onClearValidation: () -> Unit,
    onContinue: (String, String, String) -> Unit,
    onPreview: (String, String, String) -> Unit,
) {
    var step by remember { mutableStateOf(SignInStep.WELCOME) }
    var server by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var credential by remember { mutableStateOf("") }
    var showingPassword by remember { mutableStateOf(false) }
    var showingInstall by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val resolvedServer = validatedServer ?: server.trim()

    Column(
        Modifier.fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = .34f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            )
            .imePadding()
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(scrollState)
            .padding(horizontal = 22.dp, vertical = 18.dp),
    ) {
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                val forward = targetState.ordinal > initialState.ordinal
                val spec = tween<androidx.compose.ui.unit.IntOffset>(NAVIGATION_TRANSITION_DURATION_MILLIS, easing = FastOutSlowInEasing)
                (slideInHorizontally(spec) { if (forward) it / 3 else -it / 3 } + fadeIn()) togetherWith
                    (slideOutHorizontally(spec) { if (forward) -it / 3 else it / 3 } + fadeOut())
            },
            label = "sign-in-step",
        ) { currentStep ->
        Column {
        when (currentStep) {
            SignInStep.WELCOME -> {
                IosWizardHero(stage = "server")
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(com.outcomego.android.R.string.login_welcome_title),
                    modifier = Modifier.testTag("login-welcome-title"),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(com.outcomego.android.R.string.login_welcome_detail),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(Modifier.height(22.dp))
                if (showingInstall) {
                    IosGroupedCard {
                        Text(stringResource(com.outcomego.android.R.string.login_install_server), fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(com.outcomego.android.R.string.login_install_command),
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(com.outcomego.android.R.string.login_install_detail),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                }
                IosPrimaryButton(
                    label = stringResource(com.outcomego.android.R.string.login_get_started),
                    onClick = { step = SignInStep.ENDPOINT },
                    enabled = !loading,
                    testTag = "get-started",
                )
                TextButton(
                    onClick = { showingInstall = !showingInstall },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !loading,
                ) {
                    Text(
                        stringResource(
                            if (showingInstall) com.outcomego.android.R.string.login_have_server
                            else com.outcomego.android.R.string.login_install_hint,
                        ),
                    )
                }
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = { onPreview("", "", "") },
                    modifier = Modifier.fillMaxWidth().testTag("preview-workspace"),
                    enabled = !loading,
                ) { Text(stringResource(com.outcomego.android.R.string.login_sample)) }
                Text(
                    stringResource(com.outcomego.android.R.string.login_sample_detail),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            SignInStep.ENDPOINT -> {
                IosWizardTopBar(onBack = { step = SignInStep.WELCOME })
                IosWizardHero(stage = "endpoint")
                IosWizardHeader(
                    title = stringResource(com.outcomego.android.R.string.login_server_title),
                    detail = stringResource(com.outcomego.android.R.string.login_server_detail),
                )
                Spacer(Modifier.height(16.dp))
                IosGroupedCard {
                    OutlinedTextField(
                        value = server,
                        onValueChange = {
                            server = it
                            if (validatedServer != null) onClearValidation()
                        },
                        modifier = Modifier.fillMaxWidth().testTag("server-field"),
                        label = { Text(stringResource(com.outcomego.android.R.string.login_server_address)) },
                        placeholder = { Text(stringResource(com.outcomego.android.R.string.login_server_placeholder)) },
                        singleLine = true,
                        trailingIcon = {
                            if (server.isNotEmpty() && !checkingServer) {
                                IconButton(onClick = { server = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = stringResource(com.outcomego.android.R.string.login_clear_server))
                                }
                            }
                        },
                    )
                    if (checkingServer) {
                        WizardStatusLine(
                            stringResource(com.outcomego.android.R.string.login_connecting) + " " + server.trim(),
                            loading = true,
                        )
                    } else if (validatedServer != null) {
                        WizardStatusLine(
                            stringResource(
                                com.outcomego.android.R.string.login_server_found,
                                resolvedServer.removePrefix("https://").removePrefix("http://"),
                            ),
                            done = true,
                        )
                    }
                }
                if (error != null) {
                    Spacer(Modifier.height(10.dp))
                    IosErrorNotice(error)
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    stringResource(com.outcomego.android.R.string.login_server_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (server.trim().startsWith("http://", ignoreCase = true)) {
                    Text(
                        stringResource(com.outcomego.android.R.string.login_http_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                Spacer(Modifier.height(18.dp))
                val serverValidated = validatedServer != null
                IosPrimaryButton(
                    label = stringResource(
                        when {
                            serverValidated -> com.outcomego.android.R.string.login_connection_successful
                            checkingServer -> com.outcomego.android.R.string.login_connecting
                            else -> com.outcomego.android.R.string.login_connect
                        },
                    ),
                    onClick = { onCheckServer(server) },
                    enabled = !serverValidated && server.isNotBlank() && !checkingServer && !loading,
                    testTag = "connect-button",
                    busy = checkingServer,
                    success = serverValidated,
                )
                if (checkingServer) {
                    TextButton(onClick = { step = SignInStep.ENDPOINT }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(com.outcomego.android.R.string.login_cancel))
                    }
                } else if (validatedServer != null) {
                    Spacer(Modifier.height(8.dp))
                    IosSecondaryButton(
                        label = stringResource(com.outcomego.android.R.string.login_continue_credentials),
                        onClick = { step = SignInStep.CREDENTIALS },
                        enabled = !loading,
                    )
                }
            }
            SignInStep.CREDENTIALS -> {
                IosWizardTopBar(onBack = { step = SignInStep.ENDPOINT })
                IosWizardHero(stage = "credentials")
                IosWizardHeader(
                    title = stringResource(com.outcomego.android.R.string.login_credentials_title),
                    detail = stringResource(com.outcomego.android.R.string.login_credentials_detail),
                )
                Spacer(Modifier.height(12.dp))
                ServerChip(resolvedServer)
                Spacer(Modifier.height(12.dp))
                IosGroupedCard {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        modifier = Modifier.fillMaxWidth().testTag("username-field"),
                        label = { Text(stringResource(com.outcomego.android.R.string.login_username)) },
                        singleLine = true,
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = credential,
                        onValueChange = { credential = it },
                        modifier = Modifier.fillMaxWidth().testTag("credential-field"),
                        label = { Text(stringResource(com.outcomego.android.R.string.login_credential)) },
                        singleLine = true,
                        visualTransformation = if (showingPassword) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            TextButton(onClick = { showingPassword = !showingPassword }) {
                                Text(
                                    stringResource(
                                        if (showingPassword) com.outcomego.android.R.string.login_hide
                                        else com.outcomego.android.R.string.login_show,
                                    ),
                                )
                            }
                        },
                    )
                }
                if (error != null) {
                    Spacer(Modifier.height(10.dp))
                    IosErrorNotice(error)
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    stringResource(com.outcomego.android.R.string.login_forgot),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (resolvedServer.startsWith("http://", ignoreCase = true)) {
                    Text(
                        stringResource(com.outcomego.android.R.string.login_credential_http_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                Spacer(Modifier.height(18.dp))
                IosPrimaryButton(
                    label = stringResource(
                        if (loading) com.outcomego.android.R.string.login_signing_in
                        else com.outcomego.android.R.string.login_sign_in,
                    ),
                    onClick = { onContinue(resolvedServer, username, credential) },
                    enabled = username.isNotBlank() && credential.isNotBlank() && !loading,
                    testTag = "sign-in-button",
                    busy = loading,
                )
            }
        }
        }
        }
    }
}

@Composable
private fun IosWizardHero(stage: String) {
    val accent = when (stage) {
        "credentials" -> Color(0xFF7C4DFF)
        "endpoint" -> Color(0xFF2F6FEB)
        else -> Color(0xFF12A4B4)
    }
    Box(
        Modifier.fillMaxWidth().height(188.dp).clip(RoundedCornerShape(32.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        accent.copy(alpha = .24f),
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = .8f),
                        MaterialTheme.colorScheme.surface,
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(104.dp).offset(x = (-78).dp, y = 16.dp).clip(CircleShape)
                .background(accent.copy(alpha = .78f)),
        )
        Box(
            Modifier.size(82.dp).offset(x = 86.dp, y = (-18).dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = .85f)),
        )
        Card(
            Modifier.width(260.dp).rotate(-2f),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF17171A)),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                Text("●  ●  ●   Terminal", color = Color.White.copy(alpha = .55f), style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(8.dp))
                Text("$ outcomego setup", color = Color.White.copy(alpha = .48f), fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                Text("✓ ${localizedText("OutcomeGo is ready", "OutcomeGo 已就绪")}", color = Color(0xFF3DDC84), fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                Text("Server   192.168.1.20:8787", color = Color.White.copy(alpha = .88f), fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun IosWizardTopBar(onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, modifier = Modifier.testTag("wizard-back")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = localizedText("Back to conversations", "返回对话列表"))
        }
        Spacer(Modifier.weight(1f))
        Text(stringResource(com.outcomego.android.R.string.app_name), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun IosWizardHeader(title: String, detail: String) {
    Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(6.dp))
    Text(detail, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun IosGroupedCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        Modifier.fillMaxWidth().animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .55f)),
        shape = RoundedCornerShape(22.dp),
        content = { Column(Modifier.padding(12.dp), content = content) },
    )
}

@Composable
private fun IosPrimaryButton(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean,
    testTag: String,
    busy: Boolean = false,
    success: Boolean = false,
) {
    val haptic = LocalAppHaptics.current
    Button(
        onClick = { haptic.play(HapticEffect.Light); onClick() },
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = if (success) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
            contentColor = if (success) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = if (success) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
            disabledContentColor = if (success) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
        else Text(label)
    }
}

@Composable
private fun IosSecondaryButton(label: String, onClick: () -> Unit, enabled: Boolean) {
    val haptic = LocalAppHaptics.current
    OutlinedButton(
        onClick = { haptic.play(HapticEffect.Light); onClick() },
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) { Text(label) }
}

@Composable
private fun WizardStatusLine(text: String, loading: Boolean = false, done: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        when {
            loading -> CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
            done -> Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun IosErrorNotice(message: String) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = .09f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = .28f)),
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ServerChip(server: String) {
    Text(
        server.removePrefix("https://").removePrefix("http://"),
        modifier = Modifier.clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun DirectoryScreen(
    accountName: String,
    rows: List<Conversation>,
    searchResults: List<SearchResult>,
    searchLoading: Boolean,
    searchError: String?,
    sidebar: SidebarPreferences,
    onOpen: (Conversation) -> Unit,
    onRoutines: () -> Unit,
    onOutbox: () -> Unit,
    onSettings: () -> Unit,
    onSearch: (String, String) -> Unit,
    onClearSearch: () -> Unit,
    onOpenSearchResult: (SearchResult) -> Unit,
    onBotDetails: (Conversation) -> Unit,
    onTogglePin: (String) -> Unit,
    onMoveToSection: (String, String?) -> Unit,
    onToggleSection: (String?) -> Unit,
) {
    var searchVisible by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var searchFilter by remember { mutableStateOf(SearchFilter.All) }
    var rowMenuId by remember { mutableStateOf<String?>(null) }
    val searchFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val activity = LocalContext.current as? ComponentActivity
    // Only manually pinned chats appear in the top row, and they are removed from the
    // grouped list below so each chat is shown in exactly one place.
    val grouped = remember(rows, sidebar) { groupConversations(rows, sidebar) }
    val pinnedRows = grouped.pinned
    val unassignedRows = grouped.unassigned
    val showSectionHeadings = grouped.showUnassignedHeading
    val settingsLabel = localizedText("Settings", "设置")
    val closeSearch = {
        keyboardController?.hide()
        searchVisible = false
        query = ""
        searchFilter = SearchFilter.All
        onClearSearch()
    }
    BackHandler(enabled = searchVisible) {
        closeSearch()
    }
    DisposableEffect(activity, searchVisible) {
        if (activity == null || !searchVisible || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            onDispose { }
        } else {
            val callback = OnBackInvokedCallback { closeSearch() }
            activity.onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_OVERLAY,
                callback,
            )
            onDispose {
                activity.onBackInvokedDispatcher.unregisterOnBackInvokedCallback(callback)
            }
        }
    }
    LaunchedEffect(searchVisible, query, searchFilter) {
        if (searchVisible) onSearch(query, searchFilter.apiValue) else onClearSearch()
    }
    LaunchedEffect(searchVisible) {
        if (searchVisible) searchFocusRequester.requestFocus()
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (searchVisible) {
                        Text(localizedText("Search Content", "搜索内容"))
                    }
                },
                navigationIcon = {
                    if (!searchVisible) {
                        IconButton(
                            onClick = onSettings,
                            modifier = Modifier.padding(start = 10.dp).size(48.dp).semantics {
                                contentDescription = settingsLabel
                            },
                        ) {
                            Box(
                                Modifier.size(36.dp).clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = .13f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    accountName.take(1).uppercase().ifBlank { "O" },
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = .65f),
                                    fontWeight = FontWeight.Medium,
                                    style = MaterialTheme.typography.titleLarge,
                                )
                            }
                        }
                    }
                },
                actions = {
                    if (!searchVisible) {
                        IosChromeIcon(
                            icon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                            contentDescription = localizedText("Outbox", "发件箱"),
                            onClick = onOutbox,
                        )
                        IosChromeIcon(
                            icon = { Icon(Icons.Default.Add, contentDescription = null) },
                            contentDescription = localizedText("Create conversation", "创建对话"),
                            onClick = onRoutines,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            )
        },
        bottomBar = {
            if (searchVisible) {
                ExpandedSearchBar(
                    query = query,
                    onQueryChange = { query = it },
                    focusRequester = searchFocusRequester,
                    onClear = { query = "" },
                    onClose = closeSearch,
                )
            } else FloatingSearchBar(onClick = { searchVisible = true })
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.pointerInput(searchVisible) {
            if (searchVisible) return@pointerInput
            // Settings lives off the left edge: dragging the list to the right pulls it in.
            val trigger = 72.dp.toPx()
            var total = 0f
            var fired = false
            detectHorizontalDragGestures(onDragStart = { total = 0f; fired = false }) { _, delta ->
                total += delta
                if (!fired && total >= trigger) { fired = true; onSettings() }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            if (searchVisible) {
                SearchFilterRow(selected = searchFilter, onSelected = { searchFilter = it })
                Box(Modifier.fillMaxWidth().weight(1f)) {
                    if (query.isNotBlank()) {
                        SearchResultsList(
                            results = searchResults,
                            loading = searchLoading,
                            error = searchError,
                            conversations = rows,
                            accountName = accountName,
                            onSelect = onOpenSearchResult,
                        )
                    }
                }
            } else {
            if (pinnedRows.isNotEmpty()) {
                // The pinned bots form one group centered in the row. Once they no longer fit,
                // the row starts at the left edge and scrolls.
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterHorizontally),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 8.dp, bottom = 10.dp),
                ) {
                    items(pinnedRows, key = { "pinned-${it.id}" }) { row ->
                        PinnedBotCard(
                            conversation = row,
                            accountName = accountName,
                            members = groupMembers(row, rows),
                            unread = row.unread > 0 || sidebar.isUnread(row.id),
                            onClick = { onOpen(row) },
                            onLongClick = { onTogglePin(row.id) },
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            LazyColumn(
                modifier = Modifier.fillMaxSize().testTag("directory-title"),
                verticalArrangement = Arrangement.spacedBy(0.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 28.dp),
            ) {
                grouped.sections.forEach { group ->
                    val section = group.section
                    val members = group.conversations
                    item(key = "section-${section.id}") {
                        DirectorySectionHeader(
                            name = section.name,
                            collapsed = section.collapsed,
                            count = members.size,
                            onClick = { onToggleSection(section.id) },
                        )
                    }
                    if (!section.collapsed) {
                        items(members, key = Conversation::id) { row ->
                            DirectoryConversationRow(
                                row = row,
                                accountName = accountName,
                                members = groupMembers(row, rows),
                                unread = row.unread > 0 || sidebar.isUnread(row.id),
                                pinned = false,
                                sections = sidebar.sections,
                                menuExpanded = rowMenuId == row.id,
                                onOpen = { onOpen(row) },
                                onBotDetails = { onBotDetails(row) },
                                onTogglePin = { onTogglePin(row.id) },
                                onMoveToSection = { sectionId -> onMoveToSection(row.id, sectionId) },
                                onOpenMenu = { rowMenuId = row.id },
                                onDismissMenu = { rowMenuId = null },
                            )
                        }
                    }
                }
                if (showSectionHeadings) {
                    item(key = "section-unassigned") {
                        DirectorySectionHeader(
                            name = localizedText("Unassigned", "未分组"),
                            collapsed = sidebar.unassignedCollapsed,
                            count = unassignedRows.size,
                            onClick = { onToggleSection(null) },
                        )
                    }
                }
                if (!showSectionHeadings || !sidebar.unassignedCollapsed) {
                    items(unassignedRows, key = Conversation::id) { row ->
                        DirectoryConversationRow(
                            row = row,
                            accountName = accountName,
                            members = groupMembers(row, rows),
                            unread = row.unread > 0 || sidebar.isUnread(row.id),
                            pinned = false,
                            sections = sidebar.sections,
                            menuExpanded = rowMenuId == row.id,
                            onOpen = { onOpen(row) },
                            onBotDetails = { onBotDetails(row) },
                            onTogglePin = { onTogglePin(row.id) },
                            onMoveToSection = { sectionId -> onMoveToSection(row.id, sectionId) },
                            onOpenMenu = { rowMenuId = row.id },
                            onDismissMenu = { rowMenuId = null },
                        )
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun SearchResultsList(
    results: List<SearchResult>,
    loading: Boolean,
    error: String?,
    conversations: List<Conversation>,
    accountName: String,
    onSelect: (SearchResult) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(0.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 28.dp),
    ) {
        if (loading) {
            item(key = "search-loading") {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
        if (error != null) {
            item(key = "search-error") {
                Text(
                    error,
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        items(results, key = { "search-${it.kind}-${it.id}" }) { result ->
            SearchResultRow(
                result = result,
                conversation = conversations.firstOrNull { it.id == result.channelId },
                accountName = accountName,
                members = conversations.firstOrNull { it.id == result.channelId }?.let { groupMembers(it, conversations) }.orEmpty(),
                onClick = { onSelect(result) },
            )
        }
        if (!loading && error == null && results.isEmpty()) {
            item(key = "search-empty") {
                Text(
                    localizedText("No results", "没有找到结果"),
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SearchResultRow(
    result: SearchResult,
    conversation: Conversation?,
    accountName: String,
    members: List<Conversation>,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth()
            .clickable(enabled = result.channelId != null, onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (conversation != null) {
            ConversationAvatar(conversation, accountName, members, size = 46.dp)
        } else {
            Box(
                Modifier.size(46.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(result.title.take(1).uppercase().ifBlank { "O" })
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    result.title,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    localizedSearchResultKind(result.kind),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (result.subtitle.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    result.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
        }
    }
}

@Composable
private fun localizedSearchResultKind(kind: String): String = when (kind.lowercase(Locale.ROOT)) {
    "messages" -> localizedText("Messages", "消息")
    "bots" -> localizedText("Robots", "机器人")
    "channels" -> localizedText("Group chats", "群聊")
    "files" -> localizedText("Files", "文件")
    "routines" -> localizedText("Scheduled tasks", "定时任务")
    "links" -> localizedText("Other", "其他")
    else -> kind.replaceFirstChar { it.uppercase() }
}

private enum class SearchFilter(
    val apiValue: String,
    val englishLabel: String,
    val chineseLabel: String,
) {
    All("all", "All", "全部"),
    Messages("messages", "Messages", "消息"),
    Robots("bots", "Robots", "机器人"),
    Groups("channels", "Group chats", "群聊"),
    Files("files", "Files", "文件"),
    ScheduledTasks("routines", "Scheduled tasks", "定时任务"),
    Other("links", "Other", "其他"),
}

@Composable
private fun SearchFilterRow(selected: SearchFilter, onSelected: (SearchFilter) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
            .padding(top = 4.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SearchFilter.values().forEach { filter ->
            val active = filter == selected
            Text(
                localizedText(filter.englishLabel, filter.chineseLabel),
                modifier = Modifier.clip(CircleShape)
                    .background(
                        if (active) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.surface,
                    )
                    .clickable { onSelected(filter) }
                    .padding(horizontal = 18.dp, vertical = 11.dp),
                color = if (active) MaterialTheme.colorScheme.surface
                else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

private fun groupMembers(group: Conversation, rows: List<Conversation>): List<Conversation> {
    if (!group.isGroup) return emptyList()
    val botsById = rows
        .filter { it.isBot && !it.botId.isNullOrBlank() }
        .associateBy { it.botId }
    return group.memberBotIds.mapNotNull(botsById::get)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PinnedBotCard(
    conversation: Conversation,
    accountName: String,
    members: List<Conversation>,
    unread: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val haptic = LocalAppHaptics.current
    Column(
        modifier = Modifier.width(100.dp)
            .testTag("pinned-${conversation.id}")
            .combinedClickable(
                onClick = { haptic.play(HapticEffect.Light); onClick() },
                onLongClick = { haptic.play(HapticEffect.Medium); onLongClick() },
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            ConversationAvatar(conversation, accountName, members, size = 80.dp, showWorking = true)
            if (unread) {
                Box(
                    Modifier.align(Alignment.BottomEnd).size(13.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary)
                        .border(2.dp, MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                )
            }
        }
        Spacer(Modifier.height(7.dp))
        Text(
            conversation.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DirectoryConversationRow(
    row: Conversation,
    accountName: String,
    members: List<Conversation>,
    unread: Boolean,
    pinned: Boolean,
    sections: List<SidebarSection>,
    menuExpanded: Boolean,
    onOpen: () -> Unit,
    onBotDetails: () -> Unit,
    onTogglePin: () -> Unit,
    onMoveToSection: (String?) -> Unit,
    onOpenMenu: () -> Unit,
    onDismissMenu: () -> Unit,
) {
    val timestamp = conversationTimestampLabel(
        row.updatedAt,
        yesterday = localizedText("Yesterday", "昨天"),
    )
    val haptic = LocalAppHaptics.current
    Box {
        Row(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .testTag("conversation-${row.id}")
                .combinedClickable(
                    onClick = { haptic.play(HapticEffect.Light); onOpen() },
                    onLongClick = { haptic.play(HapticEffect.Medium); onOpenMenu() },
                )
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ConversationAvatar(row, accountName, members, size = 46.dp, showWorking = true)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        row.title,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                    )
                    if (timestamp != null) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            timestamp,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .8f),
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        row.preview ?: localizedText("No messages yet", "还没有消息"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                    )
                    if (unread) {
                        Box(
                            Modifier.padding(start = 8.dp).size(10.dp).clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondary),
                        )
                    }
                }
            }
        }
        DropdownMenu(expanded = menuExpanded, onDismissRequest = onDismissMenu) {
            DropdownMenuItem(
                text = {
                    Text(
                        if (pinned) localizedText("Unpin", "取消置顶")
                        else localizedText("Pin to top", "置顶"),
                    )
                },
                onClick = { onDismissMenu(); onTogglePin() },
            )
            DropdownMenuItem(
                text = {
                    Text(
                        if (row.isGroup) localizedText("Group settings", "群聊设置")
                        else localizedText("Bot details", "机器人详情"),
                    )
                },
                onClick = { onDismissMenu(); onBotDetails() },
            )
            sections.forEach { section ->
                DropdownMenuItem(
                    text = { Text(localizedText("Move to \"${section.name}\"", "移动到「${section.name}」")) },
                    onClick = { onDismissMenu(); onMoveToSection(section.id) },
                )
            }
            DropdownMenuItem(
                text = { Text(localizedText("Unassigned", "未分组")) },
                onClick = { onDismissMenu(); onMoveToSection(null) },
            )
        }
    }
}

@Composable
private fun DirectorySectionHeader(
    name: String,
    collapsed: Boolean,
    count: Int,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(top = 18.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (collapsed) "$name $count" else name,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(6.dp))
        Icon(
            if (collapsed) Icons.AutoMirrored.Filled.KeyboardArrowRight else Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .6f),
            modifier = Modifier.size(16.dp),
        )
    }
}

private fun conversationTimestampLabel(iso: String?, yesterday: String): String? {
    if (iso.isNullOrBlank()) return null
    val zoned = runCatching { Instant.parse(iso).atZone(ZoneId.systemDefault()) }.getOrNull() ?: return null
    val date = zoned.toLocalDate()
    val today = LocalDate.now(zoned.zone)
    return when {
        date == today -> DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()).format(zoned)
        date == today.minusDays(1) -> yesterday
        date.isAfter(today.minusDays(7)) ->
            DateTimeFormatter.ofPattern("EEEE", Locale.getDefault()).format(zoned)
        else -> DateTimeFormatter.ofPattern("M/d/yy", Locale.getDefault()).format(zoned)
    }
}

@Composable
private fun ConversationAvatar(
    conversation: Conversation,
    accountName: String,
    members: List<Conversation>,
    size: androidx.compose.ui.unit.Dp,
    showWorking: Boolean = false,
    ringColor: Color = MaterialTheme.colorScheme.surfaceVariant,
) {
    if (conversation.isGroup) {
        GroupAvatar(
            conversation = conversation,
            accountName = accountName,
            members = members,
            size = size,
            showWorking = showWorking,
            ringColor = ringColor,
        )
    } else {
        BotAvatar(conversation, size = size, showWorking = showWorking, ringColor = ringColor)
    }
}

@Composable
private fun GroupAvatar(
    conversation: Conversation,
    accountName: String,
    members: List<Conversation>,
    size: androidx.compose.ui.unit.Dp,
    showWorking: Boolean = false,
    ringColor: Color = MaterialTheme.colorScheme.surfaceVariant,
) {
    val avatar = rememberDecodedBitmap(conversation.avatarPath, maxSizePx = (size.value * 4).toInt())
    val visibleMembers = members.take(2)
    val memberSize = if (visibleMembers.size == 1) size * (2f / 3f) else size * (5f / 9f)
    val accountSize = if (visibleMembers.isEmpty()) size else memberSize
    Box(Modifier.size(size)) {
        if (avatar != null) {
            Image(
                bitmap = avatar,
                contentDescription = conversation.title,
                modifier = Modifier.size(size).clip(CircleShape),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            )
        } else if (visibleMembers.isEmpty()) {
            Box(
                Modifier.size(size).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .border(.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = .45f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Group,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(size * .55f),
                )
            }
        } else {
            Box(
                Modifier.align(if (visibleMembers.size == 1) Alignment.TopStart else Alignment.TopCenter)
                    .size(accountSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = .55f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    accountInitials(accountName),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = (size.value * if (visibleMembers.size == 1) .17f else .11f).sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                )
            }
            visibleMembers.forEachIndexed { index, member ->
                Box(
                    Modifier.align(if (visibleMembers.size == 1 || index == 1) Alignment.BottomEnd else Alignment.BottomStart)
                        .size(memberSize)
                        .clip(CircleShape),
                ) {
                    BotAvatar(member, size = memberSize)
                }
            }
        }
        if (showWorking && conversation.working) {
            val dot = (size.value * .26f).coerceIn(10f, 18f).dp
            Box(
                Modifier.align(Alignment.BottomEnd).size(dot).clip(CircleShape)
                    .background(Color(0xFF5BC67A))
                    .border(2.dp, ringColor, CircleShape)
                    .semantics { contentDescription = "Working" },
            )
        }
    }
}

private fun accountInitials(accountName: String): String {
    val parts = accountName.trim().split(Regex("\\s+")).filter(String::isNotBlank)
    return parts
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifBlank { accountName.trim().take(1).uppercase().ifBlank { "U" } }
}

@Composable
private fun BotAvatar(
    conversation: Conversation,
    size: androidx.compose.ui.unit.Dp,
    showWorking: Boolean = false,
    ringColor: Color = MaterialTheme.colorScheme.surfaceVariant,
) {
    val fill = conversation.avatarColor.toComposeColor()
        ?: MaterialTheme.colorScheme.secondaryContainer
    val avatar = rememberDecodedBitmap(conversation.avatarPath, maxSizePx = (size.value * 4).toInt())
    Box {
        if (avatar != null) {
            Image(
                bitmap = avatar,
                contentDescription = conversation.title,
                modifier = Modifier.size(size).clip(CircleShape),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            )
        } else {
            ServerBotAvatar(conversation.botIcon, fill, size)
        }
        if (showWorking && conversation.working) {
            val dot = (size.value * .26f).coerceIn(10f, 18f).dp
            Box(
                Modifier.align(Alignment.BottomEnd).size(dot).clip(CircleShape)
                    .background(Color(0xFF5BC67A))
                    .border(2.dp, ringColor, CircleShape)
                    .semantics { contentDescription = "Working" },
            )
        }
    }
}

@Composable
private fun ServerBotAvatar(icon: String, fill: Color, size: androidx.compose.ui.unit.Dp) {
    Canvas(Modifier.size(size).clip(CircleShape)) {
        drawCircle(fill)
        val edge = this.size.minDimension
        val face = if (fill.luminance() < .45f) Color.White else Color.Black
        val left = edge * .2f
        val top = edge * .2f
        val width = edge * .6f
        val height = edge * .57f
        when (icon.trim().lowercase()) {
            "tv-head", "dual-screen" -> drawRect(face, androidx.compose.ui.geometry.Offset(left, top), androidx.compose.ui.geometry.Size(width, height))
            "pod", "owl" -> drawOval(face, androidx.compose.ui.geometry.Offset(left, top), androidx.compose.ui.geometry.Size(width, height))
            else -> drawRoundRect(
                face,
                androidx.compose.ui.geometry.Offset(left, top),
                androidx.compose.ui.geometry.Size(width, height),
                androidx.compose.ui.geometry.CornerRadius(edge * .14f),
            )
        }
        if (icon.trim().lowercase() in setOf("terminal", "periscope", "helmet")) {
            drawLine(face, androidx.compose.ui.geometry.Offset(edge * .5f, top), androidx.compose.ui.geometry.Offset(edge * .5f, edge * .08f), strokeWidth = edge * .05f)
            drawCircle(face, radius = edge * .06f, center = androidx.compose.ui.geometry.Offset(edge * .5f, edge * .06f))
        }
        val eyeY = edge * .42f
        drawCircle(fill, radius = edge * .07f, center = androidx.compose.ui.geometry.Offset(edge * .38f, eyeY))
        drawCircle(fill, radius = edge * .07f, center = androidx.compose.ui.geometry.Offset(edge * .62f, eyeY))
        drawRoundRect(
            fill,
            androidx.compose.ui.geometry.Offset(edge * .38f, edge * .58f),
            androidx.compose.ui.geometry.Size(edge * .24f, edge * .06f),
            androidx.compose.ui.geometry.CornerRadius(edge * .03f),
        )
    }
}

private fun String.toComposeColor(): Color? {
    val value = trim().removePrefix("#")
    if (value.length != 6) return null
    return value.toLongOrNull(16)?.let { Color(it.toInt() or 0xFF000000.toInt()) }
}

@Composable
private fun FloatingSearchBar(onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .35f), RoundedCornerShape(28.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 13.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(12.dp))
            Text(
                localizedText("Search", "搜索"),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun ExpandedSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    focusRequester: FocusRequester,
    onClear: () -> Unit,
    onClose: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().imePadding().padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f).focusRequester(focusRequester),
            singleLine = true,
            placeholder = { Text(localizedText("Search", "搜索")) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = onClear) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = localizedText("Clear", "清除"),
                        )
                    }
                }
            },
            shape = RoundedCornerShape(28.dp),
        )
        IconButton(
            onClick = onClose,
            modifier = Modifier.size(56.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = .35f),
                    CircleShape,
                ),
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = localizedText("Close search", "关闭搜索"),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BotDetailsScreen(
    conversation: Conversation?,
    onBack: () -> Unit,
    onOpenChat: () -> Unit,
    onComputer: () -> Unit,
    onRoutines: () -> Unit,
    onEdit: () -> Unit,
    onHide: (Conversation) -> Unit,
    onDelete: (Conversation) -> Unit,
    onDuplicate: (Conversation) -> Unit,
    onNotifications: (Conversation, Boolean) -> Unit,
    onShare: () -> Unit,
) {
    var moreMenuVisible by remember { mutableStateOf(false) }
    var notificationsEnabled by remember(conversation?.id) { mutableStateOf(conversation?.notificationsEnabled ?: true) }
    val surface = MaterialTheme.colorScheme.surface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            ProfileChromeButton(
                contentDescription = localizedText("Back to conversations", "返回对话列表"),
                onClick = onBack,
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            }
            Spacer(Modifier.weight(1f))
            ProfileChromeButton(
                contentDescription = localizedText("Share bot", "分享机器人"),
                onClick = onShare,
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
            }
            Box {
                ProfileChromeButton(
                    contentDescription = localizedText("More settings", "更多设置"),
                    onClick = { moreMenuVisible = true },
                ) {
                    Icon(Icons.Default.MoreHoriz, contentDescription = null)
                }
                DropdownMenu(
                    expanded = moreMenuVisible,
                    onDismissRequest = { moreMenuVisible = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(localizedText("Open conversation", "打开对话")) },
                        onClick = { moreMenuVisible = false; onOpenChat() },
                    )
                    DropdownMenuItem(
                        text = { Text(localizedText("Edit profile", "编辑资料")) },
                        onClick = { moreMenuVisible = false; onEdit() },
                    )
                    conversation?.let { row ->
                        DropdownMenuItem(
                            text = { Text(localizedText("Duplicate bot", "复制机器人")) },
                            onClick = { moreMenuVisible = false; onDuplicate(row) },
                        )
                        DropdownMenuItem(
                            text = { Text(localizedText("Hide from sidebar", "从侧边栏隐藏")) },
                            onClick = { moreMenuVisible = false; onHide(row) },
                        )
                        DropdownMenuItem(
                            text = { Text(localizedText("Delete", "删除"), color = MaterialTheme.colorScheme.error) },
                            onClick = { moreMenuVisible = false; onDelete(row) },
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        conversation?.let {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                BotAvatar(it, 96.dp)
                Spacer(Modifier.height(12.dp))
                Text(it.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier.clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .clickable(onClick = onEdit)
                        .padding(horizontal = 18.dp, vertical = 9.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(localizedText("Edit", "编辑"), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        Spacer(Modifier.height(28.dp))
        ProfileGroupedCard(surface) {
            ProfileCapabilityRow(
                icon = Icons.Default.AccessTime,
                title = localizedText("Scheduled routines", "定时任务"),
                status = localizedText("0 enabled", "已开启 0 个"),
                onClick = onRoutines,
            )
            ProfileDivider(muted)
            ProfileCapabilityRow(
                icon = Icons.Default.FolderOpen,
                title = localizedText("Library", "资料库"),
                status = null,
                onClick = onOpenChat,
            )
            ProfileDivider(muted)
            ProfileCapabilityRow(
                icon = Icons.Default.Computer,
                title = localizedText("Cloud computer", "云电脑"),
                status = localizedText("Idle", "空闲"),
                onClick = onComputer,
            )
        }

        Spacer(Modifier.height(18.dp))
        Column(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(surface)
                .border(1.dp, muted.copy(alpha = .12f), RoundedCornerShape(20.dp)),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Notifications, contentDescription = null, tint = muted)
                Spacer(Modifier.width(14.dp))
                Text(
                    localizedText("Notifications", "通知"),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = notificationsEnabled, onCheckedChange = {
                    notificationsEnabled = it
                    conversation?.let { row -> onNotifications(row, it) }
                })
            }
        }
        Text(
            localizedText("The bot will notify you when work is ready for your attention.", "机器人完成任务或需要你处理时通知你。"),
            color = muted,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun CreateConversationScreen(
    bots: List<Conversation>,
    onCreateBot: (String, String, String, String, String) -> Unit,
    onCreateGroup: (String, List<String>) -> Unit,
    onBack: () -> Unit,
) {
    val haptic = LocalAppHaptics.current
    var group by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var instructions by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf("chip") }
    var color by remember { mutableStateOf("#A47952") }
    val selected = remember { mutableStateOf(emptySet<String>()) }
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, localizedText("Back", "返回")) } },
                title = { Text(if (group) localizedText("Create group", "创建群聊") else localizedText("Create bot", "创建机器人")) },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !group, onClick = { group = false }, label = { Text(localizedText("Bot", "机器人")) })
                FilterChip(selected = group, onClick = { group = true }, label = { Text(localizedText("Group", "群聊")) })
            }
            OutlinedTextField(name, { name = it }, label = { Text(localizedText("Name", "名称")) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(description, { description = it }, label = { Text(localizedText("Description", "描述")) }, modifier = Modifier.fillMaxWidth())
            if (group) {
                Text(localizedText("Members", "成员"), fontWeight = FontWeight.SemiBold)
                bots.forEach { bot ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.Checkbox(
                            checked = bot.botId?.let(selected.value::contains) == true,
                            onCheckedChange = { checked ->
                                selected.value = if (checked) selected.value + bot.botId.orEmpty() else selected.value - bot.botId.orEmpty()
                            },
                        )
                        Text(bot.title)
                    }
                }
            } else {
                OutlinedTextField(instructions, { instructions = it }, label = { Text(localizedText("Instructions", "说明")) }, modifier = Modifier.fillMaxWidth(), minLines = 5)
                OutlinedTextField(icon, { icon = it }, label = { Text(localizedText("Icon", "图标")) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(color, { color = it }, label = { Text(localizedText("Color (#RRGGBB)", "颜色（#RRGGBB）")) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            }
            Button(
                onClick = {
                    haptic.play(HapticEffect.Light)
                    if (group) onCreateGroup(name, selected.value.filter(String::isNotBlank))
                    else onCreateBot(name, description, instructions, icon, color)
                },
                enabled = name.isNotBlank() && (!group || selected.value.isNotEmpty()),
                modifier = Modifier.fillMaxWidth(),
            ) { Text(localizedText("Create", "创建")) }
        }
    }
}

@Composable
private fun ConversationProfileScreen(
    conversation: Conversation?,
    bots: List<Conversation>,
    onSaveBot: (Conversation, String, String, String, String, String, String) -> Unit,
    onSaveGroup: (Conversation, String, String, List<String>) -> Unit,
    onBack: () -> Unit,
) {
    val haptic = LocalAppHaptics.current
    if (conversation == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(localizedText("Conversation is unavailable.", "对话不可用。")) }
        return
    }
    var name by remember(conversation.id) { mutableStateOf(conversation.title) }
    var description by remember(conversation.id) { mutableStateOf(conversation.subtitle) }
    var instructions by remember { mutableStateOf("") }
    var voice by remember(conversation.id) { mutableStateOf(conversation.botVoice) }
    var icon by remember { mutableStateOf("chip") }
    var color by remember { mutableStateOf(conversation.avatarColor.ifBlank { "#A47952" }) }
    val selected = remember(conversation.id) { mutableStateOf(conversation.memberBotIds.toSet()) }
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, localizedText("Back", "返回")) } },
                title = { Text(localizedText("Edit profile", "编辑资料")) },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text(localizedText("Name", "名称")) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(description, { description = it }, label = { Text(localizedText("Description", "描述")) }, modifier = Modifier.fillMaxWidth())
            if (conversation.isGroup) {
                Text(localizedText("Members", "成员"), fontWeight = FontWeight.SemiBold)
                bots.forEach { bot ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.Checkbox(
                            checked = bot.botId?.let(selected.value::contains) == true,
                            onCheckedChange = { checked -> selected.value = if (checked) selected.value + bot.botId.orEmpty() else selected.value - bot.botId.orEmpty() },
                        )
                        Text(bot.title)
                    }
                }
            } else {
                OutlinedTextField(instructions, { instructions = it }, label = { Text(localizedText("Instructions", "说明")) }, modifier = Modifier.fillMaxWidth(), minLines = 5)
                OutlinedTextField(voice, { voice = it }, label = { Text(localizedText("Realtime voice", "实时语音")) }, supportingText = { Text(localizedText("Provider-specific voice or speaker ID. Leave blank for the server default.", "服务商专用的声音或说话人 ID。留空使用服务器默认值。")) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(icon, { icon = it }, label = { Text(localizedText("Icon", "图标")) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(color, { color = it }, label = { Text(localizedText("Color (#RRGGBB)", "颜色（#RRGGBB）")) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            }
            Button(
                onClick = {
                    haptic.play(HapticEffect.Light)
                    if (conversation.isGroup) onSaveGroup(conversation, name, description, selected.value.toList())
                    else onSaveBot(conversation, name, description, instructions, voice, icon, color)
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text(localizedText("Save", "保存")) }
        }
    }
}

@Composable
private fun ProfileChromeButton(
    contentDescription: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .semantics { this.contentDescription = contentDescription },
    ) {
        content()
    }
}

@Composable
private fun ProfileGroupedCard(
    surface: Color,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(surface)
            .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .12f), RoundedCornerShape(24.dp)),
        content = content,
    )
}

@Composable
private fun ProfileDivider(muted: Color) {
    HorizontalDivider(
        modifier = Modifier.padding(start = 56.dp),
        color = muted.copy(alpha = .12f),
    )
}

@Composable
private fun ProfileCapabilityRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    status: String?,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (!status.isNullOrBlank()) {
            Text(status, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(10.dp))
        }
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.rotate(180f), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun IosActionCard(
    title: String,
    detail: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(42.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Medium)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.rotate(180f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MarketplaceScreen(
    state: FeatureLoadState<List<Plugin>>,
    onBack: () -> Unit,
    onOpenExtensions: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(com.outcomego.android.R.string.marketplace_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = localizedText("Back to conversations", "返回对话列表"))
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(stringResource(com.outcomego.android.R.string.marketplace_detail), color = MaterialTheme.colorScheme.onSurfaceVariant)
            IosActionCard(
                title = stringResource(com.outcomego.android.R.string.marketplace_extensions),
                detail = when (state) {
                    is FeatureLoadState.Ready -> stringResource(com.outcomego.android.R.string.marketplace_extensions_count, state.value.size)
                    is FeatureLoadState.Loading -> stringResource(com.outcomego.android.R.string.marketplace_loading)
                    is FeatureLoadState.Empty -> state.message
                    is FeatureLoadState.Failure -> state.message
                },
                icon = Icons.Default.Add,
                onClick = onOpenExtensions,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ComputerViewerScreen(
    title: String,
    botId: String,
    server: String?,
    requestGrant: suspend (String) -> Result<ComputerVncGrant>,
    requestStatus: suspend (String) -> Result<JsonObject>,
    setTakeover: suspend (String, Boolean, Boolean) -> Result<JsonObject>,
    setPaused: suspend (String, Boolean) -> Result<JsonObject>,
    sendAction: suspend (String, JsonObject) -> Result<JsonObject>,
    sendClipboard: suspend (String, String) -> Result<JsonObject>,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val renderer = remember { AndroidVncWebViewRenderer(context) }
    val rendererState by renderer.state.collectAsStateWithLifecycle()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var status by remember { mutableStateOf<JsonObject?>(null) }
    var failure by remember { mutableStateOf<String?>(null) }
    var clipboardText by remember { mutableStateOf("") }
    var clipboardOpen by remember { mutableStateOf(false) }
    val controlling = status?.get("humanTakeover")?.toString()?.toBoolean() == true
    val paused = status?.get("agentInputPaused")?.toString()?.toBoolean() == true
    DisposableEffect(renderer) {
        onDispose { renderer.detach() }
    }
    LaunchedEffect(renderer, botId, server) {
        if (server.isNullOrBlank()) {
            renderer.onSocketError("Cloud computer viewing requires a connected server.")
            return@LaunchedEffect
        }
        if (botId.isBlank()) {
            renderer.onSocketError("This conversation is not linked to a bot.")
            return@LaunchedEffect
        }
        var retryDelay = 500L
        while (isActive) {
            val grant = requestGrant(botId).getOrElse {
                failure = it.message ?: "Unable to request the computer viewer."
                renderer.prepareForReconnect()
                null
            }
            if (grant != null) {
                runCatching { renderer.connect(grant, URI(server), botId) }
                    .onSuccess {
                        failure = null
                        retryDelay = 500L
                    }
                    .onFailure {
                        failure = it.message ?: "Unable to connect to the computer."
                        renderer.prepareForReconnect()
                    }
                while (isActive && renderer.state.value == VncRendererState.Connected) {
                    delay(250)
                }
            }
            if (!isActive) break
            delay(retryDelay)
            retryDelay = (retryDelay * 2).coerceAtMost(5_000L)
        }
    }
    LaunchedEffect(botId, server, controlling) {
        if (botId.isBlank() || server.isNullOrBlank()) return@LaunchedEffect
        while (isActive) {
            requestStatus(botId).onSuccess { status = it }.onFailure { failure = it.message }
            if (controlling) {
                setTakeover(botId, true, true).onSuccess { status = it }.onFailure { failure = it.message }
            }
            delay(20_000)
        }
    }
    LaunchedEffect(rendererState, controlling, paused) {
        if (rendererState == VncRendererState.Connected) {
            renderer.setControl(controlling && !paused)
        }
    }
    DisposableEffect(botId) {
        onDispose {
            if (controlling) scope.launch { setTakeover(botId, false, false) }
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                actions = {
                    TextButton(onClick = {
                        scope.launch {
                            setTakeover(botId, !controlling, false).fold(
                                onSuccess = { status = it; failure = null },
                                onFailure = { failure = it.message },
                            )
                        }
                    }) { Text(if (controlling) localizedText("Release", "释放控制") else localizedText("Take control", "接管控制")) }
                    TextButton(onClick = {
                        scope.launch {
                            setPaused(botId, !paused).fold(
                                onSuccess = { status = it; failure = null },
                                onFailure = { failure = it.message },
                            )
                        }
                    }) { Text(if (paused) localizedText("Resume", "继续") else localizedText("Pause", "暂停")) }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = localizedText("Back to conversations", "返回对话列表"))
                    }
                },
            )
        },
        containerColor = Color.Black,
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { viewContext -> WebView(viewContext).also(renderer::attach) },
            )
            if (rendererState != VncRendererState.Connected) {
                Card(
                    Modifier.align(Alignment.Center).padding(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF202020)),
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(com.outcomego.android.R.string.computer_title), color = Color.White, style = MaterialTheme.typography.titleLarge)
                        Text(
                            when (val state = rendererState) {
                                is VncRendererState.Failed -> state.message
                                is VncRendererState.Reconnecting -> {
                                    val reason = state.reason.trim()
                                    if (reason.isBlank()) "Computer connection interrupted. Reconnecting…"
                                    else "Computer connection interrupted. Reconnecting…\n$reason"
                                }
                                else -> stringResource(com.outcomego.android.R.string.computer_connecting)
                            },
                            color = Color.White.copy(alpha = .75f),
                        )
                    }
                }
            }
            // The toolbar sits on the dark remote-screen overlay, so it keeps light content,
            // but uses button colors so disabled actions (before Take control) read as disabled.
            val toolbarButtonColors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                contentColor = Color.White,
                disabledContentColor = Color.White.copy(alpha = .38f),
            )
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.Black.copy(alpha = .82f)).padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                failure?.let { Text(it, color = Color(0xFFFF8A80), style = MaterialTheme.typography.bodySmall) }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(onClick = { clipboardOpen = true }, enabled = controlling && !paused, colors = toolbarButtonColors) { Text(localizedText("Clipboard", "剪贴板")) }
                    listOf("Return", "Tab", "Escape", "BackSpace").forEach { key ->
                        TextButton(onClick = { scope.launch { runCatching { renderer.send(com.outcomego.platform.ComputerInput.Key(listOf(key))) } } }, enabled = controlling && !paused, colors = toolbarButtonColors) {
                            Text(key)
                        }
                    }
                }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("chromium" to "Browser", "thunar" to "Files", "terminal" to "Terminal").forEach { (app, label) ->
                        TextButton(onClick = { scope.launch { sendAction(botId, buildJsonObject { put("action", "open_app"); put("app", app) }) } }, enabled = controlling && !paused, colors = toolbarButtonColors) {
                            Text(label)
                        }
                    }
                }
            }
        }
    }
    if (clipboardOpen) {
        AlertDialog(
            onDismissRequest = { clipboardOpen = false },
            title = { Text(localizedText("Send text to computer", "发送文本到云电脑")) },
            text = { OutlinedTextField(clipboardText, { clipboardText = it }, label = { Text(localizedText("Text", "文本")) }, minLines = 3) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        sendClipboard(botId, clipboardText).onFailure { failure = it.message }
                        clipboardOpen = false
                    }
                }, enabled = clipboardText.isNotBlank() && controlling && !paused) { Text(localizedText("Send", "发送")) }
            },
            dismissButton = { TextButton(onClick = { clipboardOpen = false }) { Text(localizedText("Cancel", "取消")) } },
        )
    }
}

@Composable
private fun IosChromeIcon(
    icon: @Composable () -> Unit,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(44.dp).clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = .58f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .35f), CircleShape),
    ) {
        CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides MaterialTheme.colorScheme.onSurface,
        ) {
            icon()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatScreen(
    conversationId: String,
    accountName: String,
    title: String,
    messages: List<ChatMessage>,
    targetMessageId: String?,
    draft: String,
    replyingTo: ChatMessage?,
    onReply: (String) -> Unit,
    onCancelReply: () -> Unit,
    onOpenRoutine: () -> Unit,
    error: String?,
    onBack: () -> Unit,
    onBotDetails: () -> Unit,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onRetry: (String) -> Unit,
    voiceState: VoiceRecordingState,
    onStartVoice: () -> Unit,
    onStopVoice: () -> Unit,
    onRetryVoice: () -> Unit,
    onCancelVoice: () -> Unit,
    liveVoice: com.outcomego.platform.AndroidLiveVoiceSnapshot,
    /** Starts the call. [participants] and [speaker] are only meaningful in a group chat. */
    onStartLiveVoice: (participants: List<String>, speaker: String?) -> Unit,
    /** The bots in this group, for the call's member picker. Empty outside a group chat. */
    groupMembers: List<Conversation>,
    onStopLiveVoice: () -> Unit,
    onMuteLiveVoiceMicrophone: (Boolean) -> Unit,
    onMuteLiveVoiceSpeaker: (Boolean) -> Unit,
    operation: FeatureOperationState,
    pendingAttachments: List<Attachment>,
    hasMoreHistory: Boolean,
    historyLoading: Boolean,
    conversations: List<Conversation>,
    onLoadOlder: () -> Unit,
    onUpload: (String) -> Unit,
    onDownload: (String, Attachment) -> Unit,
    onSaveAttachment: (String, Attachment) -> Unit,
    onShareAttachment: (Attachment) -> Unit,
    onForwardAttachment: (Attachment, String) -> Unit,
    onAction: (String, String, Map<String, String>) -> Unit,
    onReaction: (String, String) -> Unit,
    onMessageAction: (String, MessageAction) -> Unit,
    onContext: (String) -> Unit,
    threadContext: List<ChatMessage>,
    threadContextTruncated: Boolean,
    activity: ChannelActivity? = null,
    bot: Conversation? = null,
) {
    val listState = rememberLazyListState()
    var forwardingAttachment by remember { mutableStateOf<Attachment?>(null) }
    val attachmentPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { onUpload(it.toString()) } }
    val contextRows = threadContext.take(com.outcomego.android.data.ChatHistoryPage.DEFAULT_CONTEXT_LIMIT)
    val chatRows = remember(messages) { messages.toChatRows() }
    var hasPositionedInitialMessages by remember(conversationId) { mutableStateOf(false) }
    // The list ends with the bot-activity row and a 4dp spacer. Scrolling to that last item can only
    // stop at the end of the list, so the newest message is always shown in full, however tall.
    val lastItemIndex = (if (hasMoreHistory) 1 else 0) + (if (contextRows.isNotEmpty()) 1 else 0) + chatRows.size + 1
    val currentLastItemIndex by rememberUpdatedState(lastItemIndex)
    val currentTargetMessageId by rememberUpdatedState(targetMessageId)
    val bottomThresholdPx = with(LocalDensity.current) { 48.dp.toPx() }
    // True while the reader is at (or within a thumb's width of) the newest message. It is only
    // re-evaluated when a scroll settles, so list growth between two scrolls cannot flip it.
    var followBottom by remember(conversationId) { mutableStateOf(true) }
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }.collect { scrolling ->
            if (!scrolling) followBottom = listState.isNearBottom(bottomThresholdPx)
        }
    }
    var lastScrolledId by remember(conversationId) { mutableStateOf<String?>(null) }
    val newest = messages.lastOrNull()
    // Keyed on the whole newest message so edits, reactions, attachments and delivery changes that
    // alter its height also re-run this, not only a new message id.
    LaunchedEffect(conversationId, historyLoading, targetMessageId, messages.size, newest, activity) {
        if (historyLoading || messages.isEmpty()) return@LaunchedEffect
        val targetIndex = targetMessageId?.let { id -> chatRows.indexOfFirst { it.contains(id) } }?.takeIf { it >= 0 }
        if (targetIndex != null) {
            listState.scrollToItem(targetIndex + if (contextRows.isNotEmpty()) 1 else 0)
            hasPositionedInitialMessages = true
            return@LaunchedEffect
        }
        val isNewMessage = newest?.id != lastScrolledId
        lastScrolledId = newest?.id
        when {
            !hasPositionedInitialMessages -> {
                listState.scrollToItem(lastItemIndex)
                hasPositionedInitialMessages = true
                followBottom = true
            }
            // Your own message always jumps to the bottom, even if you had scrolled up.
            followBottom || (isNewMessage && newest?.isMine == true) -> {
                if (isNewMessage || activity != null) listState.animateScrollToItem(lastItemIndex)
                else listState.scrollToItem(lastItemIndex)
                followBottom = true
            }
        }
    }
    // The viewport shrinks when the keyboard opens or the composer grows; stay on the newest message.
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.viewportSize.height }.drop(1).collect {
            if (followBottom && hasPositionedInitialMessages && currentTargetMessageId == null) {
                listState.scrollToItem(currentLastItemIndex)
            }
        }
    }
    // Jumping to a quoted original is local to this screen. Routing it through targetMessageId
    // would keep the list pinned to that message and drag it back after every new message.
    var jumpToMessageId by remember(conversationId) { mutableStateOf<String?>(null) }
    var highlightedMessageId by remember(conversationId) { mutableStateOf<String?>(null) }
    LaunchedEffect(jumpToMessageId, chatRows) {
        val id = jumpToMessageId ?: return@LaunchedEffect
        val index = chatRows.indexOfFirst { it.contains(id) }
        if (index < 0) return@LaunchedEffect
        listState.animateScrollToItem(index + if (contextRows.isNotEmpty()) 1 else 0)
        jumpToMessageId = null
        highlightedMessageId = id
        delay(1500)
        if (highlightedMessageId == id) highlightedMessageId = null
    }
    forwardingAttachment?.let { attachment ->
        AlertDialog(
            onDismissRequest = { forwardingAttachment = null },
            title = { Text(stringResource(com.outcomego.android.R.string.chat_forward)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${stringResource(com.outcomego.android.R.string.chat_forward)}: ${attachment.name}")
                    conversations.forEach { conversation ->
                        TextButton(
                            onClick = {
                                onForwardAttachment(attachment, conversation.id)
                                forwardingAttachment = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(conversation.title, textAlign = androidx.compose.ui.text.style.TextAlign.Start) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { forwardingAttachment = null }) { Text(stringResource(com.outcomego.android.R.string.login_cancel)) } },
        )
    }
    val haptic = LocalAppHaptics.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val composerFocusRequester = remember { FocusRequester() }
    var isComposerFocused by remember(conversationId) { mutableStateOf(false) }
    val imeOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    var wasImeOpen by remember(conversationId) { mutableStateOf(false) }
    LaunchedEffect(imeOpen) {
        if (wasImeOpen && !imeOpen && isComposerFocused) {
            focusManager.clearFocus()
        }
        wasImeOpen = imeOpen
    }
    var pickingVoiceMembers by remember { mutableStateOf(false) }
    if (pickingVoiceMembers) {
        VoiceMemberPickerDialog(
            members = groupMembers,
            onDismiss = { pickingVoiceMembers = false },
            onStart = { participants, speaker ->
                pickingVoiceMembers = false
                onStartLiveVoice(participants, speaker)
            },
        )
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (bot?.isGroup == true) {
                            ConversationAvatar(
                                conversation = bot,
                                accountName = accountName,
                                members = groupMembers,
                                size = 32.dp,
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("back-to-conversations"),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = localizedText("Back to conversations", "返回对话列表"),
                        )
                    }
                },
                actions = {
                    if (bot?.isBot == true || (bot?.isGroup == true && groupMembers.isNotEmpty())) {
                        IosChromeIcon(
                            icon = {
                                Icon(
                                    Icons.Default.Call,
                                    contentDescription = null,
                                )
                            },
                            contentDescription = localizedText(
                                if (liveVoice.state == com.outcomego.platform.AndroidLiveVoiceState.Idle) "Start realtime voice" else "End realtime voice",
                                if (liveVoice.state == com.outcomego.platform.AndroidLiveVoiceState.Idle) "开始实时语音" else "结束实时语音",
                            ),
                            onClick = {
                                haptic.play(HapticEffect.Medium)
                                if (liveVoice.state != com.outcomego.platform.AndroidLiveVoiceState.Idle) onStopLiveVoice()
                                else if (bot?.isGroup == true) pickingVoiceMembers = true
                                else onStartLiveVoice(emptyList(), null)
                            },
                            enabled = liveVoice.state != com.outcomego.platform.AndroidLiveVoiceState.Connecting,
                            modifier = Modifier.testTag("live-voice-header"),
                        )
                    }
                    IosChromeIcon(
                        icon = {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                            )
                        },
                        contentDescription = stringResource(com.outcomego.android.R.string.chat_bot_details),
                        onClick = onBotDetails,
                        modifier = Modifier.testTag("bot-details"),
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).windowInsetsPadding(WindowInsets.navigationBars).imePadding().padding(horizontal = 14.dp, vertical = 10.dp)) {
                if (error != null) Text(error, Modifier.padding(start = 4.dp, bottom = 6.dp), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                when (voiceState) {
                    VoiceRecordingState.Idle -> Unit
                    is VoiceRecordingState.Starting -> Text(stringResource(com.outcomego.android.R.string.chat_record_voice), style = MaterialTheme.typography.bodySmall)
                    is VoiceRecordingState.Recording -> voiceControls(localizedText("Recording voice note", "正在录制语音消息"), onStopVoice, onCancelVoice)
                    is VoiceRecordingState.Interrupted -> voiceControls(localizedText("Recording paused", "录音已暂停"), onStopVoice, onCancelVoice)
                    is VoiceRecordingState.Transcribing -> voiceControls("Transcribing…", null, onCancelVoice)
                    is VoiceRecordingState.Review -> {
                        Text(voiceState.error ?: localizedText("Transcription failed", "语音转写失败"), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { haptic.play(HapticEffect.Light); onRetryVoice() }) { Text(localizedText("Retry", "重试")) }
                            TextButton(onClick = { haptic.play(HapticEffect.Warning); onCancelVoice() }) { Text(localizedText("Discard", "丢弃")) }
                        }
                    }
                    is VoiceRecordingState.Failed -> {
                        Text(voiceState.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = onCancelVoice) { Text(localizedText("Dismiss", "关闭")) }
                    }
                }
                replyingTo?.let { target ->
                    ReplyPreviewBar(target, onCancelReply)
                }
                if (pendingAttachments.isNotEmpty()) {
                    Box(Modifier.padding(bottom = 8.dp)) {
                        ChatAttachmentStrip(pendingAttachments, onPreview = { _, _ -> }, onDownload = { _, _ -> })
                    }
                }
                ChatComposer(
                    draft = draft,
                    onDraftChange = onDraftChange,
                    hasPendingAttachments = pendingAttachments.isNotEmpty(),
                    voiceState = voiceState,
                    onAttach = { attachmentPicker.launch(arrayOf("*/*")) },
                    onStartVoice = onStartVoice,
                    onSend = onSend,
                    isFocused = isComposerFocused,
                    onFocusChange = { isComposerFocused = it },
                    focusRequester = composerFocusRequester,
                )
                when (operation) {
                    FeatureOperationState.Idle -> Unit
                    FeatureOperationState.Pending -> Text(localizedText("Working…", "处理中…"), Modifier.testTag("chat-operation-status"), style = MaterialTheme.typography.bodySmall)
                    is FeatureOperationState.Succeeded -> Unit
                    is FeatureOperationState.Failed -> Text(operation.message, Modifier.testTag("chat-operation-status"), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .pointerInput(isComposerFocused) {
                    if (!isComposerFocused) return@pointerInput
                    val touchSlop = viewConfiguration.touchSlop
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val up = waitForUpOrCancellation()
                        if (up != null && !up.isConsumed && (up.position - down.position).getDistance() < touchSlop) {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        }
                    }
                },
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (hasMoreHistory) {
                item(key = "load-older") {
                    TextButton(
                        onClick = onLoadOlder,
                        enabled = !historyLoading,
                        modifier = Modifier.fillMaxWidth().testTag("load-older-messages"),
                    ) {
                        if (historyLoading) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        else Text(stringResource(com.outcomego.android.R.string.chat_load_older))
                    }
                }
            }
            if (contextRows.isNotEmpty()) {
                item(key = "thread-context") {
                    ChatContextSummary(contextRows, threadContextTruncated || threadContext.size > contextRows.size)
                }
            }
            items(chatRows, key = ChatRow::key) { row ->
                if (row is ChatRow.Voice) {
                    VoiceConversationCard(row.messages, bot?.title ?: "Assistant")
                    return@items
                }
                val message = (row as ChatRow.Single).message
                MessageBubble(
                    message,
                    onRetry,
                    isTarget = message.id == targetMessageId || message.id == highlightedMessageId,
                    operation = operation,
                    onDownload = onDownload,
                    onSaveAttachment = onSaveAttachment,
                    onShareAttachment = onShareAttachment,
                    onForwardAttachment = { forwardingAttachment = it },
                    onAction = onAction,
                    onReaction = onReaction,
                    onMessageAction = onMessageAction,
                    onContext = onContext,
                    onReply = onReply,
                    onOpenRoutine = onOpenRoutine,
                    replyOriginal = message.replyToMessageId?.let { id -> messages.firstOrNull { it.id == id } },
                    onOpenOriginal = { id ->
                        // Load the surrounding history only when the original is not already in the list.
                        if (messages.none { it.id == id }) onContext(id)
                        jumpToMessageId = id
                    },
                )
            }
            item(key = "bot-activity") {
                BotActivityRow(
                    activity = activity,
                    bot = bot,
                    accountName = accountName,
                    members = groupMembers,
                )
            }
            item { Spacer(Modifier.height(4.dp)) }
        }
    }
}

/**
 * ChatGPT-style pill composer: collapses to a single row with attachment and voice/send controls
 * inside the container when unfocused, and splits into a full-width text row plus a bottom tool
 * row when focused.
 */
@Composable
internal fun ChatComposer(
    draft: String,
    onDraftChange: (String) -> Unit,
    hasPendingAttachments: Boolean,
    voiceState: VoiceRecordingState,
    onAttach: () -> Unit,
    onStartVoice: () -> Unit,
    onSend: () -> Unit,
    isFocused: Boolean,
    onFocusChange: (Boolean) -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalAppHaptics.current
    val canSend = draft.isNotBlank() || hasPendingAttachments
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("composer-container")
            .animateContentSize(animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
    ) {
        Layout(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp),
            content = {
                Box(Modifier.layoutId("attach"), contentAlignment = Alignment.Center) {
                    IconButton(
                        onClick = { haptic.play(HapticEffect.Light); onAttach() },
                        modifier = Modifier.size(40.dp).testTag("attach-composer"),
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = stringResource(com.outcomego.android.R.string.chat_add_attachment),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                Box(Modifier.layoutId("input")) {
                    BasicTextField(
                        value = draft,
                        onValueChange = onDraftChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .onFocusChanged { onFocusChange(it.isFocused) }
                            .testTag("message-composer"),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        maxLines = if (isFocused) 5 else 1,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                        ),
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(
                                        if (isFocused) {
                                            Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        } else {
                                            Modifier.heightIn(min = 40.dp).padding(horizontal = 4.dp, vertical = 8.dp)
                                        },
                                    ),
                                contentAlignment = if (isFocused) Alignment.TopStart else Alignment.CenterStart,
                            ) {
                                if (draft.isEmpty()) {
                                    Text(
                                        text = stringResource(com.outcomego.android.R.string.chat_message),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.52f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                innerTextField()
                            }
                        },
                    )
                }
                Box(Modifier.layoutId("action"), contentAlignment = Alignment.Center) {
                    if (canSend) {
                        IconButton(
                            onClick = { haptic.play(HapticEffect.Light); onSend() },
                            modifier = Modifier.size(40.dp).testTag("send-message"),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Default.ArrowUpward,
                                    contentDescription = stringResource(com.outcomego.android.R.string.chat_send),
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(19.dp),
                                )
                            }
                        }
                    } else {
                        IconButton(
                            onClick = { haptic.play(HapticEffect.Medium); onStartVoice() },
                            enabled = voiceState is VoiceRecordingState.Idle,
                            modifier = Modifier.size(40.dp).testTag("voice-composer"),
                        ) {
                            Icon(
                                Icons.Default.Mic,
                                contentDescription = stringResource(com.outcomego.android.R.string.chat_record_voice),
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }
            },
        ) { measurables, constraints ->
            val buttonConstraints = constraints.copy(minWidth = 0, minHeight = 0)
            val attachPlaceable = measurables.first { it.layoutId == "attach" }.measure(buttonConstraints)
            val actionPlaceable = measurables.first { it.layoutId == "action" }.measure(buttonConstraints)
            val maxWidth = constraints.maxWidth
            if (!isFocused) {
                val gapPx = 4.dp.roundToPx()
                val inputWidth = (maxWidth - attachPlaceable.width - actionPlaceable.width - gapPx * 2).coerceAtLeast(0)
                val inputPlaceable = measurables.first { it.layoutId == "input" }.measure(
                    constraints.copy(minWidth = inputWidth, maxWidth = inputWidth, minHeight = 0),
                )
                val rowHeight = maxOf(attachPlaceable.height, actionPlaceable.height, inputPlaceable.height)
                layout(maxWidth, rowHeight) {
                    attachPlaceable.placeRelative(
                        x = 0,
                        y = (rowHeight - attachPlaceable.height) / 2,
                    )
                    inputPlaceable.placeRelative(
                        x = attachPlaceable.width + gapPx,
                        y = (rowHeight - inputPlaceable.height) / 2,
                    )
                    actionPlaceable.placeRelative(
                        x = maxWidth - actionPlaceable.width,
                        y = (rowHeight - actionPlaceable.height) / 2,
                    )
                }
            } else {
                val rowGapPx = 4.dp.roundToPx()
                val inputPlaceable = measurables.first { it.layoutId == "input" }.measure(
                    constraints.copy(minWidth = maxWidth, maxWidth = maxWidth, minHeight = 0),
                )
                val toolRowHeight = maxOf(attachPlaceable.height, actionPlaceable.height)
                val totalHeight = inputPlaceable.height + rowGapPx + toolRowHeight
                layout(maxWidth, totalHeight) {
                    inputPlaceable.placeRelative(x = 0, y = 0)
                    attachPlaceable.placeRelative(
                        x = 0,
                        y = inputPlaceable.height + rowGapPx + (toolRowHeight - attachPlaceable.height) / 2,
                    )
                    actionPlaceable.placeRelative(
                        x = maxWidth - actionPlaceable.width,
                        y = inputPlaceable.height + rowGapPx + (toolRowHeight - actionPlaceable.height) / 2,
                    )
                }
            }
        }
    }
}

private fun LazyListState.isNearBottom(thresholdPx: Float): Boolean {
    val info = layoutInfo
    val last = info.visibleItemsInfo.lastOrNull() ?: return true
    return last.index >= info.totalItemsCount - 1 &&
        last.offset + last.size - info.viewportEndOffset <= thresholdPx
}

@Composable
private fun ChannelActivity.label(): String = when (this) {
    ChannelActivity.Thinking -> localizedText("Thinking", "思考中")
    ChannelActivity.Typing -> localizedText("Typing", "正在输入")
    ChannelActivity.ReadingFile -> localizedText("Reading file", "正在读取文件")
    ChannelActivity.SearchingWeb -> localizedText("Searching the web", "正在搜索网页")
    ChannelActivity.ReadingWeb -> localizedText("Reading the web", "正在阅读网页")
    ChannelActivity.DraftingFile -> localizedText("Drafting the file", "正在撰写文件")
    ChannelActivity.RunningCommands -> localizedText("Running commands", "正在运行命令")
    ChannelActivity.WaitingOnCommand -> localizedText("Waiting on a command", "正在等待命令")
    ChannelActivity.OnComputer -> localizedText("On its computer", "正在操作电脑")
    ChannelActivity.MessagingAssistant -> localizedText("Messaging another assistant", "正在联系其他助手")
    ChannelActivity.WaitingOnBot -> localizedText("Waiting on another Bot", "正在等待其他 Bot")
    ChannelActivity.GeneratingImage -> localizedText("Generating a photo", "正在生成图片")
    ChannelActivity.OrganizingContext -> localizedText("Organizing context", "正在整理上下文")
    ChannelActivity.Working -> localizedText("Working", "处理中")
    ChannelActivity.Background -> localizedText("Working in the background", "正在后台处理")
}

/**
 * The desktop's "bot is working" row: a small avatar with a soft pulse and a caption of what the
 * bot is doing. It scales and fades in and out (180ms / 140ms) so the transcript does not jump.
 */
/**
 * Choose who is on a group call and who answers when nobody is named. Everyone starts selected and the
 * first member is the default speaker, as on the desktop.
 */
@Composable
private fun VoiceMemberPickerDialog(
    members: List<Conversation>,
    onDismiss: () -> Unit,
    onStart: (participants: List<String>, speaker: String?) -> Unit,
) {
    val ids = members.mapNotNull { it.botId }
    var selected by remember(ids) { mutableStateOf(ids.toSet()) }
    var speaker by remember(ids) { mutableStateOf(ids.firstOrNull()) }
    val chosen = ids.filter { it in selected }
    val effectiveSpeaker = speaker?.takeIf { it in selected } ?: chosen.firstOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(localizedText("Voice participants", "语音参与者")) },
        text = {
            Column(Modifier.testTag("live-voice-member-picker"), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    localizedText("Choose who can answer, and who speaks by default.", "选择谁可以回答，以及默认由谁发言。"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                members.forEach { member ->
                    val botId = member.botId ?: return@forEach
                    Row(
                        Modifier.fillMaxWidth().testTag("live-voice-member-$botId"),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        androidx.compose.material3.Checkbox(
                            checked = botId in selected,
                            onCheckedChange = { checked -> selected = if (checked) selected + botId else selected - botId },
                        )
                        Text(member.title, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        androidx.compose.material3.RadioButton(
                            selected = botId == effectiveSpeaker,
                            enabled = botId in selected,
                            onClick = { speaker = botId },
                        )
                    }
                }
                Text(
                    localizedText("The round button marks the default speaker.", "圆形按钮表示默认发言人。"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onStart(chosen, effectiveSpeaker) },
                enabled = chosen.isNotEmpty(),
                modifier = Modifier.testTag("live-voice-start-group"),
            ) { Text(localizedText("Start group call", "开始群聊通话")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(localizedText("Cancel", "取消")) } },
    )
}

@Composable
private fun BotActivityRow(
    activity: ChannelActivity?,
    bot: Conversation?,
    accountName: String,
    members: List<Conversation>,
) {
    // Keep the last caption while the row animates out.
    var shown by remember { mutableStateOf(activity) }
    if (activity != null) shown = activity
    AnimatedVisibility(
        visible = activity != null,
        enter = fadeIn(tween(180, easing = FastOutSlowInEasing)) + scaleIn(tween(180, easing = FastOutSlowInEasing), initialScale = .92f, transformOrigin = TransformOrigin(0f, .5f)),
        exit = fadeOut(tween(140, easing = FastOutSlowInEasing)) + scaleOut(tween(140, easing = FastOutSlowInEasing), targetScale = .92f, transformOrigin = TransformOrigin(0f, .5f)),
    ) {
        val label = shown?.label().orEmpty()
        val pulse = rememberInfiniteTransition(label = "activity-pulse").animateFloat(
            initialValue = .45f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "activity-alpha",
        )
        Row(
            Modifier.fillMaxWidth().height(36.dp).testTag("bot-activity")
                .semantics { contentDescription = label },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (bot != null) {
                ConversationAvatar(
                    conversation = bot,
                    accountName = accountName,
                    members = members,
                    size = 22.dp,
                    ringColor = MaterialTheme.colorScheme.background,
                )
            }
            AnimatedContent(
                targetState = label,
                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                label = "activity-caption",
            ) { text ->
                Text(
                    text,
                    Modifier.alpha(pulse.value),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
fun ChatContextSummary(messages: List<ChatMessage>, truncated: Boolean) {
    Card(Modifier.fillMaxWidth().testTag("thread-context"), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(com.outcomego.android.R.string.chat_thread_context),
                    Modifier.weight(1f).testTag("thread-context-title"),
                    fontWeight = FontWeight.SemiBold,
                )
                if (truncated) {
                    Text(
                        stringResource(com.outcomego.android.R.string.chat_older_context),
                        modifier = Modifier.testTag("thread-context-truncated"),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            messages.forEach { message ->
                Column(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Text(message.author, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    Text(message.displayBody ?: "Rich content", maxLines = 3, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun voiceControls(label: String, onStop: (() -> Unit)?, onCancel: () -> Unit) {
    val haptic = LocalAppHaptics.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
        onStop?.let { IconButton(onClick = { haptic.play(HapticEffect.Light); it() }) { Icon(Icons.Default.Stop, contentDescription = localizedText("Stop voice note", "停止语音消息")) } }
        IconButton(onClick = { haptic.play(HapticEffect.Warning); onCancel() }) { Icon(Icons.Default.Close, contentDescription = localizedText("Cancel voice note", "取消语音消息")) }
    }
}

@Composable
private fun VoiceConversationCard(messages: List<ChatMessage>, botName: String) {
    var expanded by remember(messages.first().id) { mutableStateOf(false) }
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth().testTag("voice-conversation-card"),
    ) {
        Column(Modifier.clickable { expanded = !expanded }.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    localizedText("Voice conversation · ${messages.size} messages", "语音对话 · ${messages.size} 条消息"),
                    style = MaterialTheme.typography.bodyMedium,
                )
                val preview = messages.firstOrNull { it.isMine }?.body?.trim().orEmpty()
                Text(
                    if (expanded) "" else preview,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp).testTag("voice-conversation-preview"),
                    style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null)
            }
            if (expanded) {
                Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    messages.forEach { message ->
                        Text(
                            "${if (message.isMine) localizedText("You", "你") else botName}: ${message.body}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    onRetry: (String) -> Unit,
    isTarget: Boolean,
    operation: FeatureOperationState,
    onDownload: (String, Attachment) -> Unit,
    onSaveAttachment: (String, Attachment) -> Unit,
    onShareAttachment: (Attachment) -> Unit,
    onForwardAttachment: (Attachment) -> Unit,
    onAction: (String, String, Map<String, String>) -> Unit,
    onReaction: (String, String) -> Unit,
    onMessageAction: (String, MessageAction) -> Unit,
    onContext: (String) -> Unit,
    onReply: (String) -> Unit,
    onOpenRoutine: () -> Unit,
    replyOriginal: ChatMessage?,
    onOpenOriginal: (String) -> Unit,
) {
    message.routineEvent?.let { event ->
        RoutineEventRow(message.id, event, onOpenRoutine)
        return
    }
    var previewAttachment by remember(message.id) { mutableStateOf<Attachment?>(null) }
    val context = LocalContext.current
    previewAttachment?.let { attachment ->
        AttachmentGalleryDialog(
            attachments = message.attachments,
            initialId = attachment.id,
            onDismiss = { previewAttachment = null },
            onOpenDocument = { item ->
                NativeAttachmentPreview.open(context, item).onFailure {
                    previewAttachment = null
                }
            },
            onSave = { onSaveAttachment(message.id, it) },
            onShare = onShareAttachment,
            onForward = onForwardAttachment,
        )
    }
    val swipeScope = rememberCoroutineScope()
    val haptics = LocalAppHaptics.current
    val density = LocalDensity.current
    val replyThresholdPx = with(density) { 56.dp.toPx() }
    val maxSwipePx = with(density) { 84.dp.toPx() }
    val swipe = remember(message.id) { Animatable(0f) }
    // Only the other side's messages can be replied to by swiping. Unsent rows have no server id.
    val canSwipeReply = !message.isMine && message.delivery == com.outcomego.android.data.Delivery.SENT
    Box(Modifier.fillMaxWidth()) {
        Icon(
            Icons.AutoMirrored.Filled.Reply,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp)
                .size(22.dp)
                .alpha((-swipe.value / replyThresholdPx).coerceIn(0f, 1f)),
        )
    Row(
        Modifier
            .fillMaxWidth()
            .offset { IntOffset(swipe.value.roundToInt(), 0) }
            .then(
                if (!canSwipeReply) Modifier else Modifier.pointerInput(message.id) {
                    var crossed = false
                    detectHorizontalDragGestures(
                        onDragStart = { crossed = false },
                        onDragEnd = {
                            if (crossed) onReply(message.id)
                            swipeScope.launch { swipe.animateTo(0f) }
                        },
                        onDragCancel = { swipeScope.launch { swipe.animateTo(0f) } },
                    ) { change, delta ->
                        change.consume()
                        // Leftward only, with resistance past the trigger distance.
                        val next = (swipe.value + delta).coerceIn(-maxSwipePx, 0f)
                        swipeScope.launch { swipe.snapTo(next) }
                        val nowCrossed = -next >= replyThresholdPx
                        if (nowCrossed && !crossed) haptics.play(HapticEffect.Light)
                        crossed = nowCrossed
                    }
                },
            ),
        horizontalArrangement = if (message.isMine) Arrangement.End else Arrangement.Start,
    ) {
        // The column is 86% wide, so its children need their own alignment. Without it
        // the author and bubble sit at the column's left edge even for the user's own messages.
        Column(
            Modifier.fillMaxWidth(.86f),
            horizontalAlignment = if (message.isMine) Alignment.End else Alignment.Start,
        ) {
            Text(
                message.author,
                Modifier.padding(horizontal = 8.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Card(
                modifier = Modifier.testTag("message-row-${message.body.hashCode()}"),
                colors = CardDefaults.cardColors(containerColor = if (message.isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                border = if (isTarget) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.tertiary) else null,
            ) {
                Column(Modifier.padding(horizontal = 15.dp, vertical = 12.dp)) {
                    message.replyToMessageId?.let { reply ->
                        ReplyQuote(
                            original = replyOriginal,
                            ownMessage = message.isMine,
                            onOpen = { onOpenOriginal(reply) },
                        )
                    }
                    // A question card already shows its prompt, so the body text would repeat it.
                    // As on macOS, a message whose text is just its one file's name shows only the file card.
                    val repeatsFileName = message.attachments.singleOrNull()?.let { it.name == message.body } == true ||
                        (message.attachments.isNotEmpty() && (message.metadata["type"] as? kotlinx.serialization.json.JsonPrimitive)?.content == "attachment")
                    message.displayBody?.takeUnless { message.richMessage?.isQuestion == true || repeatsFileName }?.let { body ->
                        MarkdownMessage(
                            markdown = body,
                            contentColor = if (message.isMine) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            containerColor = if (message.isMine) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                        )
                    }
                    message.richMessage?.let { rich ->
                        RichMessageCardScreen(
                            message = rich,
                            operation = operation,
                            onAction = { action, values -> onAction(message.id, action.id, values) },
                        )
                    }
                    if (message.attachments.isNotEmpty()) {
                        ChatAttachmentStrip(
                            attachments = message.attachments,
                            onPreview = { attachment, _ ->
                                previewAttachment = attachment
                                if (!attachment.isImage && attachment.localUri == null) onDownload(message.id, attachment)
                            },
                            onDownload = { attachment, _ -> onDownload(message.id, attachment) },
                        )
                    }
                    if (message.reactions.isNotEmpty() || (message.richMessage != null && message.richMessage?.isQuestion != true)) {
                        ReactionsAndActionsRow(
                            state = ReactionState(message.reactions, operation),
                            onReaction = { emoji -> onReaction(message.id, emoji) },
                            onAction = { action -> onMessageAction(message.id, action) },
                        )
                    }
                    // A plain reply already has a tappable quote that jumps to the original.
                    if (message.replyToMessageId == null && (message.contextMessageId != null || message.threadId != null)) {
                        TextButton(onClick = { onContext(message.id) }) { Text(stringResource(com.outcomego.android.R.string.chat_show_context)) }
                    }
                    if (message.isMine) {
                        val status = when (message.delivery) {
                            com.outcomego.android.data.Delivery.SENDING -> "Sending…"
                    com.outcomego.android.data.Delivery.SENT -> localizedText("Sent", "已发送")
                    com.outcomego.android.data.Delivery.FAILED -> localizedText("Not sent", "未发送")
                        }
                        Text(
                            status,
                            Modifier.align(Alignment.End).padding(top = 4.dp)
                                .testTag("delivery-status-${message.body.hashCode()}"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        if (message.delivery == com.outcomego.android.data.Delivery.FAILED) {
                            Row(
                                modifier = Modifier.align(Alignment.End)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { haptics.play(HapticEffect.Light); onRetry(message.id) }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(localizedText("Retry", "重试"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun RoutineEventRow(messageId: String, event: RoutineEvent, onOpen: () -> Unit) {
    val label = when (event.action) {
        "created" -> localizedText("New routine “${event.name}”", "新建例程“${event.name}”")
        "updated" -> localizedText("Updated routine “${event.name}”", "已更新例程“${event.name}”")
        "enabled" -> localizedText("Enabled routine “${event.name}”", "已启用例程“${event.name}”")
        "disabled" -> localizedText("Disabled routine “${event.name}”", "已停用例程“${event.name}”")
        else -> localizedText("Deleted routine “${event.name}”", "已删除例程“${event.name}”")
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = event.canOpen, onClick = onOpen)
            .padding(vertical = 8.dp)
            .testTag("routine-event-$messageId"),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Default.AccessTime,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * One-line preview of the message being replied to, shown above a reply's text.
 * Tapping it scrolls to the original, loading surrounding history first when it is not on screen.
 */
@Composable
private fun ReplyQuote(original: ChatMessage?, ownMessage: Boolean, onOpen: () -> Unit) {
    val preview = original?.let { message ->
        val text = (message.displayBody ?: "").replace(Regex("\\s+"), " ").trim()
        text.ifEmpty {
            if (message.attachments.isNotEmpty()) localizedText("Attachment", "附件") else ""
        }
    }.orEmpty().ifEmpty { localizedText("View original message", "查看原消息") }
    val tint = if (ownMessage) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        Modifier
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(tint.copy(alpha = 0.14f))
            .clickable(onClick = onOpen)
            .padding(horizontal = 10.dp, vertical = 5.dp)
            .testTag("reply-quote"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.AutoMirrored.Filled.Reply,
            contentDescription = null,
            modifier = Modifier.size(12.dp),
            tint = tint,
        )
        Spacer(Modifier.width(5.dp))
        Text(
            preview,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelMedium,
            color = tint,
        )
    }
}

@Composable
private fun ReplyPreviewBar(target: ChatMessage, onCancel: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(start = 12.dp)
            .testTag("reply-preview"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.AutoMirrored.Filled.Reply,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            (target.displayBody ?: "").replace(Regex("\\s+"), " ").trim(),
            Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        IconButton(onClick = onCancel, modifier = Modifier.size(36.dp).testTag("cancel-reply")) {
            Icon(
                Icons.Default.Close,
                contentDescription = localizedText("Cancel reply", "取消回复"),
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun AttachmentGalleryDialog(
    attachments: List<Attachment>,
    initialId: String,
    onDismiss: () -> Unit,
    onOpenDocument: (Attachment) -> Unit,
    onSave: (Attachment) -> Unit,
    onShare: (Attachment) -> Unit,
    onForward: (Attachment) -> Unit,
) {
    var index by remember(initialId) {
        mutableStateOf(attachments.indexOfFirst { it.id == initialId }.coerceAtLeast(0))
    }
    val current = attachments.getOrNull(index) ?: return
    val bitmap = rememberDecodedBitmap(current.localUri, maxSizePx = 2048)
    val haptic = LocalAppHaptics.current
    val isDocument = !current.isImage
    val dark = isSystemInDarkTheme()
    val background = if (isDocument) MaterialTheme.colorScheme.background else Color.Black
    val foreground = if (isDocument && !dark) Color(0xFF1C1C1C) else Color(0xFFF5F5F5)
    val ready = current.localUri != null
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(background)) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                // Round glass controls with the title between them, as on iOS.
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ChromeButton(Icons.Default.Close, localizedText("Close", "关闭"), foreground, onClick = onDismiss)
                    Text(
                        current.name,
                        Modifier.weight(1f),
                        color = foreground,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                    Row(
                        Modifier.height(44.dp).clip(CircleShape).background(foreground.copy(alpha = .09f)).border(.5.dp, foreground.copy(alpha = .1f), CircleShape).padding(horizontal = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ChromeIcon(Icons.Outlined.FileDownload, localizedText("Download", "下载"), foreground, ready) { haptic.play(HapticEffect.Light); onSave(current) }
                        ChromeIcon(Icons.Outlined.IosShare, localizedText("Share", "分享"), foreground, ready) { haptic.play(HapticEffect.Light); onShare(current) }
                        ChromeIcon(Icons.AutoMirrored.Outlined.Reply, localizedText("Forward", "转发"), foreground, ready) { haptic.play(HapticEffect.Light); onForward(current) }
                    }
                }
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (bitmap != null) {
                        Image(bitmap, current.alt ?: current.name, Modifier.fillMaxWidth())
                    } else if (isDocument) {
                        DocumentPreview(current, onDownload = { onSave(current) }, onOpenExternally = { onOpenDocument(current) })
                    }
                }
                if (!isDocument && attachments.size > 1) {
                    Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { index = (index - 1).coerceAtLeast(0) }, enabled = index > 0) { Text(localizedText("Previous", "上一项"), color = Color.White) }
                        Text("${index + 1} / ${attachments.size}", color = Color.White)
                        TextButton(onClick = { index = (index + 1).coerceAtMost(attachments.lastIndex) }, enabled = index < attachments.lastIndex) { Text(localizedText("Next", "下一项"), color = Color.White) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChromeButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, tint: Color, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).clip(CircleShape).background(tint.copy(alpha = .09f)).border(.5.dp, tint.copy(alpha = .1f), CircleShape)
            .clickable(onClickLabel = label, role = androidx.compose.ui.semantics.Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, label, Modifier.size(20.dp), tint = tint) }
}

@Composable
private fun ChromeIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, tint: Color, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(CircleShape).clickable(enabled = enabled, onClickLabel = label, role = androidx.compose.ui.semantics.Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, label, Modifier.size(21.dp), tint = tint.copy(alpha = if (enabled) 1f else .35f)) }
}
