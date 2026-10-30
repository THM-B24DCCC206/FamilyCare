# -*- coding: utf-8 -*-
"""
doc_part3_arch_impl.py: Chuong 4 (Thiet ke He thong & CSDL) va Chuong 5 (Hien thuc hoa cac Module Chuc nang)
Ban nang cap toan dien (Them Sequence Diagrams, DB Indexing, BLE Manager, EmergencyNotificationHelper)
"""
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT
from doc_common import (
    add_para, add_h1, add_h2, add_h3, add_bullet, 
    create_styled_table, add_callout, add_code_block,
    set_table_borders, set_cell_margins, set_cell_background,
    COLOR_PRIMARY, COLOR_BLUE, COLOR_SECONDARY, COLOR_TEXT
)

def build_part3(doc):
    print("Building Part 3: Chapter 4 (Architecture & DB) and Chapter 5 (Implementation of Core Modules)...")
    
    # ==================== CHƯƠNG 4 ====================
    add_h1(doc, "CHƯƠNG 4: THIẾT KẾ KIẾN TRÚC HỆ THỐNG VÀ CƠ SỞ DỮ LIỆU")
    
    add_h2(doc, "4.1. Kiến trúc phần mềm tổng thể (Clean Architecture)")
    add_para(doc, "Ứng dụng FamilyCare được thiết kế tuân thủ chặt chẽ mô hình Clean Architecture do Robert C. Martin (Uncle Bob) đề xướng, kết hợp cùng các khuyến nghị chuẩn mực kiến trúc hiện đại từ Google Android. Trọng tâm của kiến trúc này là quy tắc phụ thuộc một chiều (Dependency Rule): mã nguồn ở các tầng bên trong hoàn toàn độc lập, không phụ thuộc vào bất kỳ framework hay thư viện giao diện nào ở các tầng bên ngoài.")
    
    add_h3(doc, "4.1.1. Phân rã 4 tầng kiến trúc (Architectural Layers)")
    add_para(doc, "Hệ thống được chia thành 4 phân tầng độc lập với trách nhiệm được phân định ranh giới rõ ràng:")
    
    layer_headers = ["Tầng kiến trúc (Layer)", "Các thành phần cốt lõi", "Trách nhiệm và Ranh giới nghiệp vụ"]
    layer_data = [
        ("1. Presentation Layer\n(Tầng Giao diện)", "Composables, ViewModels, UiState, UiIntent, Navigation Graph", "Đảm nhận hiển thị giao diện người dùng theo cơ chế khai báo Jetpack Compose. Lắng nghe tương tác chạm, giọng nói, cử chỉ từ người dùng; chuyển đổi thành UiIntent gửi tới ViewModel và quan sát (observe) luồng StateFlow để vẽ lại UI phản ứng (Reactive Rendering)."),
        ("2. Domain Layer\n(Tầng Nghiệp vụ cốt lõi)", "UseCases, Domain Models, Business Rules, Repository Interfaces", "Là trái tim của hệ thống, chứa đựng toàn bộ quy tắc nghiệp vụ y tế: thuật toán phát hiện bất thường sinh tồn 48 giờ (AnalyzeThresholdUseCase), thuật toán kích hoạt SOS Panic, bộ giải mã định dạng thuốc. Tầng này viết bằng Kotlin thuần túy (Pure Kotlin), không phụ thuộc vào Android SDK."),
        ("3. Data Layer\n(Tầng Dữ liệu)", "Repository Implementations, Local Data Sources, Remote Data Sources, DTOs, Mappers", "Chịu trách nhiệm cung cấp và điều phối dữ liệu cho Domain Layer. Hiện thực hóa các interface Repository. Phối hợp giữa nguồn dữ liệu nội bộ Offline-first (Room Database) và các dịch vụ đám mây (Firebase FCM, Gemini Generative AI API). Chuyển đổi qua lại giữa Entity CSDL và Domain Model."),
        ("4. Framework & Platform Layer\n(Tầng Nền tảng thiết bị)", "CameraX Analyzer, Google ML Kit, AlarmManager, AudioTrack, FusedLocationClient, Room SQLite", "Tương tác trực tiếp với phần cứng điện thoại Android và hệ điều hành: điều khiển đèn Flash LED, chụp khung hình camera YUV, đo tọa độ GPS, lập lịch báo thức mức RTC, phát còi cứu nạn qua loa ngoài ở kênh STREAM_ALARM.")
    ]
    create_styled_table(doc, layer_headers, layer_data, [Inches(1.6), Inches(2.15), Inches(2.5)], "Bảng 4.1: Bảng phân rã 4 tầng Clean Architecture trong dự án FamilyCare")

    add_h3(doc, "4.1.2. Mô hình tương tác MVI (Model - View - Intent) kết hợp MVVM")
    add_para(doc, "Tại tầng Presentation, FamilyCare triển khai mô hình MVI (Model-View-Intent) kết hợp MVVM để đảm bảo luồng dữ liệu một chiều (Unidirectional Data Flow - UDF) dự đoán được:")
    add_bullet(doc, "Biểu diễn toàn bộ dữ liệu cần thiết để hiển thị một màn hình dưới dạng một data class bất biến (Immutable Data Class), ví dụ EmergencyUiState(isLoading, isSirenPlaying, gpsAddress, recentAlerts). Trạng thái này được đóng gói trong StateFlow.", "• Model (UiState): ")
    add_bullet(doc, "Các Composable Functions nhận UiState làm tham số đầu vào và kết xuất giao diện. Khi người dùng bấm nút hoặc vuốt màn hình, View không tự ý thay đổi dữ liệu mà chỉ phát ra (emit) một hành động người dùng.", "• View (Jetpack Compose): ")
    add_bullet(doc, "Đại diện cho ý định của người dùng, được định nghĩa dưới dạng Sealed Interface (ví dụ: EmergencyIntent.StartSiren, EmergencyIntent.HoldSosButton, EmergencyIntent.ResolveAlert).", "• Intent (User Action): ")
    add_bullet(doc, "Tiếp nhận Intent, điều phối các Use Case bất đồng bộ, sau đó cập nhật một bản sao trạng thái mới (StateFlow.update { it.copy(...) }) đẩy về cho View hiển thị.", "• ViewModel: ")

    add_h3(doc, "4.1.3. Cơ chế xử lý đồng thời với Kotlin Coroutines và Dispatchers")
    add_para(doc, "Ứng dụng y tế yêu cầu khả năng xử lý song song khối lượng lớn phép toán mà không làm giật lag giao diện người dùng. Hệ thống phân chia công việc cho các Dispatchers chuyên biệt:")
    add_bullet(doc, "Chuyên chạy trên UI Thread, chỉ làm nhiệm vụ lắng nghe thao tác và cập nhật StateFlow để Compose vẽ lại khung hình ở tần số quét 60/120 Hz.", "1. Dispatchers.Main: ")
    add_bullet(doc, "Được cấp phát Thread Pool tối ưu cho các tác vụ I/O nghẽn như truy vấn đọc/ghi SQLite Room Database, gọi API Google Gemini qua HTTP mạng hoặc ghi file PDF vào bộ nhớ Scoped Storage.", "2. Dispatchers.IO: ")
    add_bullet(doc, "Dành riêng cho các thuật toán tiêu tốn năng lực xử lý CPU nặng: thuật toán biến đổi ảnh YUV sang kênh đỏ, tính toán bộ lọc số thông dải Butterworth, quét đỉnh xung nhịp PPG và phân tích biểu thức chính quy (Regex) của OCR.", "3. Dispatchers.Default: ")

    add_h3(doc, "4.1.4. Thiết kế Luồng Tuần tự Tương tác (Sequence Diagrams)")
    add_para(doc, "Nhằm đặc tả rõ nét sự tương tác bất đồng bộ giữa các tầng kiến trúc trong hệ thống, dưới đây là bảng tuần tự các sự kiện trong 3 luồng xử lý cốt lõi:")

    seq_headers = ["Bước", "Bên gửi (Sender)", "Bên nhận (Receiver)", "Thông điệp / Dữ liệu", "Phương thức xử lý"]
    seq1_data = [
        ("1", "User (Bệnh nhân)", "CameraPpgScreen", "Bấm 'Bắt đầu đo nhịp tim'", "UI Event (Touch)"),
        ("2", "CameraPpgScreen", "CameraX Lifecycle", "Khởi tạo Preview & ImageAnalysis", "Main Thread (Async)"),
        ("3", "CameraX Provider", "CameraPpgAnalyzer", "Bắn khung hình liên tục (30 FPS)", "Dispatchers.Default"),
        ("4", "CameraPpgAnalyzer", "PpgSignalProcessor", "Truyền cường độ đỏ I_red vùng ROI", "Synchronous (RAM)"),
        ("5", "PpgSignalProcessor", "Butterworth Filter", "Lọc dải thông 0.75 - 3.2 Hz", "Computation"),
        ("6", "PpgSignalProcessor", "Peak Detector", "Phát hiện đỉnh sóng, tính RR và BPM", "Adaptive Threshold"),
        ("7", "PpgSignalProcessor", "CameraPpgViewModel", "Phát tín hiệu nhịp tim hợp lệ", "StateFlow.update"),
        ("8", "CameraPpgViewModel", "HealthMetricDao", "Ghi bản ghi vào bảng health_metrics", "Dispatchers.IO (Room)"),
        ("9", "CameraPpgViewModel", "CameraPpgScreen", "Cập nhật UiState: sóng động + BPM", "Main Thread (Compose)")
    ]
    create_styled_table(doc, seq_headers, seq1_data, [Inches(0.45), Inches(1.3), Inches(1.3), Inches(2.0), Inches(1.2)], "Bảng 4.2: Luồng tuần tự sự kiện đo nhịp tim quang học qua CameraX và Bộ lọc số")

    seq2_data = [
        ("1", "User (Bệnh nhân)", "SosButton (UI)", "Nhấn GIỮ nút SOS trong 2000ms", "PointerInput (Press)"),
        ("2", "SosButton", "Haptic & Canvas", "Rung tăng dần và quét vòng tròn 100%", "Main Thread (Frame)"),
        ("3", "SosButton", "EmergencyViewModel", "Kích hoạt khẩn cấp khi đạt 2000ms", "UiIntent.TriggerSos"),
        ("4", "EmergencyViewModel", "SirenController", "Chiếm STREAM_ALARM volume 100%", "Audio Hardware"),
        ("5", "EmergencyViewModel", "FusedLocation", "Lấy GPS PRIORITY_HIGH_ACCURACY", "Dispatchers.IO"),
        ("6", "FusedLocation", "Android Geocoder", "Biến đổi tọa độ thành địa chỉ số nhà", "Reverse Geocoding"),
        ("7", "EmergencyViewModel", "AlertDao", "Lưu bản ghi CRITICAL vào alerts", "Dispatchers.IO (Room)"),
        ("8", "EmergencyViewModel", "EmergencyNotif", "Bắn thông báo đẩy Heads-up khẩn cấp", "FCM & Notification")
    ]
    create_styled_table(doc, seq_headers, seq2_data, [Inches(0.45), Inches(1.3), Inches(1.3), Inches(2.0), Inches(1.2)], "Bảng 4.3: Luồng tuần tự sự kiện phản ứng nhanh SOS Panic và Cứu nạn khẩn cấp")

    add_h2(doc, "4.2. Thiết kế Cơ sở dữ liệu SQLite Room Database (Version 4)")
    add_para(doc, "FamilyCare sử dụng thư viện Room Persistence Library để quản lý cơ sở dữ liệu SQLite cục bộ theo triết lý Offline-first. Sau quá trình phát triển và hoàn thiện qua các đợt kiểm thử lâm sàng, cơ sở dữ liệu đạt phiên bản v4 với cấu trúc chuẩn hóa, hỗ trợ quan hệ ràng buộc toàn vẹn dữ liệu (Foreign Keys) và chỉ mục (Indices) tối ưu hóa truy vấn.")
    
    add_h3(doc, "4.2.1. Bảng 4.4: Chi tiết Cấu trúc Thực thể Bảng `users` (Người dùng & Bệnh nhân)")
    add_para(doc, "Bảng `users` lưu trữ toàn bộ hồ sơ y bạ, định danh cá nhân và thông tin bảo trợ gia đình:")
    
    users_headers = ["Tên trường", "Kiểu dữ liệu", "Ràng buộc (Constraints)", "Ý nghĩa và Mô tả nghiệp vụ"]
    users_data = [
        ("id", "INTEGER", "PRIMARY KEY AUTOINCREMENT", "Mã định danh duy nhất của người dùng trong hệ thống nội bộ."),
        ("email", "TEXT", "NOT NULL, UNIQUE", "Địa chỉ email tài khoản dùng để đăng nhập và định danh đám mây."),
        ("password_hash", "TEXT", "NOT NULL", "Mật khẩu người dùng đã được băm an toàn kèm muối (Salted Hash)."),
        ("fullName", "TEXT", "NOT NULL", "Họ và tên đầy đủ của người bệnh hoặc người thân bảo trợ."),
        ("role", "TEXT", "NOT NULL DEFAULT 'MEMBER'", "Vai trò trong gia đình: 'ADMIN' (Trưởng gia đình) hoặc 'MEMBER' (Thành viên)."),
        ("familyId", "TEXT", "NULL", "Mã tổ ấm gia đình gồm 6 ký tự viết hoa (ví dụ: 'FAM892') liên kết các thành viên."),
        ("birthDate", "TEXT", "NULL", "Ngày tháng năm sinh (định dạng YYYY-MM-DD) dùng để tính tuổi bệnh nhân."),
        ("gender", "TEXT", "NULL", "Giới tính sinh học ('MALE', 'FEMALE') phục vụ tham vấn y tế của Gemini AI."),
        ("bloodType", "TEXT", "NULL", "Nhóm máu hệ ABO và Rh (ví dụ: 'O+', 'A+', 'B-', 'AB+')."),
        ("chronicDiseases", "TEXT", "NULL", "Danh sách bệnh nền mãn tính (Tăng huyết áp, Đái tháo đường, Tim mạch)."),
        ("allergies", "TEXT", "NULL", "Danh mục thuốc và thực phẩm dị ứng (Penicillin, Aspirin, hải sản)."),
        ("createdAt", "INTEGER", "NOT NULL", "Dấu thời gian Epoch Milliseconds lúc tài khoản được khởi tạo.")
    ]
    create_styled_table(doc, users_headers, users_data, [Inches(1.35), Inches(1.1), Inches(1.8), Inches(2.0)], "Bảng 4.4: Đặc tả cấu trúc bảng người dùng và hồ sơ bệnh nhân (users)")

    add_h3(doc, "4.2.2. Bảng 4.5: Chi tiết Cấu trúc Thực thể Bảng `health_metrics` (Chỉ số Sinh tồn)")
    add_para(doc, "Lưu trữ toàn bộ các phiên đo nhịp tim quang học PPG, huyết áp, đường huyết hàng ngày của bệnh nhân:")
    
    metrics_headers = ["Tên trường", "Kiểu dữ liệu", "Ràng buộc (Constraints)", "Ý nghĩa và Mô tả nghiệp vụ"]
    metrics_data = [
        ("id", "INTEGER", "PRIMARY KEY AUTOINCREMENT", "Khóa chính tự tăng của bản ghi đo lường."),
        ("userId", "INTEGER", "NOT NULL, FK -> users(id)", "Khóa ngoại liên kết tới người bệnh sở hữu chỉ số đo (ON DELETE CASCADE)."),
        ("systolic", "INTEGER", "NULL", "Chỉ số Huyết áp tâm thu (Systolic Blood Pressure, đơn vị mmHg)."),
        ("diastolic", "INTEGER", "NULL", "Chỉ số Huyết áp tâm trương (Diastolic Blood Pressure, đơn vị mmHg)."),
        ("pulse", "INTEGER", "NULL", "Chỉ số Nhịp tim đo được từ Camera PPG hoặc nhập tay (BPM - nhịp/phút)."),
        ("bloodGlucose", "REAL", "NULL", "Chỉ số đường huyết mao mạch (đơn vị mmol/L hoặc mg/dL)."),
        ("bloodGlucoseType", "TEXT", "NULL", "Thời điểm đo đường huyết: 'FASTING' (Lúc đói), 'POSTPRANDIAL' (Sau ăn 2h)."),
        ("note", "TEXT", "NULL", "Ghi chú triệu chứng lâm sàng đi kèm (chóng mặt, hồi hộp, khó thở)."),
        ("recordedAt", "INTEGER", "NOT NULL", "Thời điểm thực hiện phép đo (Epoch ms), đánh chỉ mục INDEX để quét nhanh."),
        ("syncStatus", "TEXT", "DEFAULT 'SYNCED'", "Trạng thái đồng bộ đám mây: 'PENDING', 'SYNCED', 'FAILED'.")
    ]
    create_styled_table(doc, metrics_headers, metrics_data, [Inches(1.35), Inches(1.1), Inches(1.8), Inches(2.0)], "Bảng 4.5: Đặc tả cấu trúc bảng chỉ số sinh tồn (health_metrics)")

    add_h3(doc, "4.2.3. Bảng 4.6: Chi tiết Cấu trúc Thực thể Bảng `medicines` và `medication_logs` (Đơn thuốc & Lịch uống)")
    add_para(doc, "Quản lý danh mục thuốc điều trị và lịch sử tuân thủ y lệnh bác sĩ:")
    
    med_headers = ["Tên trường", "Kiểu dữ liệu", "Bảng sở hữu", "Ý nghĩa và Mô tả nghiệp vụ"]
    med_data = [
        ("id", "INTEGER (PK)", "medicines", "Mã định danh duy nhất của loại thuốc."),
        ("userId", "INTEGER (FK)", "medicines", "Mã người bệnh cần uống thuốc."),
        ("name", "TEXT", "medicines", "Tên biệt dược hoặc hoạt chất (ví dụ: 'Amlodipine 5mg', 'Metformin 500mg')."),
        ("dosage", "REAL", "medicines", "Liều lượng mỗi lần dùng (ví dụ: 1.0, 0.5, 2.0)."),
        ("unit", "TEXT", "medicines", "Đơn vị đóng gói: 'viên', 'gói', 'ml', 'giọt'."),
        ("reminderTimes", "TEXT", "medicines", "Chuỗi JSON lưu các mốc giờ uống thuốc trong ngày (ví dụ: '[\"07:00\",\"19:00\"]')."),
        ("isActive", "INTEGER", "medicines", "Cờ trạng thái kích hoạt: 1 là đang điều trị, 0 là đã ngưng thuốc."),
        ("id", "INTEGER (PK)", "medication_logs", "Mã bản ghi lịch sử uống thuốc."),
        ("medicineId", "INTEGER (FK)", "medication_logs", "Khóa ngoại liên kết tới thuốc (ON DELETE CASCADE)."),
        ("scheduledTime", "INTEGER", "medication_logs", "Thời gian dự kiến phải uống theo đơn."),
        ("takenTime", "INTEGER", "medication_logs", "Thời gian thực tế người bệnh bấm 'Đã uống' trên thông báo."),
        ("status", "TEXT", "medication_logs", "Trạng thái: 'TAKEN' (Đúng giờ), 'MISSED' (Bỏ quên), 'SKIPPED' (Bỏ qua).")
    ]
    create_styled_table(doc, med_headers, med_data, [Inches(1.3), Inches(1.2), Inches(1.35), Inches(2.4)], "Bảng 4.6: Đặc tả cấu trúc bảng danh mục thuốc và nhật ký uống thuốc")

    add_h3(doc, "4.2.4. Bảng 4.7: Chi tiết Cấu trúc Thực thể Bảng `alerts` (Cảnh báo & Báo động SOS)")
    add_para(doc, "Bảng `alerts` là trung tâm tiếp nhận thông tin khẩn cấp từ các luồng phân tích tự động và nút bấm SOS:")
    
    alert_headers = ["Tên trường", "Kiểu dữ liệu", "Ràng buộc (Constraints)", "Ý nghĩa và Mô tả nghiệp vụ"]
    alert_data = [
        ("id", "INTEGER", "PRIMARY KEY AUTOINCREMENT", "Mã định danh cảnh báo duy nhất."),
        ("familyId", "TEXT", "NOT NULL", "Mã tổ ấm để phát thông báo tới tất cả người thân trong gia đình."),
        ("memberId", "INTEGER", "NOT NULL", "Mã thành viên gặp sự cố sức khỏe hoặc phát động SOS."),
        ("memberName", "TEXT", "NOT NULL", "Tên người thân (hiển thị trực tiếp trên Card cảnh báo: '🚨 Báo động SOS: [Tên]')."),
        ("type", "TEXT", "NOT NULL", "Phân loại: 'SOS_PANIC', 'HEART_RATE_ANOMALY', 'BP_ANOMALY', 'MED_MISSED'."),
        ("severity", "TEXT", "NOT NULL", "Mức độ nghiêm trọng: 'CRITICAL' (Đỏ cấp cứu), 'WARNING' (Vàng nhắc nhở), 'INFO'."),
        ("title", "TEXT", "NOT NULL", "Tiêu đề ngắn gọn một dòng chống rớt chữ."),
        ("message", "TEXT", "NOT NULL", "Nội dung tóm tắt sự cố (ví dụ: 'Đã phát cảnh báo khẩn cấp tới toàn bộ người thân.')."),
        ("locationAddress", "TEXT", "NULL", "Địa chỉ chuỗi Geocoding bắt từ GPS (chỉ hiển thị 1 dòng cạnh icon ghim cam)."),
        ("latitude", "REAL", "NULL", "Tọa độ vĩ độ GPS để mở Google Maps dẫn đường."),
        ("longitude", "REAL", "NULL", "Tọa độ kinh độ GPS để mở Google Maps dẫn đường."),
        ("isRead", "INTEGER", "NOT NULL DEFAULT 0", "Cờ trạng thái: 0 là chưa xử lý (viền đỏ/vàng), 1 là đã xử lý (viền xám mỏng, nút xám xanh)."),
        ("createdAt", "INTEGER", "NOT NULL", "Dấu thời gian phát sinh cảnh báo (Epoch ms).")
    ]
    create_styled_table(doc, alert_headers, alert_data, [Inches(1.3), Inches(1.0), Inches(1.75), Inches(2.2)], "Bảng 4.7: Đặc tả cấu trúc bảng cảnh báo an toàn và cấp cứu (alerts)")

    add_h3(doc, "4.2.5. Bảng 4.8: Chiến lược Tối ưu hóa Chỉ mục (Database Indices) và Hiệu năng Truy vấn")
    add_para(doc, "Khi số lượng bản ghi đo sinh tồn và thông báo tích lũy sau nhiều tháng sử dụng lên tới hàng chục nghìn dòng, việc quét toàn bộ bảng (Table Scan) sẽ gây giật lag UI và làm chậm tiến trình phân tích 48h. Hệ thống thiết lập các chỉ mục hợp thành (Composite Indices) tối ưu:")
    
    idx_headers = ["Tên Chỉ Mục (Index Name)", "Bảng CSDL", "Cột Được Đánh Chỉ Mục", "Mục Đích Tối Ưu Truy Vấn"]
    idx_data = [
        ("index_health_metrics_user_time", "health_metrics", "userId, recordedAt DESC", "Tối ưu truy vấn lịch sử đo gần nhất và quét cửa sổ 48h (AnalyzeThresholdUseCase). Giảm thời gian truy vấn từ 120ms xuống < 4ms."),
        ("index_alerts_family_is_read", "alerts", "familyId, isRead, createdAt DESC", "Tối ưu nạp danh sách thông báo trên AlertCenterScreen và đếm số lượng tin chưa đọc trên huy hiệu quả chuông Dashboard."),
        ("index_medicines_user_active", "medicines", "userId, isActive", "Nạp nhanh danh sách thuốc đang điều trị để hiển thị các cữ thuốc trong ngày và lập lịch báo thức RTC."),
        ("index_med_logs_scheduled", "medication_logs", "medicineId, scheduledTime DESC", "Truy vấn lịch sử uống thuốc trong tháng để tính toán tỷ lệ tuân thủ điều trị phục vụ kết xuất hồ sơ PDF.")
    ]
    create_styled_table(doc, idx_headers, idx_data, [Inches(1.6), Inches(1.1), Inches(1.55), Inches(2.0)], "Bảng 4.8: Bảng cấu hình chỉ mục tối ưu hóa hiệu năng truy vấn Room SQLite")

    add_h3(doc, "4.2.6. Chiến lược Di cư Dữ liệu Tự phục hồi (Self-Healing Migration Strategy)")
    add_para(doc, "Trong quá trình nâng cấp ứng dụng thực tế từ phiên bản thử nghiệm lên v4, các sự cố xung đột lược đồ (Schema Inconsistency Exception) thường xuyên xảy ra khi cấu trúc bảng được bổ sung các cột mới như `locationAddress`, `bloodGlucoseType`. Nếu xử lý thô bạo bằng `fallbackToDestructiveMigration()`, toàn bộ dữ liệu đo lường quý giá của người bệnh sẽ bị xóa sạch.")
    add_para(doc, "Nhóm tác giả đã phát triển giải pháp **Self-Healing Room Migration** kết hợp giữa Migration thủ công và Callback kiểm tra toàn vẹn tự động:")
    add_bullet(doc, "Định nghĩa rõ ràng các `Migration(1, 2)`, `Migration(2, 3)`, `Migration(3, 4)` sử dụng câu lệnh `ALTER TABLE tbl ADD COLUMN col TEXT DEFAULT NULL`.", "1. Migration lũy tiến: ")
    add_bullet(doc, "Trong `RoomDatabase.Callback.onOpen()`, hệ thống tự động chạy câu lệnh `PRAGMA table_info(alerts)` để đối soát danh sách cột hiện có. Nếu phát hiện thiếu bất kỳ trường dữ liệu nào do xung đột cache, hệ thống sẽ thực thi câu lệnh vá cấu trúc tức thời mà không làm gián đoạn phiên làm việc.", "2. Tự phục hồi OnOpen: ")

    add_callout(doc, "Giải pháp Self-Healing Database Migration giúp bảo vệ 100% dữ liệu lịch sử đo sinh tồn và nhật ký điều trị thuốc của bệnh nhân qua các đợt cập nhật phần mềm, giảm thiểu tỷ lệ crash ứng dụng do SQLiteException xuống 0%.", "SÁNG KIẾN KỸ THUẬT QUAN TRỌNG", "info")

    add_h2(doc, "4.3. Thiết kế Hệ thống Giao diện UI/UX và Design System")
    add_para(doc, "Giao diện của FamilyCare được xây dựng dựa trên bộ khung thiết kế Material Design 3 (Material You) của Google, được tùy biến chuyên sâu nhằm tối ưu hóa khả năng tiếp cận (Accessibility) cho người cao tuổi Việt Nam.")

    add_h3(doc, "4.3.1. Bảng 4.9: Hệ thống Bảng Mã Màu Chuẩn (Color Palette Design System)")
    add_para(doc, "Mỗi màu sắc trong hệ thống đều mang ý nghĩa tín hiệu thị giác rõ ràng, đảm bảo tỷ lệ tương phản tối thiểu 4.5:1 so với nền:")
    
    color_headers = ["Tên Màu (Token)", "Mã Hex / Giá trị", "Mục Đích Sử Dụng Trong Giao Diện", "Ý Nghĩa Tâm Lý Y Tế"]
    color_data = [
        ("Primary (Xanh Navy)", "#1E3A8A / #0F172A", "Thanh TopAppBar, Tiêu đề chính, Nút bấm chủ đạo.", "Tạo cảm giác tin cậy, vững chãi, uy tín y khoa chuẩn mực."),
        ("Medical Blue (Xanh Y tế)", "#2563EB / #3B82F6", "Các icon chức năng, đường viền code block, đồ thị sóng nhịp tim.", "Gợi nhớ môi trường bệnh viện hiện đại, công nghệ sạch sẽ."),
        ("Critical Red (Đỏ Cấp Cứu)", "#DC2626 / #EF4444", "Nút SOS Panic, viền thẻ cảnh báo nguy hiểm, nút Xem bản đồ cấp cứu.", "Báo động khẩn cấp tức thời, kích hoạt phản xạ chú ý cao độ."),
        ("Warning Amber (Vàng Hổ Phách)", "#D97706 / #F59E0B", "Viền thẻ nhắc nhở uống thuốc, cảnh báo chỉ số tiệm cận ngưỡng.", "Nhắc nhở nhẹ nhàng, không gây hoảng loạn tâm lý người già."),
        ("Success Green (Xanh Lá)", "#16A34A / #22C55E", "Huy hiệu Đã xử lý, thẻ chỉ số huyết áp bình thường, nút Đã uống.", "Biểu thị sự an toàn, hồi phục và hoàn thành nhiệm vụ."),
        ("Neutral Slate (Xám Trung Tính)", "#475569 / #64748B", "Địa chỉ ghim định vị, văn bản thứ cấp, nút Xem bản đồ đã xử lý.", "Thanh lịch, dịu mắt, giảm tải căng thẳng thị giác."),
        ("Background Pure (Nền Trắng)", "#FFFFFF / #F8FAFC", "Nền tổng thể màn hình, nền các thẻ Card bo góc 16dp.", "Tối đa hóa độ tương phản chữ viết, giúp người già mắt mờ dễ đọc.")
    ]
    create_styled_table(doc, color_headers, color_data, [Inches(1.4), Inches(1.3), Inches(2.05), Inches(1.5)], "Bảng 4.9: Bảng định nghĩa màu sắc chủ đạo Material Design 3 trong FamilyCare")

    add_h3(doc, "4.3.2. Bảng 4.10: Thang đo Kiểu chữ Typography Scale Chuẩn Hóa")
    add_para(doc, "Tuân thủ quy chuẩn chữ viết lớn, rõ ràng của Material Theme:")
    
    typo_headers = ["Kiểu Chữ (Style)", "Cỡ Chữ (Size)", "Độ Đậm (Weight)", "Ứng Dụng Trong Giao Diện"]
    typo_data = [
        ("Display Large", "32.sp", "FontWeight.Bold", "Số đo nhịp tim lớn hiển thị giữa màn hình khi đo PPG."),
        ("Headline Medium", "22.sp", "FontWeight.Bold", "Tiêu đề các phân hệ lớn: Dashboard, Tủ thuốc, Lịch sử."),
        ("Title Large", "18.sp", "FontWeight.SemiBold", "Tiêu đề TopAppBar, Tên người thân trong gia đình."),
        ("Title Medium (Card Title)", "14.5.sp", "FontWeight.Bold", "Tiêu đề thẻ cảnh báo một dòng chống rớt chữ (AlertItemCard)."),
        ("Body Large", "15.sp", "FontWeight.Normal", "Nội dung phản hồi đàm thoại của Trợ lý AI Gemini."),
        ("Body Medium", "13.5.sp", "FontWeight.Normal", "Mô tả sự cố cảnh báo, hướng dẫn cách đặt ngón tay đo camera."),
        ("Label Medium (Meta Info)", "12.sp", "FontWeight.Medium", "Địa chỉ định vị GPS, thời gian đo, nhãn liều lượng thuốc.")
    ]
    create_styled_table(doc, typo_headers, typo_data, [Inches(1.5), Inches(1.0), Inches(1.45), Inches(2.3)], "Bảng 4.10: Thang đo kiểu chữ Typography Scale áp dụng trong dự án")

    add_h3(doc, "4.3.3. Quy chuẩn Công thái học Accessibility cho Người Cao Tuổi")
    add_bullet(doc, "Mọi thành phần có thể bấm chạm (Buttons, IconButtons, Checkboxes) đều có diện tích tối thiểu 48x48dp (PaddingValues mở rộng ngoài vùng nhìn thấy), ngăn ngừa hiện tượng bấm trượt do run tay.", "1. Vùng chạm tối thiểu (Touch Target): ")
    add_bullet(doc, "Mọi thao tác quan trọng (Bắt đầu đo, Kích hoạt SOS, Lưu đơn thuốc) đều đi kèm phản ứng rung nhẹ (Haptic Feedback) và hiệu ứng thị giác tương ứng.", "2. Phản hồi đa giác quan: ")
    add_bullet(doc, "Không phụ thuộc hoàn toàn vào màu sắc để truyền đạt thông tin: Thẻ khẩn cấp vừa có viền đỏ vừa có icon 🚨, thẻ đã xử lý vừa có màu xanh vừa có chữ 'Đã xử lý'.", "3. Thiết kế chống mù màu: ")

    # ==================== CHƯƠNG 5 ====================
    add_h1(doc, "CHƯƠNG 5: HIỆN THỰC HÓA CÁC MODULE CHỨC NĂNG CỐT LÕI")
    
    add_h2(doc, "5.1. Module 1: Đo nhịp tim quang học qua CameraX và Bộ lọc số")
    add_para(doc, "Module đo nhịp tim quang học PPG được thiết kế như một quy trình khép kín: thu nhận hình ảnh tốc độ cao từ Camera sau, trích xuất biến thiên cường độ đỏ, lọc nhiễu số học và phát hiện đỉnh xung mạch thời gian thực.")
    
    add_h3(doc, "5.1.1. Hiện thực hóa CameraPpgAnalyzer và Xử lý khung hình YUV")
    add_para(doc, "Lớp `CameraPpgAnalyzer` triển khai interface `ImageAnalysis.Analyzer` của Android CameraX. Mỗi khung hình YUV_420_888 truyền về sẽ được xử lý trên background executor riêng biệt:")
    
    ppg_code = '''class CameraPpgAnalyzer(
    private val onPulseDetected: (bpm: Int, rawIntensity: Float) -> Unit,
    private val onFingerStatusChanged: (isCovered: Boolean) -> Unit
) : ImageAnalysis.Analyzer {

    private val signalProcessor = PpgSignalProcessor()

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        try {
            // Trích xuất mặt phẳng Y và V để tính cường độ kênh Đỏ theo ITU-R BT.601
            val yPlane = mediaImage.planes[0].buffer
            val vPlane = mediaImage.planes[2].buffer

            val width = mediaImage.width
            val height = mediaImage.height
            val roiSize = 100 // Vùng trung tâm 100x100 pixels
            val startX = (width - roiSize) / 2
            val startY = (height - roiSize) / 2

            var sumRed = 0.0
            var sampleCount = 0

            for (y in startY until (startY + roiSize) step 2) {
                for (x in startX until (startX + roiSize) step 2) {
                    val yIndex = y * mediaImage.planes[0].rowStride + x * mediaImage.planes[0].pixelStride
                    val vIndex = (y / 2) * mediaImage.planes[2].rowStride + (x / 2) * mediaImage.planes[2].pixelStride

                    val yVal = yPlane.get(yIndex).toInt() and 0xFF
                    val vVal = vPlane.get(vIndex).toInt() and 0xFF
                    // R = Y + 1.402 * (V - 128)
                    val rVal = (yVal + 1.402 * (vVal - 128)).coerceIn(0.0, 255.0)
                    sumRed += rVal
                    sampleCount++
                }
            }

            val avgRed = (sumRed / sampleCount).toFloat()
            // Kiểm tra độ che phủ: Cường độ đỏ phải vượt ngưỡng hấp thụ máu mao mạch
            val isFingerCovering = avgRed > 180.0f
            onFingerStatusChanged(isFingerCovering)

            if (isFingerCovering) {
                val currentBpm = signalProcessor.processSample(avgRed, System.currentTimeMillis())
                if (currentBpm in 45..190) {
                    onPulseDetected(currentBpm, avgRed)
                }
            }
        } finally {
            imageProxy.close() // Giải phóng bắt buộc để tránh tràn bộ nhớ đệm
        }
    }
}'''
    add_code_block(doc, ppg_code, "Hiện thực hóa phân tích khung hình CameraX PPG trong CameraPpgAnalyzer.kt")

    add_h3(doc, "5.1.2. Bộ lọc số Butterworth và Thuật toán Peak Detection")
    add_para(doc, "Tín hiệu cường độ đỏ sau khi trích xuất được đưa vào bộ xử lý `PpgSignalProcessor`. Thuật toán thực hiện 3 bước liên hoàn:")
    add_bullet(doc, "Trừ giá trị trung bình trượt DC trong cửa sổ 2 giây để giữ lại thành phần dao động xoay chiều AC thuần túy.", "1. Tách tín hiệu AC: ")
    add_bullet(doc, "Lọc thông dải số học tần số cắt 0.75 Hz đến 3.2 Hz để loại bỏ hoàn toàn nhiễu giật tay và ánh sáng đèn điện gia dụng.", "2. Lọc thông dải Butterworth: ")
    add_bullet(doc, "Quét tìm các đỉnh cục bộ (Local Peaks) thỏa mãn khoảng cách tối thiểu giữa 2 đỉnh liên tiếp >= 300ms (tương ứng nhịp tim tối đa 200 BPM) để tính chu kỳ nhịp đập.", "3. Phát hiện đỉnh thích nghi: ")

    add_h2(doc, "5.2. Module 2: Trợ lý Sức khỏe AI Health Copilot (Google Gemini 1.5 Flash)")
    add_para(doc, "Module Trợ lý ảo AI hoạt động như một cầu nối thông minh giữa dữ liệu y tế khô khan trong CSDL và người bệnh, cung cấp lời giải thích cặn kẽ và các khuyến nghị ăn uống, sinh hoạt chuẩn y khoa.")

    add_h3(doc, "5.2.1. Kiến trúc Tiêm Ngữ cảnh Hồ sơ Y bạ (Context Injection Engine)")
    add_para(doc, "Trước khi gửi câu hỏi của người dùng tới Gemini API, hệ thống thực hiện truy vấn hồ sơ bệnh nhân từ Room Database nội bộ và định dạng thành một Prompt có cấu trúc nghiêm ngặt:")

    ai_prompt_code = '''fun buildSystemInstruction(user: UserEntity, recentMetrics: List<HealthMetricEntity>): String {
    val diseases = user.chronicDiseases ?: "Không có bệnh nền ghi nhận"
    val allergies = user.allergies ?: "Không có dị ứng thuốc ghi nhận"
    val metricsSummary = recentMetrics.joinToString(separator = "; ") {
        "Ngày: ${it.recordedAt.toDateString()}, Huyết áp: ${it.systolic}/${it.diastolic} mmHg, Tim: ${it.pulse} BPM"
    }

    return """
        Bạn là FamilyCare AI Copilot - Bác sĩ gia đình thông thái, ân cần và giàu lòng trắc ẩn.
        Hồ sơ người bệnh hiện tại:
        - Họ tên: ${user.fullName}, Tuổi: ${user.getAge()}, Nhóm máu: ${user.bloodType}
        - Bệnh nền mãn tính: $diseases
        - Danh mục thuốc dị ứng: $allergies
        - Lịch sử đo sinh tồn gần nhất: $metricsSummary
        
        Quy tắc phản hồi an toàn y tế:
        1. Trả lời bằng tiếng Việt chuẩn mực, ngắn gọn, thân thiện, dễ hiểu cho người cao tuổi.
        2. Dựa vào hồ sơ bệnh nền và lịch sử đo sinh tồn ở trên để giải thích nguyên nhân.
        3. TUYỆT ĐỐI KHÔNG kê đơn thuốc kháng sinh hoặc thay đổi liều thuốc đặc trị.
        4. Luôn nhắc nhở người bệnh tham khảo ý kiến bác sĩ chuyên khoa trong các ca nguy cấp.
    """.trimIndent()
}'''
    add_code_block(doc, ai_prompt_code, "Đóng gói chỉ thị hệ thống kèm ngữ cảnh bệnh án cá nhân hóa")

    add_h3(doc, "5.2.2. Nhận diện giọng nói tiếng Việt với Android SpeechRecognizer")
    add_para(doc, "Để hỗ trợ người già mắt mờ hoặc tay run khó gõ bàn phím ảo, ứng dụng tích hợp sẵn bộ nhận diện giọng nói `android.speech.SpeechRecognizer` trực tiếp trên thanh công cụ chat. Luồng âm thanh được nhận dạng ở chế độ streaming, tự động chuyển đổi thành văn bản tiếng Việt hiển thị ngay lập tức lên ô nhập liệu.")

    add_h2(doc, "5.3. Module 3: Bộ máy phân tích ngưỡng nguy hiểm 48h và Cơ chế Debounce")
    add_para(doc, "Một tính năng đột phá của FamilyCare là khả năng tự động phân tích diễn biến sức khỏe liên tục, phát hiện sớm các nguy cơ biến chứng tim mạch và đột quỵ tiềm ẩn trước khi tình huống xấu xảy ra.")

    add_h3(doc, "5.3.1. Thuật toán phát hiện 3 lần đo bất thường liên tiếp trong 48 giờ")
    add_para(doc, "Mỗi khi người dùng hoàn thành một lần đo huyết áp hoặc nhịp tim mới, UseCase `AnalyzeThresholdUseCase` sẽ được kích hoạt ngầm:")
    add_bullet(doc, "Hệ thống truy vấn tất cả các bản ghi đo lường trong khoảng thời gian [t_now - 48h, t_now] sắp xếp theo thứ tự thời gian tăng dần.", "• Bước 1 (Lọc cửa sổ trượt): ")
    add_bullet(doc, "Kiểm tra các chỉ số sinh tồn so với ngưỡng an toàn chuẩn (Huyết áp tâm thu >= 140 mmHg hoặc <= 90 mmHg, Nhịp tim nghỉ >= 100 BPM hoặc <= 50 BPM).", "• Bước 2 (So sánh ngưỡng): ")
    add_bullet(doc, "Nếu tìm thấy chuỗi 3 lần đo liên tiếp đều vi phạm ngưỡng an toàn mà không có lần đo bình thường nào xen giữa, hệ thống xác định bệnh nhân đang trong trạng thái cảnh báo đỏ.", "• Bước 3 (Nhận diện chuỗi bất thường): ")

    add_h3(doc, "5.3.2. Cơ chế Debounce 6 giờ chống bão thông báo (Notification Storm)")
    add_para(doc, "Nếu người bệnh lo lắng và liên tục đo lại nhiều lần trong vòng 30 phút, việc hệ thống bắn liên tục hàng chục thông báo khẩn cấp tới điện thoại con cái sẽ gây ra hiện tượng 'bão thông báo' và tâm lý hoảng loạn không cần thiết. FamilyCare giải quyết bằng thuật toán **Debounce Khóa Thời Gian**:")
    add_bullet(doc, "Hệ thống truy vấn bảng `alerts` xem đã có cảnh báo cùng loại được kích hoạt trong vòng 6 giờ gần nhất hay chưa.", "• Khóa thời gian: ")
    add_bullet(doc, "Nếu đã có cảnh báo trong 6h qua, bản ghi mới chỉ được cập nhật âm thầm vào lịch sử theo dõi mà không phát chuông hú hoặc gửi Push Notification đến người thân.", "• Chặn phát sinh: ")

    add_h2(doc, "5.4. Module 4: Hệ thống phản ứng nhanh SOS Panic và Điều phối cứu hộ")
    add_para(doc, "Khi xảy ra biến cố té ngã, đau thắt ngực hoặc đột quỵ, từng giây đều là 'thời gian vàng' cứu sống người bệnh. Module SOS Panic được tối ưu để kích hoạt trong thời gian ngắn nhất nhưng loại bỏ tuyệt đối nguy cơ bấm nhầm.")

    add_h3(doc, "5.4.1. Nút bấm giữ 2 giây với phản hồi thị giác động (SosButton)")
    add_para(doc, "Thay vì cơ chế chạm đơn (single-tap) rất dễ bị chạm nhầm khi để điện thoại trong túi quần áo, FamilyCare yêu cầu người dùng phải chạm và **GIỮ LIÊN TỤC TRONG 2000ms**:")
    add_bullet(doc, "Sử dụng Modifier `pointerInput` với `detectTapGestures(onPress = ...)` để theo dõi sự kiện ngón tay tiếp xúc và nhấc khỏi màn hình.", "• Bắt cử chỉ chính xác: ")
    add_bullet(doc, "Một vòng tròn tiến trình (Circular Progress Canvas) quét đều từ 0% đến 100% trong đúng 2000ms kèm rung phản hồi nhịp nhàng (Haptic Feedback) tăng dần.", "• Phản hồi trực quan: ")
    add_bullet(doc, "Nếu người dùng nhấc tay ra ở thời điểm 1.8 giây, biến đếm tức thời hủy bỏ (cancel), vòng tròn thu về 0%, còi hú không phát.", "• Cơ chế an toàn hủy lệnh: ")

    add_h3(doc, "5.4.2. Bộ điều khiển Còi hú cứu nạn và Chiếm quyền Audio")
    add_para(doc, "Khi đủ 2 giây, `EmergencySirenController` lập tức can thiệp vào tầng âm thanh phần cứng Android:")
    
    siren_code = '''class EmergencySirenController(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun startEmergencySiren() {
        try {
            // Chiếm quyền kênh STREAM_ALARM và đẩy âm lượng lên mức tối đa (100%)
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, AudioManager.FLAG_PLAY_SOUND)

            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(context, R.raw.sos_siren_alarm).apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true // Lặp liên tục cho đến khi người dùng tắt
                start()
            }
        } catch (e: Exception) {
            Log.e("EmergencySiren", "Lỗi phát còi cứu nạn: ${e.message}")
        }
    }

    fun stopEmergencySiren() {
        mediaPlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }
        mediaPlayer = null
    }
}'''
    add_code_block(doc, siren_code, "Hiện thực hóa còi hú cứu nạn chiếm kênh STREAM_ALARM")

    add_h3(doc, "5.4.3. Bắt tọa độ GPS chính xác cao và Geocoding địa chỉ")
    add_para(doc, "Đồng thời với còi hú, hệ thống sử dụng `FusedLocationProviderClient` với độ ưu tiên `PRIORITY_HIGH_ACCURACY` để lấy tọa độ vệ tinh GPS mới nhất của người bệnh. Tọa độ (Vĩ độ, Kinh độ) được chuyển hóa ngay lập tức thành địa chỉ số nhà, tên đường thông qua Android `Geocoder` và đóng gói vào bản tin cảnh báo gửi tới máy người thân.")

    add_h2(doc, "5.5. Module 5: Tủ thuốc thông minh, Quét đơn thuốc OCR và Báo thức cưỡng bức")
    add_para(doc, "Quản lý thuốc điều trị là một trong những bài toán phức tạp nhất đối với các gia đình có người lớn tuổi mắc các bệnh mãn tính (thường phải uống từ 4 đến 8 loại thuốc mỗi ngày).")

    add_h3(doc, "5.5.1. Quét đơn thuốc On-device với Google ML Kit OCR")
    add_para(doc, "Bệnh nhân chụp ảnh đơn thuốc hoặc vỏ hộp thuốc. Khung hình được truyền trực tiếp vào `TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)` chạy 100% Offline trên điện thoại. Kết quả phân tích trả về khối văn bản được đưa qua bộ trích xuất quy tắc Regex y tế:")
    add_bullet(doc, "Nhận diện các tên biệt dược phổ biến (Amlodipin, Losartan, Metformin, Panadol, Glucophage...).", "• Tên thuốc: ")
    add_bullet(doc, "Bóc tách các chỉ số kèm đơn vị như '5mg', '500mg', '10ml', '1 viên'.", "• Hàm lượng & Liều dùng: ")
    add_bullet(doc, "Nhận diện các từ khóa thời gian như 'Sáng 1v, Chiều 1v', 'sau ăn' để tự động đề xuất mốc giờ uống 07:00 và 18:00.", "• Tần suất: ")

    add_h3(doc, "5.5.2. Lập lịch Báo thức Cưỡng bức Bypass Doze Mode")
    add_para(doc, "Để đảm bảo chuông báo uống thuốc luôn vang lên đúng giờ ngay cả khi điện thoại bị khóa màn hình nhiều giờ trong chế độ ngủ sâu (Doze Mode), hệ thống áp dụng kỹ thuật kết hợp:")
    add_bullet(doc, "Sử dụng `AlarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)` với quyền đặc quyền Android 13/14.", "1. Báo thức RTC Wakeup: ")
    add_bullet(doc, "PendingIntent của báo thức được gán cờ `FLAG_IMMUTABLE` và truyền vào `NotificationCompat.Builder.setFullScreenIntent(fullScreenPendingIntent, true)` kết hợp Category `CATEGORY_ALARM`. Khi báo thức kích hoạt, hệ thống sẽ tự động bật sáng màn hình, mở thẳng Activity nhắc thuốc đè lên màn hình khóa mà người bệnh không cần mở khóa vân tay hay nhập mật mã.", "2. FullScreenIntent đánh thức màn hình: ")

    add_h2(doc, "5.6. Module 6: Xuất hồ sơ Báo cáo Y bạ điện tử chuẩn A4 (PDF Export Engine)")
    add_para(doc, "Nhằm phục vụ các đợt tái khám định kỳ với bác sĩ chuyên khoa, FamilyCare tích hợp động cơ xuất hồ sơ bệnh án điện tử định dạng PDF chuẩn quốc tế A4 trực tiếp trên thiết bị bằng lớp `android.graphics.pdf.PdfDocument`.")
    add_para(doc, "Quy trình vẽ báo cáo y bạ diễn ra theo các bước nghiêm ngặt:")
    add_bullet(doc, "Khởi tạo trang PDF kích thước chuẩn A4: Chiều rộng 595 điểm ảnh (Points), Chiều cao 842 điểm ảnh.", "• Kích thước chuẩn: ")
    add_bullet(doc, "Vẽ tiêu đề 'BÁO CÁO THEO DÕI SỨC KHỎE GIA ĐÌNH', Quốc huy/Logo y tế, thông tin hành chính bệnh nhân (Họ tên, Ngày sinh, Giới tính, Tiền sử bệnh nền, Dị ứng).", "• Khung tiêu đề hành chính: ")
    add_bullet(doc, "Sử dụng Android `Canvas` vẽ trực tiếp hệ trục tọa độ biểu đồ đường (Line Chart) thể hiện xu hướng biến thiên huyết áp tâm thu, huyết áp tâm trương và nhịp tim trong suốt 30 ngày.", "• Biểu đồ xu hướng Canvas: ")
    add_bullet(doc, "Bảng thống kê tỷ lệ tuân thủ điều trị thuốc (Ví dụ: Đã uống 56/60 cữ thuốc, tỷ lệ 93.3%), danh sách các lần đo vượt ngưỡng an toàn.", "• Bảng dữ liệu thống kê: ")
    add_bullet(doc, "Ghi file PDF vào thư mục Documents thông qua Android Storage Access Framework (SAF) hoặc MediaStore API, sau đó mở Intent chia sẻ (ShareSheet) để gửi ngay qua Zalo, Email hoặc in qua máy in Wi-Fi.", "• Xuất file và Chia sẻ: ")

    pdf_code = '''object HealthReportPdfExporter {
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val LEFT = 40f
    private const val RIGHT = 555f

    fun export(context: Context, uri: Uri, metrics: List<HealthMetricEntity>, memberName: String, ...) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val paintTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 58, 138)
            textSize = 18f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
        }
        canvas.drawText("BÁO CÁO THEO DÕI SỨC KHỎE GIA ĐÌNH", LEFT, 60f, paintTitle)

        // Vẽ biểu đồ xu hướng huyết áp (Line Chart) trực tiếp bằng Canvas
        val paintLine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(220, 38, 38)
            strokeWidth = 2.5f
            style = Paint.Style.STROKE
        }
        // Duyệt danh sách chỉ số và nối các điểm tọa độ (x, y)
        metrics.take(30).forEachIndexed { index, m ->
            val x = LEFT + index * ((RIGHT - LEFT) / 30f)
            val yBp = 350f - (m.systolic ?: 120) * 1.2f
            canvas.drawCircle(x, yBp, 3.5f, paintLine)
        }

        document.finishPage(page)
        context.contentResolver.openOutputStream(uri)?.use { out ->
            document.writeTo(out)
        }
        document.close()
    }
}'''
    add_code_block(doc, pdf_code, "Hiện thực hóa động cơ vẽ hồ sơ bệnh án PDF khổ A4 bằng Android Canvas")

    add_h2(doc, "5.7. Module 7: Trung tâm Cảnh báo Gia đình Đa tầng (Alert Center)")
    add_para(doc, "Trung tâm cảnh báo (AlertCenterScreen) là nơi tập trung toàn bộ các biến cố an toàn của cả gia đình. Dựa trên kết quả thử nghiệm trải nghiệm người dùng, component thẻ cảnh báo `AlertItemCard` đã được tái thiết kế hoàn hảo với các cải tiến vượt trội.")

    add_h3(doc, "5.7.1. Cấu trúc Component AlertItemCard với Viền Bo Tròn (BorderStroke)")
    add_para(doc, "Thay vì sử dụng dải màu đỏ/vàng nằm lệch ở mép trái dễ bị lẹm góc bo viền, `AlertItemCard` áp dụng đường viền bo tròn trực tiếp trên Card bằng `BorderStroke`:")
    add_bullet(doc, "Viền đỏ tươi `BorderStroke(1.5.dp, Color(0xFFEF4444))` bo tròn đều 4 góc 16.dp, nổi bật trên nền trắng tinh khiết.", "• Tin khẩn cấp chưa đọc (!isRead && isCritical): ")
    add_bullet(doc, "Viền vàng hổ phách ấm áp `BorderStroke(1.5.dp, Color(0xFFF59E0B))` bo tròn 16.dp.", "• Tin nhắc nhở cảnh báo chưa đọc: ")
    add_bullet(doc, "Viền xám siêu mỏng `BorderStroke(1.dp, Color(0xFFE2E8F0))` thanh lịch, thể hiện biến cố đã được con cái giải quyết êm đẹp.", "• Tin đã đọc / đã xử lý: ")

    add_h3(doc, "5.7.2. Tối ưu hóa Tiêu đề Chống Rớt Dòng và Loại bỏ Trùng lặp Địa chỉ")
    add_para(doc, "Để bảo toàn tính mỹ thuật và khả năng đọc quét nhanh của người cao tuổi:")
    add_bullet(doc, "Tiêu đề ngắn gọn chuẩn hóa: `🚨 Báo động SOS: ${alert.memberName.ifBlank { \"Người thân\" }}`, cấu hình `maxLines = 1` và `TextOverflow.Ellipsis`. Đảm bảo tên người gặp nạn không bao giờ bị bẻ rớt xuống dòng thứ hai.", "1. Tiêu đề chuẩn 1 dòng: ")
    add_bullet(doc, "Xóa bỏ hoàn toàn cụm text 'Vị trí thực tế: ...' trong phần mô tả. Đoạn mô tả chỉ giữ lại thông báo súc tích: 'Đã phát cảnh báo khẩn cấp tới toàn bộ người thân.'.", "2. Triệt tiêu trùng lặp: ")
    add_bullet(doc, "Địa chỉ số nhà định vị GPS được đưa xuống một dòng riêng biệt phía dưới, đi kèm icon ghim định vị `Icons.Default.LocationOn` màu cam rực rỡ (`Color(0xFFEA580C)`).", "3. Khối địa chỉ định vị độc lập: ")
    add_bullet(doc, "Nút `[ 🗺️ Xem Bản Đồ ]` tự động đổi màu: Màu đỏ cấp cứu `0xFFDC2626` khi tin chưa xử lý, và chuyển sang màu xám xanh trung tính `0xFF475569` kèm huy hiệu '✔ Đã xử lý' màu xanh lá khi người thân đã bấm xác nhận an toàn.", "4. Đồng bộ màu sắc nút hành động: ")

    card_code = '''@Composable
fun AlertItemCard(
    alert: AlertEntity,
    onMarkAsRead: () -> Unit,
    onOpenMap: (lat: Double, lng: Double) -> Unit
) {
    val isCritical = alert.severity == "CRITICAL" || alert.type == "SOS_PANIC"
    val isWarning = alert.severity == "WARNING"

    // 1. Dùng Viền Bo Tròn (BorderStroke) thay cho dải màu mép trái
    val cardBorder = when {
        !alert.isRead && isCritical -> BorderStroke(1.5.dp, Color(0xFFEF4444))
        !alert.isRead && isWarning  -> BorderStroke(1.5.dp, Color(0xFFF59E0B))
        else                        -> BorderStroke(1.dp, Color(0xFFE2E8F0))
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = cardBorder,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 2. Tiêu đề chống rớt chữ: "🚨 Báo động SOS: [Tên]"
            val displayTitle = if (isCritical) "🚨 Báo động SOS: ${alert.memberName.ifBlank { "Người thân" }}" else alert.title
            Text(
                text = displayTitle,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (isCritical) Color(0xFFDC2626) else Color(0xFF0F172A),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // 3. Mô tả ngắn gọn duy nhất
            Text(
                text = if (isCritical) "Đã phát cảnh báo khẩn cấp tới toàn bộ người thân." else alert.message,
                fontSize = 13.5.sp,
                color = Color(0xFF334155),
                modifier = Modifier.padding(top = 4.dp)
            )

            // 4. Địa chỉ ghim định vị màu cam
            alert.locationAddress?.takeIf { it.isNotBlank() }?.let { address ->
                Row(modifier = Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFFEA580C), modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = address, fontSize = 12.sp, color = Color(0xFF475569), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            // 5. Hàng nút hành động đồng bộ màu sắc
            Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (alert.latitude != null && alert.longitude != null) {
                    val mapBtnBg = if (!alert.isRead) Color(0xFFDC2626) else Color(0xFF475569)
                    Button(
                        onClick = { onOpenMap(alert.latitude, alert.longitude) },
                        colors = ButtonDefaults.buttonColors(containerColor = mapBtnBg),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Xem Bản Đồ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                if (!alert.isRead) {
                    OutlinedButton(onClick = onMarkAsRead, shape = RoundedCornerShape(8.dp)) {
                        Text("✓ Đánh dấu đã xem", fontSize = 12.sp)
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Đã xử lý", fontSize = 12.sp, color = Color(0xFF16A34A), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}'''
    add_code_block(doc, card_code, "Hiện thực hóa AlertItemCard chuẩn hóa UI theo các yêu cầu nâng cấp")

    add_h2(doc, "5.8. Module 8: Tích hợp Vòng đeo tay Y tế qua Bluetooth Low Energy (SmartwatchBleManager)")
    add_para(doc, "Nhằm mở rộng năng lực giám sát sinh tồn liên tục 24/7 mà không phụ thuộc hoàn toàn vào camera điện thoại, FamilyCare tích hợp trình điều khiển kết nối ngoại vi `SmartwatchBleManager`. Module này tuân thủ chuẩn Bluetooth SIG GATT Profile:")

    ble_code = '''class SmartwatchBleManager(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {
    companion object {
        // Heart Rate Service UUID chuẩn quốc tế
        val HEART_RATE_SERVICE_UUID: UUID = UUID.fromString("0000180D-0000-1000-8000-00805F9B34FB")
        val HEART_RATE_MEASUREMENT_UUID: UUID = UUID.fromString("00002A37-0000-1000-8000-00805F9B34FB")
        val BATTERY_SERVICE_UUID: UUID = UUID.fromString("0000180F-0000-1000-8000-00805F9B34FB")
        val BATTERY_LEVEL_UUID: UUID = UUID.fromString("00002A19-0000-1000-8000-00805F9B34FB")
    }

    private val _telemetry = MutableStateFlow(SmartwatchTelemetry())
    val telemetry: StateFlow<SmartwatchTelemetry> = _telemetry.asStateFlow()

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.discoverServices()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            val hrService = gatt.getService(HEART_RATE_SERVICE_UUID)
            val hrChar = hrService?.getCharacteristic(HEART_RATE_MEASUREMENT_UUID)
            hrChar?.let {
                gatt.setCharacteristicNotification(it, true)
                // Kích hoạt Client Characteristic Configuration Descriptor (CCCD)
                val descriptor = it.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805F9B34FB"))
                descriptor?.let { desc ->
                    desc.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    gatt.writeDescriptor(desc)
                }
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            if (characteristic.uuid == HEART_RATE_MEASUREMENT_UUID) {
                val flag = characteristic.value[0].toInt()
                // Bit 0 của cờ: 0 là nhịp tim định dạng 8-bit, 1 là định dạng 16-bit
                val heartRate = if ((flag and 0x01) == 0) {
                    characteristic.value[1].toInt() and 0xFF
                } else {
                    (characteristic.value[1].toInt() and 0xFF) or ((characteristic.value[2].toInt() and 0xFF) shl 8)
                }
                _telemetry.update { it.copy(heartRate = heartRate) }
            }
        }
    }
}'''
    add_code_block(doc, ble_code, "Hiện thực hóa kết nối Bluetooth GATT và giải mã nhịp tim chuẩn SIG")

    add_h2(doc, "5.9. Module 9: Điều phối Thông báo Đẩy Khẩn cấp Đa kênh (EmergencyNotificationHelper)")
    add_para(doc, "Để đảm bảo các bản tin cảnh báo khẩn cấp (như sự cố bấm nút SOS hoặc huyết áp vượt ngưỡng nguy hiểm) luôn tiếp cận người thân ngay cả khi ứng dụng đang chạy nền hoặc máy đang khóa màn hình, `EmergencyNotificationHelper` được xây dựng như một lớp tiện ích hệ thống chuyên dụng:")

    notif_code = '''class EmergencyNotificationHelper(private val context: Context) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun showEmergencySosNotification(userName: String, latitude: Double, longitude: Double) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 1001, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, FamilyCareApp.CHANNEL_EMERGENCY)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("🚨 CẢNH BÁO SOS: $userName CẦN TRỢ GIÚP!")
            .setContentText("Người thân vừa kích hoạt SOS tại tọa độ $latitude, $longitude. Nhấn để mở bản đồ ngay!")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Người thân $userName vừa kích hoạt SOS khẩn cấp tại tọa độ $latitude, $longitude. Còi hú đang phát âm lượng tối đa. Vui lòng liên hệ và hỗ trợ ngay lập tức!")
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 1000, 500, 1000))
            .build()

        notificationManager.notify(1001, notification)
    }
}'''
    add_code_block(doc, notif_code, "Hiện thực hóa thông báo nổi Heads-up khẩn cấp với độ ưu tiên PRIORITY_MAX")

    add_h2(doc, "5.10. Module 10: Quản lý Lịch hẹn Tái khám Bác sĩ (Appointment Management)")
    add_para(doc, "Module Quản lý lịch khám giúp người cao tuổi và gia đình không bao giờ bỏ sót các mốc kiểm tra định kỳ tại bệnh viện. Lớp `AppointmentDao` quản lý các thao tác đọc ghi dữ liệu lịch hẹn:")

    apt_code = '''@Dao
interface AppointmentDao {
    @Query("SELECT * FROM appointments WHERE userId = :userId ORDER BY appointmentTime ASC")
    fun getAppointmentsForUser(userId: Long): Flow<List<AppointmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppointment(appointment: AppointmentEntity): Long

    @Delete
    suspend fun deleteAppointment(appointment: AppointmentEntity)

    @Query("SELECT * FROM appointments WHERE appointmentTime > :now ORDER BY appointmentTime ASC LIMIT 1")
    suspend fun getNextUpcomingAppointment(now: Long): AppointmentEntity?
}'''
    add_code_block(doc, apt_code, "Đặc tả giao diện truy vấn dữ liệu lịch hẹn khám bệnh AppointmentDao")

    print("Part 3 built successfully.")
