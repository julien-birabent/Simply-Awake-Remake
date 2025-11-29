package com.simplyawakeremake.ui.common

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.simplyawakeremake.R

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
fun FavoriteButton(onClick: () -> Unit, isFavorite: Boolean){
    IconButton(
        onClick = { onClick() },
        modifier = Modifier.padding(start = 4.dp)
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