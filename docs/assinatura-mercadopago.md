# Assinatura mensal dos profissionais com Mercado Pago

O backend cria uma assinatura recorrente mensal pelo endpoint `/preapproval` do Mercado Pago. O profissional conclui o pagamento no checkout hospedado e a conta só é liberada depois de um webhook válido e de uma confirmação servidor a servidor na API do Mercado Pago.

## Configuração

Defina estas variáveis no ambiente da API:

| Variável | Descrição |
| --- | --- |
| `LEGAL_PAYMENTS_MODE` | `TEST` ou `LIVE`; use `TEST` enquanto a operação jurídica não estiver completa |
| `MERCADOPAGO_TEST_ACCESS_TOKEN` | Credencial de teste, obrigatoriamente iniciada por `TEST-` |
| `MERCADOPAGO_LIVE_ACCESS_TOKEN` | Credencial de produção dedicada, obrigatoriamente iniciada por `APP_USR-` |
| `MERCADOPAGO_WEBHOOK_SECRET` | Assinatura secreta configurada no painel do Mercado Pago |
| `MERCADOPAGO_MONTHLY_AMOUNT_CENTS` | Valor mensal em centavos; por exemplo, `4990` para R$ 49,90 |
| `MERCADOPAGO_REASON` | Descrição exibida na assinatura |
| `MERCADOPAGO_BASE_URL` | Opcional; padrão `https://api.mercadopago.com` |
| `APP_FRONTEND_URL` | Origem pública do frontend, sem barra final |

Enquanto a credencial do ambiente estiver vazia ou o valor mensal for zero, a cobrança fica desabilitada. A variável antiga `MERCADOPAGO_ACCESS_TOKEN` é aceita temporariamente apenas em `TEST`, se contiver uma credencial de teste, e nunca funciona como fallback em `LIVE`.

O modo `LIVE` falha fechado ainda na inicialização se faltar nome legal, CPF/CNPJ, endereço empresarial, e-mail de contato, valor mensal ou credencial LIVE dedicada. O checkout também exige que o profissional aceite a versão vigente da Política de Assinatura, Cancelamento e Reembolso.

### Checklist no Railway

Cadastre os nomes abaixo nas variáveis do serviço da API, sem colocar valores ou segredos na documentação ou no repositório:

- `LEGAL_OPERATOR_TYPE`, `LEGAL_DISPLAY_NAME`, `LEGAL_LEGAL_NAME`, `LEGAL_TAX_ID`, `LEGAL_BUSINESS_ADDRESS` e `LEGAL_CONTACT_EMAIL`;
- `LEGAL_PAYMENTS_MODE` (mantenha `TEST` até a identidade estar completa);
- `MERCADOPAGO_TEST_ACCESS_TOKEN` para os testes;
- `MERCADOPAGO_LIVE_ACCESS_TOKEN` somente quando a operação estiver pronta para cobrança real;
- `MERCADOPAGO_WEBHOOK_SECRET`, `MERCADOPAGO_MONTHLY_AMOUNT_CENTS` e `MERCADOPAGO_REASON`.

Não use a mesma credencial nos dois ambientes e não copie tokens para tickets, prints, commits ou mensagens.

## Webhook

Cadastre no painel do Mercado Pago uma URL HTTPS pública apontando para:

```text
POST https://SEU_DOMINIO/api/assinaturas/prestador/webhook
```

Habilite os tópicos `subscription_preapproval` e `subscription_authorized_payment`. O backend valida `x-signature` com HMAC-SHA256, confere `x-request-id` e `data.id`, limita a idade da assinatura e consulta o recurso diretamente no Mercado Pago antes de alterar o acesso do profissional. O corpo ou os parâmetros do webhook, sozinhos, nunca ativam uma conta.

## API do profissional

As rotas usam a sessão HTTP criada em `POST /api/auth/login`:

| Método e rota | Resultado |
| --- | --- |
| `GET /api/assinaturas/prestador` | Retorna status, validade, URL do checkout e o ambiente de pagamento |
| `POST /api/assinaturas/prestador/checkout` | Cria ou reutiliza uma assinatura pendente |
| `POST /api/assinaturas/prestador/cancelar` | Cancela a assinatura no Mercado Pago e localmente |

O retorno do checkout leva o profissional para `${APP_FRONTEND_URL}/painel/assinatura`. Pagamentos aprovados renovam `activeUntil` conforme a próxima cobrança informada pelo provedor. Eventos repetidos são ignorados por uma chave idempotente. Uma assinatura cancelada não é reativada por um webhook antigo.

## Migração do provedor anterior

As migrations `V10__migrate_payment_provider_to_mercadopago.sql` e `V11__default_payment_provider_to_mercadopago.sql` preservam o histórico, renomeiam a tabela de eventos, encerram localmente assinaturas pendentes ou ativas do provedor anterior e passam a usar Mercado Pago como padrão. Antes de publicar a versão, cancele também qualquer recorrência real ainda ativa no painel do provedor anterior para evitar cobranças duplicadas.
