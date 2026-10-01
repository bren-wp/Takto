# Takto 0.1.0 — provedene provjere u ovom radnom okruženju

Ovaj dokument bilježi samo provjere koje su stvarno izvršene nad izvorom **0.1.0**. Ne zamjenjuje puni Android build i ne tvrdi da je APK uspješno izrađen.

## Prošlo

- `app/build.gradle.kts` potvrđen na **versionName 0.1.0 / versionCode 1**.
- `AndroidManifest.xml` i svi XML resursi parsirani su kao valjan XML: **11 XML datoteka**.
- manifest sadrži `DailyReminderReceiver`, novi `ShiftReminderReceiver` i `BootReceiver`.
- u produkcijskom Kotlin kodu nema `TODO` / `FIXME` placeholdera.
- aplikacija i dalje **ne deklarira `android.permission.INTERNET`**.
- `ShiftModels.kt`, `ScheduleLogic.kt`, novi `ICalendarExporter.kt` i `ScheduleStore.kt` kompajlirani su stvarnim `kotlinc` compilerom uz minimalne testne Android / Compose / JSON stubove.
- izvršen je stvarni `ScheduleStore` / iCalendar smoke test:
  - postavka **podsjetnika prije smjene** može se uključiti i spremiti vremenski odmak od 60 min
  - noćna smjena `N` na 3. 10. 2026. s vremenom `19:00–07:00` izvozi `DTSTART:20261003T190000`
  - ista noćna smjena ispravno izvozi `DTEND:20261004T070000`
  - iCalendar sažetak zadržava hrvatski naziv `N · Noć`
  - GO bez radnog vremena izvozi se kao cjelodnevni događaj od `20261004` do ekskluzivnog `20261005`
- izvršen je dodatni Unicode iCalendar smoke test:
  - dugi hrvatski tekst i emoji ostaju sačuvani
  - svi fizički iCalendar redovi nakon RFC 5545 prelamanja ostaju na **najviše 75 UTF-8 okteta**
  - Unicode znak nije prerezan na pola pri prelamanju retka
- `ReminderScheduler.kt` i novi `ShiftReminderReceiver.kt` kompajlirani su stvarnim `kotlinc` compilerom uz ciljane Android/AndroidX stubove; nisu pronađene type/syntax greške u novoj scheduler/receiver logici.
- promijenjeni Android/Compose izvori `MainActivity`, `TaktoApplication`, `SettingsScreen`, `BootReceiver` i `DailyReminderReceiver` propušteni su kroz Kotlin parser/compiler bez Android/Compose classpatha. Dobivene su očekivane `unresolved reference` poruke zbog nedostajućeg classpatha, ali **nisu pronađene parser/sintaktičke poruke** poput `expecting`, `unexpected tokens`, `unclosed`, `conflicting overloads` ili `redeclaration`.
- dnevni i smjenski alarm imaju odvojene request codeove i odvojene cancel metode, pa gašenje dnevnog podsjetnika ne mora gasiti podsjetnik prije smjene i obratno.
- JSON backup je podignut na **schema v6** i kod izvoza/uvoza uključuje `shiftRemindersEnabled` i `shiftReminderLeadMinutes`.

## Nije moguće potvrditi u ovom okruženju

- Gradle Sync
- `testDebugUnitTest`
- `assembleDebug`
- release/R8 build
- instalacija APK-a
- pokretanje na emulatoru ili fizičkom Android uređaju
- stvarno ponašanje `AlarmManager` alarma pod različitim OEM battery-management pravilima
- Android 13+ runtime dijalog za `POST_NOTIFICATIONS`
- otvaranje `.ics` datoteke u stvarnim Google Calendar / Outlook / Apple Calendar aplikacijama
- stvarni UI smoke test i TalkBack

Razlog: Android SDK i kompletni Gradle wrapper/runtime nisu dostupni u ovom radnom okruženju.
