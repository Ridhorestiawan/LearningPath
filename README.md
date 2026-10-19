# Learning Path

**Aplikasi Android pelacak progres belajar siswa berbasis gamifikasi.**
Siswa menyelesaikan modul, mengumpulkan XP, naik level, dan mengumpulkan badge —
guru memantau progres seluruh siswa dari satu dashboard.

*Dibangun untuk mata kuliah **Mobile Programming** — UTS Semester Ganjil 2025/2026.*

| | |
|---|---|
| **Nama** | Ridho |
| **NIM** | 2413025031 |
| **Stack** | Kotlin · XML Views · Firebase (Auth, Cloud Firestore, Cloud Storage) |
| **Min SDK** | 24 (Android 7.0) — Target SDK 34 |
| **Arsitektur** | Layered: `data/model` → `data/repository` → `ui/*` (tanpa framework MVVM penuh) |

---

## Fitur

### Siswa
- [x] Registrasi / login / reset password (dengan validasi form)
- [x] Beranda: daftar mata pelajaran + progres keseluruhan
- [x] Detail mata pelajaran: daftar modul + persentase penyelesaian
- [x] Baca materi (teks, file PDF, tautan video) lalu **Tandai Selesai**
- [x] XP otomatis, kalkulasi level, dan pemberian badge
- [x] Ringkasan progres per mata pelajaran
- [x] Papan peringkat real-time antar-siswa
- [x] Profil: edit nama, ganti foto profil, koleksi badge, logout

### Guru
- [x] Dashboard rekap: jumlah siswa, rata-rata XP, daftar siswa + pencarian
- [x] Kelola modul (CRUD) termasuk unggah file materi dan tautan video
- [x] Buat & hapus mata pelajaran — hapus permanen beserta seluruh modul,
  file materi di Storage (konfirmasi berisi jumlah modul yang ikut terhapus)
- [x] Rekap progres siswa per mata pelajaran

### Inti Logika
- `completeModule` berjalan dalam **satu Firestore Transaction** (ID progress
  deterministik `{userId}_{moduleId}`) sehingga XP **tidak mungkin ganda** meski
  tombol ditekan berkali-kali secara bersamaan.
- Pemberian badge otomatis dan idempoten berdasarkan akumulasi `totalPoin`.

---

## Tangkapan Layar

> 📸 **Bagian ini perlu diisi** — ambil screenshot dari emulator (resolusi minimal 720p),
> simpan dengan nama berikut di folder `docs/screenshots/`, lalu komit.

| | | |
|---|---|---|
| ![Login](docs/screenshots/01_login.png) | ![Beranda](docs/screenshots/02_home.png) | ![Detail Modul](docs/screenshots/03_detail_modul.png) |
| ![Progres & XP](docs/screenshots/04_progres_xp.png) | ![Leaderboard](docs/screenshots/05_leaderboard.png) | ![Dashboard Guru](docs/screenshots/06_dashboard_guru.png) |

Daftar file yang dibutuhkan:
1. `01_login.png` — layar login
2. `02_home.png` — beranda siswa (daftar mapel + progress bar)
3. `03_detail_modul.png` — daftar modul & status selesai
4. `04_progres_xp.png` — penambahan XP / ringkasan progres
5. `05_leaderboard.png` — papan peringkat
6. `06_dashboard_guru.png` — dashboard guru / kelola modul

---

## Menjalankan Proyek

### Prasyarat
- Android Studio (Ladybug atau lebih baru) dengan **Android SDK Platform 34**
- **JDK 17+** (Android Studio JBR sudah mencukupi)
- Akun **Firebase** untuk project ini

### 1. Konfigurasi Firebase
1. Buka [Firebase Console](https://console.firebase.google.com) → buat/reuse project.
2. Tambahkan aplikasi Android dengan package name `com.ridho.learningpath`.
3. **Authentication** → aktifkan metode *Email/Password*.
4. **Cloud Firestore** → buat database (production mode).
5. **Cloud Storage** → aktifkan (untuk unggah file materi & foto profil).
6. Unduh `google-services.json` → taruh di `LearningPathApp/app/`
   (sudah ter-commit di repo ini; ganti dengan milik Anda bila ganti project).
7. Seed data awal di Firestore — collection `achievements` dan `courses`
   (struktur lihat bagian *Skema Firestore* di bawah).

### 2. Build & Jalankan
```powershell
# Dari folder LearningPathApp (Windows)
.\gradlew.bat assembleDebug
```
Atau buka folder `LearningPathApp` di **Android Studio** → tunggu *Gradle sync* →
pilih emulator/HP → klik **Run ▶**.

> **Index Firestore** (muncul otomatis saat error, buat sekali):
> `users` → `role` ASC + `totalPoin` DESC *(untuk papan peringkat)*

### 3. Akun Uji
Daftarkan 1 akun berperan **siswa** dan 1 akun berperan **guru** langsung dari aplikasi.

---

## Skema Firestore

```
users            → id, nama, email, role, avatar, level, totalPoin
courses          → id, namaMapel, deskripsi, guruId
modules          → id, courseId, judulMateri, urutan, konten, xp, fileName, fileUrl, videoUrl
progress         → id = {userId}_{moduleId}, userId, moduleId, status, tanggalSelesai
achievements     → id, namaBadge, deskripsi, syaratPoin
userAchievements → id, userId, achievementId, tanggalDidapat
```

## Struktur Project

```
app/src/main/java/com/ridho/learningpath/
├── data/
│   ├── model/        # data class (User, Course, Module, Progress, Achievement)
│   └── repository/   # AuthRepository, CourseRepository, GamificationRepository
└── ui/
    ├── auth/         # Login, Register, ResetPassword
    ├── home/         # Beranda siswa
    ├── course/       # Detail kursus & detail modul (Tandai Selesai + XP)
    ├── progress/     # Ringkasan progres per mapel
    ├── leaderboard/  # Papan peringkat real-time
    ├── profile/      # Profil, edit profil, koleksi badge
    ├── teacher/      # Dashboard guru, kelola modul, rekap siswa
    ├── common/       # UiState
    └── MainActivity.kt
```

---

## Transparansi AI

Project ini dibangun dengan bantuan **opencode** (agen coding AI) sebagai
*copilot* produktivitas. Pembagian peran per modul, studi kasus prompting,
serta galat AI yang dideteksi dan diperbaiki secara manual didokumentasikan di
[`AI_DECLARATION.md`](AI_DECLARATION.md).

---

## Roadmap menuju UAS

- [ ] **Notifikasi pengingat belajar** — Firebase Cloud Messaging (FCM) untuk
      pengingat harian/jadwal belajar siswa
- [ ] **Analitik progres mingguan** — grafik XP per hari/minggu (MPAndroidChart)
      pada layar Progres
- [ ] Cache lokal (Room) sebagai fallback saat offline
- [ ] Pemutar video bawaan untuk materi modul
