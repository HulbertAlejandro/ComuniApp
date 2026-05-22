package com.miempresa.comuniapp.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance().apply {
            firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(
                    PersistentCacheSettings.newBuilder()
                        .setSizeBytes(50L * 1024L * 1024L)
                        .build()
                )
                .build()
        }
    }

    /**
     * Provee una instancia singleton de [FirebaseStorage].
     *
     * Firebase Storage organiza los archivos en un árbol de "rutas" igual
     * que un sistema de archivos. Usaremos:
     *   profile_pictures/{userId}.jpg   → fotos de perfil
     *   event_images/{eventId}/{uuid}.jpg → imágenes de eventos
     *
     * El bucket se configura automáticamente desde google-services.json;
     * no necesitas pasar la URL manualmente salvo que tengas múltiples buckets.
     */
    @Provides
    @Singleton
    fun provideStorage(): FirebaseStorage = FirebaseStorage.getInstance()
}