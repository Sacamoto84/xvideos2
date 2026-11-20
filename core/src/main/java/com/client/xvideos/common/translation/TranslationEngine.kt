package com.client.xvideos.common.translation

enum class TranslationEngine {
    NONE,
    ML_KIT;

    companion object {
        fun fromStoredValue(value: String): TranslationEngine =
            entries.firstOrNull { it.name == value } ?: NONE
    }
}
