# Takto 0.1.12

Takto 0.1.12 poboljšava **Kalendar** na malim Android ekranima i pri povećanom sistemskom fontu, uz pristupačniji vlastiti unos.

## Što je novo

- odabir oznaka koristi jedan stupac ispod 360 dp
- isti raspored aktivira se pri font scaleu 1.30 ili većem
- kartice oznaka mogu vertikalno rasti umjesto rezanja sadržaja u fiksnoj visini
- početak i kraj radnog vremena automatski se slažu jedan ispod drugoga kada je širina ograničena
- paleta vlastite boje prikazuje tri kontrole po retku umjesto šest skučenih kontrola
- svaka boja ima najmanje 48 dp visine
- TalkBack dobiva naziv svake boje, radio-button ulogu i informaciju koja je boja odabrana
- responzivna pravila pokrivena su regresijskim unit testovima

## Privatnost i kompatibilnost

- nema INTERNET dopuštenja
- nema clouda ni analytics/tracking SDK-ova
- raspored i korisnički podaci ostaju lokalni
- nema promjene formata pohrane ni destruktivne migracije
- postojeći backup/import podaci ostaju kompatibilni

## Verzija

- `versionName = 0.1.12`
- `versionCode = 13`

Release workflow ponovno pokreće testove, Android lint i build, verificira APK/AAB izlaze i objavljuje SHA256 checksume uz GitHub Release.
