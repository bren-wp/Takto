# Changelog

## 0.1.17

Daljnje poliranje interakcija, pristupačnosti i sigurnosti uvoza rasporeda, uz dodatni dead-code i no-op audit.

### UI i UX

- alatna traka Kalendara automatski prelazi u dva retka na uskim ekranima i pri povećanom fontu
- svih pet akcija Kalendara ostaje dostupno bez horizontalnog sabijanja ili rezanja
- stanje višestrukog odabira ima jasniji opis uključivanja i isključivanja
- naslov mjeseca izložen je pristupačnim alatima kao eksplicitna akcija za odabir drugog mjeseca

### Skeniranje rasporeda

- postojeći kalendarski unosi više se ne prepisuju po zadanim postavkama
- prije uvoza se prikazuje broj novih i već popunjenih datuma
- svaki već popunjeni datum jasno je označen u pregledu skena
- gumb za završni uvoz jasno razlikuje siguran uvoz bez zamjene od uvoza s prepisivanjem
- prepisivanje postojećih datuma traži dodatnu potvrdu
- skenirane oznake automatski koriste svijetli ili tamni tekst ovisno o boji oznake

### Stabilnost i dead-code audit

- uklonjen neupotrebljivi `exportArchiveJsonLines()` helper
- slobodni dan iz skena koji je već prazan više ne stvara lažni "uvezeni" unos ni nepotrebnu Undo reviziju
- poruka nakon skeniranog uvoza sada govori koliko je datuma stvarno promijenjeno

### Testovi

- dodani regresijski testovi za responzivnu alatnu traku Kalendara na uskim ekranima i pri velikom fontu

### Verzija

- `versionName`: `0.1.17`
- `versionCode`: `18`


## 0.1.16

Veliki UI/UX i dead-code polish: bolja responzivnost, sigurnije destruktivne radnje, pristupačniji kontrast vlastitih boja, precizniji editor skeniranja i čišći kod.

### UI i UX

- Početna je skrolabilna i mjesečne metrike se preslaguju na malim ekranima i pri većem fontu
- onboarding poštuje safe drawing insets, skrolabilan je i čuva trenutnu stranicu nakon rekreacije Activityja
- odabrana donja kartica, odabrani datum/mjesec Kalendara i mjesec Statistike čuvaju se nakon rekreacije Activityja
- vlastite svijetle/tamne boje automatski dobivaju čitljiv kontrast teksta u Kalendaru, Početnoj, Statistici, pretrazi i Postavkama
- ugrađene oznake u Postavkama više se ne sabijaju nego se prikazuju u vodoravno skrolabilnom retku
- radno vrijeme, mjesečni fond i dijalozi uzoraka prilagođeni su manjim ekranima i velikom fontu
- kontrolne metrike više se ne nazivaju prekovremenima: odvojeni su "Višak iznad fonda", "Iznad dnevnog standarda" i stvarni "Priznati prekovremeni"

### Sigurnost interakcija

- potvrda prije brisanja jednog ili više kalendarskih unosa
- potvrda prije brisanja vlastite brze oznake ili spremljenog uzorka
- potvrda prije uklanjanja radnog vremena i predloška zadanog vremena
- dodatna potvrda prije primjene uzorka s prepisivanjem postojećih unosa
- lijepljenje tjedna jasno navodi hoće li prepisati postojeće unose

### Skeniranje rasporeda

- editor izreza sada ima osam hvatišta: četiri kuta i četiri sredine ruba
- područje se može sužavati/širiti zasebno s lijeve, desne, gornje i donje strane
- pomicanje tipkama je finije, u koraku 2 %, za preciznije označavanje jednog retka
- editor izreza je skrolabilan na manjim zaslonima

### Dead-code i održavanje

- uklonjena neupotrebljiva komponenta `TaktoAmbientBackground`
- uklonjen neupotrebljivi `compact` način iz `MonthCalendar`
- uklonjeni nepotrebni importi pronađeni auditom
- tri duplicirana formattera eura zamijenjena zajedničkim helperom
- dodani testovi za kontrast vlastitih boja, crop rubove, responzivne metrike i zajedničko formatiranje eura
- QA checklist usklađen je s aktualnom backup shemom v10 i ponašanjem potvrđenih prekovremenih

### Verzija

- `versionName`: `0.1.16`
- `versionCode`: `17`


## 0.1.15

