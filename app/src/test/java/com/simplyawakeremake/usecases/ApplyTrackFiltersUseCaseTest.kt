package com.simplyawakeremake.usecases

import com.simplyawakeremake.data.audioquality.AudioQualityLevel
import com.simplyawakeremake.data.download.track.TrackFileManager
import com.simplyawakeremake.data.track.Track
import com.simplyawakeremake.ui.trackfilter.TrackFilter
import com.simplyawakeremake.ui.trackfilter.TrackSortOption
import com.simplyawakeremake.ui.trackfilter.TrackStateFilter
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class ApplyTrackFiltersUseCaseTest {

    private val useCase = ApplyTrackFiltersUseCase(trackFileManager = mockk(relaxed = true))

    private val good = track("001 Good", AudioQualityLevel.GOOD)
    private val fair = track("002 Fair", AudioQualityLevel.FAIR)
    private val poor = track("003 Poor", AudioQualityLevel.POOR)
    private val unrated = track("004 Unrated", null)

    @Test
    fun `GIVEN hide poor audio filter WHEN applied THEN poor tracks are removed and unrated ones kept`() {
        // GIVEN
        val filter = TrackFilter(stateFilters = setOf(TrackStateFilter.HidePoorAudio))

        // WHEN
        val result = useCase(listOf(good, fair, poor, unrated), filter)

        // THEN
        assertEquals(listOf(good, fair, unrated), result)
    }

    @Test
    fun `GIVEN no state filter WHEN applied THEN poor tracks stay`() {
        // GIVEN
        val filter = TrackFilter()

        // WHEN
        val result = useCase(listOf(poor, good), filter)

        // THEN
        assertEquals(listOf(poor, good), result)
    }

    @Test
    fun `GIVEN audio quality sort WHEN applied THEN good comes first and unrated last`() {
        // GIVEN
        val filter = TrackFilter(sortOption = TrackSortOption.AUDIO_QUALITY_DESC)

        // WHEN
        val result = useCase(listOf(unrated, poor, fair, good), filter)

        // THEN
        assertEquals(listOf(good, fair, poor, unrated), result)
    }

    private fun track(name: String, audioQuality: AudioQualityLevel?) = Track(
        id = name,
        name = name,
        lengthInSeconds = 600,
        tagString = "",
        duration = "10:00",
        season = 1,
        year = 2024,
        audioQuality = audioQuality,
    )
}
