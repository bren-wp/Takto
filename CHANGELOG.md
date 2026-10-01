# Changelog

Sve značajne promjene projekta Takto bilježe se u ovoj datoteci.

## [0.1.0] - 2026-10-01

Početna razvojna verzija projekta na GitHub repozitoriju `bren-wp/Takto`.

### Dodano

- Kotlin + Jetpack Compose Android projekt i Takto vizualni sustav.
- Mjesečni kalendar s velikim poljima za D, N, GO, BO i PD.
- Prazno polje kao slobodan dan i proizvoljan vlastiti unos.
- Vlastite brze oznake, vlastite boje i vlastiti uzorci smjena.
- Višestruki odabir dana, bulk uređivanje i kopiranje/lijepljenje tjedna.
- Napomene te evidencija početka, kraja i pauze smjene.
- Obračun radnih, prekovremenih, noćnih, vikend i nedjeljnih sati.
- Automatski i ručni mjesečni fond sati.
- Zadana vremena smjene po oznaci.
- Početni pregled i statistika rada.
- Dnevni podsjetnik i podsjetnik prije sljedeće smjene.
- CSV uvoz/izvoz, JSON backup/restore i iCalendar (`.ics`) izvoz.
- TalkBack opisi kalendarskih polja.
- Unit testovi za logiku rasporeda, datume i iCalendar izvoz.
- GitHub Actions CI za testove, Android lint i debug APK build.
- Dependabot konfiguracija za Gradle i GitHub Actions.

### Važno

- Aplikacijska verzija je resetirana na **0.1.0 / versionCode 1** kao početna javna razvojna linija ovog repozitorija.
- Interna JSON backup schema verzija nije vezana uz verziju aplikacije i ostaje kompatibilna s već implementiranim formatima.