Daljnje poboljšanje skeniranja stvarnih rasporeda s više zaposlenika, sigurniji odabir mjeseca, ručna korekcija pronađenih unosa i brži unos potvrđenih prekovremenih sati.

### Novo i poboljšano

- skener može koristiti ime i prezime iz radnog profila te izdvojiti samo red odabrane osobe iz rasporeda s više zaposlenika
- podudaranje imena tolerira hrvatske dijakritičke znakove
- ako osoba nije sigurno pronađena, Takto ne uvozi raspored druge osobe
- podržan je slučaj kada je ime u jednom retku, a oznake smjena u sljedećem retku
- ako mjesec nije prepoznat na slici, uvoz je blokiran dok korisnik ručno ne odabere i potvrdi točan mjesec
- pronađeni unos sada se može urediti prije uvoza: oznaka smjene, početak, kraj, pauza ili slobodan dan
- pogrešno pronađen unos može se ukloniti iz pregleda bez ponovnog skeniranja cijele slike
- galerijske slike zadržavaju više detalja za sitni tekst u velikim tablicama
- dijalog za korekciju skena prilagođen je malim ekranima i većem fontu
- dodani su brzi izbori za potvrđene prekovremene sate: 0, 30, 60 i 120 minuta
- brzi izbori prekovremenih raspoređuju se u čitljiv 2 × 2 raspored na manjim ekranima
- svi pronađeni dani iz skena prikazuju se prije uvoza; nema skrivenih unosa nakon prvih 18
- redovni sati sada izričito isključuju potvrđene prekovremene kako se isti sati ne bi prikazivali u obje kategorije
- Početna prikazuje potvrđene prekovremene, a kada je obračun potpun i procjenu isplate, neto i bruto iznosa
- tekst za standardni radni dan jasno razlikuje kontrolnu metriku od prekovremenih koji stvarno ulaze u obračun plaće
- dodani regresijski testovi za izdvajanje jedne osobe, hrvatske dijakritike, odvojeni red imena i oznaka te ručno zadani mjesec

### Verzija

- `versionName`: `0.1.15`
- `versionCode`: `16`


## 0.1.14

Pouzdanije skeniranje rasporeda, potvrđeni prekovremeni sati i precizniji obračun plaće za državne i javne službe, uz završno poliranje mobilnog UX-a.

### Novo i poboljšano

- skeniranje kamerom i uvoz iz galerije koriste isti postupak označavanja područja prije prepoznavanja
- korisnik može izrezati samo svoj red rasporeda, pomicati okvir prstom i mijenjati mu veličinu
- dodane su zasebne kontrole **Lijevo**, **Gore**, **Dolje** i **Desno** za precizno pomicanje područja na malom ekranu
- dodana rotacija slike u oba smjera: **−90°** i **+90°**
- gumb **Jedan red** vraća praktični početni okvir, a **Označi cijelu sliku** ostaje dostupan kada je raspored samo za jednu osobu
- dvosmisleni rezultati za isti datum blokiraju uvoz umjesto da miješaju podatke više osoba
- pregled prepoznatog rasporeda ostaje obavezan prije upisa u Kalendar
- slobodni dani iz skeniranog rasporeda obrađuju se kao slobodni dani, bez stvaranja lažnih oznaka
- svaki radni unos može sadržavati potvrđene prekovremene minute
- potvrđeni prekovremeni prikazuju se u Kalendaru i zasebno u Statistici
- samo potvrđeni prekovremeni ulaze u obračun plaće; duga smjena se ne proglašava automatski prekovremenom
- obračun plaće podržava državnu službu, javne službe i ostale sustave s vlastitim pravilima
- za 2026. koriste se službene osnovice po razdobljima, uz dodatak za navršene godine staža
- obračun podržava noćni rad, subotu, nedjelju, blagdan i potvrđeni prekovremeni rad
- za ostale sustave postotci se ne pretpostavljaju nego ih korisnik unosi
- potpuni neto izračun se ne prikazuje kada nedostaju porezni podaci, radno vrijeme, podaci o bolovanju ili potvrda svih primjenjivih dodataka
- podržani su MIO I./II. stup, zakonsko umanjenje osnovice i bruto kod drugih poslodavaca
- uklonjeni su preostali tehnički izrazi iz korisničkih tekstova za sigurnosne kopije i povijest promjena
- verzija i CI artefakti ispravljeni su na stvarni **0.1.14**

