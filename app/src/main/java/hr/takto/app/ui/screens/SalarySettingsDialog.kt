package hr.takto.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import hr.takto.app.model.SalaryProfile
import hr.takto.app.model.SalaryRegime
import hr.takto.app.model.WorkOrganization

@Composable
fun SalarySettingsDialog(
    current: SalaryProfile,
    onDismiss: () -> Unit,
    onSave: (SalaryProfile) -> Unit
) {
    var regime by remember { mutableStateOf(current.regime) }
    var workOrganization by remember { mutableStateOf(current.workOrganization) }
    var coefficient by remember { mutableStateOf(current.coefficient.takeIf { it > 0 }?.toString().orEmpty()) }
    var years by remember { mutableStateOf(current.completedYearsOfService.toString()) }
    var lowerTax by remember { mutableStateOf(current.lowerTaxRatePercent.takeIf { it > 0 }?.toString().orEmpty()) }
    var higherTax by remember { mutableStateOf(current.higherTaxRatePercent.takeIf { it > 0 }?.toString().orEmpty()) }
    var allowance by remember { mutableStateOf(current.personalAllowanceEur.toString()) }
    var regularShiftHours by remember {
        mutableStateOf((current.regularShiftMinutes / 60.0).toString().replace(".0", ""))
    }
    var secondShiftEligible by remember { mutableStateOf(current.secondShiftEligible) }
    var otherGross by remember { mutableStateOf(current.otherGrossAdditionsEur.toString()) }

    val parsedCoefficient = coefficient.replace(',', '.').toDoubleOrNull()
    val parsedYears = years.toIntOrNull()
    val parsedLower = lowerTax.replace(',', '.').toDoubleOrNull()
    val parsedHigher = higherTax.replace(',', '.').toDoubleOrNull()
    val parsedAllowance = allowance.replace(',', '.').toDoubleOrNull()
    val parsedShiftHours = regularShiftHours.replace(',', '.').toDoubleOrNull()
    val parsedOther = otherGross.replace(',', '.').toDoubleOrNull()

    val valid = regime != SalaryRegime.UNSET &&
        parsedCoefficient != null && parsedCoefficient > 0.0 &&
        parsedYears != null && parsedYears >= 0 &&
        parsedLower != null && parsedLower > 0.0 &&
        parsedHigher != null && parsedHigher > 0.0 &&
        parsedAllowance != null && parsedAllowance >= 0.0 &&
        parsedShiftHours != null && parsedShiftHours in 1.0..24.0 &&
        parsedOther != null && parsedOther >= 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Plaća i obračun") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Takto računa iz stvarnog rasporeda. Za neto iznos moraš unijeti svoje porezne podatke; aplikacija ih ne pogađa.",
                    fontWeight = FontWeight.SemiBold
                )

                Text("Sustav plaće", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = regime == SalaryRegime.STATE_SERVICE,
                        onClick = { regime = SalaryRegime.STATE_SERVICE },
                        label = { Text("Državna služba") }
                    )
                    FilterChip(
                        selected = regime == SalaryRegime.PUBLIC_SERVICE,
                        onClick = { regime = SalaryRegime.PUBLIC_SERVICE },
                        label = { Text("Javna služba") }
                    )
                }

                Text("Organizacija rada", fontWeight = FontWeight.Bold)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        WorkOrganization.STANDARD_WEEK to "Pon–pet",
                        WorkOrganization.SHIFTS to "Smjene",
                        WorkOrganization.TURNUS to "Turnus 12–24–12–48",
                        WorkOrganization.OTHER to "Drugi raspored"
                    ).forEach { (mode, label) ->
                        FilterChip(
                            selected = workOrganization == mode,
                            onClick = { workOrganization = mode },
                            label = { Text(label) }
                        )
                    }
                }

                OutlinedTextField(
                    value = coefficient,
                    onValueChange = { coefficient = it },
                    label = { Text("Koeficijent radnog mjesta") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = years,
                    onValueChange = { years = it.filter(Char::isDigit).take(2) },
                    label = { Text("Navršene godine radnog staža") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = lowerTax,
                    onValueChange = { lowerTax = it },
                    label = { Text("Niža stopa poreza (%)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = higherTax,
                    onValueChange = { higherTax = it },
                    label = { Text("Viša stopa poreza (%)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = allowance,
                    onValueChange = { allowance = it },
                    label = { Text("Mjesečni osobni odbitak (€)") },
                    supportingText = { Text("Osnovni odbitak je 600 €, ali unesi svoj stvarni iznos s PK kartice.") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = regularShiftHours,
                    onValueChange = { regularShiftHours = it },
                    label = { Text("Redovna smjena / turnus (h)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Dodatak za drugu smjenu", fontWeight = FontWeight.SemiBold)
                        Text("Uključi samo ako prema organizaciji rada ostvaruješ to pravo.")
                    }
                    Switch(
                        checked = secondShiftEligible,
                        onCheckedChange = { secondShiftEligible = it }
                    )
                }

                OutlinedTextField(
                    value = otherGross,
                    onValueChange = { otherGross = it },
                    label = { Text("Ostali mjesečni bruto dodaci (€)") },
                    supportingText = { Text("Unesi dodatke koje Takto ne može zaključiti samo iz rasporeda.") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (!valid) {
                    Text("Za obračun popuni sva obavezna polja ispravnim vrijednostima.")
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    onSave(
                        current.copy(
                            regime = regime,
                            workOrganization = workOrganization,
                            coefficient = parsedCoefficient ?: 0.0,
                            completedYearsOfService = parsedYears ?: 0,
                            lowerTaxRatePercent = parsedLower ?: 0.0,
                            higherTaxRatePercent = parsedHigher ?: 0.0,
                            personalAllowanceEur = parsedAllowance ?: 600.0,
                            regularShiftMinutes = (((parsedShiftHours ?: 8.0) * 60.0).toInt()).coerceIn(60, 24 * 60),
                            secondShiftEligible = secondShiftEligible,
                            otherGrossAdditionsEur = parsedOther ?: 0.0
                        )
                    )
                }
            ) {
                Text("Spremi")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Odustani") }
        }
    )
}
