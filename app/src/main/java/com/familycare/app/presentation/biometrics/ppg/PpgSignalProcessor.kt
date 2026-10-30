package com.familycare.app.presentation.biometrics.ppg

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class PpgSignalProcessor {

    private val timeSeries = mutableListOf<Pair<Long, Double>>() // (timestamp, rawIntensity)
    private val windowDurationMs = 4500L // Cửa sổ trượt 4.5 giây
    private val bpmBuffer = mutableListOf<Int>() // Lịch sử BPM gần nhất để làm mịn mượt

    /**
     * Thêm một điểm dữ liệu mới từ Camera ImageAnalysis
     * @return Nhịp tim tức thời (BPM) hoặc 0 nếu đang trong giai đoạn thu thập dữ liệu / tín hiệu chưa đủ ổn định
     */
    fun addSample(timestamp: Long, redIntensity: Double): Int {
        timeSeries.add(Pair(timestamp, redIntensity))

        // Duy trì dữ liệu trong cửa sổ thời gian 4.5 giây
        while (timeSeries.isNotEmpty() && timestamp - timeSeries.first().first > windowDurationMs) {
            timeSeries.removeAt(0)
        }

        // Cần tối thiểu 35 frame (~1.1 giây ở 30 FPS) để bắt đầu phân tích xung
        if (timeSeries.size < 35) return 0

        // 1. LOẠI BỎ THÀNH PHẦN TĨNH DC BẰNG ĐƯỜNG ĐÁY DI ĐỘNG (MOVING BASELINE DETRENDING)
        // Giúp loại bỏ hoàn toàn hiện tượng trôi mức tín hiệu do rung tay hay thay đổi áp lực tì ngón tay
        val acSignal = removeDcTrend(timeSeries, baselineWindow = 25)

        // 2. ÁP DỤNG BỘ LỌC LÀM MỊN (MOVING AVERAGE) ĐỂ LOẠI NHIỄU SENSOR CAMERA
        val smoothed = applyMovingAverage(acSignal, kernelSize = 3)

        // 3. TÌM ĐỈNH TÂM THU CHÍNH (SYSTOLIC PEAK DETECTION) VỚI THỜI KỲ TRƠ SINH LÝ
        val peaks = detectSystolicPeaks(smoothed)

        // 4. TÍNH TOÁN KHOẢNG CÁCH PEAK-TO-PEAK (RR-INTERVAL) VÀ QUY ĐỔI RA BPM
        return calculateBpmFromPeaks(peaks)
    }

    /**
     * Tách thành phần xoay chiều AC bằng cách trừ đường đáy di động (Moving Baseline)
     */
    private fun removeDcTrend(
        samples: List<Pair<Long, Double>>,
        baselineWindow: Int
    ): List<Pair<Long, Double>> {
        val result = ArrayList<Pair<Long, Double>>(samples.size)
        val halfWin = baselineWindow / 2

        for (i in samples.indices) {
            val start = max(0, i - halfWin)
            val end = min(samples.size - 1, i + halfWin)
            var sum = 0.0
            for (j in start..end) {
                sum += samples[j].second
            }
            val baseline = sum / (end - start + 1)
            result.add(Pair(samples[i].first, samples[i].second - baseline))
        }
        return result
    }

    /**
     * Làm mịn màng sóng quang học mao mạch
     */
    private fun applyMovingAverage(
        samples: List<Pair<Long, Double>>,
        kernelSize: Int
    ): List<Pair<Long, Double>> {
        val result = ArrayList<Pair<Long, Double>>(samples.size)
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

    /**
     * Nhận diện các đỉnh tâm thu (Systolic Peaks) trong cửa sổ phân tích hiện tại
     */
    private fun detectSystolicPeaks(samples: List<Pair<Long, Double>>): List<Pair<Long, Double>> {
        if (samples.size < 10) return emptyList()

        val maxVal = samples.maxOf { it.second }
        val minVal = samples.minOf { it.second }
        val amplitude = maxVal - minVal

        // Nếu biên độ sóng quá nhỏ (< 0.25) -> Không có mạch đập rõ ràng
        if (amplitude < 0.25 || maxVal <= 0) return emptyList()

        // Ngưỡng phát hiện đỉnh: nằm ở 35% biên độ dương
        val threshold = maxVal * 0.35

        val candidatePeaks = mutableListOf<Pair<Long, Double>>()
        for (i in 2 until samples.size - 2) {
            val curr = samples[i].second
            if (curr > threshold && curr > 0) {
                // Kiểm tra cực đại cục bộ trong phạm vi 2 mẫu lân cận trước và sau
                val isLocalMax = curr >= samples[i - 1].second && curr >= samples[i - 2].second &&
                        curr > samples[i + 1].second && curr >= samples[i + 2].second
                if (isLocalMax) {
                    candidatePeaks.add(samples[i])
                }
            }
        }

        if (candidatePeaks.isEmpty()) return emptyList()

        // Lọc theo thời kỳ trơ sinh lý: 2 nhịp tim thật không thể gần nhau dưới 380ms (~158 BPM)
        val acceptedPeaks = mutableListOf<Pair<Long, Double>>()
        for (candidate in candidatePeaks) {
            if (acceptedPeaks.isEmpty()) {
                acceptedPeaks.add(candidate)
            } else {
                val lastPeak = acceptedPeaks.last()
                val diffTime = candidate.first - lastPeak.first
                if (diffTime < 380L) {
                    // Nếu quá gần nhau, giữ lại đỉnh có biên độ lớn hơn (đỉnh chính)
                    if (candidate.second > lastPeak.second) {
                        acceptedPeaks[acceptedPeaks.size - 1] = candidate
                    }
                } else {
                    acceptedPeaks.add(candidate)
                }
            }
        }

        return acceptedPeaks
    }

    /**
     * Tính toán BPM từ danh sách các đỉnh tâm thu
     */
    private fun calculateBpmFromPeaks(peaks: List<Pair<Long, Double>>): Int {
        if (peaks.size < 2) return 0

        val intervals = mutableListOf<Long>()
        for (k in 0 until peaks.size - 1) {
            val interval = peaks[k + 1].first - peaks[k].first
            // Giới hạn khoảng cách sinh lý nhịp tim người (40 BPM tương đương 1500ms, 160 BPM tương đương 375ms)
            if (interval in 380L..1500L) {
                intervals.add(interval)
            }
        }

        if (intervals.size < 2) return 0

        // Lấy khoảng thời gian trung bình (có loại trừ ngoại lai nếu từ 4 nhịp trở lên)
        val sorted = intervals.sorted()
        val coreIntervals = if (sorted.size >= 4) {
            sorted.subList(1, sorted.size - 1) // Bỏ bớt giá trị nhỏ nhất và lớn nhất (Trimmed Mean)
        } else {
            sorted
        }

        val avgInterval = coreIntervals.average()
        if (avgInterval <= 0) return 0

        val instantBpm = (60000.0 / avgInterval).roundToInt().coerceIn(45, 160)

        // Làm mịn với bộ đệm lịch sử ngắn gần nhất để số đo hiển thị êm ái trên UI
        bpmBuffer.add(instantBpm)
        if (bpmBuffer.size > 5) bpmBuffer.removeAt(0)

        return bpmBuffer.average().roundToInt()
    }

    fun reset() {
        timeSeries.clear()
        bpmBuffer.clear()
    }
}
