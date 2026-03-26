package com.miempresa.comuniapp.di

import com.miempresa.comuniapp.data.repository.UserRepositoryImpl
import com.miempresa.comuniapp.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds // Indica a Hilt que esta función vincula una implementación a una interfaz
    @Singleton
    abstract fun bindUserRepository(
        userRepositoryImpl: UserRepositoryImpl
    ): UserRepository // Vincula UserRepositoryImpl con UserRepository
}