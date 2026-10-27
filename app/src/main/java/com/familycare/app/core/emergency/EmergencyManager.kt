package com.familycare.app.core.emergency

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume

/**
 * Trình quản lý sự cố khẩn cấp (EmergencyManager):
 * 1. Bắt tọa độ GPS độ chính xác cao bằng FusedLocationProviderClient thời gian thực.
 * 2. Phân giải địa chỉ tiếng Việt qua Android Geocoder (Không dùng địa chỉ giả lập hardcode).
 * 3. Kích hoạt còi hú âm lượng tối đa và rung dồn dập qua EmergencySirenController.
 * 4. Đóng gói EmergencyPayload sẵn sàng gửi qua Firebase Cloud Messaging (FCM).
 */
class EmergencyManager(
    private val context: Context
) {
    companion object {
        private const val TAG = "EmergencyManager"
    }

    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    /**
     * Lấy tọa độ GPS chính xác thời gian thực bằng FusedLocationProviderClient
     * Xóa bỏ hoàn toàn tọa độ hardcoded Lê Thánh Tôn.
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentGpsLocation(): Pair<Double, Double>? = suspendCancellableCoroutine { continuation ->
        val hasFine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            Log.w(TAG, "Chưa được cấp quyền truy cập vị trí (ACCESS_FINE_LOCATION / ACCESS_COARSE_LOCATION)")
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        val cancellationTokenSource = CancellationTokenSource()
        continuation.invokeOnCancellation {
            cancellationTokenSource.cancel()
        }

        try {
            // Ưu tiên lấy tọa độ GPS tức thời thời gian thực với độ chính xác cao nhất
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            ).addOnSuccessListener { location: Location? ->
                if (location != null) {
                    Log.d(TAG, "Lấy GPS thời gian thực thành công: ${location.latitude}, ${location.longitude}")
                    continuation.resume(Pair(location.latitude, location.longitude))
                } else {
                    // Nếu chưa bắt được GPS ngay, thử lấy vị trí gần nhất đã lưu trong cache phần cứng
                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc: Location? ->
                        if (lastLoc != null) {
                            Log.d(TAG, "Lấy lastLocation thành công: ${lastLoc.latitude}, ${lastLoc.longitude}")
                            continuation.resume(Pair(lastLoc.latitude, lastLoc.longitude))
                        } else {
                            Log.w(TAG, "Không lấy được vị trí GPS thời gian thực từ thiết bị.")
                            continuation.resume(null)
                        }
                    }.addOnFailureListener { e ->
                        Log.e(TAG, "Lỗi lấy lastLocation", e)
                        continuation.resume(null)
                    }
                }
            }.addOnFailureListener { e ->
                Log.e(TAG, "Lỗi getCurrentLocation", e)
                continuation.resume(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ngoại lệ khi gọi Location Provider", e)
            continuation.resume(null)
        }
    }

    /**
     * Tra cứu địa chỉ số nhà, tên đường, phường/xã, quận/huyện, tỉnh/thành phố thực tế từ tọa độ GPS.
     * Hoàn toàn không dùng địa chỉ mẫu giả lập.
     */
    suspend fun resolveAddress(latitude: Double, longitude: Double): String = withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale("vi", "VN"))
            @Suppress("DEPRECATION")
            val addresses: List<Address>? = geocoder.getFromLocation(latitude, longitude, 1)

            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                val fullAddress = (0..addr.maxAddressLineIndex)
                    .mapNotNull { addr.getAddressLine(it) }
                    .joinToString(", ")

                if (fullAddress.isNotBlank()) {
                    return@withContext fullAddress
                }

                // Nếu getAddressLine rỗng, ghép từ các thành phần chi tiết
                val parts = listOfNotNull(
                    addr.featureName,
                    addr.thoroughfare,
                    addr.subAdminArea,
                    addr.adminArea
                ).distinct()

                if (parts.isNotEmpty()) {
                    return@withContext parts.joinToString(", ")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi phân giải địa chỉ bằng Geocoder", e)
        }

        // Nếu Geocoder không có kết nối hoặc thiết bị thiếu backend định vị, hiển thị tọa độ cụ thể
        "Vị trí GPS: %.5f° N, %.5f° E".format(Locale.US, latitude, longitude)
    }

    /**
     * Kích hoạt còi hú báo động âm lượng tối đa 100% và rung khẩn cấp
     */
    fun startSirenAlarm() {
        EmergencySirenController.startSiren(context)
    }

    /**
     * Dừng còi hú báo động, ngắt rung và khôi phục âm lượng ban đầu
     */
    fun stopSirenAlarm() {
        EmergencySirenController.stopSiren(context)
    }

    fun isSirenPlaying(): Boolean = EmergencySirenController.isSirenPlaying()

    /**
     * Đóng gói Payload thông báo khẩn cấp gửi qua Firebase Cloud Messaging (FCM Data Message)
     */
    fun createEmergencyPayload(
        userId: String,
        senderName: String,
        senderPhone: String,
        familyId: String,
        latitude: Double,
        longitude: Double,
        address: String,
        bloodType: String = "",
        medicalConditions: String = "",
        allergies: String = "",
        familyOwnerName: String = "",
        familyOwnerPhone: String = ""
    ): EmergencyPayload {
        return EmergencyPayload(
            eventId = UUID.randomUUID().toString(),
            userId = userId,
            senderName = senderName,
            senderPhone = senderPhone,
            familyId = familyId,
            latitude = latitude,
            longitude = longitude,
            address = address,
            bloodType = bloodType,
            medicalConditions = medicalConditions,
            allergies = allergies,
            timestamp = System.currentTimeMillis(),
            familyOwnerName = familyOwnerName,
            familyOwnerPhone = familyOwnerPhone
        )
    }
}
