# ✈ AirportRouter — Havaalanı Bagaj Yönetim Sistemi

Bu proje, bir havaalanındaki bagaj akışının baştan sona nasıl yönetildiğini modellediğimiz bir masaüstü uygulaması. Bagajın check-in'de sisteme girmesinden güvenlik taramasına, uçağa yüklenmesinden teslim edilmesine kadar geçen süreci; graf, yığın, kuyruk ve öncelik kuyruğu gibi temel veri yapılarını gerçek bir senaryo üstünde kullanarak hayata geçirdik.

Amacım sadece çalışan bir CRUD uygulaması yazmak değildi; hangi problemin hangi veri yapısıyla çözüldüğünü net biçimde göstermekti. Bu yüzden her modül, arkasındaki veri yapısı açıkça görülecek şekilde kurgulandı.

Java 21 + JavaFX ile yazdık, verileri MySQL'de tutuyoruz.

---

## Neler yapabiliyor?

| Modül | Ne işe yarıyor | Arkasındaki veri yapısı |
|---|---|---|
| **Dashboard** | Anlık uçuş, bagaj ve doluluk özetleri | — |
| **Uçuş Takvimi** | Uçuş planlama, biniş/kalkış/iniş/iptal işlemleri | `PriorityQueue` (min-heap, kalkış saatine göre) |
| **Bagaj Yönetimi** | Check-in, durum takibi, uçuşa/yolcuya göre sorgu | `HashMap` (O(1) erişim + uçuş/yolcu indeksleri) |
| **Güvenlik Denetimi** | Tehlikeli madde tespiti, güvenlik havuzu, IATA kategorileri | Filtreli havuz + kategori bazlı gruplama |
| **Rota Haritası** | En az aktarmalı ve en kısa mesafeli rota, tüm alternatif rotalar | Yönlü graf — **BFS**, **DFS**, **Dijkstra** |
| **Yükleme Simülasyonu** | Ağırlığa göre yükleme/boşaltma sırası (ağır bagaj alta) | `Stack` (LIFO) |
| **Kapasite Yönetimi** | Uçuş kapasitesi dolduğunda bagajın kuyruğa alınması | `Queue` (FIFO bekleme kuyruğu) |

Öncelik mantığı da işin içinde: VIP → Business → Economy sırasıyla bagajlar önceliklendiriliyor.

---

## Kullandığımız teknolojiler

- **Java 21**
- **JavaFX 21.0.2** (FXML tabanlı arayüz, koyu tema)
- **MySQL 8** + JDBC (`mysql-connector-j`)
- **Maven** (Maven Wrapper dahil — ayrıca Maven kurmanıza gerek yok)
- **TilesFX** ve **AnimateFX** (dashboard bileşenleri ve geçiş animasyonları)
- **Lombok**, **JUnit 5**

---

## Kurulum

### Gereksinimler

- JDK 21 veya üstü
- MySQL 8 (çalışır durumda)

### 1. Projeyi klonlayın

```bash
git clone https://github.com/<kullanici-adiniz>/AirportBagRoutingSystem.git
```

### 2. Veritabanını oluşturun

Şema ve örnek veriler `database/schema.sql` içinde. Tek komutla kuruluyor:

```bash
mysql -u root -p < database/schema.sql
```

Bu script `airport_db` veritabanını, dört tabloyu (`airports`, `routes`, `flights`, `baggage`) ve uygulamayı hemen deneyebilmeniz için örnek uçuş/bagaj verilerini oluşturur.

### 3. Bağlantı ayarlarını yapın

`db.properties` dosyası güvenlik gerekçesiyle repoya dahil edilmiyor. Örnek dosyadan kendinize bir kopya çıkarın:

```bash
cp src/main/resources/db.properties.example src/main/resources/db.properties
```

Sonra kendi MySQL kullanıcı adı ve şifrenizi yazın:

```properties
db.url=jdbc:mysql://localhost:3306/airport_db?useSSL=false&serverTimezone=UTC
db.user=root
db.password=sizin_sifreniz
```

### 4. Çalıştırın

```bash
./mvnw clean javafx:run
```

Windows kullanıyorsanız:

```bash
mvnw.cmd clean javafx:run
```

---

## Proje yapısı

```
AirportBagRoutingSystem/
├── database/
│   └── schema.sql                  # MySQL şeması + örnek veriler
├── src/main/java/com/airport/
│   ├── Launcher.java               # JAR üzerinden çalıştırma girişi
│   ├── MainApp.java                # JavaFX uygulama giriş noktası
│   ├── controller/
│   │   └── AirportController.java  # Tüm servisleri toplayan facade katmanı
│   ├── datastructures/
│   │   ├── AirportGraph.java       # Yönlü graf — BFS / DFS / Dijkstra
│   │   ├── BaggageStack.java       # Ağırlığa göre yükleme yığını (LIFO)
│   │   └── WaitingQueue.java       # Kapasite aşımı bekleme kuyruğu (FIFO)
│   ├── db/
│   │   ├── DatabaseManager.java    # Bağlantı yönetimi
│   │   ├── BaggageDAO.java
│   │   ├── FlightDAO.java
│   │   └── RouteDAO.java
│   ├── model/
│   │   ├── Baggage.java
│   │   ├── Flight.java
│   │   ├── Passenger.java
│   │   └── enums/                  # BaggageStatus, PassengerClass, DangerousGoodsCategory
│   ├── service/                    # İş mantığı katmanı
│   │   ├── RoutingService.java
│   │   ├── FlightScheduleService.java
│   │   ├── BaggageTrackingService.java
│   │   ├── SecurityService.java
│   │   ├── PriorityLoadingService.java
│   │   ├── WeightLoadingService.java
│   │   └── CapacityService.java
│   └── ui/                         # JavaFX controller'ları ve yardımcı sınıflar
└── src/main/resources/
    ├── com/airport/fxml/           # Arayüz tanımları
    ├── com/airport/css/            # Koyu tema
    └── db.properties.example       # Bağlantı ayarı şablonu
```

Mimariyi katmanlı tuttuk: **UI → Controller (facade) → Service → DAO → MySQL**. Arayüz hiçbir zaman doğrudan veritabanıyla konuşmuyor, veri yapıları da bellekte tutulup veritabanıyla senkron çalışıyor.

---

## Emeği geçenler

Bu projeyi birlikte geliştirdik:

- **Emre Turan**
- **Erhan Öztürk**
- **Kaan Burak Karacay**

---

## Not

Proje eğitim amaçlı geliştirilmiştir. Soru, öneri veya katkı için issue açabilirsiniz.
