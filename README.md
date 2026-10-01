# Takto

<p align="center">
  <img src="docs/assets/takto-logo.svg" alt="Takto — Dodirni. Označi. Radi." width="760">
</p>

<p align="center">
  <img src="docs/assets/takto-app-icon.svg" alt="Takto ikona aplikacije" width="132">
</p>

> **Dodirni. Označi. Radi.**  
> Moderan Android planer rada i rasporeda za jasan pregled mjeseca, radnih sati, obveza i odsutnosti.

![Version](https://img.shields.io/badge/verzija-0.1.1-2488FF)
![Android](https://img.shields.io/badge/Android-8.0%2B-13D7A0)
![Target](https://img.shields.io/badge/target-Android%2017-8B46F6)
![Kotlin](https://img.shields.io/badge/Kotlin-2.4.20-7F52FF)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-2026.09.00-1DE1E8)
![License](https://img.shields.io/badge/license-MIT-0F172A)

Takto je aplikacija za svakoga tko želi svoj radni mjesec razumjeti **na prvi pogled**. Velike kalendarske ćelije, vlastite oznake, radni sati, fond sati, podsjetnici, uzorci i statistika spojeni su u čisto sučelje koje radi u tamnom, svijetlom ili sistemskom načinu prikaza.

<p align="center">
  <img src="docs/assets/takto-ui-preview.svg" alt="Takto pregled kalendara, fonda sati i statistike" width="100%">
</p>

## Zašto Takto

**Planiraj bez tablica i papira.** Dodirni datum i odaberi oznaku ili upiši vlastitu. Radni dan, obveza, odsutnost, edukacija, teren ili bilo koji drugi unos sprema se u nekoliko sekundi.

**Vidi cijeli mjesec odjednom.** Kalendar koristi velike obojene ćelije i dosljedan sustav boja: plava, ljubičasta, zelena, jantarna i crvena. Prazna ćelija znači slobodan dan.

**Prati stvarno radno vrijeme.** Za svaki radni unos moguće je spremiti početak, kraj i pauzu. Takto računa trajanje, redovne i prekovremene sate, noćni rad, vikend i nedjelju.

**Prilagodi aplikaciju svom poslu.** Oznake, boje, vlastite brze oznake, predlošci radnog vremena i ponavljajući uzorci mogu se prilagoditi korisniku.

**Podaci ostaju lokalni.** Takto ne deklarira INTERNET dopuštenje. Raspored se sprema na uređaju, a izvoz se pokreće samo kada korisnik to zatraži.

---

## Ključne mogućnosti

### Kalendar koji radi jednim dodirom

- veliki mjesečni pregled 6 × 7
- D — Dan
- N — Noć
- GO — Godišnji odmor
- BO — Bolovanje
- PD — Plaćeni dopust
- vlastiti tekst, naziv i boja
- dodatne kratice poput **J** i **SD** iz stvarnog referentnog rasporeda
- sve nepoznate kratice iz CSV uvoza automatski se čuvaju kao vlastite brze oznake
- napomena za svaki datum
- današnji datum i aktivni datum jasno istaknuti
- TalkBack opis datuma, smjene, napomene i radnog vremena

### Brzo uređivanje rasporeda

- navigacija do mjeseca **100 godina unatrag i 100 godina unaprijed**
- nema automatskog brisanja starih ni budućih rasporeda
- append-only lokalna arhiva svake promjene rasporeda
- višestruki odabir više datuma
- prečaci **Cijeli mjesec** i **Pon–pet**
- bulk dodjela smjena
- bulk postavljanje radnog vremena
- kopiranje i lijepljenje cijelog tjedna
- Undo / Vrati zadnju promjenu
- pretraga po oznaci, nazivu, napomeni i datumu

### Radni sati i fond

- mjesečni fond sati
- redovni odrađeni sati
- prekovremeni sati prema mjesečnom fondu
- dodatni dnevni obračun prekovremenog rada
- početak i kraj smjene
- pauza u minutama
- smjene preko ponoći
- standardni dnevni fond
- automatski mjesečni fond pon–pet
- ručni fond po mjesecu
- prekovremeni sati
- noćni rad 22:00–06:00
- vikend i nedjelja
- prosječno trajanje evidentirane smjene

### Uzorci

Takto može spremiti ponavljajuće cikluse, primjerice:

```text
D, D, N, N, -, -, -, -
```

`-` znači slobodan dan. Uzorci podržavaju ugrađene i vlastite oznake te se mogu primijeniti na veći raspon dana uz izbor hoće li postojeći unosi biti sačuvani ili prepisani.

### Radni profil

U postavkama se može spremiti osobni radni profil koji ostaje na uređaju:

- ime i prezime
- sektor
- djelatnost / industrija
- vrsta ustanove
- naziv ustanove ili poslodavca
- radno mjesto / pozicija
- prijedlozi za javni sektor, državni sektor, javne i državne ustanove, zdravstvo, obrazovanje, policiju, pravosuđe, vatrogastvo, komunalne službe i druga područja
- potpuno slobodan unos za ustanove i radna mjesta koja nisu na popisu

Početna stranica koristi ime iz profila za osobni pozdrav, dok backup čuva i profil zajedno s rasporedom i postavkama.

### Podsjetnici

- dnevni podsjetnik rasporeda
- poseban podsjetnik prije početka smjene
- podesivi odmak prije smjene
- ponovno zakazivanje nakon restarta uređaja, promjene vremena ili vremenske zone
- dodir obavijesti vodi izravno na konkretan datum

### Statistika

- ukupne, dnevne i noćne smjene
- GO / BO / PD odvojeno
- radni sati
- fond i razlika
- prekovremeni sati
- noćni, vikend i nedjeljni sati
- mjesečni i godišnji pregled
- raspodjela po vlastitim oznakama

### Uvoz, izvoz i sigurnosna kopija

- CSV izvoz
- CSV uvoz sa zarezom ili točka-zarezom
- hrvatski i ISO datumi
- JSON sigurnosna kopija i povrat
- iCalendar `.ics` izvoz
- dijeljenje rasporeda kroz Android Share
- očuvanje boja, napomena i radnog vremena

---

## Vizualni identitet

Takto koristi prepoznatljiv premium sustav boja s poboljšanim kontrastom. Korisnik može odabrati **tamni**, **svijetli** ili **sistemski** izgled:

| Element | Boja |
| --- | --- |
| Tamna pozadina | `#0D1726` |
| Tamna površina | `#152238` |
| D / primarna plava | `#2488FF` |
| N / ljubičasta | `#8B46F6` |
| GO / zelena | `#13D7A0` |
| BO / jantarna | `#FFB21D` |
| PD / crvena | `#FF4B55` |
| Cijan akcent | `#1DE1E8` |

Dizajn nije statična slika. Svi glavni elementi iz referentnih vizuala implementirani su stvarnim Jetpack Compose komponentama: onboarding, početni pregled, veliki kalendar, brzi unos, statistika, uzorci, personalizacija i donja navigacija.

---

## Tehnologija

Takto 0.1.1 koristi aktualni stabilni Android toolchain:

- **Kotlin 2.4.20**
- **Android Gradle Plugin 9.4.1**
- **Gradle 9.8.0**
- **Jetpack Compose BOM 2026.09.00**
- **Material 3**
- **AndroidX Core 1.19.1**
- **Activity Compose 1.13.0**
- **Lifecycle 2.11.0**
- **compileSdk 37.1 — Android 17 SDK**
- **targetSdk 37 — Android 17**
- **minSdk 26 — Android 8.0**
- **Java 17**

Aplikacija je pisana u Kotlinu i Jetpack Composeu bez WebView sloja i bez INTERNET dopuštenja.

---

## Build, APK i AAB

GitHub Actions pri svakom pull requestu prema `main` izvodi:

1. `testDebugUnitTest`
2. `lintDebug`
3. `lintRelease`
4. `assembleDebug`
5. `assembleRelease`
6. `bundleRelease`
7. provjeru da APK i AAB datoteke stvarno postoje i nisu prazne
8. SHA-256 izračun za sve build artefakte

Nakon uspješnog workflowa dostupni su Actions artefakti:

- **Takto-0.1.1-debug-apk** — instalabilni debug APK
- **Takto-0.1.1-release-apk-unsigned** — optimizirani release APK bez produkcijskog potpisa
- **Takto-0.1.1-release-aab-unsigned** — release Android App Bundle
- **Takto-0.1.1-SHA256** — checksum datoteka

> Za objavu na Google Playu release AAB mora biti potpisan trajnim produkcijskim ključem. Ključ se namjerno ne pohranjuje u repozitorij.

## Privatnost i sigurnost

- nema INTERNET dopuštenja
- raspored i profil pohranjuju se lokalno
- svaka promjena rasporeda ulazi u append-only lokalnu arhivu
- stari i budući rasporedi ne brišu se automatski
- notification permission traži se samo na Androidu 13+
- datoteke se izvoze kroz Androidov sustav za datoteke/dijeljenje
- uvoz ima ograničenja veličine i broja redaka
- JSON backup koristi verzioniranu shemu
- neispravni zapisi ne smiju srušiti aplikaciju
- produkcijski ključ za potpisivanje nikad ne smije biti u Git repozitoriju

---

## Verzija 0.1.1

Verzija **0.1.1** donosi svijetli način rada, poboljšanu tamnu temu, jasniji onboarding za sve tipove korisnika, dorađenu početnu stranicu i statistiku, reorganizirane postavke, novi launcher icon te Brendigo podatke i podršku unutar aplikacije.

## Licenca

MIT — vidi `LICENSE`.
