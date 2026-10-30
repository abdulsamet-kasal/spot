# Sametify

Kişisel kullanım için, Spotify benzeri bir Android müzik çalar. Şarkıları **YouTube Music**
üzerinde **NewPipe Extractor** ile arar, **Media3 (ExoPlayer)** ile arka planda oynatır ve
**WorkManager + Room** ile çevrimdışı indirir.

> Bu uygulama tek kullanıcılı, kişisel bir projedir. Hesap/giriş, analitik, izleme (tracking),
> API anahtarı veya reklam içermez. Spotify ile hiçbir marka bağıntısı yoktur.

**Son sürüm:** `1.5.8` — [sürümleri indir](https://github.com/abdulsamet-kasal/spot/releases/latest)

---

## Sürüm geçmişi

| Sürüm | Not |
| --- | --- |
| `1.5.8` | Yeni **uygulama logosu** (mint→violet ekolayzır işareti, adaptif + Android 13 temalı + monokrom katman). Medya kartına **beğen** butonu eklendi (kalp, kalıcı `liked` durumuyla senkron), bildirim küçük ikonu marka işareti oldu. **Son kullanılanlardan kaydırınca müzik artık duruyor** (snapshot temizlenir, yeniden canlanmaz). **Kulaklık/Bluetooth çıkınca müzik duraklıyor** (`ACTION_AUDIO_BECOMING_NOISY`). Manifest'e `MediaBrowserService` action'ı eklendi — ROM'ların uygulamayı "müzik uygulaması" olarak tanımasına yardım eder |
| `1.5.7` | **Deney sürümü:** v1.5.1'in arka planda hiç kesilmemesi, v1.5.4'ten itibaren gelen **sürekli CPU + Wi-Fi kilitleriyle** açıklandı. Uygulama kilitleri artık yalnızca geçiş penceresinde (`playWhenReady` ve oynatıcı `IDLE`/`BUFFERING`) tutuluyor; parça çalarken (`READY`) ExoPlayer'ın kendi `WAKE_MODE_NETWORK` kilidi yeterli. Böylece vivo'nun "anormal pil tüketimi" avına hedef olma sebebi kalkıyor, geri kalan tüm düzeltmeler (snapshot, kurtarma, OriginOS kanalı) aynen kalıyor |
| `1.5.6` | vivo log'u kesin sonucu verdi: ROM oynatmayı **force-stop** ile kapatıyor, sistem servisi asla yeniden başlatmıyor (`service destroyed (playing=true)` sonrası restart yok). Üç yeni kurtarma yolu: (1) servis durdurulduysa süreç hâlâ hayattaysa kendi kendini bütçeli yeniden başlatma denemesi, (2) kullanıcı uygulamayı yeniden açtığında 10 dakikaya kadar eski oturumun kaldığı yerden otomatik devam (kullanıcının yeni seçimi öncelikli), (3) bildirim kanalı `IMPORTANCE_LOW` → `DEFAULT` + kanal kimliği yenilendi — OriginOS kilit ekranı/medya kartı düşük önemli bildirimleri gizleyebildiğinden Origin Ada'da çalar kartı bundan etkilendi |
| `1.5.5` | Vivo/OriginOS tarzı agresif ROM'lar oynatma sürerken medya servisini sonlandırıyor ve müzik bir daha geri gelmiyordu — çalma durumu (`kuyruk + index + pozisyon + playWhenReady`) artık periyodik olarak diske yazılıyor (`PlaybackSnapshot`), sistem servisi yeniden başlattığında (`START_STICKY` + null intent) aynı parçadan ve aynı saniyeden otomatik devam; sonsuz yeniden başlatma döngüsüne karşı pencereli bütçe (30 dk'da 4 deneme, 8 saatlik tazelik sınırı) |
| `1.5.4` | Ekran kapalıyken çalmanın kesilmesine karşı **katmanlı** çözüm: servis kendi CPU + Wi-Fi kilitlerini `playWhenReady` boyunca tutuyor (`PlaybackWakeLocks`), tampon 60-300 sn'ye çıkarıldı, HTTP okuma timeout'u 60 sn, tek atımlık yeniden deneme yerine backoff'lu 4 deneme ve bir "sessiz durma" bekçisi; tüm oynatıcı olayları artık Ayarlar → Tanılama üzerinden kopyalanabiliyor |
| `1.5.3` | Ekran kapalıyken ~30 sn içinde akışın kesilmesi: `C.WAKE_MODE_NETWORK` Wi-Fi kilidi için gereken `ACCESS_WIFI_STATE` izni hiç bildirilmemişti, hata da sessizce yutuluyordu — kilit artık gerçekten alınıyor ve Ayarlar pil istisnasının güncel durumunu gösteriyor |
| `1.5.2` | Kilit ekranı / sistem medya kartında **sonraki-önceki butonu gri** sorunu giderildi: kuyruk artık Media3'e standart oynatıcı komutları olarak tanıtılıyor (`QueueAwarePlayer`), böylece geçişler bildirim intent'i yerine native oturum üzerinden yapılıyor. Karıştır/tekrar da native duruma getirildi |
| `1.5.1` | Paket kimliği `com.sametify.app` oldu ve tüm APK'lar tek bir **Sametify** anahtarıyla imzalanmaya başladı — CI her çalıştırmada yeni debug anahtarı ürettiği için sürümler üst üste kurulamiyordu |
| `1.5.0` | Uygulama adı **Sametify** + yeni dalga formu logosu, sanatçı adları düzeltmesi (aramada "Bilinmeyen sanatçı"), radyo/mix yeniden çalışıyor, bildirim & kilit ekranında karıştır/tekrar butonları, çalma listesine ekle diyaloğu artık mevcut listeleri gösteriyor, kütüphane çalma listeleri sekmesiyle açılıyor, ana sayfada kişisel öneriler |
| `1.4.3` | Arka plan düzeltmesi: ekran kapanınca Wi-Fi/CPU uykuya geçip akış ~1 dakikada duruyordu — oynatıcı artık çalırken wake lock + Wi-Fi lock tutuyor (Wi-Fi kilidi ancak 1.5.3'te gerçekten alınıyor); Ayarlar'a pil optimizasyonu hariç tutma kısayolu eklendi |
| `1.4.2` | Erken bitişin asıl kaynağı giderildi: akış/indirme isteklerinde URL'nin istemcisine uygun User-Agent (Android/iOS YouTube istemcisi akışları artık tarayıcı kimliğiyle indirilmiyor), NewPipe isteğinin kendi UA'sı ezilmiyor |
| `1.4.1` | Yayın kararlılığı: kesilen akışta otomatik kurtarma (kaldığı yerden devam), bozuk URL'de manuel oynatmayla taze çözümleme, kilit ekranı medya kartı (önceki/duraklat/sonraki), aramada 60 sonuca kadar sayfalama |
| `1.4.0` | Tekrar/oynatma hızı/uyku zamanlayıcısı, kuyruk düzenleme, radyo, sözler (LRCLIB), arama geçmişi, bağlantıdan oynatma, Geçmiş sekmesi, yedekle/geri yükle, Wi-Fi kısıtı, ana ekran widget'ı, uygulama kısayolları, güncelleme kontrolü, R8 + KSP + testler + CI |
| `1.3.0` | Oynatıcıda çalma listesine ekleme, her şarkı satırında üç nokta menüsü, net kapak görselleri, anında şarkı geçişi, alt gezinme düzeltmesi |
| `1.2.0` | Oynatma çökmesi giderildi, durum senkronizasyonu düzeltildi, hamburger menü + responsive arayüz |
| `1.1.1` | İlk oynatmadaki sessiz hata giderildi (ExoPlayer başlangıçta hazırlanıyor) |
| `1.1.0` | Oynatma ve indirme hataları giderildi, arayüz baştan tasarlandı |
| `1.0.0` | İlk sürüm: arama, oynatma, kütüphane, indirme |

### 1.5.8 neler değişti?

- **Logo:** uygulamanın artık kendine ait modern bir işareti var — beş çubuklu ekolayzır,
  mint→violet gradyan, koyu mor-siyah zemin. Adaptif ikon (yuvarlak + kare maskeler),
  Android 13 "temalı ikon" için monokrom katman ve bildirim/medya kartı için tek renk
  küçük ikon aynı işaretten türetildi.
- **Beğeni butonu:** sistem medya kartında ve bildirimde kalp butonu var; basınca şarkı
  beğenilenlere eklenir/çıkarılır ve kalp dolu/boş olarak anında güncellenir.
- **Son kullanılanlardan kaydırma:** uygulamayı recents'ten silmek artık "dur" demek —
  müzik susar, bildirim kalkar ve oturum kaydı silinir (uygulamayı sonra açınca kendi
  kendine devam etmez).
- **Kulaklık çıkarma:** kablolu veya Bluetooth kulaklık bağlantısı kopunca oynatma
  duraklıyor; müzik telefon hoparlöründen dışarı taşmıyor.
- **ROM tanınması:** manifest'e `android.media.browse.MediaBrowserService` action'ı
  eklendi. Bazı OEM arayüzleri (OriginOS dahil) bir paketi "müzik uygulaması" olarak
  sınıflandırırken bu action'a bakıyor; kilit ekranı/ada kartı için ek şans.

### 1.5.7 neler değişti?

Bu sürüm bir **deney**: v1.5.1'in arka planda hiç kesilmemesi ile v1.5.4+ sürümlerin
~60 saniyede öldürülmesi arasındaki farkın **sürekli wake lock'lar** olduğu hipotezi
test ediliyor.

- **Kilit kapsamı daraltıldı:** `PlaybackWakeLocks` artık yalnızca geçiş penceresinde
  (`playWhenReady` açık ve oynatıcı `STATE_IDLE`/`STATE_BUFFERING`) tutuluyor. Parça
  çalarken (`READY`) ExoPlayer kendi `WAKE_MODE_NETWORK` kilidini zaten tutuyor, yani
  v1.5.4'teki gibi 6 saatlik kesintisiz CPU + Wi-Fi kilidi yok.
- **Neden:** v1.5.4+ log'larında `keepAwake cpu=true wifi=true` satırını ~60 sn sonra
  `service destroyed (playing=true)` takip ediyor ve hiç sistem restart'ı gelmiyor.
  Bu, ROM'un "anormal pil tüketen uygulama" avına uyan bir imza; v1.5.1 bu kilitleri
  hiç tutmadığı için aynı davranışı görmüyordu.
- **Değişmeyenler:** snapshot + otomatik devam, force-stop kurtarma denemesi,
  uygulama açılışında devam, OriginOS medya kartı için `IMPORTANCE_DEFAULT` kanal —
  hepsi aynen duruyor.

Beklenen sonuç: ekran kapalıyken çalma **kesilmemeli**. Kesilme yine olursa bu kez
kanıt tersini gösterir (kilitler suçsuz) ve cihaz tarafı izinler/ROM beyaz listesi
tek açıklama olarak kalır.

### 1.5.6 neler değişti?

v1.5.5'in log'u kesin sonucu verdi: `service destroyed (playing=true)` satırından sonra
**hiçbir sistem restart'ı gelmiyor** — yani vivo servisi durdurmakla kalmıyor, süreci
force-stop ediyor. Öldürülen bir sürecin kendini geri getirmesinin yasal bir yolu yok;
bu sürüm kalan üç rötuşu yapıyor:

- **Kendi kendine kurtarma denemesi:** servis durdurulduğunda süreç hâlâ hayattaysa
  (force-stop değil, salt servisi durdurma vaka) 1,5 sn sonra servis bütçeli tek
  denemeye yeniden başlatılıyor ve snapshot'tan devam ediyor. 30 dk'da 4 deneme
  penceresi ping-pongu engelliyor.
- **Uygulamayı açınca otomatik devam:** force-stop sonrası geri dönüş tek yolu
  kullanıcının uygulamayı açması. Son 10 dakika içinde kesilmiş ve `playWhenReady=true`
  olan oturum, uygulama açılışında kaldığı yerden (aynı parça + saniye) devam ediyor.
  Kullanıcı uygulamayı açıp başka şarkı seçtiyse onun seçimi kazanır.
- **OriginOS medya kartı:** bildirim kanalı `IMPORTANCE_LOW`'tan `IMPORTANCE_DEFAULT`'a
  çıkarıldı ve kanal kimliği yenilendi (`spoyt_playback2`) — vivo'nun kilit ekranı ve
  kontrol merkezi çalar kartı, düşük önemli bildirimleri gizleyebiliyor; kanal yenisi
  kurulumdan sonra abonelik ayarlarıyla birlikte yeniden oluşur.

### 1.5.5 neler değişti?

Bu sürümün konusu **servis ölümünden sonra kendine gelme**: tanılama kaydında vivo'nun
oynatma sürerken servisi hata olmadan sonlandırdığı görüldü (`service destroyed
(playing=true)`); kuyruk yalnız bellekte yaşadığı için müzik bir daha dönmüyordu.

- **Kayıt altına alma:** çalma durumu (kuyruk, index, pozisyon, `playWhenReady`) artık
  hem oynatıcı bekçisinde 10 saniyede bir, hem şarkı/duraklatma geçişlerinde hem de
  servisin `onDestroy`'ünde diske yazılıyor (`AppPrefs.savePlaybackSnapshot`).
  `commit()` kullanılır: proses aniden öldürüldüğünde `apply()` yazarları diske
  zamanında ulaşmayabilir.
- **Otomatik devam:** sistem servisi kendisi yeniden başlattığında (intentsiz
  `START_STICKY` restart) `MusicPlaybackService` null intent'i yakalayıp kalıcı
  snapshot'tan kuyruğu kurar ve son pozisyonla devam eder. Kayıt yalnızca **sistem yeniden başlatmasında** çalışır: kullanıcı uygulamayı elle açıp
  yeni bir şarkı oynatırsa onun seçimi kazanır.
- **Döngü koruması:** ROM servisi sürekli öldürüyorsa sonsuz öldür-devam ping-pongunun
  batarya boşaltmaması için 30 dakikalık pencerede en fazla 4 otomatik devam
  (`AppPrefs.claimRevive`) ve 8 saatten eski oturumun otomatik geri gelmemesi var.
- **Asıl kırıcı koddan gelmiyordu:** log'ta `keepAwake cpu=true wifi=true` görüldü;
  yani v1.5.4'ün wake/Wi-Fi kilitleri sorunsuz çalışıyordu ve öldüren taraf ROM'un
  güç yönetimi. Cihaz tarafında önerilen ayarlar: vivo Ayarlar → Uygulamalar →
  Sametify → **Otomatik başlatma**, Pil → **arka plan güç yönetimi** istisnası ve son
  kullanılanlar ekranında uygulama kartını kilitlemek.

### 1.5.4 neler değişti?

Ekran kapalıyken çalmanın ~30 sn'de kesilmesine karşı tek bir neden daha denemek yerine aynı
arıza zincirinin beş halkası birden kapatıldı; kalan belirsizlik ölçülebilir hale getirildi.

- **Kilitler artık servis tutuyor (`PlaybackWakeLocks`).** ExoPlayer'ın kendi wake/Wi-Fi
  kilitleri durum makinesine bağlı: `STATE_IDLE` ve `STATE_ENDED` anında **bırakılıyor** — yani
  tam şarkı geçişi ve sıradaki URL'nin çözümlendiği pencerede. Ekran kapalıyken radyo o
  pencerede uyuyorsa bir sonraki şarkı hiç başlamıyor. Yeni kilitler `playWhenReady` izini
  sürüyor: kullanıcı müzik istediği sürece (şarkılar arasında da) CPU ve Wi-Fi uyanık kalıyor,
  duraklatılınca bırakılıyor. En fazla 6 saat, sızıntıya karşı tavanlı.
- **Tampon 60-300 sn** (öncesi 30-120 sn) ve `prioritizeTimeOverSize`: ekran karardığında
  ağ birkaç saniye throttle edilse bile çalan ses zaten indirilmiş oluyor. ~5 dk audio ≈ birkaç MB.
- **HTTP okuma timeout'u 60 sn** (öncesi tam 30 sn) ve bağlanma 20 sn. "30 sn sonra kesiliyor"
  tarifinin `readTimeout = 30_000` ile birebir çakışması tesadüf değil: arka planda yavaşlayan
  bir okuma tam o anda ölü hata olarak raporlanıyordu.
- **Backoff'lu yeniden deneme:** eskiden parça başına **tek** deneme vardı ve yeniden
  çözümlenen URL daha birkaç saniye önce ölmüş olan ağdan alınıyordu. Artık oynatım hatasında
  4 deneme (2-4-8-16 sn bekleyerek), URL çözümleme hatasında 3 deneme, erken biten gövdede
  2 deneme; şarkı `STATE_READY` olduğunda bütçe sıfırlanıyor.
- **Sessiz durma bekçisi:** güç yönetimi hata üretmeden durdurabiliyor (odak sessizce
  geri alınıyor, oynatıcı `IDLE`'da kalıyor ya da `BUFFERING`'de takılıyor). 10 sn'de bir
  yoklama ve eşikler: 60 sn bastırılmış odaksa odak yeniden isteniyor, 20 sn `IDLE`'da
  bekliyorsa şarkı yeniden yükleniyor (kaynak yoksa controller'dan taze URL isteniyor), ~90 sn
  `BUFFERING`'de takılıysa URL tazelenip kaldığı yerden yükleniyor. Normal geçişler ve kısa
  tamponlamalar eşikleri aşamadığı için bekçi oynatıcıya dokunmuyor.
- **Telemetri:** `PlaybackLog` aynı `filesDir/crash.log` dosyasına yazıyor, yani **Ayarlar →
  Tanılama → Hata kaydını kopyala** artık çökme yığınıyla birlikte oynatıcı izini de veriyor:
  Media3 hata kodu (`errorCodeName`), çalan/buffered saniyeler, deneme numarası,
  `keepAwake cpu=… wifi=…`, `playWhenReady` değişim sebebi ve servis ölüm satırları.

### 1.5.3 neler değişti?

- **Asıl neden:** `p.setWakeMode(C.WAKE_MODE_NETWORK)` iki kilit tutar — CPU için partial wake
  lock ve **Wi-Fi kilidi**. Wi-Fi kilidi `WifiManager.createWifiLock()` ister ve bu çağrı
  `ACCESS_WIFI_STATE` izni olmadan `SecurityException` atıyor. İzin hiç bildirilmediği için
  kilit hiç alınmadı; üstelik çağrı `runCatching { … }` içindeydi, hata sessizce kayboldu.
  CPU uyanık kalıyordu ama ekran kapanınca Wi-Fi radyosu uyuyor, 30-120 sn'lik tampon boşalınca
  akış duruyordu.
- **Düzeltme:** `ACCESS_WIFI_STATE` manifest'e eklendi; wake modu artık `ExoPlayer.Builder`
  üzerinde, oynatıcı kurulurken ayarlanıyor (sessizce yutulan bir çağrı yok).
- **Neden şimdi hissedildi:** v1.5.1'de paket kimliği `com.sametify.app` oldu. Pil
  optimizasyonu istisnası ve OEM "arka planda çalıştırma" izinleri **pakete göre** tutulur;
  yeni paket bu istisnalarla doğmadı, yani Wi-Fi kilidine olan bağımlılık ilk kez görülebilir
  hale geldi. v1.5.2'nin kendi değişiklikleri (oturum komutları) akışı etkilemiyor.
- **Ayarlar:** "Pil optimizasyonu" satırı artık istisnanın gerçekten verilip verilmediğini
  gösteriyor. Yeni pakete geçtikten sonra bir kez onaylayın; Xiaomi/Samsung/Huawei gibi agresif
  tasarruf yapan ROM'larda bu, kod kadar belirleyici.

### 1.5.2 neler değişti?

- **Sonraki/önceki butonu artık gri değil:** kuyruk uygulamada, ExoPlayer'da ise tek medya
  tutulduğu için oynatıcı "sonraki parça yok" diyor; Media3 de kilit ekranı ve sistem medya
  kartındaki geçiş butonlarını devre dışı gösteriyordu. `QueueAwarePlayer` (bir
  `ForwardingPlayer`) bu komutları — `SEEK_TO_NEXT`, `SEEK_TO_NEXT_MEDIA_ITEM`, `SEEK_TO_PREVIOUS`,
  `SEEK_TO_PREVIOUS_MEDIA_ITEM` — standart olarak duyurup `PlayerHolder`'a yönlendiriyor.
- **Native uyumluluk:** geçişler artık bildirim intent'leriyle değil media oturumu komutuyla
  geldiği için kilit ekranı, hızlı ayarlar medya kartı, kulaklık/medya tuşları ve üçüncü taraf
  kumandalar aynı yolu kullanıyor. Kuyruğun gerçek durumuna göre `hasNext/hasPrevious` de
  raporlanıyor (son parçada ve tekrar kapalıyken buton gerçekten kapalı görünür).
- **Karıştır ve tekrar native:** oturum `SET_SHUFFLE_MODE` / `SET_REPEAT_MODE` komutlarını da
  destekliyor; sistem tam değeri gönderiyor, uygulama kendi durumunu koruyor. Yalnızca bu iki
  buton özel komut olarak kaldı — geçiş butonlarının tekrarı önlemek için özel karttan çıkarıldı.

### 1.5.1 neler değişti?

- **Kurulum çakışması giderildi:** GitHub Actions runner'ları her derlemede kendi debug
  anahtarını oluşturduğundan yayımlanan her APK farklı imzalıydı; Android de "aynı paket,
  farklı imza" diyerek güncellemeyi reddediyordu. Artık debug ve release derlemeleri tek bir
  **Sametify** anahtarıyla imzalanıyor (CI secret'ları / yerelde `keystore.properties`), CI
  imzayı doğrulamadan release açmıyor.
- **Yeni paket kimliği:** `com.sametify.app` (kod paketi `com.example.spoyt` olarak kaldı;
  yalnızca kurulan paket adı değişti). Uygulama adı, kısayol hedefi ve release asset adı
  Sametify ile uyumlu.
- Eski `com.example.spoyt` kurulumu yanına ayrı uygulama olarak kurulur; veriler uygulamaya
  özel alanda olduğu için taşınmaz — tek uygulama isterseniz eskisini kaldırın.

### 1.5.0 neler değişti?

- **Yeni kimlik:** uygulama adı **Sametify**; launcher ikonu mor→siyah degrade zemin üzerine
  beyaz/yeşil dalga formu logosuyla yenilendi (uydurma ad ve logo çekmecesinin içinden de
  aynı marka görünüyor).
- **Sanatçı adları:** NewPipe aramasında sonuç nesneleri yanlış tür kontrolüyle okunduğu için
  sanatçı ve süre **her sonuçta boş** geliyordu ("Bilinmeyen sanatçı"). Doğru tür (`StreamInfoItem`)
  okunuyor; kayıtlı geçmiş şarkılar için eksik sanatçılar arka planda tek tek onarılıyor.
- **Radyo / mix:** `RDAMVM<videoId>` karışık listesi NewPipe'ın playlist extractor'ı ile
  çözülüyor (25 parça, sanatçı + süre ile); yeterli sonuç yoksa "ilgili şarkılar", o da olmazsa
  sanatçının diğer parçaları deniyor. Hiçbir şey bulunamazsa kullanıcı uyarılıyor.
- **Bildirim ve kilit ekranı:** önceki/sonraki artık servise giden net intent'lerle çalışıyor;
  **Karıştır** ve **Tekrar** (kapalı → tüm liste → tek şarkı) butonları eklendi, ikonlar
  güncel duruma göre değişiyor. Oynatıcı ekranına da karıştır butonu kondu.
- **Çalma listesine ekle:** diyalog doğrudan mevcut listeleri listeliyor (yeni liste ekranına
  zorlamıyor), uzun listelerde kaydırılabiliyor.
- **Kütüphane:** ilk açılışta **Çalma Listeleri** sekmesi geliyor.
- **Öneriler:** en sık dinlenen şarkılardan yola çıkarak ana sayfada "Sana öneriler" satırı.

### 1.4.3 neler değişti?

- **Arka planda çalma düzeltmesi (ekran kapalıyken kesilme):** Ekran kapanınca Android
  kısa süre sonra Wi-Fi radyosunu uyutuyordu; 30–120 saniyelik tampon bitince akış
  duruyor ve şarkı ~1 dakikada kesiliyordu. Oynatıcı artık çalarken `WAKE_MODE_NETWORK`
  ile **CPU + Wi-Fi wake lock** tutuyor — ekran kapalıyken müzik kesintisiz devam eder.
- **Pil optimizasyonu kısayolu:** Ayarlar → İndirme bölümüne "Pil optimizasyonu" satırı
  eklendi. Xiaomi/Samsung/Huawei gibi agresif güç tasarrufu yapan cihazlarda arka plan
  çalmanın öldürülmemesi için Sametify'ı tek dokunuşla optimizasyon dışında bırakabilirsiniz.
- `WAKE_LOCK` ve `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` izinleri eklendi.

### 1.4.2 neler değişti?

- **Erken bitişin asıl nedeni giderildi:** YouTube akış URL'leri artık bir "istemciye"
  (Android/iOS YouTube uygulaması) ait olarak üretiliyor; bu URL'ler o istemcinin
  User-Agent'ı ile istenmediğinde CDN yalnızca ~1 dakikalık kırpılmış veri sunuyordu.
  Oynatma ve indirme istekleri artık URL'deki istemciye göre doğru User-Agent'ı
  kullanır (`c=ANDROID` → Android uygulaması, `c=IOS` → iOS uygulaması, aksi halde
  tarayıcı). NewPipe kitaplığının kendi istemci UA'sı da artık ezilmiyor.
- İndirilen dosyalarda da aynı düzeltme geçerli: indirmeler tam boyutta tamamlandığından
  çevrimdışı çalma bundan böyle etkilenmez.

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
adb install -r Sametify-1.5.8.apk
adb shell am start -n com.sametify.app/com.example.spoyt.MainActivity
```

> **v1.5.1'de paket kimliği değişti** (`com.example.spoyt` → `com.sametify.app`) ve tüm
> sürümler artık aynı **Sametify** anahtarıyla imzalanıyor. Eski sürüm (`com.example.spoyt`)
> telefonunuzda duruyorsa yeni APK yanına ayrı bir uygulama olarak kurulur; beğenilenler,
> listeler ve indirmeler uygulamanın özel alanında tutulduğu için taşınmaz. Tek uygulama
> istiyorsanız önce eskisini kaldırın: `adb uninstall com.example.spoyt`.

## Derleme

```powershell
$env:ANDROID_HOME     = "C:\Users\<kullanıcı>\AppData\Local\Android\Sdk"
$env:ANDROID_SDK_ROOT = "C:\Users\<kullanıcı>\AppData\Local\Android\Sdk"

./gradlew.bat :app:assembleDebug
```

Yayımlanan APK'lar `keystore.properties` (repoya girmez) veya CI'daki `SAMETIFY_KEYSTORE_*`
secret'ları ile gösterilen Sametify anahtarıyla imzalanır. Anahtar yoksa debug derlemesi
normal debug imzasıyla üretilir — yani güncel kurulamaz. Yerel imza için örnek dosya:

```
# keystore.properties (proje kökü, .gitignore'da)
storeFile=C:/Users/<kullanıcı>/sametify-keystore/sametify-release.keystore
storePassword=<parola>
keyAlias=sametify
keyPassword=<parola>
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
            │   ├── PlayerHolder.kt          # ExoPlayer + HTTP katmanı + kuyruk + durma bekçisi
            │   ├── PlaybackController.kt    # tembel akış çözümleme + backoff'lu yeniden deneme
            │   ├── PlaybackEvents.kt        # kullanıcıya gösterilecek mesajlar
            │   ├── PlaybackLog.kt           # crash.log'a oynatıcı izi (teşhis için)
            │   ├── PlaybackWakeLocks.kt     # playWhenReady boyunca CPU + Wi-Fi uyanık
            │   ├── QueueAwarePlayer.kt      # kuyruğu native Media3 komutu olarak duyurur
            │   ├── SpoytNotificationProvider.kt # medya bildirimi + karıştır/tekrar butonları
            │   └── MusicPlaybackService.kt  # MediaSessionService (bildirim + keep-awake)
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
- **Arka planda çalma kesilirse:** Ayarlar → Tanılama → **Hata kaydını kopyala** artık çökme
  yığınını ve oynatıcı izini birlikte verir (`errorCodeName`, buffered saniye, deneme sayısı,
  `keepAwake cpu=… wifi=…`, `playWhenReady` değişim sebebi, servis ölüm satırı). İzde
  `service destroyed` / `service stopped: task removed` varsa sorun uygulamada değil ROM'un
  süreç kill'indedir; OEM "otomatik başlatma / arka planda çalıştırma" izinleri verilmeli.

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
