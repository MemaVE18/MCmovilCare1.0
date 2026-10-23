package com.example.mcmovilcare

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class DashboardAdminActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard_admin)

        findViewById<Button>(R.id.btnGestionarUsuarios).setOnClickListener {
        }
        findViewById<Button>(R.id.btnSupervisarOrdenes).setOnClickListener {
        }

        findViewById<TextView>(R.id.tvLogoutAdmin).setOnClickListener {
            cerrarSesion()
        }
    }

    private fun cerrarSesion() {
        val sharedPreferences = getSharedPreferences("MisPreferenciasApp", android.content.Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().apply()

        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}