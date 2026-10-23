package com.example.mcmovilcare

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

// 1. Modelo de datos simulado
data class EquipoDto(
    val id: Int,
    val nombre: String,
    val estado: String
)

// 2. Interfaz para definir endpoints
interface ApiService {
    @GET("equipos")
    suspend fun obtenerEquipos(): List<EquipoDto>
}

// 3. Cliente HTTP Singleton
object RetrofitClient {
    private const val BASE_URL = "https://jsonplaceholder.typicode.com/"

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}