# Takto

**Takto** je Android aplikacija za osobni raspored rada i evidenciju smjena. Projekt je pisan u **Kotlinu** i **Jetpack Composeu**, s tamnim premium sučeljem i velikim kalendarskim poljima prilagođenim brzom unosu rasporeda.

![Android CI](https://github.com/bren-wp/Takto/actions/workflows/android-ci.yml/badge.svg)

> Trenutna razvojna verzija: **0.1.0** (`versionCode 1`)

## Oznake rasporeda

- **D** — Dan
- **N** — Noć
- **GO** — Godišnji odmor
- **BO** — Bolovanje
- **PD** — Plaćeni dopust
- **Prazno polje** — Slobodan dan
- **Vlastiti unos** — proizvoljna oznaka ili tekst koji korisnik sam definira

## Glavne mogućnosti u 0.1.0

- veliki mjesečni kalendar s jasnim oznakama D / N / GO / BO / PD
- prazna kućica predstavlja slobodan dan
- pojedinačni i višestruki odabir dana
- bulk dodjela oznaka i bulk postavljanje slobodnih dana
- kopiranje i lijepljenje cijelog tjedna
- vlastite brze oznake i vlastiti uzorci smjena
- napomene uz datum
- evidencija početka, kraja i pauze smjene
- obračun radnih, prekovremenih, noćnih, vikend i nedjeljnih sati
- automatski ili ručni mjesečni fond sati
- zadana vremena smjena po oznaci
- početni pregled, statistika, uzorci i postavke
- Android dnevni podsjetnik i podsjetnik prije sljedeće smjene
- CSV uvoz/izvoz
- JSON sigurnosna kopija i vraćanje podataka
- iCalendar (`.ics`) izvoz za kalendarske aplikacije
- lokalno spremanje podataka bez `INTERNET` dozvole
- TalkBack opisi za kalendarske kućice

## Tehnologije

- Kotlin
- Jetpack Compose
- Material 3
- Android Gradle Plugin 8.9.1
- Kotlin 2.1.10
- Gradle Wrapper 8.11.1
- Java 17
- `minSdk 26`
- `targetSdk 35`
- `compileSdk 35`

## Build

Repozitorij sadrži potpuni Gradle Wrapper pa lokalni build ne ovisi o zasebno instaliranoj verziji Gradlea.

Linux/macOS:

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
```

Windows:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
```

GitHub Actions prije builda provjerava integritet Gradle Wrappera, zatim pokreće unit testove, Android lint i debug APK build. Debug APK se nakon uspješnog CI builda objavljuje kao GitHub Actions artifact.

## Privatnost

Takto trenutačno nema `INTERNET` permission. Raspored i postavke aplikacije ne šalju se na Takto poslužitelje. Korisnik može namjerno izvesti/podijeliti CSV, JSON ili ICS podatke kroz Android sustav. Androidova sigurnosna kopija uređaja može obuhvatiti lokalne postavke ako je sigurnosno kopiranje uključeno na uređaju.

## Struktura

```text
app/src/main/java/hr/takto/app/
├── data/         lokalna pohrana rasporeda i postavki
├── model/        modeli, izračuni i iCalendar izvoz
├── reminders/    dnevni i smjenski podsjetnici
└── ui/           Compose komponente, zasloni i tema
```

Dodatna dokumentacija nalazi se u `docs/`.

## Razvoj

Sav daljnji razvoj vodi se izravno u repozitoriju **`bren-wp/Takto`**. Verzije kreću od `0.1.0` i povećavaju se samo kada postoje stvarne promjene u funkcionalnosti, kvaliteti ili stabilnosti.

## Licenca

Pogledajte [LICENSE](LICENSE).
