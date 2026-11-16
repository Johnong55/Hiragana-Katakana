# Giả định kỹ thuật & thiết kế đã xác nhận (ASSUMPTIONS.md)

Tài liệu này ghi lại các thiết kế và quyết định kỹ thuật đã được xác nhận cho dự án **Kana**.

---

## 1. Kiến trúc & Công nghệ
1. **Cấu trúc Gradle Module & Package**:
   - Đa module: `include(":app", ":recognizer")`.
   - Module `:recognizer`: Kotlin/JVM thuần (100% không phụ thuộc Android SDK) chứa bộ chấm điểm chữ viết tay (`KanaRecognizer`, `RecognizerConfig`, thuật toán tiền xử lý, chuẩn hoá, Discrete Fréchet, template matching) và bộ kiểm thử JUnit.
   - Module `:app`: Chứa `data` (Room, DataStore, Assets loader, Repository), `domain` (Models, SRS, Lesson logic, AudioProvider interface), `ui` (Jetpack Compose, Material 3 tắt `dynamicColor`, ViewModel theo MVVM).
2. **SDK & Build**:
   - `minSdk = 26`, `targetSdk = 35`, Kotlin + Jetpack Compose, Material 3 (`dynamicColor = false`).
3. **100% Offline & Quyền hạn**:
   - Không khai báo `<uses-permission android:name="android.permission.INTERNET" />` trong `AndroidManifest.xml`.
   - Có unit test kiểm tra trực tiếp file `AndroidManifest.xml` để đảm bảo không tồn tại quyền `INTERNET`.
   - Không dùng Firebase, analytics, ads hay bất kỳ thư viện tải model runtime nào.

---

## 2. Dữ liệu nét (KanjiVG) & Font chữ
1. **Nguồn dữ liệu KanjiVG**:
   - Sử dụng dữ liệu từ dự án **KanjiVG** (CC BY-SA 3.0 - Ulrich Apel).
   - Script Python (`scripts/build_kanjivg_assets.py`) tải/đọc các file SVG gốc của Hiragana & Katakana (bao gồm 46 chữ cơ bản, dakuten/handakuten, yōon/chữ nhỏ `ぁぃぅぇぉっゃゅょ`, `ァィゥェォッャュョ` và dấu trường âm `ー`) lưu trong `scripts/kanjivg_raw/`, parse thẻ `<path d="..."/>` của từng nét theo đúng thứ tự `kvg:stroke_n`.
   - Mỗi nét được lấy mẫu (resample) thành polyline chuẩn hoá trên hệ quy chiếu `109x109` đưa về `[0.0, 1.0] x [0.0, 1.0]`, lưu vào `app/src/main/assets/strokes/kana_strokes.json` và commit vào repo để build hoàn toàn offline.
   - Ghi nguồn KanjiVG (CC BY-SA 3.0) đầy đủ trong màn hình **Giấy phép**.
2. **Font chữ**:
   - Đóng gói font `Noto Sans JP` (OFL) trong `app/src/main/res/font/` để hiển thị kana trong văn bản đồng nhất trên mọi thiết bị Android offline.
   - Chữ kana mẫu trên canvas và bảng học được render trực tiếp từ dữ liệu nét KanjiVG.

---

## 3. Từ vựng & Quy tắc hiển thị chữ chưa học
1. **Từ vựng**:
   - Mỗi chữ có 2 từ ngắn, quen thuộc với trẻ em Nhật, lưu trong `app/src/main/assets/vocab/kana_vocab.json`.
   - Cho phép từ vựng ở 46 chữ cơ bản chứa chữ chưa học (kể cả chữ ở hàng sau hoặc có dakuten như `かぎ`, `みず`).
   - Những chữ chưa học trong từ sẽ hiển thị màu xám sẵn trong ô và không bắt người dùng viết; khi người dùng đã học xong chữ đó (hoặc trong phần Ôn tập sau khi đã học hết các chữ cấu thành từ), ứng dụng sẽ bắt viết cả từ.
   - Danh sách các từ cần kiểm tra lại cách đọc/nghĩa được liệt kê trong `REVIEW.md`.

