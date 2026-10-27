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
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume

/**
 * Trình quản lý sự cố khẩn cấp (EmergencyManager):
 * 1. Tự động định vị ngầm thời gian thực bằng FusedLocationProviderClient (Chạy 100% ngầm, không hộp thoại).
 * 2. Phân giải địa chỉ tiếng Việt qua Android Geocoder (Không dùng địa chỉ giả lập hardcode).
 * 3. Cơ chế Fallback tức thời: Ưu tiên GPS chính xác cao, tự động lấy nhanh vị trí cache getLastLocation().
 * 4. Kích hoạt còi hú âm lượng tối đa và rung dồn dập qua EmergencySirenController.
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

    @Volatile
    private var lastCachedCoords: Pair<Double, Double>? = null

    /**
     * Lấy vị trí gần nhất trong bộ nhớ cache phần cứng cực nhanh (< 50ms)
     * Dùng cho cơ chế Fallback tức thời khi người dùng bấm SOS mà GPS chưa fix xong.
     */
    @SuppressLint("MissingPermission")
    suspend fun getLastKnownLocationQuickly(): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        // Nếu đã có cache gần đây trong phiên
        lastCachedCoords?.let { return@withContext it }

        if (!hasLocationPermission()) return@withContext null

        suspendCancellableCoroutine { continuation ->
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { loc: Location? ->
                    if (loc != null) {
                        val coords = Pair(loc.latitude, loc.longitude)
                        lastCachedCoords = coords
                        Log.d(TAG, "Lấy lastLocation cache thành công: ${coords.first}, ${coords.second}")
                        continuation.resume(coords)
                    } else {
                        continuation.resume(null)
                    }
                }.addOnFailureListener {
                    continuation.resume(null)
                }
            } catch (_: Exception) {
                continuation.resume(null)
            }
        }
    }

    /**
     * Tự động định vị ngầm thời gian thực với độ chính xác cao (Priority.PRIORITY_HIGH_ACCURACY).
     * Có timeout 4500ms tự ngắt để tránh treo luồng, tự động fallback sang lastLocation nếu GPS fix chậm.
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentGpsLocation(timeoutMs: Long = 4500L): Pair<Double, Double>? = withTimeoutOrNull(timeoutMs) {
        if (!hasLocationPermission()) {
            Log.w(TAG, "Chưa được cấp quyền truy cập vị trí.")
            return@withTimeoutOrNull null
        }

        suspendCancellableCoroutine { continuation ->
            val cancellationTokenSource = CancellationTokenSource()
            continuation.invokeOnCancellation {
                cancellationTokenSource.cancel()
            }

            try {
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cancellationTokenSource.token
                ).addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        val coords = Pair(location.latitude, location.longitude)
                        lastCachedCoords = coords
                        Log.d(TAG, "Định vị ngầm GPS thành công: ${coords.first}, ${coords.second}")
                        continuation.resume(coords)
                    } else {
                        // Nếu chưa bắt được GPS tức thời, lấy vị trí cache gần nhất
                        fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc: Location? ->
                            if (lastLoc != null) {
                                val coords = Pair(lastLoc.latitude, lastLoc.longitude)
                                lastCachedCoords = coords
                                Log.d(TAG, "Fallback sang lastLocation thành công: ${coords.first}, ${coords.second}")
                                continuation.resume(coords)
                            } else {
                                continuation.resume(null)
                            }
                        }.addOnFailureListener {
                            continuation.resume(null)
                        }
                    }
                }.addOnFailureListener { e ->
                    Log.e(TAG, "Lỗi getCurrentLocation, thử lấy lastLocation", e)
                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc: Location? ->
                        if (lastLoc != null) {
                            val coords = Pair(lastLoc.latitude, lastLoc.longitude)
                            lastCachedCoords = coords
                            continuation.resume(coords)
                        } else {
                            continuation.resume(null)
                        }
                    }.addOnFailureListener {
                        continuation.resume(null)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ngoại lệ khi gọi FusedLocationProviderClient", e)
                continuation.resume(null)
            }
        }
    } ?: run {
        // Nếu timeout 4500ms, fallback ngay sang lastLocation cache
        getLastKnownLocationQuickly()
    }

    private fun hasLocationPermission(): Boolean {
        val hasFine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return hasFine || hasCoarse
    }

    /**
     * Phân giải địa chỉ tiếng Việt qua Android Geocoder (Hoàn toàn không dùng địa chỉ mẫu giả lập).
     */
    suspend fun resolveAddress(latitude: Double, longitude: Double): String = withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale.Builder().setLanguage("vi").setRegion("VN").build())
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

        "Vị trí GPS: %.5f° N, %.5f° E".format(Locale.US, latitude, longitude)
    }

    fun startSirenAlarm() {
        EmergencySirenController.startSiren(context)
    }

    fun stopSirenAlarm() {
        EmergencySirenController.stopSiren(context)
    }

    fun isSirenPlaying(): Boolean = EmergencySirenController.isSirenPlaying()

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
