package com.miempresa.comuniapp.di

import com.miempresa.comuniapp.data.repository.remote.AttendanceRepositoryImpl
import com.miempresa.comuniapp.data.repository.remote.CommentRepositoryImpl
import com.miempresa.comuniapp.data.repository.remote.EventRepositoryImpl
import com.miempresa.comuniapp.data.repository.remote.UserRepositoryImpl
import com.miempresa.comuniapp.domain.repository.AttendanceRepository
import com.miempresa.comuniapp.domain.repository.CommentRepository
import com.miempresa.comuniapp.domain.repository.EventRepository
import com.miempresa.comuniapp.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo de Hilt que vincula cada interfaz de repositorio con su
 * implementación concreta en la capa de datos (Firestore).
 *
 * El paquete cambió de [data.repository.memory] a [data.repository.remote]
 * para reflejar que las implementaciones ya no son en memoria sino
 * persistentes en Firebase Firestore.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /** Vincula [UserRepositoryImpl] (Firestore) con la interfaz [UserRepository]. */
    @Binds @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    /** Vincula [EventRepositoryImpl] (Firestore) con la interfaz [EventRepository]. */
    @Binds @Singleton
    abstract fun bindEventRepository(impl: EventRepositoryImpl): EventRepository

    /** Vincula [AttendanceRepositoryImpl] (Firestore) con la interfaz [AttendanceRepository]. */
    @Binds @Singleton
    abstract fun bindAttendanceRepository(impl: AttendanceRepositoryImpl): AttendanceRepository

    /** Vincula [CommentRepositoryImpl] (Firestore) con la interfaz [CommentRepository]. */
    @Binds @Singleton
    abstract fun bindCommentRepository(impl: CommentRepositoryImpl): CommentRepository
}