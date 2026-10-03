# Takto 0.1.16 — QA checklist

## Build
- [ ] Gradle sync prolazi bez greške
- [ ] `testDebugUnitTest` prolazi
- [ ] `assembleDebug` prolazi
- [ ] release R8/minify build prolazi
- [ ] aplikacija se instalira i pokreće na Androidu 8.0+

## Kalendar
- [ ] D, N, GO, BO i PD spremaju se na odabrani datum
- [ ] prazna kućica je slobodan dan
- [ ] brisanje vraća slobodan dan
- [ ] vlastiti jednokratni unos sprema tekst, boju i napomenu
- [ ] napomena se može promijeniti bez promjene oznake
- [ ] rubni datumi prethodnog/sljedećeg mjeseca nisu slučajno klikabilni
- [ ] gumb Danas otvara današnji datum
- [ ] pretraga pronalazi kod, naziv, napomenu i datum te otvara pronađeni dan
- [ ] vlastiti unos otvara samo jedan dijalog i sprema se na pravi datum
- [ ] izbornik dana ostaje skrolabilan kada postoji mnogo vlastitih brzih oznaka
- [ ] cijan točka označava evidentirano radno vrijeme
- [ ] bijela točka označava napomenu
- [ ] dan s napomenom i radnim vremenom prikazuje oba indikatora bez preklapanja
- [ ] TalkBack čita datum, status slobodnog dana ili oznaku/naziv/napomenu/radno vrijeme

## Radno vrijeme
- [ ] početak i kraj mogu se upisati kao `7`, `07:30` ili `7.30`
- [ ] nevaljani sat (`25:00`, `12:99`, tekst) odbija se
- [ ] jednaki početak i kraj ne spremaju 24-satnu smjenu
- [ ] dnevna smjena 07:00–15:00 daje 8 h prije pauze
- [ ] noćna smjena 19:00–07:00 prelazi ponoć i daje 12 h prije pauze
- [ ] pauza se ispravno oduzima od trajanja
- [ ] pauza veća od bruto trajanja ne proizvodi negativno trajanje
- [ ] radno vrijeme može se ukloniti bez brisanja oznake smjene
- [ ] GO / BO / PD ne prihvaćaju radno vrijeme
- [ ] promjena radne oznake u GO / BO / PD uklanja ranije radno vrijeme
- [ ] promjena između radnih oznaka čuva postojeće radno vrijeme kada je to predviđeno
- [ ] bulk radno vrijeme mijenja samo popunjene ne-odsutne dane
- [ ] bulk radno vrijeme preskače prazne dane i GO / BO / PD
- [ ] promjena standardnog radnog dana mijenja samo kontrolnu metriku rada iznad standarda
- [ ] samo izričito potvrđeni prekovremeni ulaze u obračun plaće

## Fond sati i posebni sati
- [ ] automatski mjesečni fond odgovara broju dana pon–pet × standardni radni dan
- [ ] ručni fond se može postaviti samo za odabrani mjesec
- [ ] povratak na Automatski briše ručni fond samo za odabrani mjesec
- [ ] Početna prikazuje ispravan evidentirano / fond / razlika prikaz
- [ ] pozitivna razlika prikazuje višak, negativna razlika manjak sati
- [ ] noćna smjena 22:00–06:00 daje 8 h noćnog rada bez pauze
- [ ] smjena 02:00–10:00 daje 4 h noćnog rada bez pauze
- [ ] dnevna smjena 07:00–15:00 daje 0 noćnih sati
- [ ] smjena petak 20:00–subota 04:00 računa 4 h vikenda
- [ ] smjena subota 20:00–nedjelja 04:00 računa 8 h vikenda i 4 h nedjelje
- [ ] pauza smanjuje posebne sate proporcionalno bruto trajanju

