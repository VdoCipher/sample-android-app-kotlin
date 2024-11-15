package com.vdocipher.sampleapp.kotlin

import android.app.PictureInPictureParams
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.vdocipher.aegis.media.ErrorDescription
import com.vdocipher.aegis.media.PlayerOption
import com.vdocipher.aegis.media.Track
import com.vdocipher.aegis.player.PlayerHost
import com.vdocipher.aegis.player.VdoInitParams
import com.vdocipher.aegis.player.VdoPlayer
import com.vdocipher.aegis.player.VdoTimeLine
import com.vdocipher.aegis.ui.view.VdoPlayerUIFragment
import com.vdocipher.sampleapp.kotlin.databinding.ActivityPlayerBinding
import org.json.JSONException
import java.io.IOException

class PlayerActivity : AppCompatActivity(), PlayerHost.InitializationListener {

    companion object {
        private const val TAG = "PlayerActivity"
        const val EXTRA_VDO_PARAMS = "vdo_params"
        private const val MEDIA_ACTIONS_PLAY_PAUSE: Long = (PlaybackStateCompat.ACTION_PLAY
                or PlaybackStateCompat.ACTION_PAUSE
                or PlaybackStateCompat.ACTION_PLAY_PAUSE)
        const val MEDIA_ACTIONS_ALL = MEDIA_ACTIONS_PLAY_PAUSE
    }

    private lateinit var mSession: MediaSessionCompat

    private lateinit var player: VdoPlayer

    private lateinit var binding: ActivityPlayerBinding

    private lateinit var audioManager: AudioManager
    private lateinit var audioFocusRequest: AudioFocusRequest

    private lateinit var playerFragment: VdoPlayerUIFragment
    private lateinit var playerControlView: VdoPlayerControlView
    private lateinit var eventLog: TextView

    private var eventLogString = ""
    private var vdoParams: VdoInitParams? = null
    private var currentOrientation = Configuration.ORIENTATION_PORTRAIT
    private lateinit var onBackPressedCallback: OnBackPressedCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate called")
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        savedInstanceState?.let {
            vdoParams = it.getParcelable("initParams")
        }

        vdoParams.let {
            if (it == null) vdoParams =
                intent.getParcelableExtra(EXTRA_VDO_PARAMS)
        }

        playerFragment =
            supportFragmentManager.findFragmentById(R.id.vdo_player_fragment) as VdoPlayerUIFragment
        playerControlView = findViewById(R.id.player_control_view)

        findViewById<TextView>(R.id.library_version).text =
            "sdk version: " + com.vdocipher.aegis.BuildConfig.VDO_VERSION_NAME

        eventLog = findViewById(R.id.event_log)
        eventLog.movementMethod = ScrollingMovementMethod.getInstance()
        showControls(false)

        currentOrientation = resources.configuration.orientation
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        initializePlayer()

        onBackPressedCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                    showFullScreen(false)
                    playerControlView.setFullscreenState(false)
                }
            }
        }

        onBackPressedDispatcher.addCallback(onBackPressedCallback)
    }

    private fun initializeMediaSession() {
        mSession = MediaSessionCompat(this, TAG)
        mSession.isActive = true
        MediaControllerCompat.setMediaController(this, mSession.controller)
        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, "Sample playback")
            .build()
        mSession.setMetadata(metadata)
        val mMediaSessionCallback = MediaSessionCallback(player)
        mSession.setCallback(mMediaSessionCallback)
        val playing =
            player.playbackState != VdoPlayer.STATE_IDLE && player.playbackState != VdoPlayer.STATE_ENDED && player.playWhenReady
        val state =
            if (playing) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        updatePlaybackState(
            state,
            MEDIA_ACTIONS_ALL,
            player.currentTime,
            player.playbackSpeed
        )
    }

    private fun requestAudioFocus(audioManager: AudioManager): Boolean {
        val playbackAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
            .build()
        audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(playbackAttributes)
            .setOnAudioFocusChangeListener(audioFocusChangeListener)
            .build()
        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED == audioManager.requestAudioFocus(
            audioFocusRequest
        )
    }

    private class MediaSessionCallback(private val player: VdoPlayer) :
        MediaSessionCompat.Callback() {
        override fun onPlay() {
            super.onPlay()
            //resume playing
            player.playWhenReady = true
        }

        override fun onPause() {
            super.onPause()
            //pause playing
            player.playWhenReady = false
        }

    }

    /**
     * Overloaded method that persists previously set media actions.
     *
     * @param state    The state of the video, e.g. playing, paused, etc.
     * @param position The position of playback in the video.
     */
    private fun updatePlaybackState(
        @PlaybackStateCompat.State state: Int, position: Long, playBackSpeed: Float
    ) {
        val actions = mSession.controller.playbackState.actions
        updatePlaybackState(state, actions, position, playBackSpeed)
    }

    private fun updatePlaybackState(
        @PlaybackStateCompat.State state: Int,
        playbackActions: Long,
        position: Long,
        playBackSpeed: Float
    ) {
        val builder = PlaybackStateCompat.Builder()
            .setActions(playbackActions)
            .setState(state, position, playBackSpeed)
        mSession.setPlaybackState(builder.build())
    }

    override fun onStart() {
        Log.d(TAG, "onStart called")
        super.onStart()
    }

    override fun onStop() {
        Log.d(TAG, "onStop called")
        super.onStop()
        if (mSession.isActive)
            mSession.release()
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
        Log.i(
            TAG, "new orientation " +
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
                        RelativeLayout.LayoutParams.MATCH_PARENT,
                        RelativeLayout.LayoutParams.MATCH_PARENT
                    )
                playerControlView.fitsSystemWindows = true
                onBackPressedCallback.isEnabled = true
                // hide system windows
                hideSystemUI()
                showControls(false)
            }
            else -> {
                // show other views
                findViewById<TextView>(R.id.title_text).visibility = View.VISIBLE
                findViewById<TextView>(R.id.library_version).visibility = View.VISIBLE
                findViewById<View>(R.id.log_container).visibility = View.VISIBLE
                findViewById<View>(R.id.vdo_player_fragment).layoutParams =
                    RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.MATCH_PARENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT
                    )
                onBackPressedCallback.isEnabled = false
                playerControlView.fitsSystemWindows = false
                playerControlView.setPadding(0, 0, 0, 0)
                // show system windows
                showSystemUI()
            }
        }
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
        playerControlView.setPlayer(player)
        showControls(true)
        initializeMediaSession()
        playerControlView.setFullscreenActionListener(fullscreenToggleListener)
        playerControlView.setVdoParamsGenerator(vdoParamsGenerator)

        // load media to the player
        player.load(vdoParams)
        log("loaded init params to player")
    }

    override fun onInitializationFailure(
        playerHost: PlayerHost?,
        errorDescription: ErrorDescription?
    ) {
        val msg =
            "onInitializationFailure: errorCode = ${errorDescription!!.errorCode}: ${errorDescription.errorMsg}"
        Log.e(TAG, msg)
        log(msg)
        showToast("initialization failure: ${errorDescription.errorCode}")
    }

    override fun onDeInitializationSuccess() {
        TODO("Not yet implemented")
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        enterPictureInPictureMode(PictureInPictureParams.Builder().build())
    }

    private val playbackListener = object : VdoPlayer.PlaybackEventListener {
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            log(playbackStateString(playWhenReady, playbackState))
            val playing =
                playbackState != VdoPlayer.STATE_IDLE && playbackState != VdoPlayer.STATE_ENDED && playWhenReady

            // We are playing the video now. Update the media session state and the PiP
            // window will update the actions.


            // We are playing the video now. Update the media session state and the PiP
            // window will update the actions.
            updatePlaybackState(
                if (playing) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                player.currentTime,
                player.playbackSpeed
            )

            if (playing) {
                //Request audio focus and notify other players to stop playback.
                if (!requestAudioFocus(audioManager)) {
                    Log.i(TAG, "Audio focus not granted")
                }
            } else if (playbackState == VdoPlayer.STATE_ENDED) {
                // Abandon audio focus when playback complete
                audioManager.abandonAudioFocusRequest(audioFocusRequest)
            }
        }

        override fun onTracksChanged(tracks: Array<Track>, tracks1: Array<Track>) {
            Log.i(TAG, "onTracksChanged")
            log("onTracksChanged")
        }

        override fun onMetaDataLoaded(playerOption: PlayerOption?) {
        }

        override fun onTimelineChanged(vdoTimeLine: VdoTimeLine?, state: Int) {
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
            playerControlView.verifyAndUpdateCaptionsButton()
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
            if (mSession.isActive)
                mSession.release()
        }
    }

    private val fullscreenToggleListener = object : VdoPlayerControlView.FullscreenActionListener {
        override fun onFullscreenAction(enterFullscreen: Boolean): Boolean {
            showFullScreen(enterFullscreen)
            return true
        }
    }

    private val vdoParamsGenerator: VdoPlayerControlView.VdoParamsGenerator =
        object : VdoPlayerControlView.VdoParamsGenerator {
            override fun getNewVdoInitParams(): VdoInitParams? {
                return try {
                    obtainNewVdoParams()
                } catch (e: IOException) {
                    e.printStackTrace()
                    runOnUiThread {
                        showToast("Error generating new otp and playbackInfo: " + e.javaClass.simpleName)
                        log("Error generating new otp and playbackInfo")
                    }
                    null
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
     */
    private fun obtainOtpAndPlaybackInfo() {
        log("fetching params...")
        Thread {
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
        }.start()
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

    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun showSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowInsetsControllerCompat(
            window,
            window.decorView
        ).show(WindowInsetsCompat.Type.systemBars())
    }

    private val audioFocusChangeListener =
        AudioManager.OnAudioFocusChangeListener { focusChange ->
            when (focusChange) {
                AudioManager.AUDIOFOCUS_GAIN ->                     //resume playing
                    player.playWhenReady = true
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {}
                AudioManager.AUDIOFOCUS_LOSS ->                     //pause playing
                    player.playWhenReady = false
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {}
                else -> {}
            }
        }
}
