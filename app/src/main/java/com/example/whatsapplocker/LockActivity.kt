package com.example.whatsapplocker

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class LockActivity : AppCompatActivity() {

    private lateinit var etPin: EditText
    private lateinit var tvError: TextView
    private var lockedPackage: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lock)
        SecurityUtil.init(this)

        lockedPackage = intent.getStringExtra("locked_package")
        etPin = findViewById(R.id.etPin)
        tvError = findViewById(R.id.tvError)

        findViewById<Button>(R.id.btnUnlock).setOnClickListener {
            verifyPin()
        }
    }

    private fun verifyPin() {
        val pin = etPin.text.toString()
        if (SecurityUtil.checkPin(pin)) {
            lockedPackage?.let {
                SecurityUtil.setTemporarilyUnlocked(it)
            }
            finish()
        } else {
            tvError.visibility = android.view.View.VISIBLE
            etPin.text.clear()
        }
    }

    override fun onBackPressed() {
        // Intercept back button to return to home screen, rather than the locked app.
        goToHomeScreen()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            goToHomeScreen()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun goToHomeScreen() {
        val homeIntent = Intent(Intent.ACTION_MAIN)
        homeIntent.addCategory(Intent.CATEGORY_HOME)
        homeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(homeIntent)
        finish()
    }
}