### Verzija

- `versionName`: `0.1.14`
- `versionCode`: `15`


## 0.1.13

Veliki UX/IA završni zahvat prema testiranju na stvarnim Android uređajima: uravnotežene teme, fokusirana Početna, veći Kalendar, čišći Uzorci i lokalni uvoz rasporeda sa slike.

### Novo i poboljšano

- tamna tema više nije gotovo crna; koristi svjetliju navy hijerarhiju s jasnijim površinama i obrubima
- svijetla tema ima jači kontrast teksta, kartica i kontrola umjesto ispranog bijelog izgleda
- nove instalacije prema zadanim postavkama prate sistemsku temu, uz ručni izbor tamne ili svijetle teme
- uklonjeni su teški aurora i glass gradient slojevi iz glavnog UI-ja
- Početna više nije dugi scroll svih modula nego fokusirani dashboard: Danas, mjesečni fond i ulaz u Kalendar
- mini-kalendar, tjedni feed i ponovljeni sekundarni blokovi uklonjeni su s Početne jer imaju vlastite tabove
- Kalendar dobiva više horizontalnog prostora, veće datume, veće oznake i čišće ćelije
- Uzorci više ne crtaju nizove poput J J J J J kao odvojene pločice; prikazuju sažetak poput J × 5
- onboarding koristi čistu temu bez zasebnog tamnog gradijenta
- dodano skeniranje rasporeda kamerom i uvoz slike iz galerije
- OCR prepoznaje datume, oznake, mjesec/godinu i zapisano radno vrijeme, a prije uvoza prikazuje pregled
- OCR tekst se obrađuje lokalno; uvoz se potvrđuje prije izmjene Kalendara
- ako na slici nema vremena, koristi se spremljeno zadano radno vrijeme prepoznate oznake kada postoji
- prepoznati raspored upisuje se skupno uz Undo i opciju čuvanja postojećih unosa
- automatski mjesečni fond sada izuzima hrvatske blagdane koji padaju ponedjeljak–petak
- statistika zasebno prikazuje rad subotom, nedjeljom i na hrvatski blagdan
- dodani testovi za hrvatske fiksne i pomične blagdane te OCR parser rasporeda
- postojeći rasporedi, backup/import format i korisničke oznake ostaju kompatibilni
- aplikacija i dalje ne deklarira INTERNET dopuštenje

### Verzija

- `versionName`: `0.1.13`
- `versionCode`: `14`


## 0.1.12

Responzivniji i pristupačniji Kalendar za male Android ekrane, veći sistemski font i vlastite oznake.

### Poboljšano

- mreža oznaka u donjem listu Kalendara prelazi iz dva stupca u jedan kada je širina manja od 360 dp
- isti jednokolonski raspored aktivira se pri font scaleu 1.30 ili većem
- kartice oznaka više nemaju fiksnu visinu od 84 dp nego minimalnu visinu, pa se mogu proširiti bez rezanja duljih naziva
- polja početka i kraja radnog vremena prelaze u vertikalni raspored na uskim ekranima i pri velikom fontu
- paleta vlastitog unosa više nije šest skučenih kontrola u jednom retku nego dva retka po tri boje
- svaka kontrola boje ima 48 dp visinu, TalkBack naziv boje, radio-button ulogu i stanje odabira
- responzivna pravila izdvojena su u testabilni `CalendarUiLogic`
- dodani unit testovi za graničnu širinu, veliki font i sigurne fallback vrijednosti
- storage, backup/import format i postojeći korisnički podaci ostaju nepromijenjeni
- aplikacija i dalje nema INTERNET dopuštenje
- CI artefakti nose točnu oznaku verzije 0.1.12

### Verzija

- `versionName`: `0.1.12`
- `versionCode`: `13`


## 0.1.11

Pristupačnija i stabilnija kartica **Danas** na malim Android ekranima i pri većem fontu.

### Poboljšano

