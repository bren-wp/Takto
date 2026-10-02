# Takto 0.1.10 — jasnija Statistika

Takto 0.1.10 ispravlja prikaz godišnjih trendova i poboljšava čitljivost statistike na manjim Android ekranima.

## Najvažnije

- vrijednost 0 više ne proizvodi lažni minimalni stupac
- godina bez unosa ima jasno prazno stanje
- godina bez evidentiranih sati ima zasebno prazno stanje
- minute se čuvaju u kompaktnim oznakama: 30m, 1h, 1h30
- metričke kartice prilagođavaju raspored uskim ekranima
- donut graf i legenda prilagođavaju se uskim ekranima
- TalkBack dobiva mjesečne vrijednosti godišnjih grafova
- skaliranje grafikona izdvojeno je u testabilnu logiku

## Verzija

- `versionName`: `0.1.10`
- `versionCode`: `11`

## QA

Izdanje se objavljuje tek nakon zelenih Android CI i CodeQL provjera. Release workflow ponovno izvršava testove i lint, gradi APK/AAB te provjerava izlazne datoteke prije objave.

## Artefakti

- `Takto-0.1.10.apk`
- `Takto-0.1.10-release-unsigned.apk`
- `Takto-0.1.10-release-unsigned.aab`
- `SHA256SUMS.txt`

Produkcijski signing key nije pohranjen u repozitoriju; release APK/AAB bez produkcijskog potpisa jasno su označeni kao unsigned.
