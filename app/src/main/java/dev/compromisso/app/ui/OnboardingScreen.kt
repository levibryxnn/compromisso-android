package dev.compromisso.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun OnboardingScreen(onContinue: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 36.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Text(
                text = "COMPROMISSO",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Um cadeado, não um espião.",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Você escolhe o que bloquear. O filtro funciona localmente no aparelho e não registra seu histórico de navegação.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(8.dp))

            OnboardingPoint(
                title = "Proteção padrão",
                text = "Usa uma VPN local para filtrar domínios. O Android pedirá sua autorização uma vez.",
            )
            OnboardingPoint(
                title = "Pessoa de confiança",
                text = "No Modo Compromisso, pausar a proteção ou afrouxar regras exige o PIN definido por outra pessoa.",
            )
            OnboardingPoint(
                title = "Proteção reforçada",
                text = "Opcional. Exige provisionamento especial do Android como Device Owner e será configurada separadamente.",
            )
            OnboardingPoint(
                title = "Limite real",
                text = "Nenhum app comum torna um aparelho fisicamente controlado pelo usuário absolutamente impossível de resetar ou regravar.",
            )
        }

        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
        ) {
            Text("Continuar")
        }
    }
}

@Composable
private fun OnboardingPoint(title: String, text: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
