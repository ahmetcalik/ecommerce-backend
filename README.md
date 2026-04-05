# E-Ticaret Backend Projesi

![Java](https://img.shields.io/badge/Java-21-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.4-green)
![Apache Kafka](https://img.shields.io/badge/Apache-Kafka-231F20?logo=apachekafka)
![Redis](https://img.shields.io/badge/Redis-6.2-DC382D?logo=redis)
![Zipkin](https://img.shields.io/badge/Zipkin-006699?logo=zipkin&logoColor=white)
![SonarQube Coverage](https://img.shields.io/badge/coverage-90%25+-brightgreen)
![SonarQube Quality Gate](https://img.shields.io/badge/quality%20gate-passed-green)

Bu proje, modern ve ölçeklenebilir bir e-ticaret platformu için geliştirilmiş, uçtan uca (end-to-end) ve "production-ready" prensipleriyle hazırlanmış bir backend sistemidir.

---

### 🏗️ Mimari Diyagram

```mermaid
graph TD
    subgraph Kullanıcı Etkileşimi
        A[İstemci / Frontend]
    end

    subgraph Sistem Mimarisi
        B(E-Ticaret API Gateway)
        C[PostgreSQL Veritabanı]
        D[Redis Önbellek]
        E[Apache Kafka]
        F[Zipkin Sunucusu]
    end

    subgraph Arka Plan İşlemleri
        G(Sipariş İşleyici Consumer)
    end

    A -->|REST API İstekleri| B
    B -->|Veri Okuma/Yazma| C
    B -->|Cache Okuma/Yazma| D
    B -->|Sipariş Olayı Gönderme| E
    E -->|Olayı Tüketme| G
    G -->|Siparişi İşleme ve DB Güncelleme| C
    B -->|Trace Bilgisi Gönderme| F
```

### 🚀 Öne Çıkan Özellikler ve İşlevsellik

- **🔐 Güvenlik ve Kullanıcı Yönetimi:** JWT tabanlı kimlik doğrulama, rol bazlı yetkilendirme ve `Bcrypt` ile güvenli şifreleme.
- **⚡️ Performans ve Önbellekleme (Caching):** **Redis** ile sık erişilen veriler için gelişmiş önbellekleme stratejisi.
- **🛒 Gelişmiş Sepet Yönetimi:** Anonim ve kayıtlı kullanıcılar için akıllı sepet birleştirme (cart merging).
- **🔄 Asenkron ve Uçtan Uca Sipariş Yaşam Döngüsü:** **Apache Kafka** ile dayanıklı (resilient) ve ölçeklenebilir asenkron sipariş işleme.
- **📦 Yönetim Paneli ve Operasyonlar:** Merkezi sipariş, iade ve platform yönetimi.

### ✨ Kod Kalitesi ve Test Stratejisi

Projemiz, en başından itibaren "production-ready" hedefiyle geliştirilmiştir. Bu doğrultuda kod kalitesi ve güvenilirlik en üst düzeyde tutulmuştur.

- **SonarQube Analizi:** Kod tabanımız, **SonarQube** ile sürekli olarak analiz edilmektedir. Bu analizler sonucunda:
  - **Sıfır Kritik Hata:** Projede `Medium` veya `High` seviyesinde hiçbir problem bulunmamaktadır.
  - **Güvenlik Zafiyeti Yok:** Bilinen hiçbir güvenlik açığı (vulnerability) veya güvenlik riski (severity) tespit edilmemiştir.
- **Yüksek Test Kapsamı:** Projemiz, **%90'ın üzerinde birim test (unit test) kapsamına** sahiptir. Bu, iş mantığının büyük bir bölümünün güvenilir ve beklendiği gibi çalıştığını garanti eder. Testler, JUnit 5 ve Mockito kullanılarak yazılmıştır.

### 🛠 Kullanılan Teknolojiler

- **Backend:** Java 21, Spring Boot 3.5.4, Spring Security, Spring for Apache Kafka, Spring Data JPA.
- **Veritabanı & Önbellekleme:** PostgreSQL, Redis.
- **Veritabanı Yönetimi:** Liquibase (Schema Migration).
- **Gözlemlenebilirlik (Observability):** Spring Boot Actuator, Micrometer, Brave, **Zipkin** (Distributed Tracing).
- **API Dokümantasyonu:** SpringDoc OpenAPI 2.8.9.
- **Test & Kod Kalitesi:** JUnit 5, Mockito, **SonarQube**.
- **Containerization & Build:** Docker, Docker Compose, Apache Maven 4.0.0.

### 📐 Mimari Yaklaşım ve En İyi Pratikler

- **Olay Güdümlü Akışlar (Kafka):** Sipariş yönetimi gibi kritik süreçler, Kafka kullanılarak asenkron olarak yönetilir. Veri tutarlılığını en üst düzeye çıkarmak için **Idempotent Producer** ve **`acks=all`** konfigürasyonları kullanılmıştır.
- **Önbellekleme Stratejisi (Redis):** Sık okunan veriler için `10 dakikalık` bir `time-to-live` (TTL) ile Redis önbelleklemesi uygulanmıştır.
- **Veritabanı Sürümleme (Liquibase):** Veritabanı şema değişiklikleri, kod tabanının bir parçası olarak yönetilir, bu da ortamlar arası tutarlılık sağlar.
- **Dağıtık Gözlem (Distributed Tracing):** Micrometer ve **Zipkin** entegrasyonu, mikroservis ortamlarında isteklerin izlenmesini ve performans darboğazlarının tespit edilmesini sağlar.

### ⚙️ Kurulum ve Çalıştırma

#### 1. Ön Koşullar
- Java 21 (JDK)
- Apache Maven
- Docker ve Docker Compose

#### 2. Yapılandırma (Configuration)
Uygulama, `application.properties` dosyasındaki değerleri ve ortam değişkenlerini kullanır. Projeyi çalıştırmadan önce aşağıdaki ortam değişkenlerini oluşturmanız gerekebilir:

- `DB_USER`: Veritabanı kullanıcı adı (varsayılan: `postgres`).
- `DB_PASSWORD`: Veritabanı şifresi.
- `JWT_SECRET_KEY`: JWT imzalamak için kullanılacak gizli anahtar.
- `SONAR_TOKEN`: SonarQube analizi için gerekli olan token.

#### 3. Çalıştırma Adımları
1. **Projeyi klonlayın:**
   ```bash
   git clone https://github.com/ahmetcalik/ecommerce-backend.git
   cd ecommerce-backend
   ```
2. **Gerekli servisleri Docker ile başlatın:**
   ```bash
   docker-compose up -d
   ```
3. **Veritabanını Başlangıç Verileriyle Doldurun (Opsiyonel):**
   Projeyi anlamlı verilerle hızlıca test etmek için, `database/initial_data.sql` script'ini PostgreSQL veritabanınızda çalıştırabilirsiniz. Bu, size test edebileceğiniz hazır ürünler, kategoriler ve kullanıcılar sunar.

4. **Uygulamayı Maven ile derleyin ve çalıştırın:**
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```
   Uygulama varsayılan olarak `8080` portunda çalışmaya başlayacaktır.

### 📚 API Dokümantasyonu ve Test

Bu projenin API'ını keşfetmek ve test etmek için iki farklı yol sunulmuştur:

#### 1. Swagger UI (Otomatik Dokümantasyon)
Uygulama çalıştırıldıktan sonra, tüm endpoint'leri, modelleri ve deneme imkanını sunan interaktif Swagger UI arayüzüne aşağıdaki adresten erişebilirsiniz:
[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

#### 2. Postman Koleksiyonu (Kullanıma Hazır Test Seti)
Tüm API isteklerini, örnek gövdeleri (body) ve ortam değişkenlerini içeren kullanıma hazır bir Postman koleksiyonu projeye dahil edilmiştir.

- **Dosya:** `postman/ecommerce-collection.json`
- **Kullanım:** Postman uygulamasında `Import` butonuna tıklayarak bu dosyayı içeri aktarın. Koleksiyon, tüm endpoint'leri sizin için hazır hale getirecektir.
