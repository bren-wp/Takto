# Takto 0.1.17

Takto 0.1.17 nastavlja završno poliranje aplikacije s fokusom na sigurniji uvoz rasporeda, responzivni Kalendar, pristupačnost i čišći kod.

## Kalendar

- alatna traka se automatski preslaguje u dva retka kada širina ili povećani font više ne dopuštaju siguran prikaz u jednom retku
- akcije skeniranja, pretrage, poništavanja, višestrukog odabira i povratka na današnji datum ostaju dostupne i na manjim ekranima
- višestruki odabir ima jasniji pristupačni opis uključivanja i isključivanja
- naslov mjeseca eksplicitno je označen kao gumb za odabir mjeseca

## Skeniranje rasporeda

- siguran način rada je sada zadani: postojeći dani se ne prepisuju automatski
- pregled prikazuje koliko je pronađenih datuma novo, a koliko već postoji u Kalendaru
- već popunjeni datumi imaju jasnu oznaku u svakom retku pregleda
- ako korisnik uključi prepisivanje, završni gumb jasno pokazuje broj postojećih unosa koji će biti zahvaćeni
- destruktivno prepisivanje traži dodatnu potvrdu
- boja teksta na skeniranim oznakama prilagođava se pozadini radi čitljivosti svijetlih i tamnih vlastitih boja

## Stabilnost i održavanje

- uklonjen je neupotrebljivi helper za izvoz arhive u memorijski string; streaming izvoz ostaje jedini put
- uklonjene su neupotrebljive izravne Lifecycle ovisnosti, neupotrebljiva Compose preview ovisnost iz produkcijskog classpatha i neupotrebljivi Compose UI test manifest
- skenirani slobodan dan koji je već prazan više ne proizvodi lažnu promjenu niti nepotrebnu Undo reviziju
- rezultat uvoza koristi jasniji pojam "Promijenjeno" umjesto da sve pronađene slobodne dane prikazuje kao uvezene

## QA

Izdanje se spaja tek nakon uspješnih unit testova, Android lint provjera, debug/release APK builda, release AAB builda, provjere artefakata i CodeQL analize.

## Verzija

- `versionName = 0.1.17`
- `versionCode = 18`
