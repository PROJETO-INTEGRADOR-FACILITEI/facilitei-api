package psg.facilitei.Util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class HtmlSanitizerTest {

    @Test
    void sanitize_comTagsHtml_escapaCaracteresPerigosos() {
        String resultado = HtmlSanitizer.sanitize("<script>alert(1)</script>");

        assertEquals("&lt;script&gt;alert(1)&lt;/script&gt;", resultado);
    }

    @Test
    void sanitize_comTextoSemHtml_permaneceInalterado() {
        assertEquals("João da Silva", HtmlSanitizer.sanitize("João da Silva"));
    }

    @Test
    void sanitize_comNulo_retornaNulo() {
        assertNull(HtmlSanitizer.sanitize(null));
    }
}
