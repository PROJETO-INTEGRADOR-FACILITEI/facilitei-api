package psg.facilitei.Services;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import psg.facilitei.DTO.EnderecoResponseDTO;
import psg.facilitei.DTO.ServicoResponseDTO;
import psg.facilitei.DTO.TrabalhadorRequestDTO;
import psg.facilitei.DTO.TrabalhadorPageResponseDTO;
import psg.facilitei.DTO.TrabalhadorResponseDTO;
import psg.facilitei.DTO.TrabalhadorUpdateDTO;
import psg.facilitei.DTO.ResumoAvaliacaoTipoServicoDTO;
import psg.facilitei.Entity.AvaliacaoTrabalhador;
import psg.facilitei.Entity.Endereco;
import psg.facilitei.Entity.Servico;
import psg.facilitei.Entity.Trabalhador;
import psg.facilitei.Entity.AssinaturaPrestador;
import psg.facilitei.Entity.Enum.TipoServico;
import psg.facilitei.Repository.AvaliacaoClienteRepository;
import psg.facilitei.Repository.AvaliacaoServicoRepository;
import psg.facilitei.Repository.AssinaturaPrestadorRepository;
import psg.facilitei.Exceptions.BusinessRuleException;
import psg.facilitei.Repository.AvaliacaoTrabalhadorRepository;
import psg.facilitei.Repository.ServicoRepository;
import psg.facilitei.Repository.TrabalhadorRepository;
import psg.facilitei.Util.HtmlSanitizer;

import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@Service
public class TrabalhadorService {

    @Autowired
    private TrabalhadorRepository repository;
    @Value("${mercadopago.monthly-amount-cents:0}")
    private long monthlyAmountCents;
    @Value("${mercadopago.access-token:}")
    private String mercadoPagoAccessToken;
    @Autowired
    private ServicoRepository servicoRepository;
    @Autowired
    private AvaliacaoTrabalhadorRepository avaliacaoTrabalhadorRepository;
    @Autowired
    private AvaliacaoClienteRepository avaliacaoClienteRepository;
    @Autowired
    private AvaliacaoServicoRepository avaliacaoServicoRepository;
    @Autowired
    private AssinaturaPrestadorRepository assinaturaPrestadorRepository;
    @Autowired
    private PasswordHashService passwordHashService;


    public TrabalhadorResponseDTO createTrabalhador(TrabalhadorRequestDTO trabalhadorRequestDTO) {
        Trabalhador trabalhador = repository.save(toEntity(trabalhadorRequestDTO));
        return toResponseDTO(trabalhador);
    }

