package com.familycare.app.domain.model

import android.location.Location

/**
 * Mô hình dữ liệu bệnh viện / trung tâm y tế cấp cứu ở gần
 */
data class NearbyHospital(
    val id: String,
    val name: String,
    val address: String,
    val distanceKm: Double,
    val hotline: String,
    val emergencyRoom: String,
    val latitude: Double,
    val longitude: Double,
    val is24h: Boolean = true
) {
    val googleMapsUrl: String
        get() = "https://www.google.com/maps/dir/?api=1&destination=$latitude,$longitude"

    val googleMapsSearchUrl: String
        get() = "https://www.google.com/maps/search/?api=1&query=${name.replace(" ", "+")}"
}

/**
 * Bộ cung cấp danh sách bệnh viện gần nhất dựa trên vị trí GPS thực tế
 */
object NearbyHospitalProvider {
    private val ALL_HOSPITALS = listOf(
        // Miền Bắc (Hà Nội và lân cận)
        NearbyHospital(
            id = "bv_bach_mai",
            name = "Bệnh viện Bạch Mai (Trung tâm Cấp cứu A9)",
            address = "78 Giải Phóng, Phương Mai, Đống Đa, Hà Nội",
            distanceKm = 0.0,
            hotline = "0969851616",
            emergencyRoom = "Khoa Hồi sức Cấp cứu A9 - 24/7",
            latitude = 20.9996,
            longitude = 105.8415
        ),
        NearbyHospital(
            id = "bv_viet_duc",
            name = "Bệnh viện Hữu nghị Việt Đức",
            address = "40 Tràng Thi, Hàng Bông, Hoàn Kiếm, Hà Nội",
            distanceKm = 0.0,
            hotline = "02438253531",
            emergencyRoom = "Trung tâm Cấp cứu & Ngoại khoa",
            latitude = 21.0285,
            longitude = 105.8492
        ),
        NearbyHospital(
            id = "bv_dhy_hn",
            name = "Bệnh viện Đại học Y Hà Nội",
            address = "1 Tôn Thất Tùng, Kim Liên, Đống Đa, Hà Nội",
            distanceKm = 0.0,
            hotline = "0982873112",
            emergencyRoom = "Khoa Cấp cứu & Can thiệp Tim mạch",
            latitude = 21.0028,
            longitude = 105.8324
        ),
        NearbyHospital(
            id = "bv_108",
            name = "Bệnh viện TW Quân đội 108",
            address = "Số 1 Trần Hưng Đạo, Bạch Đằng, Hai Bà Trưng, Hà Nội",
            distanceKm = 0.0,
            hotline = "069572400",
            emergencyRoom = "Trung tâm Hồi sức Cấp cứu 24/7",
            latitude = 21.0182,
            longitude = 105.8601
        ),
        NearbyHospital(
            id = "bv_103_ha_dong",
            name = "Bệnh viện Quân y 103 (Hà Đông)",
            address = "261 Phùng Hưng, Phúc La, Hà Đông, Hà Nội",
            distanceKm = 0.0,
            hotline = "0967811616",
            emergencyRoom = "Trung tâm Cấp cứu & Hồi sức Tích cực",
            latitude = 20.9632,
            longitude = 105.7876
        ),
        // Miền Trung (Đà Nẵng & Huế)
        NearbyHospital(
            id = "bv_da_nang",
            name = "Bệnh viện Đà Nẵng",
            address = "124 Hải Phòng, Thạch Thang, Hải Châu, Đà Nẵng",
            distanceKm = 0.0,
            hotline = "02363821118",
            emergencyRoom = "Khoa Cấp cứu 24/7",
            latitude = 16.0712,
            longitude = 108.2134
        ),
        NearbyHospital(
            id = "bv_tw_hue",
            name = "Bệnh viện Trung ương Huế",
            address = "16 Lê Lợi, Vĩnh Ninh, TP. Huế",
            distanceKm = 0.0,
            hotline = "02343822325",
            emergencyRoom = "Trung tâm Cấp cứu Đa khoa",
            latitude = 16.4637,
            longitude = 107.5855
        ),
        // Miền Nam (TP. Hồ Chí Minh & Cần Thơ)
        NearbyHospital(
            id = "bv_cho_ray",
            name = "Bệnh viện Chợ Rẫy",
            address = "201B Nguyễn Chí Thanh, Phường 12, Quận 5, TP.HCM",
            distanceKm = 0.0,
            hotline = "02838554137",
            emergencyRoom = "Khoa Cấp cứu Hồi sức Đa khoa 24/7",
            latitude = 10.7578,
            longitude = 106.6598
        ),
        NearbyHospital(
            id = "bv_dhy_d_hcm",
            name = "Bệnh viện Đại học Y Dược TP.HCM",
            address = "215 Hồng Bàng, Phường 11, Quận 5, TP.HCM",
            distanceKm = 0.0,
            hotline = "02838554269",
            emergencyRoom = "Khoa Cấp cứu Đột quỵ & Tim mạch",
            latitude = 10.7554,
            longitude = 106.6582
        ),
        NearbyHospital(
            id = "bv_nhan_dan_115",
            name = "Bệnh viện Nhân dân 115",
            address = "527 Sư Vạn Hạnh, Phường 12, Quận 10, TP.HCM",
            distanceKm = 0.0,
            hotline = "02838652368",
            emergencyRoom = "Trung tâm Đột quỵ & Cấp cứu Tim mạch",
            latitude = 10.7725,
            longitude = 106.6667
        ),
        NearbyHospital(
            id = "bv_thong_nhat",
            name = "Bệnh viện Thống Nhất",
            address = "01 Lý Thường Kiệt, Phường 7, Tân Bình, TP.HCM",
            distanceKm = 0.0,
            hotline = "02838690277",
            emergencyRoom = "Khoa Cấp cứu & Hồi sức Tích cực",
            latitude = 10.7915,
            longitude = 106.6562
        ),
        NearbyHospital(
            id = "bv_tw_can_tho",
            name = "Bệnh viện Đa khoa TW Cần Thơ",
            address = "315 Nguyễn Văn Linh, An Khánh, Ninh Kiều, Cần Thơ",
            distanceKm = 0.0,
            hotline = "02923820071",
            emergencyRoom = "Khoa Cấp cứu 24/7",
            latitude = 10.0276,
            longitude = 105.7684
        )
    )

    fun getNearbyHospitals(userLat: Double, userLng: Double): List<NearbyHospital> {
        val calculated = ALL_HOSPITALS.map { hospital ->
            val results = FloatArray(1)
            Location.distanceBetween(userLat, userLng, hospital.latitude, hospital.longitude, results)
            val distanceKm = Math.round((results[0] / 1000.0) * 10.0) / 10.0
            hospital.copy(distanceKm = distanceKm)
        }.sortedBy { it.distanceKm }

        return calculated.take(4)
    }
}
