package dev.compromisso.app.ui

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 36.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Text(
                text = "COMPROMISSO",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Um compromisso com a liberdade.",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Fé, disciplina e escolhas conscientes. Você decide o que bloquear no seu celular, sem vigilância ou histórico de navegação.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(8.dp))

            OnboardingPoint(
                title = "Proteção padrão",
                text = "Uma VPN local filtra DNS convencional. Consultas permitidas usam DNS criptografado externo; o Android pedirá autorização.",
            )
            OnboardingPoint(
                title = "Pessoa de confiança",
                text = "No Modo Compromisso, pausar a proteção ou afrouxar regras exige o PIN definido por outra pessoa.",
            )
            OnboardingPoint(
                title = "Proteção reforçada ainda indisponível",
                text = "Always-on e Lockdown estão desabilitados: esta versão não intercepta todo o tráfego.",
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
