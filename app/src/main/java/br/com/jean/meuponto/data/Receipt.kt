package br.com.jean.meuponto.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import br.com.jean.meuponto.domain.DayOccurrence

/**
 * Um comprovante de ponto digitalizado.
 *
 * Modelo atual usa **texto** para data e horários (`"dd/MM/yyyy"` e `"HH:mm"`), porque é assim que o
 * OCR entrega e porque a UI edita diretamente. A validação/normalização é feita por
 * [br.com.jean.meuponto.domain.WorkStatsCalculator] antes de qualquer cálculo.
 *
 * Os três campos de horário existem porque o comprovante pode descrever:
 * - entrada e saída no mesmo registro -> [entryTime] + [exitTime];
 * - apenas uma batida -> só [entryTime] **ou** só [exitTime];
 * - um horário solto sem tipo -> [workTime].
 *
 * [occurrence] resolve o comprovante sem jornada completa (esquecimento, falta, folga ou atestado).
 * Nulo significa comprovante normal, que entra no pareamento de batidas.
 *
 * Migrar esses campos para `LocalDate`/`LocalTime` está no roadmap do padrão de engenharia (ENG-4):
 * é a refatoração mais invasiva e exige conversores do Room.
 */
@Entity(tableName = "receipts")
data class Receipt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val capturedAt: Long,
    val workDate: String,
    val workTime: String,
    val imagePath: String,
    val recognizedText: String,
    val entryTime: String? = null,
    val exitTime: String? = null,
    val needsReview: Boolean = false,
    val occurrence: DayOccurrence? = null
)