## Zadana vremena oznaka
- [ ] za D i N moguće je spremiti početak, kraj i pauzu
- [ ] GO / BO / PD ne nude zadano radno vrijeme
- [ ] vlastita brza radna oznaka može imati zadano radno vrijeme
- [ ] novi prazni dan pri dodjeli oznake automatski dobiva zadano vrijeme
- [ ] postojeće ručno vrijeme se ne pregazi pri običnoj promjeni radne oznake kada je čuvanje vremena predviđeno
- [ ] uzorak smjena koristi zadano vrijeme za radnu oznaku
- [ ] dijalog radnog vremena nudi Primijeni zadano kada predložak postoji
- [ ] uklanjanje predloška vremena ne briše postojeće vrijeme iz kalendara
- [ ] brisanje vlastite brze oznake uklanja njen predložak vremena, ali ne briše povijesne unose

## Undo / Vrati
- [ ] nakon promjene oznake gumb Vrati vraća prethodno stanje
- [ ] nakon uređivanja napomene Vrati vraća prethodnu napomenu
- [ ] nakon dodavanja radnog vremena Vrati vraća prethodno radno vrijeme
- [ ] nakon uklanjanja radnog vremena Vrati ga može vratiti
- [ ] bulk promjena više dana vraća sve zahvaćene datume jednim Undo korakom
- [ ] bulk radno vrijeme vraća sva zahvaćena vremena jednim Undo korakom
- [ ] bulk postavljanje slobodnih dana vraća prethodne unose
- [ ] lijepljenje tjedna može se vratiti jednim Undo korakom
- [ ] primjena uzorka može se vratiti jednim Undo korakom
- [ ] nakon CSV uvoza ili JSON restorea stari Undo snapshot više nije dostupan
- [ ] nakon izvršenog Undo gumba nema lažnog drugog Undo koraka

## Višestruki odabir
- [ ] višestruki odabir ručno uključuje/isključuje pojedine datume
- [ ] prečac Cijeli mjesec odabire samo stvarne dane mjeseca
- [ ] prečac Pon–pet ne odabire subotu i nedjelju
- [ ] bulk D / N / GO / BO / PD radi za sve odabrane dane
- [ ] bulk vlastita brza oznaka radi za sve odabrane dane
- [ ] bulk vlastiti unos sprema tekst, boju i zajedničku napomenu
- [ ] kada je Prepiši postojeće isključen, popunjeni datumi se preskaču
- [ ] bulk Slobodni dan briše samo odabrane datume
- [ ] višestruko odabrane kućice imaju jasan plavi rub

## Kopiranje tjedna
- [ ] Kopiraj tjedan uzima ponedjeljak–nedjelja bez obzira koji je dan otvoren
- [ ] Zalijepi tjedan postavlja isti raspored na ciljni ponedjeljak–nedjelja
- [ ] bez prepisivanja postojeći popunjeni ciljni dani ostaju netaknuti
- [ ] s prepisivanjem prazni dani iz izvornog tjedna brišu ciljni unos
- [ ] napomene, boje i radno vrijeme ostaju sačuvani pri kopiranju tjedna

## Vlastite brze oznake
- [ ] može se spremiti nova vlastita oznaka s kodom, nazivom i bojom
- [ ] D / N / GO / BO / PD ne mogu se prepisati kao vlastite oznake
- [ ] spremljena vlastita oznaka pojavljuje se u kalendaru kao brzi izbor
- [ ] uređivanje naziva/boje osvježava stare zapise s istim kodom
- [ ] brisanje prečaca ne briše postojeće datume s tom oznakom
- [ ] ograničenje od 20 vlastitih brzih oznaka radi

## Vlastiti uzorci
- [ ] korisnik može spremiti vlastiti slijed npr. `D,D,N,N,-,-,-,-`
- [ ] `-` i `slobodno` u editoru znače slobodan dan
- [ ] spremljene vlastite oznake mogu se koristiti u uzorku
- [ ] vlastiti kratki kod koji nije spremljen može se koristiti u uzorku
- [ ] vlastiti uzorak može se obrisati
- [ ] ograničenje 20 uzoraka / 28 koraka radi
- [ ] 7 / 14 / 28 / 56 dana rade
- [ ] bez opcije prepisivanja postojeći datumi ostaju netaknuti
- [ ] s opcijom prepisivanja null dan postaje slobodan

