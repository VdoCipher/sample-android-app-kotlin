package com.vdocipher.sampleapp.kotlin.tvapp

import android.os.Parcelable
import android.os.Parcel

/*
 * Movie class represents video entity with title, description, image thumbs and video url.
 */
class Video : Parcelable {
    var id: Long = 0
    var title: String? = null
    var description: String? = null
    var cardImageUrl: String? = null
    var videoOtp: String? = null
    var videoPlaybackInfo: String? = null

    constructor() {}
    protected constructor(`in`: Parcel) {
        id = `in`.readLong()
        title = `in`.readString()
        description = `in`.readString()
        cardImageUrl = `in`.readString()
        videoOtp = `in`.readString()
        videoPlaybackInfo = `in`.readString()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(parcel: Parcel, i: Int) {
        parcel.writeLong(id)
        parcel.writeString(title)
        parcel.writeString(description)
        parcel.writeString(cardImageUrl)
        parcel.writeString(videoOtp)
        parcel.writeString(videoPlaybackInfo)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<Video?> = object : Parcelable.Creator<Video?> {
            override fun createFromParcel(`in`: Parcel): Video? {
                return Video(`in`)
            }

            override fun newArray(size: Int): Array<Video?> {
                return arrayOfNulls(size)
            }
        }
    }
}