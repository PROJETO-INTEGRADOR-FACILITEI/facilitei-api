package psg.facilitei.Exceptions;

import java.util.Map;

/**
 * Traduz nomes técnicos de campos (ex.: "endereco.cep", "servico_principal")
 * para o nome que o usuário vê na tela (ex.: "CEP", "Serviço principal").
 */
public final class FieldLabels {

    private static final Map<String, String> LABELS = Map.ofEntries(
            Map.entry("nome", "Nome"),
            Map.entry("email", "E-mail"),
            Map.entry("telefone", "Telefone"),
            Map.entry("senha", "Senha"),
            Map.entry("novaSenha", "Nova senha"),
            Map.entry("senhaAtual", "Senha atual"),
            Map.entry("cpf", "CPF"),
            Map.entry("endereco", "Endereço"),
            Map.entry("cep", "CEP"),
            Map.entry("rua", "Rua"),
            Map.entry("numero", "Número"),
            Map.entry("bairro", "Bairro"),
            Map.entry("cidade", "Cidade"),
            Map.entry("estado", "UF"),
            Map.entry("complemento", "Complemento"),
            Map.entry("descricao", "Descrição"),
            Map.entry("titulo", "Nome do serviço"),
            Map.entry("preco", "Preço"),
            Map.entry("valor", "Valor"),
            Map.entry("tipoServico", "Tipo de serviço"),
            Map.entry("servicoPrincipal", "Serviço principal"),
            Map.entry("habilidades", "Habilidades"),
            Map.entry("disponibilidade", "Disponibilidade"),
            Map.entry("sobre", "Sobre você"),
            Map.entry("avatarUrl", "Foto"),
            Map.entry("nota", "Nota"),
            Map.entry("comentario", "Comentário"),
            Map.entry("motivo", "Motivo"),
            Map.entry("reason", "Motivo"),
            Map.entry("conteudo", "Mensagem"),
            Map.entry("mensagem", "Mensagem"),
            Map.entry("token", "Código"),
            Map.entry("legalAcceptance", "Confirmações legais"),
            Map.entry("documents", "Documentos"),
            Map.entry("trabalhadorId", "Profissional"),
            Map.entry("clienteId", "Cliente"),
            Map.entry("servicoId", "Serviço"),
            Map.entry("servicoOferecidoId", "Serviço"),
            Map.entry("durationDays", "Duração"),
            Map.entry("dataServico", "Data do serviço"),
            Map.entry("file", "Arquivo"));

    private FieldLabels() {}

    public static String of(String path) {
        if (path == null || path.isBlank()) return "Campo";
        String last = path.replaceAll("\\[\\d*]", "");
        last = last.substring(last.lastIndexOf('.') + 1);
        last = snakeToCamel(last);
        String label = LABELS.get(last);
        if (label != null) return label;
        String human = last.replaceAll("([a-z])([A-Z])", "$1 $2").toLowerCase();
        if (human.isEmpty()) return "Campo";
        return Character.toUpperCase(human.charAt(0)) + human.substring(1);
    }

    static String snakeToCamel(String value) {
        if (!value.contains("_")) return value;
        StringBuilder out = new StringBuilder();
        boolean upper = false;
        for (char c : value.toLowerCase().toCharArray()) {
            if (c == '_') {
                upper = true;
            } else {
                out.append(upper ? Character.toUpperCase(c) : c);
                upper = false;
            }
        }
        return out.toString();
    }
}
