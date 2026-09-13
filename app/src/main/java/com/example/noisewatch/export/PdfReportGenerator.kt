package com.example.noisewatch.export

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.noisewatch.R
import com.example.noisewatch.ui.screens.IncidentReportUiState
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportGenerator {

    fun generatePdfReport(
        context: Context,
        reportState: IncidentReportUiState
    ): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size: 595 x 842 points
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        return try {
            val margin = 36f
            val pageWidth = 595f
            val contentWidth = pageWidth - (margin * 2)
            var y = margin

            // Clean evidence document colors (white background, deep navy text, slate card)
            val navyColor = Color.parseColor("#18232E")
            val slateColor = Color.parseColor("#C7D2DB")
            val warmAmberBg = Color.parseColor("#F6E8C3")
            val outlineColor = Color.parseColor("#5A6B78")

            // 1. Header with Logo Branding & Title
            try {
                val iconBitmap = BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher_foreground)
                if (iconBitmap != null) {
                    val iconSize = 48f
                    val destRect = RectF(pageWidth - margin - iconSize, y, pageWidth - margin, y + iconSize)
                    canvas.drawBitmap(iconBitmap, null, destRect, null)
                }
            } catch (_: Exception) {
                // Ignore logo failure
            }

            val titlePaint = TextPaint().apply {
                color = navyColor
                textSize = 22f
                isFakeBoldText = true
                isAntiAlias = true
            }
            canvas.drawText("Noise Incident Report", margin, y + 24f, titlePaint)
            y += 38f

            // Date & Location Header Metadata
            val subPaint = TextPaint().apply {
                color = outlineColor
                textSize = 11f
                isAntiAlias = true
            }
            canvas.drawText(reportState.formattedDateTime, margin, y, subPaint)
            y += 16f

            val locPaint = TextPaint().apply {
                color = navyColor
                textSize = 14f
                isFakeBoldText = true
                isAntiAlias = true
            }
            canvas.drawText(reportState.primaryLocation, margin, y, locPaint)
            y += 24f

            // 2. Primary Measurement Card
            val cardTop = y
            val cardHeight = 110f
            val cardPaint = Paint().apply {
                color = slateColor
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(RectF(margin, cardTop, margin + contentWidth, cardTop + cardHeight), 8f, 8f, cardPaint)

            // LAeq
            val laeqValPaint = TextPaint().apply {
                color = navyColor
                textSize = 28f
                isFakeBoldText = true
                isAntiAlias = true
            }
            canvas.drawText("${reportState.laeqFormatted} dB(A)", margin + 16f, cardTop + 36f, laeqValPaint)

            val laeqLabelPaint = TextPaint().apply {
                color = navyColor
                textSize = 10f
                isAntiAlias = true
            }
            canvas.drawText("LAeq (Equivalent Continuous Sound Level)", margin + 16f, cardTop + 52f, laeqLabelPaint)

            // Divider inside card
            val linePaint = Paint().apply {
                color = navyColor
                alpha = 40
                strokeWidth = 1f
            }
            canvas.drawLine(margin + 16f, cardTop + 62f, margin + contentWidth - 16f, cardTop + 62f, linePaint)

            // Sub metrics: Max, Min, Duration
            val colWidth = contentWidth / 3f
            val metricLabelPaint = TextPaint().apply {
                color = navyColor
                textSize = 9f
                isAntiAlias = true
            }
            val metricValPaint = TextPaint().apply {
                color = navyColor
                textSize = 12f
                isFakeBoldText = true
                isAntiAlias = true
            }

            // Max
            canvas.drawText("Maximum", margin + 16f, cardTop + 78f, metricLabelPaint)
            canvas.drawText("${reportState.maxDbFormatted} dB(A)", margin + 16f, cardTop + 94f, metricValPaint)

            // Min
            canvas.drawText("Minimum", margin + 16f + colWidth, cardTop + 78f, metricLabelPaint)
            canvas.drawText("${reportState.minDbFormatted} dB(A)", margin + 16f + colWidth, cardTop + 94f, metricValPaint)

            // Duration
            canvas.drawText("Duration", margin + 16f + (colWidth * 2), cardTop + 78f, metricLabelPaint)
            canvas.drawText(reportState.durationFormatted, margin + 16f + (colWidth * 2), cardTop + 94f, metricValPaint)

            y = cardTop + cardHeight + 16f

            // 3. Threshold Context
            val threshBg = if (reportState.isHighNoise) warmAmberBg else slateColor
            val threshHeight = 36f
            val threshCardPaint = Paint().apply {
                color = threshBg
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(RectF(margin, y, margin + contentWidth, y + threshHeight), 6f, 6f, threshCardPaint)

            val threshTextPaint = TextPaint().apply {
                color = navyColor
                textSize = 10f
                isAntiAlias = true
            }
            val threshMsg = if (reportState.isHighNoise) {
                "High noise level: Exceeds the app's 75 dB(A) reporting threshold."
            } else {
                "Normal reading: Below the app's simplified 75 dB(A) reporting threshold."
            }
            canvas.drawText(threshMsg, margin + 12f, y + 22f, threshTextPaint)
            y += threshHeight + 16f

            // 4. Location Details Section
            val sectionTitlePaint = TextPaint().apply {
                color = navyColor
                textSize = 12f
                isFakeBoldText = true
                isAntiAlias = true
            }
            val sectionBodyPaint = TextPaint().apply {
                color = navyColor
                textSize = 10f
                isAntiAlias = true
            }

            canvas.drawText("Location", margin, y, sectionTitlePaint)
            y += 14f
            canvas.drawText(reportState.primaryLocation, margin, y, sectionBodyPaint)
            y += 14f
            if (reportState.secondaryLocationDetails != null) {
                canvas.drawText(reportState.secondaryLocationDetails, margin, y, subPaint)
                y += 14f
            }
            y += 8f

            // 5. Noise Source Section (only shown if present)
            if (reportState.noiseSource != null) {
                canvas.drawLine(margin, y, margin + contentWidth, y, linePaint)
                y += 12f
                canvas.drawText("Noise Source", margin, y, sectionTitlePaint)
                y += 14f
                canvas.drawText(reportState.noiseSource, margin, y, sectionBodyPaint)
                y += 18f
            }

            // 6. Notes Section (only shown if present)
            if (reportState.notes != null) {
                canvas.drawLine(margin, y, margin + contentWidth, y, linePaint)
                y += 12f
                canvas.drawText("Notes", margin, y, sectionTitlePaint)
                y += 14f

                val notesTextPaint = TextPaint().apply {
                    color = navyColor
                    textSize = 10f
                    isAntiAlias = true
                }
                val layout = StaticLayout.Builder.obtain(
                    reportState.notes,
                    0,
                    reportState.notes.length,
                    notesTextPaint,
                    contentWidth.toInt()
                ).setLineSpacing(0f, 1.2f).build()

                canvas.save()
                canvas.translate(margin, y - 10f)
                layout.draw(canvas)
                canvas.restore()

                y += layout.height + 12f
            }

            // 7. Photo Section (only shown if present & loadable)
            if (!reportState.photoUri.isNullOrBlank()) {
                try {
                    val photoBitmap = BitmapFactory.decodeFile(reportState.photoUri)
                    if (photoBitmap != null) {
                        canvas.drawLine(margin, y, margin + contentWidth, y, linePaint)
                        y += 12f
                        canvas.drawText("Photo Evidence", margin, y, sectionTitlePaint)
                        y += 12f

                        val imgWidth = contentWidth
                        val bmWidth = photoBitmap.width.toFloat()
                        val bmHeight = photoBitmap.height.toFloat()
                        val calculatedHeight = if (bmWidth > 0) (imgWidth * (bmHeight / bmWidth)).coerceAtMost(220f) else 160f

                        val photoRect = RectF(margin, y, margin + imgWidth, y + calculatedHeight)

                        canvas.drawBitmap(photoBitmap, null, photoRect, null)
                        photoBitmap.recycle()

                        y += calculatedHeight + 16f
                    }
                } catch (_: Exception) {
                    // Fail gracefully without photo
                }
            }

            // 8. Disclaimer & Footer
            canvas.drawLine(margin, 765f, margin + contentWidth, 765f, linePaint)

            val disclaimerPaint = TextPaint().apply {
                color = outlineColor
                textSize = 8f
                isAntiAlias = true
            }
            val discLayout = StaticLayout.Builder.obtain(
                reportState.disclaimerText,
                0,
                reportState.disclaimerText.length,
                disclaimerPaint,
                contentWidth.toInt()
            ).setAlignment(Layout.Alignment.ALIGN_CENTER).build()

            canvas.save()
            canvas.translate(margin, 770f)
            discLayout.draw(canvas)
            canvas.restore()

            // Subtle Footer
            val footerPaint = TextPaint().apply {
                color = outlineColor
                textSize = 7.5f
                isAntiAlias = true
            }
            val footerText = "Generated by NoiseWatch for Android"
            val footerWidth = footerPaint.measureText(footerText)
            canvas.drawText(footerText, (pageWidth - footerWidth) / 2f, 810f, footerPaint)

            pdfDocument.finishPage(page)

            // Clean up old temporary PDFs in context.filesDir/pdfs/
            val pdfsDir = File(context.filesDir, "pdfs")
            if (!pdfsDir.exists()) {
                pdfsDir.mkdirs()
            } else {
                pdfsDir.listFiles()?.forEach { oldFile ->
                    if (oldFile.isFile && System.currentTimeMillis() - oldFile.lastModified() > 86400000L) {
                        oldFile.delete()
                    }
                }
            }

            val dateFormatter = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.getDefault())
            val dateStamp = dateFormatter.format(Date(reportState.id))
            val pdfFile = File(pdfsDir, "NoiseWatch_Incident_${dateStamp}.pdf")

            FileOutputStream(pdfFile).use { out ->
                pdfDocument.writeTo(out)
            }

            pdfFile.canonicalFile
        } catch (e: Exception) {
            null
        } finally {
            pdfDocument.close()
        }
    }

    fun sharePdfReport(context: Context, pdfFile: File) {
        Handler(Looper.getMainLooper()).post {
            try {
                val canonicalFile = pdfFile.canonicalFile
                val contentUri: Uri = FileProvider.getUriForFile(
                    context,
                    "com.example.noisewatch.fileprovider",
                    canonicalFile
                )

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    putExtra(Intent.EXTRA_SUBJECT, "NoiseWatch incident report")
                    putExtra(Intent.EXTRA_TEXT, "NoiseWatch incident report")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                val chooser = Intent.createChooser(shareIntent, "Share NoiseWatch report")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to share pdf report: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
