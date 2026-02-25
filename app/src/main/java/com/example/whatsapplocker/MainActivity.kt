package com.example.whatsapplocker

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.whatsapplocker.security.SecurityUtils
import com.example.whatsapplocker.utils.PermissionUtils
import com.google.android.material.switchmaterial.SwitchMaterial

class MainActivity : AppCompatActivity() {

    private lateinit var switchLocker: SwitchMaterial
    private lateinit var spinnerMethod: Spinner
    private lateinit var etTimeoutSeconds: EditText

    private lateinit var cbWhatsappBusiness: CheckBox
    private lateinit var etCustomApp: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        SecurityUtils.init(this)

        switchLocker = findViewById(R.id.switchLocker)
        spinnerMethod = findViewById(R.id.spinnerMethod)
        etTimeoutSeconds = findViewById(R.id.etTimeoutSeconds)
        cbWhatsappBusiness = findViewById(R.id.cbWhatsappBusiness)
        etCustomApp = findViewById(R.id.etCustomApp)

        bindLockMethodSpinner()
        bindLockedApps()
        bindSwitch()

        etTimeoutSeconds.setText((SecurityUtils.getTimeoutMs() / 1000).toString())

        findViewById<Button>(R.id.btnPermissions).setOnClickListener { requestPermissionsIfNeeded() }
        findViewById<Button>(R.id.btnSetCredential).setOnClickListener { showSetCredentialDialog() }
        findViewById<Button>(R.id.btnSaveTimeout).setOnClickListener { saveTimeout() }
        findViewById<Button>(R.id.btnAddApp).setOnClickListener { addCustomApp() }
    }

    private fun bindLockMethodSpinner() {
        val methods = listOf(
            SecurityUtils.LockMethod.PIN to "PIN",
            SecurityUtils.LockMethod.PASSWORD to "Password",
            SecurityUtils.LockMethod.PATTERN to "Pattern (e.g. 1-2-3-6)"
        )
        spinnerMethod.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, methods.map { it.second })
        spinnerMethod.setSelection(methods.indexOfFirst { it.first == SecurityUtils.getLockMethod() }.coerceAtLeast(0))
    }

    private fun bindLockedApps() {
        val locked = SecurityUtils.getLockedApps()
        val requiredPackage = "com.whatsapp.w4b"
        if (!locked.contains(requiredPackage)) {
            SecurityUtils.addLockedApp(requiredPackage)
        }
        cbWhatsappBusiness.isChecked = true
    }

    private fun bindSwitch() {
        switchLocker.isChecked = SecurityUtils.isLockEnabled()
        switchLocker.setOnCheckedChangeListener { _, enabled ->
            if (enabled && !validateBeforeEnable()) {
                switchLocker.isChecked = false
                return@setOnCheckedChangeListener
            }
            SecurityUtils.setLockEnabled(enabled)
            val intent = Intent(this, LockService::class.java)
            if (enabled) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
            } else {
                stopService(intent)
            }
        }
    }

    private fun validateBeforeEnable(): Boolean {
        if (!SecurityUtils.hasCredential()) {
            Toast.makeText(this, getString(R.string.error_set_credential), Toast.LENGTH_SHORT).show()
            return false
        }
        if (!PermissionUtils.hasUsageStatsPermission(this) || !PermissionUtils.hasOverlayPermission(this)) {
            Toast.makeText(this, getString(R.string.error_grant_permissions), Toast.LENGTH_SHORT).show()
            requestPermissionsIfNeeded()
            return false
        }
        return true
    }

    private fun showSetCredentialDialog() {
        val input = EditText(this).apply {
            hint = when (selectedLockMethod()) {
                SecurityUtils.LockMethod.PATTERN -> "e.g. 1-2-3-6"
                SecurityUtils.LockMethod.PASSWORD -> "Min 6 characters"
                SecurityUtils.LockMethod.PIN -> "4-8 digit PIN"
            }
        }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.set_lock_credential))
            .setView(input)
            .setPositiveButton(R.string.save) { _, _ ->
                val value = input.text.toString().trim()
                val method = selectedLockMethod()
                if (isValidCredential(method, value)) {
                    SecurityUtils.setCredential(value, method)
                    Toast.makeText(this, R.string.credential_saved, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, R.string.invalid_credential, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun selectedLockMethod(): SecurityUtils.LockMethod {
        return when (spinnerMethod.selectedItemPosition) {
            1 -> SecurityUtils.LockMethod.PASSWORD
            2 -> SecurityUtils.LockMethod.PATTERN
            else -> SecurityUtils.LockMethod.PIN
        }
    }

    private fun isValidCredential(method: SecurityUtils.LockMethod, value: String): Boolean {
        return when (method) {
            SecurityUtils.LockMethod.PIN -> value.length in 4..8 && value.all { it.isDigit() }
            SecurityUtils.LockMethod.PASSWORD -> value.length >= 6
            SecurityUtils.LockMethod.PATTERN -> value.matches(Regex("[1-9](?:-[1-9]){3,}"))
        }
    }

    private fun saveTimeout() {
        val seconds = etTimeoutSeconds.text.toString().toLongOrNull()
        if (seconds == null || seconds < 5) {
            Toast.makeText(this, R.string.invalid_timeout, Toast.LENGTH_SHORT).show()
            return
        }
        SecurityUtils.setTimeoutMs(seconds * 1000)
        Toast.makeText(this, R.string.timeout_saved, Toast.LENGTH_SHORT).show()
    }

    private fun addCustomApp() {
        val pkg = etCustomApp.text.toString().trim()
        if (pkg.isBlank() || !pkg.contains('.')) {
            Toast.makeText(this, R.string.invalid_package, Toast.LENGTH_SHORT).show()
            return
        }
        SecurityUtils.addLockedApp(pkg)
        etCustomApp.text.clear()
        Toast.makeText(this, getString(R.string.custom_app_added, pkg), Toast.LENGTH_SHORT).show()
    }


    private fun requestPermissionsIfNeeded() {
        if (!PermissionUtils.hasUsageStatsPermission(this)) {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
        if (!PermissionUtils.hasOverlayPermission(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        }
    }
}
