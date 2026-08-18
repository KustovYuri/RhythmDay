package com.rhythmday.app.core.presentation

import com.rhythmday.app.core.domain.sync.DiaryRefresher
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DiaryRefreshViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun ignoresRepeatedRefreshWhileRequestIsRunning() = runTest(dispatcher) {
        val releaseRefresh = CompletableDeferred<Unit>()
        var refreshCount = 0
        val viewModel = DiaryRefreshViewModel(
            refresher = DiaryRefresher {
                refreshCount++
                releaseRefresh.await()
                Result.success(Unit)
            },
            messages = UserMessageManager()
        )

        viewModel.refresh()
        runCurrent()

        assertTrue(viewModel.isRefreshing.value)
        viewModel.refresh()
        runCurrent()
        assertEquals(1, refreshCount)

        releaseRefresh.complete(Unit)
        advanceUntilIdle()

        assertFalse(viewModel.isRefreshing.value)
    }
}