- brze akcije za današnji unos prelaze iz dva stupca u jedan kada je raspoloživa širina manja od 360 dp
- isti jednokolonski raspored aktivira se pri font scaleu 1.30 ili većem kako tekst i touch targeti ne bi bili stisnuti
- brzi gumbi više nemaju fiksnu visinu nego minimalnu visinu, pa se mogu proširiti bez rezanja sadržaja
- svaka brza akcija dobila je eksplicitni TalkBack opis koji navodi radnju, naziv i oznaku
- spremljeni današnji unos dobio je objedinjeni TalkBack opis s datumom, oznakom, radnim vremenom, trajanjem i napomenom
- responzivna i accessibility pravila izdvojena su u testabilni `HomeTodayUiLogic`
- dodani unit testovi za širinu zaslona, veliki font, fallback nevaljanih mjerenja i semantičke opise
- postojeći lokalni storage, backup format i podaci ostaju nepromijenjeni
- aplikacija i dalje nema INTERNET dopuštenje
- CI artefakti nose točnu oznaku verzije 0.1.11

### Verzija

- `versionName`: `0.1.11`
- `versionCode`: `12`


## 0.1.10

Točnija i čitljivija Statistika, posebno na praznim godinama i uskim Android ekranima.

### Poboljšano

- godišnji graf više ne crta minimalni obojeni stupac za mjesece s vrijednošću 0
- godina bez ijednog unosa prikazuje jasno prazno stanje umjesto grafikona bez podataka
- godina bez evidentiranih radnih sati prikazuje zasebno prazno stanje
- mjesečni radni sati više ne gube minute zbog cjelobrojnog prikaza u satima
- kompaktne oznake prikazuju npr. `30m`, `1h` i `1h30`
- logika visine grafikona izdvojena je u testabilni `StatsChartLogic`
- samo pozitivne vrijednosti dobivaju minimalnu vidljivu visinu stupca
- ključne kartice fonda i posebnih radnih sati prelaze u vertikalni raspored na uskim ekranima
- donut graf i legenda prelaze u čitljiv vertikalni raspored na uskim ekranima
- godišnji stupci dobili su TalkBack opise s mjesecom i stvarnom vrijednošću
- dodani unit testovi za nulte vrijednosti, skaliranje i preciznost minuta
- CI artefakti nose točnu oznaku verzije 0.1.10

### Verzija

- `versionName`: `0.1.10`
- `versionCode`: `11`


## 0.1.9

Pouzdaniji i brži unos radnog vremena s centraliziranom validacijom početka, kraja i pauze.

### Poboljšano

- vrijeme se može upisati kao `07:30`, `7.30`, `18,45`, `730` ili `0730`
- uvedena je zajednička validacija radnog vremena u `ScheduleLogic`
- pojedinačno, bulk i zadano radno vrijeme po oznaci koriste ista pravila
- prevelika pauza više se ne ograničava tiho na drugu vrijednost
- pauza mora biti kraća od bruto raspona rada i unutar dopuštenog maksimuma
- jednak početak i kraj više se jasno prijavljuju kao nevaljan rad od 0 minuta
- rad preko ponoći jasno prikazuje da završetak pripada sljedećem danu
- dijalozi nude brze pauze 0, 15, 30, 45 i 60 minuta
- prikazuje se neto trajanje prije spremanja
- zadana vremena po oznakama koriste isti UX i validacijska pravila kao kalendar
- dodani unit testovi za kompaktni unos vremena, prelazak ponoći, bruto trajanje i nevaljane pauze
- postojeći spremljeni podaci ostaju kompatibilni bez destruktivne migracije
- CI artefakti nose točnu oznaku verzije 0.1.9

### Verzija

- `versionName`: `0.1.9`
- `versionCode`: `10`


## 0.1.8

Pametnija lokalna pretraga rasporeda s boljim hrvatskim datumskim kontekstom, radnim vremenom i semantičkim upitima.

### Poboljšano

- pretraga prepoznaje hrvatske nazive mjeseci i kombinacije mjesec + godina
- moguće je pretraživati dane u tjednu, npr. `petak`
- početak i kraj radnog vremena ulaze u lokalni indeks pa upit poput `07:00` pronalazi odgovarajuće unose
- dodani su relativni izrazi `prekosutra`, `preksutra` i `prekjučer`
- tekstualni upiti `buduće`, `s vremenom`, `radno vrijeme` i `s napomenom` rade bez ručnog uključivanja filtra
- rangiranje dodatno nagrađuje višerječne upite koji se podudaraju kroz oznaku, naziv, napomenu, datum ili vrijeme
- točna kratica i dalje ima prednost pred slabim podudaranjem u napomeni
- dijakritička tolerancija za č/ć/š/ž/đ ostaje očuvana
- dijalog pretrage sada jasno navodi podržane primjere poput `listopad 2026`, `petak` i `07:00`
- dodani regresijski unit testovi za mjesec/godinu, dan u tjednu, radno vrijeme, semantičke filtre i proširene relativne datume
- CI artefakti nose točnu oznaku verzije 0.1.8

