package com.vdocipher.sampleapp.kotlin.tvapp

import android.graphics.drawable.Drawable
import android.util.Log
import androidx.leanback.widget.Presenter
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.vdocipher.sampleapp.kotlin.tvapp.R
import androidx.leanback.widget.ImageCardView
import com.bumptech.glide.Glide

/*
 * A CardPresenter is used to generate Views and bind Objects to them on demand.
 * It contains an Image CardView
 */
class CardPresenter : Presenter() {
    private var mDefaultCardImage: Drawable? = null
    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        Log.d(TAG, "onCreateViewHolder")
        sDefaultBackgroundColor = ContextCompat.getColor(parent.context, R.color.default_background)
        sSelectedBackgroundColor =
            ContextCompat.getColor(parent.context, R.color.selected_background)
        mDefaultCardImage = ContextCompat.getDrawable(parent.context, R.drawable.movie)
        val cardView: ImageCardView = object : ImageCardView(parent.context) {
            override fun setSelected(selected: Boolean) {
                updateCardBackgroundColor(this, selected)
                super.setSelected(selected)
            }
        }
        cardView.isFocusable = true
        cardView.isFocusableInTouchMode = true
        updateCardBackgroundColor(cardView, false)
        return ViewHolder(cardView)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        val movie = item as Video
        val cardView = viewHolder.view as ImageCardView
        Log.d(TAG, "onBindViewHolder")
        if (movie.cardImageUrl != null) {
            cardView.titleText = movie.title
            cardView.contentText = movie.description
            cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT)
            Glide.with(viewHolder.view.context)
                .load(movie.cardImageUrl)
                .centerCrop()
                .error(mDefaultCardImage)
                .into(cardView.mainImageView)
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        Log.d(TAG, "onUnbindViewHolder")
        val cardView = viewHolder.view as ImageCardView
        // Remove references to images so that the garbage collector can free up memory
        cardView.badgeImage = null
        cardView.mainImage = null
    }

    companion object {
        private const val TAG = "CardPresenter"
        private const val CARD_WIDTH = 313
        private const val CARD_HEIGHT = 176
        private var sSelectedBackgroundColor = 0
        private var sDefaultBackgroundColor = 0
        private fun updateCardBackgroundColor(view: ImageCardView, selected: Boolean) {
            val color = if (selected) sSelectedBackgroundColor else sDefaultBackgroundColor
            // Both background colors should be set because the view"s background is temporarily visible
            // during animations.
            view.setBackgroundColor(color)
            view.setInfoAreaBackgroundColor(color)
        }
    }
}