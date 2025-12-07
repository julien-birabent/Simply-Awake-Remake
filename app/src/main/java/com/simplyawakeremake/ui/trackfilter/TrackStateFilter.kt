package com.simplyawakeremake.ui.trackfilter

sealed class TrackStateFilter {
    data object Downloaded : TrackStateFilter()
    data object Favorite : TrackStateFilter()
    data object NeverPlayed : TrackStateFilter()
}