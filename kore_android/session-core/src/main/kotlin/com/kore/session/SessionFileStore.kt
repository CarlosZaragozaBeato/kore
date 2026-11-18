package com.kore.session

import java.io.File
import java.io.FileOutputStream

object SessionFileStore {
    fun read(file: File): SessionDocument = SessionImporter.fromJson(
        file.readBytes().toString(Charsets.UTF_8),
    )

    fun writeAtomically(file: File, session: SessionDocument) {
        val parent = file.absoluteFile.parentFile
            ?: throw IllegalArgumentException("The session must have a destination directory")
        if (!parent.exists() && !parent.mkdirs()) {
            throw IllegalStateException("The session directory could not be created")
        }
        val temporary = File.createTempFile(".${file.name}.", ".tmp", parent)
        try {
            FileOutputStream(temporary).use { output ->
                output.write(session.toJson().toByteArray(Charsets.UTF_8))
                output.fd.sync()
            }
            if (!temporary.renameTo(file)) {
                temporary.copyTo(file, overwrite = true)
            }
        } finally {
            temporary.delete()
        }
    }
}
