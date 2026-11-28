package psg.facilitei.Util;

/**
 * Escapa HTML do texto digitado pelo usuário antes de salvar, para evitar XSS
 * se algum lugar mostrar o valor sem escapar.
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
