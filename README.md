# Compromisso

**Um cadeado, não um espião.**

Compromisso é um aplicativo Android local-first, gratuito e open source para bloqueio voluntário de domínios. O usuário escolhe o que deseja bloquear e, no **Modo Compromisso**, ações que reduzem a proteção exigem o PIN definido por uma pessoa de confiança.

> Estado atual: MVP técnico em auditoria (`0.1.3`). A filtragem já possui uma primeira implementação funcional por DNS sobre `VpnService`, mas ainda não é uma solução anti-bypass completa.

## Princípios

- sem conta e sem backend obrigatório;
- sem histórico detalhado de navegação;
- sem envio do PIN para servidores;
- regras armazenadas localmente;
- arquitetura preparada para proteção reforçada via Device Owner;
- nenhuma promessa de ser "impossível de burlar".

## Plataforma e stack

- Android nativo / Kotlin;
- **minSdk 26 (Android 8.0)**;
- `targetSdk 36`, `compileSdk 37`;
- Jetpack Compose + Material 3;
- SQLite com domínios e categorias criptografados (AES-GCM/Android Keystore) atrás de `BlockedDomainRepository`;
- `SharedPreferences` apenas para metadados de segurança/estado; o PIN nunca é armazenado;
- Android Keystore + PBKDF2-HMAC-SHA256 + HMAC-SHA256 para a credencial da pessoa de confiança;
- `VpnService` para VPN local DNS-first;
- `DevicePolicyManager` preparado para proteção reforçada.

### Por que minSdk 26?

É um ponto de equilíbrio para um produto novo em 2026: reduz a matriz de compatibilidade, mantém suporte a aparelhos relativamente antigos e cobre as APIs modernas necessárias ao serviço em primeiro plano. Always-on VPN existe desde API 24; o app ainda pode evoluir a `minSdk` sem acoplar regras de domínio à camada Android.

## Arquitetura

```text
UI (Compose)
  ↓
Policy / Controllers
  ↓
Domain
  ├── DomainPolicyEngine
  └── ProtectionMode
  ↓
Repositories
  ├── BlockedDomainRepository
  └── SecurityConfigStore
  ↓
Network
  ├── CompromissoVpnService
  ├── DnsProxyLoop
  ├── DnsOverTlsResolver
  ├── DnsCodec
  └── Ipv4UdpPacket

Security
  ├── TrustedPinManager
  └── RateLimitPolicy

Enterprise / reinforced
  ├── CompromissoDeviceAdminReceiver
  └── ReinforcedProtectionManager
```

A UI não decide se um domínio deve ser bloqueado. O `DomainPolicyEngine` é a fonte de verdade para normalização e correspondência de subdomínios.

## Política de domínios

Adicionar `example.com` bloqueia:

- `example.com`
- `www.example.com`
- `m.example.com`
- `qualquer.subdominio.example.com`

Adicionar apenas `www.example.com` **não** bloqueia `example.com`.

Entradas com URL são normalizadas para o host. Ex.: `https://www.example.com/path` vira `www.example.com`.

## Como funciona a VPN desta versão

A VPN configura um resolvedor DNS virtual (`10.111.222.1`) e roteia somente esse destino pelo TUN. Consultas DNS convencionais são lidas localmente:

- domínio bloqueado → resposta `NXDOMAIN` local;
- domínio permitido → consulta encaminhada por DNS-over-TLS (porta 853, TLS validado) à Cloudflare; indisponibilidade retorna `SERVFAIL`, sem fallback para DNS em claro;
- nenhuma URL completa ou conteúdo de página é persistido.

Isso mantém CPU/RAM/bateria baixos porque **não** cria um proxy de tráfego completo nesta fase.

### Limitação importante

Esta primeira engine não intercepta DNS-over-HTTPS independente, conexões por IP direto e outros caminhos que não passem pelo resolvedor DNS configurado pela VPN. Esses vetores estão documentados em [`docs/THREAT_MODEL.md`](docs/THREAT_MODEL.md) e serão tratados nas fases de hardening sem usar MITM abusivo ou técnicas de malware.

## Modo Normal x Modo Compromisso

### Normal

O usuário pode administrar e desligar as próprias regras.

### Compromisso

Exigem PIN da pessoa de confiança:

- desligar a proteção;
- remover um domínio;
- desativar um domínio;
- mudar do Modo Compromisso para Normal.

Adicionar ou reforçar um bloqueio não exige PIN.

A interface usa um único controle principal: ele só fica **verde** quando o `VpnService` confirmou que a VPN foi estabelecida e o worker está em execução. **Isso não é comprovação de filtragem completa de todo o tráfego**. Desligar no Modo Compromisso abre a verificação do PIN.

## Segurança do PIN

