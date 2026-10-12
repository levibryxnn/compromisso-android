# Validação do MVP 0.1.2

## Executado neste ambiente

- smoke test compilado com `kotlinc` para `DomainPolicyEngine`, `RateLimitPolicy`, `DnsCodec` e `Ipv4UdpPacket`;
- normalização de domínio, correspondência de subdomínio e proteção contra domínio semelhante;
- parsing de consulta DNS e geração de resposta `NXDOMAIN`;
- parsing e reconstrução de pacote IPv4/UDP DNS;
- curva de atraso do rate limiting;
- parsing XML do manifesto e dos recursos;
- busca por `TODO`, `FIXME`, `HACK` e logs de debug no código entregue.

Resultado original: `PURE_KOTLIN_SMOKE_TESTS_OK`.

Atualização 0.1.1: `KOTLIN_PURE_SMOKE_OK` (normalização, subdomínios, DNS parser, NXDOMAIN/SERVFAIL, framing DoT, rate limiting); `SOURCE_SECURITY_GATE_OK` (manifesto/allowlist, componentes exportados, XML e algumas APIs proibidas). **Não equivale a build ou teste de rede em Android.**

Atualização 0.1.2 (2026-10-08): `PURE_SECURITY_SMOKE_OK` com 40.000 entradas aleatórias (DNS/IPv4), `SOURCE_SECURITY_GATE_OK`, `ANDROID_SOURCE_XML_PARSE_OK`, `APK_AUDITOR_PARSER_OK`. Os testes JUnit adicionais e o teste instrumentado de atomicidade do PIN foram escritos, mas **não executados** neste ambiente. Não significa que o Android tenha compilado.

## Não executado neste ambiente

O ambiente de geração atual não contém Android SDK/Gradle e não possui acesso externo funcional para instalar o SDK. Por isso, **não** é correto afirmar que `assembleDebug`, Android Lint ou os testes instrumentados passaram aqui.

O repositório contém CI para executar, em ambiente Android configurado:

```text
python3 scripts/security_audit.py
gradle testDebugUnitTest
gradle lintDebug
gradle assembleDebug
python3 scripts/apk_permission_audit.py "$ANDROID_HOME/build-tools/36.0.0/aapt" app/build/outputs/apk/debug/app-debug.apk
```

Os testes instrumentados com SQLite/Keystore devem ser executados em emulador ou aparelho:

```text
gradle connectedDebugAndroidTest
```

## Critério antes do primeiro APK público

1. CI verde em `testDebugUnitTest`, `lintDebug` e `assembleDebug`;
2. `connectedDebugAndroidTest` em pelo menos API 26 e uma API Android atual;
3. matriz manual de bypass em dispositivo real;
4. teste de reinício e troca Wi-Fi ↔ dados móveis;
5. validação específica de Always-on/Lockdown em aparelho de teste provisionado como Device Owner.

## Validação da Palavra do Dia e power central (0.1.3)

Testes JVM em `DailyVerseSchedulePolicyTest`: antes do horário, no horário, validação de entrada,
seleção determinística do versículo. Teste instrumentado em `DailyVersePreferencesInstrumentedTest`
verifica o armazenamento local e a troca de horário; **pendente de execução em aparelho real**.

Testes manuais necessários:

1. Sem PIN: tocar power deve abrir cadastro de PIN antes de solicitar permissão de VPN.
2. Com VPN confirmada: aro verde; revogando permissão, deve voltar ao estado desligado.
3. Em Compromisso, tocar power verde: pedir PIN. PIN incorreto não desliga a VPN.
4. Habilitar Palavra do Dia no Android 13+: permissão de notificação aparece somente sob demanda.
5. Habilitar em Modo Normal ou sem VPN: nenhum versículo deve ser entregue.
6. Com Modo Compromisso e VPN ativos: aviso local após o horário definido (sujeito a atraso Doze).
7. Desabilitar: alarmes futuros cancelados. Alterar horário: agendamento anterior substituído.
8. Reiniciar e mudar fuso horário: próximo alarme reagendado.
9. Bloqueio de tela: verificar conteúdo oculto e notificações do canal pelo usuário.
10. Validar UI em 320dp e 600dp, orientação e TalkBack.

Esta rodada modifica apenas a UI e o módulo de notificação. O build Android com Gradle,
a validação instrumentada e testes em dispositivo real continuam exigidos antes de release.
