# Takto 0.1.6 — preglednija Početna i kvalitetnija prazna stanja

Takto 0.1.6 fokusiran je na svakodnevnu upotrebljivost Početne, posebno na manjim Android ekranima i uz TalkBack.

## Najvažnije

- kartica **Danas** jasno razlikuje spremljen unos od praznog dana
- današnja napomena prikazuje do dva retka
- tjedni sažetak na uskim ekranima prelazi u stabilniji 2 + 1 raspored kartica
- tjedne ćelije smanjuju spacing i tipografiju na manjim širinama
- svaki tjedni datum ima TalkBack opis s datumom i stanjem unosa
- prazno stanje budućih unosa prikazuje konkretan datum za planiranje
- mjesečni kalendar objašnjava kada trenutni mjesec nema unosa
- neutralna terminologija i adaptivne brze oznake ostaju očuvane

## Verzija

- `versionName`: `0.1.6`
- `versionCode`: `7`

## QA

Izdanje se objavljuje tek nakon zelenih Android CI i CodeQL provjera. Release workflow ponovno izvršava testove i lint, gradi APK/AAB i provjerava izlazne datoteke prije objave.

## Artefakti

- `Takto-0.1.6.apk`
- `Takto-0.1.6-release-unsigned.apk`
- `Takto-0.1.6-release-unsigned.aab`
- `SHA256SUMS.txt`

Produkcijski signing key nije pohranjen u repozitoriju; release APK/AAB bez produkcijskog potpisa jasno su označeni kao unsigned.