## Uvoz/izvoz
- [ ] CSV sa zarezom radi
- [ ] CSV s točka-zarezom radi
- [ ] ISO datumi rade
- [ ] hrvatski datumi `1.10.2026.` rade
- [ ] prazna šifra briše unos
- [ ] vlastite oznake se uvoze
- [ ] napomene s navodnicima i novim redom ne ruše parser
- [ ] opcionalni CSV stupac `boja` vraća boju proizvoljnog unosa
- [ ] CSV stupci `pocetak`, `kraj` i `pauza_min` vraćaju radno vrijeme
- [ ] noćna smjena iz CSV-a (`19:00`–`07:00`) vraća ispravno trajanje
- [ ] GO / BO / PD iz CSV-a ignoriraju slučajno unesene sate
- [ ] stariji CSV bez `boja/pocetak/kraj/pauza_min` i dalje radi
- [ ] JSON backup se može izvesti i vratiti
- [ ] JSON sigurnosna kopija schema v10 vraća radno vrijeme, potvrđene prekovremene, standardni radni dan, mjesečne fondove, profil plaće i predloške vremena
- [ ] schema v10 vraća vlastite brze oznake, vlastite uzorke, boje, profil i postavke podsjetnika prije smjene
- [ ] starije podržane JSON sheme 1–9 i dalje se prihvaćaju
- [ ] CSV uvoz nudi čuvanje ili prepisivanje postojećih datuma
- [ ] JSON restore nudi spajanje ili potpunu zamjenu
- [ ] malformed CSV s nezatvorenim navodnicima odbija se
- [ ] novija nepodržana JSON schema odbija se
- [ ] Android Share izbornik dobiva CSV tekst rasporeda
- [ ] iCalendar izvoz stvara valjanu `.ics` datoteku s `VCALENDAR` i `VEVENT` zapisima
- [ ] radna smjena s vremenom izvozi `DTSTART` / `DTEND`
- [ ] noćna smjena izvozi `DTEND` na sljedeći kalendarski dan
- [ ] GO / BO / PD bez vremena izvoze se kao cjelodnevni događaji
- [ ] zarez, točka-zarez, novi red, backslash i hrvatski znakovi ostaju valjani nakon iCalendar escapinga/prelamanja

## Statistika
- [ ] D + N daju broj radnih smjena
- [ ] GO / BO / PD imaju odvojene brojače
- [ ] slobodni dani računaju se iz praznih datuma u mjesecu
- [ ] vlastite oznake imaju ukupni brojač i breakdown po kodu
- [ ] ukupni radni sati zbrajaju samo dane s evidentiranim vremenom
- [ ] "Iznad dnevnog standarda" ostaje samo kontrolna metrika
- [ ] "Višak iznad fonda" ostaje samo kontrolna metrika
- [ ] kartica "Priznati prekovremeni" prikazuje samo minute izričito spremljene uz radne dane
- [ ] redovni sati ne uključuju potvrđene prekovremene
- [ ] prosječno trajanje smjene koristi samo dane s evidentiranim vremenom
- [ ] godišnji trend sati prikazuje 12 mjeseci i odgovara evidenciji

## Podsjetnici
- [ ] Android 13+ traži notification permission
- [ ] dnevni i smjenski podsjetnik mogu se uključivati/isključivati neovisno
- [ ] isključeni dnevni podsjetnik ne gasi uključeni podsjetnik prije smjene
- [ ] isključeni smjenski podsjetnik ne gasi uključeni dnevni pregled
- [ ] smjenski podsjetnik se zakazuje samo za dane s početkom i krajem
- [ ] odmak 0 / 15 / 30 / 60 / 120 / 240 / 720 / 1440 min radi
- [ ] promjena datuma, oznake ili vremena sljedeće smjene ponovno sinkronizira alarm
- [ ] brisanje sljedeće smjene ne ostavlja zastarjeli alarm
- [ ] receiver ne prikazuje alarm ako očekivani početak više ne odgovara zapisu
- [ ] nakon okidanja smjenskog podsjetnika zakazuje se sljedeća buduća smjena
- [ ] ako je željeni lead trenutak već prošao, ali smjena još nije počela, zakazuje se brzi zakašnjeli podsjetnik
- [ ] isključeni podsjetnici ne zakazuju alarm
- [ ] promjena vremena ponovno zakazuje alarm
- [ ] slobodan dan ne prikazuje obavijest
- [ ] dan s unosom prikazuje kod, naziv i napomenu
- [ ] dan s radnim vremenom prikazuje početak i kraj u obavijesti
- [ ] dodir obavijesti otvara točan današnji datum u kalendaru
- [ ] reboot, promjena vremena i vremenske zone ponovno postavljaju oba alarma
- [ ] dodir podsjetnika prije smjene otvara točan datum konkretne smjene

