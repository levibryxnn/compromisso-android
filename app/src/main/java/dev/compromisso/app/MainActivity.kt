package dev.compromisso.app

import android.app.Activity
import android.app.TimePickerDialog
import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.content.pm.PackageManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationManagerCompat
import dev.compromisso.app.admin.ReinforcedProtectionManager
import dev.compromisso.app.data.BlockedDomainDatabaseHelper
import dev.compromisso.app.data.SQLiteBlockedDomainRepository
import dev.compromisso.app.daily.DailyVerseCatalog
import dev.compromisso.app.daily.DailyVersePreferences
import dev.compromisso.app.daily.DailyVerseScheduler
import dev.compromisso.app.domain.BlockedDomain
import dev.compromisso.app.domain.ProtectionMode
import dev.compromisso.app.policy.ProtectionController
import dev.compromisso.app.policy.ProtectionRuntime
import dev.compromisso.app.policy.ProtectionState
import dev.compromisso.app.security.PinVerification
import dev.compromisso.app.security.SecurityConfigStore
import dev.compromisso.app.security.TrustedPinManager
import dev.compromisso.app.ui.AddDomainDialog
import dev.compromisso.app.ui.CompromissoTheme
import dev.compromisso.app.ui.HomeScreen
import dev.compromisso.app.ui.HomeUiState
import dev.compromisso.app.ui.OnboardingScreen
import dev.compromisso.app.ui.PinSetupDialog
import dev.compromisso.app.ui.PinVerifyDialog
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    private lateinit var config: SecurityConfigStore
    private lateinit var repository: SQLiteBlockedDomainRepository
    private lateinit var pinManager: TrustedPinManager
    private lateinit var protectionController: ProtectionController
    private lateinit var reinforcedProtectionManager: ReinforcedProtectionManager
    private lateinit var versePreferences: DailyVersePreferences
    private lateinit var verseScheduler: DailyVerseScheduler

    private val worker: ExecutorService = Executors.newSingleThreadExecutor()
    private val dataWorker: ExecutorService = Executors.newSingleThreadExecutor()
    private var domainLoadGeneration = 0L

    private var uiState by mutableStateOf(HomeUiState())
    private var showOnboarding by mutableStateOf(false)
    private var showAddDialog by mutableStateOf(false)
    private var showSetupPinDialog by mutableStateOf(false)
    private var showVerifyPinDialog by mutableStateOf(false)
    private var dialogError by mutableStateOf<String?>(null)
    private var pendingAction: PendingAction? = null
    private var authorizationGeneration = 0L

    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ProtectionController.ACTION_STATUS_CHANGED) refreshState()
        }
    }

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            startVpnSafely()
            refreshState()
        } else {
            setMessage("A permissão de VPN local é necessária para ativar o filtro.")
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) updateDailyVerseEnabled(true)
        else setMessage("Permita notificações nas configurações do Android para receber a Palavra do Dia.")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Prevent screenshots, recents previews and most screen-capture paths.
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        if (Build.VERSION.SDK_INT >= 31) window.setHideOverlayWindows(true)

        config = SecurityConfigStore(applicationContext)
        repository = SQLiteBlockedDomainRepository(BlockedDomainDatabaseHelper(applicationContext))
        pinManager = TrustedPinManager(config)
        protectionController = ProtectionController(applicationContext, config)
        reinforcedProtectionManager = ReinforcedProtectionManager(applicationContext)
        versePreferences = DailyVersePreferences(applicationContext)
        verseScheduler = DailyVerseScheduler(applicationContext)
        showOnboarding = !config.onboardingComplete

        ContextCompat.registerReceiver(
            this,
            statusReceiver,
            IntentFilter(ProtectionController.ACTION_STATUS_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        refreshState()
        setContent {
            CompromissoTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (showOnboarding) {
                        OnboardingScreen {
                            config.onboardingComplete = true
                            showOnboarding = false
                        }
                    } else {
                        HomeScreen(
                            state = uiState,
                            verse = DailyVerseCatalog.forDate(LocalDate.now()),
                            onProtectionToggle = ::onProtectionToggle,
                            onAddDomain = {
                                if (uiState.storageAvailable) {
                                    dialogError = null
                                    showAddDialog = true
                                } else setMessage("Lista protegida indisponível. Não é seguro alterar regras.")
                            },
                            onDomainEnabledChange = ::onDomainEnabledChange,
                            onDeleteDomain = ::onDeleteDomain,
                            onModeChange = ::onModeChange,
                            onDailyVerseEnabledChange = ::onDailyVerseEnabledChange,
                            onDailyVerseTimeChange = ::pickDailyVerseTime,
                        )
                    }

                    if (showAddDialog) {
                        AddDomainDialog(
                            error = dialogError,
                            onDismiss = {
                                dialogError = null
                                showAddDialog = false
                            },
                            onConfirm = ::addDomain,
                        )
                    }
                    if (showSetupPinDialog) {
                        PinSetupDialog(
                            error = dialogError,
                            onDismiss = {
                                dialogError = null
                                showSetupPinDialog = false
                                pendingAction = null
                            },
                            onConfirm = ::configurePin,
                        )
                    }
                    if (showVerifyPinDialog) {
                        PinVerifyDialog(
                            error = dialogError,
                            onDismiss = {
                                authorizationGeneration++
                                dialogError = null
                                showVerifyPinDialog = false
                                pendingAction = null
                            },
                            onConfirm = ::verifyPinAndContinue,
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::config.isInitialized) refreshState()
    }

    override fun onDestroy() {
        authorizationGeneration++
        runCatching { unregisterReceiver(statusReceiver) }
        worker.shutdownNow()
        dataWorker.shutdownNow()
        super.onDestroy()
    }

    private fun onProtectionToggle() {
        if (uiState.protectionActive || uiState.protectionRequested) {
            performOrAuthorize(PendingAction.DisableProtection)
            return
        }
        if (!uiState.storageAvailable) {
            setMessage("Não é possível iniciar: banco de regras protegido indisponível.")
            return
        }
        if (!config.pinConfigured) {
            pendingAction = PendingAction.EnableProtection
            dialogError = null
            showSetupPinDialog = true
            return
        }
        beginVpnPermissionFlow()
    }

    private fun beginVpnPermissionFlow() {
        val permissionIntent = protectionController.vpnPermissionIntent()
        if (permissionIntent == null) {
            startVpnSafely()
            refreshState()
        } else {
            vpnPermissionLauncher.launch(permissionIntent)
        }
    }

    private fun startVpnSafely() {
        if (runCatching { protectionController.startProtection() }.isFailure) {
            setMessage("Não foi possível iniciar a VPN. Confira as permissões e tente novamente.")
        }
    }

    private fun onDailyVerseEnabledChange(enabled: Boolean) {
        if (enabled && Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        if (enabled && !NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            setMessage("Ative as notificações do COMPROMISSO nas configurações do Android.")
            return
        }
        updateDailyVerseEnabled(enabled)
    }

    private fun updateDailyVerseEnabled(enabled: Boolean) {
        runCatching {
            versePreferences.enabled = enabled
            verseScheduler.update()
        }.onFailure {
            if (enabled) {
                // Never show this optional feature as enabled if scheduling failed.
                runCatching { versePreferences.enabled = false }
                runCatching { verseScheduler.cancel() }
            }
            setMessage("Não foi possível configurar a Palavra do Dia.")
        }
        refreshState()
    }

    private fun pickDailyVerseTime() {
        TimePickerDialog(this, { _, hour, minute ->
            runCatching {
                versePreferences.setTime(hour, minute)
                verseScheduler.update()
            }.onFailure { setMessage("Não foi possível salvar o horário.") }
            refreshState()
        }, versePreferences.hour, versePreferences.minute, true).show()
    }

    private fun addDomain(raw: String) {
        dataWorker.execute {
            val result = runCatching {
                repository.add(raw)
                config.bumpRulesRevision()
                protectionController.reloadRules()
            }
            runOnUiThread {
                if (isDestroyed) return@runOnUiThread
                if (result.isSuccess) {
                    showAddDialog = false
                    dialogError = null
                    refreshState()
                } else {
                    dialogError = if (result.exceptionOrNull() is IllegalArgumentException)
                        result.exceptionOrNull()?.message ?: "Domínio inválido"
                    else "Não foi possível salvar o domínio com segurança."
                    refreshState()
                }
            }
        }
    }

    private fun onDomainEnabledChange(domain: BlockedDomain, enabled: Boolean) {
        if (!uiState.storageAvailable) return
        if (!enabled) {
            performOrAuthorize(PendingAction.SetDomainEnabled(domain.id, false))
        } else {
            execute(PendingAction.SetDomainEnabled(domain.id, true))
        }
    }

    private fun onDeleteDomain(domain: BlockedDomain) {
        if (!uiState.storageAvailable) return
        performOrAuthorize(PendingAction.DeleteDomain(domain.id))
    }

    private fun onModeChange(mode: ProtectionMode) {
        if (mode == uiState.protectionMode) return
        if (mode == ProtectionMode.NORMAL && uiState.protectionMode == ProtectionMode.COMMITMENT) {
            performOrAuthorize(PendingAction.SetMode(ProtectionMode.NORMAL))
            return
        }
        if (!config.pinConfigured) {
            pendingAction = PendingAction.SetMode(ProtectionMode.COMMITMENT)
            dialogError = null
            showSetupPinDialog = true
            return
        }
        config.protectionMode = ProtectionMode.COMMITMENT
        refreshState()
    }

    private fun performOrAuthorize(action: PendingAction) {
        if (config.protectionMode == ProtectionMode.COMMITMENT) {
            authorizationGeneration++
            pendingAction = action
            dialogError = null
            showVerifyPinDialog = true
        } else {
            execute(action)
        }
    }

    private fun configurePin(pin: String) {
        if (!pinManager.isValidPinFormat(pin)) {
            dialogError = "O PIN deve conter de 6 a 12 números."
            return
        }
        worker.execute {
            val result = runCatching { pinManager.configure(pin) }
            runOnUiThread {
                if (result.isFailure) {
                    dialogError = "Não foi possível proteger o PIN neste aparelho."
                    return@runOnUiThread
                }
                config.protectionMode = ProtectionMode.COMMITMENT
                showSetupPinDialog = false
                dialogError = null
                val action = pendingAction
                pendingAction = null
                refreshState()
                if (action != null) execute(action)
            }
        }
    }

    private fun verifyPinAndContinue(pin: String) {
        val authorizationAction = pendingAction ?: return
        val generation = authorizationGeneration
        worker.execute {
            val verification = runCatching { pinManager.verify(pin) }
                .getOrDefault(PinVerification.CredentialUnavailable)
            runOnUiThread {
                // A PIN for a cancelled dialog must never authorize a *new*
                // action selected while PBKDF2 was running on the worker.
                if (!showVerifyPinDialog || authorizationGeneration != generation ||
                    pendingAction !== authorizationAction || isDestroyed
                ) return@runOnUiThread
                when (verification) {
                    PinVerification.Success -> {
                        authorizationGeneration++
                        showVerifyPinDialog = false
                        dialogError = null
                        val action = pendingAction
                        pendingAction = null
                        if (action != null) execute(action)
                    }
                    PinVerification.Invalid -> dialogError = "PIN incorreto."
                    PinVerification.NotConfigured -> dialogError = "Nenhum PIN foi configurado."
                    PinVerification.CredentialUnavailable -> dialogError =
                        "A credencial protegida do aparelho não está disponível."
                    is PinVerification.Locked -> {
                        val seconds = ((verification.remainingMs + 999) / 1000).coerceAtLeast(1)
                        dialogError = "Muitas tentativas. Aguarde ${seconds}s."
                    }
                }
            }
        }
    }

    private fun execute(action: PendingAction) {
        when (action) {
            is PendingAction.DeleteDomain, is PendingAction.SetDomainEnabled -> {
                // Decryption, SQLite and Keystore operations are never performed on UI thread.
                dataWorker.execute {
                    val result = runCatching {
                        when (action) {
                            is PendingAction.DeleteDomain -> repository.delete(action.id)
                            is PendingAction.SetDomainEnabled -> repository.setEnabled(action.id, action.enabled)
                            else -> Unit
                        }
                        config.bumpRulesRevision()
                        protectionController.reloadRules()
                    }
                    runOnUiThread {
                        if (isDestroyed) return@runOnUiThread
                        if (result.isFailure) setMessage("Falha ao aplicar a regra. Confira o estado da proteção.")
                        refreshState()
                    }
                }
            }
            else -> {
                val result = runCatching {
                    when (action) {
                        PendingAction.EnableProtection -> beginVpnPermissionFlow()
                        PendingAction.DisableProtection -> protectionController.stopProtection()
                        is PendingAction.SetMode -> config.protectionMode = action.mode
                        else -> Unit
                    }
                }
                if (result.isFailure) setMessage("Falha ao aplicar a alteração. Confira o estado da proteção.")
                refreshState()
            }
        }
    }

    private fun refreshState() {
        // Network status must update immediately, without decrypting the database on UI thread.
        uiState = uiState.copy(
            protectionActive = ProtectionRuntime.state == ProtectionState.OPERATIONAL &&
                ProtectionRuntime.appliedRulesRevision == config.rulesRevision,
            protectionState = ProtectionRuntime.state,
            rulesPending = ProtectionRuntime.state != ProtectionState.OFF &&
                ProtectionRuntime.state != ProtectionState.ERROR &&
                ProtectionRuntime.appliedRulesRevision != config.rulesRevision,
            protectionRequested = config.protectionRequested,
            pinConfigured = config.pinConfigured,
            protectionMode = config.protectionMode,
            reinforcedAvailable = reinforcedProtectionManager.isDeviceOwner(),
            reinforcedActive = reinforcedProtectionManager.isCorePoliciesEnabled(),
            dailyVerseEnabled = versePreferences.enabled,
            dailyVerseNotificationsAllowed = NotificationManagerCompat.from(this).areNotificationsEnabled() &&
                (Build.VERSION.SDK_INT < 33 ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED),
            dailyVerseHour = versePreferences.hour,
            dailyVerseMinute = versePreferences.minute,
        )
        val requestId = ++domainLoadGeneration
        dataWorker.execute {
            val result = runCatching { repository.listAll() }
            runOnUiThread {
                if (isDestroyed || requestId != domainLoadGeneration) return@runOnUiThread
                if (result.isSuccess) {
                    val oldError = "Banco de regras protegido indisponível. Nenhum dado foi apagado; revise Android Keystore e armazenamento."
                    uiState = uiState.copy(
                        domains = result.getOrThrow(), storageAvailable = true,
                        message = uiState.message.takeUnless { it == oldError },
                    )
                } else {
                    uiState = uiState.copy(
                        storageAvailable = false,
                        message = "Banco de regras protegido indisponível. Nenhum dado foi apagado; revise Android Keystore e armazenamento.",
                    )
                }
            }
        }
    }

    private fun setMessage(message: String?) {
        uiState = uiState.copy(message = message)
    }

    private sealed interface PendingAction {
        data object EnableProtection : PendingAction
        data object DisableProtection : PendingAction
        data class DeleteDomain(val id: Long) : PendingAction
        data class SetDomainEnabled(val id: Long, val enabled: Boolean) : PendingAction
        data class SetMode(val mode: ProtectionMode) : PendingAction
    }
}
