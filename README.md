# StopMe

**Ingatkan Sampai Tujuan.** Aplikasi Android yang membangunkan penumpang transportasi umum Jabodetabek sebelum tiba di halte atau stasiun tujuan, supaya tidak kelewatan saat tertidur atau sibuk dengan HP.

> Status: versi uji coba (v17c). Repository ini bersifat privat.

---

## Fitur

- **Pengingat berbasis GPS real time.** Posisi dipantau di latar belakang, termasuk saat layar mati.
- **Tiga mode alarm** yang dipilih pengguna:
  - **Jarak:** alarm bunyi saat sisa jarak mencapai radius (200 m – 2 km untuk bus, 1 – 7 km untuk kereta).
  - **Waktu:** alarm bunyi 3, 4, atau 5 menit sebelum perkiraan tiba.
  - **Keduanya:** dua alarm terpisah. Jika keduanya hampir bersamaan (selisih < 30 detik), digabung menjadi satu alarm yang lebih kuat.
- **Pengingat halte berikutnya.** Notifikasi dan getar singkat saat halte berikutnya adalah tujuan.
- **Alarm layar penuh.** Tampil di atas lockscreen, tetap bunyi saat mode senyap, dimatikan dengan geser.
- **Mode offline.** Saat GPS hilang (misalnya MRT bawah tanah), perkiraan waktu dari jadwal antar halte dipakai sebagai cadangan.
- **Pencarian halte dengan nomor rute.** Contoh: `Kampung Melayu (5, 5B, 5C, 5N, 7, 7U, 11)`.
- **Riwayat & Favorit Rute.** Rute yang dipakai 5 kali otomatis masuk Favorit, dan bisa dimulai ulang dengan satu ketukan.
- **Peta perjalanan** berbasis OpenStreetMap dengan garis rute, halte, dan posisi pengguna.
- **Akun pengguna.** Login email atau Google, foto profil, dan nama yang bisa diubah.

## Layanan yang didukung

| Layanan | Cakupan data |
|---|---|
| Transjakarta | Koridor BRT dan rute non-BRT (GTFS resmi) |
| Jaklingko (Mikrotrans) | Seluruh rute JAK (GTFS resmi) |
| KAI Commuter | Lin Bogor, Cikarang, Rangkasbitung, Tangerang, Tanjung Priok |
| MRT Jakarta | Lebak Bulus – Bundaran HI |
| LRT Jakarta & LRT Jabodebek | Data tersedia, fitur sementara disembunyikan |

## Teknologi

- **Bahasa & UI:** Kotlin, Jetpack Compose (Material 3), arsitektur MVVM
- **Lokasi:** Google Play Services Location (Fused Location Provider) di Foreground Service
- **Peta:** osmdroid (OpenStreetMap)
- **Backend:** Firebase Authentication (email & Google), Cloud Firestore (profil pengguna)
- **Login Google:** Credential Manager, dengan Google Sign-In versi lama sebagai cadangan
- **Data:** `transit_data.json` lokal (tanpa server), dibaca dengan kotlinx.serialization
- **Build:** Gradle (Kotlin DSL), minSdk 26 (Android 8.0)

## Struktur project

```
app/src/main/
├── assets/transit_data.json      # Data rute, halte, dan waktu tempuh semua layanan
├── java/com/hanyz/stopme/
│   ├── data/                     # Repository: transit, perjalanan, riwayat, auth, profil
│   ├── model/                    # Model data (rute, halte, konfigurasi perjalanan)
│   ├── service/                  # TrackingService (GPS & logika alarm), notifikasi
│   ├── ui/                       # Layar Compose: auth, izin, home, planner, aktivitas, alarm, profil
│   └── util/                     # Perhitungan jarak & format
└── res/                          # Gambar, ikon, font Afacad Flux, string
```

## Menjalankan project

### Prasyarat

- JDK 17
- Android SDK (Android Studio, atau command-line tools)
- HP Android dengan USB debugging aktif

### File konfigurasi (tidak disimpan di repository)

1. **`app/google-services.json`**: unduh dari Firebase Console → Project settings → Your apps.
2. **`local.properties`** di root project:

   ```properties
   sdk.dir=/path/ke/Android/Sdk
   WEB_CLIENT_ID=xxxxxxxx.apps.googleusercontent.com
   ```

   `WEB_CLIENT_ID` diambil dari Firebase Console → Authentication → Sign-in method → Google → Web SDK configuration.

3. Daftarkan **SHA-1** keystore debug di Firebase agar login Google berfungsi:

   ```bash
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android | grep SHA1
   ```

### Build & pasang

```bash
./gradlew installDebug      # build dan pasang ke HP yang tersambung
./gradlew assembleDebug     # hanya membuat APK di app/build/outputs/apk/debug/
```

## Sumber data & atribusi

- **Transjakarta & Mikrotrans:** GTFS resmi PT Transportasi Jakarta.
- **MRT Jakarta:** jadwal resmi antar stasiun.
- **KAI Commuter & LRT:** dataset komunitas, sebagian koordinat masih perlu diverifikasi di lapangan.
- **Peta:** © OpenStreetMap contributors.
- **Font:** Afacad Flux, SIL Open Font License 1.1.

Data menunjukkan rute dan titik pemberhentian, bukan aturan tarif atau tap in/tap out.

## Rencana pengembangan

- [ ] Carousel banner di Home
- [ ] Mengaktifkan kembali layanan LRT
- [ ] Verifikasi koordinat stasiun KRL di luar Jakarta
- [ ] Fitur Cari Peron & Me Transit (perjalanan dengan transit)
- [ ] Versi iOS

## Tim

| Peran | Nama |
|---|---|
| Developer | _isi nama_ |
| UI/UX Designer | _isi nama_ |

---

© 2026 Tim StopMe. Hak cipta dilindungi.
