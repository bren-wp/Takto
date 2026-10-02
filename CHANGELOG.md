# Changelog

## 0.1.4

Pouzdanija dugoročna pohrana, automatski oporavak i zaštita velikih rasporeda.

### Poboljšano

- raspored više ne ovisi o jednom velikom JSON stringu u SharedPreferences
- uvedena je atomska glavna snimka `takto_schedule_current.json`
- uvedena je pričuvna snimka `takto_schedule_recovery.json`
- prije zamjene glavne snimke prethodno potvrđeno stanje kopira se streaming načinom u recovery datoteku
- glavna snimka koristi Android `AtomicFile` kako prekid procesa tijekom pisanja ne bi ostavio napola zapisanu datoteku
- postojeće instalacije automatski migriraju stari `entries_json` raspored u novi format
- legacy SharedPreferences raspored briše se tek nakon uspješnog zapisa glavne i recovery snimke
- append-only revizijska arhiva ostaje dodatni sloj zaštite
- snimke pamte broj već ugrađenih revizija pa se pri pokretanju obrađuju samo novije promjene
- ako je proces prekinut nakon zapisa revizije, ali prije završetka snimke, zadnja promjena automatski se vraća pri sljedećem pokretanju
- oporavak koristi `before/after` provjeru i ne prepisuje divergentno novije lokalno stanje starijom revizijom
- provjerava se deklarirani broj zapisa u snimci prije prihvaćanja datoteke
- veličina lokalne snimke strogo je ograničena prije učitavanja
- Android cloud backup i device transfer uključuju glavnu snimku, recovery snimku i revizijsku arhivu
- Postavke sada jasno prikazuju zaštitu lokalnih podataka i broj rasporednih unosa
- uklonjene su preostale formulacije koje nepotrebno pretpostavljaju noćne smjene
- dodani unit testovi za nedestruktivno rješavanje revizijskih konflikata
- broj revizija i duljina arhive spremaju se kao metadata cache pa veliki journal ne mora biti ponovno potpuno prebrojan pri svakom pokretanju

### Verzija

- `versionName`: `0.1.4`
- `versionCode`: `5`

## 0.1.3

Brži dnevni unos, pametnija pretraga i preciznije adaptivne preporuke.

### Poboljšano

- Početna sada uvijek ima jasnu karticu **Danas**
- kada današnji unos ne postoji, do četiri najrelevantnije oznake mogu se dodati jednim dodirom bez otvaranja kalendara
- kada današnji unos postoji, kartica prikazuje oznaku, radno vrijeme i napomenu te vodi izravno na uređivanje tog datuma
- budući raspored ima jasno prazno stanje i akciju **Planiraj sljedeći dan**
- sljedeći budući unos više se ne duplicira s današnjim unosom na Početnoj
- preporuke oznaka koriste ponderiranu učestalost i svježinu korištenja: zadnjih 7 dana imaju najveću težinu, zatim 30 i 90 dana
- kod jednakog rezultata prednost ima novije korištena oznaka
- kalendarska pretraga je izdvojena u testiranu logiku i ignorira dijakritičke znakove
- pretraga podržava izraze **danas**, **sutra** i **jučer**
- podržani su ISO i uobičajeni hrvatski datumi, uključujući `02.10.2026.`
- dodani filtri **Danas**, **Buduće**, **S vremenom** i **S napomenom**
- rezultati pretrage rangiraju točne kratice i nazive ispred slabijih podudaranja
- višerječne pretrage mogu kombinirati naziv i napomenu, npr. `teren rijeka`
- dodani unit testovi za pametnu pretragu i recency-aware rangiranje oznaka
- release workflow sada na novoj verziji u `main` automatski gradi, provjerava i objavljuje odgovarajući `vX.Y.Z` GitHub Release bez dupliciranja postojećih izdanja

### Toolchain

- Kotlin 2.4.20
- Android Gradle Plugin 9.4.1
- Gradle 9.8.0
- Jetpack Compose BOM 2026.09.00
- compileSdk 37.1
- targetSdk 37

### Verzija

- `versionName`: `0.1.3`
- `versionCode`: `4`

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
- vizualni graditelj uzoraka koristi najrelevantnije korisničke oznake umjesto unaprijed nametnutih D/N predložaka
- ugrađeni prijedlozi uzoraka dinamički se grade iz najčešće korištenih oznaka
- CSV parser obrađuje retke sekvencijalno i izbjegava velike privremene kolekcije
- CSV i backup datoteke čitaju se uz strogo ograničenje veličine prije potpune alokacije sadržaja
- izvoz trajne arhive zapisuje podatke izravno u odabranu datoteku bez učitavanja cijele arhive u memoriju
- Android Back iz Kalendar/Statistika/Uzorci/Više vraća korisnika na Početnu prije izlaska iz aplikacije
- notification ikona pojednostavljena je na prepoznatljivi Takto T kako bi bila čitljiva u Android statusnoj traci

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
