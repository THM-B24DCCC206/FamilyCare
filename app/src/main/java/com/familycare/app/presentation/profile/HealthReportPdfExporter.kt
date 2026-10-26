
package com.familycare.app.presentation.profile

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.familycare.app.core.database.HealthMetricEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object HealthReportPdfExporter {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val LEFT = 40f
    private const val RIGHT = 555f
    private const val BOTTOM = 780f

    fun export(
        context: Context,
        uri: Uri,
        metrics: List<HealthMetricEntity>
    ) {
        val document = PdfDocument()
        val locale = Locale("vi", "VN")

        val normalTypeface = Typeface.create(
            "sans-serif",
            Typeface.NORMAL
        )
        val boldTypeface = Typeface.create(
            "sans-serif",
            Typeface.BOLD
        )

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            typeface = normalTypeface
            textSize = 12f
        }

        var pageNumber = 1
        var page = document.startPage(
            PdfDocument.PageInfo.Builder(
                PAGE_WIDTH, PAGE_HEIGHT, pageNumber
            ).create()
        )
        var canvas = page.canvas
        var y = 48f

        fun finishCurrentPage() {
            paint.color = Color.DKGRAY
            paint.textSize = 9f
            paint.typeface = normalTypeface

            canvas.drawLine(
                LEFT, 800f, RIGHT, 800f, paint
            )
            canvas.drawText(
                "FamilyCare - Báo cáo sức khỏe",
                LEFT, 820f, paint
            )
            canvas.drawText(
                "Trang $pageNumber",
                500f, 820f, paint
            )

            document.finishPage(page)
        }

        fun startNewPage() {
            finishCurrentPage()
            pageNumber++

            page = document.startPage(
                PdfDocument.PageInfo.Builder(
                    PAGE_WIDTH, PAGE_HEIGHT, pageNumber
                ).create()
            )
            canvas = page.canvas
            y = 48f
        }

        fun drawWrapped(
            text: String,
            size: Float = 12f,
            bold: Boolean = false,
            indent: Float = 0f
        ) {
            paint.color = Color.BLACK
            paint.textSize = size
            paint.typeface = if (bold) {
                boldTypeface
            } else {
                normalTypeface
            }

            val maxWidth = RIGHT - LEFT - indent
            val lineHeight = size + 7f

            for (paragraph in text.split("\n")) {
                val words = paragraph.split(Regex("\\s+"))
                    .filter { it.isNotEmpty() }

                if (words.isEmpty()) {
                    y += lineHeight
                    continue
                }

                var line = ""

                for (word in words) {
                    val candidate = if (line.isEmpty()) {
                        word
                    } else {
                        "$line $word"
                    }

                    if (
                        line.isNotEmpty() &&
                        paint.measureText(candidate) > maxWidth
                    ) {
                        if (y + lineHeight > BOTTOM) {
                            startNewPage()
                        }

                        canvas.drawText(
                            line, LEFT + indent, y, paint
                        )
                        y += lineHeight
                        line = word
                    } else {
                        line = candidate
                    }
                }

                if (line.isNotEmpty()) {
                    if (y + lineHeight > BOTTOM) {
                        startNewPage()
                    }

                    canvas.drawText(
                        line, LEFT + indent, y, paint
                    )
                    y += lineHeight
                }
            }
        }

        try {
            val now = Calendar.getInstance()

            val monthStart = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val nextMonth = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                add(Calendar.MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val monthText = SimpleDateFormat(
                "MM/yyyy", locale
            ).format(now.time)

            val dateFormat = SimpleDateFormat(
                "dd/MM/yyyy HH:mm", locale
            )

            val monthlyMetrics = metrics
                .filter {
                    it.timestamp >= monthStart &&
                        it.timestamp < nextMonth
                }
                .sortedBy { it.timestamp }

            drawWrapped(
                "BÁO CÁO SỨC KHỎE GIA ĐÌNH",
                size = 19f,
                bold = true
            )
            y += 5f

            drawWrapped("Tháng báo cáo: $monthText")
            drawWrapped(
                "Ngày xuất: ${dateFormat.format(Date())}"
            )
            y += 12f

            drawWrapped(
                "DỮ LIỆU CHỈ SỐ SỨC KHỎE",
                size = 15f,
                bold = true
            )
            y += 5f

            drawWrapped(
                "Số bản ghi trong tháng: ${monthlyMetrics.size}"
            )
            y += 8f

            if (monthlyMetrics.isEmpty()) {
                drawWrapped(
                    "Chưa có dữ liệu sức khỏe được lưu trong tháng này."
                )
                drawWrapped(
                    "Báo cáo không tự tạo hoặc suy đoán số đo."
                )
            } else {
                monthlyMetrics.forEachIndexed { index, metric ->
                    if (y + 60f > BOTTOM) {
                        startNewPage()
                    }

                    val typeName = when (metric.type) {
                        "BLOOD_PRESSURE" -> "Huyết áp"
                        "HEART_RATE" -> "Nhịp tim"
                        "BLOOD_SUGAR" -> "Đường huyết"
                        "TEMPERATURE" -> "Nhiệt độ"
                        else -> metric.type
                    }

                    val value = if (
                        metric.valueSecondary != null
                    ) {
                        "${metric.valuePrimary}/${metric.valueSecondary} ${metric.unit}"
                    } else {
                        "${metric.valuePrimary} ${metric.unit}"
                    }

                    drawWrapped(
                        "${index + 1}. $typeName: $value",
                        size = 13f,
                        bold = true
                    )

                    drawWrapped(
                        "Thời gian đo: ${dateFormat.format(Date(metric.timestamp))}",
                        indent = 12f
                    )

                    if (metric.notes.isNotBlank()) {
                        drawWrapped(
                            "Ghi chú: ${metric.notes}",
                            indent = 12f
                        )
                    }

                    y += 8f
                }
            }

            y += 8f
            drawWrapped(
                "Lưu ý: Báo cáo tổng hợp dữ liệu đã lưu, " +
                    "không thay thế chẩn đoán của bác sĩ.",
                size = 10f
            )

            finishCurrentPage()

            val output = context.contentResolver
                .openOutputStream(uri)
                ?: throw IllegalStateException(
                    "Không thể mở tệp để ghi báo cáo PDF."
                )

            output.use {
                document.writeTo(it)
            }
        } finally {
            document.close()
        }
    }
}
