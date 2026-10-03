package hr.takto.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import hr.takto.app.model.CroatianPayrollRules2026
import hr.takto.app.model.PayrollProfile
import hr.takto.app.model.PayrollRoleCatalog2026
import hr.takto.app.model.PayrollSystem
import hr.takto.app.model.PayrollTaxCatalog2026
import hr.takto.app.model.PensionMode
import hr.takto.app.ui.components.formatEuro
import hr.takto.app.ui.theme.TaktoBlue
import java.time.YearMonth
import java.util.Locale

@Composable
fun PayrollSettingsDialog(
    initial: PayrollProfile,
    onDismiss: () -> Unit,
    onSave: (PayrollProfile) -> Unit
) {
    var enabled by remember(initial) { mutableStateOf(initial.enabled) }
    var system by remember(initial) { mutableStateOf(initial.system) }
    var rolePresetId by remember(initial) { mutableStateOf(initial.rolePresetId) }
    var roleQuery by remember(initial) {
        mutableStateOf(PayrollRoleCatalog2026.role(initial.rolePresetId)?.label.orEmpty())
    }
    var coefficient by remember(initial) { mutableStateOf(decimalText(initial.coefficient)) }
    var years by remember(initial) { mutableStateOf(initial.yearsOfService.toString()) }
    var manualBase by remember(initial) { mutableStateOf(decimalText(initial.manualBaseEur)) }
    var taxLocalityPresetId by remember(initial) { mutableStateOf(initial.taxLocalityPresetId) }
    var taxLocalityQuery by remember(initial) {
        mutableStateOf(PayrollTaxCatalog2026.locality(initial.taxLocalityPresetId)?.name.orEmpty())
    }
    var lowerRate by remember(initial) { mutableStateOf(decimalText(initial.lowerTaxRatePercent)) }
    var higherRate by remember(initial) { mutableStateOf(decimalText(initial.higherTaxRatePercent)) }
    var allowance by remember(initial) { mutableStateOf(decimalText(initial.personalAllowanceEur)) }
    var pensionMode by remember(initial) { mutableStateOf(initial.pensionMode) }
    var extraGross by remember(initial) { mutableStateOf(decimalText(initial.additionalGrossEur)) }
    var nonTaxable by remember(initial) { mutableStateOf(decimalText(initial.nonTaxableEur)) }
    var overtimePercent by remember(initial) { mutableStateOf(decimalText(initial.overtimePercent)) }
    var nightPercent by remember(initial) { mutableStateOf(decimalText(initial.nightPercent)) }
    var saturdayPercent by remember(initial) { mutableStateOf(decimalText(initial.saturdayPercent)) }
    var sundayPercent by remember(initial) { mutableStateOf(decimalText(initial.sundayPercent)) }
    var holidayPercent by remember(initial) { mutableStateOf(decimalText(initial.holidayPercent)) }
    var otherEmployersGross by remember(initial) {
        mutableStateOf(decimalText(initial.otherEmployersGrossEur))
    }
    var allAdjustmentsConfirmed by remember(initial) {
        mutableStateOf(initial.allAdjustmentsConfirmed)
    }

    val currentMonth = YearMonth.now()
    val officialBase = CroatianPayrollRules2026.officialBase(currentMonth, system)
    val selectedRole = PayrollRoleCatalog2026.role(rolePresetId)
        ?.takeIf { it.system == system }
    val matchingRoles = if (
        system == PayrollSystem.OTHER ||
        roleQuery.trim().length < 2 ||
        selectedRole?.label.equals(roleQuery.trim(), ignoreCase = true)
    ) {
        emptyList()
    } else {
        PayrollRoleCatalog2026.search(system, roleQuery, limit = 7)
    }
    val selectedTaxLocality = PayrollTaxCatalog2026.locality(taxLocalityPresetId)
    val matchingTaxLocalities = if (
        taxLocalityQuery.trim().length < 2 ||
        selectedTaxLocality?.name.equals(taxLocalityQuery.trim(), ignoreCase = true)
    ) {
        emptyList()
    } else {
        PayrollTaxCatalog2026.search(taxLocalityQuery, limit = 6)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Plaća i obračun") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Uključi obračun plaće", fontWeight = FontWeight.Bold)
                        Text(
                            "Prikaz bruto, neto, dodataka i isplate u Statistici.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                }

                Text("Sustav obračuna", fontWeight = FontWeight.SemiBold)
                PayrollSystem.entries.forEach { option ->
                    FilterChip(
                        selected = system == option,
                        onClick = {
                            if (system != option) {
                                system = option
                                if (PayrollRoleCatalog2026.role(rolePresetId)?.system != option) {
                                    rolePresetId = ""
                                    roleQuery = ""
                                }
                            }
                        },
                        label = { Text(option.label) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (officialBase != null) {
                    Text(
                        "Službena osnovica za ${currentMonth.monthValue}/${currentMonth.year}: ${formatEuro(officialBase)}",
                        color = TaktoBlue,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    OutlinedTextField(
                        value = manualBase,
                        onValueChange = { manualBase = sanitizeDecimal(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Mjesečna osnovica bruto (€)") },
                        singleLine = true
                    )
                }

                if (system == PayrollSystem.STATE_SERVICE || system == PayrollSystem.PUBLIC_SERVICE) {
                    Text("Radno mjesto i koeficijent", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Pretraži referentni katalog ili ostavi ručni koeficijent ako tvoje radno mjesto nije na popisu.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = roleQuery,
                        onValueChange = {
                            roleQuery = it.take(80)
                            if (selectedRole != null && !selectedRole.label.equals(it.trim(), ignoreCase = true)) {
                                rolePresetId = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Traži radno mjesto") },
                        placeholder = { Text("npr. medicinska sestra, učitelj, referent, policija") },
                        singleLine = true
                    )
                    matchingRoles.forEach { role ->
                        FilterChip(
                            selected = rolePresetId == role.id,
                            onClick = {
                                rolePresetId = role.id
                                roleQuery = role.label
                                coefficient = decimalText(role.coefficient)
                            },
                            label = {
                                Column {
                                    Text("${role.label} · ${decimalText(role.coefficient)}", fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "${role.sector} · ${role.code}",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    selectedRole?.let { role ->
                        Text(
                            "Odabrano: ${role.officialName} · koeficijent ${decimalText(role.coefficient)} · oznaka ${role.code}",
                            color = TaktoBlue,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        PayrollRoleCatalog2026.SOURCE_LABEL,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                OutlinedTextField(
                    value = coefficient,
                    onValueChange = {
                        coefficient = sanitizeDecimal(it)
                        rolePresetId = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Koeficijent / bodovanje") },
                    supportingText = {
                        if (system == PayrollSystem.STATE_SERVICE || system == PayrollSystem.PUBLIC_SERVICE) {
                            Text("Koeficijent možeš odabrati iz kataloga ili upisati ručno. Za obračun se koristi spremljena brojčana vrijednost.")
                        }
                    },
                    singleLine = true
                )
                OutlinedTextField(
                    value = years,
                    onValueChange = { years = it.filter(Char::isDigit).take(2) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Navršene godine radnog staža") },
                    singleLine = true
                )

                if (system == PayrollSystem.OTHER) {
                    Text("Dodaci na satnicu", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Upiši postotke koji stvarno vrijede prema tvojem kolektivnom ugovoru, pravilniku ili ugovoru o radu.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = overtimePercent,
                        onValueChange = { overtimePercent = sanitizeDecimal(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Prekovremeni rad (%)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = nightPercent,
                        onValueChange = { nightPercent = sanitizeDecimal(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Noćni rad (%)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = saturdayPercent,
                        onValueChange = { saturdayPercent = sanitizeDecimal(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Subota (%)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = sundayPercent,
                        onValueChange = { sundayPercent = sanitizeDecimal(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Nedjelja (%)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = holidayPercent,
                        onValueChange = { holidayPercent = sanitizeDecimal(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Blagdan / neradni dan (%)") },
                        singleLine = true
                    )
                }

                Text("Porez i doprinosi", fontWeight = FontWeight.SemiBold)
                Text(
                    "Možeš odabrati mjesto oporezivanja iz referentnog kataloga 2026. ili stope upisati ručno.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = taxLocalityQuery,
                    onValueChange = {
                        taxLocalityQuery = it.take(80)
                        if (selectedTaxLocality != null &&
                            !selectedTaxLocality.name.equals(it.trim(), ignoreCase = true)
                        ) {
                            taxLocalityPresetId = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Mjesto oporezivanja") },
                    placeholder = { Text("npr. Rijeka, Zagreb, Split") },
                    singleLine = true
                )
                matchingTaxLocalities.forEach { locality ->
                    FilterChip(
                        selected = taxLocalityPresetId == locality.id,
                        onClick = {
                            taxLocalityPresetId = locality.id
                            taxLocalityQuery = locality.name
                            lowerRate = decimalText(locality.lowerRate)
                            higherRate = decimalText(locality.higherRate)
                        },
                        label = {
                            Column {
                                Text(
                                    "${locality.name} · ${decimalText(locality.lowerRate)} % / ${decimalText(locality.higherRate)} %",
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "${locality.county} · ${locality.source}",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                selectedTaxLocality?.let { locality ->
                    Text(
                        "Odabrano: ${locality.name} · niža ${decimalText(locality.lowerRate)} % · viša ${decimalText(locality.higherRate)} %",
                        color = TaktoBlue,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    PayrollTaxCatalog2026.SOURCE_LABEL,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall
                )
                OutlinedTextField(
                    value = lowerRate,
                    onValueChange = {
                        lowerRate = sanitizeDecimal(it)
                        taxLocalityPresetId = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Niža stopa poreza (%)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = higherRate,
                    onValueChange = {
                        higherRate = sanitizeDecimal(it)
                        taxLocalityPresetId = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Viša stopa poreza (%)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = allowance,
                    onValueChange = { allowance = sanitizeDecimal(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Mjesečni osobni odbitak (€)") },
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PensionMode.entries.forEach { mode ->
                        FilterChip(
                            selected = pensionMode == mode,
                            onClick = { pensionMode = mode },
                            label = { Text(mode.label) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                OutlinedTextField(
                    value = otherEmployersGross,
                    onValueChange = { otherEmployersGross = sanitizeDecimal(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Bruto kod drugih poslodavaca (€)") },
                    supportingText = {
                        Text("Ostavi 0 ako u tom mjesecu nemaš drugog poslodavca.")
                    },
                    singleLine = true
                )

                OutlinedTextField(
                    value = extraGross,
                    onValueChange = { extraGross = sanitizeDecimal(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Ostali bruto dodaci (€)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = nonTaxable,
                    onValueChange = { nonTaxable = sanitizeDecimal(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Neoporezive isplate (€)") },
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Svi dodaci i naknade su uneseni", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Potvrdi tek kada si unio sve posebne i granske dodatke koji vrijede za taj mjesec.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(
                        checked = allAdjustmentsConfirmed,
                        onCheckedChange = { allAdjustmentsConfirmed = it }
                    )
                }

                Text(
                    "Za točan neto upiši porezne stope koje vrijede prema mjestu oporezivanja i ukupni osobni odbitak koji se primjenjuje na tvojoj PK.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
                if (system == PayrollSystem.STATE_SERVICE || system == PayrollSystem.PUBLIC_SERVICE) {
                    Text(
                        "Posebni granski dodaci, turnus, pripravnost ili druge naknade nisu isti u svim službama. Ako se primjenjuju, upiši ih u ostale bruto dodatke.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        CroatianPayrollRules2026.SOURCE_LABEL,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        PayrollProfile(
                            enabled = enabled,
                            system = system,
                            rolePresetId = rolePresetId,
                            coefficient = parseDecimal(coefficient),
                            yearsOfService = years.toIntOrNull() ?: 0,
                            manualBaseEur = parseDecimal(manualBase),
                            taxLocalityPresetId = taxLocalityPresetId,
                            lowerTaxRatePercent = parseDecimal(lowerRate),
                            higherTaxRatePercent = parseDecimal(higherRate),
                            personalAllowanceEur = parseDecimal(allowance),
                            pensionMode = pensionMode,
                            additionalGrossEur = parseDecimal(extraGross),
                            nonTaxableEur = parseDecimal(nonTaxable),
                            overtimePercent = parseDecimal(overtimePercent),
                            nightPercent = parseDecimal(nightPercent),
                            saturdayPercent = parseDecimal(saturdayPercent),
                            sundayPercent = parseDecimal(sundayPercent),
                            holidayPercent = parseDecimal(holidayPercent),
                            otherEmployersGrossEur = parseDecimal(otherEmployersGross),
                            allAdjustmentsConfirmed = allAdjustmentsConfirmed
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = TaktoBlue)
            ) {
                Text("Spremi")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Odustani") }
        }
    )
}

private fun sanitizeDecimal(value: String): String =
    value.filter { it.isDigit() || it == ',' || it == '.' }.take(12)

private fun parseDecimal(value: String): Double =
    value.trim().replace(',', '.').toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0

private fun decimalText(value: Double): String =
    if (value == 0.0) "" else String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')

