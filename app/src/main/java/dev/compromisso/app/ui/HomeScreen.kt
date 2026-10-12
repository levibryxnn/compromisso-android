package dev.compromisso.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.compromisso.app.daily.DailyVerse
import dev.compromisso.app.domain.BlockedDomain
import dev.compromisso.app.domain.ProtectionMode
import dev.compromisso.app.policy.ProtectionState

private val Mint = Color(0xFF31E6A0)
private val CardColor = Color(0xFF141E29)
private val Muted = Color(0xFF9BAAB8)

/** Home UI only reflects ProtectionRuntime; it never simulates an active VPN. */
@Composable
fun HomeScreen(
    state: HomeUiState,
    verse: DailyVerse,
    onProtectionToggle: () -> Unit,
    onAddDomain: () -> Unit,
    onDomainEnabledChange: (BlockedDomain, Boolean) -> Unit,
    onDeleteDomain: (BlockedDomain) -> Unit,
    onModeChange: (ProtectionMode) -> Unit,
    onDailyVerseEnabledChange: (Boolean) -> Unit,
    onDailyVerseTimeChange: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 30.dp),
    ) {
        Text("COMPROMISSO", color = Color.White, fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge, letterSpacing = 2.sp)
        Text("Um cadeado, não um espião.", color = Muted, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(34.dp))

        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = when {
                    state.rulesPending && state.protectionState == ProtectionState.OPERATIONAL -> "Aplicando regras…"
                    state.protectionActive -> "Filtro DNS operacional"
                    state.isDegraded -> "Proteção limitada · DNS indisponível"
                    state.protectionState == ProtectionState.ERROR -> "Falha ao ativar a proteção"
                    state.isStarting -> "Ativando proteção…"
                    else -> "Proteção desativada"
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
            Spacer(Modifier.height(9.dp))
            Text(
                text = when {
                    state.isDegraded -> "Bloqueios locais funcionam, mas a resolução externa falhou."
                    state.rulesPending -> "Atualizando regras do filtro…"
                    state.protectionActive -> "DNS criptografado verificado nesta sessão"
                    state.isStarting -> "Iniciando VPN e testando DNS criptografado…"
                    else -> "Toque para ativar o compromisso"
                },
                color = Muted,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(29.dp))
            CommitmentPowerButton(
                active = state.protectionActive,
                starting = state.isStarting,
                degraded = state.isDegraded ||
                    (state.rulesPending && state.protectionState == ProtectionState.OPERATIONAL),
                onClick = onProtectionToggle,
            )
            Spacer(Modifier.height(18.dp))
            Text(
                text = when {
                    state.protectionActive || state.isDegraded || state.rulesPending -> "TOQUE PARA DESATIVAR"
                    state.isStarting -> "TOQUE PARA CANCELAR"
                    else -> "TOQUE PARA ATIVAR"
                },
                color = if (state.protectionActive) Mint else Muted,
                style = MaterialTheme.typography.labelMedium,
                letterSpacing = 1.2.sp,
                fontWeight = FontWeight.SemiBold,
            )
            if (state.protectionActive || state.isDegraded) {
                Spacer(Modifier.height(9.dp))
                Text("A VPN filtra DNS convencional; não cobre todas as conexões.",
                    textAlign = TextAlign.Center,
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(Modifier.height(34.dp))
        Surface(color = CardColor, shape = RoundedCornerShape(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(22.dp),
                horizontalArrangement = Arrangement.SpaceBetween) {
                Metric("Sites na lista", if (state.storageAvailable) state.enabledDomainCount.toString() else "—")
                Metric("Categorias", "0")
                Metric("Modo", if (state.protectionMode == ProtectionMode.COMMITMENT) "Protegido" else "Normal")
            }
        }

        Spacer(Modifier.height(32.dp))
        SectionTitle("Sites bloqueados")
        Text("Um domínio bloqueia também seus subdomínios.", color = Muted,
            style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(12.dp))
        if (!state.storageAvailable) {
            Text("Armazenamento protegido indisponível. Alterações bloqueadas.",
                color = MaterialTheme.colorScheme.error)
        } else if (state.domains.isEmpty()) {
            Text("Nenhum site adicionado.", color = Muted,
                style = MaterialTheme.typography.bodyMedium)
        } else {
            state.domains.forEachIndexed { index, domain ->
                DomainRow(domain, { onDomainEnabledChange(domain, it) }, { onDeleteDomain(domain) })
                if (index < state.domains.lastIndex) HorizontalDivider(color = Color(0xFF26313C))
            }
        }
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = onAddDomain,
            enabled = state.storageAvailable,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) { Text("Adicionar site") }

        Spacer(Modifier.height(33.dp))
        SectionTitle("Palavra do Dia")
        Spacer(Modifier.height(12.dp))
        Surface(color = CardColor, shape = RoundedCornerShape(20.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Versículo pela manhã", color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text("Opcional · funciona offline", color = Muted,
                            style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = state.dailyVerseEnabled,
                        onCheckedChange = onDailyVerseEnabledChange)
                }
                Spacer(Modifier.height(14.dp))
                Text("“${verse.text}”", color = Color(0xFFE5EBF0),
                    style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(5.dp))
                Text(verse.reference, color = Mint, style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Horário: %02d:%02d".format(state.dailyVerseHour, state.dailyVerseMinute),
                        color = Muted)
                    TextButton(onClick = onDailyVerseTimeChange) { Text("Personalizar") }
                }
                Text("Notifica somente com Modo Compromisso e VPN ativos. " +
                    "O Android pode atrasar o aviso para poupar bateria.",
                    color = Muted, style = MaterialTheme.typography.bodySmall)
                if (state.dailyVerseEnabled && !state.dailyVerseNotificationsAllowed) {
                    Spacer(Modifier.height(8.dp))
                    Text("Notificações bloqueadas nas permissões do Android.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(Modifier.height(33.dp))
        SectionTitle("Segurança")
        Spacer(Modifier.height(12.dp))
        SecurityModeSelector(state.protectionMode, onModeChange)
        Spacer(Modifier.height(12.dp))
        Text(
            if (state.protectionMode == ProtectionMode.COMMITMENT)
                "Desligar a VPN e remover bloqueios exige o PIN da pessoa de confiança."
            else "No Modo Normal, você pode administrar a proteção.",
            style = MaterialTheme.typography.bodySmall, color = Muted,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            when {
                state.reinforcedActive -> "Políticas antigas de Lockdown detectadas. Esta VPN não é full-tunnel; revise antes de usar."
                state.reinforcedAvailable -> "Device Owner detectado; políticas não confirmadas."
                else -> "Proteção reforçada não configurada."
            },
            style = MaterialTheme.typography.bodySmall, color = Muted,
        )
        state.message?.let {
            Spacer(Modifier.height(15.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun CommitmentPowerButton(active: Boolean, starting: Boolean, degraded: Boolean, onClick: () -> Unit) {
    val warning = Color(0xFFFFB765)
    val ringColor = if (active) Mint else if (degraded) warning else Color(0xFF6F8393)
    val glow by animateColorAsState(ringColor, label = "powerGlow")
    Box(
        modifier = Modifier.size(216.dp)
            .shadow(if (active) 16.dp else 6.dp, CircleShape)
            .background(Brush.radialGradient(listOf(
                glow.copy(alpha = if (active) .24f else .11f),
                Color(0xFF0A111B),
            )), CircleShape)
            .border(8.dp, if (active) Mint else if (degraded) warning else Color(0xFF536270), CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription = if (active || starting || degraded) "Desativar proteção" else "Ativar proteção"
                stateDescription = when {
                    active -> "DNS criptografado operacional"
                    degraded -> "VPN ativa, DNS externo indisponível"
                    starting -> "Ativação em andamento"
                    else -> "VPN desligada"
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(95.dp)) {
            val stroke = 9.dp.toPx()
            val inset = stroke / 2f + 5.dp.toPx()
            drawArc(
                color = if (active) Mint else if (degraded) warning else Color.White,
                startAngle = -45f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(size.width - 2 * inset, size.height - 2 * inset),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawLine(
                color = if (active) Mint else if (degraded) warning else Color.White,
                start = Offset(size.width / 2f, size.height * .07f),
                end = Offset(size.width / 2f, size.height * .49f),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = Color.White, fontWeight = FontWeight.SemiBold,
        style = MaterialTheme.typography.titleLarge)
}

@Composable
private fun Metric(label: String, value: String) {
    Column {
        Text(value, style = MaterialTheme.typography.titleLarge, color = Color.White,
            fontWeight = FontWeight.SemiBold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Muted)
    }
}

@Composable
private fun DomainRow(domain: BlockedDomain, onEnabledChange: (Boolean) -> Unit, onDelete: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(domain.domain, color = Color.White, maxLines = 1,
                overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
            TextButton(onClick = onDelete) { Text("Remover") }
        }
        Switch(checked = domain.enabled, onCheckedChange = onEnabledChange)
    }
}

@Composable
private fun SecurityModeSelector(selected: ProtectionMode, onModeChange: (ProtectionMode) -> Unit) {
    Surface(color = CardColor, shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.padding(5.dp)) {
            ModeOption("Normal", selected == ProtectionMode.NORMAL, Modifier.weight(1f)) {
                onModeChange(ProtectionMode.NORMAL)
            }
            ModeOption("Compromisso", selected == ProtectionMode.COMMITMENT, Modifier.weight(1f)) {
                onModeChange(ProtectionMode.COMMITMENT)
            }
        }
    }
}

@Composable
private fun ModeOption(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(modifier = modifier.clickable(role = Role.Button, onClick = onClick),
        color = if (selected) Color(0xFF294936) else Color.Transparent,
        shape = RoundedCornerShape(12.dp)) {
        Box(Modifier.padding(vertical = 13.dp), contentAlignment = Alignment.Center) {
            Text(text, color = if (selected) Mint else Muted,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
        }
    }
}
