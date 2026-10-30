# -*- coding: utf-8 -*-
"""
build_full_report.py: Script tong hop tao Bao cao Do an Ky thuat FamilyCare
Do dai muc tieu: 70 - 100 trang A4 theo tieu chuan hoc thuat
"""
import os
import sys
import time

if sys.platform.startswith('win'):
    try:
        sys.stdout.reconfigure(encoding='utf-8', errors='replace')
    except Exception:
        pass

import doc_common
import doc_part1_intro
import doc_part2_theory_req
import doc_part3_arch_impl
import doc_part4_test_guide

def main():
    print("================================================================================")
    print("KHỞI TẠO TIẾN TRÌNH BIÊN SOẠN BÁO CÁO ĐỒ ÁN KỸ THUẬT FAMILYCARE (70 - 100 TRANG)")
    print("================================================================================")
    start_time = time.time()
    
    # Khởi tạo Document với lề chuẩn A4
    doc = doc_common.create_base_document()
    
    # 1. Xây dựng Part 1: Bìa, Lời cam đoan, Lời cảm ơn, Tóm tắt, Danh mục viết tắt, Mở đầu, Chương 1
    doc_part1_intro.build_part1(doc)
    
    # 2. Xây dựng Part 2: Chương 2 (Cơ sở lý thuyết & Công nghệ) và Chương 3 (Phân tích & Đặc tả yêu cầu)
    doc_part2_theory_req.build_part2(doc)
    
    # 3. Xây dựng Part 3: Chương 4 (Thiết kế Hệ thống & CSDL Room v4) và Chương 5 (Hiện thực hóa 7 Modules)
    doc_part3_arch_impl.build_part3(doc)
    
    # 4. Xây dựng Part 4: Chương 6 (Kiểm thử & Đánh giá lâm sàng), Chương 7 (Cài đặt & Vận hành), Kết luận & TLTK
    doc_part4_test_guide.build_part4(doc)
    
    # Lưu file Word kết quả
    output_filename = "Bao_Cao_Do_An_Ky_Thuat_FamilyCare_70_100_Trang.docx"
    output_path = os.path.join(os.path.dirname(__file__), output_filename)
    
    print(f"\nĐang lưu tài liệu vào: {output_path} ...")
    try:
        doc.save(output_path)
    except PermissionError:
        output_filename = "Bao_Cao_Do_An_Ky_Thuat_FamilyCare_70_100_Trang_v2.docx"
        output_path = os.path.join(os.path.dirname(__file__), output_filename)
        print(f"\n[LƯU Ý] File gốc đang mở trong Microsoft Word.")
        print(f"-> Tự động lưu bản mới vào: {output_path} ...")
        doc.save(output_path)
    
    elapsed = time.time() - start_time
    file_size_kb = os.path.getsize(output_path) / 1024
    
    # Đếm số đoạn văn, bảng biểu và số từ
    num_paras = len(doc.paragraphs)
    num_tables = len(doc.tables)
    total_words = sum(len(p.text.split()) for p in doc.paragraphs) + sum(
        sum(len(c.text.split()) for c in row.cells) for t in doc.tables for row in t.rows
    )
    
    print("================================================================================")
    print(f"XUẤT BÁO CÁO THÀNH CÔNG TRONG {elapsed:.2f} GIÂY!")
    print(f"- File đích: {output_path}")
    print(f"- Dung lượng file: {file_size_kb:.2f} KB")
    print(f"- Tổng số đoạn văn (Paragraphs): {num_paras}")
    print(f"- Tổng số bảng biểu (Tables): {num_tables}")
    print(f"- Tổng số từ vựng (Word count): {total_words:,} từ")
    print("================================================================================")

if __name__ == "__main__":
    main()
