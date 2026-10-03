# Takto 0.1.16

Takto 0.1.16 je veliki UI/UX, responzivnost i održavanje prolaz kroz cijelu aplikaciju. Fokus je na tome da svakodnevne radnje budu jasnije, sigurnije i pouzdanije na različitim Android ekranima i pri većem fontu.

## UI/UX

- Početna je skrolabilna i više ne može odrezati donji sadržaj na manjim ekranima
- mjesečne metrike prelaze u jedan stupac kada nema dovoljno širine
- onboarding poštuje safe drawing insets, skrolabilan je i pamti trenutni korak
- odabrana donja kartica ostaje sačuvana nakon rekreacije Activityja
- Kalendar pamti mjesec i odabrani datum, a Statistika odabrani mjesec
- dijalozi za radno vrijeme, mjesečni fond i uzorke prilagođeni su malim ekranima
- vrlo svijetle i vrlo tamne vlastite boje automatski dobivaju čitljiv foreground
- kontrolne metrike sati jasno su odvojene od potvrđenih prekovremenih koji ulaze u obračun plaće

## Sigurnije radnje

Takto sada traži potvrdu prije radnji koje mogu ukloniti ili prepisati podatke:

- brisanje jednog kalendarskog unosa
- brisanje više odabranih dana
- brisanje vlastite brze oznake
- brisanje spremljenog uzorka
- uklanjanje radnog vremena s dana
- uklanjanje predloška zadanog radnog vremena
- primjena uzorka uz prepisivanje postojećih unosa

Lijepljenje kopiranog tjedna sada u samom gumbu jasno pokazuje radi li se bez prepisivanja ili s prepisivanjem.

## Skeniranje rasporeda

- izrez ima četiri kutna i četiri rubna hvatišta
- može se zasebno pomicati lijevi, desni, gornji i donji rub
- tipke za pomicanje koriste finiji korak od 2 %
- kontrole pomicanja raspoređene su 2 × 2
- editor je skrolabilan na malim ekranima i pri velikom fontu
- postojeća rotacija −90° / +90°, jedan red i cijela slika ostaju dostupni

## Dead-code i refaktoriranje

- uklonjen je neupotrebljivi `TaktoAmbientBackground`
- uklonjen je neupotrebljivi `compact` način iz `MonthCalendar`
- uklonjeni su nepotrebni importi pronađeni auditom
- formatiranje iznosa u eurima centralizirano je u jedan helper umjesto tri kopije
- destruktivni confirmation UI centraliziran je u zajedničku komponentu
- QA dokumentacija ažurirana je na backup schema v10

## Testovi

Dodani su regresijski testovi za:

- čitljiv foreground na svijetlim i tamnim vlastitim bojama
- responzivno slaganje metrika na Početnoj
- pomicanje i promjenu veličine crop područja
- hrvatsko formatiranje iznosa u eurima

## Verzija

- `versionName = 0.1.16`
- `versionCode = 17`

## QA uvjet

Izdanje se spaja u `main` tek nakon uspješnih unit testova, Android lint provjera, debug/release APK builda, release AAB builda, provjere artefakata i CodeQL analize.
