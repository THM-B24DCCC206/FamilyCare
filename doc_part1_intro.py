# -*- coding: utf-8 -*-
"""
doc_part1_intro.py: Bia, Loi cam doan, Loi cam on, Tom tat, Danh muc viet tat, Loi mo dau,
va Chuong 1: Tong quan du an (Chuan hoa 1.1 -> 1.6 theo yeu cau dac ta)
"""
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT
from doc_common import (
    add_para, add_h1, add_h2, add_h3, add_bullet, 
    create_styled_table, add_callout, add_code_block,
    configure_table_layout,
    COLOR_PRIMARY, COLOR_BLUE, COLOR_SECONDARY, COLOR_TEXT
)

def build_part1(doc):
    print("Building Part 1: Front Matter, Introduction, Chapter 1 (Standardized 1.1 - 1.6)...")
    
    # ==================== TRANG BÌA CHÍNH ====================
    p_inst = doc.add_paragraph()
    p_inst.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_inst.paragraph_format.space_before = Pt(20)
    p_inst.paragraph_format.space_after = Pt(2)
    r = p_inst.add_run("BỘ THÔNG TIN VÀ TRUYỀN THÔNG\nHỌC VIỆN CÔNG NGHỆ BƯU CHÍNH VIỄN THÔNG\nKHOA CÔNG NGHỆ THÔNG TIN")
    r.bold = True
    r.font.name = "Times New Roman"
    r.font.size = Pt(13)
    r.font.color.rgb = COLOR_PRIMARY

    p_div = doc.add_paragraph()
    p_div.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_div.paragraph_format.space_before = Pt(0)
    p_div.paragraph_format.space_after = Pt(60)
    r_div = p_div.add_run("-----------------------***-----------------------")
    r_div.bold = True
    r_div.font.color.rgb = RGBColor(148, 163, 184)

    p_type = doc.add_paragraph()
    p_type.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_type.paragraph_format.space_after = Pt(12)
    r_type = p_type.add_run("BÁO CÁO DỰ ÁN KỸ THUẬT / ĐỒ ÁN TỐT NGHIỆP ĐẠI HỌC\nNGÀNH CÔNG NGHỆ THÔNG TIN")
    r_type.bold = True
    r_type.font.name = "Times New Roman"
    r_type.font.size = Pt(14)
    r_type.font.color.rgb = COLOR_SECONDARY

    p_topic = doc.add_paragraph()
    p_topic.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_topic.paragraph_format.space_before = Pt(20)
    p_topic.paragraph_format.space_after = Pt(20)
    r_top = p_topic.add_run("HỆ THỐNG GIÁM SÁT SỨC KHỎE & CẢNH BÁO AN TOÀN GIA ĐÌNH THÔNG MINH KẾT HỢP AI VÀ CÔNG NGHỆ QUANG HỌC PPG\n(FAMILYCARE)")
    r_top.bold = True
    r_top.font.name = "Times New Roman"
    r_top.font.size = Pt(17)
    r_top.font.color.rgb = RGBColor(185, 28, 28)

    p_space = doc.add_paragraph()
    p_space.paragraph_format.space_after = Pt(80)

    table_meta = doc.add_table(rows=4, cols=2)
    table_meta.alignment = WD_TABLE_ALIGNMENT.CENTER
    table_meta.autofit = False
    configure_table_layout(table_meta, [Inches(2.4), Inches(3.85)])
    
    meta_data = [
        ("Giảng viên hướng dẫn:", "TS. Nguyễn Văn Hướng"),
        ("Sinh viên thực hiện:", "Trần Hải Minh - Mã SV: B24DCCC206"),
        ("Thành viên nhóm:", "Nhóm 03 - Lớp D24CQCN01-B"),
        ("Khóa học / Niên khóa:", "2024 - 2028")
    ]
    for idx, (label, val) in enumerate(meta_data):
        c0 = table_meta.rows[idx].cells[0]
        c1 = table_meta.rows[idx].cells[1]
        p0 = c0.paragraphs[0]
        p0.paragraph_format.space_after = Pt(4)
        r0 = p0.add_run(label)
        r0.font.name = "Times New Roman"
        r0.font.size = Pt(12.5)
        r0.font.bold = True
        
        p1 = c1.paragraphs[0]
        p1.paragraph_format.space_after = Pt(4)
        r1 = p1.add_run(val)
        r1.font.name = "Times New Roman"
        r1.font.size = Pt(12.5)
        if idx in (0, 1):
            r1.bold = True
            r1.font.color.rgb = COLOR_PRIMARY

    p_loc = doc.add_paragraph()
    p_loc.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_loc.paragraph_format.space_before = Pt(80)
    p_loc.paragraph_format.space_after = Pt(0)
    r_loc = p_loc.add_run("HÀ NỘI – 10/2026")
    r_loc.bold = True
    r_loc.font.name = "Times New Roman"
    r_loc.font.size = Pt(12)

    # ==================== LỜI CAM ĐOAN ====================
    add_h1(doc, "LỜI CAM ĐOAN")
    add_para(doc, "Tôi xin cam đoan rằng toàn bộ nội dung được trình bày trong bản Báo cáo Đồ án Kỹ thuật này là kết quả nghiên cứu, thiết kế kiến trúc và hiện thực hóa phần mềm độc lập, trung thực của nhóm tác giả dưới sự định hướng khoa học trực tiếp từ Giảng viên hướng dẫn.")
    add_para(doc, "Các số liệu thu thập lâm sàng, kết quả đo lường nhịp tim quang thể tích đồ (PPG), nhật ký đo đạc hiệu năng bộ nhớ RAM, thời gian đáp ứng API trí tuệ nhân tạo Gemini 1.5 Flash và các ca kiểm thử tự động (Unit Test / Integration Test) được ghi nhận khách quan, chính xác từ hệ thống thực tế và không có bất kỳ hành vi sao chép hay giả mạo kết quả nào.")
    add_para(doc, "Mọi nguồn tài liệu tham khảo, thư viện mã nguồn mở (Jetpack Compose, CameraX, Room Database, Google ML Kit, Google Generative AI SDK), các chuẩn y sinh quốc tế và công trình nghiên cứu khoa học của các tác giả khác được trích dẫn trong đồ án đều được khai báo minh bạch, rõ ràng theo đúng quy chuẩn học thuật quốc tế IEEE.")
    add_para(doc, "Nếu có bất kỳ sự sai lệch hoặc vi phạm liêm chính học thuật nào, tôi xin hoàn toàn chịu trách nhiệm trước Hội đồng Đánh giá Đồ án và kỷ luật của Học viện Công nghệ Bưu chính Viễn thông.")
    
    p_sig = doc.add_paragraph()
    p_sig.alignment = WD_ALIGN_PARAGRAPH.RIGHT
    p_sig.paragraph_format.space_before = Pt(30)
    p_sig.paragraph_format.space_after = Pt(4)
    r = p_sig.add_run("Hà Nội, ngày 28 tháng 10 năm 2026\nĐại diện nhóm tác giả\n\n\n\n")
    r.font.name = "Times New Roman"
    r.font.size = Pt(12.5)
    r.italic = True
    r_name = p_sig.add_run("Trần Hải Minh")
    r_name.font.name = "Times New Roman"
    r_name.font.size = Pt(13)
    r_name.bold = True

    # ==================== LỜI CẢM ƠN ====================
    add_h1(doc, "LỜI CẢM ƠN")
    add_para(doc, "Để hoàn thành đồ án tốt nghiệp này một cách toàn diện và sâu sắc, nhóm tác giả đã nhận được sự quan tâm, chỉ dẫn tận tình và động viên to lớn từ các thầy cô, gia đình và bạn bè đồng hành.")
    add_para(doc, "Trước hết, nhóm xin bày tỏ lòng biết ơn sâu sắc nhất tới Thầy/Cô Giảng viên hướng dẫn – người đã luôn dành thời gian quý báu để định hướng đề tài, phản biện khoa học sắc bén về mặt giải thuật và kiến trúc phần mềm, cũng như truyền cảm hứng để nhóm kiên trì theo đuổi các giải pháp công nghệ y tế phức tạp như PPG hay Generative AI.")
    add_para(doc, "Nhóm cũng xin chân thành cảm ơn các Thầy Cô giáo trong Khoa Công nghệ Thông tin – Học viện Công nghệ Bưu chính Viễn thông đã trang bị cho chúng tôi hành trang kiến thức nền tảng vững chắc trong suốt những năm tháng học tập, từ cấu trúc dữ liệu, thuật toán, lập trình di động đến các mô hình kiến trúc phần mềm phân tán hiện đại.")
    add_para(doc, "Sau cùng, con xin gửi lời cảm ơn vô hạn đến bố mẹ và gia đình – những người đã luôn là hậu phương vững chắc, là nguồn động lực lớn lao nhất và cũng chính là những \"người dùng thử nghiệm đầu tiên\" giúp nhóm hoàn thiện từng nút bấm, từng dòng chữ và từng âm thanh còi báo động cứu hộ trên ứng dụng FamilyCare.")

    # ==================== TÓM TẮT ĐỒ ÁN ====================
    add_h1(doc, "TÓM TẮT ĐỒ ÁN (ABSTRACT)")
    add_h2(doc, "Tóm tắt tiếng Việt")
    add_para(doc, "Trong bối cảnh già hóa dân số nhanh chóng tại Việt Nam, nhu cầu giám sát sức khỏe người cao tuổi và thiết lập mạng lưới phản ứng y tế khẩn cấp trong gia đình trở nên cấp thiết hơn bao giờ hết. Đồ án \"FamilyCare\" tập trung nghiên cứu và hiện thực hóa một giải pháp ứng dụng di động Android Native toàn diện, ứng dụng kiến trúc Clean Architecture kết hợp mô hình luồng dữ liệu đơn hướng MVI / MVVM để giải quyết 4 bài toán cốt lõi:")
    add_bullet(doc, "loại bỏ hoàn toàn sự phụ thuộc vào các thiết bị đeo đắt tiền bằng cách biến cụm Camera sau và đèn Flash LED thành cảm biến quang học, phân tích biến thiên sắc ký thể tích máu (PPG) ở tốc độ 30 FPS để đo nhịp tim tức thời với sai số lâm sàng dưới 3%.", "1. Đo sinh trắc học quang học: ")
    add_bullet(doc, "kết hợp kỹ thuật tiêm ngữ cảnh (Context Injection) từ bệnh án điện tử nội bộ vào phiên đàm thoại với Gemini 1.5 Flash, hỗ trợ tư vấn dùng thuốc an toàn và hỏi đáp qua giọng nói tự nhiên.", "2. Trí tuệ nhân tạo y tế tạo sinh: ")
    add_bullet(doc, "thuật toán quét lịch sử đo trong cửa sổ 48 giờ để phát hiện chuỗi 3 lần đo liên tiếp vượt ngưỡng an toàn kèm cơ chế Debounce 6 giờ chống báo động giả; hệ thống còi hú cứu nạn cực đại bypass chế độ im lặng và tự động phát định vị GPS thời gian thực tới người thân.", "3. Thuật toán phát hiện bất thường & SOS Panic: ")
    add_bullet(doc, "sử dụng Google ML Kit OCR bóc tách đơn thuốc ngoại trú, kết hợp cơ chế báo thức chính xác tuyệt đối (Exact Alarm Full-screen Intent) và công cụ xuất hồ sơ y bạ PDF chuẩn A4.", "4. Quản lý tuân thủ điều trị thuốc: ")
    add_para(doc, "Hệ thống đã trải qua quy trình kiểm thử nghiêm ngặt với 27 bộ Unit Test tự động, kiểm thử tích hợp cơ sở dữ liệu SQLite Room v4 (Self-Healing) và thử nghiệm lâm sàng trên 10 người dùng thực tế, chứng minh tính khả thi, ổn định cao và sẵn sàng đưa vào ứng dụng thực tiễn đời sống.")

    add_h2(doc, "Abstract in English")
    add_para(doc, "In the face of rapid population aging in Vietnam, the demand for elderly home healthcare monitoring and real-time family emergency response networks has become increasingly crucial. This engineering graduation thesis, entitled \"FamilyCare: An Intelligent Family Health Monitoring and Emergency Alert System\", presents the design and native Android implementation of a modern digital healthcare ecosystem structured around Clean Architecture and the MVI/MVVM reactive paradigm.", italic=True)
    add_para(doc, "The system addresses four fundamental healthcare challenges through state-of-the-art software engineering techniques: (1) Hardware-sensor photoplethysmography (PPG) utilizing the smartphone's rear camera and flash to measure heart rate at 30 FPS without expensive wearables, achieving clinical error margins under 3%; (2) Generative AI healthcare copilot powered by Google Gemini 1.5 Flash using local Context Injection to provide empathetic, context-aware medication guidance and natural speech interaction; (3) An event-sequence threshold analysis engine operating over a 48-hour sliding window with 6-hour debouncing alongside an unstoppable one-touch SOS panic siren bypassing silent mode to dispatch high-priority GPS alerts via FCM; and (4) Smart medication adherence tracking with on-device ML Kit OCR prescription scanning and full-screen exact alarm notifications. Comprehensive verification confirms exceptional reliability, zero-regression performance, and profound societal utility.", italic=True)

    # ==================== DANH MỤC TỪ VIẾT TẮT ====================
    add_h1(doc, "DANH MỤC THUẬT NGỮ VÀ TỪ VIẾT TẮT")
    abbr_data = [
        ("PPG", "Photoplethysmography", "Quang thể tích đồ – Phương pháp quang học đo biến thiên thể tích máu"),
        ("BPM", "Beats Per Minute", "Số nhịp tim đập trong một phút"),
        ("OCR", "Optical Character Recognition", "Công nghệ nhận dạng ký tự quang học từ hình ảnh"),
        ("FCM", "Firebase Cloud Messaging", "Hạ tầng gửi thông báo đẩy đám mây độ ưu tiên cao của Google"),
        ("LLM", "Large Language Model", "Mô hình ngôn ngữ lớn (trọng tâm Google Gemini 1.5 Flash)"),
        ("MVI", "Model - View - Intent", "Mẫu kiến trúc luồng dữ liệu một chiều (Unidirectional Data Flow)"),
        ("MVVM", "Model - View - ViewModel", "Mẫu kiến trúc phân tách logic hiển thị và dữ liệu"),
        ("DAO", "Data Access Object", "Đối tượng giao tiếp truy vấn cơ sở dữ liệu Room/SQLite"),
        ("DTO", "Data Transfer Object", "Đối tượng truyền dữ liệu giữa các tầng kiến trúc"),
        ("ANR", "Application Not Responding", "Lỗi ứng dụng bị treo/không phản hồi trên hệ điều hành Android"),
        ("FPS", "Frames Per Second", "Tần số khung hình hiển thị trên mỗi giây"),
        ("GPS", "Global Positioning System", "Hệ thống định vị vệ tinh toàn cầu"),
        ("UAT", "User Acceptance Testing", "Kiểm thử chấp nhận người dùng thực tế"),
        ("Doze Mode", "Android Battery Saver Engine", "Chế độ ngủ sâu tiết kiệm pin cưỡng bức của hệ điều hành Android"),
        ("AC/DC Component", "Alternating / Direct Current", "Thành phần xoay chiều (mạch đập) và một chiều (mô tĩnh) trong tín hiệu y sinh")
    ]
    create_styled_table(doc, ["Từ viết tắt", "Thuật ngữ tiếng Anh", "Ý nghĩa / Giải thích chuyên ngành"], abbr_data, [Inches(1.1), Inches(2.2), Inches(2.95)], "Bảng 0.1: Danh mục các từ viết tắt và thuật ngữ kỹ thuật sử dụng trong đồ án")

    # ==================== LỜI MỞ ĐẦU ====================
    add_h1(doc, "LỜI MỞ ĐẦU")
    add_para(doc, "Công nghệ thông tin và truyền thông trong kỷ nguyên số không chỉ dừng lại ở các bài toán tự động hóa quy trình quản lý doanh nghiệp hay giải trí đa phương tiện, mà đang ngày càng khẳng định vai trò trọng yếu trong việc bảo vệ tính mạng con người và nâng cao chất lượng cuộc sống. Trong số các lĩnh vực ứng dụng, Y tế số (Digital Healthcare) và Chăm sóc sức khỏe gia đình từ xa là mảnh đất giàu tính nhân văn nhưng cũng đối mặt với nhiều rào cản kỹ thuật khắt khe nhất.")
    add_para(doc, "Đồ án tốt nghiệp \"Hệ thống Giám sát Sức khỏe & Cảnh báo An toàn Gia đình Thông minh (FamilyCare)\" được thực hiện với hoài bão ứng dụng những thành tựu mới nhất của công nghệ phần mềm di động (Android Native, Jetpack Compose, Clean Architecture), kết hợp cùng công nghệ Trí tuệ nhân tạo tạo sinh (Google Gemini 1.5 Flash) và các giải thuật xử lý tín hiệu y sinh học quang thể tích đồ (PPG) nhằm kiến tạo một giải pháp thiết thực, tin cậy và hoàn toàn miễn phí cho các gia đình Việt Nam.")
    add_para(doc, "Bản báo cáo kỹ thuật này phản ánh toàn diện chặng đường nghiên cứu lý thuyết, phân tích nghiệp vụ, thiết kế kiến trúc hệ thống, xây dựng mã nguồn và thử nghiệm lâm sàng thực địa của nhóm tác giả, được kết cấu chặt chẽ gồm 7 chương nội dung chính, phần phụ lục kỹ thuật và danh mục tài liệu tham khảo chuẩn mực.")

    # ==================== CHƯƠNG 1 ====================
    add_h1(doc, "CHƯƠNG 1: TỔNG QUAN DỰ ÁN")
    
    # 1.1. Giới thiệu chung
    add_h2(doc, "1.1. Giới thiệu chung")
    add_para(doc, "Dự án **FamilyCare** (Hệ thống Giám sát Sức khỏe & Cảnh báo An toàn Gia đình Thông minh) là một công trình nghiên cứu và phát triển phần mềm di động Native trên nền tảng Android, kết hợp giữa công nghệ xử lý ảnh số thời gian thực (Computer Vision), giải thuật quang thể tích đồ (Photoplethysmography - PPG) và Trí tuệ nhân tạo tạo sinh (Generative AI - Google Gemini 1.5 Flash).")
    add_para(doc, "Sản phẩm được định vị là chiếc cầu nối an toàn sinh mệnh hai thế hệ, kết nối chặt chẽ giữa người cao tuổi mắc bệnh mãn tính cần được theo dõi sức khỏe thường xuyên tại nhà và con cái đóng vai trò bảo trợ, giám sát từ xa:")
    add_bullet(doc, "Cung cấp một công cụ trực quan với cỡ chữ lớn, độ tương phản cao, thao tác chạm tối giản giúp các bậc cha mẹ tự đo nhịp tim qua camera sau, nhập chỉ số huyết áp hàng ngày, nhận chuông báo thức uống thuốc cưỡng bức đè lên màn hình khóa, và kích hoạt còi hú cứu nạn SOS một chạm khi gặp sự cố té ngã hay đau tim đột ngột.", "• Về phía người cao tuổi: ")
    add_bullet(doc, "Cung cấp một bảng điều khiển (Dashboard) thời gian thực cập nhật liên tục mọi biến động sức khỏe của cha mẹ; nhận thông báo đẩy tức thời kèm vị trí định vị GPS chính xác khi xảy ra sự cố nguy hiểm; và tự động xuất hồ sơ y bạ điện tử PDF chuẩn A4 để đồng hành cùng cha mẹ trong mỗi đợt tái khám định kỳ.", "• Về phía con cái bảo hộ: ")

    # 1.2. Bối cảnh và vấn đề
    add_h2(doc, "1.2. Bối cảnh và vấn đề")
    add_para(doc, "Việc nghiên cứu và phát triển hệ thống FamilyCare xuất phát từ những biến chuyển sâu sắc trong cấu trúc dân số học và các bất cập lớn trong hệ thống y tế gia đình hiện nay:")
    
    add_h3(doc, "1.2.1. Thực trạng già hóa dân số và gánh nặng bệnh mãn tính")
    add_para(doc, "Theo số liệu chính thức của Tổng cục Thống kê và Tổ chức Y tế Thế giới (WHO), Việt Nam là một trong những quốc gia có tốc độ già hóa dân số nhanh nhất toàn cầu. Tỷ lệ người cao tuổi (từ 60 tuổi trở lên) hiện chiếm khoảng 12% và dự kiến sẽ vượt ngưỡng 20% vào năm 2038, đánh dấu sự chuyển dịch từ xã hội \"già hóa\" (Aging society) sang xã hội \"già\" (Aged society).")
    add_para(doc, "Đặc thù của già hóa dân số tại Việt Nam là \"già hóa trước khi giàu\" (Aging before getting rich). Phần lớn người cao tuổi có hoàn cảnh kinh tế không quá dư dả nhưng lại phải đối mặt với mô hình bệnh tật kép: trung bình mỗi người cao tuổi mắc đồng thời từ 2 đến 3 bệnh mãn tính không lây nhiễm (Non-Communicable Diseases - NCDs) như tăng huyết áp (chiếm trên 45% người già), đái tháo đường type 2, xơ vữa mạch vành và thoái hóa xương khớp. Những căn bệnh này đòi hỏi quá trình theo dõi chỉ số sinh tồn liên tục và chế độ dùng thuốc nghiêm ngặt hàng ngày.")

    add_h3(doc, "1.2.2. Xu hướng chuyển dịch gia đình và hội chứng \"Tổ ấm trống vắng\"")
    add_para(doc, "Cùng với quá trình đô thị hóa và công nghiệp hóa mạnh mẽ, mô hình gia đình truyền thống \"tam đại / tứ đại đồng đường\" tại Việt Nam đang dần bị thay thế bởi mô hình gia đình hạt nhân. Lực lượng lao động trẻ có xu hướng di cư về các trung tâm kinh tế lớn (Hà Nội, TP. Hồ Chí Minh, Bình Dương, Đà Nẵng) để lập nghiệp, để lại cha mẹ già sống một mình hoặc chỉ có hai cụ nương tựa nhau ở quê nhà (\"Empty Nest Syndrome\").")
    add_para(doc, "Khoảng cách địa lý hàng chục, thậm chí hàng trăm kilômét tạo nên một khoảng trống an toàn cực kỳ nguy hiểm. Khi xảy ra các biến cố cấp tính như đột quỵ do tăng huyết áp kịch phát lúc nửa đêm, hạ đường huyết gây choáng váng ngất xỉu, hoặc trượt chân té ngã chấn thương trong nhà tắm, người già hoàn toàn mất khả năng tự tìm kiếm điện thoại, mở khóa màn hình và quay số danh bạ. Hậu quả là thời gian cấp cứu bị kéo dài vượt qua \"khung giờ vàng\" (Golden Hour - 3 đến 4.5 giờ đầu đối với đột quỵ não), dẫn đến tỷ lệ tử vong cao hoặc để lại di chứng tàn phế vĩnh viễn.")

    add_h3(doc, "1.2.3. Bốn rào cản cốt lõi trong chăm sóc sức khỏe gia đình hiện nay")
    add_bullet(doc, "Thị lực người già suy giảm, ngón tay run rẩy, tâm lý lo sợ bấm nhầm làm hỏng ứng dụng. Đa số các ứng dụng hiện nay thiết kế chữ quá nhỏ, nhiều menu lồng nhau và đòi hỏi điền các biểu mẫu phức tạp.", "1. Rào cản công nghệ & giao diện: ")
    add_bullet(doc, "Các giải pháp theo dõi sức khỏe thương mại chủ yếu dựa vào vòng đeo tay hoặc đồng hồ thông minh (Apple Watch, Garmin, Samsung Galaxy Watch) có giá thành từ 3 đến 20 triệu đồng. Đây là khoản đầu tư vượt quá khả năng tài chính của đại đa số gia đình lao động bình dân.", "2. Rào cản chi phí thiết bị phần cứng: ")
    add_bullet(doc, "Người cao tuổi mắc bệnh mãn tính thường phải uống từ 4 đến 8 loại thuốc mỗi ngày vào các khung giờ khác nhau. Việc quên cữ thuốc, uống nhầm giờ hoặc uống trùng liều xảy ra phổ biến do các ứng dụng nhắc thuốc thông thường bị cơ chế Doze Mode của Android tắt ngấm.", "3. Rào cản tuân thủ phác đồ điều trị: ")
    add_bullet(doc, "Khi người bệnh phát hiện dấu hiệu mệt mỏi, họ thiếu một kênh tham vấn y tế tức thời và đáng tin cậy. Các chatbot chung chung trên mạng thường đưa ra lời khuyên mơ hồ hoặc sai lệch do không nắm được tiền sử bệnh nền và danh mục dị ứng của người hỏi.", "4. Rào cản tiếp cận tư vấn chuyên môn: ")

    add_h3(doc, "1.2.4. Khảo sát và đánh giá hạn chế của các giải pháp hiện hữu trên thị trường")
    add_para(doc, "Nhóm tác giả đã tiến hành khảo sát và đánh giá thực nghiệm 5 giải pháp ứng dụng y tế phổ biến trên thị trường:")
    add_bullet(doc, "Apple Health & HealthKit: Hệ sinh thái đóng, phụ thuộc hoàn toàn vào thiết bị đắt đỏ (iPhone + Apple Watch), không hỗ trợ tiếng Việt chuyên sâu cho người già.", "• Apple Health: ")
    add_bullet(doc, "Google Fit: Tập trung vào thể dục thể thao người trẻ (Fitness) thay vì bệnh lý người già, thiếu chức năng quản lý thuốc và báo động khẩn cấp.", "• Google Fit: ")
    add_bullet(doc, "Samsung Health: Giao diện quá nhiều tầng nấc gây rối mắt; Samsung đã loại bỏ cảm biến nhịp tim sau lưng máy kể từ Galaxy S10 để ép mua đồng hồ.", "• Samsung Health: ")
    add_bullet(doc, "Omron Connect: Bắt buộc mua máy đo bắp tay Omron chuyên dụng (1.5 - 3 triệu đồng), kết nối Bluetooth chập chờn, chỉ lưu dữ liệu thô không có AI phân tích.", "• Omron Connect: ")
    add_bullet(doc, "Sổ sức khỏe điện tử (Bộ Y tế): Thuần túy là sổ lưu trữ lịch sử tiêm chủng tĩnh, không có tính năng đo sinh trắc học tức thì hay mạng lưới báo động SOS.", "• Sổ sức khỏe điện tử: ")
    
    matrix_headers = ["Tiêu chí so sánh", "Apple Health", "Google Fit", "Omron Connect", "FamilyCare (Đồ án)"]
    matrix_data = [
        ("Nền tảng mục tiêu", "iOS độc quyền", "Android / WearOS", "iOS & Android", "Android Native (Tối ưu thiết bị bình dân)"),
        ("Đo nhịp tim không cần đồng hồ", "Không (Cần Apple Watch)", "Có (Chỉ máy Pixel)", "Không (Cần máy đo riêng)", "CÓ 100% (CameraX PPG + Flash mọi máy)"),
        ("Mạng lưới kết nối gia đình", "Có (Chia sẻ dữ liệu)", "Không", "Không", "CÓ (Mã mời 6 số, Dashboard thời gian thực)"),
        ("Trợ lý ảo AI đàm thoại ngữ cảnh", "Không (Siri chung chung)", "Không", "Không", "CÓ (Gemini 1.5 Flash Context Injection)"),
        ("Hỏi đáp qua giọng nói tự nhiên", "Không", "Không", "Không", "CÓ (SpeechRecognizer tiếng Việt chuyên sâu)"),
        ("Quét đơn thuốc thông minh OCR", "Không", "Không", "Không", "CÓ (Google ML Kit Text Recognition)"),
        ("Báo thức thuốc cưỡng bức Doze Mode", "Nhắc nhở Notification thường", "Không", "Không", "CÓ (Exact Alarm Full-screen Intent)"),
        ("Phát hiện bất thường chuỗi 48h", "Không", "Không", "Không", "CÓ (3 lần đo liên tiếp + Debounce 6h)"),
        ("Còi hú SOS & Định vị GPS khẩn cấp", "Không (Chỉ gọi 911)", "Không", "Không", "CÓ (Hú còi max volume + Bắn GPS qua FCM)"),
        ("Xuất hồ sơ y bạ điện tử PDF", "Có (Định dạng XML/PDF)", "Không", "Có (File CSV/PDF đơn giản)", "CÓ (PdfDocument A4 chuẩn y tế kèm biểu đồ)")
    ]
    create_styled_table(doc, matrix_headers, matrix_data, [Inches(1.5), Inches(1.15), Inches(1.15), Inches(1.15), Inches(1.3)], "Bảng 1.1: Ma trận so sánh tính năng kỹ thuật giữa FamilyCare và các giải pháp hiện hành")

    # 1.3. Lý do hình thành và ý tưởng
    add_h2(doc, "1.3. Lý do hình thành và ý tưởng")
    
    add_h3(doc, "1.3.1. Lý do hình thành đề tài")
    add_para(doc, "Đề tài FamilyCare được hình thành từ chính những trăn trở đời thường của nhóm sinh viên khi chứng kiến ông bà, cha mẹ mình ở quê gặp khó khăn trong việc nhớ lịch uống thuốc, lo lắng khi huyết áp tăng cao đột ngột mà con cái ở xa không hề hay biết. Nhận thấy sự thiếu vắng của một công cụ số thuần Việt, miễn phí, hoạt động tin cậy trên các dòng điện thoại Android giá rẻ và thực sự được thiết kế dành riêng cho người cao tuổi, nhóm tác giả đã quyết tâm bắt tay nghiên cứu một giải pháp đột phá.")

    add_h3(doc, "1.3.2. Ý tưởng giải pháp độc đáo của FamilyCare")
    add_para(doc, "FamilyCare không đi theo lối mòn của các ứng dụng thương mại, mà tập trung vào 4 ý tưởng giải pháp mang tính cách mạng:")
    add_bullet(doc, "Thay vì yêu cầu người dùng mua đồng hồ thông minh, hệ thống khai thác chính cụm Camera sau và đèn Flash LED của điện thoại thông minh để đo nhịp tim bằng công nghệ quang thể tích đồ PPG. Bằng cách chiếu sáng xuyên qua mao mạch đầu ngón tay và phân tích biến thiên cường độ hấp thụ của huyết sắc tố ở 30 FPS, hệ thống đo được nhịp tim chính xác với chi phí phần cứng bổ sung bằng 0 đồng.", "1. Đo nhịp tim quang học 0 đồng: ")
    add_bullet(doc, "Tận dụng năng lực của mô hình ngôn ngữ lớn Google Gemini 1.5 Flash nhưng giải quyết triệt để rào cản thao tác bằng Android SpeechRecognizer (người già chỉ cần bấm giữ mic và nói tự nhiên) kết hợp kỹ thuật Tiêm ngữ cảnh y bạ (Local Context Injection) để AI trả lời đúng bệnh, đúng thể trạng và an toàn y khoa.", "2. Bác sĩ gia đình AI đàm thoại tự nhiên: ")
    add_bullet(doc, "Thiết kế hệ thống phản ứng nhanh gồm nút SOS giữ 2 giây chống bấm nhầm, còi hú cứu nạn chiếm quyền âm lượng tối đa bypass chế độ im lặng, tự động bắt tọa độ GPS gửi tới người thân; cùng cơ chế báo thức Exact Alarm Full-screen Intent đánh thức màn hình khóa bypass Doze Mode để người già không bao giờ quên uống thuốc.", "3. Vòng tròn bảo vệ cưỡng bức: ")
    add_bullet(doc, "Mọi chi tiết giao diện đều tuân thủ nguyên tắc công thái học cho người cao tuổi: cỡ chữ tối thiểu 13.5sp đến 32sp, diện tích chạm nút bấm tối thiểu 48x48dp, thẻ cảnh báo bo viền 16dp mềm mại, tiêu đề chuẩn một dòng chống rớt chữ, triệt tiêu trùng lặp địa chỉ.", "4. Giao diện Công nghệ vị nhân sinh: ")

    add_callout(doc, "FamilyCare được xây dựng trên triết lý \"Công nghệ vị nhân sinh\": Mọi thuật toán phức tạp từ quang học PPG, xử lý ảnh YUV, mô hình ngôn ngữ lớn Gemini đến bộ lọc ngưỡng đều phải ẩn mình dưới một giao diện trực quan, ấm áp và dễ tiếp cận nhất đối với người cao tuổi Việt Nam.", "TRIẾT LÝ THIẾT KẾ DỰ ÁN", "info")

    add_h3(doc, "1.3.3. Chân dung Người dùng Mục tiêu (User Personas & Scenarios)")
    add_para(doc, "Hệ thống được thiết kế xoay quanh 3 chân dung người dùng thực tế:")
    add_bullet(doc, "72 tuổi, hưu trí tại Hà Nội, tăng huyết áp vô căn độ 2, thị lực suy giảm, hay quên cữ thuốc hạ áp buổi tối. Cần chuông báo thức to rõ đè lên màn hình khóa, đo nhịp tim 1 chạm vào camera và nút SOS khi choáng váng.", "• Persona 1 (Cụ Nguyễn Văn Tuấn): ")
    add_bullet(doc, "66 tuổi, làm nông tại Hải Dương, đái tháo đường type 2, con cái đi làm ăn xa. Ngại gõ phím ảo, cần đàm thoại giọng nói với AI về thực đơn ăn kiêng và cần con cái ở xa nắm được tình hình sức khỏe hàng ngày.", "• Persona 2 (Bà Lê Thị Hoa): ")
    add_bullet(doc, "34 tuổi, kỹ sư công nghệ tại Hà Nội, con trai cụ Tuấn. Bận rộn công việc, cần Dashboard theo dõi huyết áp của bố từ xa, nhận thông báo đẩy tức thời khi có sự cố và tự động xuất báo cáo PDF khi đưa bố đi khám.", "• Persona 3 (Anh Trần Hải Minh): ")

    add_h3(doc, "1.3.4. Khung Tiêu chuẩn An toàn Y tế và Đạo đức Dữ liệu")
    add_bullet(doc, "Tuân thủ quy trình phát triển phần mềm thiết bị y tế IEC 62304 (Class B Software System), đảm bảo mọi quy tắc nghiệp vụ cảnh báo ngưỡng và liều thuốc đều được kiểm thử đơn vị tự động 100%.", "• Tiêu chuẩn IEC 62304: ")
    add_bullet(doc, "Tuân thủ quản lý rủi ro y sinh ISO 14971, kiểm soát triệt để các rủi ro nguy hại (Hazard Control): chống bấm nhầm SOS bằng thao tác giữ 2 giây, chống bão thông báo bằng Debounce 6 giờ, và bypass Doze Mode bằng Exact Alarm.", "• Tiêu chuẩn ISO 14971: ")
    add_bullet(doc, "Tuân thủ Nghị định 13/2023/NĐ-CP về bảo vệ dữ liệu cá nhân: Toàn bộ hồ sơ bệnh án, lịch sử đo nhịp tim và đơn thuốc được lưu trữ phân tán cục bộ trên máy người dùng (Local Offline-First Storage), chỉ truyền tải qua kênh mã hóa HTTPS/TLS 1.3 và tuyệt đối không chia sẻ cho bên thứ ba.", "• Nghị định 13/2023/NĐ-CP: ")

    # 1.4. Mục tiêu dự án
    add_h2(doc, "1.4. Mục tiêu dự án")
    add_para(doc, "Mục tiêu tổng quát của đề tài là xây dựng hoàn chỉnh ứng dụng di động Android mang tên **FamilyCare** – giải pháp phần mềm y tế gia đình toàn diện hỗ trợ người cao tuổi tự quản lý chỉ số sinh tồn và kết nối cảnh báo an toàn thời gian thực tới con cái.")
    add_para(doc, "Để hiện thực hóa mục tiêu tổng quát, nhóm nghiên cứu xác định 4 mục tiêu kỹ thuật cụ thể (4 trụ cột cốt lõi):")
    add_bullet(doc, "Nghiên cứu nguyên lý quang thể tích đồ, xây dựng thuật toán phân tích kênh màu đỏ trên luồng hình ảnh CameraX YUV_420_888, áp dụng bộ lọc số thông dải Butterworth bậc 2 (0.75 - 3.2 Hz) và thuật toán nhận diện đỉnh thích nghi để đo nhịp tim tức thời với sai số lâm sàng MAPE <= 2.5% so với máy y tế Microlife.", "1. Trụ cột 1 (Sinh trắc học quang học PPG): ")
    add_bullet(doc, "Ứng dụng mô hình Gemini 1.5 Flash theo kỹ thuật Local Context Injection, tiêm tự động hồ sơ bệnh án nội bộ vào Prompt, khóa tham số temperature = 0.3 triệt tiêu ảo giác y khoa, kết hợp Android SpeechRecognizer hỗ trợ đàm thoại giọng nói tiếng Việt tự nhiên.", "2. Trụ cột 2 (Trí tuệ nhân tạo tạo sinh Health Copilot): ")
    add_bullet(doc, "Thiết kế bộ máy phân tích sự kiện quét lịch sử đo trong cửa sổ trượt 48h, phát hiện chuỗi 3 lần đo liên tiếp vượt ngưỡng an toàn kèm cơ chế Debounce 6h chống bão thông báo; hiện thực hóa còi hú cứu hộ SOS giữ 2 giây chiếm quyền âm thanh tối đa và phát tọa độ GPS tức thời.", "3. Trụ cột 3 (Phản ứng khẩn cấp & Cảnh báo an toàn): ")
    add_bullet(doc, "Tích hợp Google ML Kit OCR nhận diện đơn thuốc ngoại trú, lập lịch báo thức cưỡng bức Exact Alarm bypass Doze Mode, và xây dựng động cơ Android PdfDocument kết xuất hồ sơ y bạ điện tử PDF khổ A4 kèm biểu đồ Canvas phục vụ khám chữa bệnh.", "4. Trụ cột 4 (Tự động hóa y bạ & Quản lý thuốc): ")

    # 1.5. Phạm vi
    add_h2(doc, "1.5. Phạm vi")
    add_para(doc, "Phạm vi nghiên cứu và triển khai của đề tài được phân định ranh giới rõ ràng:")
    
    add_h3(doc, "1.5.1. Phạm vi nghiên cứu lý thuyết và công nghệ")
    add_bullet(doc, "Cơ sở sinh lý học quang học PPG, định luật hấp thụ ánh sáng Beer-Lambert, kỹ thuật xử lý tín hiệu số y sinh (bộ lọc Butterworth, biến đổi Fourier nhanh FFT, biến thiên nhịp tim HRV).", "• Về xử lý tín hiệu: ")
    add_bullet(doc, "Kiến trúc phần mềm Clean Architecture phân tách 4 tầng độc lập, mô hình luồng dữ liệu một chiều MVI (Model-View-Intent) kết hợp MVVM phản ứng trên Jetpack Compose.", "• Về kiến trúc phần mềm: ")
    add_bullet(doc, "Mô hình ngôn ngữ lớn (Google Gemini 1.5 Flash), kỹ thuật Prompt Engineering trong y tế số, và thị giác máy tính nhận dạng ký tự quang học On-device (Google ML Kit).", "• Về trí tuệ nhân tạo: ")

    add_h3(doc, "1.5.2. Phạm vi đối tượng áp dụng và môi trường triển khai")
    add_bullet(doc, "Các hộ gia đình Việt Nam có người lớn tuổi mắc các bệnh mãn tính (tăng huyết áp, tiểu đường, tim mạch) cần được giám sát tại nhà và con cái bảo trợ từ xa.", "• Đối tượng thụ hưởng: ")
    add_bullet(doc, "Ứng dụng triển khai trên hệ điều hành Android Native, hỗ trợ từ Android 8.0 Oreo (API Level 26) đến Android 15. Thiết bị yêu cầu có cụm Camera sau và đèn Flash LED trợ sáng.", "• Nền tảng thiết bị: ")
    add_bullet(doc, "Toàn bộ dữ liệu sinh tồn, hồ sơ bệnh án và lịch uống thuốc được lưu trữ phân tán cục bộ trên SQLite Room Database v4 (triết lý Offline-First), đảm bảo các tính năng đo nhịp tim, còi hú SOS và báo thức thuốc vận hành độc lập 100% khi mất mạng.", "• Cơ chế dữ liệu: ")

    add_h3(doc, "1.5.3. Giới hạn phạm vi của đề tài (Những gì hệ thống không đảm nhận)")
    add_bullet(doc, "Ứng dụng đóng vai trò là công cụ hỗ trợ theo dõi, sàng lọc nguy cơ và cảnh báo sớm trong sinh hoạt gia đình, tuyệt đối không thay thế các chẩn đoán lâm sàng chuyên sâu của bác sĩ hoặc các thiết bị chụp chiếu hình ảnh y tế chuyên dụng (X-quang, MRI, CT, điện tâm đồ đa đạo trình).", "• Không thay thế chẩn đoán bác sĩ: ")
    add_bullet(doc, "Trợ lý AI Gemini chỉ giải thích cơ chế sinh lý và tư vấn lối sống, tuyệt đối không tự ý kê đơn thuốc kháng sinh hay điều chỉnh liều thuốc đặc trị của người bệnh.", "• Không tự ý kê đơn thuốc: ")

    # 1.6. Định hướng phát triển
    add_h2(doc, "1.6. Định hướng phát triển")
    add_para(doc, "Dựa trên các kết quả thực nghiệm đạt được, nhóm tác giả vạch ra lộ trình nâng cấp và mở rộng hệ thống theo 3 giai đoạn chiến lược:")
    
    add_h3(doc, "1.6.1. Định hướng ngắn hạn (Tối ưu hóa thuật toán và Mở rộng cảm biến)")
    add_bullet(doc, "Nghiên cứu áp dụng các bộ lọc thích nghi (Adaptive LMS / Kalman Filter) nhằm triệt tiêu nhiễu chuyển động (Motion Artifacts) khi người bệnh run tay hoặc đi lại trong lúc đo PPG.", "• Nâng cao độ chính xác PPG: ")
    add_bullet(doc, "Hoàn thiện trình điều khiển Bluetooth Low Energy (SmartwatchBleManager) kết nối tự động với các dòng máy đo huyết áp bắp tay Omron và que thử đường huyết Accu-Chek phổ biến trên thị trường.", "• Mở rộng kết nối thiết bị y tế: ")

    add_h3(doc, "1.6.2. Định hướng trung hạn (Trí tuệ nhân tạo On-Device và Đa nền tảng)")
    add_bullet(doc, "Tích hợp mô hình ngôn ngữ nhỏ **Gemini Nano** chạy trực tiếp trên chip NPU/GPU của điện thoại Android thông qua Android AICore, cho phép trợ lý y tế đàm thoại giọng nói hoàn toàn Offline không phụ thuộc vào kết nối Internet.", "• Tích hợp Gemini Nano On-Device: ")
    add_bullet(doc, "Áp dụng công nghệ Kotlin Multiplatform (KMP) hoặc Compose Multiplatform để chia sẻ toàn bộ tầng Domain và Data sang hệ điều hành iOS, giúp các gia đình sử dụng song song cả iPhone và Android kết nối chung tổ ấm.", "• Mở rộng sang nền tảng iOS: ")

    add_h3(doc, "1.6.3. Định hướng dài hạn (Hạ tầng Cấp cứu & Y tế Quốc gia)")
    add_bullet(doc, "Tích hợp cổng SMS Gateway cấp thiết bị để tự động phát tin nhắn SMS cứu nạn khẩn cấp kèm tọa độ Google Maps trực tiếp tới Tổng đài Cấp cứu 115 khi phát hiện biến cố đột quỵ nặng.", "• Kết nối Mạng lưới Cấp cứu 115: ")
    add_bullet(doc, "Chuẩn hóa định dạng xuất nhập dữ liệu y bạ điện tử theo tiêu chuẩn quốc tế HL7 / FHIR (Fast Healthcare Interoperability Resources) để sẵn sàng liên thông dữ liệu bệnh án với các bệnh viện công lập và nền tảng VNeID y tế quốc gia.", "• Liên thông chuẩn y tế FHIR: ")

    print("Part 1 built successfully.")
