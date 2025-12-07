package com.simplyawakeremake.ui.trackfilter

data class TrackFilter(
    val sortOption: TrackSortOption = TrackSortOption.RELEASE_ORDINAL,
    val stateFilters: Set<TrackStateFilter> = emptySet(),
    val durationBucket: DurationBucket? = null,
    val selectedCategoryIds: Set<String> = emptySet()
)

fun TrackFilter.hasActiveFilters(): Boolean =
    stateFilters.isNotEmpty() || durationBucket != null || selectedCategoryIds.isNotEmpty()
