package com.simplyawakeremake.ui.common

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.simplyawakeremake.R
import com.simplyawakeremake.data.download.track.TrackDownloadStatus

@Composable
fun LoadingButton(
    modifier: Modifier = Modifier.heightIn(min = 48.dp),
    text: String,
    isLoading: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
            )
        } else {
            Text(text)
        }
    }
}

@Composable
fun FavoriteButton(
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp,
    onClick: () -> Unit,
    isFavorite: Boolean
) {
    IconButton(
        modifier = modifier,
        onClick = { onClick() }
    ) {
        val icon = if (isFavorite) {
            Icons.Filled.Favorite
        } else {
            Icons.Outlined.FavoriteBorder
        }

        val tint = if (isFavorite) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }

        Icon(
            modifier = Modifier.requiredSize(iconSize),
            imageVector = icon,
            contentDescription = if (isFavorite) {
                stringResource(R.string.cd_unfavorite_track)
            } else {
                stringResource(R.string.cd_favorite_track)
            },
            tint = tint
        )
    }
}

@Composable
fun TrackDownloadButton(
    status: TrackDownloadStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val enabled = status != TrackDownloadStatus.DOWNLOADING

    IconButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled
    ) {
        when (status) {
            TrackDownloadStatus.NOT_DOWNLOADED -> {
                Icon(
                    imageVector = Icons.Outlined.FileDownload,
                    contentDescription = "Download"
                )
            }

            TrackDownloadStatus.DOWNLOADING -> {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp)
                )
            }

            TrackDownloadStatus.DOWNLOADED -> {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_save_24),
                    contentDescription = "Downloaded"
                )
            }
        }
    }
}
