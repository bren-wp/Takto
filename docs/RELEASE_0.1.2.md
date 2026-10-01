# Takto 0.1.2 — adaptivni raspored i statistika

Takto 0.1.2 nastavlja UI/UX i stabilizacijski razvoj s fokusom na to da aplikacija stvarno odgovara različitim vrstama korisnika, a ne samo klasičnom dnevnom/noćnom smjenskom radu.

## Najvažnije promjene

- tamni način ostaje primarni Takto izgled
- svijetli i sistemski način ostaju dostupni u Postavkama
- brzi odabir oznaka više nije fiksno vezan uz D/N/GO/BO/PD
- Takto prioritizira oznake koje je korisnik stvarno koristio u posljednjih 90 dana
- nove vlastite oznake imaju prednost pred nekorištenim ugrađenim oznakama
- isti adaptivni odabir koristi se za jedan dan i višestruko uređivanje
- statistika se gradi iz svih stvarnih oznaka korisnika
- graf trendova oznaka broji sve unose, ne samo D i N
- početna stranica prikazuje dane bez unosa umjesto specifične metrike odsutnosti
- obavijesti i kanali koriste općenitiji izraz „rad”
- prazni kalendarski dani opisani su kao dani bez unosa
- poboljšana TalkBack semantika brzih oznaka
- duge vlastite kratice bolje se skaliraju u statistici
- dodani unit testovi za zadani i spremljeni način prikaza

## Kompatibilnost

- postojeći rasporedi ostaju kompatibilni
- postojeće vlastite oznake ostaju kompatibilne
- postojeći backup format ostaje podržan
- nema migracije koja briše ili prepisuje korisničke podatke

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

- `Takto-0.1.2.apk`
- `Takto-0.1.2-release-unsigned.apk`
- `Takto-0.1.2-release-unsigned.aab`
- `SHA256SUMS.txt`

Produkcijski Google Play ključ nije pohranjen u repozitoriju.
