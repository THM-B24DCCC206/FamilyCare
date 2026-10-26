
package com.familycare.app.presentation.ocr

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.text.Normalizer
import androidx.lifecycle.viewmodel.compose.viewModel
import com.familycare.app.presentation.medication.MedicationViewModel

private const val OCR_TAG = "FamilyCareOCR"

private data class MedicineCatalogEntry(
    val name: String,
    val aliases: List<String>
)

private val medicineCatalog = listOf(
    MedicineCatalogEntry("YESOM 40mg", listOf("YESOM", "ESOMEPRAZOL")),
    MedicineCatalogEntry("SUCRATE GEL", listOf("SUCRATE GEL", "SUCRALFATE")),
    MedicineCatalogEntry("ARTHUR", listOf("ARTHUR", "TRIMEBUTINE")),
    MedicineCatalogEntry("PAPAZE", listOf("PAPAZE")),
    MedicineCatalogEntry("BIOCID MH 3.5+2g", listOf("BIOCID MH", "BIOCID")),
    MedicineCatalogEntry("DIGLUMISAN", listOf("DIGLUMISAN", "L-ARGININE"))
)

private fun normalizeMedicineText(text: String): String {
    return Normalizer.normalize(
        text.uppercase(),
        Normalizer.Form.NFD
    )
        .replace(Regex("\\p{Mn}+"), "")
        .replace('Đ', 'D')
        .replace(Regex("[^A-Z0-9]+"), " ")
        .trim()
}

private fun levenshteinDistance(first: String, second: String): Int {
    var previous = IntArray(second.length + 1) { it }

    for (i in first.indices) {
        val current = IntArray(second.length + 1)
        current[0] = i + 1

        for (j in second.indices) {
            val cost = if (first[i] == second[j]) 0 else 1
            current[j + 1] = minOf(
                current[j] + 1,
                previous[j + 1] + 1,
                previous[j] + cost
            )
        }

        previous = current
    }

    return previous[second.length]
}

private fun findMedicineInLine(line: String): MedicineCatalogEntry? {
    val normalizedLine = normalizeMedicineText(line)
    if (normalizedLine.isBlank()) return null

    val exactMatches = medicineCatalog.mapNotNull { entry ->
        val matchedAlias = entry.aliases
            .map { normalizeMedicineText(it) }
            .filter { alias ->
                Regex("(^| )${Regex.escape(alias)}( |$)")
                    .containsMatchIn(normalizedLine)
            }
            .maxByOrNull { it.length }

        if (matchedAlias != null) entry to matchedAlias.length else null
    }

    if (exactMatches.isNotEmpty()) {
        return exactMatches.maxByOrNull { it.second }?.first
    }

    val tokens = normalizedLine.split(Regex("\\s+"))
        .filter { it.isNotBlank() }

    var bestEntry: MedicineCatalogEntry? = null
    var bestDistance = Int.MAX_VALUE
    var bestLength = 0

    for (entry in medicineCatalog) {
        for (alias in entry.aliases) {
            val expectedTokens = normalizeMedicineText(alias)
                .split(Regex("\\s+"))
                .filter { it.isNotBlank() }

            if (expectedTokens.isEmpty() || tokens.size < expectedTokens.size) {
                continue
            }

            for (start in 0..(tokens.size - expectedTokens.size)) {
                val actualTokens = tokens.subList(
                    start,
                    start + expectedTokens.size
                )

                var totalDistance = 0
                var acceptable = true

                for (i in expectedTokens.indices) {
                    val expected = expectedTokens[i]
                    val actual = actualTokens[i]

                    val allowed = when {
                        expected.length < 4 -> 0
                        expected.length < 9 -> 1
                        else -> 2
                    }

                    if (kotlin.math.abs(expected.length - actual.length) > allowed) {
                        acceptable = false
                        break
                    }

                    val distance = levenshteinDistance(expected, actual)
                    if (distance > allowed) {
                        acceptable = false
                        break
                    }

                    totalDistance += distance
                }

                if (!acceptable) continue

                val aliasLength = expectedTokens.sumOf { it.length }

                if (
                    totalDistance < bestDistance ||
                    (totalDistance == bestDistance && aliasLength > bestLength)
                ) {
                    bestEntry = entry
                    bestDistance = totalDistance
                    bestLength = aliasLength
                }
            }
        }
    }

    return bestEntry
}

