package psg.facilitei.Entity.Enum;

/**
 * Status do ciclo de vida de um {@link psg.facilitei.Entity.Servico}.
 *
 * O frontend atual (repositório separado) só trata explicitamente:
 * SOLICITADO, PENDENTE, EM_ANDAMENTO, PENDENTE_APROVACAO, FINALIZADO,
 * CANCELADO, RECUSADO.
 *
 * Os estados abaixo existem no backend mas ainda não têm tratamento visual
 * ou de transição no frontend. Mantidos (não removidos) para não quebrar
 * `ddl-auto=update` sobre linhas já persistidas com esses valores; ativar
 * o tratamento no frontend antes de considerá-los "prontos para uso".
 */
public enum StatusServico {
    SOLICITADO,        // Cliente solicitou o serviço
    AGUARDANDO_CONTATO, // Aguardando retorno do prestador — sem tratamento no frontend
    EM_ANALISE,         // Avaliando a viabilidade — sem tratamento no frontend
    APROVADO,           // Serviço aprovado e agendado — sem tratamento no frontend
    EM_ANDAMENTO,       // Serviço está sendo realizado
    PAUSADO,            // Serviço temporariamente interrompido — sem tratamento no frontend
    FINALIZADO,         // Serviço concluído
    CANCELADO,          // Serviço cancelado pelo cliente ou prestador
    NAO_COMPARECEU,     // Prestador ou cliente não compareceu — sem tratamento no frontend
    PENDENTE,
    PENDENTE_APROVACAO, // Trabalhador finalizou, aguardando cliente aprovar
    RECUSADO
}
