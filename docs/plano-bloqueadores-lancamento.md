# Plano de saneamento dos bloqueadores de lançamento

## Objetivo

Permitir um beta controlado sem expor credenciais, senhas, dados de serviços,
portfólios ou conversas entre usuários.

## Fase 0 — ação externa imediata

- [ ] Revogar a credencial antiga do Cloudinary no painel do provedor.
- [ ] Criar uma credencial AWS nova com o menor privilégio possível.
- [ ] Configurar `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_S3_BUCKET`
  e `AWS_REGION` somente no ambiente de execução.
- [ ] Invalidar caches e credenciais de ambientes que usavam a chave antiga.
- [ ] Decidir, com a equipe, uma janela para reescrever o histórico Git. Essa
  operação exige `force push` coordenado e não deve ser feita automaticamente.

## Fase 1 — credenciais e autenticação

- [x] Remover valores do Cloudinary do código e migrar uploads para AWS S3.
- [x] Gravar novas senhas com BCrypt (custo 12).
- [x] Migrar senhas legadas para BCrypt na inicialização, mantendo o login
  como segunda proteção idempotente.
- [x] Aplicar BCrypt também na redefinição de senha.
- [x] Exigir senhas de 12 a 72 caracteres no backend e no frontend.
- [x] Integrar a sessão existente ao Spring Security.

## Fase 2 — autorização e isolamento de dados

- [x] Exigir autenticação para serviços, solicitações, clientes, avaliações e chat.
- [x] Validar dono/participante em leitura e escrita de serviços.
- [x] Restringir listagens a registros do usuário autenticado.
- [x] Validar dono do portfólio antes de adicionar ou remover imagens.
- [x] Impedir alteração direta de notas fora do fluxo de avaliações.
- [x] Validar autor e participantes antes de criar ou remover avaliações.
- [x] Restringir transições de status por papel e pelo estado atual.

## Fase 3 — navegador, WebSocket e abuso

- [x] Publicar token CSRF em cookie e validar o cabeçalho em mutações HTTP.
- [x] Retornar 401 para sessão ausente e 403 para acesso sem permissão.
- [x] Exigir sessão no handshake WebSocket.
- [x] Autorizar `SEND` e `SUBSCRIBE` somente para participantes do serviço.
- [x] Derivar o remetente da sessão, ignorando identidade enviada pelo navegador.
- [x] Limitar login, recuperação, redefinição, cadastro e upload por IP.
- [x] Restringir origens do WebSocket à mesma lista CORS da aplicação.
- [x] Restringir uploads a imagens de até 10 MB.

## Fase 4 — comunicação honesta

- [x] Remover promessas de verificação documental e antecedentes.
- [x] Remover a promessa de mediação formal enquanto esse processo não existir.
- [ ] Só reintroduzir selo de verificação depois de criar coleta de documentos,
  consentimento, análise administrativa, auditoria e política de retenção.

## Critérios antes do beta

- Todos os testes automatizados devem passar.
- Frontend deve passar em lint e build de produção.
- Testes negativos devem confirmar que usuário A não lê nem altera dados de B.
- Checkout recorrente e webhook devem ser validados com credenciais de teste do Mercado Pago.
- Cookies devem usar `Secure=true` em produção e todo o tráfego deve usar HTTPS.
- A chave antiga do Cloudinary deve estar revogada, não apenas removida do código.

## Próxima evolução do rate limit

O limitador atual é local por processo, adequado para uma única instância no beta.
Antes de escalar horizontalmente, mover os contadores para Redis ou para o API
gateway e aceitar `X-Forwarded-For` somente de proxies confiáveis.
