package com.simplyawakeremake.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
        modifier = modifier.wrapContentSize(),
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
    modifier: Modifier = Modifier,
    status: TrackDownloadStatus,
    onClick: () -> Unit
) {
    val enabled = status != TrackDownloadStatus.DOWNLOADING

    IconButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,

        ) {
        when (status) {
            TrackDownloadStatus.NOT_DOWNLOADED -> {
                Icon(
                    imageVector = Icons.Outlined.FileDownload,
                    contentDescription = "Download",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            TrackDownloadStatus.DOWNLOADING -> {
                StopWithCircularProgress(modifier = Modifier.size(28.dp)) {
                    onClick()
                }
            }

            TrackDownloadStatus.DOWNLOADED -> {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_check_circle),
                    contentDescription = "Downloaded",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun StopWithCircularProgress(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.fillMaxSize(),
            strokeWidth = 2.dp,
        )

        IconButton(
            onClick = onClick,
            modifier = Modifier.fillMaxSize(),
        ) {
            Icon(
                modifier = Modifier.size(20.dp),
                imageVector = Icons.Default.Stop,
                contentDescription = "",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
