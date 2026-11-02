# Laporan QA — Melati Mobile

Tanggal: 28 Oktober 2026, diperbarui 31 Oktober 2026
Artefak: `app-release-unsigned.apk` / `app-release-signed.apk` (ditandatangani
debug keystore hanya untuk pengujian lokal — **bukan** untuk distribusi).

## 1. Hasil build & ukuran

| Item | Hasil | Target PRD |
| --- | --- | --- |
| `:app:assembleRelease` | Sukses | — |
| `:app:lintVitalRelease` | Lulus | — |
| `:app:testDebugUnitTest` | 11 test lulus | — |
| Ukuran APK rilis | **2,1 MB** (2.128.981 B, build final 31 Okt; 1,8 MB sebelum aset splash + ikon) | < 10 MB |

R8 + optimized resource shrinking aktif lewat `optimization { enable = true }`
(AGP 9.3+ menggabungkan code shrinking dan resource shrinking).

## 2. Cold start (rilis, emulator Pixel 6 API 37, 1080×2400)

| Run | TotalTime |
| --- | --- |
| 1 (setelah instal, ART masih dingin) | 2.702 ms |
| 2 | 1.354 ms |
| 3 | 1.418 ms |
| Pengukuran akhir (build final) | 772 ms / 868 ms / 755 ms |
| Build dengan splash (final) | 626 ms / 690 ms |
| Build dengan ikon baru (final) | 753 ms |
| Build dengan konfigurasi API (final) | 573 ms / 702 ms |
| Build final 31 Okt (Layanan dihapus + Judul wajib), emulator di-reboot | 1.020 / 1.062 / 1.081 / 1.084 ms |

Angka `TotalTime` di atas mengukur frame pertama, yaitu layar splash. Konten utama
muncul setelah splash 0,7 dtk + crossfade 0,2 dtk, jadi sekitar **1,5–1,9 dtk** sejak
ikon diketuk — masih di bawah target 2 dtk, tetapi sisa marginnya tipis pada emulator
yang sedang berbeban (angka 1,0 dtk di atas diambil setelah reboot dengan load
host 2,3; run pertama setelah boot sempat 2,0–3,5 dtk karena layanan sistem masih
sibuk). Angka emulator ini tidak boleh dipakai sebagai bukti akhir — PRD §14 meminta
pengukuran di perangkat nyata.

Target PRD < 2 detik terpenuhi pada build rilis. Build **debug** jauh lebih lambat
(3,7–6,5 dtk) karena tanpa R8 dan dengan debug overhead — bukan angka acuan.
Angka di atas berasal dari emulator; perangkat menengah nyata perlu diukur ulang
di M5 (PRD §14: "uji di perangkat Android nyata").

## 3. Yang terverifikasi di perangkat

- Aplikasi terpasang dan diluncurkan tanpa crash (logcat `-b crash` bersih).
- Ketiga tab render: Laporan, Cek, Riwayat; bottom bar berfungsi.
- Tab Laporan: memilih **Lapor** tidak lagi menampilkan pilihan Layanan — sub-layanan
  dikunci ke **PST** oleh ViewModel saat klasifikasi dipilih (lihat §3a). Submit form
  kosong memunculkan "Wajib diisi" pada field nama, judul, dan isi laporan (validasi
  klien berjalan, tidak ada teks hardcoded di kode).
- Tab Cek: request `GET /api/check?ticket_number=…` benar-benar terkirim ke
  server test; respons 401 server ditampilkan sebagai panel "Gagal memuat tiket"
  dengan pesan Indonesia, tombol **Coba lagi** utuh, keyboard tertutup dan fokus
  dilepas setelah pencarian (baik lewat tombol Cek maupun tombol cari di IME).
- Tab Riwayat: empty state tampil.
- Tab Laporan, lampiran (diuji dengan file nyata di `/sdcard/Download`):
  - file 3 MB ditolak dengan pesan "Ukuran file melebihi 2 MB" dan tidak masuk daftar;
  - PDF 69 B diterima, tampil sebagai chip "dok.pdf / 69 B";
  - tombol hapus (×) pada chip berfungsi;
  - picker yang terbuka adalah DocumentsUI (SAF) dan hanya menampilkan jpg/pdf
    sesuai filter tipe MIME.
- Splash / layar loading (build rilis, diukur dari hierarki layout dan piksel):
  - logo `melati_big` tergambar 670×561 px (255×213 dp) dan wordmark
    `desc_melati2` 626×349 px (238×133 dp) pada layar 411 dp;
  - grup logo + jarak 28 dp + wordmark terpusat vertikal (708–1692 px,
    pusat 1200 px = pusat layar);
  - splash tampil ±0,7 dtk lalu crossfade 0,2 dtk ke layar utama;
  - system splash Android 12+ memakai ikon transparan + warna latar Melati,
    jadi ikon launcher default tidak lagi berkedip sebelum branding tampil;
  - `windowBackground` disamakan dengan `#FCF8F8` agar tidak ada kedipan putih.
