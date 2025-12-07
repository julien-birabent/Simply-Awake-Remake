package com.simplyawakeremake.ui.trackfilter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.simplyawakeremake.R
import com.simplyawakeremake.ui.theme.SimplyAwakeRemakeTheme
import com.simplyawakeremake.ui.tracklist.TrackCategoryUi

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActiveTrackFiltersHeader(
    modifier: Modifier = Modifier,
    filterState: TrackFilter,
    availableCategories: List<TrackCategoryUi>,
    onRemoveStateFilter: (TrackStateFilter) -> Unit,
    onClearDuration: () -> Unit,
    onRemoveCategory: (String) -> Unit,
    onClearAll: () -> Unit,
) {

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background
    ) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filterState.stateFilters.forEach { stateFilter ->
                RemovableFilterChip(
                    label = stringResource(stateFilter.toStringRes()),
                    onClick = { onRemoveStateFilter(stateFilter) }
                )
            }

            filterState.durationBucket?.let { bucket ->
                RemovableFilterChip(
                    label = bucket.displayLabel() + " min",
                    onClick = onClearDuration
                )
            }

            val labelById = availableCategories.associateBy({ it.id }, { it.label })
            filterState.selectedCategoryIds.forEach { categoryId ->
                val label = labelById[categoryId] ?: categoryId
                RemovableFilterChip(
                    label = label,
                    onClick = { onRemoveCategory(categoryId) }
                )
            }

            ResetFiltersChip(
                label = stringResource(id = R.string.track_filter_bottom_sheet_action_reset),
                onClick = onClearAll
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ActiveTrackFiltersHeaderPreview_AllFilters() {
    val dummyFilter = TrackFilter(
        sortOption = TrackSortOption.RELEASE_ORDINAL,
        stateFilters = setOf(
            TrackStateFilter.Downloaded,
            TrackStateFilter.Favorite,
        ),
        durationBucket = DurationBucket.MEDIUM,
        selectedCategoryIds = setOf("sleep", "anxiety")
    )

    val dummyCategories = listOf(
        TrackCategoryUi(id = "sleep", label = "Sleep"),
        TrackCategoryUi(id = "anxiety", label = "Anxiety"),
        TrackCategoryUi(id = "focus", label = "Focus"),
    )

    SimplyAwakeRemakeTheme {
        ActiveTrackFiltersHeader(
            filterState = dummyFilter,
            availableCategories = dummyCategories,
            onRemoveStateFilter = {},
            onClearDuration = {},
            onRemoveCategory = {},
            onClearAll = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ActiveTrackFiltersHeaderPreview_OnlyCategories() {
    val dummyFilter = TrackFilter(
        sortOption = TrackSortOption.TITLE_ASC,
        stateFilters = emptySet(),
        durationBucket = null,
        selectedCategoryIds = setOf("sleep", "body_scan")
    )

    val dummyCategories = listOf(
        TrackCategoryUi(id = "sleep", label = "Sleep"),
        TrackCategoryUi(id = "body_scan", label = "Body scan"),
        TrackCategoryUi(id = "focus", label = "Focus"),
    )

    SimplyAwakeRemakeTheme {
        ActiveTrackFiltersHeader(
            filterState = dummyFilter,
            availableCategories = dummyCategories,
            onRemoveStateFilter = {},
            onClearDuration = {},
            onRemoveCategory = {},
            onClearAll = {},
        )
    }
}
