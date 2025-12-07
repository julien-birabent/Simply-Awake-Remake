package com.simplyawakeremake.ui.trackfilter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.simplyawakeremake.R
import com.simplyawakeremake.ui.theme.CoolYellow
import com.simplyawakeremake.ui.theme.SimplyAwakeRemakeTheme
import com.simplyawakeremake.ui.tracklist.TrackCategoryUi
import org.koin.androidx.compose.koinViewModel


@Composable
fun TrackFilterBottomSheet(
    initialFilterState: TrackFilterState,
    availableCategories: List<TrackCategoryUi>,
    onCancel: () -> Unit,
    onApplyFilter: (TrackFilterState) -> Unit,
    viewModel: TrackFilterViewModel = koinViewModel()
) {
    LaunchedEffect(initialFilterState) {
        viewModel.setInitialFilterState(initialFilterState)
    }

    val filterState by viewModel.filterState.collectAsState()

    TrackFilterBottomSheetContent(
        filterState = filterState,
        availableCategories = availableCategories,
        onSortSelected = viewModel::onSortOptionSelected,
        onStateFilterToggled = viewModel::onToggleStateFilter,
        onDurationBucketSelected = viewModel::onDurationBucketSelected,
        onCategoryToggled = viewModel::onToggleCategory,
        onReset = viewModel::reset,
        onCancel = onCancel,
        onApply = { onApplyFilter(filterState) }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TrackFilterBottomSheetContent(
    filterState: TrackFilterState,
    availableCategories: List<TrackCategoryUi>,
    onSortSelected: (TrackSortOption) -> Unit,
    onStateFilterToggled: (TrackStateFilter) -> Unit,
    onDurationBucketSelected: (DurationBucket?) -> Unit,
    onCategoryToggled: (String) -> Unit,
    onReset: () -> Unit,
    onCancel: () -> Unit,
    onApply: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(id = R.string.track_filter_bottom_sheet_sort_by),
            style = MaterialTheme.typography.titleMedium
        )

        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            listOf(
                TrackSortOption.RELEASE_ORDINAL to stringResource(
                    id = R.string.track_filter_bottom_sheet_sort_option_release
                ),
                TrackSortOption.TITLE_ASC to stringResource(
                    id = R.string.track_filter_bottom_sheet_sort_option_title
                ),
                TrackSortOption.DURATION_ASC to stringResource(
                    id = R.string.track_filter_bottom_sheet_sort_option_duration
                )
            ).forEach { (option, label) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = filterState.sortOption == option,
                        onClick = { onSortSelected(option) }
                    )
                    Text(text = label)
                }
            }
        }

        Text(
            text = stringResource(id = R.string.track_filter_bottom_sheet_filter_by_state),
            style = MaterialTheme.typography.titleMedium
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChipCard(
                label = stringResource(id = R.string.track_filter_bottom_sheet_state_downloaded_only),
                selected = TrackStateFilter.Downloaded in filterState.stateFilters,
                onClick = { onStateFilterToggled(TrackStateFilter.Downloaded) },
                iconImage = Icons.Outlined.FileDownload,
            )
            FilterChipCard(
                label = stringResource(id = R.string.track_filter_bottom_sheet_state_favorites),
                selected = TrackStateFilter.Favorite in filterState.stateFilters,
                onClick = { onStateFilterToggled(TrackStateFilter.Favorite) },
                iconImage =  Icons.Filled.Favorite,
                iconTint = MaterialTheme.colorScheme.error
            )
            FilterChipCard(
                label = stringResource(id = R.string.track_filter_bottom_sheet_state_never_played),
                selected = TrackStateFilter.NeverPlayed in filterState.stateFilters,
                onClick = { onStateFilterToggled(TrackStateFilter.NeverPlayed) },
                iconImage = Icons.Default.AutoAwesome,
                iconTint = CoolYellow
            )
        }

        Text(
            text = stringResource(id = R.string.track_filter_bottom_sheet_duration),
            style = MaterialTheme.typography.titleMedium
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DurationBucket.entries.forEach { bucket ->
                val selected = filterState.durationBucket == bucket

                FilterChipCard(
                    label = bucket.displayLabel() + " min",
                    selected = selected,
                    onClick = {
                        onDurationBucketSelected(
                            if (selected) null else bucket
                        )
                    }
                )
            }
        }

        if (availableCategories.isNotEmpty()) {
            Text(
                text = stringResource(id = R.string.track_filter_bottom_sheet_categories),
                style = MaterialTheme.typography.titleMedium
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                availableCategories.forEach { category ->
                    val selected = category.id in filterState.selectedCategoryIds

                    FilterChipCard(
                        label = category.label,
                        selected = selected,
                        onClick = { onCategoryToggled(category.id) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onReset) {
                Text(text = stringResource(id = R.string.track_filter_bottom_sheet_action_reset))
            }

            Spacer(modifier = Modifier.weight(1f))

            TextButton(onClick = onCancel) {
                Text(text = stringResource(id = R.string.track_filter_bottom_sheet_action_cancel))
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(onClick = onApply) {
                Text(text = stringResource(id = R.string.track_filter_bottom_sheet_action_apply))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TrackFilterBottomSheetContentPreview() {
    val dummyFilterState = TrackFilterState(
        sortOption = TrackSortOption.DURATION_ASC,
        stateFilters = setOf(
            TrackStateFilter.Downloaded,
            TrackStateFilter.Favorite
        ),
        durationBucket = DurationBucket.MEDIUM,
        selectedCategoryIds = setOf("sleep", "anxiety")
    )

    val dummyCategories = listOf(
        TrackCategoryUi(id = "sleep", label = "Sleep"),
        TrackCategoryUi(id = "anxiety", label = "Anxiety"),
        TrackCategoryUi(id = "focus", label = "Focus")
    )
    SimplyAwakeRemakeTheme {
        TrackFilterBottomSheetContent(
            filterState = dummyFilterState,
            availableCategories = dummyCategories,
            onSortSelected = {},
            onStateFilterToggled = {},
            onDurationBucketSelected = {},
            onCategoryToggled = {},
            onReset = {},
            onCancel = {},
            onApply = {},
        )
    }
}
