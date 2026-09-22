package psg.facilitei.DTO;

import lombok.Data;

@Data
public class LoginResponseDTO {
    private String role;
    private Object user; // Pode ser ClienteResponseDTO ou TrabalhadorResponseDTO
    private boolean admin;

    public LoginResponseDTO(String role, Object user) {
        this(role, user, false);
    }

    public LoginResponseDTO(String role, Object user, boolean admin) {
        this.role = role;
        this.user = user;
        this.admin = admin;
    }
}
