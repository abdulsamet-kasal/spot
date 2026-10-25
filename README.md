# Spoyt

Kişisel kullanım için, Spotify benzeri bir Android müzik çalar. Şarkıları **YouTube Music**
üzerinde **NewPipe Extractor** ile arar, **Media3 (ExoPlayer)** ile arka planda oynatır ve
**WorkManager + Room** ile çevrimdışı indirir.

> Bu uygulama tek kullanıcılı, kişisel bir projedir. Hesap/giriş, analitik, izleme (tracking),
> API anahtarı veya reklam içermez. Spotify ile hiçbir marka bağıntısı yoktur.

**Son sürüm:** `1.4.1` — [sürümleri indir](https://github.com/abdulsamet-kasal/spot/releases/latest)

---

## Sürüm geçmişi

| Sürüm | Not |
| --- | --- |
| `1.4.1` | Yayın kararlılığı: kesilen akışta otomatik kurtarma (kaldığı yerden devam), bozuk URL'de manuel oynatmayla taze çözümleme, kilit ekranı medya kartı (önceki/duraklat/sonraki), aramada 60 sonuca kadar sayfalama |
| `1.4.0` | Tekrar/oynatma hızı/uyku zamanlayıcısı, kuyruk düzenleme, radyo, sözler (LRCLIB), arama geçmişi, bağlantıdan oynatma, Geçmiş sekmesi, yedekle/geri yükle, Wi-Fi kısıtı, ana ekran widget'ı, uygulama kısayolları, güncelleme kontrolü, R8 + KSP + testler + CI |
| `1.3.0` | Oynatıcıda çalma listesine ekleme, her şarkı satırında üç nokta menüsü, net kapak görselleri, anında şarkı geçişi, alt gezinme düzeltmesi |
| `1.2.0` | Oynatma çökmesi giderildi, durum senkronizasyonu düzeltildi, hamburger menü + responsive arayüz |
| `1.1.1` | İlk oynatmadaki sessiz hata giderildi (ExoPlayer başlangıçta hazırlanıyor) |
| `1.1.0` | Oynatma ve indirme hataları giderildi, arayüz baştan tasarlandı |
| `1.0.0` | İlk sürüm: arama, oynatma, kütüphane, indirme |

### 1.4.1 neler değişti?

- **Kesilen akışlarda otomatik kurtarma:** YouTube CDN bağlantısı ortada koparsa
  (şarkı ~1 dakikada durup bir daha başlamıyordu) uygulama URL'yi yeniden çözümler ve
  kaldığı andan oynatmaya devam eder; şarkının geride kalan kısmında erken bitiş
  algılanır ve taze URL ile yeniden başlatılır. Sunucu sadece aralıktaki (range) veriyi
  veren URL'leri de seçmemeye özen gösterir.
- **Tekrar başlatma düzeltmesi:** bir akış hatasından sonra oynat/duraklat düğmesi artık
  ölü URL'yi yeniden kullanmaz; her manuel oynatma taze bir çözümleme ve yeni deneme
  hakkıyla başlar.
- **Kilit ekranı medya kartı:** bildirim ve kilit ekranında şarkı adı, sanatçı ve kapak
  görünür; önceki / oynat-duraklat / sonraki düğmeleri çalışır (Android 13+ sistem medya
  kartında da atla düğmeleri özel oturum komutlarıyla eklenir). Daha geniş ses tamponu
  kısık ağlarda takılmaları azaltır.
- **Daha uzun arama listesi:** arama artık ~20 yerine 60 sonuca kadar sayfalandırır
  (aynı şarkılar tekilleştirilir; bir sayfa düşerse o zamana kadarki liste korunur).

### 1.4.0 neler değişti?

**Oynatıcı**

- **Tekrar modu:** oynatıcıdaki yeni "Tekrar" düğmesi sırayla *kapalı → tümünü tekrarla → tek şarkıyı tekrarla*
  arasında dolaşır; kuyruk sonunda liste başına sarılır veya tek şarkı baştan çalar.
- **Oynatma hızı:** üç nokta menüsünden 0.75x / 1x / 1.25x / 1.5x / 2x seçilebilir; seçim şarkılar
  arasında ve uygulama yeniden başlasa da korunur.
- **Uyku zamanlayıcısı:** 15/30/60 dakika seçenekleriyle geri sayım biterken oynatma durur;
  kalan süre menüde görünür.
- **Kuyruk yönetimi:** "Sırada" listesindeki her satırda üç nokta menüsü — *Sıradan çıkar*,
  *Öne al*, *Geri al*. Oynayan satır değişmeden sıra yeniden dizilebilir.
- **Radyo başlat:** oynatıcıdaki şarkının YouTube'daki ilgili şarkıları çözümlenip kuyruğa
  eklenir; tek şarkıyla başlayıp benzerlerle devam eder.
- **Sözler:** LRCLIB (ücretsiz, anahtarsız) üzerinden eş zamanlı veya düz sözler indirilir;
  alt sayfada aktif satır oynatmayla birlikte vurgulanır ve otomatik kaydırılır.
- **Kapaktan renk:** oynatıcı arka planı, kapak görselinden Palette ile çıkarılan koyu
  baskın renge göre boyanır; bulunamazsa mevcut yeşil/violet gradyanı kullanılır.

**Arama**

- **Arama geçmişi:** son 10 arama boş arama alanının altında çip olarak görünür; dokununca
  yeniden arar, temizleme düğmesi tüm geçmişi siler (Room'da yeni `search_history` tablosu,
  v1→v2 migration).
- **Bağlantıdan oynatma:** arama alanına yapıştırılan YouTube / YouTube Music bağlantısı
  algılanır ve "Bağlantıyı oynat" ile doğrudan çalınır.

**Kütüphane ve veri**

- **Geçmiş sekmesi:** Kütüphane'de yeni dördüncü sekme — son çalınanlar ve en çok dinlenenler
  (oynatma sayısına göre) ayrı bölümler halinde.
- **Yedekle / geri yükle:** Ayarlar'dan tüm şarkılar, beğeniler ve çalma listeleri tek JSON
  dosyası olarak dışa aktarılır ve sistem dosya seçicisiyle geri yüklenir (SAF, izin gerekmez).
- **Sadece Wi-Fi'da indir:** açıldığında indirme işleri WorkManager kısıtıyla yalnızca
  ölçülmeyen (Wi-Fi) ağda çalışır; kota altındaki bağlantıda sıra bekler.

**Platform**

- **Ana ekran widget'ı:** çalan şarkı + önceki / oynat-duraklat / sonraki düğmeleri; oynatma
  durumu değiştikçe anında güncellenir.
- **Uygulama kısayolları:** uygulama simgesine uzun basınca *Ara*, *Beğenilenler*,
  *İndirilenler* doğrudan açılır.
- **Güncelleme kontrolü:** Ayarlar açıkken GitHub Releases sessizce sorgulanır; yeni sürüm
  varsa sürüm sayfasını açan satır görünür.
- **Çökme kaydı:** beklenmeyen bir çökme `filesDir/crash.log` dosyasına yazılır; Ayarlar >
  Tanılama'dan panoya kopyalanabilir.

**Mühendislik**

- **R8 açık:** release derlemede küçültme + kaynak daraltma; NewPipe Extractor ve Rhino için
  geniş keep kuralları.
- **kapt → KSP:** Room derleyicisi artık KSP (`2.0.20-1.0.25`) ile derleniyor; derleme belirgin hızlandı.
- **Birim testleri:** `YouTubeUtils`, `LrcParser`, `ResolvedStream` (URL geçerliliği) ve
  `UpdateChecker` (sürüm karşılaştırması) saf fonksiyonları test edildi.
- **GitHub Actions:** her push'ta birim testler + debug APK derlenir; `v*` etiketlerinde APK
  otomatik olarak GitHub Release'e eklenir.

### 1.3.0 neler değişti?

- **Oynatıcıda çalma listesine ekleme:** oynatıcının sağ üstündeki üç nokta menüsünden ve
  ikincil işlem satırındaki yeni "Listeye ekle" düğmesinden çalma listesi seçilebiliyor.
- **Üç nokta menüsü her yerde:** ana ekran (son çalınanlar), beğenilenler, indirilenler,
  çalma listesi ve oynatıcı satırları artık aynı menüyü sunuyor — beğen, sonraya ekle, indir,
  çalma listesine ekle, listeden kaldır.
- **Net kapak görselleri:** büyük oynatıcı ve ana ekran kapakları artık küçük `mqdefault`
  yerine `maxresdefault`/`hqdefault` varyantını istiyor; YouTube'da yüksek çözünürlük yoksa
  otomatik olarak bir alt boyuta düşülüyor. Piksellenme giderildi.
- **Anında şarkı geçişi:** yeni şarkının akış adresi çözülene kadar eski şarkı çalmaya devam
  ediyordu. Artık geçişte eski ses hemen susuyor, tampon göstergesi görünüyor ve sıradaki iki
  şarkının adresi arka planda önceden çözülüyor; "sonraki" geçişleri beklemesiz çalışıyor.
- **Alt gezinme düzeltmesi:** Ayarlar (ve çalma listesi) ekranındayken alt gezinme düğmeleri
  tepki vermiyordu. Sekme geçişi artık yığını her zaman ana ekrandan yeniden kuruyor.

### 1.2.0 neler değişti?

- **Oynatma çökmesi giderildi:** akış çözümleme/yeniden deneme arka plan iş parçacığında
  çalışırken ExoPlayer'a doğrudan erişiyordu. Android bunu
  `IllegalStateException: Player is accessed on the wrong thread` ile öldürüyordu.
  Artık oynatıcıya dokunan her çağrı ana iş parçacığına taşınıyor; ayrıca oynatma ve indirme
  kapsamlarına `CoroutineExceptionHandler` eklendi, medya servisi kurulumu `try/catch` ile korundu.
- **Senkronizasyon düzeltmeleri:** arama sonuçları kaydedilirken mevcut şarkı satırları
  `REPLACE` ile eziliyor ve **beğeni durumu sıfırlanıyordu**. Artık yalnızca eksik satırlar
  ekleniyor. Beğeni değiştirilirken güncel değer Room'dan okunuyor (hızlı iki dokunuş artık
  birbirini iptal etmiyor). Arama ve indirme satırlarında kalp/indirme rozeti canlı güncelleniyor.
