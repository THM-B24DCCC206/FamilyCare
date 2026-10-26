package com.familycare.app.presentation.ocr

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.familycare.app.core.database.AppDatabase
import com.familycare.app.core.database.MedicationLogEntity
import com.familycare.app.core.database.MedicineEntity
import com.familycare.app.data.repository.HealthAiRepositoryImpl
import com.familycare.app.presentation.medication.MedicationAlarmScheduler
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "ScanPrescriptionVM"

/**
 * Model biểu diễn thuốc sau khi được AI bóc tách & chuẩn hóa
 */
data class ScannedMedicineItem(
    val id: String = UUID.randomUUID().toString(),
    var isSelected: Boolean = true,
    var medicineName: String = "",
    var dosage: String = "",
    var unit: String = "gói",
    var times: List<String> = emptyList(), // MẶC ĐỊNH LÀ RỖNG, KHÔNG TỰ BỊA 08:00
    var note: String = ""                  // MẶC ĐỊNH LÀ RỖNG, KHÔNG TỰ BỊA LỜI DẶN
)

data class ScanPrescriptionUiState(
    val selectedImageUri: Uri? = null,
    val previewBitmap: Bitmap? = null,
    val isAnalyzing: Boolean = false,
    val analysisStepText: String = "",
    val errorMessage: String? = null,
    val scannedItems: List<ScannedMedicineItem> = emptyList(),
    val isSaving: Boolean = false
)

class ScanPrescriptionViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val medicineDao = db.medicineDao
    private val medicationLogDao = db.medicationLogDao
    private val alarmScheduler = MedicationAlarmScheduler(application)

    private val _uiState = MutableStateFlow(ScanPrescriptionUiState())
    val uiState: StateFlow<ScanPrescriptionUiState> = _uiState.asStateFlow()

    /**
     * Bắt đầu quy trình OCR và phân tích bằng Gemini AI (kèm fallback từ điển dược phẩm)
     */
    fun processImage(context: Context, uri: Uri) {
        viewModelScope.launch {
            // Load ảnh Bitmap để hiển thị preview
            val bitmap = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Lỗi đọc file ảnh", e)
                    null
                }
            }

            _uiState.update {
                it.copy(
                    selectedImageUri = uri,
                    previewBitmap = bitmap,
                    isAnalyzing = true,
                    errorMessage = null,
                    analysisStepText = "Đang quét văn bản OCR từ hình ảnh..."
                )
            }

            try {
                // 1. Quét văn bản bằng Google ML Kit
                val inputImage = InputImage.fromFilePath(context, uri)
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

                recognizer.process(inputImage)
                    .addOnSuccessListener { visionText ->
                        val rawText = visionText.text
                        if (rawText.isBlank()) {
                            _uiState.update {
                                it.copy(
                                    isAnalyzing = false,
                                    errorMessage = "Không tìm thấy văn bản trong ảnh. Vui lòng chụp rõ nét hơn hoặc chọn ảnh khác."
                                )
                            }
                            return@addOnSuccessListener
                        }

                        // 2. Chuyển sang bước Gemini AI bóc tách & chuẩn hóa
                        _uiState.update { it.copy(analysisStepText = "AI đang đọc và chuẩn hóa tên đơn thuốc...") }

                        viewModelScope.launch {
                            val items = parsePrescriptionWithAiOrFallback(rawText)
                            _uiState.update {
                                it.copy(
                                    isAnalyzing = false,
                                    scannedItems = items,
                                    errorMessage = if (items.isEmpty()) {
                                        "Chưa tách được danh mục thuốc tự động. Bạn có thể thêm thủ công bên dưới."
                                    } else null
                                )
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Lỗi nhận diện văn bản OCR", e)
                        _uiState.update {
                            it.copy(
                                isAnalyzing = false,
                                errorMessage = "Không thể nhận diện hình ảnh: ${e.localizedMessage ?: "Lỗi không xác định"}"
                            )
                        }
                    }
                    .addOnCompleteListener {
                        recognizer.close()
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi mở input image", e)
                _uiState.update {
                    it.copy(
                        isAnalyzing = false,
                        errorMessage = "Không thể đọc tệp ảnh: ${e.localizedMessage ?: "Vui lòng thử lại"}"
                    )
                }
            }
        }
    }

    fun toggleSelectItem(id: String, isSelected: Boolean) {
        _uiState.update { state ->
            val updated = state.scannedItems.map {
                if (it.id == id) it.copy(isSelected = isSelected) else it
            }
            state.copy(scannedItems = updated)
        }
    }

    fun selectAll(selected: Boolean) {
        _uiState.update { state ->
            val updated = state.scannedItems.map { it.copy(isSelected = selected) }
            state.copy(scannedItems = updated)
        }
    }

    fun updateItem(item: ScannedMedicineItem) {
        _uiState.update { state ->
            val updated = state.scannedItems.map {
                if (it.id == item.id) item else it
            }
            state.copy(scannedItems = updated)
        }
    }

    fun removeItem(id: String) {
        _uiState.update { state ->
            val updated = state.scannedItems.filter { it.id != id }
            state.copy(scannedItems = updated)
        }
    }

    fun addItem(item: ScannedMedicineItem) {
        _uiState.update { state ->
            state.copy(scannedItems = state.scannedItems + item)
        }
    }

    fun saveSelectedMedicines(targetMemberId: String, onDone: () -> Unit) {
        val toSave = _uiState.value.scannedItems.filter { it.isSelected && it.medicineName.isNotBlank() }
        if (toSave.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            withContext(Dispatchers.IO) {
                val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date())
                for (item in toSave) {
                    val dosageWithUnit = if (item.unit.isNotBlank() && !item.dosage.contains(item.unit)) {
                        "${item.dosage} ${item.unit}".trim()
                    } else {
                        item.dosage
                    }

                    val cleanTimes = item.times.filter { it.isNotBlank() }.distinct().sorted()

                    val medicineEntity = MedicineEntity(
                        id = UUID.randomUUID().toString(),
                        userId = targetMemberId,
                        name = item.medicineName.trim(),
                        dosage = dosageWithUnit.ifBlank { "Theo hướng dẫn" },
                        instruction = item.note.trim(),
                        reminderTimes = cleanTimes,
                        startDate = System.currentTimeMillis(),
                        scannedViaOcr = true
                    )

                    medicineDao.insertMedicine(medicineEntity)

                    // Chỉ tạo MedicationLog cho ngày hôm nay nếu người dùng có thiết lập giờ uống
                    if (cleanTimes.isNotEmpty()) {
                        for (time in cleanTimes) {
                            medicationLogDao.insertIfAbsent(
                                MedicationLogEntity(
                                    id = UUID.randomUUID().toString(),
                                    medicineId = medicineEntity.id,
                                    medicineName = medicineEntity.name,
                                    userId = targetMemberId,
                                    scheduledDate = todayDate,
                                    scheduledTime = time,
                                    isTaken = false,
                                    isSkipped = false
                                )
                            )
                        }

                        // Đặt lịch nhắc báo thức
                        alarmScheduler.schedule(
                            medicineEntity.id,
                            medicineEntity.name,
                            medicineEntity.dosage,
                            medicineEntity.reminderTimes
                        )
                    }
                }
            }
            _uiState.update { it.copy(isSaving = false) }
            onDone()
        }
    }

    /**
     * Bóc tách bằng Gemini API với System Prompt chuyên ngành y tế & thực phẩm chức năng Việt Nam
     * Tuân thủ tuyệt đối quy tắc: Không tự bịa giờ uống hay lời dặn khi ảnh không có!
     */
    private suspend fun parsePrescriptionWithAiOrFallback(rawOcrText: String): List<ScannedMedicineItem> =
        withContext(Dispatchers.IO) {
            val apiKey = HealthAiRepositoryImpl.DEFAULT_API_KEY

            if (apiKey.isNotBlank() && apiKey != "YOUR_GEMINI_API_KEY") {
                try {
                    val generativeModel = GenerativeModel(
                        modelName = "gemini-1.5-flash",
                        apiKey = apiKey,
                        generationConfig = generationConfig {
                            temperature = 0.1f // Nhiệt độ rất thấp để chính xác, không hallucinate
                            topK = 20
                            topP = 0.85f
                        }
                    )

                    val prompt = """
                        Bạn là Dược sĩ AI chuyên gia về đơn thuốc, bao bì dược phẩm, thực phẩm chức năng và trà thảo dược tại Việt Nam.
                        Nhiệm vụ: Phân tích nội dung văn bản OCR được quét từ hình ảnh đơn thuốc hoặc bao bì sản phẩm y tế sau:
                        ---
                        $rawOcrText
                        ---
                        CÁC QUY TẮC BẮT BUỘC:
                        1. NHẬN DIỆN ĐÚNG CHỮ SỐ VÀ DẤU THẬP PHÂN:
                        - Tuyệt đối KHÔNG ĐƯỢC BỎ QUA dấu phẩy (,) hoặc dấu chấm (.).
                        - Ví dụ: "42,9 g" hoặc "42.9 g" phải bóc tách chính xác là "42.9 g" hoặc "42,9 g", TUYỆT ĐỐI KHÔNG được đọc thành "429 g". Tương tự "0,5 mg" không được đọc thành "5 mg".

                        2. KHẮC PHỤC LỖI NHẬN DIỆN TÊN NGHỆ THUẬT / CÁCH ĐIỆU / LỖI CHÍNH TẢ:
                        - Chữ nghệ thuật trên bao bì trà thảo mộc, bột sủi, thực phẩm chức năng thường bị OCR quét sai ký tự (ví dụ: "7hank chcêt", "Thanh nh1et", "Livecoo1", "Sensa coo1s").
                        - Đối chiếu từ điển và suy luận ngữ cảnh để chuẩn hóa thành tên đúng:
                          + "7hank chcêt" / "Thanh nhejt" -> "Bột sủi thanh nhiệt" hoặc "Trà thanh nhiệt"
                          + "Livecoo1" -> "Livecool"
                          + "Arnlodipine" -> "Amlodipine"
                          + "Paracetamon" -> "Paracetamol"
                          + "Panado1" -> "Panadol"
                          + "Effe-ralgan" -> "Efferalgan"
                        - TUYỆT ĐỐI KHÔNG để nguyên các từ vô nghĩa như "7hank chcêt".

                        3. NHẬN DIỆN ĐÚNG ĐƠN VỊ TÍNH (unit):
                        - Nếu là gói cốm, trà hòa tan, bột sủi, hoặc có khối lượng theo gam (g), miligam trên bao -> gán unit là "gói".
                        - Chỉ gán unit là "viên" khi là viên nén, viên nang, viên sủi.
                        - Nếu là dung dịch/siro -> gán "chai" hoặc "ml".
                        - Nếu là ống uống -> gán "ống".

                        4. QUY TẮC SỐNG CÒN: TUYỆT ĐỐI KHÔNG TỰ BỊA ĐẶT DỮ LIỆU THIẾU!
                        - Nếu ảnh chụp là vỏ hộp thuốc, bao bì hoặc không có đơn thuốc chỉ định cữ uống/lời dặn của bác sĩ:
                          + BẮT BUỘC để times là mảng rỗng: []
                          + BẮT BUỘC để note là chuỗi rỗng: ""
                        - NGHIÊM CẤM TỰ GÁN "08:00" NẾU TRÊN ẢNH KHÔNG CÓ CỮ UỐNG!
                        - NGHIÊM CẤM TỰ VIẾT "Uống sau khi ăn no" NẾU TRÊN ẢNH KHÔNG CÓ!
                        - Chỉ điền times khi trên ảnh có ghi rõ cữ giờ hoặc số lần uống trong ngày (ví dụ: sáng, trưa, tối).

                        5. ĐỊNH DẠNG ĐẦU RA:
                        Xuất DUY NHẤT một mảng JSON Array hợp lệ, KHÔNG viết bất kỳ câu chữ nào khác:
                        [
                          {
                            "medicineName": "Tên chuẩn hóa",
                            "dosage": "Hàm lượng (ví dụ: 42.9 g hoặc 500mg)",
                            "unit": "gói / viên / ống / chai / ml",
                            "times": [],
                            "note": ""
                          }
                        ]
                    """.trimIndent()

                    val response = generativeModel.generateContent(prompt)
                    val reply = response.text?.trim().orEmpty()

                    val jsonStart = reply.indexOf("[")
                    val jsonEnd = reply.lastIndexOf("]")

                    if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
                        val jsonStr = reply.substring(jsonStart, jsonEnd + 1)
                        val jsonArray = JSONArray(jsonStr)
                        val list = mutableListOf<ScannedMedicineItem>()

                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            val medName = obj.optString("medicineName", "").trim()
                            val dosage = obj.optString("dosage", "").trim()
                            val unit = obj.optString("unit", "gói").trim()
                            val note = obj.optString("note", "").trim()

                            val times = mutableListOf<String>()
                            val timesArray = obj.optJSONArray("times")
                            if (timesArray != null) {
                                for (j in 0 until timesArray.length()) {
                                    val t = timesArray.optString(j, "").trim()
                                    if (t.isNotBlank()) times.add(t)
                                }
                            }

                            if (medName.isNotBlank()) {
                                list.add(
                                    ScannedMedicineItem(
                                        medicineName = medName,
                                        dosage = dosage,
                                        unit = unit.ifBlank { "gói" },
                                        times = times, // Giữ nguyên danh sách (có thể rỗng []), không tự bịa!
                                        note = note,   // Giữ nguyên chuỗi (có thể rỗng ""), không tự bịa!
                                        isSelected = true
                                    )
                                )
                            }
                        }

                        if (list.isNotEmpty()) {
                            return@withContext list
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Lỗi gọi Gemini AI, chuyển sang Fallback Offline", e)
                }
            }

            // SMART FALLBACK OFFLINE
            fallbackParseMedicineOffline(rawOcrText)
        }

    /**
     * Smart Fallback: Xử lý offline đối chiếu từ điển thảo dược & dược phẩm Việt Nam
     * Chuẩn hóa dấu thập phân và tuyệt đối không tự bịa giờ uống!
     */
    private fun fallbackParseMedicineOffline(text: String): List<ScannedMedicineItem> {
        val result = mutableListOf<ScannedMedicineItem>()
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }

        // Bắt chính xác số thập phân như 42,9 g hoặc 42.9 g hoặc 500 mg
        val dosageRegex = Regex("""(?i)\b\d+(?:[.,]\d+)?\s*(?:mg|mcg|g|gam|ml|l|iu|ui|%)\b""")

        fun removeDiacritics(str: String): String {
            return Normalizer.normalize(str.uppercase(), Normalizer.Form.NFD)
                .replace(Regex("\\p{Mn}+"), "")
                .replace('Đ', 'D')
                .replace(Regex("[^A-Z0-9]+"), " ")
                .trim()
        }

        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            val cleanLine = removeDiacritics(line)

            // Kiểm tra catalog thuốc / thảo dược
            var matchedEntry: LocalMedicineEntry? = null
            for (entry in localMedicineCatalog) {
                for (alias in entry.aliases) {
                    val cleanAlias = removeDiacritics(alias)
                    if (cleanLine.contains(cleanAlias)) {
                        matchedEntry = entry
                        break
                    }
                }
                if (matchedEntry != null) break
            }

            val hasDosage = dosageRegex.containsMatchIn(line)

            if (matchedEntry != null || hasDosage) {
                val name = matchedEntry?.standardName ?: run {
                    val before = if (hasDosage) line.substringBefore(dosageRegex.find(line)?.value ?: "").trim() else line
                    before.replace(Regex("""^\d+[\s.)-]+"""), "").trim()
                }

                // Lấy đúng số thập phân (ví dụ 42,9 g)
                val dosageMatch = dosageRegex.find(line)?.value
                val dosage = dosageMatch ?: (matchedEntry?.defaultDosage ?: "")
                val unit = matchedEntry?.defaultUnit ?: if (dosage.contains("g", ignoreCase = true) || line.contains("gói", ignoreCase = true)) "gói" else "viên"

                // Tìm cữ uống & lời dặn CHỈ KHI TRÊN TEXT CÓ
                val times = mutableListOf<String>()
                val noteBuilder = StringBuilder()

                var lookAhead = 1
                while (i + lookAhead < lines.size && lookAhead <= 2) {
                    val nextLine = lines[i + lookAhead]
                    val nextClean = removeDiacritics(nextLine)

                    if (nextClean.contains("SANG") || nextClean.contains("TRUA") || nextClean.contains("CHIEU") || nextClean.contains("TOI") || nextClean.contains("UONG")) {
                        if (nextClean.contains("SANG")) times.add("08:00")
                        if (nextClean.contains("TRUA")) times.add("12:00")
                        if (nextClean.contains("CHIEU") || nextClean.contains("TOI")) times.add("18:00")
                        if (noteBuilder.isNotEmpty()) noteBuilder.append(". ")
                        noteBuilder.append(nextLine)
                        i += lookAhead
                        break
                    } else {
                        lookAhead++
                    }
                }

                // KHÔNG TỰ BỊA ĐẶT DỮ LIỆU THIẾU
                val finalTimes = times.distinct()
                val finalNote = noteBuilder.toString()

                if (name.length >= 3 && !name.contains("BỆNH", ignoreCase = true) && !name.contains("BÁC SĨ", ignoreCase = true)) {
                    result.add(
                        ScannedMedicineItem(
                            medicineName = name,
                            dosage = dosage,
                            unit = unit,
                            times = finalTimes, // Rỗng nếu không có trên ảnh
                            note = finalNote,   // Rỗng nếu không có trên ảnh
                            isSelected = true
                        )
                    )
                }
            }
            i++
        }

        return result.distinctBy { it.medicineName }
    }
}

