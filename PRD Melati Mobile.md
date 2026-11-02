# PRD — Melati Mobile (Kotlin Native, Aplikasi Publik)

**Produk:** Melati — Penanganan Konsultasi & Pengaduan BPS Provinsi Riau **Platform:** Android native (Kotlin + Jetpack Compose), ringan tanpa framework lintas platform **Penulis:** Syahridho **Status:** Draft v1.1 (versi Kotlin) **Backend:** Laravel API yang sudah ada (tanpa perubahan besar)

---

## 1. Ringkasan

Melati Mobile adalah aplikasi seluler untuk masyarakat (publik/guest) agar dapat menyampaikan laporan, aspirasi, atau permintaan informasi kepada BPS Provinsi Riau, memantau status tiket, dan berkomunikasi dengan admin/operator seperti chat. Aplikasi **hanya sebagai klien** yang mengonsumsi API Laravel yang sudah ada; tidak ada dashboard admin di aplikasi ini (dashboard tetap di web).

## 2. Latar Belakang & Masalah

- Masyarakat saat ini harus membuka web untuk mengirim pengaduan; pengalaman di ponsel kurang praktis.
- Pelapor sulit memantau perkembangan tiket dan membalas pesan admin tanpa membuka ulang formulir/halaman cek.
- Pelapor tidak punya akun, sehingga riwayat tiket mudah hilang jika nomor tiket tidak dicatat.

## 3. Tujuan

1. Mempermudah masyarakat mengirim Lapor, Aspirasi, dan Permintaan Informasi dari ponsel.
2. Memudahkan pelapor mengecek status tiket dan membalas pesan admin/operator dalam tampilan seperti chat.
3. Menyimpan riwayat tiket secara lokal di perangkat agar mudah diakses kembali tanpa login.
4. Menjaga tampilan konsisten dengan UI web Melati, dengan aplikasi yang ringan (APK kecil, start cepat, dependensi minimal).

### Non-Tujuan (Out of Scope v1)

- Login/registrasi akun pengguna.
- Fitur admin/operator (dashboard, rekap, kelola operator) — tetap di web.
- Push notification (dipertimbangkan v1.1).
- iOS dan pengembangan lintas platform (React Native/Flutter); aplikasi ini khusus Android native.
- Perubahan skema database/endpoint utama di backend.

## 4. Target Pengguna

| Persona | Deskripsi | Kebutuhan Utama |
| --- | --- | --- |
| Masyarakat umum (guest) | Warga Riau yang ingin melapor, memberi aspirasi, atau meminta informasi | Form singkat, tanpa login, dapat nomor tiket |
| Pelapor yang menunggu respons | Sudah punya nomor tiket | Cek status, baca balasan, membalas admin |

## 5. Ruang Lingkup Fitur

Aplikasi memiliki **3 bottom tab**: **Laporan**, **Cek**, **Riwayat**.

### 5.1 Tab Laporan (Buat Tiket)

**Deskripsi:** Formulir untuk membuat tiket baru.

**Alur:**

1. Pengguna memilih klasifikasi: **Lapor**, **Aspirasi**, atau **Permintaan Informasi**.
2. Jika klasifikasi **Lapor**, tampil pilihan layanan: **PST** atau **Lainnya**.
3. Pengguna mengisi data pelapor dan isi laporan, serta lampiran (opsional).
4. Tekan **Kirim** → request ke API → tampil **popup berisi nomor tiket** (prefix `L-`, `A-`, atau `I-`) dengan tombol salin.
5. Tiket otomatis tersimpan ke Riwayat lokal.

**Aturan:**

- Aplikasi selalu mengirim `channel_id = 1` saat membuat tiket.
- Lampiran maksimal **3 file**, ukuran maksimal **2 MB per file**, format **jpg/pdf**; validasi dilakukan di sisi klien sebelum upload dan ditampilkan pesan error yang jelas.
- Field wajib/opsional mengikuti validasi API yang ada (lihat bagian Pertanyaan Terbuka).
- Tombol Kirim nonaktif saat proses upload untuk mencegah submit ganda.
- Jika gagal (jaringan/validasi), data formulir tidak hilang.

**Kriteria Penerimaan:**

- Tiket berhasil dibuat dan nomor tiket tampil di popup.
- Lampiran melebihi batas ditolak sebelum dikirim.
- Error validasi dari server ditampilkan per field.

### 5.2 Tab Cek (Cek Tiket & Chat)

**Deskripsi:** Pengguna memasukkan nomor tiket untuk melihat status dan percakapan dengan admin/operator.

**Alur:**

