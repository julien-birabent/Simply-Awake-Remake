package com.simplyawakeremake.ui.trackfilter

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class TrackFilterViewModel : ViewModel() {

    private val _filterState = MutableStateFlow(TrackFilter())
    val filterState: StateFlow<TrackFilter> = _filterState

    fun setInitialFilterState(initial: TrackFilter) {
        _filterState.value = initial
    }

    fun onSortOptionSelected(option: TrackSortOption) {
        _filterState.update { it.copy(sortOption = option) }
    }

    fun onToggleStateFilter(filter: TrackStateFilter) {
        _filterState.update { current ->
            val currentSet = current.stateFilters
            val newSet = currentSet.toMutableSet().apply {
                if (contains(filter)) remove(filter) else add(filter)
            }
            current.copy(stateFilters = newSet)
        }
    }

    fun onDurationBucketSelected(bucket: DurationBucket?) {
        _filterState.update { it.copy(durationBucket = bucket) }
    }

    fun onToggleCategory(categoryId: String) {
        _filterState.update { current ->
            val currentSet = current.selectedCategoryIds
            val newSet = currentSet.toMutableSet().apply {
                if (contains(categoryId)) remove(categoryId) else add(categoryId)
            }
            current.copy(selectedCategoryIds = newSet)
        }
    }

    fun reset() {
        _filterState.value = TrackFilter()
    }
}
