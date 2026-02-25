package com.example.whatsapplocker

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.whatsapplocker.core.AppLocker
import com.example.whatsapplocker.security.SecurityUtils

class LockActivity : AppCompatActivity() {

    private lateinit var etCredential: EditText
    private lateinit var tvError: TextView
    private lateinit var tvMode: TextView

    private val appLocker = AppLocker()
    private var lockedPackage: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lock)
        SecurityUtils.init(this)

        lockedPackage = intent.getStringExtra("locked_package").orEmpty()

        etCredential = findViewById(R.id.etCredential)
        tvError = findViewById(R.id.tvError)
        tvMode = findViewById(R.id.tvMode)

        val mode = SecurityUtils.getLockMethod()
        tvMode.text = getString(R.string.unlock_mode_label, mode.name)
        etCredential.inputType = when (mode) {
            SecurityUtils.LockMethod.PIN -> android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
            SecurityUtils.LockMethod.PATTERN -> android.text.InputType.TYPE_CLASS_TEXT
            SecurityUtils.LockMethod.PASSWORD -> android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        findViewById<Button>(R.id.btnUnlock).setOnClickListener { verifyCredential() }
    }

    private fun verifyCredential() {
        val credential = etCredential.text.toString().trim()
        if (SecurityUtils.verifyCredential(credential)) {
            appLocker.onAppUnlocked(lockedPackage)
            finish()
        } else {
            tvError.visibility = View.VISIBLE
            etCredential.text.clear()
        }
    }

    override fun onBackPressed() {
        goHome()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            goHome()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun goHome() {
        startActivity(Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
        finish()
    }
}
