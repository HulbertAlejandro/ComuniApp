package com.miempresa.comuniapp.di

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import com.google.firebase.firestore.PersistentCacheSettings
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo de Hilt que provee la instancia de Firebase Firestore
 * con persistencia en disco habilitada para soporte offline.
 */
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    /**
     * Provee una instancia singleton de [FirebaseFirestore] configurada
     * con caché persistente en disco.
     *
     * ── ¿Qué hace [PersistentCacheSettings]? ───────────────────────────
     * Guarda una copia local de los documentos leídos en el almacenamiento
     * interno del dispositivo (no en RAM). Esto permite:
     *
     * 1. Leer datos sin conexión: si el usuario abre la app sin internet,
     *    Firestore sirve los últimos datos conocidos desde el disco.
     * 2. Escrituras offline: las operaciones de escritura (.set, .update,
     *    .delete) se encolan localmente y se sincronizan automáticamente
     *    cuando se restaura la conexión.
     * 3. Reducción de lecturas facturadas: los datos en caché no generan
     *    lecturas adicionales en Firestore (ahorro en el plan de pago).
     *
     * ── Tamaño de caché [setSizeBytes] ──────────────────────────────────
     * - CACHE_SIZE_UNLIMITED: sin límite (útil en desarrollo).
     * - Un número en bytes, p.ej. 100 * 1024 * 1024 = 100 MB (recomendado
     *   para producción para no consumir todo el almacenamiento del dispositivo).
     *
     * ── Alternativa: [MemoryCacheSettings] ──────────────────────────────
     * Solo guarda en RAM; se pierde al cerrar la app. Útil para apps donde
     * los datos cambian tan rápido que la caché en disco quedaría obsoleta.
     * Para ComuniApp usamos PersistentCacheSettings porque los eventos no
     * cambian con mucha frecuencia y queremos soporte offline real.
     */
    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore {
        val firestore = FirebaseFirestore.getInstance()

        val settings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(
                PersistentCacheSettings.newBuilder()
                    // 50 MB de caché en disco; ajusta según las necesidades del proyecto
                    .setSizeBytes(50L * 1024L * 1024L)
                    .build()
            )
            .build()

        firestore.firestoreSettings = settings
        return firestore
    }
}