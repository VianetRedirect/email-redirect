package com.vianetredirect.firetvremote

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import java.io.File
import java.io.FileNotFoundException

/**
 * Hands the app's own installed APK to the share sheet, so "Share this app" can send it straight
 * to another phone over Quick Share, Bluetooth, Files and the like. Read-only, not exported;
 * access is granted per share through the intent's URI permission.
 */
class ApkProvider : ContentProvider() {

    companion object {
        const val AUTHORITY = "com.vianetredirect.firetvremote.apk"
        const val FILE_NAME = "FireRemote.apk"
    }

    override fun onCreate(): Boolean = true

    private fun apkFile(): File {
        val ctx = context ?: throw FileNotFoundException("no context")
        return File(ctx.applicationInfo.sourceDir)
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        if (mode != "r") throw FileNotFoundException("read only")
        return ParcelFileDescriptor.open(apkFile(), ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun getType(uri: Uri): String = "application/vnd.android.package-archive"

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor {
        val cols = projection ?: arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
        val cursor = MatrixCursor(cols, 1)
        val row = cursor.newRow()
        for (col in cols) {
            when (col) {
                OpenableColumns.DISPLAY_NAME -> row.add(FILE_NAME)
                OpenableColumns.SIZE -> row.add(apkFile().length())
                else -> row.add(null)
            }
        }
        return cursor
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int = 0
}
