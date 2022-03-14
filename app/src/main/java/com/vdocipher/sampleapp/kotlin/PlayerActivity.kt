package com.vdocipher.sampleapp.kotlin

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import com.vdocipher.aegis.media.ErrorDescription
import com.vdocipher.aegis.media.Track
import com.vdocipher.aegis.player.VdoPlayer
import com.vdocipher.aegis.player.VdoInitParams
import com.vdocipher.aegis.player.VdoPlayerSupportFragment
import android.widget.RelativeLayout
import android.os.Build
import androidx.annotation.WorkerThread
import com.vdocipher.aegis.player.PlayerHost
import com.vdocipher.sampleapp.kotlin.databinding.ActivityPlayerBinding
import org.json.JSONException
import java.io.IOException

class PlayerActivity : AppCompatActivity(), PlayerHost.InitializationListener {

    companion object {
        private const val TAG = "PlayerActivity"
        const val EXTRA_VDO_PARAMS = "vdo_params"
    }

    private lateinit var binding: ActivityPlayerBinding

    private lateinit var playerFragment: VdoPlayerSupportFragment
    private lateinit var playerControlView: VdoPlayerControlView
    private lateinit var eventLog: TextView

    private var eventLogString = ""
    private var vdoParams: VdoInitParams? = null
    private var currentOrientation = Configuration.ORIENTATION_PORTRAIT

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate called")
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.setOnSystemUiVisibilityChangeListener(systemUiVisibilityListener)

        savedInstanceState?.let {
            vdoParams = it.getParcelable("initParams")
        }

        vdoParams.let {
            if (it == null) vdoParams = intent.getParcelableExtra(EXTRA_VDO_PARAMS)
        }

        playerFragment = supportFragmentManager.findFragmentById(R.id.vdo_player_fragment) as VdoPlayerSupportFragment
        playerControlView = findViewById(R.id.player_control_view)

        findViewById<TextView>(R.id.library_version).text = "sdk version: " + com.vdocipher.aegis.BuildConfig.VDO_VERSION_NAME

        eventLog = findViewById(R.id.event_log)
        eventLog.movementMethod = ScrollingMovementMethod.getInstance()
        showControls(false)

        currentOrientation = resources.configuration.orientation
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT

