package com.simplyawakeremake.viewmodel

import android.app.Application
import androidx.compose.ui.graphics.vector.ImageVector
import com.simplyawakeremake.ui.ToolbarAction
import com.simplyawakeremake.ui.ToolbarConfig
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any

@ExperimentalCoroutinesApi
class MainViewModelTest {

    private lateinit var viewModel: MainViewModel

    @Before
    fun setup() {
        val application = mockk<Application>(relaxed = true)
        viewModel = MainViewModel(application)
    }

    @Test
    fun `test default toolbar config is correct`() = runTest {
        // When
        val initialConfig = viewModel.toolbarConfig.value

        // Then
        assertEquals(ToolbarConfig(actions = emptyList(), showToolbar = true), initialConfig)
    }

    @Test
    fun `test updateToolbar updates the state correctly`() = runTest {
        // Given
        val actionIcon = mockk<ImageVector>() // Mock an actual Int value if necessary
        val newAction = ToolbarAction(icon = actionIcon, contentDescription  = "Action", onClick = { })
        val newConfig = ToolbarConfig(actions = listOf(newAction), showToolbar = false)

        // When
        viewModel.updateToolbar(newConfig)

        // Then
        assertEquals(newConfig, viewModel.toolbarConfig.value)
    }


    @Test
    fun `test updateToolbar with null keeps the previous state`() = runTest {
        // Given
        val originalConfig = viewModel.toolbarConfig.value

        // When
        viewModel.updateToolbar(null)

        // Then
        assertEquals(originalConfig, viewModel.toolbarConfig.value)
    }
}
