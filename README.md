# COMPROMISSO

**Fé, disciplina e escolhas conscientes.**
Um cadeado, não um espião.

Aplicativo Android gratuito, open source e local-first para ajudar o usuário a cumprir um compromisso voluntário de restringir sites. Seu foco é disciplina pessoal com base cristã e respeito à privacidade: a pessoa de confiança escolhe o PIN de administração e o próprio usuário continua sabendo como a proteção funciona.

**Estado: 0.1.5 — BETA TÉCNICO. Ainda não aprovado para lançamento público.**

## Experiência

- Interface premium nativa em Jetpack Compose, azul-noite e dourado, símbolo C + cruz + caminho.
- Botão power central: **verde somente quando o filtro DNS está operacional e as regras estão aplicadas**. Não significa filtragem total de todo o aparelho.
- Sites bloqueados e subdomínios, lista criptografada e opção de remover/ativar mediante PIN quando há compromisso ativo.
- Palavra do Dia: versículo offline e lembrete local opcional, sem servidor de conteúdo.
- Modo Normal e Modo Compromisso; PIN definido por pessoa de confiança para alterações que enfraqueçam a proteção.
- Onboarding com explicação transparente sobre o uso de VPN e suas limitações.

## Privacidade e limitações — leia antes de usar

As regras são armazenadas e avaliadas **localmente** usando SQLite, AES-GCM e Android Keystore. Não há conta, backend, publicidade, coleta de histórico de navegação ou leitura de aplicativos e arquivos pessoais.

A VPN filtra **DNS convencional** (não todo o tráfego). O DNS permitido é encaminhado por **DNS-over-TLS à Cloudflare**; portanto a Cloudflare poderá receber os domínios consultados. O aplicativo não registra um histórico individual de DNS.

**Limitações importantes:** aplicativos que utilizam DNS criptografado próprio (DoH/DoT), IPv6, proxy ou conexão direta por IP podem contornar o bloqueio. O usuário pode desativar a VPN nas configurações do Android ou desinstalar o app. PIN de pessoa de confiança não protege o sistema operacional. Always-on/Lockdown permanece desabilitado porque esta VPN é parcial e seu uso inadequado poderia deixar o aparelho sem internet.

Para obter bloqueio mais forte serão necessários um projeto de VPN full-tunnel, análise de privacidade e testes extensos antes de permitir Always-on.

## Requisitos para desenvolvimento

- Android API mínima 26 (Android 8.0).
- Kotlin, Compose Material 3, Android Gradle Plugin 9.4, Gradle 9.6, Java 17.
- compileSdk 37.0; targetSdk 36.
- Android SDK Build Tools 36.0.0.

## Build e testes

Em máquina com SDK configurado:

    gradle testDebugUnitTest lintDebug assembleDebug connectedDebugAndroidTest

APK **experimental**: app/build/outputs/apk/debug/app-debug.apk

Ou use **GitHub Actions** → COMPROMISSO Android CI → artefatos. O pipeline inclui auditoria estática, testes JVM, Lint, verificação do APK já mesclado, compilação release sem assinatura, testes em emulador Android 15 e captura da interface.

A compilação release sem assinatura **não** é um APK final distribuível. Não coloque keystores, certificados privados ou senhas no repositório.

## Segurança

- PIN 6–12 dígitos: derivado com PBKDF2-HMAC-SHA256 e confirmado via HMAC do Android Keystore; repetição de PIN incorreto sofre limitação progressiva.
- Regras criptografadas por AES-GCM e fingerprints HMAC para unicidade.
- Filtro DNS com workers limitados e resolução upstream apenas criptografada, nunca fallback a DNS em texto claro.
- Permissões do APK verificadas por lista explícita; a política não requer câmera, microfone, SMS, armazenamento ou acessibilidade.
- Componente VPN protegido pela permissão Android BIND_VPN_SERVICE; Modo Device Owner não habilitado.
- FLAG_SECURE protege a tela contra capturas em uso normal. Somente instrumentação de testes remove temporariamente a flag para screenshot de interface.

## Estado para lançamento

A revisão de riscos e **critérios de bloqueio** está em [docs/RELEASE-GATE-0.1.5.md](docs/RELEASE-GATE-0.1.5.md). Não promover esta versão a produto anti-bypass. Testes em telefone físico, auditoria de conectividade, licenciamento dos textos bíblicos, assinatura segura de produção e documentação de distribuição permanecem necessários.

Documentos complementares: [Arquitetura](docs/ARCHITECTURE.md), [Privacidade](docs/PRIVACY.md), [Modelo de ameaças](docs/THREAT_MODEL.md) e [Matriz de testes](docs/MANUAL_TEST_MATRIX.md).

## Licença

Código sob Apache-2.0, conforme LICENSE. O licenciamento/atribuição da edição bíblica usada nas citações deve ser validado antes do lançamento.
