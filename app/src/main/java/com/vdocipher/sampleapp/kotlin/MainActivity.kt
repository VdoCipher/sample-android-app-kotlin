package com.vdocipher.sampleapp.kotlin

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.TextView
import com.vdocipher.sampleapp.kotlin.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        findViewById<TextView>(R.id.library_version).text = "VdoCipher sdk version: " + com.vdocipher.aegis.BuildConfig.VDO_VERSION_NAME
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
