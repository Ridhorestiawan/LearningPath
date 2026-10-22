# AI_DECLARATION — Learning Path

Deklarasi transparansi penggunaan kecerdasan buatan
**Mata Kuliah:** Mobile Programming · **UTS** · Proyek Mandiri (Individu)
**Nama:** Ridho Restiawan · **NPM:** 2413025031 · **Program Studi:** Pendidikan Teknologi Informasi
**Institusi:** Universitas Lampung · **Tahun:** 2026

Berkas ini memuat deklarasi penggunaan AI sebagai alat bantu produktivitas (*copilot*)
sesuai ketentuan UTS. Tanggung jawab penuh atas kebenaran, pemahaman, dan keamanan
setiap baris kode tetap berada pada mahasiswa.

---

## 1. Daftar Alat

| Alat | Jenis | Peran Penggunaan |
|---|---|---|
| **opencode** (model: *Big Pickle*) | Agen coding AI berbasis CLI (terminal) dengan akses baca–tulis ke workspace project | Pair-programming: penyusunan rencana implementasi dari dokumen PRD/SRS, scaffolding kode Kotlin/Firebase, diagnosis bug, dan review kode |

**Catatan:**
- Tidak ada alat AI lain (GitHub Copilot, ChatGPT, Gemini, Claude, dsb.) yang digunakan pada project ini.
- Dokumen **PRD, SRS, Proposal, dan Laporan Proposal ditulis mandiri** di Microsoft Word sebelum sesi pemrograman dimulai (file `.docx` sudah ada pada 1 September 2026, sebelum sesi coding pertama).
- Riwayat percakapan AI project ini tersimpan di `session-ses_fa44.md` (riwayat sesi opencode yang diekspor ke repository).

---

## 2. Pembagian Peran

**Prinsip:** AI bertindak sebagai *copilot* — menghasilkan draf kode dan diagnosis; mahasiswa
menentukan **apa yang dibangun**, menyetujui keputusan desain, menguji di emulator, dan
menerima/menolak setiap saran berdasarkan hasil pengujian nyata.

| Bagian Project | Peran AI (opencode) | Peran Mahasiswa |
|---|---|---|
| PRD, SRS, Proposal, Laporan | Tidak dilibatkan | Ditulis mandiri di Word |
| To-do list pengembangan | Menyusun daftar tugas bertahap dari PRD/SRS | Menentukan urutan, prioritas, dan batasan scope per pertemuan |
| Arsitektur & stack (Kotlin, XML Views, Firebase) | Menawarkan opsi & konsekuensi teknis | **Memilih** stack dan arsitektur lapisan `model → repository → ui` |
| Skema koleksi Firestore (`users`, `courses`, `modules`, `progress`, …) | Menyusun dari kebutuhan SRS | Mengevaluasi, menyetujui, dan memverifikasi isinya |
| Model data & repository layer | Scaffolding kode | Review tiap fungsi, menyesuaikan kebutuhan fitur |
| Layar auth, home, progres, leaderboard, dashboard guru | Scaffolding Activity/Fragment + layout | Keputusan alur & role siswa/guru, iterasi desain UI/UX, pemilihan teks antarmuka |
| Fitur tambahan (upload file/PDF, link video, foto profil) | Implementasi atas instruksi | **Merumuskan kebutuhan fitur** ("guru perlu unggah materi", "tambah foto profil") |
| Konfigurasi Firebase & environment build | Memberi instruksi langkah demi langkah | **Eksekusi manual** di Firebase Console, penempatan `google-services.json`, instalasi SDK/AVD |
| Pengujian & debugging | Diagnosis berdasarkan laporan + logcat | **Menjalankan aplikasi di emulator**, menemukan & melaporkan bug, memverifikasi perbaikan benar-benar hilang |
| Perbaikan persiapan UTS (transaksi XP, DiffUtil, validasi email) | Usulan & implementasi perbaikan dari hasil review kode | Menyetujui usulan, memverifikasi hasil build & perilaku di aplikasi |


---

## 3. Studi Kasus Prompting

### Studi Kasus A — Prompt berbasis dokumen sendiri (efektif)

**Prompt (1 September 2026, sesi nyata):**
> *"saya ingin membuat aplikasi berdasarkan prd dan srs yg saya buat, buatkan to do listnya mulai dari awal hingga akhir"*

Dilanjutkan dengan instruksi inkremental:
> *"kerjakan sampai fase 1"* · *"bantu kerjakan to do yg blm dikerjakan"*

**Hasil awal AI:** daftar tugas implementasi bertahap yang diturunkan langsung dari
struktur PRD/SRS milik mahasiswa (model data → repository → autentikasi → layar siswa → gamifikasi → dashboard guru).

