# Takto 0.1.15

Ovo izdanje dodatno poboljšava najosjetljiviji dio aplikacije: uvoz stvarnog rasporeda s fotografije i rad s prekovremenim satima.

## Skeniranje rasporeda

- Takto može koristiti ime i prezime iz radnog profila i pokušati izdvojiti samo tu osobu iz tablice s više zaposlenika
- ako ime nije sigurno pronađeno, drugi zaposlenik se ne uvozi automatski
- podržani su hrvatski dijakritički znakovi u imenima
- podržan je raspored u kojem je ime u jednom retku, a smjene u sljedećem
- kada mjesec nije vidljiv na slici, korisnik mora ručno odabrati i potvrditi mjesec prije uvoza
- svaki pronađeni datum može se pregledati i ručno ispraviti prije upisa u Kalendar; svi pronađeni dani ostaju vidljivi u pregledu
- moguće je promijeniti oznaku, početak, kraj, pauzu, označiti slobodan dan ili potpuno ukloniti pogrešan red
- galerijske slike obrađuju se s većim ograničenjem rezolucije kako bi sitni tekst u širokim tablicama ostao čitljiviji

## Prekovremeni sati

- potvrđeni prekovremeni i dalje se unose izričito, bez automatskog proglašavanja dugih smjena prekovremenima
- dodani su brzi izbori 0, 30, 60 i 120 minuta
- brzi izbori su raspoređeni 2 × 2 radi preglednosti na manjim ekranima
- redovni sati više ne uključuju potvrđene prekovremene
- Početna prikazuje potvrđene prekovremene i, kada su svi ulazni podaci potpuni, procjenu isplate, neto i bruto iznosa
- standardni radni dan sada je jasnije označen kao kontrolna metrika, a ne automatski podatak za isplatu

## UI/UX

- korekcija skeniranog unosa prilagođena je manjim Android ekranima
- pregled prije uvoza ostaje obavezan
- korisnik može ponovno provjeriti drugo ime nad već prepoznatim tekstom bez ponovnog fotografiranja

## QA

Izdanje se spaja tek nakon uspješnih unit testova, Android lint provjera, debug/release APK builda, AAB builda, provjere artefakata i CodeQL analize.

## Verzija

- `versionName = 0.1.15`
- `versionCode = 16`
