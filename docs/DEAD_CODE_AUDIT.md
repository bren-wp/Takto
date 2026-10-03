# Dead-code audit

Takto koristi konzervativni statički audit produkcijskog Kotlin izvornog koda.

## Što se provjerava

- privatne funkcije i svojstva koja se pojavljuju samo na mjestu deklaracije;
- top-level klase i objekte bez druge reference, kao kandidate za ručni pregled;
- produkcijske `TODO` i `FIXME` oznake.

Audit namjerno ne briše Android/Compose entry pointe koji mogu biti dohvaćeni kroz manifest, callback, runtime ili refleksiju.

## Release pravilo

Android CI prije testova pokreće:

```bash
python3 scripts/dead_code_audit.py --strict
```

Release se blokira ako audit pronađe dokazano neiskorišten privatni Kotlin simbol, top-level deklaraciju bez reference ili produkcijsku `TODO`/`FIXME` oznaku.

## 0.1.18 cleanup

- uklonjen je cijeli model i UI za tamni način rada;
- uklonjena je njegova perzistencija i testovi;
- stare J/SD seed prečice zamijenjene su ugrađenim, semantički jasnim oznakama;
- D/N zadano radno vrijeme više se ne duplicira kroz UI i podatkovni sloj.
