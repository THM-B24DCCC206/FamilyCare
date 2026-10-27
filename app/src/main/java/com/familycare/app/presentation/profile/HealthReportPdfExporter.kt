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
        metrics: List<HealthMetricEntity>,
        memberName: String = "",
        memberRole: String = "",
        bloodType: String = "",
        chronicDiseases: List<String> = emptyList(),
        drugAllergies: List<String> = emptyList(),
        medicines: List<String> = emptyList()
    ) {
        val document = PdfDocument()
        val locale = Locale.forLanguageTag("vi-VN")

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
                "FamilyCare - Báo cáo sức khỏe cá nhân & gia đình",
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

            val title = if (memberName.isNotBlank()) {
                "BÁO CÁO Y TẾ: ${memberName.uppercase()}"
            } else {
                "BÁO CÁO SỨC KHỎE GIA ĐÌNH"
            }

            drawWrapped(
                title,
                size = 18f,
                bold = true
            )
            y += 4f

            if (memberName.isNotBlank()) {
                val roleSuffix = if (memberRole.isNotBlank()) " ($memberRole)" else ""
                drawWrapped("Thành viên: $memberName$roleSuffix")
            }
            drawWrapped("Tháng báo cáo: $monthText")
            drawWrapped(
                "Ngày xuất: ${dateFormat.format(Date())}"
            )
            y += 10f

            // 1. Thông tin y bạ & bệnh nền của thành viên
            if (memberName.isNotBlank() || bloodType.isNotBlank() || chronicDiseases.isNotEmpty() || drugAllergies.isNotEmpty()) {
                drawWrapped(
                    "THÔNG TIN Y TẾ & BỆNH NỀN",
                    size = 14f,
                    bold = true
                )
                y += 4f
                drawWrapped("• Nhóm máu: ${bloodType.ifBlank { "Chưa xác định" }}", indent = 8f)
                val diseasesStr = if (chronicDiseases.isEmpty()) "Không ghi nhận bệnh nền mãn tính" else chronicDiseases.joinToString(", ")
                drawWrapped("• Bệnh nền mãn tính: $diseasesStr", indent = 8f)
                val allergiesStr = if (drugAllergies.isEmpty()) "Không ghi nhận dị ứng" else drugAllergies.joinToString(", ")
                drawWrapped("• Dị ứng thuốc & thức ăn: $allergiesStr", indent = 8f)
                y += 8f
            }

            // 2. Danh sách đơn thuốc đang dùng
            if (medicines.isNotEmpty()) {
                drawWrapped(
                    "ĐƠN THUỐC & LỊCH UỐNG ĐANG DÙNG",
                    size = 14f,
                    bold = true
                )
                y += 4f
                medicines.forEach { med ->
                    drawWrapped("• $med", indent = 8f)
                }
                y += 8f
            }

            // 3. Dữ liệu chỉ số sức khỏe trong tháng
            drawWrapped(
                "DỮ LIỆU CHỈ SỐ SỨC KHỎE TRONG THÁNG",
                size = 14f,
                bold = true
            )
            y += 4f

            drawWrapped(
                "Số bản ghi trong tháng: ${monthlyMetrics.size}"
            )
            y += 8f

            if (monthlyMetrics.isEmpty()) {
                drawWrapped(
                    "Chưa có dữ liệu số đo sinh tồn được lưu trong tháng này."
                )
                drawWrapped(
                    "Báo cáo chỉ tổng hợp dữ liệu thực tế, không tự tạo hoặc suy đoán số đo."
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
                        "${metric.valuePrimary.toInt()}/${metric.valueSecondary.toInt()} ${metric.unit}"
                    } else {
                        "${metric.valuePrimary.toInt()} ${metric.unit}"
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
                "Lưu ý: Báo cáo tổng hợp dữ liệu cá nhân đã lưu trên ứng dụng FamilyCare, " +
                    "không thay thế chẩn đoán chuyên môn của bác sĩ.",
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