### Verzija

- `versionName`: `0.1.8`
- `versionCode`: `9`


## 0.1.7

Brže pokretanje s velikom dugoročnom arhivom rasporeda uz potpuno očuvanje postojeće recovery kompatibilnosti.

### Poboljšano

- checkpoint snimke sada pamte sigurni byte-offset u append-only revizijskoj arhivi
- pri pokretanju se, kada je cursor valjan, čita samo rep arhive noviji od checkpointa umjesto prolaska kroz sve stare revizije
- offset se koristi samo ako je unutar datoteke, broj checkpoint revizija nije ispred aktualnog journala i offset završava na granici retka
- kod nevaljanog ili starog snapshot formata automatski se koristi postojeći kompatibilni fallback prema broju revizija
- snapshot schema podignuta je na verziju 2 bez gubitka kompatibilnosti sa schema 1 datotekama
- append-only povijest se ne briše niti skraćuje; optimizacija ne ugrožava dugoročnu arhivu
- dodani unit testovi za valjani cursor, offset izvan datoteke, checkpoint ispred journala i neispravnu granicu retka
- CI artefakti nose točnu oznaku verzije 0.1.7

### Verzija

- `versionName`: `0.1.7`
- `versionCode`: `8`


## 0.1.6

Preglednija Početna, kvalitetnija prazna stanja i bolja pristupačnost na malim Android ekranima.

### Poboljšano

- kartica **Danas** jasno prikazuje stanje **Unos spremljen** ili **Nema unosa**
- današnja napomena može prikazati dva retka bez nepotrebnog rezanja teksta
- tjedni sažetak prilagođava raspored kartica uskim ekranima umjesto stiskanja tri metrike u jedan red
- tjedni pregled koristi kompaktnije dimenzije i tipografiju na uskim ekranima
- svaki dan u tjednom pregledu ima TalkBack opis s datumom i stvarnim unosom ili informacijom da unosa nema
- prazno stanje budućeg rasporeda odmah navodi konkretan sljedeći datum koji korisnik može planirati
- mjesečni pregled jasno objašnjava kada mjesec još nema nijedan unos
- zadržane su adaptivne brze oznake i neutralna terminologija rasporeda
- raspored sažetaka eksplicitno koristi punu dostupnu širinu radi stabilnog Compose mjerenja na malim ekranima
- CI artefakti nose točnu oznaku verzije 0.1.6

### Verzija

- `versionName`: `0.1.6`
- `versionCode`: `7`

## 0.1.5

Brža i jednako trajna pohrana velikih rasporeda bez prepisivanja cijele snimke pri svakoj maloj izmjeni.

### Poboljšano

- append-only revizijska arhiva sada je primarni trajni zapis između checkpointa
- pojedinačna uređivanja više ne serijaliziraju cijeli višegodišnji raspored nakon svakog dodira
- puna atomska snimka konsolidira se nakon 64 revizije
- veliki bulk zahvati od 32 ili više promijenjenih datuma odmah rade checkpoint
- journal se prije potvrde trajnosti eksplicitno sinkronizira na disk
- pri pokretanju se uspoređuju glavna i recovery snimka te se bira ona s novijim checkpointom
- postojeći replay revizija i nedestruktivni recovery ostaju kompatibilni
- legacy SharedPreferences migracija i stari backup format ostaju podržani
- emergency SharedPreferences fallback sada ima eksplicitni marker i ne može biti zasjenjen starijim checkpointom nakon rijetkog I/O kvara
- dodani unit testovi za checkpoint politiku
- CI artefakti nose točnu oznaku verzije 0.1.5

### Verzija

- `versionName`: `0.1.5`
- `versionCode`: `6`

## 0.1.4

Pouzdanija dugoročna pohrana, automatski oporavak i zaštita velikih rasporeda.

### Poboljšano

