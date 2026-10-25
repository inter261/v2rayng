# V2Rang (Android, Kotlin + Jetpack Compose, gaya neumorphism)

## Cara build
1. Buka folder ini di Android Studio (Koala atau lebih baru, JDK 17).
2. Tunggu Gradle sync (folder ini tidak menyertakan gradle wrapper; Android Studio akan menawarkan membuatnya).
3. Run di perangkat/emulator Android 9+ (minSdk 28).

## Sudah ada
- UI neumorphism terang/gelap (ikut tema sistem): Beranda, Server, Atur.
- Impor tautan vmess://, vless://, trojan://, ss:// (hanya baca nama/host untuk tampilan; tautan asli disimpan).
- Penyimpanan daftar server dan DNS (SharedPreferences, teks biasa; tautan bisa berisi kredensial).
- VpnService: meminta izin VPN, membuat TUN, mengirim fd TUN ke `Core.runner`.

## BELUM ada (perlu kamu kerjakan)
- **Inti proxy.** `Core.runner` bawaan (`NoopRunner`) selalu gagal dan menampilkan pesan error di Beranda.
  Implementasikan `CoreRunner` dengan inti native (mis. Xray-core via AAR dari proyek v2rayNG / AndroidLibXrayLite,
  perhatikan lisensinya, v2rayNG berlisensi GPL-3.0), lalu set `Core.runner = ...` di `MainActivity.onCreate`.
- **Konversi tautan -> konfigurasi JSON Xray**, dan meneruskan TUN fd ke inti (tun2socks).
- Statistik kecepatan, uji ping, notifikasi foreground, routing/bypass LAN.
- Kode ini belum dikompilasi atau diuji di perangkat; kemungkinan perlu perbaikan kecil saat pertama kali build.
