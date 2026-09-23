RENAME TABLE evento_abacatepay TO evento_pagamento;

ALTER TABLE evento_pagamento
    MODIFY COLUMN id VARCHAR(180) NOT NULL;

ALTER TABLE assinatura_prestador
    ADD COLUMN provider VARCHAR(30) NOT NULL DEFAULT 'ABACATEPAY' AFTER trabalhador_id;

-- Checkouts do provedor anterior não podem ser controlados pelo Mercado Pago.
-- Eles ficam encerrados localmente; antes do deploy, cancele recorrências reais no provedor anterior.
UPDATE assinatura_prestador
SET status = 'CANCELLED', active_until = NULL
WHERE provider = 'ABACATEPAY' AND status IN ('PENDING', 'ACTIVE');
