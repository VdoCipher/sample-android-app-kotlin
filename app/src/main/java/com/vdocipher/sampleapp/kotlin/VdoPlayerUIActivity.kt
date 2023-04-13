package com.vdocipher.sampleapp.kotlin

import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AppCompatActivity
import com.vdocipher.aegis.BuildConfig
import com.vdocipher.aegis.media.ErrorDescription
import com.vdocipher.aegis.media.Track
import com.vdocipher.aegis.player.PlayerHost
import com.vdocipher.aegis.player.VdoInitParams
import com.vdocipher.aegis.player.VdoPlayer
import com.vdocipher.aegis.player.VdoPlayer.PlaybackEventListener
import com.vdocipher.aegis.ui.view.VdoPlayerUIFragment
import org.json.JSONException
import java.io.IOException


class VdoPlayerUIActivity : AppCompatActivity(), PlayerHost.InitializationListener {

    companion object {
        private const val TAG = "PlayerActivity"
        const val EXTRA_VDO_PARAMS = "vdo_params"
    }

    private lateinit var vdoPlayerUIFragment: VdoPlayerUIFragment
    private lateinit var eventLog: TextView

    private var eventLogString = ""
    private var currentOrientation: Int = 0
    private var vdoParams: VdoInitParams? = null

    private lateinit var player: VdoPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.v(TAG, "onCreate called")
        setContentView(R.layout.activity_vdo_player_ui)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        supportActionBar?.hide()
        if (savedInstanceState != null) {
            vdoParams = savedInstanceState.getParcelable("initParams")
        }
        if (vdoParams == null) {
            vdoParams = intent.getParcelableExtra(EXTRA_VDO_PARAMS)
        }
        vdoPlayerUIFragment =
            supportFragmentManager.findFragmentById(R.id.vdo_player_fragment) as VdoPlayerUIFragment
        (findViewById<View>(R.id.library_version) as TextView).text =
            String.format("sdk version: %s", BuildConfig.VDO_VERSION_NAME)
        eventLog = findViewById(R.id.event_log)
        eventLog.movementMethod = ScrollingMovementMethod.getInstance()
        currentOrientation = resources.configuration.orientation
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        initializePlayer()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        // when switched out of PiP mode, handle the new video, stopping any existing video playback if needed.
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        //switch to PiP mode if the user presses the home or recent button,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            enterPictureInPictureMode(PictureInPictureParams.Builder().build())
        }
    }

    override fun onStart() {
        Log.v(TAG, "onStart called")
        super.onStart()
    }

    override fun onStop() {
        Log.v(TAG, "onStop called")
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        Log.v(TAG, "onSaveInstanceState called")
        super.onSaveInstanceState(outState)
        if (vdoParams != null) {
            outState.putParcelable("initParams", vdoParams)
        }
    }

    private fun initializePlayer() {
        if (vdoParams != null) {
            // initialize the playerFragment; a VdoPlayer instance will be received
            // in onInitializationSuccess() callback
            vdoPlayerUIFragment.initialize(this@VdoPlayerUIActivity)
            log("initializing player fragment")
        } else {
            // lets get otp and playbackInfo before creating the player
            obtainOtpAndPlaybackInfo()
        }
    }

    /**
     * Fetch (otp + playbackInfo) and initialize VdoPlayer
     * here we're fetching a sample (otp + playbackInfo)
     */
    private fun obtainOtpAndPlaybackInfo() {
        log("fetching params...")
        Thread {
            try {
                vdoParams = obtainNewVdoParams()
                runOnUiThread { this.initializePlayer() }
            } catch (e: IOException) {
                e.printStackTrace()
                runOnUiThread {
                    showToast("Error fetching otp and playbackInfo: " + e.javaClass.simpleName)
                    log("error fetching otp and playbackInfo")
                }
            } catch (e: JSONException) {
                e.printStackTrace()
                runOnUiThread {
                    showToast("Error fetching otp and playbackInfo: " + e.javaClass.simpleName)
                    log("error fetching otp and playbackInfo")
                }
            }
        }.start()
    }

    @WorkerThread
    @Throws(IOException::class, JSONException::class)
    fun obtainNewVdoParams(): VdoInitParams? {
        val pair: Pair<String, String> = sampleOtpAndPlaybackInfo()
        val vdoParams = VdoInitParams.Builder()
            .setOtp(pair.first)
            .setPlaybackInfo(pair.second)
            .setPreferredCaptionsLanguage("en")
            .setForceHighestSupportedBitrate(true)
            .build()
        Log.i(TAG, "obtained new otp and playbackInfo")
        return vdoParams
    }

    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(
                this@VdoPlayerUIActivity,
                message,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun log(msg: String) {
        eventLogString += """
            $msg
            
            """.trimIndent()
        eventLog.text = eventLogString
    }

    override fun onInitializationSuccess(
        playerHost: PlayerHost?,
        player: VdoPlayer,
        wasRestored: Boolean
    ) {
        Log.i(TAG, "onInitializationSuccess")
        log("onInitializationSuccess")
        this.player = player
        player.addPlaybackEventListener(playbackListener)
        // load a media to the player
        player.load(vdoParams)
        log("loaded init params to player")
    }

    override fun onInitializationFailure(
        playerHost: PlayerHost?,
        errorDescription: ErrorDescription
    ) {
        val msg =
            "onInitializationFailure: errorCode = " + errorDescription.errorCode + ": " + errorDescription.errorMsg
        log(msg)
        Log.e(TAG, msg)
        Toast.makeText(
            this@VdoPlayerUIActivity,
            "initialization failure: " + errorDescription.errorMsg,
            Toast.LENGTH_LONG
        ).show()
    }

    private val playbackListener: PlaybackEventListener = object : PlaybackEventListener {
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
            val err =
                "onLoadError code: " + errorDescription.errorCode + ": " + errorDescription.errorMsg
            Log.e(TAG, err)
            log(err)
        }

        override fun onLoaded(vdoInitParams: VdoInitParams) {
            Log.i(TAG, "onLoaded")
            log("onLoaded")
        }

        override fun onError(vdoParams: VdoInitParams, errorDescription: ErrorDescription) {
            val err =
                "onError code " + errorDescription.errorCode + ": " + errorDescription.errorMsg
            Log.e(TAG, err)
            log(err)
        }

        override fun onMediaEnded(vdoInitParams: VdoInitParams) {
            Log.i(TAG, "onMediaEnded")
            log("onMediaEnded")
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        val newOrientation = newConfig.orientation
        val oldOrientation = currentOrientation
        currentOrientation = newOrientation
        Log.i(
            TAG, "new orientation " +
                    if (newOrientation == Configuration.ORIENTATION_PORTRAIT) "PORTRAIT" else if (newOrientation == Configuration.ORIENTATION_LANDSCAPE) "LANDSCAPE" else "UNKNOWN"
        )
        super.onConfigurationChanged(newConfig)
        when (newOrientation) {
            oldOrientation -> {
                Log.i(TAG, "orientation unchanged")
            }
            Configuration.ORIENTATION_LANDSCAPE -> {
                // hide other views
                findViewById<View>(R.id.title_text).visibility = View.GONE
                findViewById<View>(R.id.library_version).visibility = View.GONE
                findViewById<View>(R.id.log_container).visibility = View.GONE
                findViewById<View>(R.id.vdo_player_fragment).layoutParams =
                    RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.MATCH_PARENT,
                        RelativeLayout.LayoutParams.MATCH_PARENT
                    )
            }
            else -> {
                // show other views
                findViewById<View>(R.id.title_text).visibility = View.VISIBLE
                findViewById<View>(R.id.library_version).visibility = View.VISIBLE
                findViewById<View>(R.id.log_container).visibility = View.VISIBLE
                findViewById<View>(R.id.vdo_player_fragment).layoutParams =
                    RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.MATCH_PARENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT
                    )
                // show system windows
            }
        }
    }
}