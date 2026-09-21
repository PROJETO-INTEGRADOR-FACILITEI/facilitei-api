package psg.facilitei.Security;

public record AuthenticatedPrincipal(Long id, String role, String name) {
    public boolean isCliente() {
        return "cliente".equals(role);
    }

    public boolean isTrabalhador() {
        return "trabalhador".equals(role);
    }
}
