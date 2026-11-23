package com.example.mcmovilcare.util

object SkuCatalogo {

    private val catalogoExacto = mapOf(
        "8806094937121" to ("Samsung" to "Galaxy S22"),
        "0194253408345" to ("Apple" to "iPhone 14"),
        "0195949035944" to ("Apple" to "MacBook Pro M1"),
        "0196802345612" to ("Lenovo" to "ThinkPad"),
        "6941812701234" to ("Xiaomi" to "Redmi Note 12")
    )

    private val prefijosMarca = mapOf(
        "SKU-SAM" to "Samsung",
        "SKU-APP" to "Apple",
        "SKU-LEN" to "Lenovo",
        "SKU-XIA" to "Xiaomi",
        "SKU-HP" to "HP"
    )

    fun buscarPorCodigo(codigoRaw: String): Pair<String, String>? {
        val codigo = codigoRaw.trim().uppercase()
        catalogoExacto[codigo]?.let { return it }

        // Soporta formato "SKU-MARCA-MODELO" (ej: "SKU-SAM-S23" o "SKU-SAMSUNG-S23")
        if (codigo.startsWith("SKU-")) {
            val partes = codigo.split("-", limit = 3)
            if (partes.size == 3) {
                val prefijo = "${partes[0]}-${partes[1]}"
                val marca = prefijosMarca[prefijo] ?: partes[1].lowercase().replaceFirstChar { it.uppercase() }
                val modelo = partes[2].replace("-", " ")
                return marca to modelo
            }
        }
        return null
    }
}