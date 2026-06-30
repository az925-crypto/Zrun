package com.zaaam.Zmusic.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zaaam.Zmusic.model.entity.SearchHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchHistoryDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(history: SearchHistoryEntity): Long

    @Query("UPDATE search_history SET searchedAt = :timestamp WHERE query = :query")
    suspend fun updateTimestamp(query: String, timestamp: Long)

    @Query("SELECT * FROM search_history ORDER BY searchedAt DESC LIMIT 20")
    fun getRecentHistory(): Flow<List<SearchHistoryEntity>>

    @Query("DELETE FROM search_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM search_history")
    suspend fun clearAll()

    // FIX #15: Atomic upsert menggunakan INSERT OR REPLACE.
    // Sebelumnya: countByQuery() + insert() terpisah dalam @Transaction,
    // tapi masih bisa race karena 2 transaction concurrent bisa lolos check.
    // Sekarang: satu SQL statement yang atomik di level database engine.
    // Unique index pada kolom `query` (dari SearchHistoryEntity) menjamin
    // ON CONFLICT terpicu saat query sudah ada.
    //
    // Note: Karena INSERT OR REPLACE akan DELETE+INSERT (bukan UPDATE),
    // id bisa berubah — tapi ini acceptable karena id tidak pernah
    // dipakai sebagai foreign key di tempat lain.
    @Query("INSERT OR REPLACE INTO search_history (query, searchedAt) VALUES (:query, :timestamp)")
    suspend fun upsertSearchQuery(query: String, timestamp: Long = System.currentTimeMillis())
}
