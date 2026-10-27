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
    var dosage: String = "1",              // Mặc định là 1 (liều uống thực tế: 1 gói/viên mỗi lần)
    var unit: String = "gói",              // gói, viên, ống, chai, ml
    var purpose: String = "",              // Công dụng ngắn gọn: "Mát gan, thanh nhiệt", "Hạ huyết áp"...
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
                    val dosageFormatted = if (item.unit.isNotBlank()) {
                        "${item.dosage.ifBlank { "1" }} ${item.unit}".trim()
                    } else {
                        item.dosage.ifBlank { "1 gói" }
                    }

                    val cleanTimes = item.times.filter { it.isNotBlank() }.distinct().sorted()

                    val medicineEntity = MedicineEntity(
                        id = UUID.randomUUID().toString(),
                        userId = targetMemberId,
                        name = item.medicineName.trim(),
                        dosage = dosageFormatted,
                        instruction = item.note.trim(),
                        reminderTimes = cleanTimes,
                        startDate = System.currentTimeMillis(),
                        scannedViaOcr = true,
                        purpose = item.purpose.trim()
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
     * Đơn giản hóa dữ liệu (không lấy số gam tổng) và tự động suy luận công dụng (purpose).
     */
    private suspend fun parsePrescriptionWithAiOrFallback(rawOcrText: String): List<ScannedMedicineItem> =
        withContext(Dispatchers.IO) {
            val sessionManager = com.familycare.app.core.common.SessionManager(getApplication())
            val apiKey = sessionManager.geminiApiKey.ifBlank { HealthAiRepositoryImpl.DEFAULT_API_KEY }

            if (apiKey.isNotBlank() && apiKey != "YOUR_GEMINI_API_KEY") {
                try {
                    val generativeModel = GenerativeModel(
                        modelName = "gemini-1.5-flash",
                        apiKey = apiKey,
                        generationConfig = generationConfig {
                            temperature = 0.1f
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

                        1. ĐƠN GIẢN HÓA LIỀU LƯỢNG & BỎ HOÀN TOÀN SỐ GRAM TỔNG:
                        - TUYỆT ĐỐI KHÔNG lấy các con số gram tổng như 42.9g hay 7.15g làm liều lượng dùng.
                        - Chuẩn hóa liều dùng thực tế (dosage & unit):
                          + Nếu thuốc dạng gói bột/sủi/cốm -> Mặc định dosage là "1" và unit là "gói" (người lớn tuổi chỉ cần biết uống 1 gói/lần).
                          + Nếu dạng viên nén/nang/sủi -> Mặc định dosage là "1" và unit là "viên".
                          + Nếu dạng ống uống -> Mặc định dosage là "1" và unit là "ống".
                          + Nếu dạng chai/siro -> Mặc định dosage là "5ml" hoặc "1" và unit là "chai".

                        2. BỔ SUNG TRƯỜNG CÔNG DỤNG THUỐC (purpose):
                        - Tự động suy luận công dụng chính của thuốc/thực phẩm chức năng dưới dạng nhãn ngắn gọn từ 2–4 từ (ví dụ: "Mát gan, thanh nhiệt", "Hạ huyết áp", "Hạ đường huyết", "Giảm đau, hạ sốt", "Bổ sung Vitamin", "Hỗ trợ tiêu hóa", "Kháng khuẩn").

                        3. KHẮC PHỤC LỖI NHẬN DIỆN TÊN NGHỆ THUẬT / CÁCH ĐIỆU:
                        - Chữ nghệ thuật trên bao bì trà thảo mộc, bột sủi, thực phẩm chức năng thường bị OCR quét sai ký tự (ví dụ: "7hank chcêt", "Thanh nh1et", "Livecoo1", "Sensa coo1s").
                        - Đối chiếu từ điển và suy luận ngữ cảnh để chuẩn hóa thành tên đúng:
                          + "7hank chcêt" / "Thanh nhejt" -> "Bột sủi thanh nhiệt Livecool" hoặc "Trà thanh nhiệt"
                          + "Livecoo1" -> "Livecool"
                          + "Arnlodipine" -> "Amlodipine"
                          + "Paracetamon" -> "Paracetamol"
                          + "Panado1" -> "Panadol"
                          + "Effe-ralgan" -> "Efferalgan"
                        - TUYỆT ĐỐI KHÔNG để nguyên các từ vô nghĩa như "7hank chcêt".

                        4. QUY TẮC ĐỂ TRỐNG: KHÔNG TỰ BỊA ĐẶT DỮ LIỆU THIẾU!
                        - Nếu ảnh chụp là vỏ hộp thuốc, bao bì hoặc không có đơn thuốc chỉ định cữ uống/lời dặn của bác sĩ:
                          + BẮT BUỘC để times là mảng rỗng: []
                          + BẮT BUỘC để note là chuỗi rỗng: ""
                        - NGHIÊM CẤM TỰ GÁN "08:00" NẾU TRÊN ẢNH KHÔNG CÓ CỮ UỐNG!
                        - NGHIÊM CẤM TỰ VIẾT "Uống sau khi ăn no" NẾU TRÊN ẢNH KHÔNG CÓ!

                        5. ĐỊNH DẠNG ĐẦU RA:
                        Xuất DUY NHẤT một mảng JSON Array hợp lệ, KHÔNG viết bất kỳ câu chữ nào khác:
                        [
                          {
                            "medicineName": "Bột sủi thanh nhiệt Livecool",
                            "dosage": "1",
                            "unit": "gói",
                            "purpose": "Mát gan, thanh nhiệt",
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
                            val dosage = obj.optString("dosage", "1").trim()
                            val unit = obj.optString("unit", "gói").trim()
                            val purpose = obj.optString("purpose", "").trim()
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
                                        dosage = dosage.ifBlank { "1" },
                                        unit = unit.ifBlank { "gói" },
                                        purpose = purpose,
                                        times = times, // Rỗng nếu không có trên ảnh
                                        note = note,   // Rỗng nếu không có trên ảnh
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
     * Chuẩn hóa liều dùng đơn giản (1 gói / 1 viên) và bổ sung công dụng (purpose)
     */
    private fun fallbackParseMedicineOffline(text: String): List<ScannedMedicineItem> {
        val result = mutableListOf<ScannedMedicineItem>()
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }

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

            val hasPackOrTablet = cleanLine.contains("GOI") || cleanLine.contains("VIEN") || cleanLine.contains("SUI")

            if (matchedEntry != null || hasPackOrTablet) {
                val name = matchedEntry?.standardName ?: run {
                    line.replace(Regex("""^\d+[\s.)-]+"""), "").trim()
                }

                val unit = matchedEntry?.defaultUnit ?: if (cleanLine.contains("GOI") || cleanLine.contains("BOT")) "gói" else "viên"
                val dosage = matchedEntry?.defaultDosage ?: "1"
                val purpose = matchedEntry?.purpose ?: ""

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

                val finalTimes = times.distinct()
                val finalNote = noteBuilder.toString()

                if (name.length >= 3 && !name.contains("BỆNH", ignoreCase = true) && !name.contains("BÁC SĨ", ignoreCase = true)) {
                    result.add(
                        ScannedMedicineItem(
                            medicineName = name,
                            dosage = dosage,
                            unit = unit,
                            purpose = purpose,
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
 * Danh mục đối chiếu thông minh với liều dùng đơn giản hóa (1 gói, 1 viên) và công dụng y tế
 */
private data class LocalMedicineEntry(
    val standardName: String,
    val defaultDosage: String,
    val defaultUnit: String,
    val purpose: String,
    val aliases: List<String>
)

private val localMedicineCatalog = listOf(
    // Bột sủi / trà thanh nhiệt / thực phẩm chức năng thảo mộc
    LocalMedicineEntry(
        "Bột sủi thanh nhiệt Livecool",
        "1",
        "gói",
        "Mát gan, thanh nhiệt",
        listOf("LIVECOOL", "7HANK CHCET", "7HANK", "CHCET", "THANH NHIET", "BOT SUI THANH NHIET", "TRA THANH NHIET", "NAM DUOC")
    ),
    LocalMedicineEntry(
        "Bột sủi Sensa Cools",
        "1",
        "gói",
        "Thanh nhiệt, giải độc",
        listOf("SENSA COOLS", "SENSA", "SENSACOOL")
    ),
    LocalMedicineEntry(
        "Trà Atiso",
        "1",
        "gói",
        "Mát gan, lợi mật",
        listOf("ATISO", "ARTICHOKE", "LADOPHAR")
    ),
    LocalMedicineEntry(
        "Viên sủi Vitamin C",
        "1",
        "viên",
        "Tăng đề kháng",
        listOf("BEROCCA", "SUI C", "VITAMIN C", "C SUI", "PLUSSSZ")
    ),

    // Các thuốc tân dược phổ biến
    LocalMedicineEntry("Amlodipine", "1", "viên", "Hạ huyết áp", listOf("AMLODIPINE", "ARNLODIPINE", "AMLO", "AMORLODIPIN")),
    LocalMedicineEntry("Paracetamol", "1", "viên", "Giảm đau, hạ sốt", listOf("PARACETAMOL", "PARACETAMON", "PARA", "EFFERALGAN", "PANADOL", "HAPACOL")),
    LocalMedicineEntry("Metformin", "1", "viên", "Hạ đường huyết", listOf("METFORMIN", "GLUCOPHAGE", "DIAPHAGE")),
    LocalMedicineEntry("Losartan", "1", "viên", "Hạ huyết áp", listOf("LOSARTAN", "COZAAR", "LOSATEC")),
    LocalMedicineEntry("Atorvastatin", "1", "viên", "Hạ mỡ máu", listOf("ATORVASTATIN", "LIPITOR", "ATOR")),
    LocalMedicineEntry("Esomeprazole", "1", "viên", "Giảm axit dạ dày", listOf("ESOMEPRAZOLE", "ESOMEPRAZOL", "YESOM", "NEXIUM")),
    LocalMedicineEntry("Sucralfate", "1", "gói", "Bảo vệ niêm mạc dạ dày", listOf("SUCRALFATE", "SUCRATE GEL", "SUCRATE")),
    LocalMedicineEntry("Trimebutine", "1", "viên", "Điều hòa nhu động ruột", listOf("TRIMEBUTINE", "ARTHUR", "DEBRIDAT")),
    LocalMedicineEntry("Berberin", "1", "viên", "Hỗ trợ tiêu hóa", listOf("BERBERIN", "BERBERINE")),
    LocalMedicineEntry("Amoxicillin", "1", "viên", "Kháng khuẩn", listOf("AMOXICILLIN", "AMOX", "CLAMOCIL", "AUGMENTIN", "KLAMENTIN")),
    LocalMedicineEntry("Cefalexin", "1", "viên", "Kháng khuẩn", listOf("CEFALEXIN", "CEPHALEXIN", "CEFALEX")),
    LocalMedicineEntry("Aspirin", "1", "viên", "Phòng ngừa huyết khối", listOf("ASPIRIN", "ASPIRIN CARDIO", "ASPEC")),
    LocalMedicineEntry("Omeprazole", "1", "viên", "Giảm axit dạ dày", listOf("OMEPRAZOLE", "OMEPRAZOL", "LOSEC"))
)
