package com.vdocipher.sampleapp.kotlin

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun onlinePlayback(v: View) {
        val intent = Intent(this, PlayerActivity::class.java)
        startActivity(intent)
    }

    fun downloads(v: View) {
        Toast.makeText(this, "not implemented", Toast.LENGTH_SHORT).show()
    }
}