- **Hamburger menü:** `ModalNavigationDrawer` ile ana gezinme, çalma listeleri, indirilenler ve
  ayarlar tek yerden erişilebilir.
- **Responsive görünüm:** Ana ekran içeriği genişlikle sınırlandı, hızlı erişim kartları
  ekran genişliğine göre 2/3/4 sütuna çıkıyor; oynatıcı 720dp ve üzerinde iki panelli düzene
  geçiyor, sıra listesi alt sayfada açılıyor.
- **Zenginleştirilmiş ana ekran:** "Bugünün ritmi" kartı (karıştır / beğenilenler), son
  çalınanlar için yatay kapak şeridi ve beğeni durumu satırlarda.
- **Zenginleştirilmiş oynatıcı:** beğen/indir/sıra ikincil işlem satırı, alt sayfada kuyruk,
  geniş ekranda yan yana kapak + kontroller.

### 1.1.1 neler değişti?

- **Kritik oynatma düzeltmesi:** ExoPlayer yalnızca medya servisi başladığında oluşturuluyordu.
  Uygulama açıldıktan sonraki ilk oynatma, motor henüz yokken çalıştığı için sessizce hiçbir şey
  yapmıyordu. Artık motor uygulama başlangıcında hazırlanıyor ve gerekirse oynatma öncesi
  yeniden oluşturuluyor.
