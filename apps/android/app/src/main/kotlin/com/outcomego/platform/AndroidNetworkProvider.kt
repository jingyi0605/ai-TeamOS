package com.outcomego.platform

import android.content.Context
import android.os.Build
import com.outcomego.android.MainActivity
import com.outcomego.android.data.AndroidProviderStore
import com.outcomego.android.data.AndroidSecureSessionStore
import com.outcomego.mobile.core.Session
import com.outcomego.mobile.network.MobileEnrollmentClient
import com.outcomego.mobile.network.ProviderConfiguration
import com.outcomego.mobile.network.ProviderSession
import com.outcomego.mobile.network.ProviderSnapshot
import com.outcomego.mobile.network.ProviderState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object AndroidNetworkProvider {
    const val ACTION_START = "com.outcomego.android.action.START_NETWORK_PROVIDER"
    const val ACTION_STOP = "com.outcomego.android.action.STOP_NETWORK_PROVIDER"
    const val EXTRA_ALLOW_SHARING = "allowSharing"

    private val mutableSnapshot = MutableStateFlow(ProviderSnapshot())
    val snapshot: StateFlow<ProviderSnapshot> = mutableSnapshot.asStateFlow()

    fun start(context: Context, allowSharing: Boolean = true) {
        val intent = android.content.Intent(context, AndroidNetworkProviderForegroundService::class.java)
            .setAction(ACTION_START)
            .putExtra(EXTRA_ALLOW_SHARING, allowSharing)
        androidx.core.content.ContextCompat.startForegroundService(context, intent)
    }

    fun stop(context: Context) {
        context.startService(
            android.content.Intent(context, AndroidNetworkProviderForegroundService::class.java)
                .setAction(ACTION_STOP),
        )
    }

    internal fun publish(snapshot: ProviderSnapshot) {
        mutableSnapshot.value = snapshot
    }
}

class AndroidNetworkProviderController(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onSnapshot: (ProviderSnapshot) -> Unit = AndroidNetworkProvider::publish,
) {
    private val providerStore = AndroidProviderStore(context)
    private val transition = Mutex()
    private var session: ProviderSession? = null
    private var savedSession: Session? = null
    private var savedLabel = "Android device"
    private var sharingEnabled = false

    suspend fun start(saved: Session, label: String, allowSharing: Boolean) {
        transition.withLock {
            stopLocked()
            savedSession = saved
            savedLabel = label
            sharingEnabled = allowSharing
            startLocked(saved)
        }
    }

    suspend fun networkUnavailable() {
        transition.withLock {
            if (session == null) return@withLock
            session?.stop()
            session = null
            val current = AndroidNetworkProvider.snapshot.value
            publish(
                current.copy(
                    state = ProviderState.UNAVAILABLE,
                    lastErrorCode = "network_unavailable",
                ),
            )
        }
    }

    suspend fun networkAvailable() {
        transition.withLock {
            if (session != null || !sharingEnabled) return@withLock
            savedSession?.let { startLocked(it) }
        }
    }

    suspend fun stop() {
        transition.withLock { stopLocked() }
    }

    private suspend fun startLocked(saved: Session) {
        val server = saved.server
        val machineId = providerStore.machineId
        publish(
            ProviderSnapshot(
                state = ProviderState.ENROLLING,
                machineId = machineId,
            ),
        )
        val enrollmentClient = MobileEnrollmentClient(AndroidProviderHttpClient())
        val storedCredential = providerStore.read(server, machineId)
        val enrollment = if (storedCredential.isNullOrBlank()) {
            enrollmentClient.enroll(server, saved.token, machineId, savedLabel)
        } else {
            runCatching { enrollmentClient.resume(server, machineId, storedCredential) }
                .getOrElse {
                    providerStore.clear(server, machineId)
                    enrollmentClient.enroll(server, saved.token, machineId, savedLabel)
                }
        }
        providerStore.write(server, machineId, enrollment.credential)
        if (sharingEnabled) {
            providerStore.setAllowed(server, enrollment.deploymentId, true)
        }
        if (!providerStore.isAllowed(server, enrollment.deploymentId)) {
            publish(
                ProviderSnapshot(
                    state = ProviderState.STOPPED,
                    machineId = machineId,
                    deploymentId = enrollment.deploymentId,
                    lastErrorCode = "network_consent_required",
                ),
            )
            return
        }
        val provider = ProviderSession(
            configuration = ProviderConfiguration(
                serverUrl = server,
                machineId = machineId,
                credential = enrollment.credential,
                connectionId = enrollment.connectionId,
                deploymentId = enrollment.deploymentId,
            ),
            webSocket = AndroidProviderWebSocketConnector(),
            tcpDialer = AndroidProviderTcpDialer(),
            clock = object : com.outcomego.mobile.network.ProviderClock {
                override fun nowMs(): Long = android.os.SystemClock.elapsedRealtime()
            },
            scope = scope,
            onChanged = onSnapshot,
        )
        session = provider
        provider.start()
    }

    private suspend fun stopLocked() {
        session?.stop()
        session = null
        savedSession = null
        sharingEnabled = false
        publish(ProviderSnapshot())
    }

    private fun publish(snapshot: ProviderSnapshot) {
        onSnapshot(snapshot)
    }
}

