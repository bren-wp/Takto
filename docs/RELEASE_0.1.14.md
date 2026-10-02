# Takto 0.1.14

Ovo izdanje završava glavni zahvat na skeniranju rasporeda, prekovremenim satima i obračunu plaće te dodatno poboljšava upravljanje označenim područjem na telefonu.

## Skeniranje rasporeda

- kamera i galerija vode kroz isti kontrolirani postupak
- prije prepoznavanja označava se samo relevantni dio rasporeda
- okvir se može pomicati i mijenjati povlačenjem
- dodani su izravni gumbi Lijevo, Gore, Dolje i Desno
- slika se može rotirati −90° ili +90°
- Jedan red vraća praktični početni izrez
- konfliktni rezultati za isti datum označavaju se dvosmislenima i ne mogu se automatski uvesti
- slobodni dani ostaju slobodni dani, bez lažnih oznaka
- pregled je obavezan prije upisa u Kalendar

## Radni sati i prekovremeni

- svaki radni dan može imati zasebno potvrđene prekovremene minute
- potvrđeni prekovremeni vidljivi su uz radni unos i u Statistici
- mjesečni fond i rad iznad standardnog dana ostaju zasebne kontrolne metrike
- samo izričito potvrđeni prekovremeni ulaze u obračun plaće

## Plaća

- podržani su državna služba, javne službe i ostali sustavi
- službene osnovice za 2026. biraju se prema mjesecu za državnu i javne službe
- koeficijent, navršene godine staža, porezne stope i osobni odbitak unosi korisnik
- podržani su prekovremeni, noćni rad, subota, nedjelja i blagdan
- obračun uključuje MIO I./II. stup, primjenjivo umanjenje osnovice, porez, neoporezive isplate i bruto kod drugih poslodavaca
- ako nedostaje podatak potreban za potpun obračun, Takto prikazuje što nedostaje umjesto netočnog neto iznosa

## UI/UX

- preciznije upravljanje područjem skeniranja bez ovisnosti o sitnim gestama
- čišći tekstovi sigurnosnih kopija i povijesti promjena
- stvarna verzija aplikacije i build artefakata usklađena je na 0.1.14

## QA

Release se objavljuje tek nakon uspješnih unit testova, Android lint provjera, debug/release APK builda, AAB builda, provjere artefakata i CodeQL analize.

## Verzija

- `versionName = 0.1.14`
- `versionCode = 15`
