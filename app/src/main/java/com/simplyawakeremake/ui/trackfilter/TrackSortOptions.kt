package com.simplyawakeremake.ui.trackfilter

import com.simplyawakeremake.R

enum class TrackSortOption {
    TITLE_ASC,
    DURATION_ASC,
    RELEASE_ORDINAL,
    MOST_PLAYED,
    AUDIO_QUALITY_DESC
}

fun TrackSortOption.toStringRes(): Int =
    when (this) {
        TrackSortOption.RELEASE_ORDINAL -> R.string.track_filter_bottom_sheet_sort_option_release
        TrackSortOption.TITLE_ASC -> R.string.track_filter_bottom_sheet_sort_option_title
        TrackSortOption.DURATION_ASC -> R.string.track_filter_bottom_sheet_sort_option_duration
        TrackSortOption.MOST_PLAYED -> R.string.track_filter_bottom_sheet_sort_option_most_played
        TrackSortOption.AUDIO_QUALITY_DESC -> R.string.track_filter_bottom_sheet_sort_option_audio_quality
    }
