# Deploy — API no Render + Front no Vercel

Arquitetura: front (Vercel) e API (Render) ficam em **origens diferentes**. A
autenticacao usa cookie de sessao `HttpOnly` + CSRF. Para funcionar cross-origin
os cookies vao com `SameSite=None; Secure`, o front manda a URL absoluta da API
e o token CSRF (lido do corpo de `/auth/csrf`) no cabecalho.

## 1. Banco de dados (pre-requisito)

O Render **nao** oferece MySQL gerenciado. Provisione um MySQL 8 externo
(Aiven, Railway, Clever Cloud, PlanetScale) e guarde a connection string. Rode
as migrations do Flyway automaticamente no primeiro start da API.

## 2. API no Render

1. New > Web Service (ou "Blueprint" apontando para o `render.yaml`).
2. Runtime **Docker** (usa o `Dockerfile` da raiz). O Render injeta `PORT`; a
   API ja le `server.port=${PORT:8080}`.
3. Configure as variaveis de ambiente (ver `.env` / `render.yaml`). Essenciais
   para producao:
   - `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
   - `SESSION_COOKIE_SECURE=true`
   - `SESSION_COOKIE_SAME_SITE=none`
   - `MYAPP_CORS_ALLOWED_ORIGINS=https://SEU-FRONT.vercel.app`
   - `APP_FRONTEND_URL` e `RESET_PASSWORD_URL` apontando para o dominio do Vercel
   - AWS S3, SMTP, Mercado Pago, VAPID, `APP_ADMIN_EMAILS`
4. Deploy. Anote a URL publica, ex.: `https://facilitei-api.onrender.com`.

## 3. Front no Vercel

1. Importe o repo `facilitei-front`. Framework **Vite** (detectado). O
   `vercel.json` ja define build, output `dist` e o fallback SPA.
2. Settings > Environment Variables:
   - `VITE_API_URL=https://facilitei-api.onrender.com/api`
   - `VITE_WS_URL=wss://facilitei-api.onrender.com/buildrun-livechat-websocket`
3. Deploy. Anote a URL, ex.: `https://facilitei-front.vercel.app`, e confirme
   que ela esta em `MYAPP_CORS_ALLOWED_ORIGINS` na API.

## 4. Pos-deploy

- Configure o webhook do Mercado Pago para `https://SEU-API.onrender.com/api/assinaturas/prestador/webhook`.
- Garanta que o bucket S3 existe e esta privado; use CloudFront em `AWS_S3_PUBLIC_BASE_URL`.

## Observacoes importantes

- **Cookies de terceiros**: `SameSite=None` funciona em Chrome/Firefox/Edge, mas
  o Safari (ITP) bloqueia cookies de terceiros por padrao. Para producao robusta,
  use dominios proprios sob o mesmo dominio-raiz (ex.: `app.facilitei.com` +
  `api.facilitei.com`): ai da para usar `SameSite=Lax` com cookie de dominio
  `.facilitei.com`, evitando o problema de terceiros.
- **Cold start**: no plano free/starter do Render a API hiberna; a primeira
  requisicao apos ociosidade demora alguns segundos.
