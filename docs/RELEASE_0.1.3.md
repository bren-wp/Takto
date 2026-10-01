# Takto 0.1.3 — brži dnevni unos i pametna pretraga

Takto 0.1.3 dodatno skraćuje najčešće dnevne radnje i poboljšava pronalaženje unosa u većim rasporedima.

## Najvažnije promjene

- nova stalna kartica **Danas** na Početnoj
- ako za danas nema unosa, do četiri najrelevantnije oznake mogu se dodati jednim dodirom
- ako današnji unos postoji, kartica odmah prikazuje oznaku, vrijeme i napomenu te otvara uređivanje tog datuma
- budući raspored ima jasno prazno stanje i akciju za planiranje sljedećeg dana
- sljedeći budući unos više se ne duplicira s današnjim unosom
- preporuke oznaka koriste učestalost i svježinu korištenja, s većom težinom za zadnjih 7 i 30 dana
- kod jednakog rezultata prednost ima novije korištena oznaka
- nova pametna pretraga ignorira dijakritičke znakove
- podržani su upiti **danas**, **sutra** i **jučer**
- podržani su ISO i hrvatski datumi poput **02.10.2026.**
- filtri pretrage: **Sve**, **Danas**, **Buduće**, **S vremenom**, **S napomenom**
- točne kratice i nazivi rangiraju se ispred slabijih podudaranja
- višerječni upiti mogu kombinirati naziv i napomenu
- dodani unit testovi za pretragu i recency-aware rangiranje preporuka
- release workflow automatski objavljuje ovu verziju nakon uspješnog mergea u `main`, uz ponovnu provjeru testova, linta, APK-a, AAB-a i SHA-256 datoteke

## Kompatibilnost

- postojeći rasporedi i vlastite oznake ostaju kompatibilni
- postojeći backup format ostaje podržan
- nema promjene sheme koja briše ili prepisuje korisničke podatke
- nema novih mrežnih dopuštenja

## Toolchain

- Kotlin 2.4.20
- Android Gradle Plugin 9.4.1
- Gradle 9.8.0
- Jetpack Compose BOM 2026.09.00
- compileSdk 37.1
- targetSdk 37
- Java 17

## Artefakti

Release workflow priprema:

- `Takto-0.1.3.apk`
- `Takto-0.1.3-release-unsigned.apk`
- `Takto-0.1.3-release-unsigned.aab`
- `SHA256SUMS.txt`

Produkcijski Google Play ključ nije pohranjen u repozitoriju.
