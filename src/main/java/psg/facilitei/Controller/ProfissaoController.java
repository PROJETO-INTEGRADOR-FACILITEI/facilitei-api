package psg.facilitei.Controller;

import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import psg.facilitei.Services.ProfissaoService;

@RestController
@RequestMapping("/api/profissoes")
public class ProfissaoController {

    public record ProfissaoResponse(String codigo, String nome, boolean personalizada) {}

    private final ProfissaoService service;

    public ProfissaoController(ProfissaoService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista de profissões (as da plataforma e as criadas pelos profissionais)")
    public List<ProfissaoResponse> listar() {
        return service.listar().stream()
                .map(p -> new ProfissaoResponse(p.getCodigo(), p.getNome(), p.isPersonalizada()))
                .toList();
    }
}
