package psg.facilitei.Domain;

import java.util.Arrays;
import java.util.Optional;

public enum LegalDocumentType {
    TERMS("terms", "Termos de Uso", "legal/terms-v1.md"),
    PRIVACY("privacy", "Política de Privacidade", "legal/privacy-v1.md"),
    SUBSCRIPTION("subscription", "Política de Assinatura, Cancelamento e Reembolso",
            "legal/subscription-v1.md");

    private final String slug;
    private final String title;
    private final String resourcePath;

    LegalDocumentType(String slug, String title, String resourcePath) {
        this.slug = slug;
        this.title = title;
        this.resourcePath = resourcePath;
    }

    public String slug() { return slug; }
    public String title() { return title; }
    public String resourcePath() { return resourcePath; }

    public static Optional<LegalDocumentType> fromSlug(String slug) {
        return Arrays.stream(values())
                .filter(type -> type.slug.equalsIgnoreCase(slug))
                .findFirst();
    }
}
