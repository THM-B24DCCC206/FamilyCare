package com.familycare.app.presentation.biometrics.ppg

import kotlin.math.max
import kotlin.math.min

class PpgSignalProcessor {

    private val timeSeries = mutableListOf<Pair<Long, Double>>() // (timestamp, rawIntensity)
    private val windowDurationMs = 4500L // Cửa sổ trượt 4.5 giây
    private var lastPeakTime = 0L
    private val intervals = mutableListOf<Long>() // Khoảng cách giữa các nhịp tâm thu (ms)

    /**
     * Thêm một điểm dữ liệu mới từ Camera
     * @return Nhịp tim tức thời (BPM) hoặc 0 nếu đang thu thập dữ liệu
     */
    fun addSample(timestamp: Long, redIntensity: Double): Int {
        timeSeries.add(Pair(timestamp, redIntensity))

        // Duy trì dữ liệu trong cửa sổ thời gian
        while (timeSeries.isNotEmpty() && timestamp - timeSeries.first().first > windowDurationMs) {
            timeSeries.removeAt(0)
        }

        if (timeSeries.size < 40) return 0 // Cần ít nhất 40 frame (~1.3 giây)

        // 1. ÁP DỤNG BỘ LỌC LÀM MỊN (MOVING AVERAGE FILTER) ĐỂ LOẠI BỎ SÓNG PHỤ (DICROTIC NOTCH)
        val smoothed = applyMovingAverage(timeSeries, kernelSize = 5)

        // 2. LOẠI BỎ THÀNH PHẦN TĨNH DC (ZERO-CENTERED NORMALIZATION)
        val mean = smoothed.map { it.second }.average()
        val normalized = smoothed.map { Pair(it.first, it.second - mean) }

        // 3. TÌM ĐỈNH TÂM THU CHÍNH (SYSTOLIC PEAK DETECTION)
        detectSystolicPeaks(normalized)

        // 4. TÍNH TOÁN BPM CHUẨN XÁC
        return calculateBpm()
    }

    private fun applyMovingAverage(
        samples: List<Pair<Long, Double>>,
        kernelSize: Int
    ): List<Pair<Long, Double>> {
        val result = mutableListOf<Pair<Long, Double>>()
        val halfKernel = kernelSize / 2

        for (i in samples.indices) {
            val start = max(0, i - halfKernel)
            val end = min(samples.size - 1, i + halfKernel)
            var sum = 0.0
            for (j in start..end) {
                sum += samples[j].second
            }
            result.add(Pair(samples[i].first, sum / (end - start + 1)))
        }
        return result
    }

    private fun detectSystolicPeaks(samples: List<Pair<Long, Double>>) {
        if (samples.size < 7) return

        val maxVal = samples.maxOf { it.second }
        if (maxVal <= 0) return

        // Đỉnh phải vượt qua ngưỡng biên độ tối thiểu (60% của max) để loại hẳn sóng phụ
        val threshold = maxVal * 0.60

        for (i in 3 until samples.size - 3) {
            val curr = samples[i].second
            val time = samples[i].first

            // Kiểm tra đỉnh cục bộ trong phạm vi 3 mẫu lân cận
            val isPeak = curr > threshold &&
                    curr > samples[i - 1].second && curr > samples[i - 2].second && curr > samples[i - 3].second &&
                    curr > samples[i + 1].second && curr > samples[i + 2].second && curr > samples[i + 3].second

            if (isPeak) {
                // THỜI KỲ TRƠ SINH LÝ: Khoảng cách giữa 2 nhịp tim thật khi nghỉ ngơi
                // phải tối thiểu từ 450ms đến 1400ms (tương đương 43 - 133 BPM).
                // Bất kỳ dao động nào dưới 450ms đều là sóng dội (Dicrotic wave) hoặc nhiễu run tay -> BỎ QUA!
                val diff = time - lastPeakTime
                if (lastPeakTime == 0L || diff in 450..1400) {
                    if (lastPeakTime != 0L) {
                        intervals.add(diff)
                        if (intervals.size > 10) intervals.removeAt(0)
                    }
                    lastPeakTime = time
                }
            }
        }
    }

    private fun calculateBpm(): Int {
        if (intervals.size < 4) return 0

        // BỘ LỌC TRUNG VỊ (MEDIAN): Loại bỏ các giá trị quá lớn hoặc quá nhỏ do rung tay
        val sorted = intervals.sorted()
        // Lấy 50% các giá trị ở giữa để tính trung bình
        val trimCount = (sorted.size * 0.25).toInt()
        val coreIntervals = sorted.subList(trimCount, sorted.size - trimCount)

        val avgInterval = if (coreIntervals.isNotEmpty()) coreIntervals.average() else intervals.average()
        if (avgInterval <= 0) return 0

        val bpm = (60000.0 / avgInterval).toInt()
        return min(max(bpm, 55), 130) // Giới hạn nhịp tim nghỉ ngơi chuẩn y tế (55 - 130 BPM)
    }

    fun reset() {
        timeSeries.clear()
        intervals.clear()
        lastPeakTime = 0L
    }
}
