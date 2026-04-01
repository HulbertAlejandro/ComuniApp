package com.miempresa.comuniapp.domain.repository

import com.miempresa.comuniapp.domain.model.Event
import kotlinx.coroutines.flow.StateFlow

interface EventRepository {

    val events: StateFlow<List<Event>>

    fun save(event: Event)

    fun findById(id: String): Event?

    fun update(event: Event)

    fun delete(id: String)
}