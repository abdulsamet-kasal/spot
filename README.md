# Spoyt

Kişisel kullanım için, Spotify benzeri bir Android müzik çalar. Şarkıları **YouTube Music**
üzerinde **NewPipe Extractor** ile arar, **Media3 (ExoPlayer)** ile arka planda oynatır ve
**WorkManager + Room** ile çevrimdışı indirir.

> Bu uygulama tek kullanıcılı, dağıtılmayacak kişisel bir projedir. Hesap/giriş, analitik,
> izleme (tracking), API anahtarı veya reklam içermez. Spotify ile hiçbir marka bağıntısı yoktur.

---

## Özellikler

| Alan | Durum |
| --- | --- |
| Arama (NewPipe → YouTube Music, `music_songs` filtresi) | ✅ |
| Arama sonuçlarında üç nokta menüsü (beğen, sıraya ekle, indir, listeye ekle) | ✅ |
| Arama satırlarında yükleme / boş / hata durumları | ✅ |
| Oynatma + kalıcı mini oynatıcı + tam ekran oynatıcı (seek bar) | ✅ |
| Kuyruk (listeye dokununca liste boyunca oynatma, sonraki/önceki) | ✅ |
| Kütüphane: Beğenilenler / İndirilenler / Çalma Listeleri | ✅ |
| Beğeni (like) aç/kapa | ✅ |
| Çalma listesi: oluştur, yeniden adlandır, sil, şarkı ekle/çıkar, tümünü oynat | ✅ |
| İndirme: sırada / indiriliyor / tamamlandı / başarısız | ✅ |
| Ana sayfa: son dinlenenler + kısayollar | ✅ |
| Ayarlar: sürüm + önbellek temizleme + geçmişi temizleme | ✅ |
| Arka planda oynatma + medya bildirimi / kilit ekranı kontrolleri | ✅ |
| Bildirim izni (Android 13+) çalışma zamanı isteği | ✅ |

---

## Teknoloji

- **Dil / UI:** Kotlin, Jetpack Compose + Material 3, tek `Activity`, Navigation Compose
- **Mimari:** MVVM (`ViewModel` + `StateFlow`), manuel bağımlılık enjeksiyonu (`AppContainer`), Hilt yok
- **Oynatma:** AndroidX Media3 (`media3-exoplayer`, `media3-session`, `media3-ui`) + `MediaSessionService`
  (`foregroundServiceType="mediaPlayback"`)
- **Veri:** Room (şema dışa aktarımı kapalı), Kotlin Flow (LiveData / RxJava yok)
- **Ağ:** NewPipe Extractor `v0.26.0` (JitPack), kendi `Downloader` implementasyonu OkHttp ile,
  tarayıcı benzeri `User-Agent`
- **İndirme:** WorkManager `CoroutineWorker` → uygulamaya özel depolama, durum Room'da
- **Görsel:** Coil (Compose)

### Sürüm notları (yerel ortama göre)

Bu proje yalnızca yerel makinedeki SDK ile derlenebilir şekilde yapılandırıldı:

- `compileSdk = 34`, `targetSdk = 34`, `minSdk = 26` (yalnızca `platforms/android-34` ve
  `build-tools/34.0.0` kurulu)
- AGP `8.6.0`, Gradle `8.10`, Kotlin `2.0.20`, Compose BOM `2024.10.00`
- Room için **kapt** (KSP değil), Coil `2.7.0` (`io.coil-kt` grubu), WorkManager `2.9.0`

---

## Derleme

```powershell
$env:ANDROID_HOME   = "C:\Users\abdul\AppData\Local\Android\Sdk"
$env:ANDROID_SDK_ROOT = "C:\Users\abdul\AppData\Local\Android\Sdk"

./gradlew.bat :app:assembleDebug
```

Linux / macOS:

```bash
./gradlew :app:assembleDebug
```

Çıktı APK:

```
app/build/outputs/apk/debug/app-debug.apk
```

---

