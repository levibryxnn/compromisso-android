# Matriz manual de bypass / regressão

Preencher em aparelho real antes de cada release candidata.

| Cenário | Esperado MVP | Resultado |
|---|---|---|
| Chrome normal | domínio DNS bloqueado | ☐ |
| Chrome anônimo | domínio DNS bloqueado | ☐ |
| Firefox normal | domínio DNS bloqueado se usar resolver do sistema | ☐ |
| Firefox privado | idem | ☐ |
| Brave | domínio DNS bloqueado se usar resolver do sistema | ☐ |
| Edge | domínio DNS bloqueado se usar resolver do sistema | ☐ |
| WebView | domínio DNS bloqueado | ☐ |
| Wi‑Fi | bloqueio mantém | ☐ |
| 4G/5G | bloqueio mantém | ☐ |
| alternar Wi‑Fi ↔ dados | serviço continua | ☐ |
| reboot | proteção solicitada retorna após BOOT_COMPLETED | ☐ |
| domínio exato | bloqueado | ☐ |
| `www.` | bloqueado quando apex está na lista | ☐ |
| subdomínio profundo | bloqueado | ☐ |
| domínio parecido (`notexample.com`) | permitido | ☐ |
| remover regra no Modo Compromisso | exige PIN | ☐ |
| desativar regra no Modo Compromisso | exige PIN | ☐ |
| desligar proteção no Modo Compromisso | exige PIN | ☐ |
| 5º PIN incorreto | inicia rate limit | ☐ |
| DNS-over-HTTPS próprio | conhecido como bypass atual; registrar comportamento | ☐ |
| DNS privado | registrar comportamento por aparelho/API | ☐ |
| VPN externa | registrar comportamento | ☐ |
| captive portal | não deve inutilizar conexão sem explicação | ☐ |

## Aparelhos mínimos sugeridos

- Android 8/9 (API baixa suportada);
- Android 12/13;
- Android 14/15;
- Android 16/17 quando disponíveis no laboratório;
- ao menos Samsung, Motorola e Pixel/AOSP se possível.
