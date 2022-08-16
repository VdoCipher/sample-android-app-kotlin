package com.vdocipher.sampleapp.kotlin

import android.content.Intent
import android.os.*
import android.util.Log
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.vdocipher.aegis.media.ErrorDescription
import com.vdocipher.aegis.media.Track
import com.vdocipher.aegis.offline.*
import com.vdocipher.aegis.player.VdoInitParams
import com.vdocipher.sampleapp.kotlin.databinding.ActivityDownloadsBinding
import java.io.File
import java.util.*

class DownloadsActivity : AppCompatActivity(), VdoDownloadManager.EventListener {
    companion object {
        private const val TAG = "DownloadsActivity"

        private fun getDownloadItemName(track: Track, durationMs: Long): String {
            val type = when (track.type) {
                Track.TYPE_VIDEO -> "V"
                Track.TYPE_AUDIO -> "A"
                else -> "?"
            }
            return "$type ${track.bitrate / 1024} kbps, ${getSizeString(track.bitrate, durationMs)}"
        }

        private fun statusString(status: DownloadStatus): String {
            return when (status.status) {
                VdoDownloadManager.STATUS_COMPLETED -> "Completed"
                VdoDownloadManager.STATUS_FAILED -> "Error " + status.reason + " " + status.reasonDescription
                VdoDownloadManager.STATUS_PENDING -> "Queued"
                VdoDownloadManager.STATUS_PAUSED -> "Paused " + status.downloadPercent + "%"
                VdoDownloadManager.STATUS_DOWNLOADING -> "Downloading " + status.downloadPercent + "%"
                else -> "Not found"
            }
        }
    }

    private lateinit var binding: ActivityDownloadsBinding

    private lateinit var download1: Button
    private lateinit var download2: Button
    private lateinit var download3: Button
    private lateinit var deleteAll: Button
    private lateinit var refreshList: Button
    private lateinit var downloadsListView: RecyclerView

    // dataset which backs the adapter for downloads recyclerview
    private lateinit var downloadStatusList: ArrayList<DownloadStatus>
    private lateinit var downloadAdapter: DownloadAdapter

    private var vdoDownloadManager: VdoDownloadManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDownloadsBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        download1 = findViewById(R.id.download_btn_1)
        download2 = findViewById(R.id.download_btn_2)
        download3 = findViewById(R.id.download_btn_3)
        download1.isEnabled = false
        download2.isEnabled = false
        download3.isEnabled = false
        downloadsListView = findViewById(R.id.downloads_list)
        val resumeAll = findViewById<AppCompatButton>(R.id.resume_all)
        val stopAll = findViewById<AppCompatButton>(R.id.stop_all)
        val downloadAll = findViewById<AppCompatButton>(R.id.download_all)
        deleteAll = findViewById(R.id.delete_all)
        deleteAll.isEnabled = false
        refreshList = findViewById(R.id.refresh_list)

        downloadStatusList = ArrayList()
        downloadAdapter = DownloadAdapter(downloadStatusList)
        downloadsListView.adapter = downloadAdapter
        downloadsListView.addItemDecoration(
            DividerItemDecoration(
                this,
                DividerItemDecoration.VERTICAL
            )
        )

        stopAll.setOnClickListener { stopAll() }

        resumeAll.setOnClickListener { resumeAll() }

        downloadAll.setOnClickListener { v: View? -> downloadAllMediaItems() }

