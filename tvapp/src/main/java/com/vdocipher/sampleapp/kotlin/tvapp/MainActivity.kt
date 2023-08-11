package com.vdocipher.sampleapp.kotlin.tvapp

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.vdocipher.sampleapp.kotlin.tvapp.R

/*
 * Main Activity class that loads {@link MainFragment}.
 */
class MainActivity : FragmentActivity() {
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.main_browse_fragment, MainFragment())
                .commitNow()
        }
    }
}