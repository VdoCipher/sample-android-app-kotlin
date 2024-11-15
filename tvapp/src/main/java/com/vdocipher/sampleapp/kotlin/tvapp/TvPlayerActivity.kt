package com.vdocipher.sampleapp.kotlin.tvapp

import com.vdocipher.aegis.player.PlayerHost.InitializationListener
import com.vdocipher.aegis.player.VdoPlayer.PlaybackEventListener
import com.vdocipher.aegis.player.VdoPlayer
import android.os.Bundle
import android.util.Log
import com.vdocipher.aegis.player.VdoPlayerSupportFragment
import com.vdocipher.aegis.player.PlayerHost
import com.vdocipher.aegis.media.ErrorDescription
import com.vdocipher.aegis.player.VdoInitParams
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.vdocipher.aegis.media.PlayerOption
import com.vdocipher.aegis.media.Track
import com.vdocipher.aegis.player.VdoTimeLine

/**
 * This class serves as a basic example of integrating playback in android TV.
 *
 *
 * It uses a class extending [androidx.leanback.app.PlaybackSupportFragment] to provide a
 * standard playback control ui for android TV. However, you can make your own or use a different
 * playback controller layout.
 *
 *
 * The [PlaybackOverlayFragment] handles user interaction and interacts with the [VdoPlayer]
 * held by this class.
 */
class TvPlayerActivity : FragmentActivity(), InitializationListener, PlaybackEventListener {
    private var overlayFragment: PlaybackOverlayFragment? = null
    private var mPlayer: VdoPlayer? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tv_player)
        overlayFragment =
            supportFragmentManager.findFragmentById(R.id.tv_player_overlay_fragment) as PlaybackOverlayFragment?
        val playerFragment =
            supportFragmentManager.findFragmentById(R.id.tv_player_fragment) as VdoPlayerSupportFragment?
        playerFragment?.initialize(this@TvPlayerActivity)
    }

    override fun onInitializationSuccess(
        playerHost: PlayerHost,
        vdoPlayer: VdoPlayer,
        restored: Boolean
    ) {
        Log.i(TAG, "init success")
        mPlayer = vdoPlayer
        vdoPlayer.addPlaybackEventListener(this@TvPlayerActivity)
        loadParams()
    }

    override fun onInitializationFailure(
        playerHost: PlayerHost,
        errorDescription: ErrorDescription
    ) {
        Log.e(TAG, "init failure")
        showToast("Initialization failure. Reason: " + errorDescription.errorCode + ", " + errorDescription.errorMsg)
    }

    override fun onDeInitializationSuccess() {
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, state: Int) {
        overlayFragment!!.playbackStateChanged(playWhenReady, state)
    }

    override fun onSeekTo(timeMs: Long) {}
    override fun onProgress(timeMs: Long) {
        overlayFragment!!.playbackPositionChanged(timeMs)
    }

    override fun onBufferUpdate(timeMs: Long) {}
    override fun onPlaybackSpeedChanged(speed: Float) {}
    override fun onLoading(vdoInitParams: VdoInitParams) {
        Log.i(TAG, "onLoading")
    }

    override fun onLoaded(vdoInitParams: VdoInitParams) {
        Log.i(TAG, "onLoaded")
        mPlayer!!.playWhenReady = true
        overlayFragment!!.playbackDurationChanged(mPlayer!!.duration)
    }

    override fun onLoadError(vdoInitParams: VdoInitParams, errorDescription: ErrorDescription) {
        showToast("onLoadError " + errorDescription.errorCode + ": " + errorDescription.errorMsg)
    }

    override fun onMediaEnded(vdoInitParams: VdoInitParams) {
        Log.i(TAG, "onMediaEnded")
    }

    override fun onError(vdoInitParams: VdoInitParams, errorDescription: ErrorDescription) {
        showToast("onError code " + errorDescription.errorCode + ": " + errorDescription.errorMsg)
    }

    override fun onTracksChanged(tracks: Array<Track>, tracks1: Array<Track>) {}
    override fun onMetaDataLoaded(playerOptions: PlayerOption?) {
    }

    override fun onTimelineChanged(vdoTimeLine: VdoTimeLine?, state: Int) {
    }

    private fun loadParams() {
        val video = intent.getParcelableExtra<Video>(TvPlayerUIActivity.VIDEO)
        val vdoParams = VdoInitParams.createParamsWithOtp(video!!.videoOtp, video.videoPlaybackInfo)
        mPlayer!!.load(vdoParams)
    }

    fun setPlayWhenReady(playWhenReady: Boolean) {
        if (mPlayer != null) {
            mPlayer!!.playWhenReady = playWhenReady
        }
    }

    fun fastForward() {
        if (mPlayer != null) {
            val target = Math.min(mPlayer!!.duration, mPlayer!!.currentTime + 5000)
            mPlayer!!.seekTo(target)
        }
    }

    fun rewind() {
        if (mPlayer != null) {
            val target = Math.max(0, mPlayer!!.currentTime - 5000)
            mPlayer!!.seekTo(target)
        }
    }

    private fun showToast(msg: String) {
        Log.i(TAG, msg)
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val TAG = "TvPlayerActivity"
    }
}