package com.simplyawakeremake.ui.trackfilter

data class TrackFilterState(
    val sortOption: TrackSortOption = TrackSortOption.RELEASE_ORDINAL,
    val stateFilters: Set<TrackStateFilter> = emptySet(),
    val durationBucket: DurationBucket? = null,
    val selectedCategoryIds: Set<String> = emptySet()
)