1. Input nomor tiket → tekan **Cek** (`/api/check`).
2. Tampil detail tiket: klasifikasi, status, tanggal, isi laporan, dan **thread balasan** (respon awal dan respon substantif dari admin/operator).
3. Pelapor dapat **membalas** admin dalam tampilan seperti chat (bubble kiri = admin/operator, kanan = pelapor) melalui endpoint reply.
4. Pelapor dapat **menandai selesai** (`complete`) bila endpoint/aturan bisnis mengizinkan, dengan modal konfirmasi.

**Aturan:**

- Nomor tiket tidak ditemukan → tampil state kosong dengan pesan ramah.
- Input balasan dinonaktifkan jika tiket sudah selesai/ditutup.
- Lampiran pada balasan mengikuti batas yang sama (3 file, 2 MB, jpg/pdf) jika didukung API.
- Tarik-untuk-refresh (pull to refresh) untuk memuat balasan terbaru; polling ringan opsional.

**Kriteria Penerimaan:**

- Status dan balasan admin tampil sesuai data API.
- Balasan pelapor muncul di thread setelah terkirim.
- Aksi selesai meminta konfirmasi sebelum dikirim.

### 5.3 Tab Riwayat (Lokal)

**Deskripsi:** Daftar tiket yang pernah dibuat/dicek dari perangkat ini, disimpan **lokal** (tanpa login).

**Aturan:**

- Setiap tiket baru otomatis masuk Riwayat; tiket yang dicek lewat tab Cek dapat disimpan juga.
- Data minimal disimpan: nomor tiket, klasifikasi, judul/ringkasan, tanggal dibuat, status terakhir yang diketahui.
- Ketuk item → membuka tab Cek dengan nomor tiket terisi dan otomatis memuat detail.
- **Hapus** item memunculkan **modal konfirmasi** (sama seperti UI web). Opsi hapus semua dengan konfirmasi.
- Penyimpanan lokal menggunakan storage perangkat (Jetpack DataStore); data hilang bila aplikasi di-uninstall — informasikan ini kepada pengguna.

**Kriteria Penerimaan:**

- Riwayat tetap ada setelah aplikasi ditutup dan dibuka kembali.
- Hapus selalu didahului modal konfirmasi.

## 6. Kebutuhan Non-Fungsional

- **Performa:** layar utama muncul < 2 detik pada perangkat menengah; target ukuran APK rilis di bawah 10 MB; request memiliki timeout dan indikator loading.
- **Keandalan:** tangani offline dengan pesan jelas; Riwayat tetap dapat dibuka offline.
- **Keamanan & Privasi:** seluruh komunikasi via HTTPS; tidak menyimpan data sensitif selain yang dibutuhkan di perangkat; tidak ada token/rahasia di dalam aplikasi; nomor tiket diperlakukan sebagai kunci akses — pertimbangkan rate limiting di backend.
- **Aksesibilitas:** ukuran sentuh minimal 44 px, kontras memadai, label untuk pembaca layar.
- **Kompatibilitas:** Android 8.0 (API 26) ke atas.
- **Lokalisasi:** Bahasa Indonesia.

## 7. Desain & UI

- Gaya **Material 3** yang bersih dan minimal (kartu, input, tombol, dialog, badge status), warna dan tipografi disesuaikan agar menyerupai UI web Melati.
- Implementasi dengan **Jetpack Compose** + Material 3 saja, tanpa pustaka UI tambahan.
- Komponen inti: `Button`, `OutlinedTextField`, `Card`, badge status (`AssistChip`), `AlertDialog`, `NavigationBar` (bottom), `Snackbar`, dan bubble chat (composable kustom).
- Warna badge status mengikuti web agar konsisten.
- Dukungan mode terang; mode gelap opsional v1.1.

## 8. Arsitektur Teknis

| Aspek | Keputusan |
| --- | --- |
| Bahasa & platform | Kotlin, Android native (minSdk 26) |
| UI | Jetpack Compose + Material 3 |
| Navigasi | Navigation Compose, bottom bar (Laporan, Cek, Riwayat) |
| Arsitektur | MVVM sederhana: ViewModel + StateFlow, injeksi manual tanpa framework DI |
| HTTP & JSON | Retrofit + OkHttp + kotlinx.serialization, base URL via `BuildConfig` |
| Storage lokal | Jetpack DataStore untuk Riwayat (daftar tiket dalam JSON) |
| Upload | `multipart/form-data` via Retrofit (`MultipartBody.Part`); pilih file dengan Photo Picker / Storage Access Framework |
| Optimasi | R8 minify + resource shrinking; tanpa pustaka gambar, analytics, atau framework berat |
| Backend | Laravel API yang ada; **tanpa perubahan skema** |