---

## 4. Cơ chế chấm điểm & Luồng bài học (5 bước)
1. **Thời điểm chấm điểm (Bước 2, 3, 4, 5 & Kiểm tra)**:
   - Tất cả các bước có vẽ (Bước 2: Đồ theo, Bước 3: Viết có gợi ý, Bước 4: Viết từ trí nhớ, Bước 5: Từ vựng, và Kiểm tra/Ôn tập) đều đợi người dùng viết xong toàn bộ chữ rồi bấm nút **"Kiểm tra"** mới tiến hành chấm điểm.
   - Ở **Bước 2 (Đồ theo)**: Hiển thị toàn bộ nét mờ của chữ mẫu. Người dùng vẽ chồng lên từng nét rồi bấm "Kiểm tra". Nếu sai nét thì rung nhẹ, hiện/animate lại nét đúng để người dùng viết lại.
   - Ở **Bước 3 (Viết có gợi ý)**: Dựa trên số nét người dùng đã vẽ trên canvas (ví dụ đã vẽ $k$ nét), hiển thị số thứ tự và điểm bắt đầu + hướng khởi đầu của nét kế tiếp ($k+1$). Viết xong bấm "Kiểm tra".
   - Ở **Bước 4, 5 & Kiểm tra / Ôn tập**: Không gợi ý nét, không animation. Chỉ khi bấm "Kiểm tra" mà kết quả sai mới hiện đáp án + animate nét mẫu, rồi bắt buộc người dùng viết lại đúng 1 lần.
2. **Viết từ nhiều chữ (Dãy ô vuông 原稿用紙)**:
   - Phía trên hiển thị dãy các ô vuông nhỏ của toàn bộ từ (kiểu giấy 原稿用紙), trong đó các chữ chưa học hiện sẵn màu xám, các chữ đã viết hiện nét người dùng vừa vẽ.
   - Phía dưới là 1 ô vuông canvas lớn tỉ lệ 1:1 (chiếm tối đa chiều rộng) cho ô chữ đang active.
   - Sau khi người dùng ngừng bút 700ms (mặc định, chỉnh được trong Cài đặt) và số nét trên ô hiện tại đã đủ số nét của chữ đó, ứng dụng tự động chuyển sang ô cần viết kế tiếp. Khi viết xong các ô trong từ, bấm **"Kiểm tra"** để chấm từng ô của cả từ.

---

## 5. Bộ chấm điểm (Recognizer) & SRS
1. **Thuật toán chấm (`:recognizer`)**:
   - Tiền xử lý: loại bỏ nét quá ngắn (chấm vô tình), resample mỗi nét thành 32 điểm cách đều, làm mượt nhẹ.
   - Chuẩn hoá toàn chữ theo bounding box (giữ tỉ lệ), kiểm tra kích thước tối thiểu và độ lệch góc.
   - So khớp số nét, thứ tự nét (`WRONG_ORDER`), hướng nét (`WRONG_DIRECTION`), hình dạng (`WRONG_SHAPE`: khoảng cách điểm-điểm trung bình + Discrete Fréchet), vị trí tương đối (`WRONG_POSITION`).
   - Chống chấp nhận nhầm: so sánh với toàn bộ chữ trong cùng bộ (Hiragana hoặc Katakana); chỉ `passed` khi chữ mục tiêu là `bestMatchChar` (hoặc đồng hạng nhất trong ngưỡng cho phép) VÀ `score >= threshold` theo 3 mức: **Dễ**, **Chuẩn**, **Nghiêm** trong `RecognizerConfig`.
2. **Spaced Repetition (SRS)**:
   - Dùng thuật toán SM-2 lưu trong Room cho từng chữ và từng từ; quy đổi `score` của `Recognizer` thành mức nhớ.
   - Bảng kana trên màn Home thể hiện độ thuộc bằng 4 sắc độ xám (không dùng gradient).