private fun suggestMedicineName(text: String): String? {
    return findMedicineInLine(text)?.name
}

private fun extractMedicineCandidates(text: String): List<String> {
    val result = mutableListOf<String>()
    val lines = text.lines()
        .map { it.trim() }
        .filter { it.isNotBlank() }

    val numberedStart = Regex("""^\s*(\d{1,2})\s*[.)]\s*(.+)$""")
    val dosagePattern = Regex(
        """(?i)\b\d+(?:[.,]\d+)?\s*(?:mg|mcg|g|ml|iu|ui|%)\b"""
    )

    val adminKeywords = listOf(
        "HO TEN", "BENH NHAN", "DIA CHI", "CHAN DOAN",
        "CHUAN DOAN", "NGAY KHAM", "GIOI TINH", "TUOI",
        "BAC SI", "DON THUOC", "DI UNG", "TIEN SU",
        "LOI DAN", "SO DIEN THOAI", "MA BENH NHAN",
        "KHOA", "BENH VIEN", "PHONG KHAM"
    )

    val instructionStarts = listOf(
        "UONG ", "NGAY UONG", "SANG UONG", "CHIEU UONG",
        "TOI UONG", "TRUOC AN", "SAU AN", "SAU KHI AN",
        "MOI NGAY", "LAN MOI NGAY", "DUONG DUNG"
    )

    var current = StringBuilder()

    fun saveCurrent() {
        val value = current.toString().trim()
        if (value.isNotBlank()) result.add(value)
        current = StringBuilder()
    }

    fun isAdministrative(line: String): Boolean {
        val normalized = normalizeMedicineText(line)
        return adminKeywords.any { normalized.contains(it) }
    }

    fun looksLikeMedicine(line: String): Boolean {
        val normalized = normalizeMedicineText(line)
        if (normalized.length < 3 || isAdministrative(line)) return false
        if (instructionStarts.any { normalized.startsWith(it) }) return false

        if (findMedicineInLine(line) != null) return true

        if (dosagePattern.containsMatchIn(line)) {
            val beforeDosage = line.substringBefore(
                dosagePattern.find(line)?.value ?: ""
            ).trim()

            val words = normalizeMedicineText(beforeDosage)
                .split(Regex("\\s+"))
                .filter { it.isNotBlank() }

            return words.any { it.length >= 3 }
        }

        val dosageForms = listOf(
            "VIEN", "CAPSULE", "CAPSULES", "TABLET", "TABLETS",
            "SIRO", "SYRUP", "GEL", "INJECTION", "DUNG DICH",
            "SUSPENSION", "CREAM", "OINTMENT", "DROPS"
        )

        return dosageForms.any { normalized.contains(it) } &&
            normalized.split(Regex("\\s+")).size >= 2
    }

    for (line in lines) {
        if (isAdministrative(line)) continue

        val normalized = normalizeMedicineText(line)
        if (instructionStarts.any { normalized.startsWith(it) }) {
            if (current.isNotEmpty()) current.append("\n").append(line)
            continue
        }

        val numbered = numberedStart.matchEntire(line)
        val numberedContent = numbered?.groupValues?.get(2)?.trim()
        val candidateLine = numberedContent ?: line

        if (numbered != null && looksLikeMedicine(candidateLine)) {
            saveCurrent()
            current.append(candidateLine)
            continue
        }

        if (looksLikeMedicine(candidateLine)) {
            if (current.isNotEmpty()) saveCurrent()
            current.append(candidateLine)
            continue
        }

        val isDosageOrInstruction =
            dosagePattern.containsMatchIn(line) ||
            normalized.startsWith("UONG ") ||
            normalized.startsWith("NGAY ") ||
            normalized.startsWith("SANG ") ||
            normalized.startsWith("CHIEU ") ||
            normalized.startsWith("TOI ") ||
            normalized.startsWith("TRUOC AN") ||
            normalized.startsWith("SAU AN")

        if (current.isNotEmpty() && isDosageOrInstruction) {
            current.append("\n").append(line)
        }
    }

    saveCurrent()
    return result.distinct()
}

