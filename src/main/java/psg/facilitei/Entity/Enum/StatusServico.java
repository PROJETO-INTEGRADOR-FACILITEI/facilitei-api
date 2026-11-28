package psg.facilitei.Entity.Enum;

/**
 * Status de um {@link psg.facilitei.Entity.Servico}.
 * Os marcados "sem tratamento no front" ainda não aparecem na tela, mas continuam
 * aqui porque pode haver linhas no banco com esses valores.
 */
public enum StatusServico {
    SOLICITADO,        // Cliente solicitou o serviço
    AGUARDANDO_CONTATO, // Aguardando retorno do prestador (sem tratamento no front)
    EM_ANALISE,         // Avaliando a viabilidade (sem tratamento no front)
    APROVADO,           // Serviço aprovado e agendado (sem tratamento no front)
    EM_ANDAMENTO,       // Serviço está sendo realizado
    PAUSADO,            // Serviço temporariamente interrompido (sem tratamento no front)
    FINALIZADO,         // Serviço concluído
    CANCELADO,          // Serviço cancelado pelo cliente ou prestador
    NAO_COMPARECEU,     // Prestador ou cliente não compareceu (sem tratamento no front)
    PENDENTE,
    PENDENTE_APROVACAO, // Trabalhador finalizou, aguardando cliente aprovar
    RECUSADO
}
