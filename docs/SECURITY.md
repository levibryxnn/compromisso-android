# Segurança

## Credencial administrativa

- formato MVP: PIN numérico de 6–12 dígitos;
- PBKDF2-HMAC-SHA256, 250.000 iterações, salt de 128 bits;
- resultado autenticado com HMAC-SHA256 usando chave não exportável do Android Keystore;
- comparação em tempo constante via `MessageDigest.isEqual`;
- rate limiting local progressivo após falhas;
- nenhum segredo hardcoded;
- nenhum backdoor;
- backup Android desativado no manifesto para evitar restaurar metadados sem a chave Keystore correspondente.

## Limites

PIN numérico tem entropia menor que senha longa. O pepper em Keystore dificulta ataque offline contra o arquivo de preferências, mas não transforma um PIN curto em uma senha de alta entropia. Uma fase futura pode oferecer passphrase opcional.

## Relato de vulnerabilidade

Antes de um release público estável, será criado um fluxo de security advisory no repositório GitHub. Não publicar PINs, dumps de dispositivo ou dados pessoais em issues públicas.

## Atualização 0.1.1

Regra de backup/migração explícita, `FLAG_SECURE`, bloqueio de overlays no Android 12+, teclado de PIN interno, blocklist criptografada e consultas DNS externas via TLS com verificação de certificado. Consulte `SECURITY-HARDENING-0.1.1.md`. **Nenhuma garantia de segurança absoluta; APK e testes instrumentados pendentes.**

## Módulo opcional de versículos (0.1.3)

- `DailyVerseReceiver` é explicitamente `exported=false` e só aceita a ação interna do alarme.
- Usa `POST_NOTIFICATIONS` que **já constava** no manifesto; pede permissão apenas ao habilitar.
- Usa alarmes inexatos sem `SCHEDULE_EXACT_ALARM` e sem acesso a arquivos ou outros aplicativos.
- Falhas de agendamento não são propagadas à engine de VPN/PIN; o estado da opção é revertido
  quando não foi possível configurar o agendamento inicial.
- Mensagens são estáticas e locais, exibidas somente com VPN ativa e Modo Compromisso.
- Privacidade: sem conteúdo bíblico exposto na versão pública da notificação em tela bloqueada.
- A auditoria estática de componentes e permissões agora inclui o novo receiver.
