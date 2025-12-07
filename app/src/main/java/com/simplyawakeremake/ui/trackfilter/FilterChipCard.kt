package com.simplyawakeremake.ui.trackfilter

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.ClearAll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.simplyawakeremake.ui.theme.SimplyAwakeRemakeTheme


@Composable
fun FilterChipCard(
    modifier: Modifier = Modifier,
    label: String,
    selected: Boolean,
    iconImage: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    val backgroundColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
            iconImage?.let {
                Icon(
                    modifier = Modifier.size(20.dp),
                    imageVector = iconImage,
                    contentDescription = "",
                    tint = iconTint
                )
            }

        }
    }
}

@Composable
fun RemovableFilterChip(
    modifier: Modifier = Modifier,
    label: String,
    onClick: () -> Unit
) {
    FilterChipCard(
        modifier = modifier,
        label = label,
        selected = true,
        onClick = onClick,
        iconImage = Icons.Filled.Close,
        iconTint = MaterialTheme.colorScheme.onSurface
    )
}

@Composable fun ResetFiltersChip(
    modifier: Modifier = Modifier,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.ClearAll,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}



@Preview
@Composable
private fun preview() {
    SimplyAwakeRemakeTheme {
        Column {
            FilterChipCard(
                modifier = Modifier,
                "Favorites",
                selected = true,
                iconImage = Icons.Filled.Favorite
            ) {

            }

            FilterChipCard(
                modifier = Modifier,
                "Favorites",
                selected = true
            ) {}

            FilterChipCard(
                modifier = Modifier,
                "Favorites",
                selected = false
            ) {}
            RemovableFilterChip(Modifier, "Favorite") {}
            ResetFiltersChip(Modifier, "Reset"){}
        }
    }
}
