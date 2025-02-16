package com.simplyawakeremake.data.common

import io.mockk.every
import io.mockk.mockk
import io.reactivex.rxjava3.core.Single
import org.junit.Before
import org.junit.Test

class DataRepositoryTest {

    class TestDataRepository(
        override val fetchAllCall: () -> Single<List<TestDTO>>,
        override val saver: DataSaver<TestDB>,
        override val dtoToDbMapper: (TestDTO) -> TestDB,
        override val dbToUiModelMapper: (TestDB) -> TestUiModel
    ) : DataRepository<TestUiModel, TestDTO, TestDB>()

    data class TestDTO(val id: String, val name: String)
    data class TestDB(val id: String, val name: String)
    data class TestUiModel(val id: String, val name: String)

    private lateinit var repository: TestDataRepository
    private lateinit var mockSaver: DataSaver<TestDB>
    private lateinit var mockFetchAllCall: () -> Single<List<TestDTO>>

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
    fun `getAll() returns cached data first then returns updated data from remote data source`() {
        val oldCachedData = listOf(TestDB("1", "Old Cached Track"))
        val newCachedData = listOf(TestDB("2", "New Remote Track"))
        val remoteData = listOf(TestDTO("2", "New Remote Track"))

        // First the old cached data is returned, then the new data is fetched and stored, and then new data is available from cache
        every { mockSaver.loadAll() } returnsMany listOf(
            Single.just(oldCachedData),
            Single.just(newCachedData)
        )
        every { mockFetchAllCall() } returns Single.just(remoteData)

        val testObserver = repository.getAll().test()

        testObserver.assertValueAt(0) {
            it is ResultState.Success && it.data == listOf(TestUiModel("1", "Old Cached Track"))
        }
        testObserver.assertValueAt(1) {
            it is ResultState.Success && it.data == listOf(TestUiModel("2", "New Remote Track"))
        }
    }

    @Test
    fun `getAll() fetches remote data when cache is empty`() {
        every { mockSaver.loadAll() } returnsMany listOf(
            Single.just(emptyList()),
            Single.just(listOf(TestDB("1", "Remote Track Saved")))
        )
        every { mockFetchAllCall() } returns Single.just(listOf(TestDTO("1", "Remote Track")))

        val testObserver = repository.getAll().test()

        testObserver.assertValueAt(1) {
            it is ResultState.Success && it.data == listOf(TestUiModel("1", "Remote Track Saved"))
        }
    }

    @Test
    fun `getAll() returns cached data if remote fetch fails`() {
        val cachedData = listOf(TestDB("1", "Cached Track"))
        every { mockSaver.loadAll() } returns Single.just(cachedData)
        every { mockFetchAllCall() } returns Single.error(RuntimeException("Network Error"))

        val testObserver = repository.getAll().test()

        testObserver.assertValueAt(0) {
            it is ResultState.Success && it.data == listOf(TestUiModel("1", "Cached Track"))
        }
    }

    @Test
    fun `getAll() returns error if cache is empty and remote fetch fails`() {
        every { mockSaver.loadAll() } returns Single.just(emptyList())
        every { mockFetchAllCall() } returns Single.error(RuntimeException("Network Error"))

        val testObserver = repository.getAll().test()

        testObserver.assertValueAt(1) { it is ResultState.Error }
    }
}