- raspored više ne ovisi o jednom velikom JSON stringu u SharedPreferences
- uvedena je atomska glavna snimka `takto_schedule_current.json`
- uvedena je pričuvna snimka `takto_schedule_recovery.json`
- prije zamjene glavne snimke prethodno potvrđeno stanje kopira se streaming načinom u recovery datoteku
- glavna snimka koristi Android `AtomicFile` kako prekid procesa tijekom pisanja ne bi ostavio napola zapisanu datoteku
- postojeće instalacije automatski migriraju stari `entries_json` raspored u novi format
- legacy SharedPreferences raspored briše se tek nakon uspješnog zapisa glavne i recovery snimke
- append-only revizijska arhiva ostaje dodatni sloj zaštite
- snimke pamte broj već ugrađenih revizija pa se pri pokretanju obrađuju samo novije promjene
- ako je proces prekinut nakon zapisa revizije, ali prije završetka snimke, zadnja promjena automatski se vraća pri sljedećem pokretanju
- oporavak koristi `before/after` provjeru i ne prepisuje divergentno novije lokalno stanje starijom revizijom
- provjerava se deklarirani broj zapisa u snimci prije prihvaćanja datoteke
- veličina lokalne snimke strogo je ograničena prije učitavanja
- Android cloud backup i device transfer uključuju glavnu snimku, recovery snimku i revizijsku arhivu
- Postavke sada jasno prikazuju zaštitu lokalnih podataka i broj rasporednih unosa
- uklonjene su preostale formulacije koje nepotrebno pretpostavljaju noćne smjene
- dodani unit testovi za nedestruktivno rješavanje revizijskih konflikata
- broj revizija i duljina arhive spremaju se kao metadata cache pa veliki journal ne mora biti ponovno potpuno prebrojan pri svakom pokretanju

### Verzija

- `versionName`: `0.1.4`
- `versionCode`: `5`

## 0.1.3

Brži dnevni unos, pametnija pretraga i preciznije adaptivne preporuke.

### Poboljšano

- Početna sada uvijek ima jasnu karticu **Danas**
- kada današnji unos ne postoji, do četiri najrelevantnije oznake mogu se dodati jednim dodirom bez otvaranja kalendara
- kada današnji unos postoji, kartica prikazuje oznaku, radno vrijeme i napomenu te vodi izravno na uređivanje tog datuma
- budući raspored ima jasno prazno stanje i akciju **Planiraj sljedeći dan**
- sljedeći budući unos više se ne duplicira s današnjim unosom na Početnoj
- preporuke oznaka koriste ponderiranu učestalost i svježinu korištenja: zadnjih 7 dana imaju najveću težinu, zatim 30 i 90 dana
- kod jednakog rezultata prednost ima novije korištena oznaka
- kalendarska pretraga je izdvojena u testiranu logiku i ignorira dijakritičke znakove
- pretraga podržava izraze **danas**, **sutra** i **jučer**
- podržani su ISO i uobičajeni hrvatski datumi, uključujući `02.10.2026.`
- dodani filtri **Danas**, **Buduće**, **S vremenom** i **S napomenom**
- rezultati pretrage rangiraju točne kratice i nazive ispred slabijih podudaranja
- višerječne pretrage mogu kombinirati naziv i napomenu, npr. `teren rijeka`
- dodani unit testovi za pametnu pretragu i recency-aware rangiranje oznaka
- release workflow sada na novoj verziji u `main` automatski gradi, provjerava i objavljuje odgovarajući `vX.Y.Z` GitHub Release bez dupliciranja postojećih izdanja

### Toolchain

- Kotlin 2.4.20
- Android Gradle Plugin 9.4.1
- Gradle 9.8.0
- Jetpack Compose BOM 2026.09.00
- compileSdk 37.1
- targetSdk 37

### Verzija

- `versionName`: `0.1.3`
- `versionCode`: `4`

## 0.1.2

Adaptivni UI/UX, pristupačnost i daljnja generalizacija Takto rasporeda.

### Poboljšano

