# COMPROMISSO — Check de release 0.1.4

## Arquitetura vigente

Android Kotlin + Compose, local-first, sem conta nem backend. Banco de regras cifrado AES-GCM (Keystore), PIN derivado via PBKDF2/HMAC e Keystore. VpnService intercepta somente UDP/53 para 10.111.222.1 e resolve domínios permitidos via DoT protegido, autenticado no host cloudflare-dns.com:853.

## Implementado nesta versão

- `ProtectionState` OFF / STARTING / OPERATIONAL / DEGRADED / ERROR: sem verde antes de resposta DNS autenticada.
- Sondagem inicial fixa e explícita ao domínio público `example.com` para avaliar DoT; não envolve histórico nem dados de navegação.
- Filtro local de domínios funciona mesmo sem DNS upstream, mas UI sinaliza DEGRADED.
- Pool DNS com quatro workers, fila de 64, resposta SERVFAIL na saturação e serialização da escrita de pacotes no TUN.
- Conexões DoT reaproveitadas por worker com timeout, uma tentativa de reconexão e nunca fallback para UDP em claro.
- Revisão de regras persistida e confirmação de aplicação pela VPN, com recarga de atualizações concorrentes.
- Carregamento, criptografia e alterações do banco fora da thread de UI; início VPN também lê o banco em background.
- Metadado Android desabilita Always-on até implementar túnel completo, confirmando uso seguro em modo experimental.
- Pipeline CI preparado para instalar o SDK, compilar APK, verificar permissões e executar teste instrumentado com disparo manual.

## Limites críticos NÃO resolvidos

- Não é full-tunnel. DoH nativo de aplicativos, proxies, IP direto, IPv6 e algumas consultas TCP/53 não têm cobertura garantida.
- VPN concorrente e revogação da permissão seguem controle do Android; no modo padrão o usuário pode desativar a VPN no sistema.
- Device Owner Lockdown continua DESABILITADO porque o roteamento atual poderia impedir conexões legítimas.
- Não há bloqueio de palavras-chave de conteúdo HTTPS sem inspecionar outras apps ou usar MITM — ambos fora do escopo de privacidade atual.
- Não há prova de funcionamento Android real, APK assinado para lançamento nem teste manual de resistência a bypass.
- DNS upstream em Cloudflare recebe os domínios consultados; não recebe nomes das regras armazenadas nem URLs completas do app.
- Estado OPERATIONAL significa **somente que o filtro DNS é operacional**, não que todo aplicativo esteja protegido.

## Validação necessária antes de distribuir

1. Gradle 9.6 e SDK 37: `gradle testDebugUnitTest lintDebug assembleDebug`.
2. Inspecionar permissões no APK mesclado com `scripts/apk_permission_audit.py`.
3. `gradle connectedDebugAndroidTest` em API 26 e API recente.
4. Realizar teste manual Chrome, Firefox, Brave, DoH próprio, Wi-Fi, dados móveis, reinício e falha do DoT.
5. Testar latência com dezenas de consultas concorrentes, aparelho sem conectividade, migração do banco cifrado e PIN com reinício.
6. Medir consumo de memória, CPU e bateria em dispositivo físico.
7. Antes do modo reforçado, construir e comprovar **VPN full-tunnel** com configuração recuperável sem comprometer a internet do aparelho.

**Não aprovado para release público.**