@Composable
fun OcrPrescriptionScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val medicationViewModel: MedicationViewModel = viewModel()
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var recognizedText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var showRawText by remember { mutableStateOf(false) }

    val medicineCandidates = remember {
        mutableStateListOf<String>()
    }

    val selectedMedicines = remember {
        mutableStateListOf<Boolean>()
    }

    fun replaceCandidates(candidates: List<String>) {
        medicineCandidates.clear()
        medicineCandidates.addAll(candidates)

        selectedMedicines.clear()
        repeat(candidates.size) {
            selectedMedicines.add(false)
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) {
            Log.d(OCR_TAG, "Người dùng đã hủy chọn ảnh")
        } else {
            selectedUri = uri
            recognizedText = ""
            replaceCandidates(emptyList())
            errorMessage = ""
            showRawText = false
            isProcessing = true

            try {
                val image = InputImage.fromFilePath(context, uri)
                val recognizer = TextRecognition.getClient(
                    TextRecognizerOptions.DEFAULT_OPTIONS
                )

                recognizer.process(image)
                    .addOnSuccessListener { result ->
                        recognizedText = result.text
                        replaceCandidates(
                            extractMedicineCandidates(result.text)
                        )

                        if (result.text.isBlank()) {
                            errorMessage =
                                "Không nhận diện được chữ. Hãy thử ảnh rõ nét hơn."
                        } else if (medicineCandidates.isEmpty()) {
                            errorMessage =
                                "Chưa tách được mục thuốc tự động. Bạn có thể thêm thuốc thủ công."
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.e(OCR_TAG, "Lỗi nhận diện văn bản OCR", exception)
                        errorMessage =
                            "Nhận diện thất bại: ${
                                exception.localizedMessage ?: "Lỗi không xác định"
                            }"
                    }
                    .addOnCompleteListener {
                        isProcessing = false
                        recognizer.close()
                    }
            } catch (exception: Exception) {
                isProcessing = false
                Log.e(OCR_TAG, "Không thể đọc ảnh đơn thuốc", exception)
                errorMessage =
                    "Không thể mở ảnh: ${
                        exception.localizedMessage ?: "Lỗi không xác định"
                    }"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Quét đơn thuốc",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "Chọn ảnh đơn thuốc để nhận diện bằng ML Kit.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Button(
            onClick = {
                Log.d(OCR_TAG, "Đã bấm nút chọn ảnh")

                Toast.makeText(
                    context,
                    "Đang mở trình chọn ảnh...",
                    Toast.LENGTH_SHORT
                ).show()

                try {
                    imagePicker.launch("image/*")
                } catch (exception: Exception) {
                    Log.e(OCR_TAG, "Không thể mở trình chọn ảnh", exception)
                    Toast.makeText(
                        context,
                        "Không thể mở thư viện ảnh: ${
                            exception.localizedMessage ?: "Lỗi không xác định"
                        }",
                        Toast.LENGTH_LONG
                    ).show()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isProcessing
        ) {
            Text(if (selectedUri == null) "Chọn ảnh đơn thuốc" else "Chọn ảnh khác")
        }

        if (selectedUri != null) {
            Text(
                text = "Đã chọn ảnh đơn thuốc",
                style = MaterialTheme.typography.bodySmall
            )
        }

        if (isProcessing) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator()
                Text("Đang nhận diện văn bản...")
            }
        }

        if (errorMessage.isNotBlank()) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        if (recognizedText.isNotBlank() && !isProcessing) {
            HorizontalDivider()

            Text(
                text = "Danh sách mục thuốc",
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = "Đã tách ${medicineCandidates.size} mục. Tên thuốc OCR có thể sai; hãy kiểm tra với đơn gốc.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (medicineCandidates.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Đã chọn ${selectedMedicines.count { it }}/${medicineCandidates.size} mục",
                    modifier = Modifier.padding(top = 12.dp)
                )

                TextButton(
                    onClick = {
                        val selectAll = selectedMedicines.any { !it }
                        selectedMedicines.indices.forEach { index ->
                            selectedMedicines[index] = selectAll
                        }
                    }
                ) {
                    Text(
                        if (selectedMedicines.isNotEmpty() &&
                            selectedMedicines.all { it }
                        ) {
                            "Bỏ chọn tất cả"
                        } else {
                            "Chọn tất cả"
                        }
                    )
                }
            }
        }

        OutlinedButton(
            onClick = {
                medicineCandidates.add("")
                selectedMedicines.add(false)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isProcessing
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Text("Thêm thuốc thủ công")
        }

        medicineCandidates.forEachIndexed { index, medicine ->
            val suggestedName = suggestMedicineName(medicine)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Checkbox(
                                checked = selectedMedicines.getOrElse(index) { false },
                                onCheckedChange = { checked ->
                                    if (index < selectedMedicines.size) {
                                        selectedMedicines[index] = checked
                                    }
                                }
                            )

                            Text(
                                text = "Mục thuốc ${index + 1}",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(top = 12.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                medicineCandidates.removeAt(index)
                                if (index < selectedMedicines.size) {
                                    selectedMedicines.removeAt(index)
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Xóa mục thuốc"
                            )
                        }
                    }

                    if (suggestedName != null) {
                        Text(
                            text = "Tên thuốc gợi ý: $suggestedName",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    OutlinedTextField(
                        value = medicine,
                        onValueChange = { updated ->
                            medicineCandidates[index] = updated
                        },
                        label = { Text("Tên thuốc, liều lượng, cách dùng") },
                        placeholder = { Text("Nhập thông tin thuốc tại đây...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            }
        }

        if (recognizedText.isNotBlank() && !isProcessing) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(
                            onClick = { showRawText = !showRawText }
                        ) {
                            Text("Xem văn bản OCR gốc")
                        }

                        IconButton(
                            onClick = { showRawText = !showRawText }
                        ) {
                            Icon(
                                imageVector = if (showRawText) {
                                    Icons.Default.ExpandLess
                                } else {
                                    Icons.Default.ExpandMore
                                },
                                contentDescription = if (showRawText) {
                                    "Thu gọn văn bản OCR"
                                } else {
                                    "Mở rộng văn bản OCR"
                                }
                            )
                        }
                    }

                    AnimatedVisibility(visible = showRawText) {
                        OutlinedTextField(
                            value = recognizedText,
                            onValueChange = { updated ->
                                recognizedText = updated
                                replaceCandidates(
                                    extractMedicineCandidates(updated)
                                )
                            },
                            label = { Text("Văn bản OCR gốc") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 5
                        )
                    }
                }
            }
        }
        
        Button(
            onClick = {
                val selectedItems = medicineCandidates
                    .mapIndexedNotNull { index, medicine ->
                        if (
                            selectedMedicines.getOrElse(index) { false } &&
                            medicine.isNotBlank()
                        ) {
                            medicine.trim()
                        } else {
                            null
                        }
                    }

                if (selectedItems.isEmpty()) {
                    Toast.makeText(
                        context,
                        "Vui lòng tích chọn ít nhất một mục thuốc",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    medicationViewModel.addOcrMedicines(selectedItems)

                    Toast.makeText(
                        context,
                        "Đã gửi ${selectedItems.size} mục thuốc để lưu",
                        Toast.LENGTH_SHORT
                    ).show()

                    onBack()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isProcessing && medicineCandidates.isNotEmpty()
        ) {
            Text(
                "Lưu ${selectedMedicines.count { it }} thuốc đã chọn"
            )
        }


        Text(
            text = "OCR có thể nhận diện sai hoặc bỏ sót thuốc. Hãy đối chiếu tên thuốc, liều lượng và cách dùng với đơn gốc. Các mục đã tích chọn hiện chưa được lưu vào cơ sở dữ liệu.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Quay lại")
        }
    }
}
