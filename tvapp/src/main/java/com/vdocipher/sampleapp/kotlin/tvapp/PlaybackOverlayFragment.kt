package com.vdocipher.sampleapp.kotlin.tvapp

import androidx.leanback.app.PlaybackSupportFragment
import androidx.leanback.widget.PlaybackControlsRow.PlayPauseAction
import androidx.leanback.widget.PlaybackControlsRow.FastForwardAction
import androidx.leanback.widget.PlaybackControlsRow.RewindAction
import android.os.Bundle
import android.app.Activity
import android.util.Log
import androidx.leanback.widget.*

/**
 * Handles the UI for playback controls and state.
 *
 *
 * The UI is updated by calls from the host activity when it receives appropriate callbacks from
 * the VdoPlayer.
 */
class PlaybackOverlayFragment : PlaybackSupportFragment() {
    private var mPlaybackControlsRow: PlaybackControlsRow? = null
    private var mPlayPauseAction: PlayPauseAction? = null
    private var mFastForwardAction: FastForwardAction? = null
    private var mRewindAction: RewindAction? = null
    private var mRowsAdapter: ArrayObjectAdapter? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        backgroundType = BG_LIGHT
        isControlsOverlayAutoHideEnabled = true
        setUpRows()
    }

    /**
     * called when state change callback is received from VdoPlayer
     *
     * @param playWhenReady true if currently playing else false.
     * @param state         current state of playback. One of the [com.vdocipher.aegis.player.VdoPlayer.STATE_IDLE],
     * [com.vdocipher.aegis.player.VdoPlayer.STATE_BUFFERING],
     * [com.vdocipher.aegis.player.VdoPlayer.STATE_READY], [com.vdocipher.aegis.player.VdoPlayer.STATE_ENDED]
     */
    fun playbackStateChanged(playWhenReady: Boolean, state: Int) {
        if (playWhenReady) {
            isControlsOverlayAutoHideEnabled = true
            mPlayPauseAction!!.index = PlayPauseAction.INDEX_PAUSE
            mPlayPauseAction!!.icon =
                mPlayPauseAction!!.getDrawable(PlayPauseAction.INDEX_PAUSE)
        } else {
            mPlayPauseAction!!.index = PlayPauseAction.INDEX_PLAY
            mPlayPauseAction!!.icon =
                mPlayPauseAction!!.getDrawable(PlayPauseAction.INDEX_PLAY)
        }
        notifyPlaybackRowChanged()
    }

    fun playbackPositionChanged(positionMs: Long) {
        mPlaybackControlsRow!!.currentPosition = positionMs
    }

    fun playbackDurationChanged(durationMs: Long) {
        mPlaybackControlsRow!!.duration = durationMs
    }

    private fun setUpRows() {
        val ps = ClassPresenterSelector()
        val playbackControlsRowPresenter = PlaybackControlsRowPresenter()
        ps.addClassPresenter(PlaybackControlsRow::class.java, playbackControlsRowPresenter)
        ps.addClassPresenter(ListRow::class.java, ListRowPresenter())
        mRowsAdapter = ArrayObjectAdapter(ps)

        // add as first Row of mRowsAdapter
        addPlaybackControlsRow()

        // set action click listener
        playbackControlsRowPresenter.onActionClickedListener =
            OnActionClickedListener { action: Action ->
                if (action.id == mPlayPauseAction!!.id) {
                    togglePlayback(mPlayPauseAction!!.index == PlayPauseAction.INDEX_PLAY)
                } else if (action.id == mFastForwardAction!!.id) {
                    fastForward()
                } else if (action.id == mRewindAction!!.id) {
                    rewind()
                }
            }
        adapter = mRowsAdapter
    }

    private fun addPlaybackControlsRow() {
        mPlaybackControlsRow = PlaybackControlsRow()
        mRowsAdapter!!.add(mPlaybackControlsRow)
        val presenterSelector = ControlButtonPresenterSelector()
        val mPrimaryActionsAdapter = ArrayObjectAdapter(presenterSelector)
        mPlaybackControlsRow!!.primaryActionsAdapter = mPrimaryActionsAdapter
        val activity: Activity = requireActivity()
        mRewindAction = RewindAction(activity)
        mPlayPauseAction = PlayPauseAction(activity)
        mFastForwardAction = FastForwardAction(activity)

        // PrimaryAction setting
        mPrimaryActionsAdapter.add(mRewindAction)
        mPrimaryActionsAdapter.add(mPlayPauseAction)
        mPrimaryActionsAdapter.add(mFastForwardAction)
    }

    private fun togglePlayback(playWhenReady: Boolean) {
        (requireActivity() as TvPlayerActivity).setPlayWhenReady(playWhenReady)
    }

    // fast forward will simply seek 5 seconds forward
    private fun fastForward() {
        (requireActivity() as TvPlayerActivity).fastForward()
    }

    // rewind will simply seek 5 seconds backward
    private fun rewind() {
        (requireActivity() as TvPlayerActivity).rewind()
    }

    companion object {
        private val TAG = PlaybackOverlayFragment::class.java.simpleName
    }
}