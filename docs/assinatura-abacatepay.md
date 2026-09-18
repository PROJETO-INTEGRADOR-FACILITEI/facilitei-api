# Assinatura mensal dos prestadores

O backend usa o checkout recorrente da API v2 da AbacatePay. O preço é definido no produto da AbacatePay, não no código. Crie um produto com `cycle=MONTHLY`, sem período de teste, e configure seu ID.

## Configuração

Defina estas variáveis no ambiente da API:

| Variável | Valor |
| --- | --- |
| `ABACATEPAY_API_KEY` | Chave da API v2 com acesso a assinaturas e checkouts |
| `ABACATEPAY_MONTHLY_PRODUCT_ID` | ID do produto mensal, por exemplo `prod_...` |
| `ABACATEPAY_WEBHOOK_SECRET` | Secret do webhook, gerado no painel da AbacatePay |
| `ABACATEPAY_WEBHOOK_PUBLIC_KEY` | Chave usada pela AbacatePay para assinar webhooks com HMAC-SHA256 |

Configure um webhook HTTPS apontando para `POST /api/assinaturas/prestador/webhook`. Assine os eventos `subscription.completed`, `subscription.renewed`, `subscription.payment_failed` e `subscription.cancelled`. O secret chegará em `?webhookSecret=...`; a assinatura do corpo original chegará no header `X-Webhook-Signature`. O backend exige ambos.

Quando `ABACATEPAY_MONTHLY_PRODUCT_ID` está configurado, a listagem pública mostra apenas profissionais com mensalidade vigente. Novas solicitações e criação de serviços também exigem a mensalidade vigente. Sem essa variável, a regra de acesso permanece desabilitada para permitir implantação gradual.

## API do prestador

Os endpoints abaixo usam `Authorization: Basic <base64(email:senha)>` com as credenciais do prestador. Use somente HTTPS. Eles não aceitam um ID de prestador enviado pelo cliente; o ID vem das credenciais.

| Método e rota | Resultado |
| --- | --- |
| `GET /api/assinaturas/prestador` | Status, validade e URL do checkout |
| `POST /api/assinaturas/prestador/checkout` | Cria ou reutiliza um checkout pendente |
| `POST /api/assinaturas/prestador/cancelar` | Cancela imediatamente uma assinatura ativa |

O checkout retorna `checkoutUrl` para redirecionar o prestador. A assinatura só fica ativa após `subscription.completed` autenticado. A cada `subscription.renewed`, a validade local avança até um mês após o pagamento. Falhas de cobrança não antecipam o cancelamento; o acesso expira no fim do período já pago ou é bloqueado imediatamente ao receber `subscription.cancelled`. Eventos repetidos são ignorados pelo ID do evento.

O backend atual não fornece sessão ou token de autenticação para as demais rotas. Esta integração protege os endpoints de pagamento com Basic Auth e aplica a regra de assinatura nas listagens e no início de novos serviços. Para proteger todas as ações da plataforma por identidade, será necessário adicionar autenticação geral às rotas existentes e adaptar o cliente HTTP do frontend.

Referências: [checkout recorrente](https://docs.abacatepay.com/pages/subscriptions/create), [eventos de assinatura](https://docs.abacatepay.com/pages/webhooks/events/subscriptions), [verificação do webhook](https://docs.abacatepay.com/pages/webhooks/security).
