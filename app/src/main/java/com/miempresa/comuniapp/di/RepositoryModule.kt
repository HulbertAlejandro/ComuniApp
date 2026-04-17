package com.miempresa.comuniapp.di

import com.miempresa.comuniapp.data.repository.memory.AttendanceRepositoryImpl
import com.miempresa.comuniapp.data.repository.memory.CommentRepositoryImpl
import com.miempresa.comuniapp.data.repository.memory.EventRepositoryImpl
import com.miempresa.comuniapp.data.repository.memory.UserRepositoryImpl
import com.miempresa.comuniapp.domain.repository.AttendanceRepository
import com.miempresa.comuniapp.domain.repository.CommentRepository
import com.miempresa.comuniapp.domain.repository.EventRepository
import com.miempresa.comuniapp.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        userRepositoryImpl: UserRepositoryImpl
    ): UserRepository

    @Binds
    @Singleton
    abstract fun bindEventRepository(
        eventRepositoryImpl: EventRepositoryImpl
    ): EventRepository

    @Binds
    @Singleton
    abstract fun bindAttendanceRepository(
        attendanceRepositoryImpl: AttendanceRepositoryImpl
    ): AttendanceRepository

    @Binds
    @Singleton
    abstract fun bindCommentRepository(
        commentRepositoryImpl: CommentRepositoryImpl
    ): CommentRepository
}
