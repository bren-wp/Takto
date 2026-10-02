# Takto 0.1.4 — sigurnija lokalna pohrana i oporavak rasporeda

Takto 0.1.4 fokusiran je na dugoročnu stabilnost i zaštitu rasporeda, posebno za velike višegodišnje kalendare.

## Najvažnije promjene

- raspored više se ne pohranjuje kao jedan veliki `SharedPreferences` JSON string
- nova glavna snimka `takto_schedule_current.json` koristi Android `AtomicFile`
- uvedena je pričuvna snimka `takto_schedule_recovery.json`
- prethodno potvrđeno stanje kopira se streaming načinom prije zamjene glavne snimke
- postojeće instalacije automatski migriraju stari raspored u novi format
- stari `entries_json` uklanja se tek kada su glavna i recovery snimka uspješno zapisane
- append-only revizijska arhiva ostaje treći sloj zaštite
- snimka pamti checkpoint arhive pa se pri pokretanju obrađuju samo novije revizije
- prekid procesa između zapisa revizije i spremanja glavne snimke više ne mora izgubiti zadnju promjenu
- recovery logika provjerava `before` i `after` stanje te ne prepisuje divergentno novije stanje starijom revizijom
- provjerava se deklarirani broj zapisa u lokalnoj snimci
- prevelika ili nevaljana snimka ne prihvaća se kao valjana
- cloud backup i device transfer uključuju glavnu snimku, recovery snimku i revizijsku arhivu
- Postavke prikazuju broj rasporednih unosa i slojeve zaštite lokalnih podataka
- dodani unit testovi za nedestruktivni recovery algoritam
- broj revizija i duljina arhive imaju cache; cijela višegodišnja arhiva ponovno se skenira samo kada se stvarna datoteka razlikuje od spremljene metadata vrijednosti

## Kompatibilnost

- postojeći rasporedi migriraju se automatski
- vlastite oznake, profil, postavke, backup i arhiva ostaju kompatibilni
- nema migracije koja briše stare ili buduće datume
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

Automatski release workflow priprema:

- `Takto-0.1.4.apk`
- `Takto-0.1.4-release-unsigned.apk`
- `Takto-0.1.4-release-unsigned.aab`
- `SHA256SUMS.txt`

Produkcijski Google Play ključ nije pohranjen u repozitoriju.
