package com.miempresa.comuniapp.di

import com.miempresa.comuniapp.core.resources.ResourceProvider
import com.miempresa.comuniapp.core.resources.ResourceProviderImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ResourceModule {

    @Binds
    @Singleton
    abstract fun bindResourceProvider(
        resourceProviderImpl: ResourceProviderImpl
    ): ResourceProvider
}

