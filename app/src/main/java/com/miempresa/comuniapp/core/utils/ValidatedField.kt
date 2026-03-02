package com.miempresa.comuniapp.core.utils

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class ValidatedField<T>(
    private val initialValue: T,
    private val validate: (T) -> String?
) {

    var value by mutableStateOf(initialValue)
        private set

    var showError by mutableStateOf(false)
        private set

    val error: String?
        get() = if (showError) validate(value) else null

    val isValid: Boolean
        get() = validate(value) == null

    fun onChange(newValue: T) {
        value = newValue
        showError = true
    }

    fun reset() {
        value = initialValue
        showError = false
    }
}