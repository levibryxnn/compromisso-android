# Threat Model — Compromisso

Objetivo de segurança: **tornar a burla difícil, deliberada e não impulsiva**, sem alegar impossibilidade absoluta e sem retirar o controle legítimo do proprietário por técnicas abusivas.

## Perfis

- **Proteção padrão:** app comum + `VpnService` local.
- **Proteção reforçada:** aparelho voluntariamente provisionado como Device Owner + Always-on VPN/Lockdown e políticas oficiais.

Legenda:

- **PROTEGIDO:** existe mitigação efetiva na arquitetura-alvo usando API oficial.
- **PARCIALMENTE PROTEGIDO:** há mitigação, mas permanecem caminhos relevantes.
- **NÃO PROTEGÍVEL:** não existe garantia realista para um app Android convencional com o usuário em controle físico do aparelho.

| Vetor | Padrão atual | Reforçado alvo | Mitigação / observação |
|---|---|---|---|
| Trocar Chrome por Firefox/Brave/Edge/Opera | PROTEGIDO para DNS convencional | PROTEGIDO | Regra é de rede, não extensão de navegador. |
| Modo anônimo | PROTEGIDO para DNS convencional | PROTEGIDO | Incógnito não altera a VPN do SO. |
| Limpar dados do navegador | PROTEGIDO para DNS convencional | PROTEGIDO | Regras vivem no app/VPN. |
| Wi‑Fi → 4G/5G | PROTEGIDO para DNS convencional | PROTEGIDO | VPN é por dispositivo, não por rede Wi‑Fi. |
| Reiniciar | PARCIALMENTE PROTEGIDO | PROTEGIDO | BootReceiver tenta restaurar após boot; Always-on é a proteção forte. |
| DNS privado / DoT | PARCIALMENTE PROTEGIDO | PARCIALMENTE PROTEGIDO | Precisa de testes por versão/OEM e política de VPN completa. |
| DNS-over-HTTPS no navegador/app | NÃO PROTEGIDO no DNS-first atual | PARCIALMENTE PROTEGIDO | Exige full-tunnel/controle de endpoints; ECH reduz visibilidade de host. |
| Alterar DNS manualmente | PARCIALMENTE PROTEGIDO | PROTEGIDO | No reforçado, Always-on/Lockdown + política de VPN; padrão depende do caminho usado. |
| VPN concorrente | PARCIALMENTE PROTEGIDO | PROTEGIDO | Android só mantém uma VPN por usuário; Device Owner pode fixar Always-on e lockdown. |
| Proxy configurado manualmente | PARCIALMENTE PROTEGIDO | PARCIALMENTE PROTEGIDO | DNS-first pode ser contornado por alguns proxies. |
| Forçar parada do app | PARCIALMENTE PROTEGIDO | PROTEGIDO/PARCIAL | Always-on reinicia VPN; comportamento deve ser testado por OEM. |
| Desativar VPN nas Configurações | NÃO PROTEGIDO contra usuário determinado | PROTEGIDO | Device Owner pode configurar Always-on VPN com lockdown. |
| Remover permissão VPN | NÃO PROTEGIDO contra usuário determinado | PROTEGIDO | Reforçado fixa política; padrão não controla Settings. |
| Desinstalar app | NÃO PROTEGIDO | PROTEGIDO | `DevicePolicyManager.setUninstallBlocked` quando Device Owner. |
| Limpar dados do app | NÃO PROTEGIDO | PARCIALMENTE PROTEGIDO | Proteção de Settings exige políticas adicionais e varia por gestão/OEM. |
| ADB | NÃO PROTEGIDO | PARCIALMENTE PROTEGIDO | Device Owner pode restringir debugging; proprietário físico ainda pode resetar/provisionar novamente. |
| Modo seguro | NÃO PROTEGIDO | PARCIALMENTE PROTEGIDO | `DISALLOW_SAFE_BOOT` pode ser usado no reforçado quando compatível. |
| Trocar usuário/perfil | NÃO PROTEGIDO | PARCIALMENTE PROTEGIDO | Pode ser restringido em dispositivo totalmente gerenciado; precisa de validação multiusuário. |
| Links abertos por outros apps/WebViews | PROTEGIDO para DNS convencional | PROTEGIDO | Filtro não depende do navegador. |
| IP direto | NÃO PROTEGIDO | NÃO PROTEGIDO por regra de domínio pura | Requer regra IP/full-tunnel; CDN/shared IP torna bloqueio por IP impreciso. |
| Subdomínios | PROTEGIDO | PROTEGIDO | `example.com` cobre `*.example.com`. |
| IPv6 | PARCIALMENTE PROTEGIDO | PARCIALMENTE PROTEGIDO | DNS via IPv4 virtual funciona, mas full IPv6 interception ainda não foi implementada. |
| QUIC / HTTP/3 | PARCIALMENTE PROTEGIDO | PARCIALMENTE PROTEGIDO | DNS-first bloqueia antes da conexão se o app usar resolver interceptado; DoH embutido pode escapar. |
| Captive portal | PARCIALMENTE PROTEGIDO | PARCIALMENTE PROTEGIDO | Precisa de exceções cuidadosamente definidas para não quebrar login em Wi‑Fi. |
| Factory reset | NÃO PROTEGÍVEL | NÃO PROTEGÍVEL | Apaga app/políticas em cenário de posse física. |
| Recovery | NÃO PROTEGÍVEL | NÃO PROTEGÍVEL | Fora do controle de app convencional. |
| Bootloader desbloqueado / ROM nova | NÃO PROTEGÍVEL | NÃO PROTEGÍVEL | Fora do modelo de ameaça. |

## Ameaças internas ao próprio app

- PIN nunca em texto puro.
- Sem senha mestra.
- Sem backend de recuperação.
- Sem logs de URL completa.
- Ações que reduzem proteção passam por uma única autorização (`TrustedPinManager`).
- Regras de domínio ficam fora da UI.
- Atualização da blocklist para a VPN é orientada a evento, não polling.

## Próximo hardening

1. full-tunnel seletivo ou estratégia de bloqueio de resolvers DoH conhecidos;
2. suporte e testes IPv6;
3. proteção de DNS privado conforme comportamento real por API/OEM;
4. políticas Device Owner reversíveis e testadas;
5. validar transições de estado da VPN sob kill/restart do processo e variações de OEM;
6. testes de captive portal e falha de DNS para evitar lockout de rede.