**Base URL:**

- Test: `https://test.syahridho.my.id`
- Produksi: *(diisi kemudian)*

**Endpoint yang dikonsumsi (sesuai API yang sudah ada):**

| Fungsi | Endpoint | Catatan |
| --- | --- | --- |
| Buat tiket | `/api/tickets` | Kirim `channel_id=1`; mendukung lampiran |
| Cek tiket | `/api/check` | Berdasarkan nomor tiket |
| Balas | endpoint reply | Balasan pelapor ke admin/operator |
| Selesai | endpoint complete | Menandai tiket selesai |

> Bentuk request/response detail mengikuti implementasi API Laravel saat ini dan perlu dikonfirmasi saat integrasi.

## 9. Alur Pengguna Utama

1. **Buat laporan:** buka app → tab Laporan → pilih klasifikasi → isi form → kirim → popup nomor tiket → tersimpan di Riwayat.
2. **Pantau & balas:** tab Riwayat/Cek → masukkan/pilih nomor tiket → baca balasan admin → balas → refresh untuk melihat respons berikutnya.
3. **Kelola riwayat:** tab Riwayat → geser/ketuk hapus → konfirmasi modal → item terhapus dari perangkat.

## 10. Metrik Keberhasilan

- Persentase submit tiket berhasil dari total percobaan (target > 95%).
- Jumlah tiket yang dibuat via aplikasi per bulan.
- Persentase tiket yang mendapat balasan pelapor lewat tab Cek.
- Tingkat crash-free sessions (target > 99%).

## 11. Risiko & Mitigasi

| Risiko | Mitigasi |
| --- | --- |
| Nomor tiket hilang karena riwayat lokal terhapus | Tombol salin/bagikan nomor tiket di popup; peringatan di Riwayat |
| Spam/penyalahgunaan karena tanpa login | Rate limiting & validasi di backend; pertimbangkan captcha ringan |
| Upload lampiran gagal di jaringan lemah | Validasi ukuran di klien, indikator progres, retry |
| Perbedaan perilaku API dengan asumsi PRD | Verifikasi kontrak API sebelum implementasi tiap layar |

## 12. Rencana Rilis

| Fase | Cakupan |
| --- | --- |
| M1 | Setup proyek Kotlin + Compose, navigasi tab, tema Material 3 |
| M2 | Tab Laporan + integrasi `/api/tickets` + lampiran |
| M3 | Tab Cek + tampilan chat + reply + complete |
| M4 | Tab Riwayat lokal + modal hapus |
| M5 | QA, perbaikan, build APK/AAB, uji internal di server test |
| M6 | Rilis terbatas lalu produksi |

## 13. Pertanyaan Terbuka

1. Field apa saja yang wajib pada form (nama, kontak, email, judul, isi)? Samakan dengan validasi API.
2. Apakah endpoint reply dan complete menerima lampiran?
3. Apakah pelapor boleh menandai tiket selesai, dan pada status apa?
4. Bagaimana format respons status tiket (daftar status dan label)?
5. URL produksi dan kebijakan CORS/rate limit untuk klien mobile?
6. Perlu push notification (FCM) pada rilis berikutnya?
7. Distribusi: Google Play atau APK langsung?

## 14. Checklist Implementasi (TODO)

- [ ] Buat proyek Android Studio (Kotlin, Jetpack Compose, minSdk 26)
- [ ] Siapkan tema Material 3 yang menyerupai UI web Melati
- [ ] Bottom bar dengan Navigation Compose: Laporan, Cek, Riwayat
- [ ] Buat API client (Retrofit + OkHttp, base URL via BuildConfig) dan penanganan error global
- [ ] Form Laporan: klasifikasi, layanan PST/Lainnya, validasi input
- [ ] Upload lampiran (maks 3 file, 2 MB, jpg/pdf) via Photo Picker / SAF
- [ ] Kirim tiket dengan `channel_id=1` + popup nomor tiket
- [ ] Tab Cek: input nomor tiket + detail tiket
- [ ] Tampilan chat + kirim balasan + aksi selesai dengan konfirmasi
- [ ] Tab Riwayat: simpan di DataStore, buka detail, hapus dengan modal konfirmasi
- [ ] State kosong, loading, dan offline di semua layar
- [ ] Aktifkan R8 + resource shrinking, cek ukuran APK (< 10 MB)
- [ ] Uji di server test dan perangkat Android nyata
- [ ] Build APK/AAB dan siapkan rilis