O PIN aceito no MVP tem 6–12 dígitos. O app armazena somente:

- salt aleatório;
- digest derivado;
- contador de falhas;
- instante de bloqueio temporário.

Fluxo:

```text
PIN
 ↓ PBKDF2WithHmacSHA256 (250.000 iterações + salt)
derived key
 ↓ HMAC-SHA256 com chave não exportável do Android Keystore
digest persistido
```

Após tentativas incorretas repetidas há atraso progressivo, chegando a 30 minutos.

Não existe senha mestra, backdoor ou recuperação remota.

## Build

Recomendado: Android Studio Quail 4 ou mais recente compatível com AGP 9.4.

1. Instale Android SDK Platform 37 e Build Tools 36.0.0 ou compatíveis.
2. Abra a pasta raiz no Android Studio.
3. Aguarde o Gradle Sync.
4. Rode `app` em aparelho/emulador Android 8.0+.

Na linha de comando, com Gradle 9.6+ disponível:

```bash
gradle testDebugUnitTest lintDebug assembleDebug
```

O APK debug será gerado em:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Teste em aparelho real

1. Instale o APK.
2. Conclua o onboarding.
3. Adicione `example.com` ou outro domínio de teste.
4. Ative o controle principal.
5. Aceite o diálogo oficial de criação da VPN do Android.
6. Confirme que o controle ficou verde.
7. Teste o domínio em Chrome/Firefox e em Wi‑Fi e dados móveis.
8. Configure o PIN da pessoa de confiança e mantenha o Modo Compromisso.
9. Tente desligar a proteção: o PIN deve ser exigido.
10. Reinicie o aparelho e valide o retorno da proteção. **Não habilite Lockdown nesta versão**: o túnel é somente DNS e Lockdown pode interromper outras conexões.

Use também a matriz em [`docs/MANUAL_TEST_MATRIX.md`](docs/MANUAL_TEST_MATRIX.md).

O que já foi validado e o que ainda depende de Android SDK/dispositivo está registrado em [`docs/VALIDATION.md`](docs/VALIDATION.md).

## Proteção reforçada / Device Owner

O código já contém o receiver de administração e o `ReinforcedProtectionManager`, mas o fluxo de provisionamento ainda não faz parte da UI do MVP.

Em **aparelho de teste sem contas**, o provisionamento de desenvolvimento é:

```bash
adb shell dpm set-device-owner dev.compromisso.app/.admin.CompromissoDeviceAdminReceiver
```

Device Owner não é uma permissão comum concedida por um botão. Em implantação real, exige provisionamento adequado (por exemplo QR code durante setup de dispositivo totalmente gerenciado). Consulte a documentação Android Enterprise antes de usar em aparelho principal.

**Atenção — 0.1.2:** provisionar Device Owner não habilita automaticamente a proteção reforçada. A versão atual informa separadamente se as políticas foram efetivamente confirmadas. O método de *ativação* das políticas foi bloqueado propositalmente até existir uma VPN full-tunnel validada e um mecanismo de recuperação: Lockdown em uma VPN DNS-only pode deixar o dispositivo sem acesso à internet. **Não ative manualmente Always-on/Lockdown no aparelho principal.**

A arquitetura permite futuramente Always-on VPN + Lockdown e o bloqueio de desinstalação **somente do COMPROMISSO**, após ensaios de reversibilidade e conectividade em dispositivo dedicado. Não foi aplicada restrição global a outros apps.

## Privacidade

Veja [`docs/PRIVACY.md`](docs/PRIVACY.md).

## Threat model

Veja [`docs/THREAT_MODEL.md`](docs/THREAT_MODEL.md).

## Roadmap

- **Fase 1:** app base, UI, PIN, blocklist local — implementado.
- **Fase 2:** VPN local e DNS filtering — primeira versão implementada; hardening pendente.
- **Fase 3:** boot persistence, always-on/lockdown — boot básico implementado; always-on manual/Device Owner preparado.
- **Fase 4:** Device Owner e proteção reforçada — base arquitetural implementada, UI/provisionamento pendentes.
- **Fase 5:** categorias/listas locais.
- **Fase 6:** hardening, IPv6, DoH/DoT, bypass matrix e otimização.
- **Fase 7:** assinatura, APK release e GitHub Release.

## Licença

Apache-2.0. Veja [`LICENSE`](LICENSE).

## Auditoria atual

Consulte [`docs/SECURITY-HARDENING-0.1.2.md`](docs/SECURITY-HARDENING-0.1.2.md) para o registro das correções, limitações e testes pendentes. O APK ainda não foi compilado/validado neste ambiente.

### Android build and device validation (pending)

