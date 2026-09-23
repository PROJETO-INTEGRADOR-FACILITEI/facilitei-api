# Armazenamento de imagens no AWS S3

Uploads de portfólio e anexos de imagem são gravados no S3. O banco guarda a URL de exibição e a chave do objeto, usada para remoção segura.

## Configuração

| Variável | Descrição |
| --- | --- |
| `AWS_ACCESS_KEY_ID` | Credencial com acesso restrito ao bucket |
| `AWS_SECRET_ACCESS_KEY` | Segredo da credencial |
| `AWS_S3_BUCKET` | Nome do bucket |
| `AWS_REGION` | Região do bucket; padrão `us-east-1` |
| `AWS_S3_PUBLIC_BASE_URL` | URL pública sem barra final, preferencialmente uma distribuição CloudFront |
| `AWS_S3_ENDPOINT` | Opcional; endpoint de LocalStack, MinIO ou serviço compatível com S3 |

Sem `AWS_S3_BUCKET`, a API inicia normalmente, mas endpoints de upload retornam indisponibilidade. Isso evita que uma configuração incompleta grave arquivos no destino errado.

## Segurança recomendada

- Mantenha o bucket privado e sirva as imagens por CloudFront.
- Restrinja a credencial da aplicação a `s3:PutObject` e `s3:DeleteObject` no prefixo usado pelo Facilitei.
- Não versione access keys nem use credenciais de usuário administrador.
- Configure CORS e políticas públicas apenas quando forem realmente necessários.

A API aceita JPEG, PNG, WebP, GIF e AVIF, com limite de 10 MB por arquivo. Os objetos recebem nomes UUID e cabeçalho de cache imutável.