    @Transactional(readOnly = true)
    public List<TrabalhadorResponseDTO> findAll() {
        List<Trabalhador> trabalhadores = repository.findAll(
                (root, query, cb) -> filtroAssinatura(root, query, cb));

        Map<Long, List<ResumoAvaliacaoTipoServicoDTO>> resumos = buscarResumos(trabalhadores);
        return trabalhadores.stream()
                .map(trabalhador -> toResponseDTO(trabalhador, resumos.getOrDefault(trabalhador.getId(), List.of())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TrabalhadorPageResponseDTO findAllPaginado(int page, int size, String nome,
            String localizacao, List<TipoServico> tiposServico, Double notaMinima) {
        Specification<Trabalhador> filtros = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(filtroAssinatura(root, query, cb));

            if (nome != null && !nome.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("nome")),
                        "%" + nome.trim().toLowerCase(Locale.ROOT) + "%"));
            }

            if (localizacao != null && !localizacao.isBlank()) {
                String termo = "%" + localizacao.trim().toLowerCase(Locale.ROOT) + "%";
                var endereco = root.join("endereco", JoinType.LEFT);
                predicates.add(cb.or(
                        cb.like(cb.lower(endereco.get("cidade")), termo),
                        cb.like(cb.lower(endereco.get("bairro")), termo),
                        cb.like(cb.lower(endereco.get("estado")), termo)));
            }

            if (tiposServico != null && !tiposServico.isEmpty()) {
                query.distinct(true);
                predicates.add(root.join("habilidades", JoinType.INNER).in(tiposServico));
            }

            if (notaMinima != null && notaMinima > 0) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("notaTrabalhador"), notaMinima));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Trabalhador> trabalhadores = repository.findAll(
                filtros, PageRequest.of(page, size, Sort.by("id").ascending()));
        Map<Long, List<ResumoAvaliacaoTipoServicoDTO>> resumos = buscarResumos(trabalhadores.getContent());
        Page<TrabalhadorResponseDTO> resultado = trabalhadores.map(trabalhador ->
                toResponseDTO(trabalhador, resumos.getOrDefault(trabalhador.getId(), List.of())));
        return TrabalhadorPageResponseDTO.from(resultado);
    }

    private Predicate filtroAssinatura(Root<Trabalhador> root, CriteriaQuery<?> query,
                                      CriteriaBuilder cb) {
        if (monthlyAmountCents <= 0 || mercadoPagoAccessToken == null || mercadoPagoAccessToken.isBlank()) {
            return cb.conjunction();
        }
        Subquery<Long> assinaturasAtivas = query.subquery(Long.class);
        Root<AssinaturaPrestador> assinatura = assinaturasAtivas.from(AssinaturaPrestador.class);
        assinaturasAtivas.select(assinatura.get("id")).where(
                cb.equal(assinatura.get("trabalhador").get("id"), root.get("id")),
                cb.equal(assinatura.get("status"), "ACTIVE"),
                cb.greaterThan(assinatura.get("activeUntil"), java.time.Instant.now()));
        return cb.exists(assinaturasAtivas);
    }

    public TrabalhadorResponseDTO atualizar(Long id, TrabalhadorUpdateDTO dto) {
        Trabalhador trabalhador = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Trabalhador não encontrado com ID: " + id));

        if (dto.getNome() != null) trabalhador.setNome(HtmlSanitizer.sanitize(dto.getNome()));
        if (dto.getEmail() != null) trabalhador.setEmail(dto.getEmail());
        if (dto.getDisponibilidade() != null) trabalhador.setDisponibilidade(dto.getDisponibilidade());
        if (dto.getSobre() != null) trabalhador.setSobre(HtmlSanitizer.sanitize(dto.getSobre()));
        if (dto.getTelefone() != null) trabalhador.setTelefone(dto.getTelefone());
        if (dto.getAvatarUrl() != null) trabalhador.setUrlFoto(dto.getAvatarUrl());
        if (dto.getHabilidades() != null) trabalhador.setHabilidades(dto.getHabilidades());
        if (dto.getServicoPrincipal() != null) trabalhador.setServicoPrincipal(dto.getServicoPrincipal());

        if (dto.getEndereco() != null) {
            Endereco endereco = trabalhador.getEndereco();
            endereco.setRua(dto.getEndereco().getRua());
            endereco.setNumero(dto.getEndereco().getNumero());
            endereco.setBairro(dto.getEndereco().getBairro());
            endereco.setCidade(dto.getEndereco().getCidade());
            endereco.setEstado(dto.getEndereco().getEstado());
            endereco.setCep(dto.getEndereco().getCep());
        }

        return toResponseDTO(repository.save(trabalhador));
    }

    @Transactional
    public void delete(Long id) {
        Trabalhador trabalhador = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Trabalhador não encontrado com ID: " + id));

        assinaturaPrestadorRepository.findByTrabalhadorId(id).ifPresent(assinatura -> {
            if (!"CANCELLED".equals(assinatura.getStatus())) {
                throw new BusinessRuleException("Cancele ou conclua a assinatura antes de excluir o profissional.");
            }
            assinaturaPrestadorRepository.delete(assinatura);
        });


        avaliacaoClienteRepository.deleteByTrabalhadorId(id);
        avaliacaoTrabalhadorRepository.deleteByTrabalhadorId(id);

        repository.delete(trabalhador);
    }

    public TrabalhadorResponseDTO findById(Long id) {
        Trabalhador trabalhador = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Trabalhador não encontrado com ID: " + id));

        return toResponseDTO(trabalhador);
    }

    public Trabalhador buscarEntidadePorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Trabalhador não encontrado com ID: " + id));
    }

    public Trabalhador toEntity(TrabalhadorRequestDTO dto) {
        Trabalhador trabalhador = new Trabalhador();

        trabalhador.setNome(HtmlSanitizer.sanitize(dto.getNome()));
        trabalhador.setEmail(dto.getEmail());
        trabalhador.setNotaTrabalhador(dto.getNotaTrabalhador());
        trabalhador.setSenha(passwordHashService.hash(dto.getSenha()));
        trabalhador.setDisponibilidade(dto.getDisponibilidade());
        trabalhador.setSobre(HtmlSanitizer.sanitize(dto.getSobre()));
        trabalhador.setTelefone(dto.getTelefone());
        // CORREÇÃO AQUI: Mapeando a URL da foto vinda do frontend
        trabalhador.setUrlFoto(dto.getAvatarUrl()); 
        
        trabalhador.setHabilidades(dto.getHabilidades());
        trabalhador.setServicoPrincipal(dto.getServicoPrincipal());

        Endereco endereco = new Endereco();
        endereco.setRua(dto.getEndereco().getRua());
        endereco.setBairro(dto.getEndereco().getBairro());
        endereco.setCidade(dto.getEndereco().getCidade());
        endereco.setEstado(dto.getEndereco().getEstado());
        endereco.setCep(dto.getEndereco().getCep());
        endereco.setNumero(dto.getEndereco().getNumero());
        trabalhador.setEndereco(endereco);

        if (dto.getHabilidades() != null) {
            trabalhador.setHabilidades(dto.getHabilidades());
        }
        
        if (dto.getServicoPrincipal() != null) {
            trabalhador.setServicoPrincipal(dto.getServicoPrincipal());
        }
        if (dto.getAvaliacoesIds() != null) {
            List<AvaliacaoTrabalhador> avaliacoes = avaliacaoTrabalhadorRepository.findAllById(dto.getAvaliacoesIds());
            trabalhador.setAvaliacoesTrabalhador(avaliacoes);
        }

        return trabalhador;
    }

    private Map<Long, List<ResumoAvaliacaoTipoServicoDTO>> buscarResumos(List<Trabalhador> trabalhadores) {
        Map<Long, List<ResumoAvaliacaoTipoServicoDTO>> resumos = new HashMap<>();
        if (trabalhadores.isEmpty()) return resumos;

        List<Long> ids = trabalhadores.stream().map(Trabalhador::getId).toList();
        for (Object[] linha : avaliacaoServicoRepository.resumirPorTrabalhadores(ids)) {
            Long trabalhadorId = (Long) linha[0];
            resumos.computeIfAbsent(trabalhadorId, ignored -> new ArrayList<>()).add(
                    new ResumoAvaliacaoTipoServicoDTO((TipoServico) linha[1],
                            ((Number) linha[2]).doubleValue(), ((Number) linha[3]).longValue()));
        }
        return resumos;
    }

    public TrabalhadorResponseDTO toResponseDTO(Trabalhador entity) {
        return toResponseDTO(entity, buscarResumos(List.of(entity)).getOrDefault(entity.getId(), List.of()));
    }

    private TrabalhadorResponseDTO toResponseDTO(Trabalhador entity,
            List<ResumoAvaliacaoTipoServicoDTO> resumos) {
        TrabalhadorResponseDTO dto = new TrabalhadorResponseDTO();

        dto.setId(String.valueOf(entity.getId()));
        dto.setNome(entity.getNome());
        dto.setEmail(entity.getEmail());
        dto.setTelefone(entity.getTelefone());
        dto.setDisponibilidade(entity.getDisponibilidade());
        dto.setNotaTrabalhador(entity.getNotaTrabalhador());
        dto.setAvaliacoesPorServico(resumos);
        dto.setSobre(entity.getSobre());
        dto.setServicoPrincipal(entity.getServicoPrincipal());
        dto.setAvatarUrl(entity.getUrlFoto());

        if (entity.getHabilidades() != null) {
            dto.setServicos(entity.getHabilidades());
        }

        Endereco endereco = entity.getEndereco();
        if (endereco != null) {
            EnderecoResponseDTO enderecoDTO = new EnderecoResponseDTO();
            enderecoDTO.setRua(endereco.getRua());
            enderecoDTO.setCidade(endereco.getCidade());
            enderecoDTO.setEstado(endereco.getEstado());
            enderecoDTO.setCep(endereco.getCep());
            enderecoDTO.setBairro(endereco.getBairro());
            enderecoDTO.setNumero(endereco.getNumero());
            dto.setEndereco(enderecoDTO);
        }

        return dto;
    }

}
