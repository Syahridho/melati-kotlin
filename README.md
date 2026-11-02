# Melati Mobile

Aplikasi Android native (Kotlin + Jetpack Compose) untuk menyampaikan Laporan,
Aspirasi, dan Permintaan Informasi kepada BPS Provinsi Riau, memantau status
tiket, dan berbincang dengan petugas — tanpa login.

Spesifikasi lengkap: `PRD Melati Mobile.md`.
Kontrak API & temuan integrasi: `docs/API-ASSUMPTIONS.md`.

## Ringkasan teknis

| Aspek | Keputusan |
| --- | --- |
| Bahasa / UI | Kotlin 2.2.10, Jetpack Compose + Material 3 |
| minSdk / targetSdk | 26 / 37 |
| Arsitektur | MVVM (`StateFlow` + ViewModel), injeksi manual (`di/AppContainer`) |
| HTTP | Retrofit + OkHttp, timeout 15s/30s/60s |
| JSON | kotlinx.serialization (parser toleran di `data/remote/TicketParser.kt`) |
| Storage | Jetpack DataStore (riwayat tiket, JSON) |
| Optimasi | R8 + optimized resource shrinking (`optimization { enable = true }`) |

Struktur paket:

```
com.example.melati
├── data/{local,remote,repository}
├── di/AppContainer.kt
├── domain/{model,repository}
└── ui/{components,navigation,screens,theme}
```

Layar pembuka ada di `ui/screens/splash/SplashScreen.kt` (logo `melati_big` di atas,
wordmark `desc_melati2` di bawah) dan dipasang di `MainActivity` selama 0,7 dtk
sebelum layar utama. Ukuran gambar mengikuti lebar layar dengan `fillMaxWidth(fraksi)`
+ `widthIn(max)` + `aspectRatio` asli; tanpa `aspectRatio`, `Image` memakai tinggi
piksel intrinsik drawable dan `ContentScale.Fit` akan mengecilkan gambarnya.

### Ikon aplikasi

Ikon memakai `melati_big.png` di atas latar **putih**:

- Android 8+ — adaptive icon `mipmap-anydpi-v26/ic_launcher.xml`: background
  `@color/ic_launcher_background` (#FFFFFF), foreground PNG
  `mipmap-*/ic_launcher_foreground.png` (kanvas 108 dp, logo 50% lebar kanvas
  agar tidak terpotong mask bulat/squircle).
- Android 7.x — `mipmap-*/ic_launcher.png` dan `ic_launcher_round.png`
  (kanvas 48 dp, logo 60%, latar putih, sudah menyatu dalam PNG).
- Logo tidak dipasang sebagai `monochrome`: ikon berwarna tidak punya siluet
  satu warna, jadi tanpa tag itu ikon temas tetap memakai versi berwarna.

Semua PNG di atas dihasilkan dari `drawable/melati_big.png` (Pillow), bukan
digambar manual — kalau logo berubah, buat ulang dengan skrip di
`tools/generate_launcher_icon.py`.

## Build

JDK: gunakan JBR bawaan Android Studio (JDK 25).

```bash
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"

./gradlew :app:assembleDebug          # APK debug
./gradlew :app:testDebugUnitTest      # unit test
./gradlew :app:assembleRelease        # APK rilis (unsigned)
```

### Konfigurasi (wajib)

Server Melati menolak request tanpa API key. Semua nilai lingkungan dibaca dari
`local.properties` (tidak masuk VCS, tidak pernah ditulis di source):

```properties
API_BASE_URL=https://test.syahridho.my.id
API_KEY=KUNCI_ANDA
CHANNEL_ID=11
```

`app/build.gradle.kts` memetakannya ke `BuildConfig.BASE_URL`, `API_KEY`, dan
`CHANNEL_ID`. Untuk CI tanpa `local.properties`, pakai gradle property:

```bash
./gradlew :app:assembleRelease   -Pmelati.baseUrl=https://test.syahridho.my.id   -Pmelati.apiKey=KUNCI_ANDA   -Pmelati.channelId=11
```

Header API key `X-API-Key` sudah diverifikasi terhadap server test (tanpa key → 401).
Base URL wajib berakhiran `/`; nilai `.../v1` yang pernah dipakai **tidak ada di
server** (semua path `/v1/*` mengembalikan HTML 404).

## Status implementasi (M1–M4 PRD)

- [x] M1 — setup, tema Material 3 terang, navigasi 3 tab
- [x] M2 — form Laporan + validasi klien + lampiran (maks 3, 2 MB, jpg/pdf) + popup nomor tiket
- [x] M3 — tab Cek: detail tiket, thread chat, balas, tandai selesai, pull-to-refresh
- [x] M4 — Riwayat lokal (DataStore), buka detail, hapus dengan konfirmasi
- [x] M5 — QA end-to-end di server test (buat tiket + lampiran, cek, balas, selesai,
      persistensi Riwayat)
- [ ] M5 (sisa) — keystore produksi, build AAB
- [ ] M6 — rilis

## Yang belum diverifikasi

Alur utama sudah diuji ke server test dengan tiket nyata (lihat `docs/QA-REPORT.md` §5).
Sisa: batas lampiran di sisi server, balasan dengan lampiran, uji di perangkat fisik,
dan keputusan produk soal PNG vs jpg/pdf serta nama paket final.