- Ikon launcher (build rilis, diukur dari piksel app drawer dan layar App info):
  - `aapt2 dump badging` menunjukkan `icon='res/BW.xml'` → adaptive icon benar-benar
    dipakai, bukan PNG legacy;
  - latar putih penuh di dalam mask bulat (diameter 159 px pada grid drawer);
  - logo 118×98 px = **74%** diameter mask, seluruh logo utuh termasuk kelopak
    bunga di kanan bawah (tidak terpotong mask);
  - `pm clear` pada launcher dilakukan sebelum verifikasi karena launcher
    menyimpan cache ikon per paket;
  - ikon pertama yang sempat diuji memakai logo 58% kanvas (87% diameter mask) dan
    tampak sesak dibanding ikon bawaan, jadi diturunkan ke 50% (75%).
  - skrip pembuat ulang: `tools/generate_launcher_icon.py` (hasilnya identik
    dengan PNG yang ada di `res/`).

## 3a. Perubahan terakhir (31 Oktober 2026): Layanan dihapus, PST default, Judul wajib

**Layanan hanya PST.** Pemilih Layanan (PST/Lainnya) dihapus dari form. Saat pengguna
memilih **Lapor**, ViewModel langsung menetapkan `service_type = pst`; memilih
klasifikasi lain mengosongkannya. `LaporanEvent.LayananSelected` dan string
`label_layanan` dihapus. Baris "Layanan" di detail Cek juga dihapus karena nilainya
tidak lagi bisa berbeda — `LayananType` tetap dipertahankan di model/parser agar
tiket lama masih terbaca.

Bukti di perangkat:

| Build | Tindakan | Hasil |
| --- | --- | --- |
| Debug | Pilih Lapor, isi form tanpa menyentuh Layanan, kirim | tidak ada seksi "Layanan" di layar; tiket **L-1400/102026/PD08** tersimpan dengan `service_type: "pst"` (dicek lewat `GET /api/check`) |
| Rilis (R8 + resource shrinking) | idem, tiket **L-1400/102026/YP09** | `POST /api/tickets` sukses, `service_type: "pst"`, `channel: "WBS"`; tab Cek memuat tiket yang sama tanpa baris "Layanan" |

**Judul wajib.** PRD menyebut judul opsional, tetapi server menolak tiket tanpa
`title` (`422 {"title":["Kolom title wajib diisi."]}`). Klien kini mengikuti server:
label "Opsional" dihapus dari field Judul dan `validate()` menandainya `REQUIRED`.

Bukti: isi Nama + Isi laporan, biarkan Judul kosong, tekan Kirim → field Judul
berbingkai merah dengan pesan **"Wajib diisi"**, dan `logcat` tidak menunjukkan
request `POST /api/tickets` sama sekali (submit diblokir lokal, tanpa round-trip).

## 4. Bug yang ditemukan & diperbaiki selama QA

| Bug | Dampak | Perbaikan |
| --- | --- | --- |
| Jalur endpoint salah (`/api/reply`, `/api/complete`) | 404 | Diganti ke kontrak terverifikasi: `GET /api/check`, `POST /api/check/{n}/reply`, `POST /api/check/{n}/complete` |
| Pemetaan error validasi server tidak mengenali nama field asli (`reporter_name`, `reporter_wa`, `title`, `content`, `classification`, `service_type`) | Error per field dari server tidak pernah tampil | Kunci dinormalisasi (buang `_` dan `.`) dan dipetakan ke `LaporanField` |
| `serverFieldErrors` tidak dibersihkan saat submit ulang | Pesan error lama menetap | Direset bersama `fieldErrors` |
| Pesan 401 server tampil dalam bahasa Inggris | Melanggar lokalisasi PRD | Kode 401/403 dipetakan ke string Indonesia `error_unauthorized` |
| Panel Failed terpotong (tombol "Coba lagi" terpotong) | Tidak bisa menekan retry | Panel dibuat `verticalScroll` di dalam `Box`, kartu `fillMaxWidth` |
| Keyboard tetap terbuka setelah pencarian | Hasil tertutup | Sembunyikan berbasis fase (`Failed`/`NotFound`/`Loaded`), bukan hanya saat event |
| Tombol Cek menempel pada field | Cacat visual | Jarak 12 dp |
| Parser menandai balasan pelapor sebagai "Petugas" | Balasan pengguna sendiri tampil sebagai pesan petugas (salah sisi + salah label) | `is_reporter`/`is_admin` dibaca eksplisit; `type` (`balasan_pelapor`) ikut dikenali. Regresi ditutup unit test dengan payload server asli |
| Judul ditandai opsional padahal server mewajibkannya | Submit tanpa judul selalu gagal 422 setelah request terkirim | Label "Opsional" dihapus, `LaporanField.SUBJECT` divalidasi `REQUIRED` di klien |
| Pilihan Layanan (PST/Lainnya) tidak punya padanan keputusan produk | Pengguna harus memilih hal yang tidak mereka pahami; server hanya memakai `pst` | Pemilih dihapus, `service_type` dikunci ke `pst` untuk klasifikasi Lapor |

