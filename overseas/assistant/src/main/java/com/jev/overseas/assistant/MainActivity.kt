package com.jev.overseas.assistant

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings as SystemSettings
import android.text.InputType
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.jev.overseas.assistant.service.AssistantService
import com.jev.overseas.assistant.ui.Ui
import com.jev.overseas.core.net.ModelException
import com.jev.overseas.core.net.OpenRouterGateway
import java.util.Locale
import java.util.concurrent.Executors

/**
 * Setup and settings on one screen: the key, the
 * accessibility service, the models, spelling, status and the privacy note.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var settings: Settings
    private lateinit var ui: Ui
    private val worker = Executors.newSingleThreadExecutor()

    private lateinit var keyStatus: TextView
    private lateinit var keyInput: EditText
    private lateinit var testResult: TextView
    private lateinit var serviceStatus: TextView
    private lateinit var keyDot: TextView
    private lateinit var serviceDot: TextView
    private lateinit var spellingRow: LinearLayout
    private lateinit var autoRow: LinearLayout
    private lateinit var diagRow: LinearLayout
    private lateinit var diagCount: TextView
    private lateinit var diagnostics: DiagnosticLog
    private lateinit var chatsCount: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = Settings(this)
        diagnostics = DiagnosticLog(this)
        ui = Ui(this)
        val page = ui.column(paddingDp = 16, gapDp = 10)
        page.addView(ui.label(getString(R.string.setup_title), 22f, ui.text, bold = true))

        // 1. Key
        keyDot = ui.label("●", 13f, ui.muted)
        page.addView(ui.row(6).apply { addView(keyDot); addView(ui.label(getString(R.string.step_key), 15f, ui.text, bold = true)) })
        keyStatus = ui.label("", 13f, ui.secondary)
        page.addView(keyStatus)
        keyInput = field(getString(R.string.key_hint)).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        page.addView(keyInput, full())
        page.addView(ui.row(8).apply {
            addView(ui.weighted(ui.primaryButton(getString(R.string.save)) { saveKey() }))
            addView(ui.ghostButton(getString(R.string.remove)) { settings.removeKey(); refresh() })
            addView(ui.ghostButton(getString(R.string.test_connection), ui.accent) { testConnection() })
        }, full())
        testResult = ui.label("", 13f, ui.secondary)
        page.addView(testResult)

        // 2. Service
        serviceDot = ui.label("●", 13f, ui.muted)
        page.addView(ui.row(6).apply { addView(serviceDot); addView(ui.label(getString(R.string.step_service), 15f, ui.text, bold = true)) })
        serviceStatus = ui.label("", 13f, ui.secondary)
        page.addView(serviceStatus)
        page.addView(ui.ghostButton(getString(R.string.open_accessibility), ui.accent) {
            startActivity(Intent(SystemSettings.ACTION_ACCESSIBILITY_SETTINGS))
        }, full())
        page.addView(ui.label(getString(R.string.restricted_note), 12f, ui.muted))

        // 3. Try it
        page.addView(ui.label(getString(R.string.step_try), 15f, ui.text, bold = true))
        page.addView(ui.label(getString(R.string.try_note), 13f, ui.secondary))

        // Models
        page.addView(section(getString(R.string.models_title)))
        val jev = field(getString(R.string.jev_model)).apply { setText(settings.jevModel) }
        val draft = field(getString(R.string.draft_model)).apply { setText(settings.draftModel) }
        page.addView(ui.label(getString(R.string.jev_model), 12f, ui.muted))
        page.addView(jev, full())
        page.addView(ui.label(getString(R.string.draft_model), 12f, ui.muted))
        page.addView(draft, full())
        page.addView(ui.row(8).apply {
            addView(ui.weighted(ui.primaryButton(getString(R.string.save)) {
                settings.jevModel = jev.text.toString(); settings.draftModel = draft.text.toString()
                jev.setText(settings.jevModel); draft.setText(settings.draftModel)
            }))
            addView(ui.ghostButton(getString(R.string.reset)) {
                settings.resetModels(); jev.setText(settings.jevModel); draft.setText(settings.draftModel)
            })
        }, full())

        // Spelling and behaviour
        page.addView(section(getString(R.string.spelling_title)))
        spellingRow = ui.row(8)
        page.addView(spellingRow)
        page.addView(section(getString(R.string.analyse_when_opened)))
        autoRow = ui.row(8)
        page.addView(autoRow)
        page.addView(ui.label(getString(R.string.analyse_when_opened_detail), 12f, ui.muted))
        chatsCount = ui.label("", 12f, ui.secondary)
        page.addView(chatsCount)
        page.addView(ui.ghostButton(getString(R.string.forget_chats)) { settings.forgetChats(); refresh() }, full())

        // Diagnostics: a local log the user can attach to a bug report
        page.addView(section(getString(R.string.diagnostics_title)))
        page.addView(ui.label(getString(R.string.diagnostics_detail), 12f, ui.muted))
        diagRow = ui.row(8)
        page.addView(diagRow)
        diagCount = ui.label("", 12f, ui.secondary)
        page.addView(diagCount)
        page.addView(ui.row(8).apply {
            addView(ui.weighted(ui.ghostButton(getString(R.string.share_report), ui.accent) {
                diagnostics.share(this@MainActivity, diagnostics.report(null, null))
            }))
            addView(ui.ghostButton(getString(R.string.clear_log)) { diagnostics.clear(); diagCount.postDelayed({ refresh() }, 300) })
        }, full())

        // Status and privacy
        page.addView(section(getString(R.string.status_title)))
        page.addView(ui.label(whatsappVersion(), 13f, ui.secondary))
        page.addView(ui.label("Jev ${BuildConfig.VERSION_NAME}", 13f, ui.secondary))
        page.addView(section(getString(R.string.privacy_title)))
        page.addView(ui.label(getString(R.string.privacy_text), 13f, ui.secondary))

        setContentView(ScrollView(this).apply {
            setBackgroundColor(ui.base)
            addView(page, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        })
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    override fun onDestroy() {
        worker.shutdownNow()
        super.onDestroy()
    }

    private fun refresh() {
        val masked = settings.maskedKey
        keyStatus.text = if (masked != null) getString(R.string.key_saved, masked) else getString(R.string.key_missing)
        keyDot.setTextColor(if (masked != null) ui.ok else ui.warn)
        val on = serviceEnabled()
        serviceStatus.text = getString(if (on) R.string.service_on else R.string.service_off)
        serviceDot.setTextColor(if (on) ui.ok else ui.warn)
        spellingRow.removeAllViews()
        for ((value, label) in listOf("AUTO" to R.string.spelling_auto, "US" to R.string.spelling_us, "UK" to R.string.spelling_uk)) {
            spellingRow.addView(ui.chip(getString(label), settings.spelling == value) { settings.spelling = value; refresh() })
        }
        diagRow.removeAllViews()
        diagRow.addView(ui.chip("Keep log", settings.keepDiagnostics) { settings.keepDiagnostics = true; refresh() })
        diagRow.addView(ui.chip("Off", !settings.keepDiagnostics) { settings.keepDiagnostics = false; refresh() })
        diagCount.text = getString(R.string.diagnostics_count, diagnostics.lineCount())
        chatsCount.text = getString(R.string.remembered_chats, settings.rememberedChats)
        autoRow.removeAllViews()
        autoRow.addView(ui.chip("On", settings.analyseWhenOpened) { settings.analyseWhenOpened = true; refresh() })
        autoRow.addView(ui.chip("Off", !settings.analyseWhenOpened) { settings.analyseWhenOpened = false; refresh() })
    }

    private fun saveKey() {
        val text = keyInput.text.toString().trim()
        if (text.isEmpty()) return
        settings.saveKey(text)
        keyInput.setText("")
        testResult.text = ""
        refresh()
    }

    /** Free: asks OpenRouter about the key itself, no model is called. */
    private fun testConnection() {
        val config = settings.modelConfig() ?: run { testResult.text = getString(R.string.key_missing); return }
        testResult.setTextColor(ui.secondary)
        testResult.text = getString(R.string.testing)
        worker.execute {
            val result = runCatching { OpenRouterGateway(config).keyInfo() }
            runOnUiThread {
                result.onSuccess { info ->
                    val remaining = info.limitRemaining?.let { getString(R.string.key_remaining, money(it)) } ?: ""
                    testResult.setTextColor(ui.ok)
                    testResult.text = getString(R.string.key_ok, money(info.usage), remaining)
                }.onFailure { e ->
                    testResult.setTextColor(ui.warn)
                    val reason = (e as? ModelException)?.message ?: e.javaClass.simpleName
                    testResult.text = getString(R.string.key_failed, reason)
                }
            }
        }
    }

    private fun money(v: Double) = String.format(Locale.US, "%.4f", v)

    private fun serviceEnabled(): Boolean {
        val enabled = SystemSettings.Secure.getString(contentResolver, SystemSettings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        val me = ComponentName(this, AssistantService::class.java)
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == me }
    }

    private fun whatsappVersion(): String = runCatching {
        val info = if (android.os.Build.VERSION.SDK_INT >= 33) {
            packageManager.getPackageInfo("com.whatsapp", PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION") packageManager.getPackageInfo("com.whatsapp", 0)
        }
        getString(R.string.whatsapp_version, info.versionName)
    }.getOrElse { getString(R.string.whatsapp_missing) }

    private fun section(title: String): TextView = ui.label(title, 15f, ui.text, bold = true).apply { setPadding(0, ui.dp(10), 0, 0) }

    private fun field(hint: String): EditText = EditText(this).apply {
        this.hint = hint
        setTextColor(ui.text)
        setHintTextColor(ui.muted)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        background = ui.rounded(ui.raised, 9, ui.line)
        setPadding(ui.dp(12), ui.dp(10), ui.dp(12), ui.dp(10))
        isSingleLine = true
    }

    private fun full() = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
}
