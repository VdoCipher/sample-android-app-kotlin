package com.vdocipher.sampleapp.kotlin

import android.app.Notification
import android.content.Context
import com.vdocipher.aegis.offline.DownloadStatus
import com.vdocipher.aegis.offline.exoplayer.VdoDownloadNotificationHelper

/**
 * Custom implementation for in-progress, completed and failed notifications.
 * This class will be instantiated by aegis.
 */
class CustomDownloadNotificationHelper
/**
 * @param context   A context.
 * @param channelId The id of the notification channel to use.
 */(context: Context?, channelId: String?) : VdoDownloadNotificationHelper(context, channelId) {

    /**
     * Returns a progress notification for the given download.
     *
     * @param context A context.
     * @param downloadStatuses The downloadStatuses.
     * @return The notification.
     */
    override fun buildProgressNotification(
        context: Context?,
        downloadStatuses: List<DownloadStatus?>?
    ): Notification? {
        //Make changes here to suit your app needs
        return super.buildProgressNotification(context, downloadStatuses)
    }

    /**
     * Returns a notification for a completed download.
     *
     * @param context A context.
     * @param downloadStatus Download status information corresponding to a media download.
     * @return The notification.
     */
    override fun buildDownloadCompletedNotification(
        context: Context?,
        downloadStatus: DownloadStatus?
    ): Notification? {
        //Make changes here to suit your app needs
        return super.buildDownloadCompletedNotification(context, downloadStatus)
    }

    /**
     * Returns a notification for a failed download.
     *
     * @param context A context.
     * @param downloadStatus Download status information corresponding to a media download.
     * @return The notification.
     */
    override fun buildDownloadFailedNotification(
        context: Context?,
        downloadStatus: DownloadStatus?
    ): Notification? {
        //Make changes here to suit your app needs
        return super.buildDownloadFailedNotification(context, downloadStatus)
    }
}