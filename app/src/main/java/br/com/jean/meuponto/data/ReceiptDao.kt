package br.com.jean.meuponto.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReceiptDao {
    @Query("SELECT * FROM receipts ORDER BY workDate DESC, capturedAt DESC")
    fun observeAll(): Flow<List<Receipt>>

    @Query("SELECT * FROM receipts WHERE workDate LIKE '%' || :query || '%' OR workTime LIKE '%' || :query || '%' OR recognizedText LIKE '%' || :query || '%' ORDER BY workDate DESC")
    fun search(query: String): Flow<List<Receipt>>

    @Insert
    suspend fun insert(receipt: Receipt): Long

    @Query("DELETE FROM receipts WHERE id = :id")
    suspend fun delete(id: Long)
}
