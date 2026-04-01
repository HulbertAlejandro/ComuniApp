package com.miempresa.comuniapp.di

import com.miempresa.comuniapp.data.repository.EventRepositoryImpl
import com.miempresa.comuniapp.data.repository.UserRepositoryImpl
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
}
