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
import hr.takto.app.model.PayrollSystem
import hr.takto.app.model.PensionMode
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
    var coefficient by remember(initial) { mutableStateOf(decimalText(initial.coefficient)) }
    var years by remember(initial) { mutableStateOf(initial.yearsOfService.toString()) }
    var manualBase by remember(initial) { mutableStateOf(decimalText(initial.manualBaseEur)) }
    var lowerRate by remember(initial) { mutableStateOf(decimalText(initial.lowerTaxRatePercent)) }
    var higherRate by remember(initial) { mutableStateOf(decimalText(initial.higherTaxRatePercent)) }
    var allowance by remember(initial) { mutableStateOf(decimalText(initial.personalAllowanceEur)) }
    var pensionMode by remember(initial) { mutableStateOf(initial.pensionMode) }
    var extraGross by remember(initial) { mutableStateOf(decimalText(initial.additionalGrossEur)) }
    var nonTaxable by remember(initial) { mutableStateOf(decimalText(initial.nonTaxableEur)) }

    val currentMonth = YearMonth.now()
    val officialBase = CroatianPayrollRules2026.officialBase(currentMonth, system)

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
                        onClick = { system = option },
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

                OutlinedTextField(
                    value = coefficient,
                    onValueChange = { coefficient = sanitizeDecimal(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Koeficijent radnog mjesta") },
                    supportingText = {
                        if (system == PayrollSystem.STATE_SERVICE || system == PayrollSystem.PUBLIC_SERVICE) {
                            Text("Službena platna ljestvica koristi raspon 1,00–8,00.")
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

                Text("Porez i doprinosi", fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = lowerRate,
                    onValueChange = { lowerRate = sanitizeDecimal(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Niža stopa poreza (%)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = higherRate,
                    onValueChange = { higherRate = sanitizeDecimal(it) },
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
                            coefficient = parseDecimal(coefficient),
                            yearsOfService = years.toIntOrNull() ?: 0,
                            manualBaseEur = parseDecimal(manualBase),
                            lowerTaxRatePercent = parseDecimal(lowerRate),
                            higherTaxRatePercent = parseDecimal(higherRate),
                            personalAllowanceEur = parseDecimal(allowance),
                            pensionMode = pensionMode,
                            additionalGrossEur = parseDecimal(extraGross),
                            nonTaxableEur = parseDecimal(nonTaxable)
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

private fun formatEuro(value: Double): String =
    String.format(Locale("hr", "HR"), "%,.2f €", value)
