# Takto 0.1.0 — QA checklist

## Build i statičke provjere

- [ ] GitHub Actions `testDebugUnitTest` prolazi.
- [ ] GitHub Actions `lintDebug` prolazi.
- [ ] GitHub Actions `assembleDebug` prolazi.
- [ ] Debug APK artifact postoji i može se preuzeti iz workflow runa.

## Kalendar

- [ ] Mjesec se može mijenjati naprijed/natrag.
- [ ] D = Dan.
- [ ] N = Noć.
- [ ] GO = Godišnji odmor.
- [ ] BO = Bolovanje.
- [ ] PD = Plaćeni dopust.
- [ ] Prazna kućica ostaje slobodan dan.
- [ ] Vlastiti unos prihvaća proizvoljnu oznaku.
- [ ] Višestruki odabir dana radi bez gubitka podataka.
- [ ] Kopiranje/lijepljenje tjedna poštuje odabranu politiku prepisivanja.
- [ ] Undo vraća zadnju podržanu promjenu.

## Radno vrijeme i statistika

- [ ] Dnevna smjena pravilno računa sate i pauzu.
- [ ] Noćna smjena preko ponoći pravilno računa trajanje.
- [ ] GO/BO/PD ne ulaze u radne sate.
- [ ] Prekovremeni sati koriste postavljeni standardni radni dan.
- [ ] Noćni rad 22:00–06:00 pravilno se izračunava.
- [ ] Vikend i nedjeljni sati pravilno se razdvajaju.
- [ ] Mjesečni fond i razlika evidentirano/fond prikazuju se ispravno.

## Podsjetnici

- [ ] Android 13+ traži notification permission kada je potrebno.
- [ ] Dnevni podsjetnik otvara odgovarajući datum.
- [ ] Podsjetnik prije smjene koristi početak konkretne smjene.
- [ ] Boot/time/timezone promjena ponovno sinkronizira alarme.

## Uvoz i izvoz

- [ ] CSV uvoz radi s `,` i `;` separatorom.
- [ ] Hrvatski formati datuma rade.
- [ ] CSV izvoz čuva radno vrijeme i pauzu.
- [ ] JSON backup/restore čuva raspored i postavke.
- [ ] ICS izvoz pravilno izvozi cjelodnevne i vremenske događaje.

## Pristupačnost i UI

- [ ] Velike kalendarske kućice ostaju lako dodirljive.
- [ ] TalkBack čita datum i status kućice.
- [ ] Tekst se ne preklapa na uskim ekranima.
- [ ] Tamna tema ostaje čitljiva u svim glavnim zaslonima.
