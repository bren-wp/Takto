package hr.takto.app.model

data class UserProfile(
    val fullName: String = "",
    val sector: String = "",
    val industry: String = "",
    val institutionType: String = "",
    val organizationName: String = "",
    val position: String = ""
) {
    val firstName: String
        get() = fullName.trim().substringBefore(' ').takeIf { it.isNotBlank() }.orEmpty()

    val isEmpty: Boolean
        get() = listOf(fullName, sector, industry, institutionType, organizationName, position)
            .all { it.isBlank() }
}

/**
 * Takto ne pokušava zaključati korisnika u konačan popis zanimanja.
 * Katalog nudi česte kategorije za javni, državni i privatni sektor, a sva
 * polja u profilu ostaju slobodno uređiva kako bi podržala svaku ustanovu i
 * svako radno mjesto.
 */
object EmploymentCatalog {
    val sectors = listOf(
        "Javni sektor",
        "Državni sektor",
        "Javne ustanove",
        "Državne ustanove",
        "Javna poduzeća",
        "Lokalna i područna samouprava",
        "Zdravstvo",
        "Socijalna skrb",
        "Obrazovanje i znanost",
        "Policija i sigurnost",
        "Pravosuđe",
        "Vatrogastvo i civilna zaštita",
        "Promet i infrastruktura",
        "Komunalne službe",
        "Kultura",
        "Sport",
        "Privatni sektor",
        "Neprofitni sektor",
        "Samozaposlen / obrt",
        "Ostalo"
    )

    val industries = listOf(
        "Zdravstvo",
        "Bolnica / klinika",
        "Hitna medicina",
        "Socijalna skrb",
        "Dom za starije i nemoćne",
        "Škola / obrazovanje",
        "Vrtić",
        "Sveučilište / znanost",
        "Državna uprava",
        "Javna uprava",
        "Lokalna uprava",
        "Policija",
        "Vojska",
        "Pravosuđe",
        "Vatrogastvo",
        "Civilna zaštita",
        "Komunalne djelatnosti",
        "Promet",
        "Željeznica",
        "Zračni promet",
        "Pomorstvo",
        "Pošta i logistika",
        "Energetika",
        "Telekomunikacije",
        "Proizvodnja",
        "Trgovina",
        "Ugostiteljstvo",
        "Turizam",
        "Zaštita i sigurnost",
        "IT i tehnologija",
        "Financije",
        "Kultura",
        "Sport",
        "Ostalo"
    )

    val institutionTypes = listOf(
        "Ministarstvo",
        "Državna upravna organizacija",
        "Državna agencija",
        "Sud",
        "Državno odvjetništvo",
        "Policijska uprava / postaja",
        "Oružane snage",
        "Jedinica lokalne samouprave",
        "Jedinica područne samouprave",
        "Javna ustanova",
        "Bolnica",
        "Dom zdravlja",
        "Zavod",
        "Dom socijalne skrbi",
        "Škola",
        "Vrtić",
        "Fakultet / sveučilište",
        "Javno poduzeće",
        "Komunalno društvo",
        "Privatna tvrtka",
        "Obrt",
        "Udruga / neprofitna organizacija",
        "Ostalo"
    )

    val commonPositions = listOf(
        "Medicinska sestra / medicinski tehničar",
        "Liječnik",
        "Prvostupnik sestrinstva",
        "Njegovatelj",
        "Fizioterapeut",
        "Laboratorijski tehničar",
        "Radiološki tehnolog",
        "Farmaceutski tehničar",
        "Socijalni radnik",
        "Odgojitelj",
        "Učitelj",
        "Nastavnik",
        "Profesor",
        "Asistent",
        "Referent",
        "Viši referent",
        "Stručni suradnik",
        "Viši stručni suradnik",
        "Savjetnik",
        "Viši savjetnik",
        "Voditelj",
        "Ravnatelj",
        "Načelnik",
        "Policijski službenik",
        "Vatrogasac",
        "Pravosudni policajac",
        "Zaštitar",
        "Vozač",
        "Dispečer",
        "Operater",
        "Tehničar",
        "Inženjer",
        "Programer",
        "Administrator",
        "Domar",
        "Kuhar",
        "Pomoćni radnik",
        "Spremač / spremačica",
        "Ostalo"
    )
}
