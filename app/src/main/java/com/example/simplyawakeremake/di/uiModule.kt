package com.example.simplyawakeremake.di

import com.example.simplyawakeremake.viewmodel.TrackListViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val uiModule = module {

    viewModel { TrackListViewModel() }
}