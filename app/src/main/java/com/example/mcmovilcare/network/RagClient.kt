package com.example.mcmovilcare.network

import com.google.gson.annotations.SerializedName
import okhttp3.ConnectionPool
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Protocol
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.net.Inet4Address
import java.net.InetAddress
import java.util.concurrent.TimeUnit

data class DiagnosticoRagRequest(
    val categoria: String,
    val marca: String,
    val modelo: String,
    val ean: String? = null,
    val sintoma: String
)

data class CasoReferenciaRag(
    @SerializedName("orden_codigo") val ordenCodigo: String? = "",
    val equipo: String? = "",
    val sintoma: String? = "",
    val diagnostico: String? = "",
    val solucion: String? = "",
    @SerializedName("estado_final") val estadoFinal: String? = "",
    @SerializedName("repuestos_sku") val repuestosSku: List<String>? = emptyList(),
    val similitud: Double? = 0.0
)

data class DiagnosticoRagResponse(
    @SerializedName("causas_probables") val causasProbables: List<String>? = emptyList(),
    @SerializedName("pasos_revision") val pasosRevision: List<String>? = emptyList(),
    @SerializedName("soluciones_alternativas") val solucionesAlternativas: List<String>? = emptyList(),
    @SerializedName("advertencia_devolucion") val advertenciaDevolucion: String? = null,
    @SerializedName("repuestos_sku_sugeridos") val repuestosSkuSugeridos: List<String>? = emptyList(),
    @SerializedName("casos_referencia") val casosReferencia: List<CasoReferenciaRag>? = emptyList(),
    @SerializedName("modo_busqueda") val modoBusqueda: String? = "RAG"
) {
    val causasSeguras: List<String> get() = causasProbables.orEmpty()
    val pasosSeguros: List<String> get() = pasosRevision.orEmpty()
    val solucionesSeguras: List<String> get() = solucionesAlternativas.orEmpty()
    val skusSeguros: List<String> get() = repuestosSkuSugeridos.orEmpty()
    val casosSeguros: List<CasoReferenciaRag> get() = casosReferencia.orEmpty()
    val modoSeguro: String get() = modoBusqueda?.takeIf { it.isNotBlank() } ?: "RAG"
}

data class NuevoCasoRagRequest(
    @SerializedName("orden_codigo") val ordenCodigo: String,
    val categoria: String,
    val marca: String,
    val modelo: String,
    val ean: String? = null,
    val sintoma: String,
    val diagnostico: String,
    @SerializedName("solucion_aplicada") val solucionAplicada: String,
    @SerializedName("estado_final") val estadoFinal: String,
    @SerializedName("repuestos_sku") val repuestosSku: List<String> = emptyList()
)

interface RagApiService {
    @POST("api/rag/diagnosticar")
    suspend fun obtenerDiagnostico(@Body request: DiagnosticoRagRequest): DiagnosticoRagResponse

    @POST("api/rag/casos")
    suspend fun registrarCasoCerrado(@Body request: NuevoCasoRagRequest): Map<String, Any>
}

object RagRetrofitClient {
    private const val BASE_URL = "http://13.218.171.220:8080/"

    private val ipv4OnlyDns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            return Dns.SYSTEM.lookup(hostname).filter { it is Inet4Address }
        }
    }

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .dns(ipv4OnlyDns)
        .protocols(listOf(Protocol.HTTP_1_1))
        .connectionPool(ConnectionPool(0, 1, TimeUnit.SECONDS))
        .retryOnConnectionFailure(true)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val api: RagApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RagApiService::class.java)
    }
}