- Medya servisi, şarkı başlamadan önce ayağa kaldırılıyor (bildirim/kilit ekranı tutarlı).
- Uygulama konteyneri (DB, depolar) artık idempotent: activity yeniden oluşturulsa da
  oynatma denetleyicisi sıfırlanmıyor.

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
| Arama geçmişi çipleri + YouTube bağlantısından doğrudan oynatma | ✅ |
| Arama satırlarında yükleme / boş / hata durumları | ✅ |
| Oynatma + kalıcı mini oynatıcı + tam ekran oynatıcı (seek bar) | ✅ |
| Kuyruk (listeye dokununca liste boyunca oynatma, sonraki/önceki) | ✅ |
| Kuyruk yönetimi (çıkar / öne al / geri al) + tekrar modu + oynatma hızı + uyku zamanlayıcısı | ✅ |
| Radyo (ilgili şarkılarla devam etme) + LRCLIB sözleri + Palette kapak rengi | ✅ |
| Kütüphane: Beğenilenler / İndirilenler / Çalma Listeleri / Geçmiş | ✅ |
| Beğeni (like) aç/kapa | ✅ |
| Çalma listesi: oluştur, yeniden adlandır, sil, şarkı ekle/çıkar, tümünü oynat, tümünü indir | ✅ |
| Yedekle / geri yükle (SAF ile JSON) | ✅ |
| İndirme: sırada / indiriliyor (yüzde) / tamamlandı / başarısız + yalnızca Wi-Fi kısıtı | ✅ |
| Ana sayfa: karşılama, hızlı erişim, son dinlenenler | ✅ |
| Ana ekran widget'ı + uygulama kısayolları (Ara / Beğenilenler / İndirilenler) | ✅ |
| Ayarlar: sürüm, depolama, Wi-Fi kısıtı, yedekleme, güncelleme kontrolü, çökme kaydı | ✅ |
| Arka planda oynatma + medya bildirimi / kilit ekranı kontrolleri | ✅ |
| Bildirim izni (Android 13+) çalışma zamanı isteği | ✅ |
| Birim testler + GitHub Actions CI (test + APK derleme + etikette release) | ✅ |

