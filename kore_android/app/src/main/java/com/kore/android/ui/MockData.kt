package com.kore.android.ui

import androidx.compose.ui.graphics.Color

data class MuseumPiece(
    val title: String,
    val date: String,
    val distance: String,
    val time: String,
    val story: String,
    val badge: String,
    val color: Color,
)

data class PersonalBest(
    val distance: String,
    val time: String,
    val pace: String,
    val date: String,
    val improvement: String,
)

data class Shoe(
    val name: String,
    val type: String,
    val acquired: String,
    val currentKm: Int,
    val limitKm: Int,
    val price: String,
    val use: String,
    val rating: Double,
    val color: Color,
    val status: String = "ACTIVA",
)

data class DemoRoute(
    val name: String,
    val location: String,
    val distanceKm: Double,
    val elevationMeters: Int,
    val surface: String,
    val note: String,
)

object MockData {
    val museum = listOf(
        MuseumPiece(
            "El día que bajé de 39'", "12 MAY 2026", "10,00 km", "38:47",
            "Una carrera perfecta: paciencia hasta el km 7 y todo lo que quedaba al final.",
            "PB 10K", Color(0xFFE15B3D),
        ),
        MuseumPiece(
            "Mi primera media", "24 NOV 2024", "21,10 km", "1:34:12",
            "El principio de todo. Llegué sin saber hasta dónde podía correr.",
            "PRIMERA VEZ", Color(0xFF356D9A),
        ),
        MuseumPiece(
            "Tirada bajo la tormenta", "18 OCT 2026", "30,40 km", "2:19:08",
            "La sesión que me enseñó que también podía seguir cuando el día se torcía.",
            "MEMORABLE", Color(0xFF4B6657),
        ),
    )

    val currentBests = listOf(
        PersonalBest("5K", "18:21", "3:40/km", "28 OCT 2026", "−19 s"),
        PersonalBest("10K", "38:47", "3:53/km", "12 MAY 2026", "−41 s"),
        PersonalBest("21,1K", "1:28:42", "4:12/km", "09 MAR 2026", "−3:18"),
        PersonalBest("42,2K", "3:14:26", "4:36/km", "17 NOV 2025", "PRIMERA"),
    )

    val shoes = listOf(
        Shoe(
            "ASICS Novablast 5", "Entrenamiento diario", "03 FEB 2026", 482, 700,
            "149,95 €", "Rodajes · Tiradas largas", 4.6, Color(0xFF5A78C7),
        ),
        Shoe(
            "Nike Vaporfly 3", "Competición", "18 MAR 2026", 128, 350,
            "259,99 €", "Series hasta 200 km · Después rodaje", 4.8, Color(0xFFE15B3D),
        ),
        Shoe(
            "Adidas Adizero SL2", "Mixta", "10 AGO 2025", 611, 650,
            "104,00 €", "Rodajes suaves", 4.1, Color(0xFF3F8B67), "OBSERVAR",
        ),
    )

    val routes = listOf(
        DemoRoute("Vuelta larga Casa de Campo", "Madrid", 24.6, 318, "Mixta", "Mi tirada larga de referencia."),
        DemoRoute("Anillo del Retiro", "Madrid", 4.3, 34, "Asfalto", "Buena para tempo y repeticiones largas."),
        DemoRoute("Subida a las antenas", "Sierra de Guadarrama", 17.8, 946, "Trail", "Ruta exigente con vistas al valle."),
    )
}
