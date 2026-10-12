# Teste no Android — COMPROMISSO 0.1.4

**ATENÇÃO: a versão 0.1.4 é um projeto-fonte, não APK validado.** Não instale nem confie nesta versão como bloqueio anti-bypass completo sem build e verificação no aparelho.

## Como obter APK de teste

1. Abra a pasta do projeto num computador com Android Studio e Android SDK 37 / Java 17 / Gradle 9.6, ou publique os arquivos num repositório GitHub para usar `Actions > Android CI`.
2. No GitHub: execute `Android CI > Run workflow`. Aguarde o job `build` passar nos testes, Lint e auditoria de permissões. O artifact `COMPROMISSO-debug-APK-UNVERIFIED` conterá o APK de debug.
3. No próprio Android, baixe e extraia o artifact ZIP, toque em `app-debug.apk` e permita instalar pelo gerenciador de arquivos apenas se você confia no repositório e na origem da compilação. Desative a permissão de instalação externa depois.
4. O segundo job do workflow executa testes instrumentados em emulador. Verifique também esse resultado: **o artifact debug não é um selo de aprovação de segurança**.

Não há APK compilado dentro deste pacote. Não carregue scripts ou executáveis desconhecidos em serviços de build externos.

## Matriz de testes manuais

- **VPN ativada:** adicione `example.com`, configure PIN (somente pessoa de confiança), toque no power e aceite a autorização oficial. O power deve estar cinza durante o início e verde somente depois da resposta autenticada do DoT e da carga das regras.
- **Domínio bloqueado:** teste `example.com` e `www.example.com` no Chrome com DNS seguro desligado. Ambos devem falhar na resolução. Um domínio não listado deve resolver.
- **PIN:** no Modo Compromisso, desativar o power ou remover uma regra deve exigir PIN. PIN incorreto deve rejeitar a ação.
- **Atualização ao vivo:** com VPN ativa, adicione um domínio, depois tente acessá-lo; observe o texto de regras pendentes até o carregamento confirmado.
- **Rede indisponível:** desconecte Wi-Fi e dados; faça uma consulta permitida. O indicador deve ficar laranja quando o DNS externo não funcionar, e retornar a verde após uma consulta funcionar novamente.
- **Troca Wi-Fi ↔ dados móveis:** teste bloqueios após cada mudança de rede; o primeiro DNS externo pode falhar durante a reconexão TLS.
- **Reinício:** reinicie, abra o app e confirme se a proteção foi retomada. BootReceiver pode ser limitado pelo fabricante e não substitui Always-on.
- **Palavra do Dia:** habilite opcionalmente, configure horário próximo; confirme notificação somente com Compromisso operacional e permissão de notificações concedida.
- **Bypass conhecido:** configure DNS-over-HTTPS independente no navegador ou use conexão IP direta. Isso **pode contornar** a filtragem DNS-first. Anote como falha esperada da cobertura atual, não resultado de proteção bem-sucedida.
- **Sem acesso indevido:** veja as permissões concedidas pelo Android. O app não precisa de galeria, arquivos, SMS, câmera, microfone ou acessibilidade.

NÃO habilite Lockdown, Device Owner, VPN sempre ativa nem tente provisionamento administrativo neste estágio. Não teste no seu único aparelho sem plano de recuperação.
