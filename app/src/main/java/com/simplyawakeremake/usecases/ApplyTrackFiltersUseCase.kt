package com.simplyawakeremake.usecases

import com.simplyawakeremake.data.download.track.TrackFileManager
import com.simplyawakeremake.data.track.Track
import com.simplyawakeremake.data.track.categories
import com.simplyawakeremake.data.track.rawDurationSeconds
import com.simplyawakeremake.ui.trackfilter.DurationBucket
import com.simplyawakeremake.ui.trackfilter.TrackFilterState
import com.simplyawakeremake.ui.trackfilter.TrackSortOption
import com.simplyawakeremake.ui.trackfilter.TrackStateFilter

fun interface TrackListFilter {
    fun apply(tracks: List<Track>, filter: TrackFilterState): List<Track>
}

class ApplyTrackFiltersUseCase(
    trackFileManager: TrackFileManager
) {

    private val filters: List<TrackListFilter> = listOf(
        StateTrackListFilter(
            isTrackDownloaded = { trackId -> trackFileManager.isTrackDownloaded(trackId) }
        ),
        DurationTrackListFilter(),
        CategoryTrackListFilter(),
    )

    operator fun invoke(
        tracks: List<Track>,
        filter: TrackFilterState
    ): List<Track> {
        val filtered = filters.fold(tracks) { current, strategy ->
            strategy.apply(current, filter)
        }

        return applySort(filtered, filter.sortOption)
    }

    private fun applySort(
        tracks: List<Track>,
        sortOption: TrackSortOption
    ): List<Track> {
        return when (sortOption) {
            TrackSortOption.TITLE_ASC ->
                tracks.sortedBy { it.displayName.lowercase() }

            TrackSortOption.DURATION_ASC ->
                tracks.sortedBy { it.rawDurationSeconds() }

            TrackSortOption.RELEASE_ORDINAL -> {
                tracks.sortedBy { it.ordinal }
            }
        }
    }
}

private class StateTrackListFilter(
    private val isTrackDownloaded: (trackId: String) -> Boolean
) : TrackListFilter {

    override fun apply(tracks: List<Track>, filter: TrackFilterState): List<Track> {
        val stateFilters = filter.stateFilters
        if (stateFilters.isEmpty()) return tracks

        var result = tracks

        if (TrackStateFilter.Downloaded in stateFilters) {
            result = result.filter { track ->
                isTrackDownloaded(track.id)
            }
        }

        if (TrackStateFilter.Favorite in stateFilters) {
            result = result.filter { track -> track.isFavorite }
        }

        if (TrackStateFilter.NeverPlayed in stateFilters) {
            result = result.filter { track -> track.playCount == 0 }
        }

        return result
    }
}

private class DurationTrackListFilter : TrackListFilter {

    override fun apply(tracks: List<Track>, filter: TrackFilterState): List<Track> {
        val durationBucket: DurationBucket = filter.durationBucket ?: return tracks

        return tracks.filter { track ->
            val minutes = track.rawDurationSeconds() / 60
            minutes in durationBucket.minMinutesInclusive until durationBucket.maxMinutesExclusive
        }
    }
}

private class CategoryTrackListFilter : TrackListFilter {

    override fun apply(tracks: List<Track>, filter: TrackFilterState): List<Track> {
        val selectedCategoryIds = filter.selectedCategoryIds
        if (selectedCategoryIds.isEmpty()) return tracks

        return tracks.filter { track ->
            val trackCategories = track.categories()
            trackCategories.all { it in selectedCategoryIds }
        }
    }
}
