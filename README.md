# J7 Việt Nam - Car Video (cá nhân)

Vũ Ngọc Chưởng - Cộng Đồng Người Dùng J7 Việt Nam

App Android phát **YouTube** và **TV (IPTV/M3U/HLS)** lên màn hình xe qua Android Auto.
Chỉ dùng cá nhân, không phát hành lên Play Store. App không giới hạn lúc phát; xem video khi lái xe nguy hiểm và có thể vi phạm luật.

## Giao diện trên xe
- Mặc định màn hình xe hiện **giao diện YouTube thật** (m.youtube.com): chạm để chọn video/đề xuất, vuốt để cuộn.
- Thanh nút: 🏠 trang chủ, ⬅ quay lại, 🎤 tìm bằng giọng nói (tiếng Việt, dùng micro điện thoại, cần cấp quyền micro), ⏯ phát/dừng.
- Chọn kênh TV trên điện thoại thì xe chuyển sang chế độ TV (⏮ ⏯ ⏭ và nút YouTube để quay lại).

## Cách hoạt động
- Khai báo là app *navigation* của Car App Library để Android Auto cấp một `Surface` toàn màn hình.
- ExoPlayer (Media3) vẽ video lên Surface đó; điều khiển ⏮ ⏯ ⏭ nằm ở action strip.
- Trên điện thoại: dán link YouTube (giải mã bằng NewPipeExtractor) hoặc URL danh sách M3U rồi chọn kênh.

## Cài đặt
0. Không cần Android Studio: vào tab **Actions** của repo, mở lần chạy "Build APK" mới nhất và tải artifact `j7-viet-nam-debug-apk` về điện thoại để cài.
1. Mở thư mục này bằng Android Studio, để nó sync Gradle (tự tạo wrapper), rồi Run để cài lên điện thoại.
2. Trên điện thoại: Cài đặt > Android Auto > bấm nhiều lần vào "Phiên bản" để bật **Developer settings**,
   rồi bật **Unknown sources**.
3. Kết nối xe, mở app "Car Video" trong danh sách ứng dụng của Android Auto.
   (Có thể cần bật "Application mode: Developer" nếu app không hiện.)
4. Mở app trên điện thoại: link YouTube và danh sách TV Việt Nam đã điền sẵn, app tự tải danh sách kênh; chọn kênh để phát.

## Nguồn mặc định
- TV: `https://iptv-org.github.io/iptv/countries/vn.m3u` (iptv-org, cộng đồng duy trì; kênh có thể hỏng bất kỳ lúc nào, chưa kiểm tra được từ môi trường dev).
- YouTube: `https://www.youtube.com/watch?v=CL13X-8o4h0` (sửa tuỳ ý trong app; giá trị được nhớ lại).

## Lưu ý
- Chưa được build/test trong repo này (môi trường không có Android SDK); có thể cần chỉnh nhẹ khi build lần đầu.
- Android Auto có thể chặn hoặc đổi hành vi Surface sau các bản cập nhật.
- Giải mã YouTube bằng NewPipeExtractor vi phạm điều khoản YouTube; chỉ dùng cá nhân và tự chịu trách nhiệm.
- Cần tôn trọng bản quyền các kênh TV bạn dùng.
