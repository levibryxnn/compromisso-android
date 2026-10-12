package dev.compromisso.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun AddDomainDialog(
    error: String?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adicionar site") },
        text = {
            Column {
                Text("Digite um domínio, por exemplo: instagram.com")
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    singleLine = true,
                    label = { Text("Domínio") },
                )
                if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(value) }) { Text("Adicionar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

/** In-app PIN pad: does not send the PIN to a third-party keyboard/IME or autofill. */
@Composable
private fun PrivatePinPad(pin: String, onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
            text = "●".repeat(pin.length).padEnd(6, '○'),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(10.dp).clearAndSetSemantics { },
            style = MaterialTheme.typography.titleLarge,
        )
        val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("", "0", "⌫"))
        for (row in rows) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (digit in row) {
                    if (digit.isEmpty()) {
                        androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                    } else {
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            enabled = digit == "⌫" && pin.isNotEmpty() || digit != "⌫" && pin.length < 12,
                            onClick = {
                                onChange(if (digit == "⌫") pin.dropLast(1) else pin + digit)
                            },
                        ) { Text(digit) }
                    }
                }
            }
        }
    }
}

@Composable
fun PinSetupDialog(
    error: String?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var confirming by remember { mutableStateOf(false) }
    val current = if (confirming) confirmation else pin
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (confirming) "Confirme o PIN" else "PIN da pessoa de confiança") },
        text = {
            Column {
                Text(if (confirming) "Digite novamente o PIN escolhido." else "A pessoa de confiança deve definir de 6 a 12 números.")
                PrivatePinPad(current) { if (confirming) confirmation = it else pin = it }
                if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
                if (confirming && confirmation.length == pin.length && confirmation != pin) {
                    Text("Os PINs não coincidem.", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = if (confirming) confirmation.length in 6..12 && confirmation == pin else pin.length in 6..12,
                onClick = {
                    if (!confirming) {
                        confirming = true
                    } else {
                        val credential = pin
                        pin = ""
                        confirmation = ""
                        onConfirm(credential)
                    }
                },
            ) { Text(if (confirming) "Salvar" else "Próximo") }
        },
        dismissButton = {
            TextButton(onClick = {
                if (confirming) {
                    confirming = false
                    confirmation = ""
                } else onDismiss()
            }) { Text(if (confirming) "Voltar" else "Cancelar") }
        },
    )
}

@Composable
fun PinVerifyDialog(
    error: String?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirmar com PIN") },
        text = {
            Column {
                Text("Esta ação reduz a proteção. Confirme com o PIN da pessoa de confiança.")
                PrivatePinPad(pin) { pin = it }
                if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(
                enabled = pin.length in 6..12,
                onClick = {
                    val entered = pin
                    pin = ""
                    onConfirm(entered)
                },
            ) { Text("Confirmar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
