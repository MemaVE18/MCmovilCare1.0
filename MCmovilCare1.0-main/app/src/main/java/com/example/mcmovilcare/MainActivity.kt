package com.example.mcmovilcare

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Botón para generar el PDF de la orden
        val btnGenerarPdf = findViewById<Button>(R.id.btnGenerarPdf)
        btnGenerarPdf?.setOnClickListener {
            generarPdfOrden("Orden de Servicio #1024 - Cliente: MCmovilCare")
        }
    }

    private fun generarPdfOrden(tituloOrden: String) {
        val pdfDocument = PdfDocument()
        val paint = Paint()
        val pageInfo = PdfDocument.PageInfo.Builder(300, 600, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        paint.textSize = 14f
        canvas.drawText("MCmovilCare - Comprobante", 20f, 40f, paint)
        paint.textSize = 10f
        canvas.drawText(tituloOrden, 20f, 70f, paint)
        canvas.drawText("Estado: Completado / Sincronizado", 20f, 90f, paint)

        pdfDocument.finishPage(page)

        val file = File(getExternalFilesDir(null), "Orden_Servicio.pdf")
        try {
            pdfDocument.writeTo(FileOutputStream(file))
            Toast.makeText(this, "PDF guardado en: ${file.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Error al generar PDF: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            pdfDocument.close()
        }
    }
}