package com.vdocipher.sampleapp.kotlin

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun onlinePlayback(v: View) {
        val intent = Intent(this, PlayerActivity::class.java)
        startActivity(intent)
    }

    fun downloads(v: View) {
        val intent = Intent(this, DownloadsActivity::class.java)
        startActivity(intent)
    }
}
