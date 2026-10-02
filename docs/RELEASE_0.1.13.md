# Takto 0.1.13

Takto 0.1.13 je veliki završni UX i funkcionalni zahvat napravljen nakon pregleda aplikacije na stvarnim Android uređajima.

## Sučelje

- uravnotežena tamna tema bez gotovo crnih površina
- svijetla tema s jasnijim kontrastom i manje ispranim karticama
- sistemska tema je zadana za nove instalacije
- uklonjeni teški glass/aurora slojevi
- Početna je fokusirani dashboard bez dugog skrolanja kroz Kalendar, Statistiku i ostale module
- Kalendar ima više prostora, veće ćelije, veće datume i čitljivije oznake
- ponovljene oznake u Uzorcima sažete su u oblik poput `J × 5 · SD × 2`
- onboarding je vizualno jednostavniji i jasniji

## Skeniranje rasporeda

- skeniranje rasporeda kamerom
- uvoz slike iz galerije
- OCR prepoznavanje datuma, oznaka, mjeseca/godine i zapisanog radnog vremena
- pregled prepoznatih stavki prije uvoza
- skupni unos u Kalendar uz Undo i kontrolu prepisivanja postojećih dana
- kada OCR ne pronađe vrijeme, može se koristiti spremljeno zadano radno vrijeme oznake
- lokalni OCR model za obradu odabrane slike; Takto i dalje ne deklarira INTERNET dopuštenje

## Obračun rada

- automatski mjesečni fond izuzima hrvatske blagdane koji padaju na radni dan
- podržani su fiksni i pomični hrvatski blagdani
- Statistika posebno računa rad na blagdan
- ostaju noćni rad, vikend, nedjelja, redovni sati, prekovremeni po fondu i dnevni prekovremeni

## Stabilnost

- dodani unit testovi za OCR parser i hrvatske blagdane
- postojeći lokalni rasporedi i backup podaci ostaju kompatibilni
- nema destruktivne migracije storagea
- release workflow ponovno provjerava testove, lint, APK, AAB i SHA256 prije objave

## Verzija

- `versionName = 0.1.13`
- `versionCode = 14`
