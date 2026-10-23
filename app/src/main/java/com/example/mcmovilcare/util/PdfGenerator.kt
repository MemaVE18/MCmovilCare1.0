package com.example.mcmovilcare.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class PdfGenerator {
    fun generarPdfOrden(context: Context, tituloOrden: String) {
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

        val file = File(context.getExternalFilesDir(null), "Orden_Servicio.pdf")
        try {
            pdfDocument.writeTo(FileOutputStream(file))
            Toast.makeText(context, "PDF guardado en: ${file.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(context, "Error al generar PDF: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            pdfDocument.close()
        }
    }
}