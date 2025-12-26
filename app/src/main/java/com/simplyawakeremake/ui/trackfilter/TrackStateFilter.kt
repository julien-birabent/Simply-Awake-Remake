package com.simplyawakeremake.ui.trackfilter

import com.simplyawakeremake.R

sealed class TrackStateFilter {
    data object Downloaded : TrackStateFilter()
    data object Favorite : TrackStateFilter()
    data object NeverPlayed : TrackStateFilter()
}

fun TrackStateFilter.toStringRes(): Int =
    when (this) {
        TrackStateFilter.Downloaded -> R.string.track_filter_bottom_sheet_state_downloaded_only

        TrackStateFilter.Favorite -> R.string.track_filter_bottom_sheet_state_favorites

        TrackStateFilter.NeverPlayed ->
            R.string.track_filter_bottom_sheet_state_never_played

    }