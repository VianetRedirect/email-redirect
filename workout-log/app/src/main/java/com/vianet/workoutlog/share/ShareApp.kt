package com.vianet.workoutlog.share

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/** Ways to pass the app on to someone else. */
object ShareApp {
    /** Public releases page. Every push publishes a "Workout Log build N" release with the APK attached. */
    const val DOWNLOAD_URL = "https://github.com/VianetRedirect/email-redirect/releases"

    /**
     * Shares this app's own installed APK as a file, so it can go phone to phone over
     * Nearby Share, Bluetooth, WhatsApp or email with no account or store involved.
     */
    fun shareApk(context: Context): Boolean {
        val source = File(context.applicationInfo.sourceDir)
        if (!source.exists()) return false
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        val target = File(dir, "WorkoutLog.apk")
        source.copyTo(target, overwrite = true)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", target)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.android.package-archive"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Workout Log app")
            putExtra(Intent.EXTRA_TEXT, "Here's the Workout Log app. Open the file on your phone to install it.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "Share app").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return true
    }

    fun shareLink(context: Context) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Workout Log app")
            putExtra(Intent.EXTRA_TEXT, "Download Workout Log for Android: $DOWNLOAD_URL")
        }
        context.startActivity(Intent.createChooser(send, "Share link").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun shareText(context: Context, text: String) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(send, "Share").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
