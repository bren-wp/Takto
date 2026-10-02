# Takto 0.1.8 — pametnija lokalna pretraga

Takto 0.1.8 proširuje pretragu rasporeda bez clouda, analitike ili slanja korisničkih podataka izvan uređaja.

## Najvažnije

- hrvatski nazivi mjeseci i upiti mjesec + godina
- pretraga dana u tjednu
- pronalaženje unosa po početku ili kraju radnog vremena
- dodatni relativni izrazi: prekosutra, preksutra i prekjučer
- tekstualni semantički upiti: buduće, s vremenom, radno vrijeme i s napomenom
- poboljšano rangiranje višerječnih upita
- očuvana tolerancija na hrvatske dijakritičke znakove
- regresijski testovi za nove oblike pretrage

## Verzija

- `versionName`: `0.1.8`
- `versionCode`: `9`

## QA

Izdanje se objavljuje tek nakon zelenih Android CI i CodeQL provjera. Release workflow ponovno pokreće testove i lint, gradi APK/AAB i provjerava izlazne datoteke prije objave.

## Artefakti

- `Takto-0.1.8.apk`
- `Takto-0.1.8-release-unsigned.apk`
- `Takto-0.1.8-release-unsigned.aab`
- `SHA256SUMS.txt`

Produkcijski signing key nije pohranjen u repozitoriju; release APK/AAB bez produkcijskog potpisa jasno su označeni kao unsigned.
