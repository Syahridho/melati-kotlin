# Kontrak API Melati — Hasil Verifikasi

Dokumen ini awalnya berisi asumsi. Sebagian besar kini **sudah diverifikasi langsung**
terhadap server test `https://test.syahridho.my.id` dan bundle web Melati yang berjalan.
Bagian yang masih tebakan ditandai jelas.

**Pembaruan 28 Oktober 2026:** API key sudah tersedia dan seluruh alur (buat tiket +
lampiran, cek, balas, selesai) berhasil dijalankan dari aplikasi terhadap server test.
Bukti HTTP ada di `docs/QA-REPORT.md` §5.

## 1. Endpoint (terverifikasi)

| Fungsi | Metode & path | Bukti |
| --- | --- | --- |
| Buat tiket | `POST /api/tickets` (multipart) | `GET` → 405 "Supported methods: POST" |
| Cek tiket | `GET /api/check?ticket_number=…` | `POST` → 405 "Supported methods: GET, HEAD" |
| Balas | `POST /api/check/{ticketNumber}/reply` | route table Ziggy: `tickets.check.reply` |
| Selesai | `POST /api/check/{ticketNumber}/complete` | route table Ziggy: `tickets.check.complete` |

> Versi pertama implementasi memakai path `/api/reply` & `/api/complete` — keduanya
> **404** di server. Sudah diperbaiki.

## 2. Payload buat tiket (terverifikasi dari form web)

| Field | Nilai / sumber |
| --- | --- |
| `channel_id` | dari `local.properties` (`CHANNEL_ID`), nilai aktif **`11`** — server menerima dan mencatat `channel: "WBS"`. PRD §5.1 menyebut `1`; nilai final mengikuti backend. |
| `classification` | `pengaduan` \| `aspirasi` \| `permintaan_informasi` |
| `service_type` | diisi hanya untuk klasifikasi pengaduan. Aplikasi kini **selalu** mengirim `pst` (pilihan PST/Lainnya dihapus dari form, 31 Okt 2026). Nilai `pst` dan `lainnya` sama-sama diterima server. |
| `reporter_name` | nama pelapor |
| `reporter_email` | opsional |
| `reporter_wa` | nomor WhatsApp pelapor |
| `title` | judul — **wajib**; server menolak dengan `422 {"title":["Kolom title wajib diisi."]}` bila kosong (PRD menyebut opsional, klien mengikuti server) |
| `content` | isi laporan |
| `attachments[]` | maks 3 file, 2 MB/file |

Field tambahan yang dipakai web tetapi **belum ada di aplikasi**: `tanggal_kejadian`,
`satuan_tugas` (kemungkinan relevan untuk pengaduan PST). Tidak wajib: tiket berhasil
dibuat tanpa keduanya (`satuan_tugas: null` di respons).

**Catatan validasi server:** `reporter_email` divalidasi lebih ketat daripada klien —
`budi.e2e@example.com` ditolak dengan
`{"reporter_email":["Format reporter email tidak valid.", …]}` (422), sedangkan domain
nyata seperti `gmail.com` lolos. Aplikasi menampilkan pesan server itu apa adanya di
bawah field Email, jadi tidak perlu menebak aturannya di klien.

## 3. Payload balasan (sebagian terverifikasi)

`POST /api/check/{ticketNumber}/reply`, multipart:

| Field | Nilai |
| --- | --- |
| `message` | teks balasan |
| `attachments[]` | lampiran (jika didukung) |

Batas lampiran web: 3 file, 2 MB, ekstensi `jpg`, `jpeg`, `png`, `pdf`.
**Perbedaan:** PRD §5.1 hanya mengizinkan jpg/pdf; aplikasi mengikuti PRD sehingga
file PNG ditolak di klien meski web menerimanya. Perlu keputusan produk.

## 4. Status tiket (sebagian terverifikasi)

Nilai yang terlihat di kode web: `baru`, `selesai` (dengan `closed_reason`
`auto_close`), serta label tampilan "Menunggu Tanggapan". Pemetaan aplikasi:

| Nilai API | Status aplikasi |
| --- | --- |
| `baru`, `new`, `pending`, `menunggu…` | `NEW` (Baru) |
| mengandung `proses`/`progress`/`tindak` | `IN_PROGRESS` (Diproses) |
| `dijawab`, `answered`, `respons` | `ANSWERED` (Dijawab) |
| `selesai`, `resolved`, `done`, `complete` | `RESOLVED` (Selesai) |
| `tutup`, `closed` | `CLOSED` (Ditutup) |
| lainnya | `UNKNOWN` (Tidak diketahui) |

Balasan dinonaktifkan untuk `RESOLVED` dan `CLOSED`.

## 5. API key & base URL — TERVERIFIKASI

Server menolak semua `/api/*` tanpa key:

```json
{"success":false,"message":"Unauthorized. Invalid or missing API key."}
```

- **Header `X-API-Key` dikonfirmasi benar**: dengan key → lolos (200/201/404 "tiket
  tidak ditemukan"), tanpa key → 401, dan `Api-Key` → 401. `Authorization: Bearer`
  dan `X-Api-Key` tidak dikenali (404 HTML).
- Nilai key, base URL, dan channel dibaca dari `local.properties`
  (`API_KEY`, `API_BASE_URL`, `CHANNEL_ID`) → `BuildConfig`. Fallback ke gradle
  property `-Pmelati.apiKey`, `-Pmelati.baseUrl`, `-Pmelati.channelId`. Key tidak
  pernah ditulis di source.
- **Base URL**: `https://test.syahridho.my.id/`. Versi `/v1` yang sempat ada di
  `local.properties` **tidak ada di server** — semua path `/v1/...` mengembalikan
  halaman HTML 404 (Inertia), bukan JSON. Path API yang benar diawali `/api/`.
- **Bertentangan dengan PRD §6**: "tidak ada token/rahasia di dalam aplikasi".
  Key yang ditanam di APK dapat diekstrak. Opsi yang perlu diputuskan backend:
  1. endpoint publik tanpa key + rate limit (paling sesuai PRD), atau
  2. key jangka pendek per-instalasi, atau
  3. terima risiko key statis di APK (tidak disarankan).

Rate limit teramati: `X-RateLimit-Limit: 20` per menit.

## 6. Sisa pertanyaan untuk backend (PRD §13)

1. Field wajib sebenarnya; apakah `tanggal_kejadian` dan `satuan_tugas` wajib.
   (Sebagian terjawab: `title` wajib di server.)
2. Nilai sah `service_type` — aplikasi sekarang hanya mengirim `pst`.
3. ~~Nama header/parameter API key~~ → **`X-API-Key`**, terverifikasi.
4. Apakah `reply` menerima `attachments[]` (balasan teks sudah terbukti 201), dan
   apakah `complete` butuh body (tanpa body → 200).
5. Daftar status resmi + label warna web.
6. Aturan bisnis `complete` (status apa yang boleh ditandai selesai oleh pelapor).
7. URL produksi (sekarang masih server test).
8. Apakah `GET /api/check` mengembalikan seluruh thread atau perlu endpoint terpisah.
