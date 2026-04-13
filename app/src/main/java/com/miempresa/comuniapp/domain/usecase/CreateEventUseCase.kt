package com.miempresa.comuniapp.domain.usecase

import com.miempresa.comuniapp.domain.model.Event
import com.miempresa.comuniapp.domain.repository.EventRepository
import javax.inject.Inject

class CreateEventUseCase @Inject constructor(
    private val eventRepository: EventRepository
) {
    /**
     * Registra un nuevo evento en estado PENDIENTE.
     * La asignación de puntos se realizará únicamente tras la aprobación del moderador.
     */
    suspend operator fun invoke(event: Event) {
        eventRepository.save(event)
    }
}