        initializePlayer()
    }

    override fun onStart() {
        Log.d(TAG, "onStart called")
        super.onStart()
    }

    override fun onStop() {
        Log.d(TAG, "onStop called")
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        Log.d(TAG, "onSaveInstanceState called")
        super.onSaveInstanceState(outState)
        vdoParams?.let {
            outState.putParcelable("initParams", it)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        val newOrientation = newConfig.orientation
        val oldOrientation = currentOrientation
        currentOrientation = newOrientation
        Log.i(TAG, "new orientation " +
                when (newOrientation) {
                    Configuration.ORIENTATION_PORTRAIT -> "PORTRAIT"
                    Configuration.ORIENTATION_LANDSCAPE -> "LANDSCAPE"
                    else -> "UNKNOWN"
                }
        )
        super.onConfigurationChanged(newConfig)

        when (newOrientation) {
            oldOrientation -> Log.i(TAG, "orientation unchanged")

            Configuration.ORIENTATION_LANDSCAPE -> {
                // hide other views
                findViewById<TextView>(R.id.title_text).visibility = View.GONE
                findViewById<TextView>(R.id.library_version).visibility = View.GONE
                findViewById<View>(R.id.log_container).visibility = View.GONE
                findViewById<View>(R.id.vdo_player_fragment).layoutParams =
                    RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT
                    )
                playerControlView.fitsSystemWindows = true
                // hide system windows
                showSystemUi(false)
                showControls(false)
            }

            else -> {
                // show other views
                findViewById<TextView>(R.id.title_text).visibility = View.VISIBLE
                findViewById<TextView>(R.id.library_version).visibility = View.VISIBLE
                findViewById<View>(R.id.log_container).visibility = View.VISIBLE
                findViewById<View>(R.id.vdo_player_fragment).layoutParams =
                    RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT
                    )
                playerControlView.fitsSystemWindows = false
                playerControlView.setPadding(0, 0, 0, 0)
                // show system windows
                showSystemUi(true)
            }
        }
    }

    override fun onBackPressed() {
        if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            showFullScreen(false)
            playerControlView.setFullscreenState(false)
        } else {
            super.onBackPressed()
        }
    }

    override fun onInitializationSuccess(playerHost: PlayerHost?, player: VdoPlayer?, wasRestored: Boolean) {
        Log.i(TAG, "onInitializationSuccess")
        log("onInitializationSuccess")
        player!!.addPlaybackEventListener(playbackListener)
        playerControlView.setPlayer(player)
        showControls(true)

        playerControlView.setFullscreenActionListener(fullscreenToggleListener)
        playerControlView.setControllerVisibilityListener(visibilityListener)
        playerControlView.setVdoParamsGenerator(vdoParamsGenerator)

        // load media to the player
        player.load(vdoParams)
        log("loaded init params to player")
    }

    override fun onInitializationFailure(playerHost: PlayerHost?, errorDescription: ErrorDescription?) {
        val msg = "onInitializationFailure: errorCode = ${errorDescription!!.errorCode}: ${errorDescription.errorMsg}"
        Log.e(TAG, msg)
        log(msg)
        showToast("initialization failure: ${errorDescription.errorCode}")
    }

    private val playbackListener = object : VdoPlayer.PlaybackEventListener {
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            log(playbackStateString(playWhenReady, playbackState))
        }

        override fun onTracksChanged(tracks: Array<Track>, tracks1: Array<Track>) {
            Log.i(TAG, "onTracksChanged")
            log("onTracksChanged")
        }

        override fun onBufferUpdate(bufferTime: Long) {}

        override fun onSeekTo(millis: Long) {
            Log.i(TAG, "onSeekTo: $millis")
        }

        override fun onProgress(millis: Long) {}

        override fun onPlaybackSpeedChanged(speed: Float) {
            Log.i(TAG, "onPlaybackSpeedChanged $speed")
            log("onPlaybackSpeedChanged $speed")
        }

        override fun onLoading(vdoInitParams: VdoInitParams) {
            Log.i(TAG, "onLoading")
            log("onLoading")
        }

        override fun onLoadError(vdoInitParams: VdoInitParams, errorDescription: ErrorDescription) {
            val err = "onLoadError code: " + errorDescription.errorCode + ": " + errorDescription.errorMsg
            Log.e(TAG, err)
            log(err)
        }

        override fun onLoaded(vdoInitParams: VdoInitParams) {
            Log.i(TAG, "onLoaded")
            log("onLoaded")
        }

        override fun onError(vdoParams: VdoInitParams, errorDescription: ErrorDescription) {
            val err = "onError code " + errorDescription.errorCode + ": " + errorDescription.errorMsg
            Log.e(TAG, err)
            log(err)
        }

        override fun onMediaEnded(vdoInitParams: VdoInitParams) {
            Log.i(TAG, "onMediaEnded")
            log("onMediaEnded")
        }
    }

    private val fullscreenToggleListener = object : VdoPlayerControlView.FullscreenActionListener {
        override fun onFullscreenAction(enterFullscreen: Boolean): Boolean {
            showFullScreen(enterFullscreen)
            return true
        }
    }

    private val visibilityListener = object : VdoPlayerControlView.ControllerVisibilityListener {
        override fun onControllerVisibilityChange(visibility: Int) {
            Log.i(TAG, "controller visibility $visibility")
            if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                if (visibility != View.VISIBLE) {
                    showSystemUi(false)
                }
            }
        }
    }

    private val vdoParamsGenerator: VdoPlayerControlView.VdoParamsGenerator =
        object : VdoPlayerControlView.VdoParamsGenerator {
            override fun getNewVdoInitParams(): VdoInitParams?
            {
                try {
                    return obtainNewVdoParams()
                } catch (e: IOException) {
                    e.printStackTrace()
                    runOnUiThread {
                        showToast("Error generating new otp and playbackInfo: " + e.javaClass.simpleName)
                        log("Error generating new otp and playbackInfo")
                    }
                    return null
                }
            }
        }

    private fun initializePlayer() {
        vdoParams.let {
            if (it != null) {
                // initialize the playerFragment; a VdoPlayer instance will be received
                // in onInitializationSuccess() callback
                playerFragment.initialize(this@PlayerActivity)
                log("initializing player fragment")
            } else {
                // lets get otp and playbackInfo before creating the player
                obtainOtpAndPlaybackInfo()
            }
        }
    }

    /**
     * Fetch (otp + playbackInfo) and initialize VdoPlayer
     * here we're fetching a sample (otp + playbackInfo)
     * TODO you need to generate/fetch (otp + playbackInfo) OR (signature + playbackInfo) for the
     * video you wish to play
     */
    private fun obtainOtpAndPlaybackInfo() {
        // todo use asynctask
        log("fetching params...")

        Thread(Runnable {
            try {
                vdoParams = obtainNewVdoParams()
                runOnUiThread { initializePlayer() }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    showToast("Error fetching otp and playbackInfo: " + e.javaClass.simpleName)
                    log("error fetching otp and playbackInfo")
                }
            }
        }).start()
    }

    @WorkerThread
    @Throws(IOException::class, JSONException::class)
    private fun obtainNewVdoParams(): VdoInitParams? {
        val pair = sampleOtpAndPlaybackInfo()
        val vdoParams = VdoInitParams.Builder()
            .setOtp(pair.first)
            .setPlaybackInfo(pair.second)
            .setPreferredCaptionsLanguage("en")
            .build()
        Log.i(TAG, "obtained new otp and playbackInfo")
        return vdoParams
    }

    private fun showToast(msg: String) {
        runOnUiThread { Toast.makeText(this@PlayerActivity, msg, Toast.LENGTH_SHORT).show() }
    }

    private fun log(msg: String) {
        eventLogString += msg + "\n"
        eventLog.text = eventLogString
    }

    private fun showControls(show: Boolean) {
        Log.d(TAG, "${if (show) "show" else "hide"} controls")
        playerControlView.apply { if (show) show() else hide() }
    }

    private fun showFullScreen(show: Boolean) {
        Log.v(TAG, (if (show) "enter" else "exit") + " fullscreen")
        requestedOrientation = if (show) {
            // go to landscape orientation for fullscreen mode
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            // go to portrait orientation
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        }
    }

    private fun showSystemUi(show: Boolean) {
        Log.v(TAG, (if (show) "show" else "hide") + " system ui")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            if (!show) {
                window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN)
            } else {
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            }
        }
    }

    private val systemUiVisibilityListener = View.OnSystemUiVisibilityChangeListener { visibility ->
        Log.v(TAG, "onSystemUiVisibilityChange")
        // show player controls when system ui is showing
        if ((visibility and View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
            Log.v(TAG, "system ui visible, making controls visible")
            showControls(true)
        }
    }
}
