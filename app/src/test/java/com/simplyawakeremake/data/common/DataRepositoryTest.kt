package com.simplyawakeremake.data.common

import io.mockk.coEvery
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class DataRepositoryTest {

    class TestDataRepository(
        override val fetchAllCall: suspend () -> List<TestDTO>,
        override val saver: DataSaver<TestDB>,
        override val dtoToDbMapper: (TestDTO) -> TestDB,
        override val dbToUiModelMapper: (TestDB) -> TestUiModel
    ) : DataRepository<TestUiModel, TestDTO, TestDB>()

    data class TestDTO(val id: String, val name: String)
    data class TestDB(val id: String, val name: String)
    data class TestUiModel(val id: String, val name: String)

    private lateinit var repository: TestDataRepository
    private lateinit var mockSaver: DataSaver<TestDB>
    private lateinit var mockFetchAllCall: suspend () -> List<TestDTO>

    @Before
    fun setup() {
        mockSaver = mockk(relaxed = true)
        mockFetchAllCall = mockk()

        repository = TestDataRepository(
            fetchAllCall = mockFetchAllCall,
            saver = mockSaver,
            dtoToDbMapper = { dto -> TestDB(dto.id, dto.name) },
            dbToUiModelMapper = { db -> TestUiModel(db.id, db.name) }
        )
    }

    @Test
    fun `getAll() returns cached data first then updates from remote source`() = runTest {
        val oldCachedData = listOf(TestDB("1", "Old Cached Track"))
        val newCachedData = listOf(TestDB("2", "New Remote Track"))
        val remoteData = listOf(TestDTO("2", "New Remote Track"))

        coEvery { mockSaver.loadAll() } coAnswers {
            oldCachedData // First call: returns old data
        } andThen {
            newCachedData // After remote fetch, returns updated cache
        }

        coEvery { mockFetchAllCall() } returns remoteData

        val emittedStates = mutableListOf<ResultState<List<TestUiModel>>>()
        repository.getAll().toList(emittedStates)

        assertEquals(
            ResultState.Success(listOf(TestUiModel("1", "Old Cached Track"))),
            emittedStates[0]
        )
        assertEquals(
            ResultState.Success(listOf(TestUiModel("2", "New Remote Track"))),
            emittedStates[1]
        )
    }

    @Test
    fun `getAll() fetches remote data when cache is empty`() = runTest {
        coEvery { mockSaver.loadAll() } coAnswers {
            emptyList()
        } andThen {
            listOf(TestDB("1", "Remote Track Saved"))
        }
        coEvery { mockFetchAllCall() } returns listOf(TestDTO("1", "Remote Track"))

        val emittedStates = mutableListOf<ResultState<List<TestUiModel>>>()
        repository.getAll().toList(emittedStates)

        assertEquals(
            ResultState.Success(listOf(TestUiModel("1", "Remote Track"))),
            emittedStates[1]
        )
    }

    @Test
    fun `getAll() returns cached data if remote fetch fails`() = runTest {
        val cachedData = listOf(TestDB("1", "Cached Track"))

        coEvery { mockSaver.loadAll() } returns cachedData
        coEvery { mockFetchAllCall() } throws RuntimeException("Network Error")

        val emittedStates = mutableListOf<ResultState<List<TestUiModel>>>()
        repository.getAll().toList(emittedStates)

        assertEquals(
            ResultState.Success(listOf(TestUiModel("1", "Cached Track"))),
            emittedStates[0]
        )
    }

    @Test
    fun `getAll() returns error if cache is empty and remote fetch fails`() = runTest {
        coEvery { mockSaver.loadAll() } returns emptyList()
        coEvery { mockFetchAllCall() } throws RuntimeException("Network Error")

        val emittedStates = mutableListOf<ResultState<List<TestUiModel>>>()
        repository.getAll().toList(emittedStates)

        assert(emittedStates[1] is ResultState.Error)
    }
}
