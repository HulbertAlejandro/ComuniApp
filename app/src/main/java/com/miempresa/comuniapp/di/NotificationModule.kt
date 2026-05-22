package com.miempresa.comuniapp.di

import com.miempresa.comuniapp.data.repository.remote.NotificationRepositoryImpl
import com.miempresa.comuniapp.domain.repository.NotificationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo de Hilt que vincula la interfaz [NotificationRepository]
 * con su implementación de Firestore [NotificationRepositoryImpl].
 *
 * Separado de [RepositoryModule] para mantener la modularidad:
 * el sistema de notificaciones puede evolucionar independientemente.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationModule {

    /**
     * Vincula [NotificationRepositoryImpl] con la interfaz [NotificationRepository].
     * Hilt inyectará esta implementación en el [MyFirebaseMessagingService]
     * y en el [NotificationViewModel].
     */
    @Binds
    @Singleton
    abstract fun bindNotificationRepository(
        impl: NotificationRepositoryImpl
    ): NotificationRepository
}