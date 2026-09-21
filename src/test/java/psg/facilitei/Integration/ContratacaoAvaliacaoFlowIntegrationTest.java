package psg.facilitei.Integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import psg.facilitei.DTO.AvaliacaoServicoRequestDTO;
import psg.facilitei.DTO.ServicoRequestDTO;
import psg.facilitei.Entity.Cliente;
import psg.facilitei.Entity.Enum.TipoServico;
import psg.facilitei.Entity.Trabalhador;
import psg.facilitei.Repository.ClienteRepository;
import psg.facilitei.Repository.TrabalhadorRepository;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ContratacaoAvaliacaoFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private TrabalhadorRepository trabalhadorRepository;

    @Test
    void fluxoCompleto_criarServicoEAvaliar() throws Exception {
        Cliente cliente = new Cliente();
        cliente.setNome("Cliente Teste");
        cliente.setEmail("cliente.integ@teste.com");
        cliente.setSenha("123456");
        cliente = clienteRepository.save(cliente);

        Trabalhador trabalhador = new Trabalhador();
        trabalhador.setNome("Trabalhador Teste");
        trabalhador.setEmail("trabalhador.integ@teste.com");
        trabalhador.setSenha("123456");
        trabalhador = trabalhadorRepository.save(trabalhador);

        ServicoRequestDTO servicoDto = new ServicoRequestDTO();
        servicoDto.setTitulo("Instalação elétrica");
        servicoDto.setDescricao("Instalar tomadas na cozinha");
        servicoDto.setTipoServico(TipoServico.ELETRICISTA);
        servicoDto.setTrabalhadorId(trabalhador.getId());
        servicoDto.setClienteId(cliente.getId());

        String servicoResponse = mockMvc.perform(post("/api/servicos")
                        .with(csrf())
                        .sessionAttr("auth.role", "trabalhador")
                        .sessionAttr("auth.userId", trabalhador.getId())
                        .sessionAttr("auth.name", trabalhador.getNome())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(servicoDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.titulo", is("Instalação elétrica")))
                .andReturn().getResponse().getContentAsString();

        Long servicoId = objectMapper.readTree(servicoResponse).get("id").asLong();

        AvaliacaoServicoRequestDTO avaliacaoDto = new AvaliacaoServicoRequestDTO();
        avaliacaoDto.setClienteId(cliente.getId());
        avaliacaoDto.setServicoId(servicoId);
        avaliacaoDto.setNota(5);
        avaliacaoDto.setComentario("Serviço excelente, super recomendo");

        mockMvc.perform(post("/api/avaliacoes-servico/Criar")
                        .with(csrf())
                        .sessionAttr("auth.role", "cliente")
                        .sessionAttr("auth.userId", cliente.getId())
                        .sessionAttr("auth.name", cliente.getNome())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(avaliacaoDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nota", is(5)));

        mockMvc.perform(get("/api/avaliacoes-servico/" + servicoId)
                        .sessionAttr("auth.role", "cliente")
                        .sessionAttr("auth.userId", cliente.getId())
                        .sessionAttr("auth.name", cliente.getNome()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].comentario", is("Serviço excelente, super recomendo")))
                .andExpect(jsonPath("$[0].tipoServico", is("ELETRICISTA")));

        mockMvc.perform(get("/api/avaliacoes-servico/trabalhador/" + trabalhador.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipoServico", is("ELETRICISTA")))
                .andExpect(jsonPath("$[0].clienteNome", is("Cliente Teste")));

        mockMvc.perform(get("/api/trabalhadores/buscarPorId/" + trabalhador.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notaTrabalhador", is(5.0)));
    }
}
