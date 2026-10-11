# COMPROMISSO — Android

Aplicativo Android local-first, gratuito e open source para bloqueio voluntário de domínios, com PIN administrativo definido por pessoa de confiança.

**Status:** código-fonte 0.1.4 ainda não importado. Nenhum APK compilado ou validado. A engine é DNS-first e não cobre todos os contornos (DoH de terceiros, IP direto, IPv6, proxies). Não usar como proteção garantida.

## Importação e testes sem celular físico

1. Baixe `COMPROMISSO-Android-0.1.4-codigo-fonte.zip` do ChatGPT para um computador.
2. Faça upload **do ZIP sem descompactar nem renomear** em [Upload files](https://github.com/levibryxnn/compromisso-android/upload/main), na raiz da branch `main`, e confirme o commit.
3. O workflow `.github/workflows/import.yml` verificará a integridade do ZIP e extrairá o projeto, mantendo o código aberto no repositório.
4. Na mesma execução, tentará rodar testes JVM, Android Lint, gerar o APK experimental, auditar suas permissões e executar testes instrumentados em **emulador Android 35 no GitHub Actions**. Relatórios do emulador e APK serão anexados como artifacts, somente quando suas etapas forem concluídas.
5. Acompanhe em [GitHub Actions](https://github.com/levibryxnn/compromisso-android/actions). Uma execução de CI aprovada não substitui testes de rede, boot, bateria e fabricantes em um aparelho Android físico.

Não são necessários celular conectado ou pagamento de servidor para os testes emulados. O uso de runners hospedados está sujeito às políticas e cotas da sua conta GitHub.

**Privacidade:** o aplicativo não solicita acesso a arquivos pessoais, galeria, câmera, microfone, SMS ou dados de outros aplicativos. Não ativar Always-on/Lockdown nesta VPN DNS-first.
