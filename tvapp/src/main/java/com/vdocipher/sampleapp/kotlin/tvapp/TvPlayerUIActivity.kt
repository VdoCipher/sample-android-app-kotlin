package com.vdocipher.sampleapp.kotlin.tvapp

import androidx.appcompat.app.AppCompatActivity
import com.vdocipher.aegis.player.PlayerHost.InitializationListener
import com.vdocipher.aegis.ui.view.VdoPlayerUIFragment
import com.vdocipher.aegis.player.VdoInitParams
import android.os.Bundle
import com.vdocipher.sampleapp.kotlin.tvapp.R
import android.view.WindowManager
import android.content.pm.ActivityInfo
import android.util.Log
import com.vdocipher.aegis.player.PlayerHost
import com.vdocipher.aegis.player.VdoPlayer
import com.vdocipher.aegis.media.ErrorDescription
import android.widget.Toast
import com.vdocipher.aegis.media.PlayerOption
import com.vdocipher.aegis.media.Track
import com.vdocipher.aegis.player.VdoPlayer.PlaybackEventListener
import com.vdocipher.aegis.player.VdoTimeLine

class TvPlayerUIActivity : AppCompatActivity(), InitializationListener {
    private var vdoPlayerUIFragment: VdoPlayerUIFragment? = null
    private var vdoParams: VdoInitParams? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tv_player_ui)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (supportActionBar != null) {
            supportActionBar!!.hide()
        }
        if (vdoParams == null) {
            val video = intent.getParcelableExtra<Video>(VIDEO)
            vdoParams = VdoInitParams.Builder()
                .setOtp(video!!.videoOtp)
                .setPlaybackInfo(video.videoPlaybackInfo)
                .setPreferredCaptionsLanguage("en")
                .setForceHighestSupportedBitrate(true)
                .build()
        }
        vdoPlayerUIFragment =
            supportFragmentManager.findFragmentById(R.id.vdo_player_fragment) as VdoPlayerUIFragment?
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        initializePlayer()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (vdoParams != null) {
            outState.putParcelable("initParams", vdoParams)
        }
    }

    private fun initializePlayer() {
        if (vdoParams != null) {
            // initialize the playerFragment; a VdoPlayer instance will be received
            // in onInitializationSuccess() callback
            vdoPlayerUIFragment!!.initialize(this@TvPlayerUIActivity)
        }
    }

    override fun onInitializationSuccess(
        playerHost: PlayerHost,
        player: VdoPlayer,
        wasRestored: Boolean
    ) {
        player.addPlaybackEventListener(playbackListener)
        // load a media to the player
        player.load(vdoParams)
    }

    override fun onInitializationFailure(
        playerHost: PlayerHost,
        errorDescription: ErrorDescription
    ) {
        val msg =
            "onInitializationFailure: errorCode = " + errorDescription.errorCode + ": " + errorDescription.errorMsg
        Log.e(TAG, msg)
        Toast.makeText(
            this@TvPlayerUIActivity,
            "initialization failure: " + errorDescription.errorMsg,
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onDeInitializationSuccess() {
        TODO("Not yet implemented")
    }

    private val playbackListener: PlaybackEventListener = object : PlaybackEventListener {
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {}
        override fun onTracksChanged(tracks: Array<Track>, tracks1: Array<Track>) {}
        override fun onMetaDataLoaded(playerOption: PlayerOption?) {
        }

        override fun onTimelineChanged(vdoTimeLine: VdoTimeLine?, state: Int) {
        }

        override fun onBufferUpdate(bufferTime: Long) {}
        override fun onSeekTo(millis: Long) {
            Log.i(TAG, "onSeekTo: $millis")
        }

        override fun onProgress(millis: Long) {}
        override fun onPlaybackSpeedChanged(speed: Float) {}
        override fun onLoading(vdoInitParams: VdoInitParams) {}
        override fun onLoadError(
            vdoInitParams: VdoInitParams,
            errorDescription: ErrorDescription
        ) {
        }

        override fun onLoaded(vdoInitParams: VdoInitParams) {}
        override fun onError(vdoParams: VdoInitParams, errorDescription: ErrorDescription) {
            Log.d("error", errorDescription.toString())
        }

        override fun onMediaEnded(vdoInitParams: VdoInitParams) {}
    }

    companion object {
        private const val TAG = "TvPlayerUIActivity"
        const val VIDEO = "video"
    }
}