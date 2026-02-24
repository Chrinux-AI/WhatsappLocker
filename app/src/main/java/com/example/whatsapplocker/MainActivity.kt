package com.example.whatsapplocker

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.switchmaterial.SwitchMaterial

class MainActivity : AppCompatActivity() {

    private lateinit var switchLocker: SwitchMaterial
    private lateinit var cbWhatsappBusiness: CheckBox
    private lateinit var cbWhatsapp: CheckBox
    private lateinit var cbInstagram: CheckBox
    private lateinit var etCustomApp: EditText
    private lateinit var btnAddApp: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SecurityUtil.init(this)
        setContentView(R.layout.activity_main)

        switchLocker = findViewById(R.id.switchLocker)
        cbWhatsappBusiness = findViewById(R.id.cbWhatsappBusiness)
        cbWhatsapp = findViewById(R.id.cbWhatsapp)
        cbInstagram = findViewById(R.id.cbInstagram)
        etCustomApp = findViewById(R.id.etCustomApp)
        btnAddApp = findViewById(R.id.btnAddApp)

        findViewById<Button>(R.id.btnPermissions).setOnClickListener {
            requestPermissionsIfNeeded()
        }

        findViewById<Button>(R.id.btnSetPin).setOnClickListener {
            showSetPinDialog()
        }

        switchLocker.isChecked = SecurityUtil.isLockEnabled()
        switchLocker.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !SecurityUtil.hasPin()) {
                switchLocker.isChecked = false
                Toast.makeText(this, "Set a PIN first!", Toast.LENGTH_SHORT).show()
                return@setOnCheckedChangeListener
            }
            if (isChecked && !hasUsageStatsPermission(this)) {
                switchLocker.isChecked = false
                Toast.makeText(this, "Grant Usage Access Permission!", Toast.LENGTH_SHORT).show()
                requestPermissionsIfNeeded()
                return@setOnCheckedChangeListener
            }
            if (isChecked && !Settings.canDrawOverlays(this)) {
                switchLocker.isChecked = false
                Toast.makeText(this, "Grant Overlay Permission!", Toast.LENGTH_SHORT).show()
                requestPermissionsIfNeeded()
                return@setOnCheckedChangeListener
            }

            SecurityUtil.setLockEnabled(isChecked)

            val intent = Intent(this, LockService::class.java)
            if (isChecked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
            } else {
                stopService(intent)
            }
        }

        setupCheckboxes()

        btnAddApp.setOnClickListener {
            val pkg = etCustomApp.text.toString().trim()
            if (pkg.isNotEmpty()) {
                SecurityUtil.addLockedApp(pkg)
                etCustomApp.text.clear()
                Toast.makeText(this, "Added $pkg", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupCheckboxes() {
        val lockedApps = SecurityUtil.getLockedApps()

        cbWhatsappBusiness.isChecked = lockedApps.contains("com.whatsapp.w4b")
        cbWhatsapp.isChecked = lockedApps.contains("com.whatsapp")
        cbInstagram.isChecked = lockedApps.contains("com.instagram.android")

        val listener = { view: android.view.View ->
            val pkg = when (view.id) {
                R.id.cbWhatsappBusiness -> "com.whatsapp.w4b"
                R.id.cbWhatsapp -> "com.whatsapp"
                R.id.cbInstagram -> "com.instagram.android"
                else -> ""
            }
            val cb = view as CheckBox
            if (cb.isChecked) {
                SecurityUtil.addLockedApp(pkg)
            } else {
                SecurityUtil.removeLockedApp(pkg)
            }
        }

        cbWhatsappBusiness.setOnClickListener(listener)
        cbWhatsapp.setOnClickListener(listener)
        cbInstagram.setOnClickListener(listener)
    }

    private fun showSetPinDialog() {
        val input = EditText(this)
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
        input.hint = "Enter 4-8 digit PIN"

        AlertDialog.Builder(this)
            .setTitle("Set Locker PIN")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val pin = input.text.toString()
                if (pin.length >= 4) {
                    SecurityUtil.savePin(pin)
                    Toast.makeText(this, "PIN Saved Successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "PIN must be at least 4 digits", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun requestPermissionsIfNeeded() {
        if (!hasUsageStatsPermission(this)) {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            Toast.makeText(this, "Please enable Usage Access for WhatsApp Locker", Toast.LENGTH_LONG).show()
        }

        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            Toast.makeText(this, "Please allow drawing over other apps", Toast.LENGTH_LONG).show()
        }
    }

    private fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
        } else {
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }
}
