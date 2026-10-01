# Takto 0.1.0 — UI specifikacija

## Vizualni smjer

- tamna premium podloga
- plavi, ljubičasti, zeleni, jantarni i koraljni akcenti
- veliki zaobljeni elementi
- kalendar kao primarni element aplikacije
- minimalan broj koraka za unos rasporeda

## Kodovi

| Kod | Značenje | Preporučeni akcent |
| --- | --- | --- |
| D | Dan | plava |
| N | Noć | ljubičasta |
| GO | Godišnji odmor | zelena |
| BO | Bolovanje | jantarna |
| PD | Plaćeni dopust | koraljna |
| prazno | Slobodan dan | tamna neutralna |

Vlastiti unos mora podržati proizvoljan tekst i korisnički odabranu boju.

## Kalendar

- 7 stupaca, ponedjeljak–nedjelja
- velika dodirna površina svake kućice
- broj dana ostaje čitljiv uz veliku oznaku smjene
- prazno polje ne prikazuje `+`; praznina sama znači slobodan dan
- napomena i radno vrijeme koriste male sekundarne indikatore
- današnji datum i višestruki odabir moraju imati jasno različite obrube

## Navigacija

Glavni dijelovi aplikacije:

1. Početna
2. Kalendar
3. Statistika
4. Uzorci
5. Više / Postavke

## Pristupačnost

- kontrast teksta mora ostati čitljiv na tamnoj podlozi
- kalendarske ćelije moraju imati semantički TalkBack opis
- važne akcije ne smiju ovisiti samo o boji
