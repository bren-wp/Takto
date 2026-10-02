# Takto 0.1.7 — brži dugoročni storage

Takto 0.1.7 optimizira pokretanje aplikacije kada lokalna revizijska arhiva kroz godine naraste na velik broj promjena.

## Najvažnije

- checkpoint snimke pamte sigurni byte-offset u append-only arhivi
- valjani checkpoint izravno nastavlja čitanje od svojeg offseta, bez prolaska kroz sve stare retke
- cursor se provjerava prema duljini datoteke, broju revizija i granici retka
- stari schema 1 snapshoti ostaju potpuno podržani
- nevaljani cursor automatski pada na postojeći replay po broju revizija
- povijest promjena ostaje append-only i ne skraćuje se
- dodani su unit testovi za sigurnosne uvjete cursora

## Verzija

- `versionName`: `0.1.7`
- `versionCode`: `8`

## QA

Izdanje se objavljuje tek nakon zelenih Android CI i CodeQL provjera. Release workflow ponovno pokreće testove i lint, gradi APK/AAB i provjerava izlazne datoteke prije objave.

## Artefakti

- `Takto-0.1.7.apk`
- `Takto-0.1.7-release-unsigned.apk`
- `Takto-0.1.7-release-unsigned.aab`
- `SHA256SUMS.txt`

Produkcijski signing key nije pohranjen u repozitoriju; release APK/AAB bez produkcijskog potpisa jasno su označeni kao unsigned.
