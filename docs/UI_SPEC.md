# Takto UI specifikacija

## Brend

- Pozadina: `#06101F`
- Površina: `#0F172A`
- Sekundarna površina: `#142136`
- D / primarno plava: `#2488FF`
- N / ljubičasta: `#8B46F6`
- GO / zelena: `#13D7A0`
- BO / jantarna: `#FFB21D`
- PD / crvena: `#FF4B55`
- Cijan akcent: `#1DE1E8`

## Kalendar

- tjedan počinje ponedjeljkom
- uvijek se renderira 6 redaka × 7 stupaca
- kućice su velike, zaobljene i imaju dominantnu oznaku smjene
- broj dana ostaje u gornjem lijevom kutu
- D i N koriste veći font; GO / BO / PD nešto manji kako bi bili čitljivi
- vlastiti tekst dinamički smanjuje font ako je duži
- prazna kućica nema `+`, nema oznaku i znači slobodan dan

## Interakcija

1. Dodir datuma.
2. Otvara se donji panel.
3. Korisnik bira D, N, GO, BO, PD ili Vlastiti unos.
4. Odabrana oznaka odmah ispunjava kućicu.
5. "Postavi kao slobodan dan" briše oznaku s datuma.

## Navigacija

- Početna
- Kalendar
- Statistika
- Uzorci
- Više

## Dodatna pravila 0.1.0 — kalendar i personalizacija

- rubni datumi prethodnog i sljedećeg mjeseca prikazuju se prigušeno i nisu klikabilni
- današnji datum ima cijan obrub
- odabrani datum ima plavi obrub
- mala bijela točka pri dnu kućice označava da postoji napomena
- boje D / N / GO / BO / PD mogu se personalizirati; promjena boje mora biti dosljedna kroz kalendar i statistiku
- vlastiti unos ima izbor boje i maksimalno 24 znaka
- napomena ima maksimalno 180 znakova

## Dodatna pravila 0.1.0 — višestruko uređivanje

- ikona **višestrukog odabira** u zaglavlju kalendara uključuje način rada za uređivanje više datuma
- višestruko odabrane kućice koriste isti jaki plavi obrub kao primarni signal odabira
- prečaci **Cijeli mjesec** i **Pon–pet** moraju jasno pokazati broj odabranih dana
- bulk izbornik koristi iste velike D / N / GO / BO / PD kartice kao pojedinačni unos kako bi ponašanje bilo dosljedno
- bulk način uvijek prikazuje hoće li postojeći unosi biti prepisani ili preskočeni
- kopiranje tjedna radi u logičkom rasponu ponedjeljak–nedjelja bez obzira na to koji je dan otvoren
- lijepljenje tjedna mora jasno upozoriti da opcija prepisivanja može pretvoriti postojeći unos u slobodan dan ako je izvorni dan prazan
- kalendarske kućice moraju imati TalkBack opis: puni datum + status slobodnog dana ili šifra/naziv + napomena kada postoji

## Dodatna pravila 0.1.0 — radno vrijeme i Undo

- datum s evidentiranim početkom i krajem smjene prikazuje **cijan točku** pri dnu kućice
- datum s napomenom prikazuje **bijelu točku**; ako postoje i vrijeme i napomena, oba indikatora moraju ostati jasno vidljiva
- radno vrijeme uređuje se u zasebnom dijalogu s poljima **Početak**, **Kraj** i **Pauza (min)**
- UI prihvaća uobičajeni unos `7`, `07:30` i `7.30`, ali ga nakon spremanja prikazuje kao `HH:mm`
- kada je kraj prije početka, UI jasno označava da smjena prelazi ponoć
- jednaki početak i kraj nisu valjana 24-satna smjena i ne smiju se spremiti kao radno vrijeme
- GO / BO / PD ne prikazuju uređivanje radnog vremena jer se tretiraju kao odsustvo
- bulk dijalog smije primijeniti radno vrijeme samo na već popunjene radne dane, a prazne dane i odsustva mora preskočiti
- u zaglavlju kalendara postoji **Vrati** ikona; aktivna je samo kada postoji zadnja promjena koju je moguće vratiti
- contentDescription Undo ikone mora sadržavati naziv promjene kada je dostupan
- standardni radni dan postavlja se u Postavkama i koristi se samo kao prag obračuna prekovremenog rada, ne mijenja zapisano trajanje smjene
- kartice statistike moraju razlikovati **Ukupno sati**, **Prekovremeno** i **Prosjek smjene**
- sljedeća smjena na Početnoj prikazuje vremenski raspon i trajanje kada postoje
- podsjetnik smjene uključuje vremenski raspon kada postoji evidentirano radno vrijeme


## Dodatna pravila 0.1.0 — fond i posebni sati

- Početna prikazuje mjesečni blok **evidentirano / fond / razlika** bez zamjene postojećeg tjednog sažetka
- razlika fonda koristi zelenu/cijan signalizaciju za nulu/višak i crvenu za manjak
- Statistika mora jasno razlikovati automatski i ručno postavljen mjesečni fond
- gumb **Fond** otvara uređivanje fonda za trenutno odabrani mjesec, ne globalno za sve mjesece
- automatski fond u UI-u mora imati napomenu da računa pon–pet i ne oduzima blagdane
- posebni sati se prikazuju u zasebnoj kartici: **Noćni rad**, **Vikend**, **Nedjelja**
- noćni prozor je 22:00–06:00; ovaj podatak mora biti vidljiv uz statistiku
- zadano vrijeme oznake uređuje se u Postavkama uz samu radnu oznaku
- GO / BO / PD ne smiju nuditi predložak radnog vremena
- red predloška prikazuje `HH:mm – HH:mm · pauza N min` kada je vrijednost spremljena
- dijalog ručnog radnog vremena pokazuje **Primijeni zadano** samo ako aktualna oznaka ima predložak
- zadana vremena moraju ostati sekundarna funkcija: korisnik uvijek može ručno promijeniti vrijeme samo za jedan datum

## Dodatna pravila 0.1.0 — podsjetnici i iCalendar

- Postavke moraju jasno razdvojiti **Dnevni podsjetnik** od **Podsjetnika prije smjene**; gašenje jednog ne smije vizualno ni funkcionalno gasiti drugi
- red **Koliko ranije upozoriti** prikazuje trenutno odabrani odmak u ljudskom obliku (`30 min`, `1 h`, `1 dan`)
- podsjetnik prije smjene dostupan je samo kao sekundarna automatizacija; raspored i unos moraju ostati potpuno upotrebljivi bez notifikacijskih dopuštenja
- iCalendar izvoz nalazi se uz CSV izvoz i jasno koristi nastavak `.ics`
- tekst uz iCalendar mora objasniti da se datoteka može otvoriti u standardnim kalendarskim aplikacijama
- obavijest prije smjene u naslovu navodi da smjena uskoro počinje, a u sadržaju prikazuje kod/naziv i raspon `HH:mm–HH:mm`
- dodir takve obavijesti vodi na konkretan datum, ne samo na početni ekran