**Penyesuaian manual yang dilakukan:**
- Mahasiswa **memecah pekerjaan per pertemuan** dan memilih sendiri urutan prioritasnya,
  bukan menerima seluruh rencana sekaligus.
- Setiap keluaran AI **divalidasi terhadap dokumen SRS** (penomoran kebutuhan `SRS-F-01` … `SRS-F-15`)
  sebelum dilanjutkan ke tahap berikutnya.

**Mengapa prompt ini efektif:** konteksnya berupa dokumen spesifikasi yang sudah
ditulis sendiri, sehingga jawaban AI terukur pada kebutuhan nyata, bukan tebakan umum.

---

### Studi Kasus B — Galat AI: klaim "sudah diperbaiki" yang belum terverifikasi

**Kronik prompt pengujian (September 2026, sesi nyata — berurutan):**
> *"sekarang malah aplikasi nya jadi fc ketika di buka"*
> *"masih ada bug di mode guru ketika saya buka aplikasinya terjadi fc terus menerus"*
> *"masih terus terusan fc ketika saya login sebagai guru"*
> *"bantu eksekusi dan pastikan no bug dan berikan lokasi tempat kamu koreksi tadi"*

**Hasil awal AI:** AI beberapa kali menyatakan perbaikan selesai, namun force-close
masih terulang saat login sebagai guru.

**Cara deteksi & perbaikan manual:**
1. Mahasiswa **menolak menerima klaim "sudah selesai"** dan menguji ulang aplikasi di emulator,
   melaporkan gejala spesifik setiap kali (bukan sekadar "masih error").
2. Diagnosis akar masalah dilakukan dari **logcat**, antara lain:
   - `ClassNotFoundException` untuk `MainActivity` saat aplikasi dibuka;
   - `GridLayoutManager` tanpa `spanCount` di `fragment_profile.xml` (crash tab Profil);
   - tombol "Tambah Modul" tersembunyi (`visibility="gone"` permanen);
   - tombol back hilang karena `?attr/homeAsUpIndicator` gagal render, serta back-stack
     Dashboard Guru terputus oleh `FLAG_ACTIVITY_CLEAR_TASK`.
3. **Verifikasi final dilakukan oleh mahasiswa** dengan mengulang seluruh alur login guru
   di emulator sampai tidak ada crash — bukan dengan mempercayai pernyataan AI.

**Pelajaran:** AI sangat mudah mengklaim "selesai"; bukti satu-satunya yang sah adalah
aplikasi yang berjalan di depan layar. Kebiasaan *uji ulang setiap klaim* inilah yang
menjadi tameng utama terhadap halusinasi AI.

---

### Studi Kasus C — Galat logika: race condition pada pemberian XP

**Konteks:** versi awal `GamificationRepository.completeModule` (saran AI) memakai pola
*"baca dulu → baru tulis"*: cek apakah modul sudah pernah diselesaikan, baru menulis progres
dan menambah XP.

**Ditemukan saat:** bedah kode persiapan UTS — pola tersebut **tidak atomik**. Jika tombol
"Tandai Selesai" ditekan dua kali cepat, kedua proses bisa lolos pengecekan bersamaan sehingga
XP terhitung dua kali.

**Penyesuaian logika manual:**
- Seluruh operasi dibungkus ke dalam **satu Firestore Transaction** (`runTransaction`).
- Karena transaksi Firestore hanya bisa membaca `DocumentReference` (bukan query),
  ID dokumen progress diubah menjadi **deterministik `{userId}_{moduleId}`** agar
  pengecekan idempoten bisa dilakukan di dalam transaksi.
- Hasil akhir diverifikasi dengan build `assembleDebug` (**BUILD SUCCESSFUL**) dan
  komit terpisah `fix(gamification): bungkus completeModule dalam Firestore Transaction agar idempoten`.

---

## 4. Verifikasi dan Tanggung Jawab

1. **Build** — setiap perubahan kode di-build ulang (`assembleDebug`) sampai lulus sebelum diterima.
2. **Uji emulator** — alur login siswa/guru, tandai selesai, CRUD modul, dan navigasi back diuji langsung.
3. **Riwayat Git** — komit bertahap di repository menunjukkan alur pengerjaan, termasuk
   komit perbaikan terpisah (`fix`, `refactor`) sehingga proses koreksi terhadap saran AI
   dapat ditelusuri.
4. **Tanggung jawab** — mahasiswa memahami setiap baris yang dikirim ke repository dan
   mampu menjelaskannya pada sesi *code walkthrough* UTS.
