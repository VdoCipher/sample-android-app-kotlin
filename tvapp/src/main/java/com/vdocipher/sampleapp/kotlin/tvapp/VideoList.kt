package com.vdocipher.sampleapp.kotlin.tvapp

import java.util.ArrayList

object VideoList {
    val VIDEO_CATEGORY = arrayOf(
        "Sample Videos",
        "Sample Videos (Custom Ui)"
    )
    private var list: MutableList<Video>? = null
    private var count: Long = 0
    fun getList(): List<Video>? {
        if (list == null) {
            list = setupVideos()
        }
        return list
    }

    fun setupVideos(): MutableList<Video>? {
        list = ArrayList()
        val title = arrayOf(
            "Big buck bunny",
            "Elephant Dream"
        )
        val otp = arrayOf(
            "20160313versASE323iH4eNlQDjw93A8kxlDIuD0JVji4KTAsQbq5vWzyt7NvXTg",
            "20160313versASE323SXXgm4fzUPSaZxsUIpX9vMO0t3MgCSTEKyRCALfJG8je72"
        )
        val playbackInfo = arrayOf(
            "eyJ2aWRlb0lkIjoiMTlkNzg3NzcwMmFlNGE0NmIwZDcwZTIwZThlM2FjNzIifQ==",
            "eyJ2aWRlb0lkIjoiZWFiMTU2ZWM3ODM3NGRjYzk1NTFhMDIwNTU1MmRkYTcifQ=="
        )
        val description = arrayOf(
            "Big Buck Bunny tells the story of a giant rabbit with a heart bigger than himself.",
            "The first Blender Open Movie from 2006"
        )
        val cardImageUrl = arrayOf(
            "https://d1z78r8i505acl.cloudfront.net/poster/toQsFmrSDfY8z.720.jpeg",
            "https://d1z78r8i505acl.cloudfront.net/poster/BGbRHpGC5OAzy.720.jpeg"
        )
        for (index in title.indices) {
            (list as ArrayList<Video>).add(
                buildVideoInfo(
                    title[index],
                    description[index],
                    otp[index],
                    playbackInfo[index],
                    cardImageUrl[index]
                )
            )
        }
        return list
    }

    private fun buildVideoInfo(
        title: String,
        description: String,
        otp: String,
        playbackInfo: String,
        cardImageUrl: String
    ): Video {
        val video = Video()
        video.id = count++
        video.title = title
        video.description = description
        video.videoOtp = otp
        video.videoPlaybackInfo = playbackInfo
        video.cardImageUrl = cardImageUrl
        return video
    }
}