- tamni način je primarni zadani Takto izgled; svijetli i sistemski način ostaju dostupni
- brzi odabir oznaka u kalendaru više nije fiksno vezan uz D/N/GO/BO/PD
- Takto rangira spremljene oznake prema korištenju u posljednjih 90 dana
- vlastite oznake bez prethodne uporabe imaju prednost pred nekorištenim ugrađenim oznakama
- pojedinačno i višestruko uređivanje koriste isti adaptivni odabir oznaka
- statistika se dinamički gradi iz svih stvarnih oznaka korisnika
- trendovi oznaka sada broje sve unose, a ne samo D i N
- početni sažetak više ne koristi specifičnu metriku odsutnosti nego broj dana bez unosa
- podsjetnici i notification channeli koriste općenitiji izraz „rad” umjesto pretpostavke smjenskog rada
- prazni dani u kalendaru i uzorcima opisani su kao dani bez unosa
- poboljšane TalkBack semantike za tipke oznaka
- duge vlastite kratice bolje se skaliraju u statistici
- dodani unit testovi za spremanje i zadani odabir teme
- vizualni graditelj uzoraka koristi najrelevantnije korisničke oznake umjesto unaprijed nametnutih D/N predložaka
- ugrađeni prijedlozi uzoraka dinamički se grade iz najčešće korištenih oznaka
- CSV parser obrađuje retke sekvencijalno i izbjegava velike privremene kolekcije
- CSV i backup datoteke čitaju se uz strogo ograničenje veličine prije potpune alokacije sadržaja
- izvoz trajne arhive zapisuje podatke izravno u odabranu datoteku bez učitavanja cijele arhive u memoriju
- Android Back iz Kalendar/Statistika/Uzorci/Više vraća korisnika na Početnu prije izlaska iz aplikacije
- notification ikona pojednostavljena je na prepoznatljivi Takto T kako bi bila čitljiva u Android statusnoj traci

### Toolchain

- Kotlin 2.4.20
- Android Gradle Plugin 9.4.1
- Gradle 9.8.0
- Jetpack Compose BOM 2026.09.00
- compileSdk 37.1
- targetSdk 37

### Verzija

- `versionName`: `0.1.2`
- `versionCode`: `3`

## 0.1.1

UI/UX, stabilnost i branding izdanje.

### Poboljšano

- tamna tema je svjetlija i čitljivija uz veći kontrast teksta, kartica i obruba
- uveden svijetli način i opcija praćenja izgleda sustava
- onboarding više nije fokusiran samo na dnevne i noćne smjene nego na bilo koji oblik rada i rasporeda
- početna stranica koristi općenitije metrike: upisani dani, evidentirani sati i odsutnosti
- pozdrav se mijenja prema dobu dana: jutro, dan, večer i noć
- mjesečni fond na početnoj prikazuje redovne i prekovremene sate
- statistika koristi općenitiji jezik i bolje podržava vlastite oznake
- postavke su podijeljene u jasne cjeline
- dodani Brendigo, brendigo.com i info@brendigo.com u podršku
- redizajnirana adaptivna ikona aplikacije i dodana monochrome/themed ikona
- poboljšana podrška za svijetli i tamni prikaz kroz zajedničke Compose komponente
- zabranjen cleartext mrežni promet
- targetSdk podignut na Android 17 / API 37
- očuvana kompatibilnost sa spremljenim rasporedima, profilom i arhivom

### Toolchain

- Kotlin 2.4.20
- Android Gradle Plugin 9.4.1
- Gradle 9.8.0
- Jetpack Compose BOM 2026.09.00
- AndroidX Core 1.19.1
- Activity Compose 1.13.0
- Lifecycle 2.11.0
- compileSdk 37.1
- targetSdk 37

### Verzija

- `versionName`: `0.1.1`
- `versionCode`: `2`

## 0.1.0

Početno izdanje Takto aplikacije u repozitoriju `bren-wp/Takto`.

### Uključeno

- premium tamni Android UI prema Takto referentnim vizualima
- onboarding, Početna, Kalendar, Statistika, Uzorci i Postavke
- D / N / GO / BO / PD i vlastiti unosi
- personalizacija oznaka i boja
- višestruki odabir, bulk uređivanje i kopiranje tjedna
- napomene, radno vrijeme, pauze, fond sati i prekovremeni rad
- statistike i vlastiti uzorci
- dnevni i smjenski podsjetnici
- CSV, JSON backup i iCalendar izvoz
- trajna append-only arhiva svih promjena rasporeda
- praktična navigacija 100 godina unatrag i 100 godina unaprijed
- radni profil: ime i prezime, sektor, djelatnost, ustanova i pozicija
- J i SD brze oznake iz referentnog rasporeda te automatsko čuvanje nepoznatih uvezenih kratica
- mjesečni fond, redovni sati i prekovremeni sati prema fondu
- lokalno spremanje bez INTERNET dopuštenja
- unit testovi i GitHub Actions provjera bez vlastitih Secrets varijabli

### Verzija

- `versionName`: `0.1.0`
- `versionCode`: `1`
