package com.example.realtimeapplication

import android.content.Intent
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        try {
            val tvAppName = findViewById<TextView>(R.id.tv_app_name)
            if (tvAppName != null) {
                val paint = tvAppName.paint
                val width = paint.measureText(tvAppName.text.toString())
                if (width > 0) {
                    val textShader = LinearGradient(
                        0f, 0f, width, tvAppName.textSize,
                        intArrayOf(
                            ContextCompat.getColor(this, R.color.primary),
                            ContextCompat.getColor(this, R.color.accent)
                        ), null, Shader.TileMode.CLAMP
                    )
                    tvAppName.paint.shader = textShader
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        Handler(Looper.getMainLooper()).postDelayed({
            val user = FirebaseAuth.getInstance().currentUser
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("is_logged_in", user != null)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }, 1500)
    }
}