/**
 * Danh mục đối chiếu thông minh (bao gồm cả các lỗi nghệ thuật OCR thường gặp như '7hank chcêt')
 */
private data class LocalMedicineEntry(
    val standardName: String,
    val defaultDosage: String,
    val defaultUnit: String,
    val aliases: List<String>
)

private val localMedicineCatalog = listOf(
    // Bột sủi / trà thanh nhiệt / thực phẩm chức năng thảo mộc
    LocalMedicineEntry(
        "Bột sủi thanh nhiệt Livecool",
        "42.9 g",
        "gói",
        listOf("LIVECOOL", "7HANK CHCET", "7HANK", "CHCET", "THANH NHIET", "BOT SUI THANH NHIET", "TRA THANH NHIET", "NAM DUOC")
    ),
    LocalMedicineEntry(
        "Bột sủi Sensa Cools",
        "1 gói",
        "gói",
        listOf("SENSA COOLS", "SENSA", "SENSACOOL")
    ),
    LocalMedicineEntry(
        "Trà Atiso",
        "1 gói",
        "gói",
        listOf("ATISO", "ARTICHOKE", "LADOPHAR")
    ),
    LocalMedicineEntry(
        "Bột sủi Vitamin C",
        "1 viên",
        "viên",
        listOf("BEROCCA", "SUI C", "VITAMIN C", "C SUI", "PLUSSSZ")
    ),

    // Các thuốc tân dược phổ biến
    LocalMedicineEntry("Amlodipine", "5mg", "viên", listOf("AMLODIPINE", "ARNLODIPINE", "AMLO", "AMORLODIPIN")),
    LocalMedicineEntry("Paracetamol", "500mg", "viên", listOf("PARACETAMOL", "PARACETAMON", "PARA", "EFFERALGAN", "PANADOL", "HAPACOL")),
    LocalMedicineEntry("Metformin", "500mg", "viên", listOf("METFORMIN", "GLUCOPHAGE", "DIAPHAGE")),
    LocalMedicineEntry("Losartan", "50mg", "viên", listOf("LOSARTAN", "COZAAR", "LOSATEC")),
    LocalMedicineEntry("Atorvastatin", "10mg", "viên", listOf("ATORVASTATIN", "LIPITOR", "ATOR")),
    LocalMedicineEntry("Esomeprazole", "40mg", "viên", listOf("ESOMEPRAZOLE", "ESOMEPRAZOL", "YESOM", "NEXIUM")),
    LocalMedicineEntry("Sucralfate", "1g", "gói", listOf("SUCRALFATE", "SUCRATE GEL", "SUCRATE")),
    LocalMedicineEntry("Trimebutine", "100mg", "viên", listOf("TRIMEBUTINE", "ARTHUR", "DEBRIDAT")),
    LocalMedicineEntry("Berberin", "100mg", "viên", listOf("BERBERIN", "BERBERINE")),
    LocalMedicineEntry("Amoxicillin", "500mg", "viên", listOf("AMOXICILLIN", "AMOX", "CLAMOCIL", "AUGMENTIN", "KLAMENTIN")),
    LocalMedicineEntry("Cefalexin", "500mg", "viên", listOf("CEFALEXIN", "CEPHALEXIN", "CEFALEX")),
    LocalMedicineEntry("Aspirin", "81mg", "viên", listOf("ASPIRIN", "ASPIRIN CARDIO", "ASPEC")),
    LocalMedicineEntry("Omeprazole", "20mg", "viên", listOf("OMEPRAZOLE", "OMEPRAZOL", "LOSEC"))
)
