package psg.facilitei.Config;

import java.time.LocalDate;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "legal")
public class LegalProperties {

    public enum OperatorType { INDIVIDUAL, COMPANY }
    public enum PaymentsMode { TEST, LIVE }

    private OperatorType operatorType = OperatorType.INDIVIDUAL;
    private String displayName = "Arthur Faria Estrela";
    private String legalName;
    private String taxId;
    private String businessAddress;
    private String contactEmail;
    private PaymentsMode paymentsMode = PaymentsMode.TEST;
    private Documents documents = new Documents();

    public boolean isProfileComplete() {
        return hasText(legalName) && hasText(taxId) && hasText(businessAddress) && hasText(contactEmail);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public OperatorType getOperatorType() { return operatorType; }
    public void setOperatorType(OperatorType operatorType) { this.operatorType = operatorType; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getLegalName() { return legalName; }
    public void setLegalName(String legalName) { this.legalName = legalName; }
    public String getTaxId() { return taxId; }
    public void setTaxId(String taxId) { this.taxId = taxId; }
    public String getBusinessAddress() { return businessAddress; }
    public void setBusinessAddress(String businessAddress) { this.businessAddress = businessAddress; }
    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }
    public PaymentsMode getPaymentsMode() { return paymentsMode; }
    public void setPaymentsMode(PaymentsMode paymentsMode) { this.paymentsMode = paymentsMode; }
    public Documents getDocuments() { return documents; }
    public void setDocuments(Documents documents) { this.documents = documents; }

    public static class Documents {
        private Document terms = new Document();
        private Document privacy = new Document();
        private Document subscription = new Document();

        public Document getTerms() { return terms; }
        public void setTerms(Document terms) { this.terms = terms; }
        public Document getPrivacy() { return privacy; }
        public void setPrivacy(Document privacy) { this.privacy = privacy; }
        public Document getSubscription() { return subscription; }
        public void setSubscription(Document subscription) { this.subscription = subscription; }
    }

    public static class Document {
        private String version = "1.0.0";
        private LocalDate effectiveDate = LocalDate.of(2026, 10, 30);
        private boolean requiresReaccept = true;

        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        public LocalDate getEffectiveDate() { return effectiveDate; }
        public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }
        public boolean isRequiresReaccept() { return requiresReaccept; }
        public void setRequiresReaccept(boolean requiresReaccept) { this.requiresReaccept = requiresReaccept; }
    }
}
