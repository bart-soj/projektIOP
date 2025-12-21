package com.example.projektiop.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.projektiop.R

@Composable
fun translateInterestName(rawName: String): String {
    return when (rawName) {
        "Cyberbezpieczeństwo" -> stringResource(R.string.interest_cybersecurity)
        "Czytanie Książek" -> stringResource(R.string.interest_reading)
        "Gry Komputerowe" -> stringResource(R.string.interest_gaming)
        "Górskie Wędrówki" -> stringResource(R.string.interest_mountain_hiking)
        "Kino Niezależne" -> stringResource(R.string.interest_indie_cinema)
        "Kolarstwo" -> stringResource(R.string.interest_cycling)
        "Muzyka Elektroniczna" -> stringResource(R.string.interest_electronic_music)
        "Piłka Nożna" -> stringResource(R.string.interest_football)
        "Podróże z Plecakiem" -> stringResource(R.string.interest_backpacking)
        "Programowanie" -> stringResource(R.string.interest_programming)
        "Siłownia i Fitness" -> stringResource(R.string.interest_gym_fitness)
        else -> rawName
    }
}