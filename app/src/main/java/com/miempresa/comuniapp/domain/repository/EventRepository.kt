package com.miempresa.comuniapp.domain.repository

import com.miempresa.comuniapp.domain.model.Event
import kotlinx.coroutines.flow.StateFlow

interface EventRepository {
    val events: StateFlow<List<Event>>
    fun getEventById(id: String): Event?
}
