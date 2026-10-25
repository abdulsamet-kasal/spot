# Spoyt

Kişisel kullanım için, Spotify benzeri bir Android müzik çalar. Şarkıları **YouTube Music**
üzerinde **NewPipe Extractor** ile arar, **Media3 (ExoPlayer)** ile arka planda oynatır ve
**WorkManager + Room** ile çevrimdışı indirir.

> Bu uygulama tek kullanıcılı, kişisel bir projedir. Hesap/giriş, analitik, izleme (tracking),
> API anahtarı veya reklam içermez. Spotify ile hiçbir marka bağıntısı yoktur.

**Son sürüm:** `1.1.0` — [sürümleri indir](https://github.com/abdulsamet-kasal/spot/releases/latest)

---

## Sürüm geçmişi

| Sürüm | Not |
| --- | --- |
| `1.1.0` | Oynatma ve indirme hataları giderildi, arayüz baştan tasarlandı |
| `1.0.0` | İlk sürüm: arama, oynatma, kütüphane, indirme |

### 1.1.0 neler değişti?

**Oynatma düzeltmeleri**

- NewPipe Extractor `v0.26.0` → **`v0.26.5`**. Eski sürüm güncel YouTube yanıtlarını
  çözümleyemiyordu; hem oynatma hem indirme bu yüzden sessizce başarısız oluyordu.
- ExoPlayer'a tarayıcı benzeri `User-Agent`, çapraz protokol yönlendirme ve zaman aşımı
  içeren özel HTTP katmanı eklendi (CDN'in `403` yanıtlarını engeller).
- Kuyruk artık **tembel** çözümleniyor: 20 şarkılık bir listede ilk şarkı hemen başlar,
  diğerleri sırası gelince çözümlenir (önceden hepsi önden çözümlenmeye çalışılıyordu).
- Süresi dolmuş akış adresleri (YouTube URL'leri ~6 saat geçerli) otomatik yenilenir;
  bir şarkı için tek otomatik yeniden deneme yapılır.
- Oynatma/indirme hataları artık Türkçe bildirim (Snackbar) olarak gösterilir.

**İndirme düzeltmeleri**

- Aynı NewPipe sürüm düzeltmesi indirmeyi de onardı.
- İndirme ilerlemesi yüzde olarak satırlarda gösterilir.
- 64 KB tampon, 20s/60s zaman aşımı, boş dosya kontrolü ve 2 otomatik yeniden deneme.
- Ayarlar'da indirilenlerin boyutu ve "İndirilenleri sil" eylemi.

**Yeni tasarım**

- Derin siyah zemin + canlı yeşil/violet gradyanlar, yeni tipografi ve köşe yarıçapları.
- Yüzen (floating) mini oynatıcı: kapak, ince ilerleme çizgisi, yükleme göstergesi.
- Yeni alt gezinme çubuğu: seçili sekmede tintli ikon rozeti.
- Ana sayfa: karşılama başlığı, arama hapı, gradyanlı hızlı erişim kartları.
- Arama: çerçevesiz yuvarlak arama alanı, sonuç sayısı, "Tümünü oynat" düğmesi.
- Kütüphane: pill (hap) biçiminde segment sekmeler, indirme ilerlemesi.
- Tam ekran oynatıcı: büyük kapak, özel seek bar, gradyanlı oynat düğmesi, "Sırada" listesi.

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
| İndirme: sırada / indiriliyor (yüzde) / tamamlandı / başarısız | ✅ |
| Ana sayfa: karşılama, hızlı erişim, son dinlenenler | ✅ |
| Ayarlar: sürüm, önbellek, indirilenler ve geçmiş temizleme | ✅ |
| Arka planda oynatma + medya bildirimi / kilit ekranı kontrolleri | ✅ |
| Bildirim izni (Android 13+) çalışma zamanı isteği | ✅ |

---

## Teknoloji

- **Dil / UI:** Kotlin, Jetpack Compose + Material 3, tek `Activity`, Navigation Compose
- **Mimari:** MVVM (`ViewModel` + `StateFlow`), manuel bağımlılık enjeksiyonu (`AppContainer`), Hilt yok
- **Oynatma:** AndroidX Media3 (`media3-exoplayer`, `media3-session`, `media3-ui`) + `MediaSessionService`
  (`foregroundServiceType="mediaPlayback"`)
- **Veri:** Room (şema dışa aktarımı kapalı), Kotlin Flow (LiveData / RxJava yok)
- **Ağ:** NewPipe Extractor `v0.26.5` (JitPack), kendi `Downloader` implementasyonu OkHttp ile,
  tarayıcı benzeri `User-Agent`
- **İndirme:** WorkManager `CoroutineWorker` → uygulamaya özel depolama, durum + ilerleme Room / StateFlow
- **Görsel:** Coil (Compose)

### Sürüm notları (yerel ortama göre)

Bu proje yalnızca yerel makinedeki SDK ile derlenebilir şekilde yapılandırıldı:

- `compileSdk = 34`, `targetSdk = 34`, `minSdk = 26` (yalnızca `platforms/android-34` ve
  `build-tools/34.0.0` kurulu)
- AGP `8.6.0`, Gradle `8.10`, Kotlin `2.0.20`, Compose BOM `2024.10.00`
- Room için **kapt** (KSP değil), Coil `2.7.0` (`io.coil-kt` grubu), WorkManager `2.9.0`

---

## Kurulum

Hazır APK'yı [Releases](https://github.com/abdulsamet-kasal/spot/releases) sayfasından indirip
telefona kurabilirsiniz (Android 8.0+).

```powershell
adb install -r Spoyt-1.1.0.apk
adb shell am start -n com.example.spoyt/.MainActivity
```

## Derleme

```powershell
$env:ANDROID_HOME     = "C:\Users\<kullanıcı>\AppData\Local\Android\Sdk"
$env:ANDROID_SDK_ROOT = "C:\Users\<kullanıcı>\AppData\Local\Android\Sdk"

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
            ├── MainActivity.kt              # tek activity, scaffold + izin + snackbar
            ├── di/
            │   ├── SpoytApplication.kt      # NewPipe.init + AppContainer.init
            │   └── AppContainer.kt          # manuel DI
            ├── data/
            │   ├── local/                   # Room: entity, DAO, AppDatabase, repository
            │   └── remote/
            │       ├── NewPipeDownloader.kt # OkHttp tabanlı NewPipe Downloader
            │       └── YoutubeSource.kt     # tüm NewPipe kullanımı tek sınıfta
            ├── player/
            │   ├── PlayerHolder.kt          # ExoPlayer + HTTP katmanı + kuyruk
            │   ├── PlaybackController.kt    # tembel akış çözümleme + yeniden deneme
            │   ├── PlaybackEvents.kt        # kullanıcıya gösterilecek mesajlar
            │   └── MusicPlaybackService.kt  # MediaSessionService (bildirim)
            ├── download/
            │   ├── DownloadWorker.kt        # WorkManager indirme işçisi (ilerleme + retry)
            │   └── Downloads.kt             # kuyruğa alma / silme / ilerleme
            └── ui/
                ├── theme/Theme.kt           # koyu tema, renk/tipografi/şekil sistemi
                ├── nav/                     # Screen + NavHost
                ├── components/              # Layout, TrackRow, MiniPlayer, BottomBar, diyaloglar
                ├── viewmodel/               # MVVM ekran mantığı
                └── screens/                 # Home, Search, Library, PlaylistDetail, Player, Settings
```

---

## Hata yönetimi

- Tüm NewPipe çağrıları `YoutubeSource` içinde toplanır ve `Dispatchers.IO` üzerinde
  `try/catch` ile sarılır; hata durumunda liste boş / `null` döner, uygulama **asla çökmez**.
- Kullanıcıya her zaman Türkçe ve anlaşılır bir mesaj gösterilir
  (`Bu şarkı şu anda oynatılamıyor.`, `Arama sırasında bir sorun oluştu.`, ...).
- Akış çözümlenemezse oynatıcı bunu bildirir; bir sonraki şarkıya geçilebilir.
- Ağ hatası çalışma zamanında istisna üretmez — arayüz durumu ve Snackbar üzerinden bildirilir.

---

## Bilinen sınırlılıklar

1. **Yalnızca derleme/çalışma hedefi SDK 34.** Yerel makinede Android 35 kurulu olmadığı için
   `targetSdk 34`'te kalıyor; Android 15+ cihazlarda uyumluluk modunda çalışır.
2. **Şarkı adı/artist bilgisi YouTube Music'ten geldiği şekliyle kullanılır.** Sanatçı adındaki
   ` - Topic` / ` - VEVO` ekleri temizlenir; çevrimdışı indirilen dosyalar uygulamanın iç
   deposunda (`filesDir/downloads`) tutulur, uygulama silinirse kaybolur.
3. **URL'ler kalıcı değil.** Akış adresleri her oynatım/indirme anında yeniden çözümlenir
   (ve 90 dakikadan sonra otomatik tazelenir); bu yüzden URL'ler Room'a yazılmaz.
4. **Sıralama (sürükle-bırak) yok.** Çalma listesine eklenen şarkılar ekleme sırasına göre
   oynatılır.
5. **Çapraz cihaz senkronizasyonu yok** (hesap yok, kasıtlı olarak).
6. **NewPipe/YouTube arayüzü değişirse** arama veya akış çözümleme bozulabilir; tüm bağımlılık
   `YoutubeSource` sınıfında olduğundan tek dosya (veya sürüm kataloğundaki NewPipe sürümü)
   güncellenerek onarılır.
7. **APK'lar debug anahtarıyla imzalanır.** Kişisel kullanım için yeterlidir; Play Store
   dağıtımı hedeflenmedi.
