package com.example.projektiop.domain.models

enum class Language(val tag: String) {
    SYSTEM("system"),
    ENGLISH("en"),
    POLISH("pl");

    companion object {
        fun fromTag(tag: String?): Language =
            Language.entries.firstOrNull { it.tag == tag } ?: SYSTEM
    }
}