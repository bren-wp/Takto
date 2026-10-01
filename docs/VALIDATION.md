# Takto 0.1.0 — validacija

Ovaj dokument razlikuje lokalne provjere izvornog koda od stvarnog Android builda na GitHub Actionsu.

## Lokalno provjereno prije migracije na GitHub

- Kotlin jezgrena logika i modeli provjeravani su `kotlinc` smoke testovima uz testne stubove.
- XML resursi parsirani su bez sintaktičkih pogrešaka.
- Implementirani su unit testovi za `ScheduleLogic`, `DateUtils` i `ICalendarExporter`.
- Manifest nema `INTERNET` permission.
- Projekt nema namjerne `TODO` / `FIXME` placeholdere u produkcijskom Kotlin kodu.

## GitHub CI

Workflow `.github/workflows/android-ci.yml` je izvor istine za puni Android build. Na push/PR prema `main` pokreće:

1. `testDebugUnitTest`
2. `lintDebug`
3. `assembleDebug`
4. upload debug APK-a kao Actions artifact

0.1.0 se smatra build-potvrđenim tek kada navedeni workflow prođe na GitHubu.
