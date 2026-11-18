package com.kore.android.session

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.kore.session.SessionDocument
import com.kore.session.SessionFactory
import com.kore.session.SessionFileStore
import com.kore.session.SessionImporter
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidSessionRepository(context: Context) {
    private val appContext = context.applicationContext
    private val activeSessionFile = File(appContext.filesDir, "sessions/active-session.kore.json")

    suspend fun loadActive(): SessionDocument? = withContext(Dispatchers.IO) {
        if (!activeSessionFile.exists()) return@withContext null
        SessionFileStore.read(activeSessionFile)
    }

    suspend fun create(label: String): SessionDocument = withContext(Dispatchers.IO) {
        val session = SessionFactory.create(label)
        save(session)
        session
    }

    suspend fun importFrom(uri: Uri): SessionDocument = withContext(Dispatchers.IO) {
        val text = appContext.contentResolver.readText(uri)
        val session = SessionImporter.fromJson(text)
        save(session)
        session
    }

    suspend fun save(session: SessionDocument) = withContext(Dispatchers.IO) {
        SessionFileStore.writeAtomically(activeSessionFile, session)
    }

    suspend fun exportTo(uri: Uri, session: SessionDocument) = withContext(Dispatchers.IO) {
        appContext.contentResolver.openOutputStream(uri, "wt")
            ?.use { stream -> stream.write(session.toJson().toByteArray(Charsets.UTF_8)) }
            ?: throw IllegalStateException("The export destination could not be opened")
    }

    private fun ContentResolver.readText(uri: Uri): String = openInputStream(uri)
        ?.use { stream -> stream.readBytes().toString(Charsets.UTF_8) }
        ?: throw IllegalArgumentException("The selected file could not be opened")
}
