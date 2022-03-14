package com.vdocipher.sampleapp.kotlin

import com.vdocipher.aegis.media.ErrorDescription

/**
 * Descriptive error messages to show in player user interface for cases where user action might be
 * helpful.
 */
object ErrorMessages {
    /**
     * Customize error message displayed on the player view depending on the error code.
     */
    fun getErrorMessage(errorDescription: ErrorDescription): String {
        val messagePrefix = "Error: " + errorDescription.errorCode + ". "
        return when (errorDescription.errorCode) {
            1201, 1202 -> messagePrefix + "Please make sure USB debugging is disabled. In " +
                    "\"Settings > System > Advanced > Developer options > Turn off USB debugging\""
            2013, 2018 -> messagePrefix + "OTP is expired or invalid. Please go back, and start " +
                    "playback again."
            4101 -> messagePrefix + "Invalid video parameters. Please contact the app developer."
            4102 -> messagePrefix + "Offline video not found. Please make sure the video was " +
                    "downloaded successfully and not deleted."
            5110, 5124, 5130 -> messagePrefix + "Please check your internet connection and try restarting " +
                    "the app."
            5113, 5123, 5133, 5152 -> messagePrefix + "Temporary service error. This should automatically resolve " +
                    "quickly. Please try playback again."
            5151 -> messagePrefix + "Network error, possibly with your local ISP. Please try " +
                    "after some time."
            5160, 5161 -> messagePrefix + "Downloaded media files have been accidentally deleted by " +
                    "some other app in your mobile. Kindly download the video again and do " +
                    "not use cleaner apps."
            6101, 6120, 6122 -> messagePrefix + "Error decoding video. Kindly try restarting the phone and app."
            6102 -> messagePrefix + "Offline video download is not yet complete or it failed. " +
                    "Please make sure it is successfully downloaded."
            1220, 1250, 1253, 2021, 2022, 6155, 6156, 6157, 6161, 6166, 6172, 6177, 6178, 6181, 6186, 6190, 6196 -> messagePrefix + "Phone is not compatible for secure playback. " +
                    "Kindly update your OS, restart the phone and app. If still not " +
                    "corrected, factory reset can be tried if possible."
            6187 -> messagePrefix + "Rental license for downloaded video has expired. Kindly " +
                    "download again."
            else -> """
                 An error occurred: ${errorDescription.errorCode}
                 Tap to retry
                 """.trimIndent()
        }
    }
}