## Vizualni QA
- [ ] nema preklapanja na manjim zaslonima
- [ ] veliki fontovi ne režu D / N / GO / BO / PD
- [ ] vlastite oznake od 4+ znakova ostaju čitljive u kalendaru
- [ ] promjena boja vidljiva je u kalendaru, statistici i novim unosima
- [ ] dijalog Radno vrijeme ostaje upotrebljiv pri većem system font scaleu
- [ ] Undo ikona ima jasan disabled/enabled status i pristupačan opis
- [ ] tamna tema ostaje čitljiva pri većem system font scaleu

## UI/UX 0.1.16 regresija
- [ ] Početna se može skrolati na manjim ekranima i pri velikom fontu
- [ ] mjesečne metrike na Početnoj prelaze u jedan stupac kada nema dovoljno širine
- [ ] onboarding je skrolabilan, poštuje safe drawing insets i ne vraća korisnika na prvu stranicu nakon rekreacije Activityja
- [ ] odabrana donja kartica ostaje ista nakon rekreacije Activityja
- [ ] Kalendar čuva mjesec i odabrani datum nakon rekreacije Activityja
- [ ] Statistika čuva odabrani mjesec nakon rekreacije Activityja
- [ ] vrlo svijetle vlastite boje koriste tamni tekst, a tamne boje svijetli tekst
- [ ] tekst ostaje čitljiv na vlastitim bojama u Kalendaru, Početnoj, pretrazi, Statistici i Postavkama
- [ ] ugrađene oznake u Postavkama ne sabijaju se kada ih ima više, nego se vodoravno skrolaju
- [ ] brisanje jednog kalendarskog unosa traži potvrdu
- [ ] brisanje više odabranih dana traži potvrdu i može se vratiti jednim Undo korakom
- [ ] brisanje vlastite brze oznake traži potvrdu i ne briše postojeće kalendarske zapise
- [ ] brisanje spremljenog uzorka traži potvrdu i ne mijenja već primijenjene dane
- [ ] uklanjanje zadanog radnog vremena traži potvrdu
- [ ] uklanjanje radnog vremena s pojedinog dana traži potvrdu i ne briše oznaku/napomenu
- [ ] primjena uzorka s prepisivanjem traži dodatnu potvrdu
- [ ] lijepljenje tjedna jasno razlikuje "bez prepisivanja" i "prepiši"
- [ ] skener rasporeda omogućuje pomicanje izreza po 2 % radi preciznog poravnanja
- [ ] skener rasporeda omogućuje promjenu veličine preko sva četiri ruba i sva četiri kuta
- [ ] editor slike ostaje skrolabilan na manjim ekranima i pri većem fontu
- [ ] rotacija −90°/+90°, "Jedan red" i "Označi cijelu sliku" i dalje rade
- [ ] svi pronađeni dani skena mogu se pregledati i ispraviti prije uvoza
- [ ] raspored s više osoba ne uvozi drugu osobu kada ciljna osoba nije sigurno pronađena

## Dead-code / održavanje 0.1.16
- [ ] nema referenci na uklonjeni TaktoAmbientBackground
- [ ] MonthCalendar nema neupotrebljivi compact način prikaza
- [ ] formatiranje eura koristi zajednički helper umjesto tri duplicirane funkcije
- [ ] nema nepotrebnih UI importa otkrivenih auditom
- [ ] novi helperi za kontrast, crop i responzivni layout imaju unit testove
