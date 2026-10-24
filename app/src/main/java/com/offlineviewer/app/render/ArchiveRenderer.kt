package com.offlineviewer.app.render

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.offlineviewer.app.render.logic.ArchiveLister

/**
 * Lists the contents of an archive (zip, jar, tar, gz/tgz) offline using the
 * pure, dependency-free [ArchiveLister] (java.util.zip only). This is a
 * table-of-contents view: it shows each entry's name and uncompressed size plus
 * a header with the total entry count and total size. Contents are NOT
 * extracted - a clear note states this.
 *
 * The archive is read as raw bytes. A malformed archive that yields nothing
 * degrades to a [RendererError]; a partial/capped listing shows a notice.
 */
class ArchiveRenderer : FileRenderer {

    override val formatName: String = "Archive"
    override val supportedMimeTypes: Set<String> = setOf(
        "application/zip",
        "application/java-archive",
        "application/x-tar",
        "application/gzip",
        "application/x-gzip",
    )
    override val supportedExtensions: Set<String> = setOf(
        "zip", "jar", "tar", "gz", "tgz",
    )

    @Composable
    override fun Render(file: LoadedFile, modifier: Modifier) {
        val listing = remember(file) {
            val kind = ArchiveLister.kindFor(file.extension, file.mimeType)
            ArchiveLister.list(file.bytes, kind)
        }

        if (listing.isEmpty) {
            RendererError(
                message = if (listing.truncatedByError) {
                    "This archive appears to be malformed or truncated, so its contents could not be listed."
                } else {
                    "This archive is empty or could not be read."
                },
                modifier = modifier,
            )
            return
        }

        Column(modifier = modifier.fillMaxSize()) {
            Header(listing)
            if (listing.truncated) {
                FallbackNotice(
                    message = "Large archive: showing the first ${ArchiveLister.MAX_ENTRIES} entries.",
                )
            } else if (listing.truncatedByError) {
                FallbackNotice(
                    message = "This archive seems partially damaged; the listing below may be incomplete.",
                )
            }
            HorizontalDivider()
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(listing.entries) { entry ->
                    EntryRow(entry)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }

    @Composable
    private fun Header(listing: ArchiveLister.Listing) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(
                text = "${listing.entries.size} entries " +
                    "(${listing.fileCount} files, ${listing.directoryCount} folders)",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Total size ${ArchiveLister.humanReadableSize(listing.totalUncompressedSize)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
            Text(
                text = "Contents are listed, not extracted.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }

    @Composable
    private fun EntryRow(entry: ArchiveLister.Entry) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .semantics {
                    contentDescription = if (entry.isDirectory) {
                        "Folder: ${entry.name}"
                    } else {
                        "File: ${entry.name}, ${ArchiveLister.humanReadableSize(entry.size)}"
                    }
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = if (entry.isDirectory) {
                    Icons.Outlined.Folder
                } else {
                    Icons.AutoMirrored.Outlined.InsertDriveFile
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = entry.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (!entry.isDirectory) {
                Text(
                    text = ArchiveLister.humanReadableSize(entry.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
