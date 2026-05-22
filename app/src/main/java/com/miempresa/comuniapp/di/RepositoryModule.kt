package com.miempresa.comuniapp.di

import com.miempresa.comuniapp.data.repository.remote.AttendanceRepositoryImpl
import com.miempresa.comuniapp.data.repository.remote.CommentRepositoryImpl
import com.miempresa.comuniapp.data.repository.remote.EventRepositoryImpl
import com.miempresa.comuniapp.data.repository.remote.StorageRepositoryImpl
import com.miempresa.comuniapp.data.repository.remote.UserRepositoryImpl
import com.miempresa.comuniapp.data.service.CategorySuggestionServiceImpl
import com.miempresa.comuniapp.domain.repository.AttendanceRepository
import com.miempresa.comuniapp.domain.repository.CommentRepository
import com.miempresa.comuniapp.domain.repository.EventRepository
import com.miempresa.comuniapp.domain.repository.StorageRepository
import com.miempresa.comuniapp.domain.repository.UserRepository
import com.miempresa.comuniapp.domain.service.CategorySuggestionService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo de Hilt que vincula cada interfaz con su implementación de Firestore.
 * [UserRepositoryImpl] ahora recibe tanto [FirebaseAuth] como [FirebaseFirestore],
 * ambos provistos por [FirebaseModule].
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds @Singleton
    abstract fun bindEventRepository(impl: EventRepositoryImpl): EventRepository

    @Binds @Singleton
    abstract fun bindAttendanceRepository(impl: AttendanceRepositoryImpl): AttendanceRepository

    @Binds @Singleton
    abstract fun bindCommentRepository(impl: CommentRepositoryImpl): CommentRepository

    @Binds @Singleton
    abstract fun bindStorageRepository(impl: StorageRepositoryImpl): StorageRepository
}