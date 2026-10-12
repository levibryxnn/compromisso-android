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
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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

private val Midnight = Color(0xFF071320)
private val Card = Color(0xFF121E2B)
private val Outline = Color(0xFF2A3849)
private val White = Color(0xFFF4F6F7)
private val Secondary = Color(0xFF9FAEBF)
private val Gold = Color(0xFFF1C998)
private val Mint = Color(0xFF36DA9B)
private val Warning = Color(0xFFF4B46D)

/** Premium presentation, but protection states and actions remain owned by MainActivity. */
@Composable
fun PremiumHomeScreen(
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
    var sitesOpen by rememberSaveable { mutableStateOf(false) }
    var modeOpen by rememberSaveable { mutableStateOf(false) }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    val pageScroll = rememberScrollState()
    LaunchedEffect(settingsOpen) {
        if (settingsOpen) pageScroll.animateScrollTo(pageScroll.maxValue)
    }

    val usedCategories = state.domains.filter { it.enabled }
        .mapNotNull { it.category?.trim()?.takeIf { category -> category.isNotEmpty() } }
        .distinct().size
    val title = when {
        state.rulesPending && state.protectionState == ProtectionState.OPERATIONAL -> "Aplicando regras"
        state.protectionActive -> "Proteção ativa"
        state.isDegraded -> "Proteção limitada"
        state.protectionState == ProtectionState.ERROR -> "Falha na proteção"
        state.isStarting -> "Iniciando proteção"
        else -> "Proteção desativada"
    }
    val subtitle = when {
        state.rulesPending -> "Aguardando a confirmação do filtro"
        state.protectionActive -> "DNS criptografado operacional nesta sessão"
        state.isDegraded -> "DNS externo indisponível · bloqueios locais permanecem"
        state.protectionState == ProtectionState.ERROR -> "Verifique a conexão e tente novamente"
        state.isStarting -> "Preparando a conexão segura"
        else -> "Toque para ativar o compromisso"
    }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
        listOf(Color(0xFF0C1B2B), Midnight, Color(0xFF050D17))
    ))) {
        PremiumMountains(Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize().verticalScroll(pageScroll)
            .padding(horizontal = 21.dp).padding(top = 25.dp, bottom = 46.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                ChristianMark(Modifier.size(42.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("COMPROMISSO", color = White, fontWeight = FontWeight.Bold,
                        fontSize = 20.sp, letterSpacing = 1.9.sp)
                    Text("Um cadeado, não um espião.", color = Secondary, fontSize = 12.sp)
                }
                Box(Modifier.size(44.dp).clip(CircleShape).clickable(role = Role.Button) {
                    settingsOpen = !settingsOpen
                }.semantics { contentDescription = "Configurações protegidas" },
                    contentAlignment = Alignment.Center) {
                    Text("⚙", fontSize = 25.sp, color = Secondary)
                }
            }

            Spacer(Modifier.height(44.dp))
            Text(title, color = White, modifier = Modifier.fillMaxWidth(),
                fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center)
            Spacer(Modifier.height(7.dp))
            Text(subtitle, color = Secondary, modifier = Modifier.fillMaxWidth(),
                fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 18.sp)
            Spacer(Modifier.height(33.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                PremiumPower(
                    on = state.protectionActive,
                    starting = state.isStarting,
                    warning = state.isDegraded || state.rulesPending,
                    onClick = onProtectionToggle,
                )
            }
            Spacer(Modifier.height(18.dp))
            Text(
                if (state.protectionActive || state.isDegraded || state.rulesPending)
                    "TOQUE PARA DESATIVAR" else if (state.isStarting)
                    "TOQUE PARA CANCELAR" else "TOQUE PARA ATIVAR",
                color = if (state.protectionActive) Mint else Secondary,
                modifier = Modifier.fillMaxWidth(),
                fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp, textAlign = TextAlign.Center,
            )
            if (state.protectionActive || state.isDegraded) {
                Spacer(Modifier.height(10.dp))
                Text("Filtro DNS local · não cobre todas as conexões",
                    modifier = Modifier.fillMaxWidth(), color = Secondary,
                    fontSize = 11.sp, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(38.dp))
            PremiumCard {
                Row(Modifier.fillMaxWidth().padding(vertical = 21.dp, horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly) {
                    PremiumMetric(if (state.storageAvailable) state.enabledDomainCount.toString()
                        else "—", "Sites na lista", Modifier.weight(1f))
                    Box(Modifier.width(1.dp).height(43.dp).background(Outline))
                    PremiumMetric(if (state.storageAvailable) usedCategories.toString()
                        else "—", "Categorias usadas", Modifier.weight(1f))
                    Box(Modifier.width(1.dp).height(43.dp).background(Outline))
                    PremiumMetric(if (state.protectionMode == ProtectionMode.COMMITMENT) "PIN"
                        else "Livre", "Modo", Modifier.weight(1f))
                }
            }

            Spacer(Modifier.height(18.dp))
            PremiumCard {
                Box(Modifier.fillMaxWidth().background(Brush.linearGradient(
                    listOf(Color(0xFF1A293B), Color(0xFF2A2C36), Color(0xFF40332F))
                ))) {
                    Canvas(Modifier.matchParentSize()) {
                        val p = Path().apply {
                            moveTo(0f, size.height); lineTo(0f, size.height*.91f)
                            quadraticBezierTo(size.width*.21f,size.height*.69f,size.width*.45f,size.height*.87f)
                            quadraticBezierTo(size.width*.79f,size.height*.62f,size.width,size.height*.8f)
                            lineTo(size.width,size.height); close()
                        }
                        drawPath(p,Color(0xFF0B1624).copy(alpha=.65f))
                    }
                    Column(Modifier.padding(20.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("✝", color = Gold, fontSize = 24.sp)
                            Spacer(Modifier.width(11.dp))
                            Text("PALAVRA DO DIA", color = Gold, modifier = Modifier.weight(1f),
                                fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp)
                            Text(verse.reference, color = Secondary, fontSize = 12.sp)
                        }
                        Spacer(Modifier.height(13.dp))
                        Text("“" + verse.text + "”", color = White,
                            style = MaterialTheme.typography.bodyLarge,
                            fontSize = 17.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(17.dp))
                        HorizontalDivider(color = Color.White.copy(alpha=.12f))
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("Lembrete diário", color = White,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f))
                            Text("%02d:%02d".format(state.dailyVerseHour,state.dailyVerseMinute),
                                color = Secondary, fontSize = 12.sp)
                            TextButton(onClick = onDailyVerseTimeChange) { Text("Alterar") }
                            Switch(checked = state.dailyVerseEnabled,
                                onCheckedChange = onDailyVerseEnabledChange,
                                modifier = Modifier.semantics {
                                    contentDescription = "Lembrete Palavra do Dia"
                                })
                        }
                        if (state.dailyVerseEnabled && !state.dailyVerseNotificationsAllowed) {
                            Text("Permissão de notificação desativada no Android",
                                color = Warning, fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            PremiumCard {
                Column {
                    PremiumNavigateRow("Sites bloqueados",
                        "Gerencie seus domínios protegidos", "⊘",
                        sitesOpen, { sitesOpen = !sitesOpen })
                    if (sitesOpen) {
                        HorizontalDivider(color = Outline)
                        Column(Modifier.padding(horizontal = 19.dp, vertical = 13.dp)) {
                            Text("O bloqueio de um domínio inclui seus subdomínios.",
                                color = Secondary, fontSize = 12.sp)
                            Spacer(Modifier.height(12.dp))
                            if (!state.storageAvailable) {
                                Text("Armazenamento criptografado indisponível.",
                                    color = Warning, fontSize = 12.sp)
                            } else if (state.domains.isEmpty()) {
                                Text("Nenhum site cadastrado.", color = Secondary)
                            } else {
                                state.domains.forEachIndexed { index, domain ->
                                    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text(domain.domain, color = White, maxLines = 1,
                                                overflow = TextOverflow.Ellipsis)
                                            TextButton(onClick = { onDeleteDomain(domain) }) {
                                                Text("Remover")
                                            }
                                        }
                                        Switch(checked = domain.enabled,
                                            onCheckedChange = { onDomainEnabledChange(domain,it) },
                                            modifier = Modifier.semantics {
                                                contentDescription = "Bloquear " + domain.domain
                                            })
                                    }
                                    if (index != state.domains.lastIndex)
                                        HorizontalDivider(color = Outline)
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Button(onClick = onAddDomain, enabled = state.storageAvailable,
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(12.dp)) {
                                Text("Adicionar site")
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            PremiumCard {
                Column {
                    PremiumNavigateRow("Modo Compromisso",
                        if (state.protectionMode == ProtectionMode.COMMITMENT)
                            "Ativo · alterações exigem PIN" else "Normal · escolha sua proteção",
                        "✦", modeOpen, { modeOpen = !modeOpen })
                    if (modeOpen) {
                        HorizontalDivider(color = Outline)
                        Column(Modifier.padding(18.dp)) {
                            Surface(color = Color(0xFF0B1827),
                                shape = RoundedCornerShape(12.dp)) {
                                Row(Modifier.padding(5.dp)) {
                                    PremiumMode("Normal",
                                        state.protectionMode == ProtectionMode.NORMAL,
                                        Modifier.weight(1f)) { onModeChange(ProtectionMode.NORMAL) }
                                    PremiumMode("Compromisso",
                                        state.protectionMode == ProtectionMode.COMMITMENT,
                                        Modifier.weight(1f)) { onModeChange(ProtectionMode.COMMITMENT) }
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Text("Desligar a proteção, retirar sites e mudar para Normal exige o PIN da pessoa de confiança.",
                                color = Secondary, fontSize = 12.sp, lineHeight = 17.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            PremiumCard {
                Column {
                    PremiumNavigateRow("Configurações protegidas",
                        "Privacidade e limites desta versão", "⚙",
                        settingsOpen, { settingsOpen = !settingsOpen })
                    if (settingsOpen) {
                        HorizontalDivider(color = Outline)
                        Column(Modifier.padding(19.dp)) {
                            Text("Sem conta, sem rastreamento e sem histórico de navegação.",
                                color = White, fontSize = 13.sp)
                            Spacer(Modifier.height(8.dp))
                            Text("Esta versão intercepta somente DNS convencional. DoH independente, IPv6, proxy e conexões por IP podem contornar o filtro.",
                                color = Secondary, fontSize = 12.sp, lineHeight = 18.sp)
                            Spacer(Modifier.height(10.dp))
                            Text(when {
                                state.reinforcedActive -> "Atenção: Lockdown antigo detectado; revise as políticas."
                                state.reinforcedAvailable -> "Device Owner detectado; políticas ainda não confirmadas."
                                else -> "Always-on/Lockdown desabilitado para evitar cortes de conectividade."
                            }, color = Warning, fontSize = 12.sp, lineHeight = 17.sp)
                            Spacer(Modifier.height(9.dp))
                            Text("A Palavra do Dia é opcional e local. O Android pode atrasar o lembrete.",
                                color = Secondary, fontSize = 12.sp)
                        }
                    }
                }
            }
            state.message?.let {
                Spacer(Modifier.height(15.dp))
                Text(it, color = Warning, fontSize = 13.sp)
            }
            Spacer(Modifier.height(22.dp))
            Text("COMPROMISSO · Gratuito e feito para preservar sua privacidade",
                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                color = Secondary.copy(alpha = .7f), fontSize = 10.sp)
        }
    }
}

@Composable
private fun PremiumMountains(modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        for (layer in 0..2) {
            val y = h * (.34f + layer*.022f)
            val ridge = Path().apply {
                moveTo(0f,y)
                lineTo(w*.14f,y-h*.024f)
                lineTo(w*.27f,y+h*.004f)
                lineTo(w*.43f,y-h*.022f)
                lineTo(w*.66f,y+h*.01f)
                lineTo(w*.84f,y-h*.020f)
                lineTo(w,y+h*.009f)
                lineTo(w,h); lineTo(0f,h); close()
            }
            drawPath(ridge,Color(0xFF010A14).copy(alpha=.10f+layer*.11f))
        }
    }
}

@Composable
private fun ChristianMark(modifier: Modifier) {
    Canvas(modifier.semantics { contentDescription = "Compromisso: C, cruz e caminho" }) {
        val w=size.width
        val h=size.height
        drawArc(Gold, 47f, 267f, false, Offset(w*.12f,h*.10f),
            Size(w*.76f,h*.78f),style=Stroke(width=w*.095f,cap=StrokeCap.Round))
        drawLine(White,Offset(w*.51f,h*.22f),Offset(w*.51f,h*.64f),
            strokeWidth=w*.077f,cap=StrokeCap.Round)
        drawLine(White,Offset(w*.35f,h*.37f),Offset(w*.68f,h*.37f),
            strokeWidth=w*.077f,cap=StrokeCap.Round)
        val path=Path().apply {
            moveTo(w*.30f,h*.92f)
            quadraticBezierTo(w*.42f,h*.67f,w*.53f,h*.60f)
            quadraticBezierTo(w*.63f,h*.47f,w*.75f,h*.51f)
        }
        drawPath(path, Gold, style=Stroke(width=w*.060f,cap=StrokeCap.Round))
    }
}

@Composable
private fun PremiumPower(on:Boolean,starting:Boolean,warning:Boolean,onClick:()->Unit) {
    val target = if (on) Mint else if (warning) Warning else Color(0xFF7A90A5)
    val ring by animateColorAsState(target,label="powerRing")
    Box(Modifier.size(224.dp).shadow(if(on) 16.dp else 4.dp,CircleShape)
        .background(Brush.radialGradient(listOf(ring.copy(alpha=.21f),
            Color(0xFF0E1C2D),Color(0xFF091524))),CircleShape)
        .border(3.dp,Brush.linearGradient(listOf(ring,ring.copy(alpha=.36f))),CircleShape)
        .clip(CircleShape).clickable(role=Role.Button,onClick=onClick)
        .semantics {
            contentDescription = if(on||starting||warning) "Desativar proteção" else "Ativar proteção"
            stateDescription = when {
                on -> "DNS criptografado operacional e regras aplicadas"
                warning -> "Proteção degradada ou regras pendentes"
                starting -> "Ativação em andamento"
                else -> "Proteção desligada"
            }
        },contentAlignment=Alignment.Center) {
        Canvas(Modifier.size(108.dp)) {
            val width=9.dp.toPx()
            val margin=width*.5f+4.dp.toPx()
            val iconColor=if(on||warning) ring else White
            drawArc(iconColor,-43f,266f,false,Offset(margin,margin),
                Size(size.width-margin*2,size.height-margin*2),
                style=Stroke(width=width,cap=StrokeCap.Round))
            drawLine(iconColor,Offset(size.width*.5f,size.height*.05f),
                Offset(size.width*.5f,size.height*.47f),strokeWidth=width,cap=StrokeCap.Round)
        }
    }
}

@Composable
private fun PremiumCard(content: @Composable () -> Unit) {
    Surface(shape=RoundedCornerShape(21.dp),color=Card.copy(alpha=.96f),
        border=androidx.compose.foundation.BorderStroke(1.dp,Outline.copy(alpha=.64f)),
        content=content)
}

@Composable
private fun PremiumMetric(value:String,label:String,modifier:Modifier) {
    Column(modifier,horizontalAlignment=Alignment.CenterHorizontally) {
        Text(value,color=White,style=MaterialTheme.typography.titleLarge,
            maxLines=1,fontWeight=FontWeight.Bold)
        Spacer(Modifier.height(3.dp))
        Text(label,color=Secondary,fontSize=11.sp,lineHeight=15.sp,textAlign=TextAlign.Center)
    }
}

@Composable
private fun PremiumNavigateRow(
    title:String,description:String,symbol:String,expanded:Boolean,onClick:()->Unit
) {
    Row(Modifier.fillMaxWidth().clickable(role=Role.Button,onClick=onClick)
        .padding(horizontal=19.dp,vertical=19.dp),verticalAlignment=Alignment.CenterVertically) {
        Box(Modifier.size(42.dp).background(Color(0xFF223248),CircleShape),
            contentAlignment=Alignment.Center) {
            Text(symbol,color=Gold,fontSize=24.sp)
        }
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(title,color=White,fontWeight=FontWeight.SemiBold,fontSize=16.sp)
            Spacer(Modifier.height(2.dp))
            Text(description,color=Secondary,fontSize=12.sp,lineHeight=16.sp)
        }
        Text(if(expanded) "⌃" else "›",color=Secondary,fontSize=28.sp)
    }
}

@Composable
private fun PremiumMode(text:String,selected:Boolean,modifier:Modifier,onClick:()->Unit) {
    Surface(modifier.clip(RoundedCornerShape(9.dp))
        .clickable(role=Role.Button,onClick=onClick),
        color=if(selected) Color(0xFF263F4C) else Color.Transparent,
        shape=RoundedCornerShape(9.dp)) {
        Box(Modifier.padding(vertical=13.dp),contentAlignment=Alignment.Center) {
            Text(text,color=if(selected) Gold else Secondary,
                fontWeight=if(selected) FontWeight.Bold else FontWeight.Normal)
        }
    }
}
