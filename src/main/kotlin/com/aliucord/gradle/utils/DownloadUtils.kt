/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.aliucord.gradle.utils

import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.internal.logging.progress.ProgressLogger
import org.gradle.internal.logging.progress.ProgressLoggerFactory
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import kotlin.math.max

internal fun createProgressLogger(project: Project, loggerCategory: String): ProgressLogger {
    val services = (project as ProjectInternal).services
    val progressLoggerFactory = services.get(ProgressLoggerFactory::class.java)

    return progressLoggerFactory.newOperation(loggerCategory).apply {
        description = loggerCategory
    }
}

internal fun downloadFromStream(url: String, output: File, progressLogger: ProgressLogger) {
    progressLogger.started("Connecting to remote for download")

    val url = URI.create(url).toURL()
    val connection = url.openConnection().apply {
        connectTimeout = 30_000
        connect()
    }

    var downloaded = 0L
    val size = toLengthText(max(connection.contentLengthLong, 0))
    val stream = connection.getInputStream()

    val tempFile = File.createTempFile(output.name, ".part", output.parentFile)

    try {
        FileOutputStream(tempFile).use { file ->
            val buf = ByteArray(1024 * 1024 * 10) // 10MiB
            var read: Int
            while (stream.read(buf).also { read = it } >= 0) {
                file.write(buf, 0, read)
                file.flush()
                downloaded += read
                progressLogger.progress("Downloaded ${toLengthText(downloaded)}/$size")
            }
        }
        tempFile.renameTo(output)
    } catch (t: Throwable) {
        tempFile.delete()
        progressLogger.completed(t.message ?: "Unknown error", /* failed: */ true)
        throw t
    } finally {
        stream.close()
        progressLogger.completed()
    }
}

private fun toLengthText(bytes: Long): String {
    return if (bytes < 1024) {
        "$bytes B"
    } else if (bytes < 1024 * 1024) {
        (bytes / 1024).toString() + " KB"
    } else if (bytes < 1024 * 1024 * 1024) {
        String.format("%.2f MB", bytes / (1024.0 * 1024.0))
    } else {
        String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
    }
}
