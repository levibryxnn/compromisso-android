# COMPROMISSO 0.1.5 — Auditoria e gate de lançamento

Atualizado em 12/10/2026. Projeto Android nativo, gratuito, open-source, local-first e com identidade cristã.

## Entregas

Interface Compose premium em azul-noite e dourado, montanhas desenhadas localmente, símbolo C + cruz + caminho em Canvas e no ícone Android, botão power central e painéis funcionais para sites, Modo Compromisso, Palavra do Dia e informações de segurança. Nenhum botão depende de serviço web ou simula a proteção. O power só fica verde com DNS operacional e revisão de regras aplicada.

Onboarding rolável e atualizado para explicar limites da VPN, privacidade e propósito cristão. Ícone monocromático específico para notificações Android. Nenhuma permissão nova.

Preservados: PIN da pessoa de confiança (PBKDF2, Keystore e retentativas), regras SQLite com AES-GCM, VPN DNS-first sem fallback DNS claro, DNS sobre TLS autenticado, workers limitados, tratamento de falhas e cache em memória; screenshot da app bloqueado por FLAG_SECURE fora do teste instrumentado. Não existe coleta do histórico de navegação.

## Princípios precisos de privacidade

As regras ficam no aparelho; o app não tem conta, backend próprio ou acesso a dados de outros aplicativos. Consultas DNS permitidas são encaminhadas por TLS para Cloudflare (1.1.1.1 / cloudflare-dns.com), logo esse resolvedor externo recebe os domínios solicitados. A Palavra do Dia é totalmente embarcada; não faz chamadas de API.

Permissões previstas: INTERNET, FOREGROUND_SERVICE, FOREGROUND_SERVICE_SYSTEM_EXEMPTED, RECEIVE_BOOT_COMPLETED, POST_NOTIFICATIONS e HIDE_OVERLAY_WINDOWS. A permissão local DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION pode ser adicionada pelo AndroidX. A auditoria do APK rejeita outras permissões inesperadas.

## Pipeline CI a exigir em cada mudança

- Auditoria estática de manifesto, componentes exportados, XML e APIs sensíveis.
- Testes unitários JVM, Android Lint, geração de debug APK.
- Auditoria de permissões do APK já mesclado, impressão de SHA-256.
- Compilação da variante release minificada, sem assinatura de distribuição.
- Testes instrumentados e screenshot da interface em Android 15/API35.
- Concorrência limitada no GitHub Actions para não desperdiçar minutos gratuitos.

Evidências anteriores: 0.1.4 passou build, Lint, auditoria e testes no emulador na execução 38188603210. O preview real anterior passou na execução 38200414446. Para 0.1.5 consultar a execução mais recente em github.com/levibryxnn/compromisso-android/actions; não inferir aprovação antes do término.

## NÃO resolvido — bloqueios críticos P0 para anúncio de proteção robusta

1. VPN intercepta somente DNS UDP/53 convencional: aplicativos e navegadores com DNS criptografado próprio (DoH e DoT), TCP/53, IPv6, proxies e endereços IP diretos podem contornar. NÃO É full-tunnel.
2. O proprietário pode revogar VPN, forçar parada, resetar ou desinstalar o aplicativo fora dele. O PIN protege ações internas, não o sistema operacional.
3. Always-on e Lockdown estão DESABILITADOS: ativá-los com essa engine parcial pode deixar o telefone sem conectividade.
4. Testes físicos e medição de consumo ainda pendentes, incluindo Wi-Fi / dados móveis, reinicialização, economia de bateria, roteadores, provedores DoT indisponíveis e aparelhos de vários fabricantes.

## Pendências P1 para primeira distribuição pública

- Matriz de API26, API33 e API35+ em emulador e telefone físico; rotação, tela de 320dp, aumento de fonte, TalkBack e contraste.
- Stress do DNS, migração real v1→v2 do SQLite, falha no Keystore, restauração, reinstalação e limites de armazenamento.
- Análise de licenciamento da tradução exata de cada versículo embutido, antes de distribuição.
- Política de privacidade pública clara e atualizada; transparência sobre uso de VpnService, DNS externo, permissões e revogação.
- Rever conformidade da Play Store e requisitos vigentes de declaração de VpnService antes de enviar a loja.
- Chave de assinatura RELEASE sob guarda do mantenedor, fora do repositório. Testar a assinatura, o upgrade e integridade do artefato. Debug APK não é um instalador estável.
- Criar release versionada no GitHub APENAS após aprovação do gate.

## P2 — pós-beta

Categorias editáveis, listas opcionais, fallback/resolvedor configurável sempre criptografado, documentação avançada e políticas de rede adicionais que respeitem privacidade.

DECISÃO: v0.1.5 é beta técnico e não pode ser anunciada como filtro anti-bypass. Ainda não liberada publicamente como proteção confiável. Segurança do usuário e preservação da internet têm prioridade sobre prazo.
