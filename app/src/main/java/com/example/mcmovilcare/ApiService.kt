package com.example.mcmovilcare

import com.example.mcmovilcare.model.OrdenTrabajo
import com.example.mcmovilcare.model.Usuario
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ApiService {
    @GET("ordenes")
    suspend fun obtenerOrdenes(): List<OrdenTrabajo>

    @POST("ordenes")
    suspend fun crearOrden(@Body orden: OrdenTrabajo): OrdenTrabajo

    @PUT("ordenes/{id}")
    suspend fun actualizarOrden(@Path("id") id: String, @Body orden: OrdenTrabajo): OrdenTrabajo

    @GET("usuarios")
    suspend fun obtenerUsuarios(): List<Usuario>

    @POST("usuarios")
    suspend fun crearUsuario(@Body usuario: Usuario): Usuario
}

object RetrofitClient {
    private const val BASE_URL = "https://6af9d9cf6d8b86fb22d74b96.mockapi.io/"

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}