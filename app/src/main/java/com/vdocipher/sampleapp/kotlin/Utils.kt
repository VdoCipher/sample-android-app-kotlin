package com.vdocipher.sampleapp.kotlin

import android.util.Log

import com.vdocipher.aegis.player.VdoPlayer

import org.json.JSONException
import org.json.JSONObject

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.abs

/**
 * Utility functions.
 */

private const val TAG = "Utils"

// call on non-ui thread only
/**
 * @throws JSONException
 * @throws IOException
 */
fun sampleOtpAndPlaybackInfo(): Pair<String, String> {
    val SAMPLE_OTP_PLAYBACK_INFO_URL = "https://dev.vdocipher.com/api/site/homepage_video"

    val url = URL(SAMPLE_OTP_PLAYBACK_INFO_URL)
    val connection = url.openConnection() as HttpURLConnection
    connection.requestMethod = "GET"
    val responseCode = connection.responseCode

    if (responseCode == 200) {
        val inputStream = connection.inputStream

        val br = BufferedReader(InputStreamReader(inputStream))
        var line: String?
        val responseBuffer = StringBuffer()

        while (true) {
            line = br.readLine()
            if (line == null) break
            responseBuffer.append(line)
        }
        br.close()

        val response = responseBuffer.toString()
        Log.i(TAG, "response: $response")

        val jObj = JSONObject(response)
        val otp = jObj.getString("otp")
        val playbackInfo = jObj.getString("playbackInfo")
        return Pair(otp, playbackInfo)
    } else {
        Log.e(TAG, "Network error, response code $responseCode")
        throw IOException("Network error, code $responseCode")
    }
}

fun playbackStateString(playWhenReady: Boolean, playbackState: Int): String {
    val stateName = when (playbackState) {
        VdoPlayer.STATE_IDLE -> "STATE_IDLE"
        VdoPlayer.STATE_READY -> "STATE_READY"
        VdoPlayer.STATE_BUFFERING -> "STATE_BUFFERING"
        VdoPlayer.STATE_ENDED -> "STATE_ENDED"
        else -> "STATE_UNKNOWN"
    }
    return "playWhenReady $playWhenReady, $stateName"
}

/**
 * @return index of number in provided array closest to the provided number
 */
fun getClosestFloatIndex(refArray: FloatArray, comp: Float): Int {
    var distance = Math.abs(refArray[0] - comp)
    var index = 0
    refArray.forEachIndexed { i, fl ->
        val currDistance = abs(fl - comp)
        if (currDistance < distance) {
            index = i
            distance = currDistance
        }
    }
    return index
}

fun getSizeBytes(bitsPerSec: Int, millis: Long): Long {
    return ((bitsPerSec.toDouble() / 8) * (millis / 1000)).toLong()
}

fun round(value: Double, places: Int): Double {
    if (places < 0) throw IllegalArgumentException()

    var bd = BigDecimal(value)
    bd = bd.setScale(places, RoundingMode.HALF_UP)
    return bd.toDouble()
}

fun getSizeString(bitsPerSec: Int, millis: Long): String {
    val sizeMB = (bitsPerSec * (millis / 1000)).toDouble() / (8 * 1024 * 1024)
    return round(sizeMB, 2).toString() + " MB"
}

fun digitalClockTime(timeInMillis: Int): String {
    val totalSeconds = timeInMillis / 1000
    val hours = totalSeconds / (60 * 60)
    val minutes = (totalSeconds - hours * 60 * 60) / 60
    val seconds = totalSeconds - hours * 60 * 60 - minutes * 60

    var timeThumb = ""
    timeThumb += when {
        hours >= 10 -> "$hours:"
        hours > 0 -> "0$hours:"
        else -> ""
    }

    timeThumb += when {
        minutes >= 10 -> "$minutes:"
        minutes > 0 -> "0$minutes:"
        else -> "00" + ":"
    }

    timeThumb += if (seconds < 10) "0$seconds" else seconds

    return timeThumb
}
