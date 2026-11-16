# Kana — Ứng dụng học viết Hiragana & Katakana 100% Offline

Ứng dụng Android native giúp người Việt học **VIẾT** bảng chữ cái tiếng Nhật (**Hiragana** trước, **Katakana** sau, tiếp theo là **Dakuten/Handakuten**, **Yōon**, `っ`, `ー`) theo cách trẻ em Nhật học: học theo hàng Gojūon, viết đúng thứ tự nét, gắn với từ vựng quen thuộc.

---

## Tính năng chính

1. **100% Offline & Không quyền Internet**:
   - Hoạt động hoàn toàn offline ngay sau khi cài đặt, không khai báo `android.permission.INTERNET` trong `AndroidManifest.xml`.
   - Không dùng Firebase, analytics, quảng cáo hay bất kỳ thư viện tải model runtime nào.
2. **Dữ liệu nét chuẩn KanjiVG & Từ vựng trẻ em Nhật**:
   - Đóng gói dữ liệu nét của **161 chữ Kana** từ dự án [KanjiVG](https://kanjivg.tagaini.net) (CC BY-SA 3.0) đã chuẩn hoá thành polyline 32 điểm/nét trong `assets/strokes/kana_strokes.json`.
   - Đóng gói **302 từ vựng** (2 từ/chữ) kèm phiên âm Romaji và nghĩa tiếng Việt trong `assets/vocab/kana_vocab.json`.
   - Đóng gói font **Noto Sans JP subset** (SIL OFL 1.1) trong `res/font/`.
3. **Luồng bài học 5 bước giảm dần gợi ý + Kiểm tra vượt bài**:
   - **Bước 1 (XEM)**: Animate từng nét theo thứ tự kèm số thứ tự nét, mũi tên hướng viết, phát âm, chỉnh tốc độ (`0.5x–2.0x`) và lặp lại.
   - **Bước 2 (ĐỒ THEO)**: Vẽ chồng lên nét mờ có sẵn.
   - **Bước 3 (VIẾT CÓ GỢI Ý)**: Chỉ hiện số thứ tự và điểm bắt đầu của nét kế tiếp.
   - **Bước 4 (VIẾT TỪ TRÍ NHỚ)**: Tự viết chữ từ Romaji & âm thanh (sai mới hiện đáp án + animate nét mẫu rồi bắt viết lại 1 lần).
   - **Bước 5 (TỪ VỰNG)**: Viết từ nhiều chữ trên dãy ô vuông kiểu giấy **原稿用紙** (chữ chưa học hiển thị xám sẵn, tự động chuyển ô sau 700ms ngừng bút).
   - **Kiểm tra nhanh / Vượt cấp**: Cho phép kiểm tra nhanh các chữ/hàng đã biết; nếu viết đúng hết 100% ngay lần đầu sẽ được Pass ngay và mở khoá chữ cái mới.
4. **Bộ chấm điểm chữ viết tay thuần Kotlin (`:recognizer`)**:
   - Không dùng ML; đối sánh hình học từng nét (khoảng cách điểm-điểm trung bình, Discrete Fréchet, góc vector hướng, cosine tiếp tuyến, trọng tâm nét và tỉ lệ độ dài nét).
   - Phát hiện lỗi: `WRONG_STROKE_COUNT`, `WRONG_ORDER`, `WRONG_DIRECTION`, `WRONG_SHAPE`, `WRONG_POSITION`.
   - Chống nhận nhầm bằng cách chấm đối chiếu với toàn bộ chữ cùng bộ; kiểm thử riêng 10 cặp chữ dễ nhầm (`さ/き`, `ぬ/め`, `わ/れ/ね`, `は/ほ`, `る/ろ`, `あ/お`, `ソ/ン`, `シ/ツ`, `ク/タ`, `ウ/ワ`).
   - Xem báo cáo kiểm thử và Confusion Matrix tại [RECOGNIZER_REPORT.md](RECOGNIZER_REPORT.md).
5. **Ôn tập ngắt quãng (SRS SM-2)**:
   - Lập lịch ôn tập tự động theo điểm của bộ chấm, trộn chữ đơn và từ vựng, ưu tiên các chữ hay viết sai.
   - Bảng Kana trên màn hình Trang chủ thể hiện độ thuộc bằng 4 cấp độ đậm nhạt của màu xám phẳng (không dùng gradient).

---

## Cấu trúc dự án

- `:recognizer` — Module Kotlin/JVM thuần chứa bộ chấm điểm `KanaRecognizer`, `StrokePreprocessor`, `DiscreteFrechet`, `RecognizerConfig` và bộ JUnit test.
- `:app` — Ứng dụng Android (Jetpack Compose, Material 3 tắt `dynamicColor`, Room, DataStore, MVVM tách `data`, `domain`, `ui`).
- `scripts/` — Script Python xử lý dữ liệu SVG KanjiVG (`build_kanjivg_assets.py`), từ vựng (`build_vocab_assets.py`) và font subset (`build_font_subset.py`).

## Cách build & Chạy kiểm thử

```bash
./gradlew test assembleDebug
```
