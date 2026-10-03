# Takto 0.1.18

Takto 0.1.18 donosi novi završni pass kroz izgled, početne smjene, obračun plaće i održavanje koda.

## Jedan svijetli izgled

- tamni način rada potpuno je uklonjen iz produkcijskog koda
- uklonjen je sistemski/automatski izbor teme
- uklonjene su postavke, perzistencija i testovi vezani uz stare teme
- Android window chrome i Compose paleta koriste isti svijetli vizualni sustav

## Oznake i radno vrijeme

- početne brze oznake su D i N
- ugrađene oznake su D, N, J, GO, SD, BO i PD
- svaka ugrađena oznaka ima zasebnu početnu boju
- D ima početni predložak 07:00–19:00
- N ima početni predložak 19:00–07:00
- oba predloška predstavljaju 12 sati rada i mogu se mijenjati
- SD je neradni status i ne računa se kao odrađena smjena
- 12-satni predlošci ne mijenjaju automatski mjesečni fond; fond ostaje zasebna obračunska postavka

## Plaća i “bodovanje”

Iz projekta `bren-wp/RASPORED` prenesen je referentni katalog odabranih radnih mjesta i koeficijenata za 2026. u zdravstvu, školstvu, državnoj službi i policiji.

- korisnik može pretraživati radna mjesta
- izbor preseta popunjava koeficijent
- prikazuju se naziv, sektor i oznaka radnog mjesta iz kataloga
- koeficijent se može ručno promijeniti kada konkretno radno mjesto ili akt poslodavca odstupa
- Takto ne izmišlja koeficijent za radna mjesta kojih nema u katalogu
- posebniji sustavi koji zahtijevaju druga pravila dodataka nisu automatski mapirani na pogrešan režim

## Dead-code i QA

- dodan je strogi, konzervativni audit privatnih Kotlin simbola
- audit se pokreće u Android CI-ju prije unit testova
- top-level Android entry pointovi poput BroadcastReceivera ostaju samo kandidati za ručni pregled i ne brišu se napamet
- prošireni su unit testovi za početne oznake, boje, SD i koeficijente kataloga

## Verzija

- `versionName = 0.1.18`
- `versionCode = 19`