        refreshList.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                refreshDownloadsList()
            } else {
                showToastAndLog("Minimum api level required is 21", Toast.LENGTH_LONG)
            }
        }

        deleteAll.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                deleteAllDownloads()
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            refreshDownloadsList()
        } else {
            showToastAndLog("Minimum api level required is 21", Toast.LENGTH_LONG)
        }
    }

    override fun onStart() {
        Log.d(TAG, "onStart")
        super.onStart()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            maybeCreateManager()
            vdoDownloadManager!!.addEventListener(this)
        }
    }

    override fun onStop() {
        vdoDownloadManager?.removeEventListener(this)
        super.onStop()
    }

    // VdoDownloadManager.EventListener implementation

    override fun onQueued(mediaId: String, downloadStatus: DownloadStatus) {
        showToastAndLog("Download queued : $mediaId", Toast.LENGTH_SHORT)
        addListItem(downloadStatus)
    }

    override fun onChanged(mediaId: String, downloadStatus: DownloadStatus) {
        Log.d(
            TAG,
            "Download status changed: " + mediaId + " " + downloadStatus.downloadPercent + "%"
        )
        updateListItem(downloadStatus)
    }

    override fun onCompleted(mediaId: String, downloadStatus: DownloadStatus) {
        showToastAndLog("Download complete: $mediaId", Toast.LENGTH_SHORT)
        updateListItem(downloadStatus)
    }

    override fun onFailed(mediaId: String, downloadStatus: DownloadStatus) {
        Log.e(
            TAG,
            mediaId + " download error: " + downloadStatus.reason + " " + downloadStatus.reasonDescription
        )
        Toast.makeText(
            this,
            " download error: " + downloadStatus.reason + " " + downloadStatus.reasonDescription,
            Toast.LENGTH_LONG
        ).show()
        updateListItem(downloadStatus)
    }

    override fun onDeleted(mediaId: String) {
        showToastAndLog("Deleted $mediaId", Toast.LENGTH_SHORT)
        removeListItem(mediaId)
    }

    private fun getAllMediaIds(): Array<String> {
        val mediaIdList = ArrayList<String>()
        for (status in downloadStatusList) {
            mediaIdList.add(status.mediaInfo.mediaId)
        }
        return mediaIdList.toTypedArray()
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun maybeCreateManager() {
        if (vdoDownloadManager == null) {
            vdoDownloadManager = VdoDownloadManager.getInstance(this)
            //Provide custom implementation if you want to customize notifications look and feel
            vdoDownloadManager?.setDownloadNotificationHelper(CustomDownloadNotificationHelper::class.java)
        }
    }

    private fun stopAll() {
        if (downloadStatusList.isNotEmpty()) {
            val mediaIds: Array<String> = getAllMediaIds()
            vdoDownloadManager!!.stopDownloads(*mediaIds)
        }
    }


    private fun resumeAll() {
        if (downloadStatusList.isNotEmpty()) {
            val mediaIds: Array<String> = getAllMediaIds()
            vdoDownloadManager!!.resumeDownloads(*mediaIds)
        }
    }

    private fun downloadAllMediaItems() {
        //list of OTP and Playback Info
        val otpPlaybackInfoList: MutableList<Pair<String, String>> = ArrayList()
        otpPlaybackInfoList.add(Pair(OTP_1, PLAYBACK_INFO_1))
        otpPlaybackInfoList.add(Pair(OTP_2, PLAYBACK_INFO_2))
        otpPlaybackInfoList.add(Pair(OTP_3, PLAYBACK_INFO_3))
        downloadMediaItems(otpPlaybackInfoList)
    }

    private fun downloadMediaItems(otpAndPlaybackInfoList: List<Pair<String, String>>) {
        val handlerThread = HandlerThread(TAG)
        handlerThread.start()
        val handler = Handler(handlerThread.looper)
        for (otpAndPlaybackInfo in otpAndPlaybackInfoList) {

            //retrieve the download option iteratively for each video
            handler.post {
                OptionsDownloader().downloadOptionsWithOtp(
                    otpAndPlaybackInfo.first,
                    otpAndPlaybackInfo.second,
                    null,
                    object : OptionsDownloader.Callback {
                        override fun onOptionsReceived(options: DownloadOptions) {
                            Log.i(TAG, "onOptionsReceived")

                            //Before starting download we have to select one audio and one video track from available tracks in options.

                            //We will store selected tracks here
                            val selectionIndices = IntArray(2)

                            //Selecting video track and audio track
                            var maxBitrate = Int.MIN_VALUE
                            var videoTrackIndex = -1
                            var audioTrackIndex = -1
                            for (index in options.availableTracks.indices) {
                                val track =
                                    options.availableTracks[index]

                                //Download option can contain multiple video track, we select video track with max bitrate.
                                if (track.type == Track.TYPE_VIDEO && track.bitrate > maxBitrate) {
                                    videoTrackIndex = index
                                    maxBitrate = track.bitrate
                                }

                                //Download option will always contain only one audio track.
                                if (track.type == Track.TYPE_AUDIO) {
                                    audioTrackIndex = index
                                }
                            }
                            selectionIndices[0] = audioTrackIndex //Set audio track index.
                            selectionIndices[1] = videoTrackIndex //Set video track index.
                            downloadSelectedOptions(options, selectionIndices)
                        }

                        override fun onOptionsNotReceived(errDesc: ErrorDescription) {
                            val errMsg = "onOptionsNotReceived : $errDesc"
                            Log.e(TAG, errMsg)
                            Toast.makeText(this@DownloadsActivity, errMsg, Toast.LENGTH_LONG).show()
                        }
                    })
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun refreshDownloadsList() {
        maybeCreateManager()
        vdoDownloadManager!!.query(VdoDownloadManager.Query(),
            VdoDownloadManager.QueryResultListener { statusList ->
                // enable sample download buttons for media not downloaded or queued
                if (!containsMediaId(statusList, MEDIA_ID_1))
                    setDownloadListeners(download1, "sample 1", OTP_1, PLAYBACK_INFO_1)
                if (!containsMediaId(statusList, MEDIA_ID_2))
                    setDownloadListeners(download2, "sample 2", OTP_2, PLAYBACK_INFO_2)
                if (!containsMediaId(statusList, MEDIA_ID_3))
                    setDownloadListeners(download3, "sample 3", OTP_3, PLAYBACK_INFO_3)

                // notify recyclerview
                downloadStatusList.clear()
                downloadStatusList.addAll(statusList)
                updateDeleteAllButton()
                downloadAdapter.notifyDataSetChanged()

                if (statusList.isEmpty()) {
                    Log.w(TAG, "No query results found")
                    Toast.makeText(
                        this@DownloadsActivity,
                        "No query results found",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@QueryResultListener
                }
                Log.i(TAG, statusList.size.toString() + " results found")

                val builder = StringBuilder()
                builder.append("query results:").append("\n")
                for (status in statusList) {
                    builder.append(statusString(status)).append(" : ")
                        .append(status.mediaInfo.mediaId).append(", ")
                        .append(status.mediaInfo.title).append("\n")
                }
                Log.i(TAG, builder.toString())
            })
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun deleteAllDownloads() {
        if (downloadStatusList.isNotEmpty()) {
            maybeCreateManager()
            val mediaIdList = ArrayList<String>()
            for (status in downloadStatusList) {
                mediaIdList.add(status.mediaInfo.mediaId)
            }
            val mediaIds = mediaIdList.toTypedArray()
            vdoDownloadManager!!.remove(*mediaIds)
        }
    }

    private fun containsMediaId(statusList: List<DownloadStatus>, mediaId: String): Boolean {
        for (status in statusList) {
            if (status.mediaInfo.mediaId == mediaId) return true
        }
        return false
    }

    private fun setDownloadListeners(
        downloadButton: Button, mediaName: String,
        otp: String, playbackInfo: String
    ) {
        runOnUiThread {
            downloadButton.isEnabled = true
            downloadButton.text = "Download $mediaName"
            downloadButton.setOnClickListener { getOptions(otp, playbackInfo) }
        }
    }

    private fun getOptions(otp: String, playbackInfo: String) {
        val handlerThread = HandlerThread(TAG)
        handlerThread.start()
        Handler(handlerThread.looper).post {
            OptionsDownloader().downloadOptionsWithOtp(
                otp,
                playbackInfo,
                null,
                object : OptionsDownloader.Callback {
                    override fun onOptionsReceived(options: DownloadOptions) {
                        Log.i(TAG, "onOptionsReceived")
                        showSelectionDialog(options, options.mediaInfo.duration)
                    }

                    override fun onOptionsNotReceived(errDesc: ErrorDescription) {
                        val errMsg = "onOptionsNotReceived : $errDesc"
                        Log.e(TAG, errMsg)
                        Toast.makeText(this@DownloadsActivity, errMsg, Toast.LENGTH_LONG).show()
                    }
                })
        }
    }

    fun showSelectionDialog(downloadOptions: DownloadOptions, durationMs: Long) {
        OptionSelector(
            downloadOptions,
            durationMs,
            optionsSelectedCallback,
            OptionSelector.OptionStyle.SHOW_HIGHEST_AND_LOWEST_QUALITY
        )
            .showSelectionDialog(this, "Download options")
    }

    private val optionsSelectedCallback = object : OptionSelector.OptionsSelectedCallback {
        override fun onTracksSelected(downloadOptions: DownloadOptions, selectedTracks: IntArray) {
            Log.i(
                TAG,
                selectedTracks.size.toString() + " options selected: " + selectedTracks.contentToString()
            )
            val durationMs = downloadOptions.mediaInfo.duration
            Log.i(TAG, "---- selected tracks ----")
            for (trackIndex in selectedTracks) {
                Log.i(
                    TAG,
                    getDownloadItemName(downloadOptions.availableTracks[trackIndex], durationMs)
                )
            }
            Log.i(TAG, "---- selected tracks ----")

            // currently only (1 video + 1 audio) track supported
            if (selectedTracks.size != 2) {
                showToastAndLog("Invalid selection", Toast.LENGTH_LONG)
                return
            }

            downloadSelectedOptions(downloadOptions, selectedTracks)

            // disable the corresponding download button
            if (downloadOptions.mediaId == MEDIA_ID_1) download1.isEnabled = false
            if (downloadOptions.mediaId == MEDIA_ID_2) download2.isEnabled = false
            if (downloadOptions.mediaId == MEDIA_ID_3) download3.isEnabled = false
        }
    }

    private fun downloadSelectedOptions(
        downloadOptions: DownloadOptions,
        selectionIndices: IntArray
    ) {
        val selections = DownloadSelections(downloadOptions, selectionIndices)

        // ensure external storage is in read-write mode
        if (!isExternalStorageWritable()) {
            showToastAndLog("External storage is not available", Toast.LENGTH_LONG)
            return
        }

        val downloadLocation: String
        try {
            downloadLocation = getExternalFilesDir(null)!!.path + File.separator + "offlineVdos"
        } catch (npe: NullPointerException) {
            Log.e(TAG, "external storage not available: " + Log.getStackTraceString(npe))
            Toast.makeText(this, "external storage not available", Toast.LENGTH_LONG).show()
            return
        }

        // ensure download directory is created
        val dlLocation = File(downloadLocation)
        if (!(dlLocation.exists() && dlLocation.isDirectory)) {
            // directory not created yet; let's create it
            if (!dlLocation.mkdir()) {
                Log.e(TAG, "failed to create storage directory")
                Toast.makeText(this, "failed to create storage directory", Toast.LENGTH_LONG).show()
                return
            }
        }

        Log.i(TAG, "will save media to $downloadLocation")

        // build a DownloadRequest
        val request = DownloadRequest.Builder(selections).build()

        // enqueue request to VdoDownloadManager for download
        try {
            vdoDownloadManager!!.enqueueV2(request)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "error enqueuing download request")
            Toast.makeText(this, "error enqueuing download request", Toast.LENGTH_LONG).show()
        } catch (e: IllegalStateException) {
            Log.e(TAG, "error enqueuing download request")
            Toast.makeText(this, "error enqueuing download request", Toast.LENGTH_LONG).show()
        }

    }

    private fun isExternalStorageWritable(): Boolean {
        val state = Environment.getExternalStorageState()
        return Environment.MEDIA_MOUNTED == state
    }

    private fun showItemSelectedDialog(downloadStatus: DownloadStatus) {
        val builder = AlertDialog.Builder(this@DownloadsActivity)
        builder.setTitle(downloadStatus.mediaInfo.title)
            .setMessage("Status: " + statusString(downloadStatus).uppercase(Locale.getDefault()))

        if (downloadStatus.status == VdoDownloadManager.STATUS_COMPLETED) {
            builder.setPositiveButton("PLAY") { dialog, _ ->
                startPlayback(downloadStatus)
                dialog.dismiss()
            }
        } else {
            builder.setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
        }
        builder.setNegativeButton("DELETE") { dialog, _ ->
            vdoDownloadManager!!.remove(downloadStatus.mediaInfo.mediaId)
            dialog.dismiss()
        }

        builder.create().show()
    }

    private fun updateListItem(status: DownloadStatus) {
        // if media already in downloadStatusList, update it
        val mediaId = status.mediaInfo.mediaId
        var position = -1
        for (i in 0 until downloadStatusList.size) {
            if (downloadStatusList[i].mediaInfo.mediaId == mediaId) {
                position = i
                break
            }
        }
        if (position >= 0) {
            downloadStatusList[position] = status
            downloadAdapter.notifyItemChanged(position)
        } else {
            Log.e(TAG, "item not found in adapter")
        }
        updateDeleteAllButton()
    }

    private fun addListItem(downloadStatus: DownloadStatus) {
        downloadStatusList.add(0, downloadStatus)
        updateDeleteAllButton()
        downloadAdapter.notifyItemInserted(0)
    }

    private fun removeListItem(status: DownloadStatus) {
        // remove by comparing mediaId; status may change
        val mediaId = status.mediaInfo.mediaId
        removeListItem(mediaId)
    }

    private fun removeListItem(mediaId: String) {
        var position = -1
        for (i in 0 until downloadStatusList.size) {
            if (downloadStatusList[i].mediaInfo.mediaId == mediaId) {
                position = i
                break
            }
        }
        if (position >= 0) {
            downloadStatusList.removeAt(position)
            downloadAdapter.notifyItemRemoved(position)
        }
        updateDeleteAllButton()
    }

    private fun updateDeleteAllButton() {
        deleteAll.isEnabled = downloadStatusList.isNotEmpty()
    }

    private fun startPlayback(downloadStatus: DownloadStatus) {
        if (downloadStatus.status != VdoDownloadManager.STATUS_COMPLETED) {
            showToastAndLog("Download not complete", Toast.LENGTH_SHORT)
            return
        }
        val intent = Intent(this, PlayerActivity::class.java)
        val vdoParams = VdoInitParams.createParamsForOffline(downloadStatus.mediaInfo.mediaId)
        intent.putExtra(PlayerActivity.EXTRA_VDO_PARAMS, vdoParams)
        startActivity(intent)
    }

    private fun showToastAndLog(message: String, toastLength: Int) {
        runOnUiThread { Toast.makeText(applicationContext, message, toastLength).show() }
        Log.i(TAG, message)
    }

    private inner class DownloadAdapter(private val statusList: List<DownloadStatus>) :
        RecyclerView.Adapter<DownloadAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
            View.OnClickListener {
            var title: TextView
            var status: TextView

            init {
                title = itemView.findViewById<View>(R.id.vdo_title) as TextView
                status = itemView.findViewById<View>(R.id.download_status) as TextView
                itemView.setOnClickListener(this)
            }

            override fun onClick(v: View) {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val status = statusList[position]
                    showItemSelectedDialog(status)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val itemView = inflater.inflate(R.layout.sample_list_item, parent, false)
            return ViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val status = statusList[position]
            holder.title.text = status.mediaInfo.title
            holder.status.text = statusString(status).uppercase(Locale.getDefault())
        }

        override fun getItemCount(): Int {
            return statusList.size
        }
    }
}