## Kurulum (adb)

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb shell am start -n com.example.spoyt/.MainActivity
```

---

## Proje yapısı

```
native/
├── build.gradle.kts
├── settings.gradle.kts              # pluginManagement + JitPack deposu
├── gradle.properties
├── gradle/libs.versions.toml        # sürüm kataloğu
└── app/
    ├── build.gradle.kts
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml      # izinler + MediaSessionService
        ├── res/values/strings.xml   # Türkçe arayüz metinleri
        └── java/com/example/spoyt/
            ├── MainActivity.kt              # tek activity, scaffold + izin isteği
            ├── di/
            │   ├── SpoytApplication.kt      # NewPipe.init + AppContainer.init
            │   └── AppContainer.kt          # manuel DI
            ├── data/
            │   ├── local/                   # Room: entity, DAO, AppDatabase, repository
            │   └── remote/
            │       ├── NewPipeDownloader.kt # OkHttp tabanlı NewPipe Downloader
            │       └── YoutubeSource.kt     # tüm NewPipe kullanımı tek sınıfta
            ├── player/
            │   ├── PlayerHolder.kt          # ExoPlayer + kuyruk StateFlow'ları
            │   ├── PlaybackController.kt    # akış çözümleme + beğen + geçmiş
            │   └── MusicPlaybackService.kt  # MediaSessionService (bildirim)
            ├── download/
            │   ├── DownloadWorker.kt        # WorkManager indirme işçisi
            │   └── Downloads.kt             # kuyruğa alma / silme
            └── ui/
                ├── theme/Theme.kt           # koyu, Spotify benzeri tema
                ├── nav/                     # Screen + NavHost
                ├── components/              # TrackRow, MiniPlayer, BottomBar, diyaloglar
                ├── viewmodel/               # MVVM ekran mantığı
                └── screens/                 # Home, Search, Library, PlaylistDetail, Player, Settings
```

---

## Hata yönetimi

- Tüm NewPipe çağrıları `YoutubeSource` içinde toplanır ve `Dispatchers.IO` üzerinde
  `try/catch` ile sarılır; hata durumunda liste boş / `null` döner, uygulama **asla çökmez**.
- Kullanıcıya her zaman Türkçe ve anlaşılır bir mesaj gösterilir
  (`Arama sırasında bir sorun oluştu.`, `Şu anda oynatılamıyor.`, ...).
- Akış çözümlenemezse satır oynatılamaz olarak kalır; aynı menüden tekrar denenebilir.
- Ağ hatası çalışma zamanında istisna üretmez — arayüz durumu üzerinden bildirilir.

---

## Bilinen sınırlılıklar

1. **Yalnızca derleme/çalışma hedefi SDK 34.** Yerel makinede Android 35 kurulu olmadığı için
   `targetSdk 34`'te kalıyor; Android 15 cihazlarda uyumluluk modunda çalışır.
2. **Şarkı adı/artist bilgisi YouTube Music'ten geldiği şekliyle kullanılır.** Sanatçı adındaki
   ` - Topic` / ` - VEVO` ekleri temizlenir; çevrimdışı indirilen dosyalar uygulamanın iç
   deposunda (`filesDir/downloads`) tutulur, uygulama silinirse kaybolur.
3. **URL'ler kalıcı değil.** Akış adresleri her oynatım/indirme anında yeniden çözümlenir;
   bu yüzden indirme işçisi URL'leri Room'a yazmaz.
4. **Sıralama (sürükle-bırak) yok.** Çalma listesine eklenen şarkılar ekleme sırasına göre
   oynatılır.
5. **Çapraz cihaz senkronizasyonu yok** (hesap yok, kasıtlı olarak).
6. **NewPipe/YouTube arayüzü değişirse** arama veya akış çözümleme bozulabilir; tüm bağımlılık
   `YoutubeSource` sınıfında olduğundan tek dosya güncellenerek onarılır.
7. **Sürüm kodu 1 / adı `1.0.0`.** Yayınlama (release) imzalama yapılandırması eklenmedi;
   derleme hedefi `assembleDebug`.
