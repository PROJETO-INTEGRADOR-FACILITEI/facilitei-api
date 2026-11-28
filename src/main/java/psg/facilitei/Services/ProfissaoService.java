package psg.facilitei.Services;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import psg.facilitei.Entity.Profissao;
import psg.facilitei.Exceptions.BusinessRuleException;
import psg.facilitei.Repository.ProfissaoRepository;

/**
 * Lista de profissões salva no banco. Quem não se encaixa em nenhuma cria a sua
 * no cadastro e ela passa a valer para todos.
 */
@Service
public class ProfissaoService implements ApplicationRunner {

    public static final int MAX_HABILIDADES = 30;
    public static final int MAX_NOVAS_POR_VEZ = 5;

    /** Profissões da plataforma. Os códigos são os mesmos já gravados nas outras tabelas. */
    public static final Map<String, String> PADRAO = new LinkedHashMap<>();

    static {
        PADRAO.put("PEDREIRO", "Pedreiro");
        PADRAO.put("ELETRICISTA", "Eletricista");
        PADRAO.put("ENCANADOR", "Encanador");
        PADRAO.put("PINTOR", "Pintor");
        PADRAO.put("GESSEIRO", "Gesseiro");
        PADRAO.put("AZULEJISTA", "Azulejista");
        PADRAO.put("MARCENEIRO", "Marceneiro");
        PADRAO.put("SERRALHEIRO", "Serralheiro");
        PADRAO.put("DIARISTA", "Diarista");
        PADRAO.put("FAXINEIRA", "Faxineira");
        PADRAO.put("PASSADEIRA", "Passadeira");
        PADRAO.put("COZINHEIRA", "Cozinheira");
        PADRAO.put("CUIDADOR_IDOSOS", "Cuidador de idosos");
        PADRAO.put("BABA", "Babá");
        PADRAO.put("TECNICO_INFORMATICA", "Técnico de informática");
        PADRAO.put("TECNICO_ELETRODOMESTICOS", "Técnico de eletrodomésticos");
        PADRAO.put("INSTALADOR_AR_CONDICIONADO", "Instalador de ar-condicionado");
        PADRAO.put("TECNICO_REDES", "Técnico de redes");
        PADRAO.put("INSTALADOR_CAMERAS", "Instalador de câmeras");
        PADRAO.put("JARDINEIRO", "Jardineiro");
        PADRAO.put("PAISAGISTA", "Paisagista");
        PADRAO.put("DEDETIZADOR", "Dedetizador");
        PADRAO.put("LIMPADOR_PISCINA", "Limpador de piscina");
        PADRAO.put("REFORCO_ESCOLAR", "Reforço escolar");
        PADRAO.put("AULAS_INGLES", "Aulas de inglês");
        PADRAO.put("AULAS_MUSICA", "Aulas de música");
        PADRAO.put("AULAS_INFORMATICA", "Aulas de informática");
        PADRAO.put("PERSONAL_TRAINER", "Personal trainer");
    }

    private final ProfissaoRepository repository;

    public ProfissaoService(ProfissaoRepository repository) {
        this.repository = repository;
    }

    /** Garante que as profissões da plataforma existem (banco novo, testes, etc.). */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int ordem = 0;
        for (Map.Entry<String, String> entry : PADRAO.entrySet()) {
            ordem++;
            if (!repository.existsById(entry.getKey())) {
                repository.save(new Profissao(entry.getKey(), entry.getValue(), false, ordem, null));
            }
        }
    }

    @Transactional(readOnly = true)
    public List<Profissao> listar() {
        return repository.findAllByOrderByPersonalizadaAscOrdemAscNomeAsc();
    }

    /** "Montador de Móveis" -> "MONTADOR_DE_MOVEIS". */
    public static String codigoDe(String nome) {
        String semAcento = Normalizer.normalize(nome, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String codigo = semAcento.toUpperCase().replaceAll("[^A-Z0-9]+", "_").replaceAll("^_+|_+$", "");
        return codigo.length() > 60 ? codigo.substring(0, 60).replaceAll("_+$", "") : codigo;
    }

    private static String nomeFormatado(String entrada) {
        String nome = entrada.trim().replaceAll("\\s+", " ");
        return Character.toUpperCase(nome.charAt(0)) + nome.substring(1);
    }

    private static void validarNomeNovo(String nome) {
        if (nome.length() < 3 || nome.length() > 60) {
            throw new BusinessRuleException("O nome da profissão deve ter entre 3 e 60 caracteres.");
        }
        if (!nome.matches("[\\p{L}0-9 '()/&.-]+") || !nome.matches(".*\\p{L}.*")) {
            throw new BusinessRuleException("Use só letras, números e espaços no nome da profissão \"" + nome + "\".");
        }
    }

    /**
     * Recebe códigos (ex.: "PEDREIRO") ou nomes digitados (ex.: "Montador de móveis") e devolve
     * os códigos. O que não existir vira profissão nova, até MAX_NOVAS_POR_VEZ por chamada.
     */
    @Transactional
    public List<String> resolver(List<String> entradas, Long criadorId) {
        if (entradas == null) return null;
        Set<String> codigos = new LinkedHashSet<>();
        int novas = 0;
        for (String entrada : entradas) {
            if (entrada == null || entrada.isBlank()) continue;
            String texto = entrada.trim();
            Optional<Profissao> existente = repository.findById(texto);
            if (existente.isEmpty()) {
                String codigo = codigoDe(texto);
                if (codigo.isEmpty()) {
                    throw new BusinessRuleException("Use só letras, números e espaços no nome da profissão \"" + texto + "\".");
                }
                // Pode já existir escrito de outro jeito: procura pelo código gerado e pelo nome
                existente = repository.findById(codigo).or(() -> repository.findFirstByNomeIgnoreCase(nomeFormatado(texto)));
                if (existente.isEmpty()) {
                    String nome = nomeFormatado(texto);
                    validarNomeNovo(nome);
                    if (++novas > MAX_NOVAS_POR_VEZ) {
                        throw new BusinessRuleException("Você pode criar até " + MAX_NOVAS_POR_VEZ + " profissões novas por vez.");
                    }
                    existente = Optional.of(repository.save(new Profissao(codigo, nome, true, 1000, criadorId)));
                }
            }
            codigos.add(existente.get().getCodigo());
        }
        if (codigos.size() > MAX_HABILIDADES) {
            throw new BusinessRuleException("Escolha no máximo " + MAX_HABILIDADES + " profissões.");
        }
        return new ArrayList<>(codigos);
    }

    @Transactional
    public String resolverUm(String entrada, Long criadorId) {
        if (entrada == null || entrada.isBlank()) return null;
        return resolver(List.of(entrada), criadorId).get(0);
    }

    /** Para pedidos, serviços e portfólio: aceita só profissões que já existem. */
    @Transactional(readOnly = true)
    public String exigir(String codigo) {
        if (codigo == null || codigo.isBlank()) return null;
        String valor = codigo.trim();
        if (!repository.existsById(valor)) {
            throw new BusinessRuleException("Profissão não encontrada. Escolha uma opção da lista.");
        }
        return valor;
    }

    /** Nome para mostrar ao usuário ("MONTADOR_DE_MOVEIS" -> "Montador de móveis"). */
    @Transactional(readOnly = true)
    public String nome(String codigo) {
        if (codigo == null) return "";
        return repository.findById(codigo).map(Profissao::getNome).orElseGet(() -> humanizar(codigo));
    }

    public static String humanizar(String codigo) {
        if (codigo == null || codigo.isBlank()) return "";
        String texto = codigo.toLowerCase().replace('_', ' ');
        return Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }
}