## 5. E2E terhadap server test (28 Oktober 2026, diulang 31 Oktober 2026)

API key tersedia lewat `local.properties`. Seluruh alur dijalankan dari APK di
emulator, bukan lewat curl — kecuali baris pembanding yang ditandai.

Tiket uji alur penuh: **L-1400/102026/RM06** (id 6, `channel: WBS`).
Tiket uji build final (Layanan dihapus): **L-1400/102026/PD08** (debug) dan
**L-1400/102026/YP09** (rilis R8) — keduanya `service_type: "pst"`.

| Langkah | Hasil |
| --- | --- |
| Buat tiket (Lapor / Lainnya, + lampiran PDF 193 B) | `POST /api/tickets` → **201** |
| Lampiran tersimpan | `GET /api/check` → `attachments[0].original_name = bukti_e2e.pdf`, `mime_type = application/pdf`, `size = 193` |
| Cek tiket | `GET /api/check?ticket_number=L-1400%2F102026%2FRM06` → **200**, status `baru` |
| Kirim balasan | `POST /api/check/{n}/reply` → **201**, thread tampil sebagai "Anda" (kanan) |
| Balasan petugas tampil benar | pesan dari web tampil sebagai "Petugas" (kiri) |
| Tandai selesai | `POST /api/check/{n}/complete` → **200**, badge berubah "Selesai", tombol hilang |
| Balasan dikunci setelah selesai | field nonaktif + "Tiket sudah selesai. Balasan tidak dapat dikirim." |
| Riwayat | tiket muncul dengan status `Selesai`; tetap ada setelah `am force-stop` + buka ulang (DataStore) |
| Ketuk item Riwayat | pindah ke tab Cek, nomor terisi, tiket langsung dimuat |
| Validasi server tampil di UI | email `example.com` ditolak 422 → pesan server di bawah field Email + banner "Validasi gagal." |
| Layanan dihapus (debug) | tiket PD08 dibuat tanpa memilih Layanan → `service_type: "pst"` |
| Layanan dihapus (rilis R8) | tiket YP09 dibuat dari APK rilis → 201, `service_type: "pst"`, lalu dimuat ulang di tab Cek tanpa baris "Layanan" |
| Judul wajib | submit dengan Judul kosong → "Wajib diisi" di klien, tanpa request `POST /api/tickets` |
| `title` wajib di server (pembanding curl) | `POST /api/tickets` tanpa `title` → `422 {"title":["Kolom title wajib diisi."]}` |

Catatan pengujian:
- Server menolak `reporter_email` berdomain `example.com` (422). Pakai domain nyata
  saat menguji.
- Nomor tiket memuat `/`; aplikasi meng-encode-nya (`%2F`) dan server tetap
  mencocokkannya — tidak perlu normalisasi di klien.

### Celah UX yang ditemukan (belum diperbaiki)

Saat submit gagal validasi dan field pertama yang salah berada di luar layar
(mis. Klasifikasi belum dipilih), tombol Kirim terlihat "tidak bereaksi" karena
pesan error muncul di atas. Perbaikan yang disarankan: gulir otomatis ke field
bermasalah pertama.

## 6. Belum terverifikasi

- Batas 3 lampiran / 2 MB / jpg & pdf di sisi server (batas klien sudah diuji).
- `reply` dengan lampiran (balasan teks sudah terbukti).
- Perilaku `complete` untuk tiket yang sudah selesai.
- Uji di perangkat Android nyata (PRD §14) — semua angka berasal dari emulator.

Lihat `docs/API-ASSUMPTIONS.md` untuk daftar pertanyaan backend.

## 7. Catatan sebelum rilis

1. **Package name** masih `com.example.melati` — tidak bisa dipublikasikan di
   Play Store; harus diganti sebelum rilis.
2. **API key** ditanam lewat `BuildConfig.API_KEY`; bertentangan dengan PRD §6
   ("tidak ada token/rahasia di dalam aplikasi") dan bisa diekstrak dari APK.
3. **Signing**: APK uji memakai debug keystore; rilis butuh keystore produksi
   dan AAB (`bundleRelease`) bila distribusi lewat Play Store.
4. PRD §7 menyebut badge status memakai `AssistChip`; implementasi memakai
   komponen pill kustom agar warna mengikuti web — perilaku setara, bentuk beda.
