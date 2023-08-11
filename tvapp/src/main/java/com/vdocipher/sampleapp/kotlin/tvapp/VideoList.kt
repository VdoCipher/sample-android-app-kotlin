package com.vdocipher.sampleapp.kotlin.tvapp

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
            "Sample")

        val otp = arrayOf(
            "20150519versASE31ba8fc50a0ac49b8e74b9c40f49e099755cd36dc8adccaa3")

        val playbackInfo = arrayOf(
            "eyJ2aWRlb0lkIjoiM2YyOWI1NDM0YTVjNjE1Y2RhMThiMTZhNjIzMmZkNzUifQ==")

        val description = arrayOf(
            "Sample Video")


        val cardImageUrl = arrayOf(
            "https://d1z78r8i505acl.cloudfront.net/poster/toQsFmrSDfY8z.720.jpeg")

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