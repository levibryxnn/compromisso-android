# COMPROMISSO 0.1.2 — Continuação da auditoria (2026-10-08)

**Estado: revisão de fonte/testes puros, NÃO aprovado para distribuição.** Base: 0.1.1, código-fonte verificado contra checkpoint SHA-256.

## Alterações feitas

1. **Pacotes IPv4/UDP:** validação do checksum IPv4, tamanho declarado, destino DNS virtual e rejeição de pacotes fragmentados, inclusive com bit *more fragments* (MF). Rejeita pacotes inválidos antes de interpretá-los como DNS.
2. **DNS-over-TLS:** não aceita resposta de transação distinta, opcode diferente, domínio diferente ou QTYPE/QCLASS diferentes. Compara o domínio sem diferenciar maiúsculas/minúsculas. Mantém TLS com validação de certificado e falha fechada.
3. **PIN:** temporização combinando relógio de parede e `elapsedRealtime`, resistente a adiantar o relógio *sem reiniciar o aparelho*. Entradas que não tenham formato de PIN válido são rejeitadas antes de PBKDF2. Atualização de PIN e Modo Compromisso agora é atômica na mesma transação de preferências; evita falha em que um crash deixasse NORMAL após instalar PIN.
4. **Device Owner:** a UI diferencia provisionamento da efetiva confirmação das políticas. Ativação de Lockdown pela aplicação permanece desabilitada deliberadamente: DNS-only + Lockdown pode bloquear toda a conectividade. Não há alteração global de controle de apps ou usuários.
5. **Início da VPN:** tentativa de iniciar o serviço em primeiro plano agora desfaz a intenção persistida quando rejeitada pelo Android e exibe erro sem registrar conteúdo sensível.
6. **UI:** `FLAG_SECURE` e `setHideOverlayWindows(true)` (Android 12+) para reduzir captura e sobreposição.
7. **Domínios:** suporte a IDN Unicode via IDNA, mantendo regras de host e subdomínio. Rejeita esquemas diferentes de HTTPS/HTTP.
8. **Revisão contínua:** verificação estática com allowlist **exata** dos componentes Android; CI preparada para conferir permissões efetivas do APK por `aapt`, inclusive se introduzidas por dependências.
9. **Testes:** novos casos unitários de respostas DNS, falhas no parser e relógio; teste instrumentado de atomicidade da configuração do PIN; smoke compilado Kotlin independente.

## Limites e riscos restantes

- A VPN continua DNS-only, não full-tunnel: DoH próprio de navegador, IPv6, IP direto, proxy e VPN concorrente podem contornar. **Não habilitar Lockdown** até uma arquitetura de rede completa, recuperável e testada em dispositivo.
- Um atacante com privilégio root ou controle completo sobre backup/imagem do dispositivo pode manipular dados, alterar o relógio após reboot ou apagar o app. O controle monotônico do PIN não é um relógio confiável de hardware entre reinícios.
- Não existe leitura de galeria, arquivos pessoais, dados bancários ou apps vizinhos; porém o `VpnService` precisa observar **domínios consultados** para filtrá-los. DNS permitido é enviado ao resolvedor Cloudflare por TLS; não há garantia de invisibilidade para o resolvedor externo.
- Histórico de domínios não é persistido no app; o SQLite armazena **regras do usuário** criptografadas com Keystore. Cryptographic erase de flash/WAL antigo não é garantido.
- Palavras-chave em conteúdo/URLs HTTPS não são filtradas; isso exigiria capacidades invasivas que conflitam com o princípio de mínimo privilégio.
- Revisão estática e testes puros não provam a correção da integração Android nem garantem inexistência de vulnerabilidades.

## Validações locais

- SHA-256 da base 0.1.1 conferido com o checkpoint anterior.
- `PURE_SECURITY_SMOKE_OK`, Kotlin compilado; 40.000 entradas aleatórias sem crash.
- `SOURCE_SECURITY_GATE_OK`, manifesto e XML parseados.
- `APK_AUDITOR_PARSER_OK` com exemplo válido e uma permissão proibida injetada.
- `git diff --check` sem erros de whitespace.

## Bloqueios de release

1. SDK Android + Gradle: `testDebugUnitTest`, `lintDebug`, `assembleDebug`, `assembleRelease`.
2. Executar `scripts/apk_permission_audit.py` **sobre APK real** e conferir manifesto mesclado, exportados e inventário de dependências.
3. `connectedDebugAndroidTest`: SQLite 1→2, Keystore, persistência, PIN/crash e teste de rede DoT.
4. Matriz em aparelho real: Wi-Fi, LTE, IPv6, DNS privado, DoH e reinicialização.
5. Testes da VPN full-tunnel e procedimento de recuperação antes de habilitar Device Owner/Lockdown.
6. Revisão de segurança independente e verificação da cadeia de assinatura do APK de release.
