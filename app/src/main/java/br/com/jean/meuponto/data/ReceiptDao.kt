package br.com.jean.meuponto.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Acesso à tabela `receipts`.
 *
 * Consultas de listagem ordenam pelo dia do comprovante e, dentro do dia, pelo momento da captura;
 * comprovantes sem data (OCR falhou) vão para o topo porque são os que exigem ação do usuário.
 */
@Dao
interface ReceiptDao {
    @Query(
        "SELECT * FROM receipts " +
            "ORDER BY CASE WHEN workDate = '' THEN 0 ELSE 1 END, workDate DESC, capturedAt DESC"
    )
    fun observeAll(): Flow<List<Receipt>>

    @Query(
        "SELECT * FROM receipts " +
            "WHERE workDate LIKE '%' || :query || '%' " +
            "OR workTime LIKE '%' || :query || '%' " +
            "OR IFNULL(entryTime, '') LIKE '%' || :query || '%' " +
            "OR IFNULL(exitTime, '') LIKE '%' || :query || '%' " +
            "OR recognizedText LIKE '%' || :query || '%' " +
            "ORDER BY CASE WHEN workDate = '' THEN 0 ELSE 1 END, workDate DESC, capturedAt DESC"
    )
    fun search(query: String): Flow<List<Receipt>>

    @Query("SELECT * FROM receipts ORDER BY capturedAt ASC")
    suspend fun getAll(): List<Receipt>

    @Query("SELECT * FROM receipts WHERE id = :id")
    suspend fun getById(id: Long): Receipt?

    /** Usado no restore para deduplicar sem depender do `id` antigo (que é regenerado). */
    @Query("SELECT * FROM receipts WHERE capturedAt = :capturedAt")
    suspend fun getByCapturedAt(capturedAt: Long): List<Receipt>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(receipt: Receipt): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(receipts: List<Receipt>)

    @Query(
        "UPDATE receipts SET workDate = :workDate, workTime = :workTime, " +
            "entryTime = :entryTime, exitTime = :exitTime, needsReview = :needsReview " +
            "WHERE id = :id"
    )
    suspend fun updateReceiptDetails(
        id: Long,
        workDate: String,
        workTime: String,
        entryTime: String?,
        exitTime: String?,
        needsReview: Boolean
    )

    @Query("DELETE FROM receipts WHERE id = :id")
    suspend fun delete(id: Long)
}
