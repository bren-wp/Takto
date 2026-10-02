# Takto 0.1.5 — brža dugoročna pohrana rasporeda

Takto 0.1.5 optimizira način na koji se veliki višegodišnji rasporedi trajno spremaju, bez promjene postojećeg korisničkog formata podataka.

## Najvažnije

- male promjene više ne prepisuju cijelu JSON snimku rasporeda
- append-only journal trajno čuva svaku promjenu između checkpointa
- journal se eksplicitno sinkronizira na disk prije nego se promjena smatra spremljenom
- puna atomska snimka radi se nakon 64 revizije
- bulk promjene od 32 ili više datuma odmah konsolidiraju novu snimku
- pri pokretanju Takto bira noviju između glavne i recovery snimke
- postojeći replay revizija oporavlja promjene novije od checkpointa
- stari backup format i migracija iz SharedPreferences ostaju kompatibilni

## Kvaliteta

- dodani unit testovi za checkpoint politiku
- Android CI i CodeQL ostaju obavezni prije mergea
- release workflow provjerava APK, unsigned release APK, unsigned AAB i SHA-256

## Artefakti

- `Takto-0.1.5.apk`
- `Takto-0.1.5-release-unsigned.apk`
- `Takto-0.1.5-release-unsigned.aab`
- `SHA256SUMS.txt`

Produkcijski signing key nije pohranjen u repozitoriju; release APK/AAB bez produkcijskog potpisa jasno su označeni kao unsigned.
