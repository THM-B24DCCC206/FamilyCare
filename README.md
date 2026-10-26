# 🏥 FAMILYCARE – HỆ THỐNG GIÁM SÁT SỨC KHỎE & CẢNH BÁO AN TOÀN GIA ĐÌNH THÔNG MINH

> **Đồ án môn học:** Phát triển Ứng dụng Di động Android  
> **Nền tảng:** Android Native (100% Kotlin)  
> **Giao diện:** Jetpack Compose (Material Design 3)  
> **Kiến trúc:** Clean Architecture + MVI / MVVM + Dagger Hilt  
> **Quy mô nhóm:** 3 thành viên  

---

## 📑 MỤC LỤC
1. [Giới thiệu tổng quan](#1-giới-thiệu-tổng-quan)
2. [Kiến trúc & Công nghệ sử dụng](#2-kiến-trúc--công-nghệ-sử-dụng)
3. [Đặc tả chi tiết các chức năng (7 Module)](#3-đặc-tả-chi-tiết-các-chức-năng)
4. [Phân công công việc chi tiết cho nhóm 3 người](#4-phân-công-công-việc-chi-tiết-cho-nhóm-3-người)
5. [Cấu trúc thư mục mã nguồn (Clean Architecture)](#5-cấu-trúc-thư-mục-mã-nguồn)
6. [Quy chuẩn làm việc nhóm & Quy tắc Git](#6-quy-chuẩn-làm-việc-nhóm--quy-tắc-git)
7. [Kế hoạch triển khai tiến độ (4 Tuần)](#7-kế-hoạch-triển-khai-tiến-độ)

---

## 1. GIỚI THIỆU TỔNG QUAN

### 1.1. Đặt vấn đề
Trong xã hội hiện đại, người cao tuổi thường ở nhà một mình trong khi con cái đi làm xa. Việc theo dõi sức khỏe, nhắc nhở uống thuốc đúng giờ và phản ứng kịp thời trước các tình huống khẩn cấp (té ngã, tăng huyết áp đột ngột, đau tim) là bài toán cấp thiết. Hầu hết các ứng dụng trên thị trường hiện nay chỉ dừng lại ở mức "sổ tay ghi chép thủ công" (CRUD đơn thuần) hoặc quá phức tạp đối với người già.

### 1.2. Mục tiêu dự án
**FamilyCare** là giải pháp kết nối hai thế hệ trong một gia đình:
* **Đối với người cao tuổi / Người cần chăm sóc:** Giao diện chữ to, nút bấm cực lớn, tự động hóa tối đa (đo nhịp tim trực tiếp bằng camera, quét đơn thuốc bằng AI, báo thức uống thuốc cưỡng bức toàn màn hình, nút bấm SOS 1-chạm).
* **Đối với con cái / Người giám sát:** Dashboard trực quan, xem biểu đồ chỉ số sinh tồn của bố mẹ theo thời gian thực, nhận thông báo khẩn cấp khi chỉ số vượt ngưỡng hoặc khi bố mẹ quên uống thuốc.

---

## 2. KIẾN TRÚC & CÔNG NGHỆ SỬ DỤNG

### 2.1. Tech Stack
* **Ngôn ngữ:** Kotlin 2.x
* **UI Framework:** Jetpack Compose (Material 3)
* **Xử lý bất đồng bộ & Luồng:** Kotlin Coroutines & StateFlow / SharedFlow
* **Dependency Injection:** Dagger Hilt
* **Lưu trữ CSDL Local:** Room Database (Hỗ trợ Offline-first)
* **Backend & Cloud Services:** Firebase Authentication, Cloud Firestore, Firebase Cloud Messaging (FCM)
* **Thư viện chuyên dụng:**
  * **Google AI Client SDK (Gemini 1.5 Flash):** Trợ lý ảo AI đàm thoại, giải thích chỉ số y tế và tư vấn dùng thuốc.
  * **CameraX:** Bắt luồng hình ảnh phân tích nhịp tim quang học (PPG).
  * **Google ML Kit (Text Recognition):** Nhận diện chữ quét đơn thuốc/hộp thuốc.
  * **Android AlarmManager & Full-screen Intent:** Cơ chế báo thức chuẩn xác bypass Doze Mode.
  * **FusedLocationProviderClient:** Định vị GPS khẩn cấp.
  * **Android PdfDocument API:** Kết xuất báo cáo y tế ra file PDF.

### 2.2. Mô hình kiến trúc Clean Architecture
Dự án tuân thủ nghiêm ngặt 3 tầng độc lập:
* **Presentation Layer:** Jetpack Compose, ViewModel, State/Event.
* **Domain Layer:** Business Logic thuần túy (UseCases), Domain Models, Repository Interfaces (Không phụ thuộc Android Framework).
* **Data Layer:** Room DAO, Firebase Data Sources, Repository Implementations, Mappers.

---

## 3. ĐẶC TẢ CHI TIẾT CÁC CHỨC NĂNG

### Module 1: Xác thực & Mạng lưới Gia đình (Auth & Family Circle)
* **Đăng ký / Đăng nhập:** Đăng nhập an toàn qua Email/Password hoặc Google Sign-in.
* **Tạo "Không gian gia đình" (Family Space):** Người tạo sẽ sinh ra một **Mã mời 6 ký tự ngẫu nhiên (Invite Code)** hoặc Mã QR.
* **Gia nhập gia đình:** Thành viên khác tải app, nhập mã mời để vào chung nhà.
* **Phân quyền người dùng:**
  * *Trưởng gia đình (Admin):* Có quyền chỉnh sửa/xóa hồ sơ thành viên, cấu hình ngưỡng cảnh báo y tế.
  * *Thành viên (Member):* Xem dữ liệu chung, tự đo và cập nhật chỉ số của bản thân.

### Module 2: Hồ sơ Sức khỏe & Trang chủ Tổng quan (Home Dashboard & Profiles)
* **Hồ sơ bệnh án cá nhân:** Lưu trữ ngày sinh, nhóm máu (A, B, AB, O), tiền sử bệnh nền (tiểu đường, cao huyết áp, tim mạch...), danh mục dị ứng thuốc.
* **Thanh chuyển đổi thành viên (Member Switcher):** Trên đỉnh màn hình chính, hiển thị avatar ngang của các thành viên để chuyển đổi góc nhìn theo dõi nhanh.
* **Dashboard trực quan (Overview Cards):**
  * Tóm tắt chỉ số sinh tồn mới nhất (Huyết áp, Nhịp tim).
  * Lịch thuốc trong ngày: Đã uống mấy cữ, cữ tiếp theo lúc mấy giờ?
  * Lịch khám sắp tới trong 7 ngày tới.
  * Thẻ cảnh báo bất thường (nếu có cảnh báo chưa xem).

### Module 3: Chỉ số Sinh tồn & Đo Nhịp tim bằng Camera (Biometrics & PPG)
* **Nhập liệu & Lưu trữ chỉ số:** Huyết áp (Tâm thu/Tâm trương), Đường huyết, Nhiệt độ cơ thể.
* **Biểu đồ trực quan:** Vẽ biểu đồ đường (Line Chart) xu hướng chỉ số theo 7 ngày hoặc 30 ngày gần nhất; hiển thị vùng an toàn (xanh) và vùng nguy hiểm (đỏ).
* ⭐ **TÍNH NĂNG ĐỘC ĐÁO (ĂN ĐIỂM 10): Đo nhịp tim quang học (PPG - Photoplethysmography):**
  * Người dùng đặt đầu ngón tay che kín cụm Camera sau và đèn Flash.
  * App tự động bật Flash, mở `CameraX` với `ImageAnalysis` (YUV_420_888 format).
  * Đọc giá trị cường độ kênh màu Đỏ (Red Component Intensity) trên từng frame (30 FPS). Khi tim đập, máu dồn về mao mạch làm đổi độ hấp thụ ánh sáng.
  * Áp dụng thuật toán phát hiện đỉnh sóng (Peak Detection Algorithm) và bộ lọc thông dải (Bandpass Filter) để tính ra nhịp tim tức thời (**BPM**) và vẽ sóng nhịp tim thời gian thực.
  * Tự động lưu kết quả đo vào lịch sử.

### Module 4: Quản lý Thuốc Thông minh (Smart Medication Engine)
* **Thiết lập đơn thuốc:** Lưu tên thuốc, hướng dẫn sử dụng (uống trước/sau ăn), liều lượng và các mốc giờ uống trong ngày.
* ⭐ **TÍNH NĂNG NÂNG CAO 1: Quét đơn thuốc bằng AI (Google ML Kit Text Recognition):**
  * Chụp ảnh đơn thuốc viết tay/in của bác sĩ hoặc vỏ hộp thuốc.
  * Sử dụng OCR on-device trích xuất chuỗi ký tự, dùng regex/parser thông minh để tự động điền Tên thuốc, Hàm lượng (mg/ml) vào form tạo lịch uống.
* ⭐ **TÍNH NĂNG NÂNG CAO 2: Báo thức chính xác tuyệt đối (Exact Alarm + Full-screen Intent):**
  * Sử dụng `AlarmManager.setExactAndAllowWhileIdle()` để đảm bảo chuông báo nổ đúng giây, kể cả khi máy đang ngủ sâu (Doze Mode).
  * Kích hoạt `FullScreenIntent` bật sáng màn hình hiển thị ảnh viên thuốc, tên thuốc và 2 nút lớn: **"ĐÃ UỐNG"** và **"NHẮC LẠI SAU 10 PHÚT"**.
* **Đồng bộ tuân thủ thuốc:** Bấm "Đã uống" sẽ cập nhật tỷ lệ tuân thủ điều trị (Compliance Rate) và gửi tín hiệu real-time về máy con cái.

### Module 5: Thuật toán Cảnh báo Ngưỡng & SOS Khẩn cấp (Smart Alert & SOS)
* ⭐ **Thuật toán phát hiện bất thường (Rule-based Threshold Engine):**
  * Cho phép thiết lập ngưỡng an toàn cá nhân hóa cho từng người (Ví dụ: Người cao tuổi huyết áp tâm thu > 140 mmHg là nguy cơ).
  * **Xử lý chuỗi sự kiện:** Nếu hệ thống ghi nhận **3 lần đo liên tiếp vượt ngưỡng** trong vòng 48 giờ $\rightarrow$ Tự động tạo một thẻ cảnh báo khẩn cấp màu đỏ trên Dashboard và gửi thông báo đẩy (FCM) đến máy người bảo hộ.
* ⭐ **Hệ thống SOS 1-chạm (One-touch Panic SOS):**
  * Một nút SOS to bản ở vị trí thuận tiện nhất.
  * Khi nhấn giữ 2 giây:
    1. Phát âm thanh còi hú báo động âm lượng tối đa qua Audio Manager.
    2. Gọi `FusedLocationProviderClient` lấy tọa độ GPS chính xác hiện tại.
    3. Gửi thông báo khẩn cấp độ ưu tiên cao nhất (**High-Priority Push Notification**) qua Firebase Cloud Messaging đến điện thoại của tất cả người thân kèm link bản đồ vị trí.

### Module 6: Lịch Tái khám & Xuất Báo cáo Y tế PDF (Appointments & PDF Report)
* **Quản lý lịch khám bệnh:** Lưu tên bệnh viện, chuyên khoa, bác sĩ khám, ngày giờ và dặn dò. Hẹn giờ thông báo trước 1 ngày và trước 2 tiếng.
* ⭐ **Xuất Báo cáo Y tế chuyên nghiệp (Android `PdfDocument`):**
  * Tạo file PDF chuẩn định dạng A4 gồm:
    * Thông tin hành chính bệnh nhân (Họ tên, ngày sinh, nhóm máu, bệnh nền).
    * Biểu đồ biến thiên huyết áp / nhịp tim trong tháng.
    * Bảng thống kê các lần đo vượt ngưỡng cảnh báo.
    * Tỷ lệ tuân thủ uống thuốc.
  * Hỗ trợ xem trước (Preview) và chia sẻ nhanh qua Zalo, Email hoặc in trực tiếp.

### Module 7: Trợ lý Ảo AI Sức Khỏe Gia Đình (AI Health Copilot - Gemini API)
* ⭐ **TÍNH NĂNG ĐỘT PHÁ (GENERATIVE AI TÍCH HỢP):**
  * Tích hợp **Google AI Client SDK for Android** sử dụng mô hình **Gemini 1.5 Flash**.
  * **Hiểu ngữ cảnh hồ sơ bệnh nhân (Context-Aware Health AI):**
    * Khi người dùng hỏi: *"Hôm nay huyết áp của tôi 145/95 thì có sao không?"*, hệ thống tự động gắn kèm ngữ cảnh (Context Injection: Người dùng 68 tuổi, tiền sử cao huyết áp, dị ứng thuốc hạ áp nhóm X) để Gemini đưa ra câu trả lời chuẩn xác, ân cần và mang tính định hướng.
  * **Hỏi đáp cách dùng & tương tác thuốc:** Hỗ trợ người già hỏi nhanh: *"Thuốc này uống trước hay sau ăn?", "Uống chung hai loại thuốc A và B cùng lúc được không?"*.
  * **Nhập liệu bằng giọng nói tự nhiên (Voice-to-Data):** Người già bấm giữ mic nói tự do $\rightarrow$ AI phân tích nội dung, trích xuất con số huyết áp/nhịp tim và triệu chứng để lưu thẳng vào cơ sở dữ liệu.
  * **Tóm tắt y tế cuối tháng:** Tự động sinh đoạn văn tóm tắt diễn biến sức khỏe trong tháng để đính kèm vào báo cáo PDF cho bác sĩ.

---

## 4. PHÂN CÔNG CÔNG VIỆC CHI TIẾT CHO NHÓM 3 NGƯỜI

Để đảm bảo **công bằng khối lượng (Workload Balance)** và **ai cũng có tính năng kỹ thuật nâng cao để bảo vệ trước Hội đồng**, công việc được phân chia cụ thể như sau:

---

### 👤 THÀNH VIÊN 1: Nhóm trưởng – Kiến trúc & Phần cứng (Hardware & Biometrics Specialist)
* **Vai trò:** Dựng khung sườn dự án, chịu trách nhiệm tầng dữ liệu người dùng và tương tác phần cứng Camera.
* **Các màn hình & thành phần phụ trách:**
  1. Khởi tạo dự án, thiết lập `build.gradle.kts`, Hilt Container, Jetpack Navigation đồ thị chung.
  2. **Auth & Family Mesh Screen:** Màn hình Đăng ký/Đăng nhập; Màn hình Tạo/Gia nhập gia đình qua Mã mời 6 ký tự.
  3. **Biometrics Screens:** Màn hình nhập chỉ số huyết áp, đường huyết, nhiệt độ; Màn hình xem biểu đồ 7/30 ngày.
  4. ⭐ **Module đinh (Kỹ thuật khó): Camera PPG Heart Rate Measurement:**
     * Code màn hình Camera đo nhịp tim với `CameraX`.
     * Thuật toán phân tích khung hình YUV, tính độ sáng kênh đỏ, bộ lọc lọc nhiễu tần số và đếm đỉnh xung nhịp (Peak Detection).
     * Vẽ đồ thị xung nhịp tim động (Custom Canvas Waveform) trong khi đo.
* **Package sở hữu chính:**
  * `core/network`, `core/designsystem`
  * `feature/auth`
  * `feature/family`
  * `feature/biometrics` (Bao gồm camera PPG analyzer)
* **Kịch bản trả lời khi bảo vệ trước Hội đồng:**
  * Trình bày kiến trúc Clean Architecture, cách DI hoạt động với Hilt.
  * Giải thích chi tiết nguyên lý quang học PPG và cách xử lý luồng ảnh thời gian thực 30 FPS không gây lag giao diện bằng Coroutines.

---

### 👤 THÀNH VIÊN 2: AI Generative, Hệ thống & Cảnh báo (AI & Emergency Specialist)
* **Vai trò:** Chịu trách nhiệm về giao diện tổng quan, quản lý hồ sơ bệnh án, tích hợp Trợ lý AI Gemini, thuật toán cảnh báo dữ liệu thông minh và hệ thống phản ứng nhanh SOS.
* **Tình trạng hoàn thiện:** ✅ **100% ĐÃ HOÀN TẤT & ĐÃ QUA TOÀN BỘ UNIT TEST**
* **Các màn hình & thành phần phụ trách:**
  1. **Home Dashboard Screen (`presentation/dashboard`):** Màn hình Trang chủ tổng quan chuẩn Material Design 3 (Nền sáng `#F8F9FA`, Medical Blue `#1976D2`, nút to $\ge 56$dp); gồm thanh Member Switcher chuyển đổi thành viên, thẻ cảnh báo đỏ vượt ngưỡng, thẻ tóm tắt huyết áp/nhịp tim và lịch thuốc trong ngày. (`DashboardScreen.kt`, `DashboardViewModel.kt`).
  2. **Profile Screen (`presentation/profile`):** Màn hình xem và **chỉnh sửa trực tiếp** hồ sơ y tế bệnh nhân (nhóm máu, ngày sinh, tiền sử bệnh nền, danh mục dị ứng thuốc); tích hợp lưu trữ Room Database (`UserDao`) và tự động đồng bộ sang Context Injection của AI Copilot. (`ProfileScreen.kt`, `ProfileViewModel.kt`).
  3. ⭐ **Module đinh 1 (Trí tuệ nhân tạo Generative AI): Trợ lý Ảo Sức khỏe (AI Health Copilot):**
     * Thiết kế màn hình Chatbot tư vấn sức khỏe bằng Jetpack Compose.
     * Tích hợp **Google AI Client SDK (Gemini 1.5 Flash)**.
     * Viết kỹ thuật Prompt Engineering với Context Injection: Tự động đính kèm thông tin bệnh nền và chỉ số đo gần nhất vào prompt để Gemini giải thích chỉ số và tư vấn cách dùng thuốc an toàn.
     * Tích hợp nút Micro thu âm giọng nói tự nhiên (**Voice-to-Text**) qua `SpeechRecognizer` (`vi-VN`) và StateFlow quản lý 4 trạng thái (`Idle`, `Loading`, `Success`, `Error`).
     * Tệp mã nguồn: `AiAssistantScreen.kt`, `AiAssistantViewModel.kt`, `HealthAiRepositoryImpl.kt`, `UserHealthContext.kt`, `SendChatMessageUseCase.kt`, `GetHealthContextUseCase.kt`.
  4. ⭐ **Module đinh 2 (Thuật toán): Smart Alert Rule Engine:**
     * Xây dựng `AnalyzeThresholdUseCase` phân tích lịch sử đo sinh tồn trong cửa sổ 48 giờ gần nhất theo ngưỡng an toàn cá nhân hóa (`HealthThreshold`).
     * Thuật toán phát hiện chuỗi **3 lần đo liên tiếp vượt ngưỡng** trong 48 giờ; sinh cảnh báo đỏ và cơ chế Debounce 6 giờ chống spam lặp (`DEFAULT_DEBOUNCE_WINDOW_MS`).
     * Màn hình Trung Tâm Cảnh Báo (`AlertListScreen.kt`, `AlertViewModel.kt`): Quản lý danh sách cảnh báo, bộ lọc theo mức độ (`CRITICAL`, `WARNING`), đánh dấu đã xem, xem định vị Google Maps.
     * Tệp mã nguồn: `AnalyzeThresholdUseCase.kt`, `HealthThreshold.kt`, `ThresholdAnalysisResult.kt`, `Alert.kt`, `AlertDao.kt`, `AlertRepositoryImpl.kt`, `AlertListScreen.kt`, `AlertViewModel.kt`.
  5. ⭐ **Module đinh 3 (Hệ thống): SOS Panic System & Real-time Notification:**
     * Thiết kế nút bấm SOS chống bấm nhầm (`SosButton.kt`): Cơ chế nhấn giữ liên tục trong **2 giây** với vòng tiến trình tròn SVG; nhả tay trước 2s sẽ tự hủy tiến trình về 0.
     * Tích hợp `FusedLocationProviderClient` bắt tọa độ GPS tức thời.
     * Tích hợp hệ thống còi hú cứu hộ cực đại bằng `MediaPlayer` & `AudioManager.STREAM_ALARM`, kết hợp rung khẩn cấp và `EmergencyNotificationHelper` phát Heads-up Notification khẩn cấp kèm link Google Maps.
     * Tệp mã nguồn: `SosButton.kt`, `EmergencyManager.kt`, `EmergencyViewModel.kt`, `EmergencyScreen.kt`, `EmergencyPayload.kt`, `EmergencyNotificationHelper.kt`.
* **Package sở hữu chính:**
  * `presentation/dashboard` (Bao gồm Member Switcher & Dashboard UI)
  * `presentation/profile` (Bao gồm Profile View & Edit Room DB)
  * `presentation/ai_assistant` (Bao gồm Gemini 1.5 Flash SDK & Voice-to-Text)
  * `presentation/alert` (Bao gồm AlertListScreen & AlertViewModel)
  * `presentation/emergency` & `core/emergency` (Bao gồm SosButton, EmergencyManager, GPS & Siren)
  * `core/network` (EmergencyNotificationHelper)
  * `domain/usecase` (`AnalyzeThresholdUseCase`, `SendChatMessageUseCase`, `GetHealthContextUseCase`)
* **Kịch bản trả lời khi bảo vệ trước Hội đồng:**
  * *Câu hỏi 1:* "Tại sao chọn Gemini 1.5 Flash mà không phải model khác?"  
    $\rightarrow$ *Trả lời:* Gemini 1.5 Flash có tốc độ phản hồi cực nhanh (low latency), chi phí tối ưu, cửa sổ ngữ cảnh lớn và hỗ trợ tiếng Việt rất tự nhiên; đặt `temperature = 0.3f` để phản hồi y tế chuẩn xác, không bị bịa thông tin.
  * *Câu hỏi 2:* "Cơ chế Context Injection hoạt động thế nào để tránh lộ lọt dữ liệu?"  
    $\rightarrow$ *Trả lời:* Dữ liệu bệnh án (tuổi, tiền sử, dị ứng, huyết áp đo gần nhất) được truy vấn on-device từ Room Database ngay trước khi gửi tin nhắn, tiêm trực tiếp vào `systemInstruction` của session đó chứ không lưu trữ trên máy chủ công cộng nào khác.
  * *Câu hỏi 3:* "Thuật toán phát hiện chuỗi đo vượt ngưỡng xử lý thế nào khi người dùng đo nhiều lần?"  
    $\rightarrow$ *Trả lời:* Sử dụng cửa sổ trượt 48 giờ (`TIME_WINDOW_48_HOURS_MS`), kiểm tra tính liên tiếp từ lần đo gần nhất; nếu có một lần đo an toàn xen vào thì chuỗi lập tức bị ngắt. Đồng thời có cơ chế Debounce 6 giờ (`DEFAULT_DEBOUNCE_WINDOW_MS`) để không gửi cảnh báo trùng lặp gây phiền toái.
  * *Câu hỏi 4:* "Làm sao đảm bảo nút SOS không bị bấm nhầm khi người già bỏ máy trong túi quần?"  
    $\rightarrow$ *Trả lời:* Sử dụng cử chỉ `detectTapGestures(onPress = ...)` kết hợp `Animatable` yêu cầu nhấn giữ liên tục trọn vẹn 2000ms; chỉ cần ngón tay rời màn hình ở mốc 1.9s thì tiến trình bị hủy và reset về 0 ngay lập tức. Còi hú sử dụng stream `STREAM_ALARM` để phát được âm lượng tối đa kể cả khi máy đang để chế độ im lặng.


---

### 👤 THÀNH VIÊN 3: AI On-Device, Báo thức & Báo cáo (AI & Automation Specialist)
* **Vai trò:** Chịu trách nhiệm về quản lý thuốc, tích hợp AI nhận diện ký tự, cơ chế báo thức chính xác cao và xuất báo cáo PDF.
* **Các màn hình & thành phần phụ trách:**
  1. **Medication Management Screen:** Danh sách lịch uống thuốc, form thêm thuốc thủ công, đánh dấu đã uống và thống kê tỷ lệ tuân thủ.
  2. ⭐ **Module đinh 1 (Trí tuệ nhân tạo): ML Kit OCR Prescription Scanner:**
     * Tích hợp Camera chụp ảnh đơn thuốc / vỏ hộp thuốc.
     * Sử dụng **Google ML Kit Text Recognition** trích xuất văn bản on-device.
     * Xây dựng bộ lọc Regex thông minh để bóc tách Tên thuốc, Đơn vị (mg/ml) tự động điền vào form tạo lịch.
  3. ⭐ **Module đinh 2 (Tiến trình hệ thống): Exact Alarm & Fullscreen Reminder:**
     * Lập lịch báo thức bằng `AlarmManager.setExactAndAllowWhileIdle()`.
     * Viết `BroadcastReceiver` đón giờ hẹn và kích hoạt `Activity` toàn màn hình đè lên màn hình khóa (`showWhenLocked`, `turnScreenOn`).
  4. **Appointments & PDF Export Screen:**
     * Màn hình quản lý lịch hẹn khám bệnh và thông báo nhắc hẹn.
     * Viết Engine xuất file PDF bằng `android.graphics.pdf.PdfDocument`, tính toán tọa độ canvas vẽ bảng biểu, thông tin bệnh nhân và lưu trữ an toàn qua Scoped Storage.
* **Package sở hữu chính:**
  * `feature/medication` (Bao gồm ML Kit OCR Analyzer)
  * `feature/alarm` (Bao gồm AlarmReceiver & Fullscreen Alert Activity)
  * `feature/appointments`
  * `feature/report` (Bao gồm PDF Generator)
* **Kịch bản trả lời khi bảo vệ trước Hội đồng:**
  * Trình bày cách tích hợp Google ML Kit và xử lý trích xuất văn bản trên thiết bị.
  * Giải thích cơ chế hoạt động của `AlarmManager` trên Android 13/14 và cách bypass chế độ tiết kiệm pin Doze Mode để báo thức nổ đúng giờ.
  * Trình bày cách vẽ giao diện văn bản và đồ thị trên trang PDF Canvas.

---

## 5. CẤU TRÚC THƯ MỤC MÃ NGUỒN (CLEAN ARCHITECTURE)

```
com.familycare.app/
│
├── core/
│   ├── common/                  # Resource, Dispatchers, Extensions, Result wrappers
│   ├── database/                # Room DB: Entities, AppDatabase, TypeConverters
│   ├── network/                 # Firebase config, FCM service, Retrofit/Ktor (nếu có)
│   └── designsystem/            # Material 3 Theme, Typography, Reusable Components (Buttons, Dialogs)
│
├── data/
│   ├── repository/              # Triển khai các Repository (Implement Interfaces từ Domain)
│   └── mapper/                  # Chuyển đổi qua lại giữa Entity <-> Domain Model
│
├── domain/
│   ├── model/                   # Data classes thuần túy (User, Family, HealthMetric, Medicine)
│   ├── repository/              # Repository Interfaces định nghĩa các hợp đồng nghiệp vụ
│   └── usecase/                 # Các UseCase độc lập (GetMetricsUseCase, AnalyzeThresholdUseCase,...)
│
└── presentation/
    ├── auth/                    # [Thành viên 1] Login, Register, Invite Family Screens
    ├── biometrics/              # [Thành viên 1] Nhập chỉ số, Biểu đồ, Camera PPG Screen
    ├── dashboard/               # [Thành viên 2] Home Dashboard, Member Switcher
    ├── profile/                 # [Thành viên 2] Quản lý hồ sơ bệnh án
    ├── ai_assistant/            # [Thành viên 2] Chatbot AI Gemini Screen & ViewModel
    ├── alert/                   # [Thành viên 2] Danh sách cảnh báo, Alert Rules
    ├── emergency/               # [Thành viên 2] Nút bấm SOS, Gửi tọa độ GPS
    ├── medication/              # [Thành viên 3] Quản lý thuốc, Quét OCR đơn thuốc
    ├── alarm/                   # [Thành viên 3] Fullscreen Alarm Activity & Receiver
    ├── appointments/            # [Thành viên 3] Lịch khám bệnh
    └── report/                  # [Thành viên 3] Màn hình xem trước & Xuất file PDF
```

---

## 6. QUY CHUẨN LÀM VIỆC NHÓM & QUY TẮC GIT

### 6.1. Quy tắc phân nhánh (Branching Strategy)
* **`main`:** Nhánh ổn định, chỉ merge khi có bản demo chạy mượt, không lỗi biên dịch.
* **`develop`:** Nhánh tích hợp chính của cả nhóm.
* **Nhánh cá nhân theo tính năng:**
  * Thành viên 1: `feature/tv1-auth-biometrics-ppg`
  * Thành viên 2: `feature/tv2-dashboard-ai-sos`
  * Thành viên 3: `feature/tv3-medication-alarm-pdf`

### 6.2. Quy tắc chống xung đột (Conflict Prevention)
1. **Tuyệt đối không sửa chéo thư mục:** Mỗi người chỉ tạo file và code trong đúng package của mình (đã phân công ở mục 4 và 5).
2. **File dùng chung:** Nếu cần sửa các file dùng chung như `NavHost.kt`, `AppDatabase.kt` hoặc `AndroidManifest.xml`, phải thông báo cho Nhóm trưởng (Thành viên 1) trước khi sửa hoặc để Thành viên 1 thực hiện.
3. **Quy trình đẩy code:**
   ```bash
   git checkout develop
   git pull origin develop
   git checkout feature/ten-nhanh-cua-ban
   git merge develop
   # Giải quyết conflict (nếu có) trên máy mình trước
   git push origin feature/ten-nhanh-cua-ban
   # Tạo Pull Request (PR) trên GitHub
   ```

---

## 7. KẾ HOẠCH TRIỂN KHAI TIẾN ĐỘ (4 TUẦN)

| Tuần | Mục tiêu chính | Nhiệm vụ Thành viên 1 | Nhiệm vụ Thành viên 2 | Nhiệm vụ Thành viên 3 |
| :---: | :--- | :--- | :--- | :--- |
| **Tuần 1** | **Khởi tạo & Khung xương CRUD** | Setup Hilt, NavHost, Firebase Auth & Tạo/Gia nhập nhóm gia đình. | Thiết kế UI Dashboard Trang chủ, màn hình Hồ sơ bệnh án. | CRUD Lịch uống thuốc, CRUD Lịch khám bệnh cơ bản. |
| **Tuần 2** | **Tích hợp Kỹ thuật Nâng cao** | Tích hợp CameraX, xử lý luồng YUV, thuật toán đo nhịp tim PPG. | Tích hợp Google AI SDK (Gemini 1.5 Flash), dựng màn hình Chatbot tư vấn sức khỏe. | Tích hợp ML Kit OCR quét đơn thuốc, viết `AlarmManager` Exact Alarm. |
| **Tuần 3** | **Đồng bộ Realtime & Cảnh báo** | Vẽ biểu đồ chỉ số 7/30 ngày, lưu trữ lịch sử nhịp tim PPG vào Room/Firestore. | Hoàn thiện Prompt Context Injection cho AI, thuật toán cảnh báo 3 lần và bắn FCM SOS. | Hoàn thiện Fullscreen Alarm Activity, viết engine kết xuất file PDF. |
| **Tuần 4** | **Kiểm thử & Chuẩn bị Báo cáo** | Tối ưu hóa CameraX và hiệu năng bộ nhớ, sửa bug crash. | Kiểm thử kịch bản hỏi đáp AI, kịch bản vượt ngưỡng và bắn thông báo khẩn cấp chéo giữa 2 máy. | Kiểm thử tính năng xuất PDF, in thử báo cáo; cùng nhóm làm slide & kịch bản demo. |

---

## 8. TIÊU CHÍ ĐÁNH GIÁ ĐỒ ÁN (BẢNG TỰ CHẤM ĐIỂM)

* ✅ **Tính hoàn thiện sản phẩm (100% Native Kotlin & Compose):** Giao diện Material 3 đồng bộ, không lỗi crash ngớ ngẩn.
* ✅ **Độ sâu kỹ thuật Android:** Sử dụng CameraX cấp thấp, Foreground Service/AlarmManager, Fused Location, ML Kit on-device và Generative AI (Google AI SDK).
* ✅ **Kiến trúc phần mềm chuẩn mực:** Clean Architecture 3 lớp rõ ràng, áp dụng Dependency Injection (Hilt), phân luồng Coroutines.
* ✅ **Tính thực tiễn & Giá trị nhân văn:** Giải quyết trọn vẹn bài toán chăm sóc người cao tuổi và kết nối gia đình.
