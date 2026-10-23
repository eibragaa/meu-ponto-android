package br.com.jean.meuponto.domain

/**
 * Ocorrência que resolve um comprovante sem jornada completa.
 *
 * Um comprovante com ocorrência sai do pareamento de batidas: não soma horas e não deixa o dia
 * pendente. Quem registra a ocorrência é o usuário, na folha de edição.
 */
enum class DayOccurrence {
    /** Só uma batida foi registrada e a outra foi esquecida. As horas do dia ficam zeradas. */
    FORGOT_PUNCH,

    /** Falta: o dia de trabalho não aconteceu e não há horário. */
    ABSENCE,

    /** Folga: dia de folga sem horário. */
    DAY_OFF,

    /** Atestado médico: dia afastado sem horário. */
    MEDICAL_LEAVE
}

/** Verdadeiro para ocorrências que cobrem o dia inteiro (falta, folga, atestado), sem horário. */
val DayOccurrence.isDayAbsence: Boolean get() = this != DayOccurrence.FORGOT_PUNCH
