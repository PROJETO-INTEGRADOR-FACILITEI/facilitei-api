package psg.facilitei.Util;

/**
 * Escapa caracteres HTML em texto livre fornecido pelo usuário antes de persistir,
 * como defesa em profundidade contra XSS armazenado caso algum consumidor da API
 * renderize o valor em HTML sem escapar por conta própria.
 */
public final class HtmlSanitizer {

    private HtmlSanitizer() {
    }

    public static String sanitize(String input) {
        if (input == null) {
            return null;
        }
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }
}
