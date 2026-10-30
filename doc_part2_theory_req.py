# -*- coding: utf-8 -*-
"""
doc_part2_theory_req.py: Chuong 2 (Co so ly thuyet & Cong nghe) va Chuong 3 (Phan tich & Dac ta yeu cau)
Chuong 2 duoc viet lai voi ngon ngu de hieu, ngan gon, truc quan va can chinh thut le phan cap ro rang
"""
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT
from doc_common import (
    add_para, add_h1, add_h2, add_h3, add_h4, add_bullet, 
    create_styled_table, add_callout, add_code_block,
    set_table_borders, set_cell_margins, set_cell_background,
    configure_table_layout,
    COLOR_PRIMARY, COLOR_BLUE, COLOR_SECONDARY, COLOR_TEXT
)

def build_part2(doc):
    print("Building Part 2: Chapter 2 (Rewritten: Intuitive, Concise, Hierarchical Indent) and Chapter 3...")
    
    # ==================== CHƯƠNG 2 ====================
    add_h1(doc, "CHƯƠNG 2: CƠ SỞ LÝ THUYẾT VÀ CÔNG NGHỆ NỀN TẢNG")
    
    # 2.1. Công nghệ đo nhịp tim qua Camera điện thoại (PPG)
    add_h2(doc, "2.1. Công nghệ đo nhịp tim bằng Camera điện thoại (Quang thể tích đồ - PPG)")
    add_para(doc, "Quang thể tích đồ (Photoplethysmography - gọi tắt là PPG) là phương pháp đo nhịp tim bằng ánh sáng quang học cực kỳ an toàn, không cần chọc kim hay can thiệp vào cơ thể. Thay vì phải mua đồng hồ thông minh hay máy đo đắt tiền, phương pháp này tận dụng ngay chính chiếc camera sau và đèn flash có sẵn trên điện thoại thông minh.")

    add_h3(doc, "2.1.1. Nguyên lý hoạt động cơ bản: Máu lưu thông làm đổi màu ngón tay", left_indent=Inches(0.2))
    add_para(doc, "Trái tim con người hoạt động giống hệt một chiếc máy bơm nước nhịp nhàng:", left_indent=Inches(0.2))
    add_bullet(doc, "Mỗi khi tim co bóp tống máu đi khắp cơ thể (kỳ tâm thu), một lượng máu giàu oxy được bơm dồn về các mao mạch ở đầu ngón tay. Lượng máu tăng lên làm ngón tay căng nhẹ và hấp thụ nhiều ánh sáng hơn, khiến vùng da ở đầu ngón tay chuyển sang màu đỏ sẫm hơn một chút.", "a) Khi tim đập (Tống máu): ", left_indent=Inches(0.35))
    add_bullet(doc, "Ngay sau đó, tim giãn ra để hút máu về (kỳ tâm trương), áp lực máu giảm xuống, thể tích máu tại ngón tay rút bớt, làm da sáng màu trở lại.", "b) Khi tim nghỉ (Hút máu về): ", left_indent=Inches(0.35))
    add_para(doc, "Mắt thường chúng ta không thể nhận ra sự đổi màu cực nhỏ này. Tuy nhiên, khi người dùng đặt nhẹ đầu ngón tay che kín ống kính camera và bật đèn flash chiếu sáng xuyên qua da, camera sẽ ghi lại được sự thay đổi sáng - tối của màu đỏ 30 lần mỗi giây. Ứng dụng chỉ cần đếm số lần đổi màu này trong một phút là tính ra chính xác nhịp tim (BPM - Beats Per Minute).", left_indent=Inches(0.2))

    add_h3(doc, "2.1.2. Phân biệt tín hiệu mạch đập thật và các tín hiệu tĩnh (AC và DC)", left_indent=Inches(0.2))
    add_para(doc, "Tín hiệu ánh sáng mà camera thu được từ ngón tay gồm 2 phần riêng biệt:", left_indent=Inches(0.2))
    add_bullet(doc, "Là phần ánh sáng biến đổi nhấp nhô liên tục theo từng nhịp đập của tim. Tín hiệu này có tần số từ 0.75 Hz đến 3.2 Hz (tương đương 45 đến 192 nhịp tim/phút). Đây chính là phần dữ liệu quý giá mà ứng dụng cần giữ lại để tính nhịp tim.", "• Phần mạch đập (Thành phần AC): ", left_indent=Inches(0.35))
    add_bullet(doc, "Là phần ánh sáng cố định, không thay đổi theo nhịp tim do chiếu qua xương ngón tay, mô mỡ, sắc tố da và lượng máu tĩnh mạch nằm yên. Thành phần này còn chứa cả các dao động chậm do người dùng thở hoặc ấn tay không đều.", "• Phần ánh sáng tĩnh (Thành phần DC): ", left_indent=Inches(0.35))
    add_para(doc, "Nhiệm vụ đầu tiên của thuật toán là trừ bỏ phần ánh sáng tĩnh DC này đi, chỉ giữ lại phần sóng AC dập dềnh phản ánh đúng nhịp đập của quả tim.", left_indent=Inches(0.2))

    add_h3(doc, "2.1.3. Bộ lọc số Butterworth: Chiếc \"rây lọc\" loại bỏ rung tay và nhiễu đèn", left_indent=Inches(0.2))
    add_para(doc, "Trong thực tế, khi cầm điện thoại đo nhịp tim, tay người già có thể hơi run nhẹ, hoặc ánh đèn điện trong phòng (50Hz) chớp tắt tạo ra các tín hiệu rác (nhiễu). Nếu không xử lý, ứng dụng sẽ đếm nhầm các rung động này thành nhịp tim.", left_indent=Inches(0.2))
    add_para(doc, "FamilyCare sử dụng bộ lọc số **Butterworth bậc 2**. Bộ lọc này hoạt động như một chiếc rây thông minh:", left_indent=Inches(0.2))
    add_bullet(doc, "Gạt bỏ triệt để tất cả các dao động quá chậm dưới 0.75 Hz (tức dưới 45 nhịp/phút) sinh ra do nhịp thở hoặc ngón tay xê dịch.", "1. Chặn tần số quá thấp: ", left_indent=Inches(0.35))
    add_bullet(doc, "Cắt bỏ toàn bộ các dao động quá nhanh trên 3.2 Hz (tức trên 192 nhịp/phút) do bóng đèn huỳnh quang hoặc nhiễu điện tử của cảm biến camera gây ra.", "2. Chặn tần số quá cao: ", left_indent=Inches(0.35))
    add_bullet(doc, "Chỉ cho phép duy nhất dải tần số từ 45 đến 192 nhịp/phút đi qua, đây chính là dải nhịp tim an toàn của con người.", "3. Giữ lại dải nhịp tim chuẩn: ", left_indent=Inches(0.35))

    add_h3(doc, "2.1.4. Thuật toán phát hiện đỉnh sóng (Peak Detection) để tính nhịp tim", left_indent=Inches(0.2))
    add_para(doc, "Sau khi đi qua bộ lọc Butterworth, đường tín hiệu sẽ biến thành những con sóng uốn lượn đều đặn rất đẹp mắt. Mỗi ngọn sóng nhô cao nhất tương ứng với một lần quả tim co bóp tống máu.", left_indent=Inches(0.2))
    add_para(doc, "Thuật toán phát hiện đỉnh sóng hoạt động qua 2 bước đơn giản:", left_indent=Inches(0.2))
    add_bullet(doc, "Điểm được chọn làm đỉnh sóng phải cao hơn tất cả các điểm lân cận xung quanh trong khoảng thời gian nửa giây (500 mili-giây).", "Bước 1: Tìm điểm cao nhất cục bộ: ", left_indent=Inches(0.35))
    add_bullet(doc, "Điểm đó phải vượt qua một đường ngưỡng trung bình tự động điều chỉnh theo độ đậm nhạt của từng người, tránh bắt nhầm các gợn sóng lăn tăn.", "Bước 2: So sánh với ngưỡng động: ", left_indent=Inches(0.35))
    add_bullet(doc, "Đo thời gian giữa 2 đỉnh liên tiếp (tính bằng mili-giây). Ví dụ khoảng cách là 800ms, nhịp tim tức thời sẽ là: (60 x 1000) / 800 = 75 nhịp/phút (BPM). Hệ thống lấy trung bình các nhịp trong 15 giây để cho ra kết quả đo chính xác nhất.", "Bước 3: Quy đổi ra nhịp tim: ", left_indent=Inches(0.35))

    # 2.2. Xử lý ảnh CameraX mượt mà, không nóng máy
    add_h2(doc, "2.2. Xử lý ảnh thời gian thực với Android CameraX: Nhẹ máy và Tiết kiệm pin")
    add_para(doc, "Để đo được nhịp tim, điện thoại phải quay camera liên tục 30 hình/giây. Nếu lập trình không khéo, máy sẽ rất nhanh nóng, tốn pin và bị giật lag (treo ứng dụng). FamilyCare sử dụng thư viện CameraX chính thức của Google với các kỹ thuật tối ưu phần cứng:")

    add_h3(doc, "2.2.1. Trích xuất màu đỏ từ định dạng YUV_420_888", left_indent=Inches(0.2))
    add_para(doc, "Mỗi khung hình từ camera điện thoại truyền về ở định dạng màu YUV (gồm độ sáng Y và độ lệch màu UV). Ứng dụng áp dụng công thức toán học nhanh để đổi sang mức độ màu đỏ (Red Channel):", left_indent=Inches(0.2))
    add_para(doc, "Mức đỏ R = Độ sáng Y + 1.402 x (Độ lệch màu V - 128)", italic=True, align=WD_ALIGN_PARAGRAPH.CENTER)
    add_para(doc, "Để không làm đơ máy, ứng dụng không quét toàn bộ bức ảnh lớn (hàng triệu điểm ảnh) mà chỉ cắt lấy một ô vuông nhỏ 100x100 điểm ảnh ngay chính giữa camera – nơi đầu ngón tay tiếp xúc tốt nhất. Thuật toán tính độ đỏ trung bình của ô vuông này chỉ mất chưa đầy 3 mili-giây, giúp CPU hoàn toàn rảnh rỗi.", left_indent=Inches(0.2))

    add_h3(doc, "2.2.2. Chiến lược xử lý không chặn (Non-Blocking Drop-Frame)", left_indent=Inches(0.2))
    add_para(doc, "Trong chế độ phân tích ảnh, hệ thống cấu hình cờ `STRATEGY_KEEP_ONLY_LATEST`:", left_indent=Inches(0.2))
    add_bullet(doc, "Hệ thống chỉ giữ lại bức ảnh mới nhất vừa chụp. Nếu máy đang bận tính toán nhịp tim của khung hình trước, các khung hình trung gian sẽ tự động bị bỏ qua (Drop frame) thay vì xếp hàng chờ đợi. Điều này giúp bộ nhớ không bao giờ bị đầy (tránh lỗi tràn bộ nhớ Out-Of-Memory).", "• Chống tràn bộ nhớ: ", left_indent=Inches(0.35))
    add_bullet(doc, "Ngay sau khi tính xong độ đỏ của khung hình, lệnh `imageProxy.close()` được gọi lập tức để trả lại bộ nhớ cho phần cứng camera chụp tiếp ảnh mới.", "• Thu hồi bộ nhớ tức thì: ", left_indent=Inches(0.35))

    # 2.3. Trí tuệ nhân tạo tạo sinh Gemini 1.5 Flash
    add_h2(doc, "2.3. Trí tuệ nhân tạo tạo sinh Google Gemini 1.5 Flash: Bác sĩ gia đình thông thái")
    add_para(doc, "Khác với các ứng dụng sức khỏe thông thường chỉ hiển thị các con số khô khan, FamilyCare tích hợp mô hình ngôn ngữ lớn tiên tiến nhất của Google là **Gemini 1.5 Flash** để trò chuyện, giải thích và chăm sóc người bệnh như một bác sĩ gia đình thực thụ.")

    add_h3(doc, "2.3.1. Ưu điểm nổi bật của Gemini 1.5 Flash", left_indent=Inches(0.2))
    add_bullet(doc, "Tốc độ phản hồi cực nhanh (chỉ mất hơn 1 giây để trả lời xong câu hỏi), người cao tuổi không phải chờ đợi lâu.", "• Tốc độ siêu tốc: ", left_indent=Inches(0.35))
    add_bullet(doc, "Khả năng đọc hiểu ngữ cảnh khổng lồ (lên tới 1 triệu từ), cho phép nạp toàn bộ lịch sử đo huyết áp, tiền sử bệnh nền và danh sách thuốc của người bệnh trong nhiều tháng vào cuộc trò chuyện mà không bị quên.", "• Trí nhớ dài hạn: ", left_indent=Inches(0.35))

    add_h3(doc, "2.3.2. Kỹ thuật Tiêm ngữ cảnh y bạ (Context Injection): Trả lời đúng người, đúng bệnh", left_indent=Inches(0.2))
    add_para(doc, "Một nhược điểm nguy hiểm của các ứng dụng AI thông thường là trả lời chung chung hoặc khuyên sai (ví dụ: khuyên người cao huyết áp uống thuốc có thành phần họ bị dị ứng). FamilyCare giải quyết triệt để vấn đề này bằng kỹ thuật **Tiêm ngữ cảnh tự động**:", left_indent=Inches(0.2))
    add_bullet(doc, "Khi người dùng hỏi một câu (ví dụ: 'Hôm nay tôi thấy hơi chóng mặt, có sao không?'), ứng dụng tự động truy vấn hồ sơ bệnh nhân từ cơ sở dữ liệu trên máy (Họ tên, Tuổi tác, Nhóm máu, Bệnh nền cao huyết áp/tiểu đường, Danh mục dị ứng thuốc Penicillin, và chỉ số Huyết áp/Nhịp tim vừa đo sáng nay).", "1. Bước lấy hồ sơ: ", left_indent=Inches(0.35))
    add_bullet(doc, "Ứng dụng đóng gói hồ sơ này vào một chỉ thị bí mật gửi kèm câu hỏi tới Gemini AI: 'Bạn là Bác sĩ gia đình của bệnh nhân Tuấn, 72 tuổi, tiền sử cao huyết áp, huyết áp sáng nay là 145/95 mmHg. Hãy trả lời ân cần, giải thích nguyên nhân và khuyên chế độ nghỉ ngơi phù hợp'.", "2. Bước tiêm ngữ cảnh: ", left_indent=Inches(0.35))
    add_bullet(doc, "Nhờ đó, câu trả lời của AI mang tính cá nhân hóa 100%, đúng thể trạng của người bệnh, giúp các cụ an tâm và hiểu rõ tình trạng sức khỏe của mình.", "3. Kết quả cá nhân hóa: ", left_indent=Inches(0.35))

    add_h3(doc, "2.3.3. Khóa thông số Temperature = 0.3: Ngăn chặn tuyệt đối bịa đặt thông tin y tế", left_indent=Inches(0.2))
    add_para(doc, "Trong trí tuệ nhân tạo, thông số `temperature` quy định mức độ 'sáng tạo bay bổng' của mô hình. Với làm thơ hay viết văn, nhiệt độ cao giúp câu từ phong phú; nhưng trong y tế, nhiệt độ cao sẽ dẫn đến hiện tượng 'ảo giác' (AI nói linh tinh hoặc bịa đặt tên thuốc nguy hiểm).", left_indent=Inches(0.2))
    add_para(doc, "Nhóm tác giả đã khóa cứng thông số `temperature = 0.3f`. Điều này buộc Gemini 1.5 Flash phải trả lời một cách chuẩn mực, thận trọng, tuyệt đối tuân theo kiến thức y khoa chính thống, từ chối kê đơn thuốc bừa bãi và luôn nhắc nhở người bệnh đi khám bác sĩ khi có dấu hiệu nguy kịch.", left_indent=Inches(0.2))

    add_h3(doc, "2.3.4. Nói chuyện tự nhiên bằng giọng nói tiếng Việt (Speech-to-Text)", left_indent=Inches(0.2))
    add_para(doc, "Người già mắt mờ, tay run rất khó gõ bàn phím chữ nhỏ trên màn hình cảm ứng. Ứng dụng tích hợp bộ nhận diện giọng nói `Android SpeechRecognizer` trực tiếp:", left_indent=Inches(0.2))
    add_bullet(doc, "Người dùng chỉ cần chạm giữ nút Micro màu xanh và nói tự nhiên bằng tiếng Việt như đang gọi điện thoại cho con cháu.", "• Thao tác 1 chạm: ", left_indent=Inches(0.35))
    add_bullet(doc, "Hệ thống tự động chuyển giọng nói thành văn bản hiển thị lên ô chat và gửi cho AI trả lời tức thì.", "• Chuyển đổi chữ viết tức thì: ", left_indent=Inches(0.35))

    # 2.4. Đọc đơn thuốc tự động bằng Camera (Google ML Kit OCR)
    add_h2(doc, "2.4. Công nghệ đọc đơn thuốc tự động bằng Camera (Google ML Kit OCR)")
    add_para(doc, "Người cao tuổi mắc bệnh mãn tính thường phải uống rất nhiều loại thuốc với những cái tên Tây y dài dòng khó nhớ (như Amlodipine, Losartan, Metformin...). Việc phải tự tay gõ từng tên thuốc vào điện thoại là cực hình đối với các cụ.")
    add_para(doc, "FamilyCare ứng dụng công nghệ Thị giác máy tính nhận dạng ký tự quang học (OCR) của Google ML Kit chạy hoàn toàn Offline trên máy:", left_indent=Inches(0.2))
    add_bullet(doc, "Người bệnh chỉ cần cầm điện thoại chụp bức ảnh đơn thuốc hoặc vỏ hộp thuốc do bác sĩ kê.", "• Bước 1 (Chụp ảnh đơn thuốc): ", left_indent=Inches(0.35))
    add_bullet(doc, "Mô hình trí tuệ nhân tạo On-device tự động quét từng dòng chữ trên ảnh mà không cần kết nối mạng Internet, đảm bảo an toàn riêng tư.", "• Bước 2 (Quét chữ tự động): ", left_indent=Inches(0.35))
    add_bullet(doc, "Bộ lọc quy tắc thông minh (Regex) sẽ tự động bóc tách: Tên biệt dược (VD: Amlodipine), Hàm lượng (VD: 5mg), và Số lần uống (VD: Ngày 1 viên vào buổi sáng) để tự động điền vào lịch nhắc thuốc.", "• Bước 3 (Bóc tách thông tin thuốc): ", left_indent=Inches(0.35))

    # 2.5. Chuông báo thức cưỡng bức chống quên thuốc (Exact Alarm)
    add_h2(doc, "2.5. Chuông báo thức cưỡng bức: Bật sáng màn hình, không bao giờ trễ giờ thuốc")
    add_para(doc, "Các hệ điều hành Android hiện đại (từ Android 8 đến Android 15) đều có tính năng tiết kiệm pin rất gắt gao gọi là **Doze Mode** (Chế độ ngủ sâu). Khi người dùng để điện thoại trên bàn một lúc không dùng, máy sẽ tắt ngắt các tiến trình chạy ngầm để đỡ hao pin. Hậu quả là các ứng dụng nhắc nhở thông thường sẽ bị hoãn lại, có khi trễ tới 30 - 45 phút, gây nguy hiểm cho người bệnh tim mạch cần uống thuốc đúng giờ.")
    add_para(doc, "FamilyCare giải quyết triệt để vấn đề này bằng giải pháp báo thức cưỡng bức hai lớp:", left_indent=Inches(0.2))
    add_bullet(doc, "Sử dụng hàm đặc quyền `AlarmManager.setExactAndAllowWhileIdle()`. Lệnh này ra lệnh trực tiếp cho con chip đồng hồ thời gian thực (RTC) của phần cứng điện thoại đánh thức máy dậy đúng chính xác đến từng giây, phá vỡ hoàn toàn rào cản ngủ sâu Doze Mode.", "1. Đánh thức phần cứng đúng giây: ", left_indent=Inches(0.35))
    add_bullet(doc, "Khi đến giờ uống thuốc, thay vì chỉ hiện một dòng thông báo nhỏ dễ bị bỏ qua, hệ thống kích hoạt màn hình toàn cảnh `FullScreenIntent`. Điện thoại sẽ tự động bật sáng màn hình ngay cả khi đang khóa phím, phát chuông báo to rõ và hiện ảnh viên thuốc to cùng 2 nút lớn: 'ĐÃ UỐNG' và 'NHẮC LẠI SAU 10 PHÚT'.", "2. Bật sáng màn hình khóa (Full-Screen): ", left_indent=Inches(0.35))

    # 2.6. Thích ứng phần cứng đa dạng giữa các hãng điện thoại
    add_h2(doc, "2.6. Tự động thích ứng với nhiều dòng điện thoại Android khác nhau")
    add_para(doc, "Tại Việt Nam, người cao tuổi sử dụng rất nhiều dòng máy điện thoại khác nhau từ các thương hiệu phổ biến như Samsung, Xiaomi, Oppo, Realme, Google Pixel hay Vsmart. Mỗi hãng lại đặt đèn flash ở vị trí xa gần khác nhau so với camera và có cách xử lý màu sắc khác nhau.")
    add_para(doc, "Để ứng dụng hoạt động chính xác trên mọi dòng máy, FamilyCare tích hợp cơ chế tự hiệu chuẩn thông minh:", left_indent=Inches(0.2))
    add_bullet(doc, "Khi vừa bật camera đo nhịp tim, ứng dụng lập tức khóa chặt độ sáng (khóa Auto-Exposure) và khóa tiêu cự (khóa Auto-Focus). Điều này ngăn không cho camera tự ý đổi độ sáng làm rung lắc tín hiệu nhịp tim.", "• Khóa cứng độ sáng và tiêu cự: ", left_indent=Inches(0.35))
    add_bullet(doc, "Hệ thống kiểm tra 10 khung hình đầu tiên. Nếu phát hiện ngón tay ấn quá chặt (làm nghẽn mạch máu khiến ảnh trắng bệch) hoặc ấn quá nhẹ (lọt ánh sáng ngoài vào), ứng dụng sẽ hiện hướng dẫn bằng giọng nói và chữ lớn nhắc người dùng điều chỉnh lại ngón tay cho chuẩn xác.", "• Tự động nhắc nhở lực ấn tay: ", left_indent=Inches(0.35))

    print("Part 2 built successfully (Chapter 2 rewritten cleanly with intuitive language & indented hierarchy).")
    
    # ==================== CHƯƠNG 3 ====================
    add_h1(doc, "CHƯƠNG 3: PHÂN TÍCH VÀ ĐẶC TẢ YÊU CẦU HỆ THỐNG")
    
    add_h2(doc, "3.1. Phân tích các bên liên quan và Mô hình tác nhân (Actors)")
    add_para(doc, "Hệ thống FamilyCare được vận hành xoay quanh 3 tác nhân chính tương tác lẫn nhau trong một vòng tròn bảo vệ khép kín:")
    
    actor_headers = ["Tác nhân (Actor)", "Mô tả vai trò", "Mục tiêu và Hành vi chính trong hệ thống"]
    actor_data = [
        ("Thành viên gia đình / Người cao tuổi (Member)", "Người cần theo dõi sức khỏe thường xuyên tại nhà, thao tác trực tiếp trên ứng dụng.", "- Đo nhịp tim qua Camera sau.\n- Nhập chỉ số huyết áp, đường huyết hàng ngày.\n- Nhận chuông báo thức uống thuốc và bấm 'Đã uống'.\n- Đàm thoại với Trợ lý AI qua micro giọng nói.\n- Bấm giữ phím cứu hộ SOS khi gặp sự cố khẩn cấp."),
        ("Trưởng gia đình / Con cái bảo hộ (Admin)", "Người tạo lập mạng lưới gia đình, giám sát tình trạng sức khỏe của cha mẹ từ xa.", "- Tạo 'Không gian gia đình' và cấp mã mời 6 ký tự.\n- Thiết lập ngưỡng cảnh báo an toàn cho cha mẹ.\n- Xem Dashboard tổng quan chỉ số sinh tồn của gia đình.\n- Nhận thông báo khẩn cấp và định vị GPS khi có SOS.\n- Xuất báo cáo y bạ PDF để đưa cha mẹ đi khám bệnh."),
        ("Hệ thống ngầm (System Daemon / Background)", "Tiến trình chạy nền độc lập quản lý lịch trình và phân tích dữ liệu tự động.", "- Lập lịch báo thức chuẩn xác với phần cứng RTC.\n- Lắng nghe luồng ảnh CameraX ở 30 FPS.\n- Quét chuỗi dữ liệu 48h để phát hiện bất thường.\n- Đồng bộ tin nhắn khẩn cấp qua hạ tầng FCM đám mây.")
    ]
    create_styled_table(doc, actor_headers, actor_data, [Inches(1.6), Inches(2.15), Inches(2.5)], "Bảng 3.1: Danh mục tác nhân và vai trò tương tác trong hệ thống FamilyCare")

    add_h2(doc, "3.2. Yêu cầu chức năng chi tiết (Functional Requirements)")
    add_para(doc, "Hệ thống bao gồm 7 phân hệ chức năng hoàn chỉnh, được mã hóa từ FR-01 đến FR-07:")
    
    req_headers = ["Mã yêu cầu", "Phân hệ chức năng", "Mô tả đặc tả kỹ thuật chi tiết", "Mức độ ưu tiên"]
    req_data = [
        ("FR-01", "Xác thực & Mạng gia đình", "Đăng ký/Đăng nhập Email an toàn. Tạo gia đình sinh mã mời ngẫu nhiên 6 ký tự duy nhất. Thành viên nhập mã mời để gia nhập chung tổ ấm. Phân quyền Admin / Member rõ ràng.", "Bắt buộc (High)"),
        ("FR-02", "Hồ sơ Bệnh án & Dashboard", "Lưu trữ thông tin y bạ (Nhóm máu, ngày sinh, tiền sử bệnh nền mãn tính, danh mục thuốc dị ứng). Thanh chuyển đổi thành viên (Member Switcher) xem nhanh tình trạng cha mẹ. Thẻ tóm tắt huyết áp, cữ thuốc trong ngày.", "Bắt buộc (High)"),
        ("FR-03", "Đo Nhịp Tim Quang Học PPG", "Tự động kích hoạt đèn Flash, mở CameraX luồng YUV ở 30 FPS. Kiểm tra độ che phủ ngón tay. Lọc thông dải Butterworth và nhận diện đỉnh sóng thời gian thực. Hiển thị đồ thị xung nhịp động và lưu kết quả BPM vào CSDL.", "Bắt buộc (High)"),
        ("FR-04", "Trợ lý AI Health Copilot", "Tích hợp Google Gemini 1.5 Flash SDK. Tiêm ngữ cảnh y bạ (Context Injection) từ CSDL Room. Thu âm giọng nói tự nhiên qua SpeechRecognizer chuyển thành văn bản. Khóa tham số temperature = 0.3 chống ảo giác y khoa.", "Bắt buộc (High)"),
        ("FR-05", "Thuật toán Cảnh báo & SOS Panic", "Thuật toán quét lịch sử đo trong cửa sổ trượt 48h; nếu 3 lần đo liên tiếp vượt ngưỡng an toàn sẽ tự sinh thẻ cảnh báo đỏ và khóa Debounce 6h. Nút bấm SOS giữ 2s kích hoạt còi hú âm lượng tối đa, bắt tọa độ GPS và bắn thông báo FCM.", "Bắt buộc (High)"),
        ("FR-06", "Quản lý Thuốc & Báo thức Cưỡng bức", "Quét đơn thuốc qua Google ML Kit Text Recognition tự bóc tách tên thuốc/liều lượng. Lập lịch Exact Alarm bằng AlarmManager bypass Doze Mode. Bật sáng màn hình khóa Full-screen Intent đè lên màn hình khóa.", "Bắt buộc (High)"),
        ("FR-07", "Lịch Khám & Xuất Báo Cáo PDF", "Lưu trữ lịch hẹn tái khám bác sĩ. Động cơ Android PdfDocument kết xuất file PDF chuẩn khổ A4 gồm: thông tin bệnh nhân, biểu đồ huyết áp tháng, bảng kê các lần vượt ngưỡng và tỷ lệ tuân thủ điều trị thuốc.", "Trung bình (Medium)")
    ]
    create_styled_table(doc, req_headers, req_data, [Inches(0.85), Inches(1.65), Inches(2.95), Inches(0.8)], "Bảng 3.2: Ma trận yêu cầu chức năng phần mềm FamilyCare")

    add_h2(doc, "3.3. Yêu cầu phi chức năng (Non-Functional Requirements)")
    add_bullet(doc, "Thời gian phản hồi các thao tác chuyển trang và nạp dữ liệu nội bộ < 100ms. Độ trễ phân tích khung hình Camera PPG < 15ms/frame đảm bảo duy trì ổn định 30 FPS. Thời gian xử lý đàm thoại AI Copilot < 1.5 giây.", "1. Hiệu năng và Độ mượt mà (Performance): ")
    add_bullet(doc, "Dữ liệu được lưu trữ phân tán Offline-first trên SQLite Room Database, đảm bảo các chức năng đo nhịp tim, báo thức thuốc và phát còi SOS hoạt động độc lập 100% ngay cả khi mất mạng Wi-Fi/4G.", "2. Tính sẵn sàng và Bền bỉ (Availability & Reliability): ")
    add_bullet(doc, "Mật khẩu người dùng được băm an toàn. Dữ liệu y bạ cá nhân chỉ lưu nội bộ trên máy bệnh nhân, chỉ đồng bộ qua mạng khi có yêu cầu chia sẻ từ người thân.", "3. An toàn thông tin và Quyền riêng tư (Security & Privacy): ")
    add_bullet(doc, "Tuân thủ nghiêm ngặt bộ tiêu chuẩn Material Design 3 Accessibility: Cỡ chữ mặc định từ 13sp đến 16sp, độ tương phản màu đạt chuẩn WCAG 2.1 AA (tỷ lệ tương phản tối thiểu 4.5:1), diện tích chạm nút bấm tối thiểu 48x48dp.", "4. Khả năng tiếp cận người cao tuổi (Usability & Accessibility): ")
    add_bullet(doc, "Kiến trúc Clean Architecture phân tách rõ ràng 4 tầng giúp dễ dàng thay thế mô hình AI (chuyển sang Gemini Nano) hoặc mở rộng sang nền tảng iOS trong tương lai.", "5. Khả năng bảo trì và Mở rộng (Maintainability): ")

    add_h2(doc, "3.4. Bảng Ma trận Truy xuất Nguồn gốc Yêu cầu (Requirements Traceability Matrix - RTM)")
    add_para(doc, "Ma trận RTM thể hiện mối quan hệ đối soát chặt chẽ giữa các yêu cầu nghiệp vụ (FR), ca sử dụng (Use Case), thực thể dữ liệu (Room Entity), tầng giao diện (ViewModel) và ca kiểm thử chấp nhận (Test Case):")
    
    rtm_headers = ["Mã Yêu Cầu", "Tên Phân Hệ", "Use Case Liên Kết", "Bảng CSDL Phụ Trách", "Thành Phần Mã Nguồn / ViewModel", "Test Cases Đối Soát"]
    rtm_data = [
        ("FR-01", "Xác thực & Gia đình", "UC-01, UC-13", "users", "AuthViewModel, FamilyViewModel", "TC-01, TC-02, TC-03, TC-04"),
        ("FR-02", "Hồ sơ & Dashboard", "UC-09, UC-15", "users, health_metrics", "HomeViewModel, ProfileViewModel", "TC-10, TC-11, TC-27"),
        ("FR-03", "Đo Nhịp Tim PPG", "UC-02, UC-12", "health_metrics", "CameraPpgViewModel, PpgSignalProcessor", "TC-05, TC-06, UT-01, UT-03"),
        ("FR-04", "AI Health Copilot", "UC-04", "users, health_metrics", "AiHealthAssistantViewModel, GeminiRepo", "TC-14, TC-15, UT-21, UT-22"),
        ("FR-05", "Cảnh Báo & SOS Panic", "UC-03, UC-07, UC-08", "alerts", "EmergencyViewModel, AlertCenterViewModel", "TC-07, TC-08, TC-09, TC-12, TC-20"),
        ("FR-06", "Tủ Thuốc & Báo Thức", "UC-05, UC-06", "medicines, medication_logs", "MedicineViewModel, AlarmReceiver", "TC-16, TC-17, TC-18, UT-09, UT-23"),
        ("FR-07", "Lịch Khám & Xuất PDF", "UC-10, UC-11, UC-14", "appointments, health_metrics", "AppointmentViewModel, HealthReportPdfExporter", "TC-19, UT-24, UT-25")
    ]
    create_styled_table(doc, rtm_headers, rtm_data, [Inches(0.7), Inches(1.1), Inches(1.0), Inches(1.0), Inches(1.35), Inches(1.1)], "Bảng 3.3: Ma trận truy xuất nguồn gốc yêu cầu (Requirements Traceability Matrix)")

    add_h2(doc, "3.5. Bảng đặc tả 15 Use Case chi tiết của hệ thống")
    add_para(doc, "Dưới đây là bảng đặc tả chi tiết toàn bộ 15 ca sử dụng cốt lõi và mở rộng của hệ thống:")
    
    uc_list = [
        ("UC-01", "Gia nhập tổ ấm gia đình qua Mã mời", "Member", "Người dùng đã đăng ký tài khoản và nhận được mã 6 ký tự từ Admin.", "1. Người dùng mở màn hình 'Tổ Ấm Gia Đình'.\n2. Chọn 'Gia nhập tổ ấm'.\n3. Nhập 6 ký tự mã mời (ví dụ: FAM892).\n4. Bấm 'Xác nhận gia nhập'.", "Hệ thống kiểm tra mã trên CSDL, liên kết User vào FamilyId và chuyển thẳng đến Dashboard chung.", "Mã không tồn tại: Báo lỗi 'Mã mời không chính xác, vui lòng thử lại'."),
        ("UC-02", "Đo nhịp tim quang học qua Camera PPG", "Member", "Thiết bị có Camera sau và Flash LED, đã cấp quyền Camera.", "1. Bấm nút 'Đo nhịp tim' trên màn hình chính.\n2. Hệ thống bật đèn Flash và mở CameraX.\n3. Người dùng đặt đầu ngón tay che kín camera.\n4. Thuật toán kiểm tra độ phủ màu đỏ.\n5. Đếm nhịp tim trong 15 giây và hiển thị sóng động.\n6. Vòng tiến trình đạt 100%, rung phản hồi nhẹ.", "Chỉ số nhịp tim BPM được tính toán thành công và tự động ghi vào bảng health_metrics.", "Nhấc ngón tay giữa chừng: Tạm dừng tiến trình và nhắc nhở 'Vui lòng giữ ngón tay che kín camera'."),
        ("UC-03", "Kích hoạt Báo động Cấp cứu SOS Panic", "Member", "Người dùng gặp sự cố té ngã, đau tim đột ngột.", "1. Mở màn hình SOS Khẩn cấp.\n2. Nhấn và GIỮ LIÊN TỤC nút tròn đỏ trong 2 giây.\n3. Vòng tròn tiến trình SVG chạy từ 0 đến 100%.\n4. Giữ đủ 2000ms: Hệ thống kích hoạt báo động.", "Còi hú phát âm lượng tối đa bypass chế độ im lặng, GPS bắt tọa độ hiện tại, lưu bản ghi CRITICAL vào alerts table và gửi FCM tới người thân.", "Nhả tay trước 2 giây: Vòng tròn reset về 0%, còi không hú (cơ chế chống bấm nhầm)."),
        ("UC-04", "Hỏi đáp y tế với AI Health Copilot", "Member / Admin", "Thiết bị có kết nối Internet, đã cấp quyền Microphone.", "1. Bấm icon Chatbot AI trên thanh điều hướng.\n2. Người dùng gõ câu hỏi hoặc bấm giữ nút Mic nói: 'Huyết áp tôi hôm nay thế nào?'.\n3. Hệ thống nạp ngữ cảnh bệnh án từ Room DB.\n4. Gửi Prompt kèm ngữ cảnh tới Gemini 1.5 Flash.\n5. Gemini phản hồi câu trả lời ân cần, chuẩn xác.", "Người bệnh nhận được lời giải thích cặn kẽ về chỉ số sức khỏe của bản thân và lời khuyên ăn uống phù hợp.", "Mất kết nối mạng: Thông báo 'Không có kết nối Internet, vui lòng kiểm tra Wi-Fi/4G'."),
        ("UC-05", "Quét đơn thuốc bằng AI (OCR)", "Member", "Có đơn thuốc giấy hoặc vỏ hộp thuốc, đã cấp quyền Camera.", "1. Vào tab 'Tủ thuốc' -> Bấm 'Quét đơn thuốc'.\n2. Chụp ảnh rõ nét đơn thuốc của bác sĩ.\n3. ML Kit OCR nhận diện văn bản On-device.\n4. Bộ lọc Regex bóc tách Tên thuốc và Liều dùng.\n5. Điền tự động vào biểu mẫu tạo thuốc.", "Lịch uống thuốc mới được khởi tạo và lưu vào bảng medicines.", "Ảnh chụp mờ nhòe: Báo lỗi 'Không thể nhận diện văn bản, vui lòng chụp lại nơi đủ sáng'."),
        ("UC-06", "Báo thức uống thuốc cưỡng bức", "System", "Đến giờ hẹn uống thuốc được cài đặt trước.", "1. AlarmManager kích hoạt BroadcastReceiver đúng giờ hẹn.\n2. Bật còi báo thức âm lượng tăng dần.\n3. Kích hoạt FullScreenIntent bật sáng màn hình khóa.\n4. Hiển thị tên thuốc, liều lượng và 2 nút lớn: 'ĐÃ UỐNG' và 'NHẮC LẠI'.", "Bệnh nhân bấm 'Đã uống' -> Cập nhật trạng thái tuân thủ vào medication_logs và dừng chuông.", "Bấm 'Nhắc lại': Hoãn báo thức và kích hoạt lại sau đúng 10 phút."),
        ("UC-07", "Tự động phân tích ngưỡng nguy hiểm 48h", "System", "Có chỉ số huyết áp/nhịp tim mới vừa được ghi nhận.", "1. Trigger AnalyzeThresholdUseCase.\n2. Quét toàn bộ lần đo trong 48h gần nhất.\n3. Phát hiện 3 lần liên tiếp vượt ngưỡng an toàn.\n4. Kiểm tra Debounce window 6h.\n5. Chèn bản ghi cảnh báo đỏ vào bảng alerts.", "Thẻ cảnh báo viền đỏ xuất hiện trên Trung tâm cảnh báo, Quả chuông trên Home nhảy badge đỏ.", "Có 1 lần đo an toàn xen giữa: Chuỗi bị ngắt, không sinh cảnh báo đỏ."),
        ("UC-08", "Giám sát & Đánh dấu đã xử lý cảnh báo", "Admin", "Admin nhận được thông báo đỏ từ người thân.", "1. Mở 'Trung Tâm Cảnh Báo' (biểu tượng Quả chuông).\n2. Lọc danh sách theo tab 'Khẩn cấp'.\n3. Bấm 'Xem Bản Đồ' để mở Google Maps điều hướng vị trí người thân gặp nạn.\n4. Bấm 'Đánh dấu đã xem'.", "Trạng thái isRead chuyển thành true, nút chuyển sang màu xám xanh, hiển thị huy hiệu '✔ Đã xử lý'.", "Bấm nút Thùng rác: Hiển thị hộp thoại xóa sạch danh sách cảnh báo."),
        ("UC-09", "Cấu hình Ngưỡng an toàn sinh tồn", "Admin", "Admin đăng nhập tài khoản có quyền Trưởng gia đình.", "1. Mở màn hình Hồ sơ thành viên cần chỉnh sửa.\n2. Thiết lập Ngưỡng huyết áp tâm thu tối đa (VD: 140 mmHg).\n3. Thiết lập Ngưỡng nhịp tim tối đa (VD: 100 BPM).\n4. Bấm 'Lưu cấu hình'.", "Ngưỡng an toàn mới được lưu trữ và áp dụng ngay lập tức cho các lần đo tiếp theo.", "Nhập giá trị phi lý (Huyết áp > 300): Báo lỗi 'Chỉ số không hợp lệ'."),
        ("UC-10", "Xuất hồ sơ Báo cáo Y bạ điện tử PDF", "Admin / Member", "Đã có dữ liệu đo lường và lịch thuốc trong tháng.", "1. Mở màn hình Lịch sử sức khỏe -> Chọn 'Xuất báo cáo PDF'.\n2. Hệ thống khởi tạo PdfDocument khổ A4.\n3. Vẽ thông tin hành chính, biểu đồ đo và bảng thống kê.\n4. Mở màn hình Preview xem trước báo cáo.\n5. Bấm 'Chia sẻ qua Zalo' hoặc 'In trực tiếp'.", "File PDF chuẩn A4 được tạo trong bộ nhớ ngoài Scoped Storage, sẵn sàng gửi cho bác sĩ điều trị.", "Không có dữ liệu đo: Thông báo 'Chưa có đủ dữ liệu để tạo báo cáo y tế'."),
        ("UC-11", "Quản lý Lịch hẹn Tái khám Bác sĩ", "Member / Admin", "Người dùng có lịch hẹn khám tại bệnh viện chuyên khoa.", "1. Mở phân hệ 'Lịch khám'.\n2. Bấm 'Thêm lịch hẹn mới'.\n3. Nhập tên bác sĩ, chuyên khoa, bệnh viện, ngày giờ khám và ghi chú dặn dò.\n4. Chọn thời gian nhắc trước (1 ngày hoặc 2 giờ).\n5. Bấm 'Lưu lịch hẹn'.", "Bản ghi được lưu vào bảng appointments và lập lịch thông báo Exact Alarm nhắc nhở chuẩn xác.", "Thời gian hẹn trong quá khứ: Báo lỗi 'Ngày hẹn không hợp lệ'."),
        ("UC-12", "Kết nối Vòng đeo tay Y tế qua BLE", "Member", "Vòng đeo tay thông minh hỗ trợ Bluetooth GATT Profile nhịp tim.", "1. Mở cài đặt 'Thiết bị ngoại vi'.\n2. Bật Bluetooth trên điện thoại.\n3. Hệ thống quét các thiết bị xung quanh có Service UUID 0x180D (Heart Rate Service).\n4. Chọn thiết bị 'SmartBand-HR' để ghép nối.\n5. Lắng nghe dữ liệu nhịp tim liên tục.", "Dữ liệu nhịp tim từ vòng đeo tay được cập nhật tự động vào ứng dụng theo thời gian thực.", "Mất kết nối Bluetooth: Tự động kích hoạt cơ chế Auto-Reconnect thử lại sau mỗi 5 giây."),
        ("UC-13", "Chuyển giao Quyền Trưởng Gia đình", "Admin", "Trưởng gia đình muốn bàn giao quyền quản trị cho con cái khác.", "1. Mở màn hình 'Quản lý Tổ ấm'.\n2. Chọn thành viên cần trao quyền trong danh sách.\n3. Bấm 'Chuyển quyền Trưởng gia đình'.\n4. Nhập mật mã xác nhận bảo mật.\n5. Bấm 'Xác nhận chuyển giao'.", "Cập nhật role của tài khoản hiện tại về MEMBER và nâng cấp tài khoản được chọn lên ADMIN.", "Nhập sai mật mã xác nhận: Báo lỗi 'Mật khẩu không chính xác'."),
        ("UC-14", "Đồng bộ Đám mây khi có Mạng lại", "System", "Thiết bị có các bản ghi đo lường lưu Offline chưa đồng bộ.", "1. BroadcastReceiver phát hiện trạng thái mạng chuyển sang CONNECTED.\n2. Kích hoạt SyncWorkManager ngầm.\n3. Truy vấn các bản ghi có syncStatus = 'PENDING'.\n4. Đóng gói JSON gửi lên máy chủ đám mây.\n5. Cập nhật syncStatus = 'SYNCED'.", "Toàn bộ dữ liệu đo lường Offline được đồng bộ toàn vẹn lên máy chủ mà người dùng không cần thao tác.", "Đường truyền gián đoạn giữa chừng: Giữ nguyên cờ PENDING và lên lịch Exponential Backoff thử lại."),
        ("UC-15", "Cấu hình Dị ứng thuốc & Bệnh nền", "Member / Admin", "Người dùng cần cập nhật bệnh án cá nhân.", "1. Mở màn hình 'Hồ sơ y tế'.\n2. Chọn 'Chỉnh sửa thông tin bệnh nền & dị ứng'.\n3. Đánh dấu các bệnh mãn tính (Tăng huyết áp, Đái tháo đường, Tim mạch).\n4. Nhập danh mục dị ứng (Penicillin, Aspirin...).\n5. Bấm 'Lưu hồ sơ'.", "Hồ sơ y bạ được cập nhật tức thời vào bảng users và làm ngữ cảnh cho Gemini AI trong các lần hỏi đáp tiếp theo.", "Để trống họ tên: Báo lỗi 'Họ và tên không được để trống'.")
    ]
    
    for uc in uc_list:
        uc_table_data = [
            ("Mã Use Case", uc[0], "Tên Use Case", uc[1]),
            ("Tác nhân chính", uc[2], "Tiền điều kiện", uc[3]),
            ("Luồng sự kiện chính (Basic Flow)", uc[4], "Hậu điều kiện / Kết quả", uc[5]),
            ("Xử lý ngoại lệ (Exception Flow)", uc[6], "Mức độ ưu tiên", "Quan trọng (Core Feature)")
        ]
        t = doc.add_table(rows=4, cols=4)
        t.alignment = WD_TABLE_ALIGNMENT.CENTER
        t.autofit = False
        set_table_borders(t, color="CBD5E1", sz="4")
        
        widths = [Inches(1.3), Inches(1.95), Inches(1.25), Inches(1.75)]
        configure_table_layout(t, widths)
        for r_i, r_data in enumerate(uc_table_data):
            row = t.rows[r_i]
            for c_i, c_val in enumerate(r_data):
                cell = row.cells[c_i]
                cell.width = widths[c_i]
                set_cell_margins(cell, top=70, bottom=70, left=90, right=90)
                p = cell.paragraphs[0]
                p.paragraph_format.space_before = Pt(0)
                p.paragraph_format.space_after = Pt(0)
                p.paragraph_format.line_spacing = 1.15
                
                if c_i in (0, 2):
                    set_cell_background(cell, "F1F5F9")
                    r = p.add_run(c_val)
                    r.bold = True
                    r.font.name = "Times New Roman"
                    r.font.size = Pt(9.5)
                    r.font.color.rgb = COLOR_PRIMARY
                else:
                    set_cell_background(cell, "FFFFFF")
                    r = p.add_run(c_val)
                    r.font.name = "Times New Roman"
                    r.font.size = Pt(9.5)
                    r.font.color.rgb = COLOR_TEXT
        doc.add_paragraph().paragraph_format.space_after = Pt(3)

    print("Part 2 built successfully.")
