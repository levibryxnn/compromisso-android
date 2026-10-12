# Arquitetura

## Decisões atuais

1. **Um módulo Gradle no MVP, pacotes por responsabilidade.** Evita custo de build e complexidade de multi-module antes de existir massa crítica. Interfaces de repositório e engines já delimitam fronteiras para extração futura.
2. **SQLite nativo atrás de repository.** A tabela atual é simples; Room ainda não oferece ganho proporcional. Se categorias/relações/migrations crescerem, `RoomBlockedDomainRepository` pode substituir a implementação sem alterar domínio/UI.
3. **SharedPreferences para estado persistente pequeno e não secreto.** O segredo real é a chave HMAC não exportável do Android Keystore. O estado de execução real da VPN não é persistido: `ProtectionRuntime` é process-local, enquanto `protectionRequested` representa apenas a intenção persistida. Migrar para DataStore posteriormente não muda o contrato de segurança.
4. **DNS-first VPN.** É a solução mais leve para provar o fluxo completo. Full-tunnel fica para hardening porque exige engine de forwarding muito mais complexa.
5. **Sem AccessibilityService.** Evita uma superfície de privilégio desnecessária e frágil para este caso de uso.
6. **Device Owner separado da proteção padrão.** Provisionamento Enterprise tem requisitos e riscos próprios; não é mascarado como permissão comum.

## Evolução prevista

- `BlockedDomainRepository` → SQLite hoje, Room/API opcional amanhã.
- `DnsProxyLoop` → substituível por `NetworkFilterEngine` full-tunnel.
- `SecurityConfigStore` → pode migrar a DataStore.
- `ReinforcedProtectionManager` → políticas adicionais sem contaminar UI/domínio.
- categorias → novo repositório/engine sem `if (competition...)`-style espalhado.
