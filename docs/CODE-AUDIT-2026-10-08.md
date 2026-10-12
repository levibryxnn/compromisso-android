# COMPROMISSO — revisão de código, comunicação e regressão (08/10/2026)

**Código-base:** `COMPROMISSO-0.1.2-build-readiness.zip`. **Status:** correções de fonte aplicadas; ainda sem build Android aprovado. O `versionName` continua `0.1.2`, pois esta não é uma versão validada para publicação.

## Escopo inspecionado

- Código Kotlin do aplicativo: Activity, telas Compose, domínio, SQLite, criptografia, credenciais/PIN, VPN, DNS e administração Device Owner.
- `AndroidManifest.xml`, XML de backup/transferência, Gradle, CI, scripts de auditoria, testes JVM e instrumentados.
- Comunicação e estados: UI -> autorização por PIN -> repository/config -> controller -> VpnService -> DNS proxy -> DNS-over-TLS.

## Problemas corrigidos nesta revisão

| Prioridade | Falha / risco de regressão | Correção aplicada |
|---|---|---|
| Alta | Autorização assíncrona de PIN podia ser associada a outra ação iniciada após cancelar/substituir o diálogo. | Vinculação da resposta à identidade da ação e à geração da autorização; ignora resultados obsoletos ou Activity destruída. |
| Alta | Falha ao ler SQLite/Android Keystore podia provocar exceção não tratada no início da VPN, ou na Activity. | Regras são carregadas antes de estabelecer o túnel; falha cancela ativação; tela indica armazenamento indisponível sem apagá-lo e desabilita edições. |
| Alta | Falha ao recarregar blocklist podia derrubar o serviço sem sinalizar erro de proteção. | Recarregamento trata exceção, desativa intenção de proteção e interrompe VPN, sem declarar atualização bem-sucedida. |
| Média | Worker DNS encerrado inesperadamente podia deixar a intenção persistida em estado de ativação interminável. | Finalização inesperada remove estado de intenção antes de desligar. |
| Média | Controle principal era desabilitado em 'iniciando', bloqueando cancelamento após uma tentativa travada. | Cancelamento volta a ficar disponível e continua exigindo PIN no Modo Compromisso. |
| Média | Parser aceitava mensagem DNS de resposta como consulta recebida ou consulta com respostas anexas. | Valida QR, opcode padrão, QDCOUNT=1 e ausência de respostas/autoridades em consultas. |
| Média | Pacotes UDP com checksum não nulo inválido podiam ser aceitos. | Validação de UDP/IPv4 com pseudo-cabeçalho, mantendo compatibilidade com checksum zero permitido pelo IPv4. |
| Média | URL com credenciais (`user@dominio`) ou consulta podia bloquear host inesperado. | Normalização recusa userinfo, delimitadores ambíguos e query/fragment. |
| Média | Chamadas repetidas de configuração poderiam sobrescrever uma credencial existente se a API fosse reutilizada incorretamente. | `TrustedPinManager.configure` só aceita o primeiro provisionamento; redefinição exigiria fluxo de autorização próprio. |
| Média | Intenção de proteção gravada de forma assíncrona podia não sobreviver a término repentino do processo. | Gravação com confirmação síncrona (`SharedPreferences.commit`) para a flag crítica. |

## Executado no ambiente de revisão

- Compilação dos componentes Kotlin puros usados no teste de fumaça: passou.
- Testes de unidade JVM do próprio projeto, compilados e executados via harness JUnit local: **15 aprovados, 0 falhas**.
- Teste de robustez dos parsers com **40.000 entradas pseudoaleatórias**: passou sem exceções.
- Testes adicionais no smoke: DNS válido com UDP checksum zero e não-zero, corrupção do checksum, QR de resposta e domínio ambíguo.
- `scripts/security_audit.py`: passou (permissões declaradas, componentes exportados, XML, tokens de APIs de acesso não permitido).

**Limite importante:** o harness/JVM não executa Android SDK. As mudanças em `MainActivity`, Compose, SQLite/Keystore, `VpnService` e lifecycle foram inspecionadas estaticamente, **não compiladas nem validadas em aparelho**.

## Riscos ainda ABERTOS

1. **Bloqueador de release:** sem Gradle/Android SDK/dispositivo nesta execução, não há `assembleDebug`, `lintDebug`, manifesto final mesclado, teste em celular ou regressão instrumentada comprovados.
2. **Arquitetura DNS-first:** DoH/DoT próprios dos navegadores, IPv6, conexões diretas por IP, VPN concorrente e certos proxies ainda podem contornar o filtro.
3. **DNS sequencial:** o proxy processa um pedido por vez; TLS com timeouts de 3 s pode atrasar consultas subsequentes e prejudicar conectividade em redes instáveis.
4. **Saúde do resolvedor:** a indicação de VPN ativa confirma o túnel e o worker, não a disponibilidade do resolvedor DoT remoto. O bloqueio de domínios locais permanece; consultas permitidas podem retornar SERVFAIL.
5. **I/O na thread principal:** consulta/criptografia SQLite no refresh de UI e no startup da VPN ainda merecem migração para executores e teste de ANR.
6. **Fluxos Android não exercitados:** rotação de tela durante PIN, recuperação após morte do processo, restauração no boot, mudança Wi-Fi/dados móveis, armazenamento corrompido e autorização VPN revogada.
7. **Device Owner/Lockdown:** a ativação reforçada segue bloqueada por segurança até existir túnel completo e plano de recuperação validado.
8. **Palavras-chave e página visual HTTPS:** não implementadas; não adicionar Accessibility Service invasivo como atalho.
9. **Segurança final de binário:** analisar permissões transitivas do APK, dependências e assinatura depois do build. Não é possível prometer ausência absoluta de vulnerabilidades.

## Próxima validação obrigatória

Executar `scripts/build-and-test-windows.ps1` em máquina com Android Studio/SDK 37/Gradle 9.6 e dispositivo de teste isolado. Em seguida executar manualmente `docs/MANUAL_TEST_MATRIX.md` e testes adicionais sobre todas as correções desta auditoria. Não publicar o aplicativo enquanto falhar algum bloqueador.

## Privacidade / permissões

Nenhuma nova permissão ou biblioteca foi adicionada nesta revisão. O aplicativo continua limitado aos recursos de VPN, rede, boot, foreground service, notificação e supressão de overlays. Um filtro VPN vê metadados DNS necessários ao bloqueio, não conteúdo de HTTPS. A ausência de chamadas a APIs de galeria/documentos em fonte **não comprova** ausência absoluta de vulnerabilidades do Android, das dependências ou do APK final.
