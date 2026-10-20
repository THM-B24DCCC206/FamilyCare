package com.familycare.app.presentation.biometrics.ppg

import kotlin.math.max
import kotlin.math.min

class PpgSignalProcessor {

    private val timeSeries = mutableListOf<Pair<Long, Double>>() // (timestamp, redValue)
    private val windowDurationMs = 4000L // Cửa sổ trượt 4 giây để tính nhịp tim
    private var lastPeakTime = 0L
    private val intervals = mutableListOf<Long>() // Khoảng thời gian giữa các nhịp (ms)

    /**
     * Thêm một điểm dữ liệu mới từ camera
     * @return Nhịp tim tức thời (BPM) hoặc 0 nếu chưa đủ dữ liệu
     */
    fun addSample(timestamp: Long, redIntensity: Double): Int {
        timeSeries.add(Pair(timestamp, redIntensity))

        // Giữ lại dữ liệu trong khoảng thời gian windowDurationMs
        while (timeSeries.isNotEmpty() && timestamp - timeSeries.first().first > windowDurationMs) {
            timeSeries.removeAt(0)
        }

        if (timeSeries.size < 30) return 0 // Cần ít nhất ~1 giây dữ liệu (30 FPS)

        // Tính trung bình để loại bỏ thành phần tĩnh DC
        val mean = timeSeries.map { it.second }.average()
        val normalized = timeSeries.map { Pair(it.first, it.second - mean) }

        // Tìm các đỉnh sóng (Peak Detection)
        detectPeaks(normalized)

        // Tính BPM dựa trên trung bình khoảng cách giữa các đỉnh gần nhất
        return calculateBpm()
    }

    private fun detectPeaks(samples: List<Pair<Long, Double>>) {
        if (samples.size < 5) return

        // Ngưỡng phát hiện đỉnh
        val maxVal = samples.maxOf { it.second }
        val threshold = maxVal * 0.45

        for (i in 2 until samples.size - 2) {
            val prev2 = samples[i - 2].second
            val prev1 = samples[i - 1].second
            val curr = samples[i].second
            val next1 = samples[i + 1].second
            val next2 = samples[i + 2].second
            val time = samples[i].first

            // Điểm i là đỉnh cục bộ và vượt ngưỡng
            if (curr > threshold && curr > prev1 && curr > prev2 && curr > next1 && curr > next2) {
                // Khoảng cách tối thiểu giữa 2 nhịp là 300ms (tương đương max 200 BPM)
                if (lastPeakTime == 0L || (time - lastPeakTime) in 300..1500) {
                    if (lastPeakTime != 0L) {
                        intervals.add(time - lastPeakTime)
                        if (intervals.size > 8) intervals.removeAt(0)
                    }
                    lastPeakTime = time
                }
            }
        }
    }

    private fun calculateBpm(): Int {
        if (intervals.size < 3) return 0
        // Loại bỏ ngoại lai, tính trung bình các khoảng nhịp (RR intervals)
        val sorted = intervals.sorted()
        val trimmed = sorted.subList(1, sorted.size - 1) // Bỏ min và max
        val avgInterval = if (trimmed.isNotEmpty()) trimmed.average() else intervals.average()

        if (avgInterval <= 0) return 0
        val bpm = (60000.0 / avgInterval).toInt()
        return min(max(bpm, 50), 160) // Giới hạn nhịp tim sinh lý người 50 - 160 BPM
    }

    fun reset() {
        timeSeries.clear()
        intervals.clear()
        lastPeakTime = 0L
    }
}
