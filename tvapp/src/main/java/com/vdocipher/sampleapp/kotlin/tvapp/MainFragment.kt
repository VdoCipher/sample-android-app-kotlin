package com.vdocipher.sampleapp.kotlin.tvapp

import android.content.Context
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.app.BackgroundManager
import android.util.DisplayMetrics
import com.vdocipher.sampleapp.kotlin.tvapp.R
import android.widget.Toast
import android.content.Intent
import android.graphics.Color
import android.view.View
import androidx.core.content.ContextCompat
import androidx.leanback.widget.*
import java.util.*

class MainFragment : BrowseSupportFragment() {
    private var mBackgroundManager: BackgroundManager? = null
    override fun onAttach(context: Context) {
        super.onAttach(context)
        prepareBackgroundManager()
        setupUIElements()
        loadRows()
        setupEventListeners()
    }

    private fun loadRows() {
        val list = VideoList.getList()
        val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
        val cardPresenter = CardPresenter()
        val listRowAdapter = ArrayObjectAdapter(cardPresenter)
        val header = HeaderItem(0, VideoList.VIDEO_CATEGORY[0])
        listRowAdapter.addAll(0, list)
        rowsAdapter.add(ListRow(header, listRowAdapter))
        val cardPresenter1 = CardPresenter()
        val listRowAdapter1 = ArrayObjectAdapter(cardPresenter1)
        val header1 = HeaderItem(1, VideoList.VIDEO_CATEGORY[1])
        listRowAdapter1.addAll(0, list)
        rowsAdapter.add(ListRow(header1, listRowAdapter1))
        adapter = rowsAdapter
    }

    private fun prepareBackgroundManager() {
        mBackgroundManager = BackgroundManager.getInstance(requireActivity())
       // mBackgroundManager.attach(activity?.window)
        val mMetrics = DisplayMetrics()
        requireActivity().windowManager.defaultDisplay.getMetrics(mMetrics)
    }

    private fun setupUIElements() {
        title = getString(R.string.browse_title)
        headersState = HEADERS_ENABLED
        isHeadersTransitionOnBackEnabled = true
        brandColor =
            ContextCompat.getColor(requireActivity(), R.color.fastlane_background)
        searchAffordanceColor =
            ContextCompat.getColor(requireActivity(), R.color.search_opaque)
    }

    private fun setupEventListeners() {
        setOnSearchClickedListener { view: View? ->
            Toast.makeText(
                activity,
                "Implement your own in-app search",
                Toast.LENGTH_LONG
            ).show()
        }
        onItemViewClickedListener = ItemViewClickedListener()
        onItemViewSelectedListener = ItemViewSelectedListener()
    }

    private fun updateBackground() {
        val rnd = Random()
        val color = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256))
        mBackgroundManager!!.color = color
    }

    private inner class ItemViewClickedListener : OnItemViewClickedListener {
        override fun onItemClicked(
            itemViewHolder: Presenter.ViewHolder,
            item: Any,
            rowViewHolder: RowPresenter.ViewHolder,
            row: Row
        ) {
            if (item is Video) {
                val intent: Intent
                intent = if (row.headerItem.id == 0L) {
                    Intent(activity, TvPlayerUIActivity::class.java)
                } else {
                    Intent(activity, TvPlayerActivity::class.java)
                }
                intent.putExtra(TvPlayerUIActivity.VIDEO, item)
                requireActivity().startActivity(intent)
            }
        }
    }

    private inner class ItemViewSelectedListener : OnItemViewSelectedListener {
        override fun onItemSelected(
            itemViewHolder: Presenter.ViewHolder?,
            item: Any?,
            rowViewHolder: RowPresenter.ViewHolder?,
            row: Row?
        ) {
            if (item is Video) {
                updateBackground()
            }
        }
    }
}