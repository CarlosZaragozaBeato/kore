package com.kore.android.ui

data class MockPlanDay(
    val day: String,
    val date: String,
    val type: String,
    val title: String,
    val distance: String,
    val status: MockDayStatus,
)

enum class MockDayStatus { Completed, Today, Planned, Rest }

object MockData {
    val week = listOf(
        MockPlanDay("L", "16", "RECOVERY", "Rodaje regenerativo", "10 km", MockDayStatus.Completed),
        MockPlanDay("M", "17", "SERIES", "6 × 1 km", "14 km", MockDayStatus.Completed),
        MockPlanDay("X", "18", "FÁCIL", "Rodaje + strides", "12 km", MockDayStatus.Today),
        MockPlanDay("J", "19", "UMBRAL", "3 × 10 min", "15 km", MockDayStatus.Planned),
        MockPlanDay("V", "20", "DESCANSO", "Descanso", "—", MockDayStatus.Rest),
        MockPlanDay("S", "21", "FÁCIL", "Rodaje aeróbico", "13 km", MockDayStatus.Planned),
        MockPlanDay("D", "22", "TIRADA", "Progresiva", "20 km", MockDayStatus.Planned),
    )

    val fitness = listOf(
        "Base aeróbica" to 84,
        "Umbral" to 76,
        "Velocidad" to 72,
        "Resistencia específica" to 79,
        "Continuidad" to 88,
    )
}
