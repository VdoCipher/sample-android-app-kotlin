package com.vdocipher.sampleapp.kotlin

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import com.vdocipher.aegis.media.ErrorDescription
import com.vdocipher.aegis.media.Track
import com.vdocipher.aegis.player.VdoInitParams
import com.vdocipher.aegis.player.VdoPlayer
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min


class VdoPlayerControlView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : FrameLayout(context, attrs, defStyle) {

    companion object {
        private const val TAG = "VdoPlayerControlView"

        private const val DEFAULT_FAST_FORWARD_MS = 10000
        private const val DEFAULT_REWIND_MS = 10000
        private const val DEFAULT_SHOW_TIMEOUT_MS = 3000
        private val ERROR_CODES_FOR_NEW_PARAMS = listOf(2013, 2018)

        private val allowedSpeedList = floatArrayOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)
        private val allowedSpeedStrList =
            arrayOf<CharSequence>("0.5x", "0.75x", "1x", "1.25x", "1.5x", "1.75x", "2x")
    }

    interface ControllerVisibilityListener {
        /**
         * Called when the visibility of the controller ui changes.
         *
         * @param visibility new visibility of controller ui. Either [View.VISIBLE] or
         * [View.GONE].
         */
        fun onControllerVisibilityChange(visibility: Int)
    }

    interface FullscreenActionListener {
        /**
         * @return if enter or exit fullscreen action was handled
         */
        fun onFullscreenAction(enterFullscreen: Boolean): Boolean
    }

    interface VdoParamsGenerator {
        /**
         * @return new vdo params
         */
        fun getNewVdoInitParams(): VdoInitParams?
    }


    private val playButton: View
    private val pauseButton: View
    private val fastForwardButton: View
    private val rewindButton: View
    private val durationView: TextView
    private val positionView: TextView
    private val seekBar: SeekBar
    private val speedControlButton: Button
    private val captionsButton: ImageButton
    private val qualityButton: ImageButton
    private val enterFullscreenButton: ImageButton
    private val exitFullscreenButton: ImageButton
    private val loaderView: ProgressBar
    private val errorView: ImageButton
    private val errorTextView: TextView
    private val controlPanel: View
    private val controllerBackground: View

    private var helperThread: HandlerThread? = null
    private var helperHandler: Handler? = null

    private val ffwdMs: Int = DEFAULT_FAST_FORWARD_MS
    private val rewindMs: Int = DEFAULT_REWIND_MS
    private val showTimeoutMs: Int = DEFAULT_SHOW_TIMEOUT_MS


    private var scrubbing: Boolean = false
    private var attachedToWindow: Boolean = false
    private var fullscreen: Boolean = false
    private var chosenSpeedIndex = 2

    private var player: VdoPlayer? = null
    private val uiListener: UiListener
    private var lastErrorParams: VdoInitParams? =
        null // todo gather all relevant state and update UI using it
    private var needNewVdoParams: Boolean = false
    private var fullscreenActionListener: FullscreenActionListener? = null
    private var visibilityListener: ControllerVisibilityListener? = null
    private var vdoParamsGenerator: VdoParamsGenerator? = null

    private val hideAction = Runnable { hide() }

    init {

        uiListener = UiListener()
        LayoutInflater.from(context).inflate(R.layout.vdo_control_view, this)

        playButton = findViewById(R.id.vdo_play)
        playButton.setOnClickListener(uiListener)
        pauseButton = findViewById(R.id.vdo_pause)
        pauseButton.setOnClickListener(uiListener)
        pauseButton.visibility = GONE
        fastForwardButton = findViewById(R.id.vdo_ffwd)
        fastForwardButton.setOnClickListener(uiListener)
        rewindButton = findViewById(R.id.vdo_rewind)
        rewindButton.setOnClickListener(uiListener)
        durationView = findViewById(R.id.vdo_duration)
        positionView = findViewById(R.id.vdo_position)
        seekBar = findViewById(R.id.vdo_seekbar)
        seekBar.setOnSeekBarChangeListener(uiListener)
        speedControlButton = findViewById(R.id.vdo_speed)
        speedControlButton.setOnClickListener(uiListener)
        captionsButton = findViewById(R.id.vdo_captions)
        captionsButton.setOnClickListener(uiListener)
        captionsButton.visibility = GONE
        qualityButton = findViewById(R.id.vdo_quality)
        qualityButton.setOnClickListener(uiListener)
        enterFullscreenButton = findViewById(R.id.vdo_enter_fullscreen)
        enterFullscreenButton.setOnClickListener(uiListener)
        exitFullscreenButton = findViewById(R.id.vdo_exit_fullscreen)
        exitFullscreenButton.setOnClickListener(uiListener)
        exitFullscreenButton.visibility = GONE
        loaderView = findViewById(R.id.vdo_loader)
        loaderView.visibility = GONE
        errorView = findViewById(R.id.vdo_error)
        errorView.setOnClickListener(uiListener)
        errorView.visibility = GONE
        errorTextView = findViewById(R.id.vdo_error_text)
        errorTextView.setOnClickListener(uiListener)
        errorTextView.visibility = GONE
        controlPanel = findViewById(R.id.vdo_control_panel)
        controllerBackground = findViewById(R.id.vdo_controller_bg)
        setOnClickListener(uiListener)
    }

    fun setPlayer(newPlayer: VdoPlayer?) {
        if (newPlayer === player) return

        player?.removePlaybackEventListener(uiListener)
        player = newPlayer
        newPlayer?.addPlaybackEventListener(uiListener)
    }

    fun verifyAndUpdateCaptionsButton() {
        // get all available tracks of type trackType
        val availableTracks = player!!.availableTracks
        Log.i(TAG, availableTracks.size.toString() + " tracks available")
        val typeTrackList: ArrayList<Track> = ArrayList()
        for (availableTrack in availableTracks) {
            if (availableTrack.type == Track.TYPE_CAPTIONS) {
                typeTrackList.add(availableTrack)
            }
        }
        if (typeTrackList.size > 0) {
            captionsButton.visibility = VISIBLE
        }
    }

    fun setFullscreenActionListener(listener: FullscreenActionListener) {
        fullscreenActionListener = listener
    }

    fun setControllerVisibilityListener(listener: ControllerVisibilityListener) {
        visibilityListener = listener
    }

    fun setVdoParamsGenerator(vdoParamsGenerator: VdoParamsGenerator) {
        this.vdoParamsGenerator = vdoParamsGenerator
    }

    fun show() {
        if (!controllerVisible()) {
            controlPanel.visibility = VISIBLE
            updateAll()
            visibilityListener?.onControllerVisibilityChange(controlPanel.visibility)
        }
        hideAfterTimeout()
    }

    fun hide() {
        if (controllerVisible() && lastErrorParams == null) {
            controlPanel.visibility = GONE
            removeCallbacks(hideAction)
            visibilityListener?.onControllerVisibilityChange(controlPanel.visibility)
        }
    }

    fun setFullscreenState(fullscreen: Boolean) {
        this.fullscreen = fullscreen
        updateFullscreenButtons()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        attachedToWindow = true

        helperThread = HandlerThread(TAG)
        helperThread!!.start()
        helperHandler = Handler(helperThread!!.looper)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        attachedToWindow = false
        removeCallbacks(hideAction)

        helperThread?.quit()
        helperThread = null
        helperHandler = null
    }

    fun controllerVisible(): Boolean {
        return controlPanel.visibility == View.VISIBLE
    }

    private fun hideAfterTimeout() {
        removeCallbacks(hideAction)
        val playing = player?.playWhenReady ?: false
        if (showTimeoutMs > 0 && attachedToWindow && lastErrorParams != null && playing) {
            postDelayed(hideAction, showTimeoutMs.toLong())
        }
    }

    private fun updateAll() {
        updatePlayPauseButtons()
        updateSpeedControlButton()
    }

    private fun updatePlayPauseButtons() {
        if (!controllerVisible() || !attachedToWindow) {
            return
        }

        val playbackState = player?.playbackState ?: VdoPlayer.STATE_IDLE
        val playing = (player?.playWhenReady ?: false)
                && playbackState != VdoPlayer.STATE_IDLE
                && playbackState != VdoPlayer.STATE_ENDED
        playButton.visibility = if (playing) GONE else VISIBLE
        pauseButton.visibility = if (playing) VISIBLE else GONE
    }

    private fun rewind() {
        if (rewindMs > 0) {
            player?.seekTo(max(0, player!!.currentTime - rewindMs))
        }
    }

    private fun fastForward() {
        if (ffwdMs > 0) {
            player?.seekTo(min(player!!.duration, player!!.currentTime + ffwdMs))
        }
    }

    private fun updateLoader(loading: Boolean) {
        loaderView.visibility = if (loading) VISIBLE else GONE
    }

    private fun updateSpeedControlButton() {
        if (!controllerVisible() || !attachedToWindow) {
            return
        }

        player?.let {
            if (it.isSpeedControlSupported) {
                speedControlButton.visibility = VISIBLE
                val speed = it.playbackSpeed
                chosenSpeedIndex = getClosestFloatIndex(allowedSpeedList, speed)
                speedControlButton.text = allowedSpeedStrList[chosenSpeedIndex]
            } else {
                speedControlButton.visibility = GONE
            }
        }
    }

    private fun toggleFullscreen() {
        fullscreenActionListener?.let {
            val handled = it.onFullscreenAction(!fullscreen)
            if (handled) {
                fullscreen = !fullscreen
                updateFullscreenButtons()
            }
        }
    }

    private fun updateFullscreenButtons() {
        if (!controllerVisible() || !attachedToWindow) {
            return
        }

        enterFullscreenButton.visibility = if (fullscreen) GONE else VISIBLE
        exitFullscreenButton.visibility = if (fullscreen) VISIBLE else GONE
    }

    private fun showSpeedControlDialog() {
        AlertDialog.Builder(context)
            .setSingleChoiceItems(
                allowedSpeedStrList,
                chosenSpeedIndex
            ) { dialog, which ->
                player?.let { it.playbackSpeed = allowedSpeedList[which] }
                dialog.dismiss()
            }
            .setTitle("Choose playback speed")
            .show()
    }

    private fun showTrackSelectionDialog(trackType: Int) {
        val playerRef = player ?: return

        // get all available tracks of type trackType
        val availableTracks = playerRef.availableTracks.filter { it.type == trackType }

        // get the selected track of type trackType
        val selectedTrack = playerRef.selectedTracks.find { it.type == trackType }

        // get index of selected type track to indicate selection in dialog
        var selectedIndex = availableTracks.indexOf(selectedTrack)

        // We need audio track bitrate in order to show combined bandwidth
        val audioBitrate = getAudioBitrate(playerRef.selectedTracks)

        // first, let's convert tracks to array of TrackHolders for better display in dialog
        val trackHolders = availableTracks.map {
            if (trackType == Track.TYPE_VIDEO) TrackHolder(
                it, audioBitrate
            ) else TrackHolder(it)
        }.toMutableList()

        // if captions tracks are available, lets add a DISABLE_CAPTIONS track for turning off captions
        if (trackType == Track.TYPE_CAPTIONS && trackHolders.size > 0) {
            trackHolders.add(TrackHolder(Track.DISABLE_CAPTIONS))

            // if no captions are selected, indicate DISABLE_CAPTIONS as selected in dialog
            if (selectedIndex < 0) selectedIndex = trackHolders.size - 1
        } else if (trackType == Track.TYPE_VIDEO) {
            if (trackHolders.size <= 1) {
                // just show a default track option
                trackHolders.clear()
                trackHolders.add(TrackHolder.DEFAULT)
            } else {
                trackHolders.add(0, TrackHolder(Track.SET_ADAPTIVE))
                selectedIndex = if (player!!.isAdaptive) 0 else selectedIndex + 1
            }
        }

        val trackHolderArr: Array<TrackHolder> = trackHolders.toTypedArray()
        Log.i(TAG, "total ${trackHolders.size}, selected $selectedIndex")

        // show the type tracks in dialog for selection
        val title = if (trackType == Track.TYPE_CAPTIONS) "CAPTIONS" else "Quality"
        showSelectionDialog(title, trackHolderArr, selectedIndex)
    }

    private fun showSelectionDialog(
        title: CharSequence,
        trackHolders: Array<TrackHolder>,
        selectedTrackIndex: Int
    ) {
        val adapter: ListAdapter =
            ArrayAdapter(context, android.R.layout.simple_list_item_single_choice, trackHolders)
        AlertDialog.Builder(context)
            .setTitle(title)
            .setSingleChoiceItems(adapter, selectedTrackIndex) { dialog, which ->
                player?.let {
                    if (selectedTrackIndex != which) {
                        // set selection
                        val selectedTrack = trackHolders[which].track
                        Log.i(
                            TAG,
                            "selected track index: " + which + ", " + selectedTrack.toString()
                        )
                        it.setSelectedTracks(arrayOf(selectedTrack))
                    } else {
                        Log.i(TAG, "track selection unchanged")
                    }
                }
                dialog.dismiss()
            }
            .show()
    }

    private fun getAudioBitrate(selectedTracks: Array<Track>): Int {
        for (selectedTrack in selectedTracks) {
            if (selectedTrack.type == Track.TYPE_AUDIO) {
                return selectedTrack.bitrate
            }
        }
        return 0
    }

    private fun updateErrorView(errorDescription: ErrorDescription?) {
        if (errorDescription != null) {
            updateLoader(false)
            controlPanel.visibility = View.GONE
            errorView.visibility = View.VISIBLE
            errorTextView.visibility = View.VISIBLE
            val errMsg = ErrorMessages.getErrorMessage(errorDescription)
            errorTextView.text = errMsg
            show()
        } else {
            controlPanel.visibility = View.VISIBLE
            errorView.visibility = View.GONE
            errorTextView.visibility = View.GONE
            show()
        }
    }

    private fun retryAfterError() {
        player?.let { player ->
            lastErrorParams?.let {
                if (!needNewVdoParams) {
                    errorView.visibility = GONE
                    errorTextView.visibility = GONE
                    controlPanel.visibility = VISIBLE
                    player.load(it)
                    lastErrorParams = null
                } else if (vdoParamsGenerator != null && helperHandler != null) {
                    helperHandler!!.post {
                        val retryParams: VdoInitParams? = vdoParamsGenerator!!.getNewVdoInitParams()
                        retryParams?.let {
                            post {
                                errorView.visibility = GONE
                                errorTextView.visibility = GONE
                                controlPanel.visibility = VISIBLE
                                player.load(it)
                                lastErrorParams = null
                            }
                        }
                    }
                } else {
                    Log.e(TAG, "cannot retry loading params")
                    Toast.makeText(context, "cannot retry loading params", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    private inner class UiListener : VdoPlayer.PlaybackEventListener,
        SeekBar.OnSeekBarChangeListener,
        OnClickListener {

        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {}

        override fun onStartTrackingTouch(seekBar: SeekBar) {
            scrubbing = true
            removeCallbacks(hideAction)
        }

        override fun onStopTrackingTouch(seekBar: SeekBar) {
            scrubbing = false
            val seekTarget = seekBar.progress
            player?.seekTo(seekTarget.toLong())
            hideAfterTimeout()
        }

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            updatePlayPauseButtons()
            updateLoader(playbackState == VdoPlayer.STATE_BUFFERING)
        }

        override fun onClick(v: View) {
            var hideAfterTimeout = true

            if (player == null) return

            if (v === rewindButton) {
                rewind()
            } else if (v === playButton) {
                if (VdoPlayer.STATE_ENDED == player!!.playbackState) {
                    player!!.seekTo(0)
                }
                player!!.playWhenReady = true
            } else if (v === pauseButton) {
                hideAfterTimeout = false
                player!!.playWhenReady = false
            } else if (v === fastForwardButton) {
                fastForward()
            } else if (v === speedControlButton) {
                hideAfterTimeout = false
                showSpeedControlDialog()
            } else if (v === captionsButton) {
                hideAfterTimeout = false
                showTrackSelectionDialog(Track.TYPE_CAPTIONS)
            } else if (v === qualityButton) {
                hideAfterTimeout = false
                showTrackSelectionDialog(Track.TYPE_VIDEO)
            } else if (v === enterFullscreenButton || v === exitFullscreenButton) {
                toggleFullscreen()
            } else if (v === errorView || v === errorTextView) {
                retryAfterError()
            } else if (v === this@VdoPlayerControlView) {
                hideAfterTimeout = false
                if (controllerVisible()) {
                    hide()
                } else {
                    show()
                }
            }

            if (hideAfterTimeout) {
                hideAfterTimeout()
            }
        }

        override fun onSeekTo(millis: Long) {}

        override fun onProgress(millis: Long) {
            positionView.text = digitalClockTime(millis.toInt())
            seekBar.progress = millis.toInt()
        }

        override fun onBufferUpdate(bufferTime: Long) {
            seekBar.secondaryProgress = bufferTime.toInt()
        }

        override fun onPlaybackSpeedChanged(speed: Float) {
            updateSpeedControlButton()
        }

        override fun onLoading(vdoInitParams: VdoInitParams) {
            updateLoader(true)
            lastErrorParams = null
            updateErrorView(null)
        }

        override fun onLoaded(vdoInitParams: VdoInitParams) {
            player?.let {
                durationView.text = digitalClockTime(it.duration.toInt())
                seekBar.max = it.duration.toInt()
                updateSpeedControlButton()
            }
        }

        override fun onLoadError(vdoParams: VdoInitParams, errorDescription: ErrorDescription) {
            lastErrorParams = vdoParams
            needNewVdoParams = ERROR_CODES_FOR_NEW_PARAMS.contains(errorDescription.errorCode)
            updateErrorView(errorDescription)
        }

        override fun onMediaEnded(vdoInitParams: VdoInitParams) {
            // todo
        }

        override fun onError(vdoParams: VdoInitParams, errorDescription: ErrorDescription) {
            lastErrorParams = vdoParams
            needNewVdoParams = ERROR_CODES_FOR_NEW_PARAMS.contains(errorDescription.errorCode)
            updateErrorView(errorDescription)
        }

        override fun onTracksChanged(availableTracks: Array<Track>, selectedTracks: Array<Track>) {}
    }

    /**
     * A helper class that holds a Track instance and overrides [kotlin.toString] for
     * captions tracks for displaying to user.
     */
    private open class TrackHolder(
        val track: Track?,
        val audioBitrate: Int = 0
    ) {

        /**
         * Change this implementation to show track descriptions as per your app's UI requirements.
         */
        override fun toString(): String {
            return when {
                track == null ->
                    "Default"
                track === Track.DISABLE_CAPTIONS ->
                    "Turn off Captions"
                track === Track.SET_ADAPTIVE ->
                    "Auto"
                track.type == Track.TYPE_VIDEO ->
                    "${(track.bitrate + audioBitrate) / 1024}kbps (${dataExpenditurePerHour(track.bitrate + audioBitrate)})"
                track.type == Track.TYPE_CAPTIONS ->
                    track.language ?: "unknown"
                else ->
                    track.toString()
            }

        }

        private fun dataExpenditurePerHour(bitsPerSec: Int): String {
            val bytesPerHour = if (bitsPerSec <= 0) 0 else bitsPerSec * 3600L / 8
            if (bytesPerHour == 0L) {
                return "-"
            } else {
                val megabytesPerHour = bytesPerHour / (1024 * 1024).toFloat()

                return when {
                    megabytesPerHour < 1 ->
                        "1 MB per hour"
                    megabytesPerHour < 1000 ->
                        megabytesPerHour.toInt().toString() + " MB per hour"
                    else -> {
                        val df = DecimalFormat("#.#")
                        df.roundingMode = RoundingMode.CEILING
                        df.format(megabytesPerHour / 1024) + " GB per hour"
                    }
                }
            }
        }

        companion object {
            internal val DEFAULT: TrackHolder = TrackHolder(null)
        }
    }
}