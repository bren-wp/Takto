# Takto

Takto je Android aplikacija za planiranje smjenskog rada, razvijena u Kotlinu i Jetpack Composeu prema dostavljenom Takto vizualnom identitetu: tamna premium podloga, plavo–cijan–ljubičasti akcenti, velike kalendarske ćelije i brzo označavanje smjena.

**Početna verzija:** `0.1.0`  
**Android versionCode:** `1`  
**Minimalni Android:** 8.0 / API 26  
**Target/compile SDK:** 35

## Funkcionalnosti

- Početna, Kalendar, Statistika, Uzorci i Postavke
- D / N / GO / BO / PD i vlastite oznake
- veliki mjesečni kalendar s brzim unosom jednim dodirom
- bilješke, radno vrijeme, pauze, fond sati i prekovremeni rad
- višestruki odabir i bulk uređivanje
- kopiranje/lijepljenje tjedna i ponavljajući uzorci
- vlastite brze oznake, nazivi i boje
- mjesečne i godišnje statistike
- dnevni podsjetnik i podsjetnik prije smjene
- CSV uvoz/izvoz, JSON sigurnosna kopija i iCalendar (.ics) izvoz
- lokalno spremanje podataka; aplikacija ne deklarira INTERNET dopuštenje
- TalkBack opisi za ključne kalendarske interakcije

## Oznake

| Oznaka | Značenje |
| --- | --- |
| D | Dan |
| N | Noć |
| GO | Godišnji odmor |
| BO | Bolovanje |
| PD | Plaćeni dopust |
| prazno | Slobodan dan |

## Tehnologija

- Kotlin 2.1.10
- Jetpack Compose + Material 3
- Android Gradle Plugin 8.9.1
- Gradle 8.11.1
- Java 17

## Build i provjera

GitHub Actions na svakom PR-u prema `main` pokreće:

1. `testDebugUnitTest`
2. `lintDebug`
3. `assembleDebug`
4. `assembleRelease` s R8/minify provjerom

Workflow ne zahtijeva vlastite GitHub Secrets. Debug APK i nepotpisani release APK spremaju se kao Actions artefakti.

Za lokalni build u Android Studiju instaliraj Android SDK 35 i koristi Java 17. Ako imaš Gradle 8.11.1 u PATH-u:

```bash
gradle testDebugUnitTest lintDebug assembleDebug assembleRelease
```

## Dizajn

UI nije statični prikaz referentnih slika. Ekrani su implementirani stvarnim Compose komponentama prema dostavljenom dizajnu: Takto gradient logo, tamne staklaste površine, velike obojene oznake smjena, zaobljene kartice i bottom navigation.

## Privatnost

Podaci o rasporedu ostaju lokalno u aplikaciji. Ručni CSV/JSON/iCalendar izvoz pokreće korisnik preko Android sustava za datoteke/dijeljenje.

## Verzije

Razvoj ovog repozitorija počinje s verzijom **0.1.0**. Sljedeće verzije moraju sadržavati stvarne funkcionalne promjene i proći CI prije spajanja u `main`.
