# Changelog

## 0.1.2

Adaptivni UI/UX, pristupačnost i daljnja generalizacija Takto rasporeda.

### Poboljšano

- tamni način je primarni zadani Takto izgled; svijetli i sistemski način ostaju dostupni
- brzi odabir oznaka u kalendaru više nije fiksno vezan uz D/N/GO/BO/PD
- Takto rangira spremljene oznake prema korištenju u posljednjih 90 dana
- vlastite oznake bez prethodne uporabe imaju prednost pred nekorištenim ugrađenim oznakama
- pojedinačno i višestruko uređivanje koriste isti adaptivni odabir oznaka
- statistika se dinamički gradi iz svih stvarnih oznaka korisnika
- trendovi oznaka sada broje sve unose, a ne samo D i N
- početni sažetak više ne koristi specifičnu metriku odsutnosti nego broj dana bez unosa
- podsjetnici i notification channeli koriste općenitiji izraz „rad” umjesto pretpostavke smjenskog rada
- prazni dani u kalendaru i uzorcima opisani su kao dani bez unosa
- poboljšane TalkBack semantike za tipke oznaka
- duge vlastite kratice bolje se skaliraju u statistici
- dodani unit testovi za spremanje i zadani odabir teme

### Toolchain

- Kotlin 2.4.20
- Android Gradle Plugin 9.4.1
- Gradle 9.8.0
- Jetpack Compose BOM 2026.09.00
- compileSdk 37.1
- targetSdk 37

### Verzija

- `versionName`: `0.1.2`
- `versionCode`: `3`

## 0.1.1

UI/UX, stabilnost i branding izdanje.

### Poboljšano

- tamna tema je svjetlija i čitljivija uz veći kontrast teksta, kartica i obruba
- uveden svijetli način i opcija praćenja izgleda sustava
- onboarding više nije fokusiran samo na dnevne i noćne smjene nego na bilo koji oblik rada i rasporeda
- početna stranica koristi općenitije metrike: upisani dani, evidentirani sati i odsutnosti
- pozdrav se mijenja prema dobu dana: jutro, dan, večer i noć
- mjesečni fond na početnoj prikazuje redovne i prekovremene sate
- statistika koristi općenitiji jezik i bolje podržava vlastite oznake
- postavke su podijeljene u jasne cjeline
- dodani Brendigo, brendigo.com i info@brendigo.com u podršku
- redizajnirana adaptivna ikona aplikacije i dodana monochrome/themed ikona
- poboljšana podrška za svijetli i tamni prikaz kroz zajedničke Compose komponente
- zabranjen cleartext mrežni promet
- targetSdk podignut na Android 17 / API 37
- očuvana kompatibilnost sa spremljenim rasporedima, profilom i arhivom

### Toolchain

- Kotlin 2.4.20
- Android Gradle Plugin 9.4.1
- Gradle 9.8.0
- Jetpack Compose BOM 2026.09.00
- AndroidX Core 1.19.1
- Activity Compose 1.13.0
- Lifecycle 2.11.0
- compileSdk 37.1
- targetSdk 37

### Verzija

- `versionName`: `0.1.1`
- `versionCode`: `2`

## 0.1.0

Početno izdanje Takto aplikacije u repozitoriju `bren-wp/Takto`.

### Uključeno

- premium tamni Android UI prema Takto referentnim vizualima
- onboarding, Početna, Kalendar, Statistika, Uzorci i Postavke
- D / N / GO / BO / PD i vlastiti unosi
- personalizacija oznaka i boja
- višestruki odabir, bulk uređivanje i kopiranje tjedna
- napomene, radno vrijeme, pauze, fond sati i prekovremeni rad
- statistike i vlastiti uzorci
- dnevni i smjenski podsjetnici
- CSV, JSON backup i iCalendar izvoz
- trajna append-only arhiva svih promjena rasporeda
- praktična navigacija 100 godina unatrag i 100 godina unaprijed
- radni profil: ime i prezime, sektor, djelatnost, ustanova i pozicija
- J i SD brze oznake iz referentnog rasporeda te automatsko čuvanje nepoznatih uvezenih kratica
- mjesečni fond, redovni sati i prekovremeni sati prema fondu
- lokalno spremanje bez INTERNET dopuštenja
- unit testovi i GitHub Actions provjera bez vlastitih Secrets varijabli

### Verzija

- `versionName`: `0.1.0`
- `versionCode`: `1`
