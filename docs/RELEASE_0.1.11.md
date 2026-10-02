# Takto 0.1.11

Takto 0.1.11 poboljšava karticu **Danas** tako da ostane čitljiva, brza i pristupačna na malim Android ekranima i pri povećanom sistemskom fontu.

## Što je novo

- brze oznake na kartici **Danas** koriste jedan stupac na vrlo uskim ekranima
- pri font scaleu 1.30 ili većem automatski se koristi širi jednokolonski raspored
- gumbi se mogu vertikalno proširiti umjesto rezanja teksta u fiksnih 52 dp
- TalkBack za brze akcije jasno izgovara radnju, naziv i oznaku
- spremljeni današnji unos ima objedinjeni opis datuma, oznake, radnog vremena, trajanja i napomene
- responzivna pravila izdvojena su u čistu testabilnu logiku
- dodani su regresijski unit testovi za male ekrane, veliki font i accessibility opise

## Privatnost i kompatibilnost

- nema INTERNET dopuštenja
- raspored i korisnički podaci ostaju lokalni
- nema promjene formata pohrane ni destruktivne migracije
- postojeći backup/import podaci ostaju kompatibilni

## Verzija

- `versionName = 0.1.11`
- `versionCode = 12`

Release workflow ponovno pokreće testove, Android lint i build, verificira APK/AAB izlaze i objavljuje SHA256 checksume uz GitHub Release.
