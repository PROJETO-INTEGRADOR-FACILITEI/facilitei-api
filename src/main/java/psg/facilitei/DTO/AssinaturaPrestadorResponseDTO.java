package psg.facilitei.DTO;

import java.time.Instant;

public record AssinaturaPrestadorResponseDTO(
        String status,
        boolean ativa,
        Instant ativaAte,
        String checkoutUrl,
        Long valorCentavos,
        boolean cobrancaHabilitada) {}