---

## Teknoloji

- **Dil / UI:** Kotlin, Jetpack Compose + Material 3, tek `Activity`, Navigation Compose
- **Mimari:** MVVM (`ViewModel` + `StateFlow`), manuel bağımlılık enjeksiyonu (`AppContainer`), Hilt yok
- **Oynatma:** AndroidX Media3 (`media3-exoplayer`, `media3-session`, `media3-ui`) + `MediaSessionService`
  (`foregroundServiceType="mediaPlayback"`)
- **Veri:** Room (şema dışa aktarımı kapalı, **KSP** derleyicisi), Kotlin Flow (LiveData / RxJava yok)
- **Ağ:** NewPipe Extractor `v0.26.5` (JitPack), kendi `Downloader` implementasyonu OkHttp ile,
  tarayıcı benzeri `User-Agent`; sözler için LRCLIB, güncelleme kontrolü için GitHub Releases API
- **İndirme:** WorkManager `CoroutineWorker` → uygulamaya özel depolama, durum + ilerleme Room / StateFlow,
  isteğe bağlı yalnızca Wi-Fi kısıtı
- **Görsel:** Coil (Compose) + androidx.palette (kapaktan vurgu rengi)
- **Sürüm yönetimi:** R8 küçültme (release), GitHub Actions CI

### Sürüm notları (yerel ortama göre)

Bu proje yalnızca yerel makinedeki SDK ile derlenebilir şekilde yapılandırıldı:

- `compileSdk = 34`, `targetSdk = 34`, `minSdk = 26` (yalnızca `platforms/android-34` ve
  `build-tools/34.0.0` kurulu)
- AGP `8.6.0`, Gradle `8.10`, Kotlin `2.0.20`, Compose BOM `2024.10.00`
- Room için **KSP** (`2.0.20-1.0.25`), Coil `2.7.0` (`io.coil-kt` grubu), WorkManager `2.9.0`,
  Palette `1.0.0`

---

## Kurulum

Hazır APK'yı [Releases](https://github.com/abdulsamet-kasal/spot/releases) sayfasından indirip
telefona kurabilirsiniz (Android 8.0+).

```powershell
adb install -r Spoyt-1.1.1.apk
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
