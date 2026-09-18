package psg.facilitei.DTO;

import psg.facilitei.Entity.Enum.TipoServico;

public record ResumoAvaliacaoTipoServicoDTO(
        TipoServico tipoServico,
        Double media,
        Long quantidadeAvaliacoes) {
}
