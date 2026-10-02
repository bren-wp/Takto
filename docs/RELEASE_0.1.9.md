# Takto 0.1.9 — sigurniji unos radnog vremena

Takto 0.1.9 ujednačava unos i validaciju radnog vremena kroz kalendar, bulk uređivanje i zadana vremena po oznakama.

## Najvažnije

- podržani unosi: 07:30, 7.30, 18,45, 730 i 0730
- centralna validacija početka, kraja i pauze
- nema tihog ograničavanja nevaljane pauze
- brze pauze: 0, 15, 30, 45 i 60 minuta
- neto trajanje vidljivo prije spremanja
- rad preko ponoći jasno označen kao završetak sljedeći dan
- ista pravila za pojedinačne, bulk i preset izmjene
- regresijski unit testovi za rubne slučajeve

## Verzija

- `versionName`: `0.1.9`
- `versionCode`: `10`

## QA

Izdanje se objavljuje tek nakon zelenih Android CI i CodeQL provjera. Release workflow ponovno izvršava testove i lint, gradi APK/AAB te provjerava izlazne datoteke prije objave.

## Artefakti

- `Takto-0.1.9.apk`
- `Takto-0.1.9-release-unsigned.apk`
- `Takto-0.1.9-release-unsigned.aab`
- `SHA256SUMS.txt`

Produkcijski signing key nije pohranjen u repozitoriju; release APK/AAB bez produkcijskog potpisa jasno su označeni kao unsigned.
