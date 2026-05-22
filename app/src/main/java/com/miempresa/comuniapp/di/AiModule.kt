// di/AiModule.kt
package com.miempresa.comuniapp.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.miempresa.comuniapp.BuildConfig
import com.miempresa.comuniapp.data.remote.api.OpenRouterApiService
import com.miempresa.comuniapp.data.service.CategorySuggestionServiceImpl
import com.miempresa.comuniapp.data.service.HeuristicAiCategorizer
import com.miempresa.comuniapp.data.service.OpenRouterAiCategorizer
import com.miempresa.comuniapp.domain.service.AiCategorizer
import com.miempresa.comuniapp.domain.service.CategorySuggestionService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AiModule {

    // ── Credenciales ─────────────────────────────────────────────────────

    @Provides
    @Named("openrouter_api_key")
    fun provideOpenRouterApiKey(): String = BuildConfig.OPENROUTER_API_KEY

    // ── Red: OkHttp con headers de autenticación ─────────────────────────

    /**
     * OkHttpClient dedicado para OpenRouter.
     * Los headers de autenticación van aquí (interceptor) para mantener
     * [OpenRouterApiService] limpio y sin conocer la API key.
     *
     * Usamos @Named para no colisionar con un posible OkHttpClient global.
     */
    @Provides
    @Singleton
    @Named("openrouter_okhttp")
    fun provideOpenRouterOkHttpClient(
        @Named("openrouter_api_key") apiKey: String
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(logging)
            // Interceptor de autenticación: inyecta headers en CADA petición
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("Authorization",  "Bearer $apiKey")
                    .header("Content-Type",   "application/json")
                    // Headers opcionales de atribución (aparece en el dashboard de OpenRouter)
                    .header("HTTP-Referer",   "https://comuniapp.miempresa.com")
                    .header("X-Title",        "ComuniApp")
                    .build()
                chain.proceed(request)
            }
            .build()
    }

    // ── Red: Retrofit para OpenRouter ─────────────────────────────────────

    @Provides
    @Singleton
    @Named("openrouter_retrofit")
    fun provideOpenRouterRetrofit(
        @Named("openrouter_okhttp") okHttpClient: OkHttpClient
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://openrouter.ai/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().setLenient().create()))
            .build()
    }

    @Provides
    @Singleton
    fun provideOpenRouterApiService(
        @Named("openrouter_retrofit") retrofit: Retrofit
    ): OpenRouterApiService = retrofit.create(OpenRouterApiService::class.java)

    // ── Categorizadores ───────────────────────────────────────────────────

    @Provides
    @Singleton
    @Named("primary_categorizer")
    fun providePrimaryCategorizer(
        apiService: OpenRouterApiService,
        @Named("openrouter_api_key") apiKey: String
    ): AiCategorizer = OpenRouterAiCategorizer(apiService, apiKey)

    @Provides
    @Singleton
    @Named("fallback_categorizer")
    fun provideFallbackCategorizer(): AiCategorizer = HeuristicAiCategorizer()

    // ── Servicio de alto nivel ────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideCategorySuggestionService(
        @Named("primary_categorizer")  primary : AiCategorizer,
        @Named("fallback_categorizer") fallback: AiCategorizer
    ): CategorySuggestionService = CategorySuggestionServiceImpl(primary, fallback)
}