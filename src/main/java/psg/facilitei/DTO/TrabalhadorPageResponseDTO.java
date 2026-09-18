package psg.facilitei.DTO;

import java.util.List;

import org.springframework.data.domain.Page;

public record TrabalhadorPageResponseDTO(
        List<TrabalhadorResponseDTO> content,
        long totalElements,
        int totalPages,
        int page,
        int size,
        boolean first,
        boolean last) {

    public static TrabalhadorPageResponseDTO from(Page<TrabalhadorResponseDTO> result) {
        return new TrabalhadorPageResponseDTO(
                result.getContent(), result.getTotalElements(), result.getTotalPages(),
                result.getNumber(), result.getSize(), result.isFirst(), result.isLast());
    }
}
