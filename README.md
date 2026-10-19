# Learning Path — Aplikasi Android

Pelacak progres belajar siswa berbasis gamifikasi (Kotlin, XML Views, Firebase).

## Status Implementasi
- [x] Struktur project & dependency Gradle
- [x] Model data (User, Course, Module, Progress, Achievement, UserAchievement)
- [x] Repository layer (Auth, Course, Gamification)
- [x] Autentikasi: Login, Register, Reset Password (SRS-F-01,02,03)
- [x] Edit Profil (SRS-F-04)
- [x] MainActivity + navigasi role (siswa/guru)
- [x] HomeFragment: daftar mapel + progress bar (SRS-F-05,08)
- [x] CourseDetailActivity: daftar modul + tandai selesai (SRS-F-06,07)
- [x] ProgressFragment: ringkasan progres per mapel (SRS-F-05)
- [x] XP otomatis + Level (SRS-F-09,10)
- [x] Badge otomatis (SRS-F-11)
- [x] Leaderboard real-time (SRS-F-12)
- [x] ProfileFragment: profil + koleksi badge + logout
- [x] Dashboard Guru: rekap siswa + search/filter (SRS-F-13,15)
- [x] Kelola Modul: CRUD guru (SRS-F-14)

## Langkah yang HARUS Anda lakukan (sedang berjalan)

### 1. Selesaikan instalasi Android SDK
Buka **Android Studio** → *SDK Manager* → install **Android SDK Platform 34**,
**Build-Tools**, dan **Platform-Tools**. Buat **AVD** (emulator Pixel, API 34).

### 2. Siapkan Firebase (wajib untuk build)
1. Buka `https://console.firebase.google.com` → **Buat project** (mis. `learning-path-app`)
2. Tambahkan **aplikasi Android**, package name: `com.ridho.learningpath`
   - Debug signing SHA-1 diambil dari `~/.android/debug.keystore`
3. **Aktifkan Authentication** → Sign-in method → **Email/Password**
4. **Aktifkan Cloud Firestore** → buat database (mode production)
5. Unduh **`google-services.json`** → letakkan di folder: `LearningPathApp/app/`
6. Di Firestore, buat collection `achievements` dan `courses` (seed bad bank)

### 3. Buka project di Android Studio
Buka folder `LearningPathApp` dengan Android Studio. Studio akan:
- Men-generate **Gradle wrapper** (gradlew, jar)
- Menyinkronkan dependency
- Klik **Run** untuk menjalankan di emulator

> **Index yang perlu dibuat di Firestore** (auto muncul saat error):
> - `users` : `role` ASC + `totalPoin` DESC (untuk leaderboard)

## Catatan Firestore — koleksi utama
```
users                 -> id, nama, email, role, avatar, level, totalPoin
courses               -> id, namaMapel, deskripsi, guruId
modules               -> id, courseId, judulMateri, urutan, konten, xp
progress              -> id, userId, moduleId, status, tanggalSelesai
achievements          -> id, namaBadge, deskripsi, syaratPoin
userAchievements      -> id, userId, achievementId, tanggalDidapat
```

## Struktur
```
app/src/main/java/com/ridho/learningpath/
├── data/model/       (data class)
├── data/repository/  (Auth, Course, Gamification)
├── ui/auth/          (Login, Register, ResetPassword)
├── ui/               (MainActivity)
├── ui/home|progress|leaderboard|profile|teacher/
```
