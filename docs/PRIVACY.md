# Privacidade

O Compromisso foi desenhado para funcionar sem conta e sem servidor obrigatório.

## Dados persistidos

- domínios configurados para bloqueio;
- estado habilitado/desabilitado da regra;
- categoria quando essa função for implementada;
- modo de proteção;
- estado solicitado da VPN;
- salt e digest da credencial administrativa;
- contador/temporização de tentativas incorretas.

## Dados que não devem ser persistidos

- histórico completo de navegação;
- URLs completas;
- termos de busca;
- conteúdo de páginas;
- mensagens;
- credenciais de sites;
- lista de tentativas de acesso bloqueadas.

A engine DNS observa o hostname em memória para aplicar regras, sem gravar histórico. Para domínios permitidos, envia a consulta criptografada a um resolvedor DNS externo; esse provedor vê as consultas e metadados de rede necessários para prestar o serviço.

## Rede

Consultas DNS permitidas são encaminhadas por **DNS-over-TLS (TLS/853)** para `cloudflare-dns.com` no endereço `1.1.1.1`, usando validação de certificado/hostname. **Cloudflare pode observar domínios consultados**, embora o Compromisso não transmita PIN, listas completas ou histórico a servidores próprios. Não há fallback UDP/53 quando o servidor TLS estiver indisponível. O destino e as implicações de privacidade deverão ser revistos antes de release público.

## Palavra do Dia (opcional, 0.1.3)

A preferência de habilitação e o horário escolhido ficam em `SharedPreferences` privadas do próprio
COMPROMISSO. O versículo é selecionado por data local a partir de um catálogo embutido no APK.
Não há API bíblica, acesso à galeria, outros aplicativos, leituras externas, histórico de notificações
ou registro de quais versículos o usuário abriu. O aviso só aparece com Modo Compromisso e VPN
realmente ativos. O canal permite ao usuário desativar os avisos pelo sistema Android. O conteúdo
da mensagem é ocultado na visualização pública da tela bloqueada.
