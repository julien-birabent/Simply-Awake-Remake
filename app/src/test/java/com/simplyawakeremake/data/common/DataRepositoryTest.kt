package com.simplyawakeremake.data.common

import android.util.Log
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class DataRepositoryTest {

    data class TestDTO(val id: String, val name: String)
    data class TestDB(val id: String, val name: String)
    data class TestDomain(val id: String, val name: String)

    class TestDataRepository(
        override val fetchAllCall: suspend () -> List<TestDTO>,
        override val saver: DataSaver<TestDB>,
        override val dtoToDomainMapper: (TestDTO) -> TestDomain,
        override val domainToDbMapper: (TestDomain) -> TestDB,
        override val dbToDomainMapper: (TestDB) -> TestDomain,
    ) : DataRepository<TestDomain, TestDTO, TestDB>()

    private lateinit var repository: TestDataRepository
    private lateinit var mockSaver: DataSaver<TestDB>
    private lateinit var mockFetchAllCall: suspend () -> List<TestDTO>

    @Before
    fun setup() {
        mockSaver = mockk(relaxed = true)
        mockFetchAllCall = mockk()
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        repository = TestDataRepository(
            fetchAllCall = mockFetchAllCall,
            saver = mockSaver,
            dtoToDomainMapper = { dto -> TestDomain(dto.id, dto.name) },
            domainToDbMapper = { domain -> TestDB(domain.id, domain.name) },
            dbToDomainMapper = { db -> TestDomain(db.id, db.name) }
        )
    }

    @Test
    fun `getAll - GIVEN cached data AND remote different WHEN called THEN emits cached then remote`() = runTest {
        // GIVEN
        val oldCachedData = listOf(TestDB("1", "Old Cached Track"))
        val remoteData = listOf(TestDTO("2", "New Remote Track"))

        coEvery { mockSaver.loadAll() } returns oldCachedData
        coEvery { mockFetchAllCall() } returns remoteData

        // WHEN
        val emittedStates = mutableListOf<ResultState<List<TestDomain>>>()
        repository.getAll().toList(emittedStates)

        // THEN
        assertEquals(
            ResultState.Success(listOf(TestDomain("1", "Old Cached Track"))),
            emittedStates[0]
        )
        assertEquals(
            ResultState.Success(listOf(TestDomain("2", "New Remote Track"))),
            emittedStates[1]
        )
    }

    @Test
    fun `getAll - GIVEN empty cache WHEN remote succeeds THEN emits Loading then remote data`() = runTest {
        // GIVEN
        coEvery { mockSaver.loadAll() } returnsMany listOf(
            emptyList(),                             // initial cache
        )
        coEvery { mockFetchAllCall() } returns listOf(TestDTO("1", "Remote Track"))

        // WHEN
        val emittedStates = mutableListOf<ResultState<List<TestDomain>>>()
        repository.getAll().toList(emittedStates)

        // THEN
        // First emission: Loading (empty cache)
        assertEquals(
            ResultState.Loading<List<TestDomain>>(null),
            emittedStates[0]
        )

        // Second emission: remote data
        assertEquals(
            ResultState.Success(listOf(TestDomain("1", "Remote Track"))),
            emittedStates[1]
        )
    }

    @Test
    fun `getAll - GIVEN cached data WHEN remote fails THEN emits cached twice`() = runTest {
        // GIVEN
        val cachedData = listOf(TestDB("1", "Cached Track"))

        coEvery { mockSaver.loadAll() } returns cachedData
        coEvery { mockFetchAllCall() } throws RuntimeException("Network Error")

        // WHEN
        val emittedStates = mutableListOf<ResultState<List<TestDomain>>>()
        repository.getAll().toList(emittedStates)

        // THEN
        // 1st emission: cached
        assertEquals(
            ResultState.Success(listOf(TestDomain("1", "Cached Track"))),
            emittedStates[0]
        )
        // 2nd emission: fallback = cached again (per new implementation)
        assertEquals(
            ResultState.Success(listOf(TestDomain("1", "Cached Track"))),
            emittedStates[1]
        )
    }

    @Test
    fun `getAll - GIVEN empty cache WHEN remote fails THEN emits only Loading`() = runTest {
        // GIVEN
        coEvery { mockSaver.loadAll() } returns emptyList()
        coEvery { mockFetchAllCall() } throws RuntimeException("Network Error")

        // WHEN
        val emittedStates = mutableListOf<ResultState<List<TestDomain>>>()
        repository.getAll().toList(emittedStates)

        // THEN
        // New behavior: we only get the initial Loading, error is logged but not emitted
        assertEquals(1, emittedStates.size)
        assertEquals(
            ResultState.Loading<List<TestDomain>>(null),
            emittedStates[0]
        )
    }
}
