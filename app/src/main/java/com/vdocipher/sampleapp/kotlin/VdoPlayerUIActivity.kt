package com.vdocipher.sampleapp.kotlin

import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener
import android.os.Build
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
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.util.Consumer
import com.vdocipher.aegis.BuildConfig
import com.vdocipher.aegis.media.ErrorDescription
import com.vdocipher.aegis.media.Track
import com.vdocipher.aegis.player.PlayerHost
import com.vdocipher.aegis.player.VdoInitParams
import com.vdocipher.aegis.player.VdoPlayer
import com.vdocipher.aegis.player.VdoPlayer.PlaybackEventListener
import com.vdocipher.aegis.ui.view.VdoPlayerControlView
import com.vdocipher.aegis.ui.view.VdoPlayerUIFragment
import org.json.JSONException
import java.io.IOException

class VdoPlayerUIActivity : AppCompatActivity(), PlayerHost.InitializationListener {

    companion object {
        private const val TAG = "PlayerActivity"
        const val EXTRA_VDO_PARAMS = "vdo_params"
        private const val MEDIA_ACTIONS_PLAY_PAUSE: Long = (PlaybackStateCompat.ACTION_PLAY
                or PlaybackStateCompat.ACTION_PAUSE
                or PlaybackStateCompat.ACTION_PLAY_PAUSE)

        const val MEDIA_ACTIONS_ALL = MEDIA_ACTIONS_PLAY_PAUSE
    }

    private lateinit var vdoPlayerUIFragment: VdoPlayerUIFragment
    private lateinit var playerControlView: VdoPlayerControlView
    private lateinit var eventLog: TextView

    private var eventLogString = ""
    var currentOrientation: Int = 0
    private var vdoParams: VdoInitParams? = null

    private lateinit var player: VdoPlayer

    private lateinit var audioManager: AudioManager

    private lateinit var mSession: MediaSessionCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.v(TAG, "onCreate called")
        setContentView(R.layout.activity_vdo_player_ui)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.setOnSystemUiVisibilityChangeListener(systemUiVisibilityListener)
        supportActionBar?.hide()
        if (savedInstanceState != null) {
            vdoParams = savedInstanceState.getParcelable("initParams")
        }
        if (vdoParams == null) {
            vdoParams = intent.getParcelableExtra(EXTRA_VDO_PARAMS)
        }
        vdoPlayerUIFragment =
            supportFragmentManager.findFragmentById(R.id.vdo_player_fragment) as VdoPlayerUIFragment
        playerControlView = vdoPlayerUIFragment.requireView()
            .findViewById(R.id.player_control_view)
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        (findViewById<View>(R.id.library_version) as TextView).text =
            String.format("sdk version: %s", BuildConfig.VDO_VERSION_NAME)
        eventLog = findViewById(R.id.event_log)
        eventLog.movementMethod = ScrollingMovementMethod.getInstance()
        showControls(false)
        currentOrientation = resources.configuration.orientation
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        addOnPictureInPictureModeChangedListener(pictureInPictureModeChangeListener)
        initializePlayer()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        // when switched out of PiP mode, handle the new video, stopping any existing video playback if needed.
    }

    private fun requestAudioFocus(audioManager: AudioManager): Boolean {
        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED == audioManager.requestAudioFocus(
            audioFocusChangeListener,
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN
        )
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
        disablePlayerUI()
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
     * TODO you need to generate/fetch (otp + playbackInfo) OR (signature + playbackInfo) for the
     * video you wish to play
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

    private fun showControls(show: Boolean) {
        Log.v(TAG, (if (show) "show" else "hide") + " controls")
        if (show) {
            playerControlView.show()
        } else {
            playerControlView.hide()
        }
    }

    private fun disablePlayerUI() {
//        showControls(false);
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
        showControls(true)
        initializeMediaSession()
        playerControlView.setFullscreenActionListener(fullscreenToggleListener)
        playerControlView.setControllerVisibilityListener(visibilityListener)
        playerControlView.setVdoParamsGenerator(vdoParamsGenerator)

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
                audioManager.abandonAudioFocus(audioFocusChangeListener)
            }
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

    private val fullscreenToggleListener = object : VdoPlayerControlView.FullscreenActionListener {
        override fun onFullscreenAction(enterFullscreen: Boolean): Boolean {
            showFullScreen(enterFullscreen)
            return true
        }
    }

    private val visibilityListener: VdoPlayerControlView.ControllerVisibilityListener =
        object : VdoPlayerControlView.ControllerVisibilityListener {
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
            override val newVdoInitParams: VdoInitParams?
                get() = kotlin.run {
                    try {
                        return obtainNewVdoParams()
                    } catch (e: IOException) {
                        e.printStackTrace()
                        runOnUiThread {
                            showToast("Error generating new otp and playbackInfo: " + e.javaClass.simpleName)
                            log("Error generating new otp and playbackInfo")
                        }
                        return null
                    } catch (e: JSONException) {
                        e.printStackTrace()
                        runOnUiThread {
                            showToast("Error generating new otp and playbackInfo: " + e.javaClass.simpleName)
                            log("Error generating new otp and playbackInfo")
                        }
                        return null
                    }
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
                playerControlView.fitsSystemWindows = true
                // hide system windows
                showSystemUi(false)
                showControls(false)
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

        override fun onSkipToNext() {
            super.onSkipToNext()
            //Implement this according to your app needs
        }

        override fun onSkipToPrevious() {
            super.onSkipToPrevious()
            //Implement this according to your app needs
        }
    }

    private val audioFocusChangeListener =
        OnAudioFocusChangeListener { focusChange ->
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

    private val pictureInPictureModeChangeListener = Consumer<PictureInPictureModeChangedInfo> {
        showControls(!it.isInPictureInPictureMode)
    }

}