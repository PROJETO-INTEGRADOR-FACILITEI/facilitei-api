# Operação administrativa e suporte

## Habilitar administradores

Defina `APP_ADMIN_EMAILS` nas variáveis do serviço da API. O valor aceita uma lista separada por vírgulas e cada endereço precisa corresponder a uma conta já cadastrada no Facilitei.

```env
APP_ADMIN_EMAILS=operador1@exemplo.com,operador2@exemplo.com
```

Depois de salvar a variável, faça um novo deploy. Sessões existentes não ganham a permissão retroativamente: o operador deve sair e entrar novamente. Quando a lista estiver vazia, nenhum usuário recebe acesso a `/api/admin/**`.

## Fluxo de atendimento

1. O usuário abre suporte geral, denúncia ou disputa pelo painel.
2. Administradores configurados recebem uma notificação e encontram o protocolo em `/admin`.
3. Um administrador assume o atendimento, analisa relato e evidências e registra respostas ou notas internas.
4. Casos resolvidos ou rejeitados exigem conclusão escrita.
5. Quando existe uma conta reportada, o administrador pode suspendê-la ou reativá-la com justificativa. A ação entra na trilha de auditoria.

Uma conta suspensa não consegue iniciar uma nova sessão e perde sessões autenticadas existentes na próxima requisição. Contas presentes em `APP_ADMIN_EMAILS` não podem ser suspensas por esse fluxo.

## Limites externos

O endereço `contato@facilitei.online` continua sendo um canal de e-mail independente. Mensagens recebidas no Zoho não são convertidas automaticamente em protocolos; essa automação exige um provedor de e-mail de entrada ou webhook específico.