class AndroidNetworkProviderForegroundService : android.app.Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var controller: AndroidNetworkProviderController
    private lateinit var networkMonitor: AndroidNetworkMonitor

    override fun onCreate() {
        super.onCreate()
        controller = AndroidNetworkProviderController(
            applicationContext,
            scope,
            onSnapshot = { snapshot ->
                AndroidNetworkProvider.publish(snapshot)
                updateNotification(snapshot)
            },
        )
        networkMonitor = AndroidNetworkMonitor(
            applicationContext,
            onAvailable = { scope.launch { controller.networkAvailable() } },
            onUnavailable = { scope.launch { controller.networkUnavailable() } },
        )
        networkMonitor.start()
        NotificationChannels(this).create()
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(
                NOTIFICATION_ID,
                notification("Connecting"),
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification("Connecting"))
        }
    }

    override fun onStartCommand(intent: android.content.Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            AndroidNetworkProvider.ACTION_START -> {
                val allowSharing = intent.getBooleanExtra(AndroidNetworkProvider.EXTRA_ALLOW_SHARING, false)
                scope.launch {
                    val saved = AndroidSecureSessionStore(applicationContext).readActive()
                    if (saved == null) {
                        AndroidNetworkProvider.publish(ProviderSnapshot(state = ProviderState.FAILED, lastErrorCode = "unauthorized"))
                        stopSelf()
                    } else {
                        runCatching {
                            controller.start(saved, deviceLabel(), allowSharing)
                        }.onFailure {
                            AndroidNetworkProvider.publish(
                                ProviderSnapshot(
                                    state = ProviderState.FAILED,
                                    lastErrorCode = "enrollment_failed",
                                ),
                            )
                            stopSelf()
                        }
                    }
                }
            }
            AndroidNetworkProvider.ACTION_STOP -> {
                scope.launch {
                    controller.stop()
                    stopSelf()
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        runBlocking { controller.stop() }
        networkMonitor.stop()
        scope.cancel()
        AndroidNetworkProvider.publish(ProviderSnapshot())
        super.onDestroy()
    }

    override fun onBind(intent: android.content.Intent?): android.os.IBinder? = null

    private fun deviceLabel(): String =
        listOf(Build.MANUFACTURER, Build.MODEL)
            .joinToString(" ")
            .trim()
            .ifBlank { "Android device" }

    private fun notification(status: String): android.app.Notification {
        val openIntent = android.app.PendingIntent.getActivity(
            this,
            8602,
            android.content.Intent(this, MainActivity::class.java)
                .addFlags(android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP),
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = android.app.PendingIntent.getService(
            this,
            8601,
            android.content.Intent(this, AndroidNetworkProviderForegroundService::class.java)
                .setAction(AndroidNetworkProvider.ACTION_STOP),
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
        )
        return androidx.core.app.NotificationCompat.Builder(this, NotificationChannels.NETWORK)
            .setSmallIcon(com.outcomego.android.R.drawable.ic_notification)
            .setContentTitle("Network sharing")
            .setContentText(status)
            .setContentIntent(openIntent)
            .setCategory(androidx.core.app.NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(com.outcomego.android.R.drawable.ic_notification, "Stop sharing", stopIntent)
            .build()
    }

    private fun updateNotification(snapshot: ProviderSnapshot) {
        val status = when (snapshot.state) {
            ProviderState.READY -> "Sharing is active"
            ProviderState.ENROLLING, ProviderState.CONNECTING, ProviderState.RECONNECTING -> "Connecting…"
            ProviderState.UNAVAILABLE -> "Temporarily unavailable"
            ProviderState.REVOKED -> "Authorization revoked"
            ProviderState.FAILED -> "Needs attention"
            ProviderState.STOPPED -> "Stopped"
        }
        getSystemService(android.app.NotificationManager::class.java)
            ?.notify(NOTIFICATION_ID, notification(status))
    }

    private companion object {
        const val NOTIFICATION_ID = 8600
    }
}
