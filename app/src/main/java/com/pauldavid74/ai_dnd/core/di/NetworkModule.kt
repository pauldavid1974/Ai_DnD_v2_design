package com.pauldavid74.ai_dnd.core.di

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.sse.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import com.pauldavid74.ai_dnd.core.network.AiProvider
import com.pauldavid74.ai_dnd.core.network.OpenAiProvider
import com.pauldavid74.ai_dnd.core.network.AnthropicProvider
import com.pauldavid74.ai_dnd.core.network.GroqProvider
import com.pauldavid74.ai_dnd.core.data.repository.AiProviderRepository
import com.pauldavid74.ai_dnd.core.data.repository.AiProviderRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkModule {

    @Binds
    @Singleton
    abstract fun bindAiProviderRepository(
        impl: AiProviderRepositoryImpl
    ): AiProviderRepository

    companion object {
        @Provides
        @Singleton
        fun provideJson(): Json = Json {
            ignoreUnknownKeys = true
            prettyPrint = true
            isLenient = true
            encodeDefaults = true
        }

        @Provides
        @Singleton
        fun provideHttpClient(json: Json): HttpClient {
            return HttpClient(OkHttp) {
                install(ContentNegotiation) {
                    json(json)
                }
                install(SSE)
                install(Logging) {
                    level = LogLevel.INFO
                }
                install(HttpTimeout) {
                    requestTimeoutMillis = 300_000 // 5 minutes
                    connectTimeoutMillis = 30_000 // 30 seconds
                    socketTimeoutMillis = 300_000 // 5 minutes
                }
                engine {
                    config {
                        connectTimeout(30, TimeUnit.SECONDS)
                        readTimeout(300, TimeUnit.SECONDS)
                        writeTimeout(300, TimeUnit.SECONDS)
                    }
                }
            }
        }

        @Provides
        @IntoSet
        fun provideOpenAiProvider(httpClient: HttpClient, json: Json): AiProvider {
            return OpenAiProvider(httpClient, json)
        }

        @Provides
        @IntoSet
        fun provideAnthropicProvider(httpClient: HttpClient, json: Json): AiProvider {
            return AnthropicProvider(httpClient, json)
        }

        @Provides
        @IntoSet
        fun provideGroqProvider(httpClient: HttpClient, json: Json): AiProvider {
            return GroqProvider(httpClient, json)
        }
    }
}