The original 0.1.2 code passed standalone Kotlin/JVM smoke tests but has **not yet been built with Android Gradle or tested on a physical device**. The AGP 9.4 built-in Kotlin configuration was corrected in the subsequent build-readiness checkpoint. See `docs/BUILD-VALIDATION-2026-10-08.md` for verified vs pending checks. For Windows + Android Studio, use `scripts/build-and-test-windows.ps1` once SDK/Gradle/ADB are installed and a test phone is connected.

## Interface power central e Palavra do Dia (0.1.3)

A tela inicial apresenta um botão circular central, inspirado na referência visual aprovada. O aro muda
para verde **somente** quando `ProtectionRuntime.active` confirma que o serviço de VPN estabeleceu
o túnel e seu processamento foi iniciado. Clicar para desativar no Modo Compromisso mantém a
verificação do PIN. Nenhuma alteração nas permissões de leitura de dados ou na lógica de filtragem
foi necessária para o botão.

**Palavra do Dia** é um módulo independente, inicialmente **desativado**. O usuário pode habilitar
notificações e escolher um horário (padrão 08:00). Uma coleção de versículos é incluída localmente,
sem APIs de Bíblia, histórico de leitura, conta ou tráfego de rede. A seleção do versículo depende
somente do dia local: não há coleta de comportamento.

- Notificação enviada **apenas** se a opção estiver habilitada, o **Modo Compromisso** estiver
  selecionado e a **VPN estiver realmente ativa** no instante do lembrete.
- Android 13+: o sistema pedirá autorização de notificações **quando a opção for ativada**.
- Usa `AlarmManager.setAndAllowWhileIdle` **sem** permissão para alarmes exatos. O Android poderá
  atrasar a entrega por economia de bateria; não se promete chegada no minuto exato.
- Reinicialização e mudanças de horário/fuso reagendam o próximo aviso. Não há serviço em execução
  contínua nem polling para essa função.
- Não abre janelas sobre outros aplicativos. A notificação oculta o versículo na tela bloqueada.
- Habilitar, alterar o horário ou desabilitar a Palavra do Dia **não** afeta o PIN e as regras de VPN.
- Os trechos bíblicos usam formulações clássicas curtas; antes de publicar no GitHub/Play Store,
  confirme a origem e a licença da edição textual selecionada.

Arquivos: `daily/DailyVerseCatalog.kt`, `DailyVerseSchedulePolicy.kt`, `DailyVersePreferences.kt`,
`DailyVerseScheduler.kt`, `DailyVerseReceiver.kt`.

## 0.1.4 — estabilização da VPN DNS (ainda pré-release)

- A cor verde do botão é reservada a `OPERATIONAL`: túnel + resposta válida do DNS-over-TLS.
- O estado laranja `DEGRADED` significa VPN em execução, bloqueios locais aplicáveis, mas DNS externo sem confirmação; **não é proteção completa**.
- As consultas DNS externas são resolvidas com até quatro workers, uma fila limitada e TCP/TLS autenticado. Nenhum conteúdo, URL completa ou histórico é armazenado.
- A lista de regras criptografada é carregada fora da thread da interface, tem revisão persistida e confirmação após aplicação no serviço.
- Os avisos da Palavra do Dia são opcionais e ficam independentes da engine de rede.
- Always-on foi desabilitado no manifesto porque **ainda não há VPN full-tunnel**. Não usar Lockdown/Device Owner neste MVP.
- O DNS criptografado de navegadores (DoH independente), IPv6, proxy e conexão por IP ainda podem burlar o filtro. Bloqueio de palavras-chave em conteúdo HTTPS ainda não existe.

### A partir de um celular Android

O **arquivo .zip é código-fonte e não pode ser instalado como APK**. Para testar o aplicativo real, execute o workflow `Android CI` em um repositório GitHub contendo estes arquivos (preferencialmente público e open source). Ele compila `app-debug.apk`, executa testes e valida permissões; em `Actions → Android CI → Run workflow`, faz também os testes instrumentados em emulador. Baixe o artifact `COMPROMISSO-debug-APK-UNVERIFIED` do job `build` após **todos os passos do job passarem**. Instale o APK apenas num dispositivo de testes; o Android pedirá confirmação para instalação externa. Este workflow não foi executado neste ambiente e, portanto, não há APK validado anexado.

**Teste no dispositivo:** adicione `example.com`, ative o power, conceda autorização de VPN, aguarde a confirmação verde, tente `example.com` e `www.example.com` (bloqueados) e outro domínio (permitido). Desative Wi-Fi/dados: o estado deve ser laranja quando uma consulta externa falhar; confirme recuperação ao reconectar. Teste PIN incorreto, reativação, edição de regras e Palavra do Dia. Faça a comparação também com navegador que usa DoH próprio — ele ainda pode contornar esse MVP. Não configure Always-on/Lockdown.
