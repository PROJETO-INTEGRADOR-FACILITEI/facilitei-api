package psg.facilitei.Exceptions;


import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponseDTO {

    /** Um erro ligado a um campo do formulário (field = nome técnico, label = nome na tela). */
    public record FieldErrorDTO(String field, String label, String message) {}

    private Integer status;
    private String error;
    private String message;
    private LocalDateTime timestamp;
    private List<FieldErrorDTO> errors;

    public ErrorResponseDTO(Integer status, String error, String message, LocalDateTime timestamp) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.timestamp = timestamp;
    }

    public ErrorResponseDTO(Integer status, String error, String message, LocalDateTime timestamp,
                            List<FieldErrorDTO> errors) {
        this(status, error, message, timestamp);
        this.errors = errors == null || errors.isEmpty() ? null : errors;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public List<FieldErrorDTO> getErrors() {
        return errors;
    }

    public void setErrors(List<FieldErrorDTO> errors) {
        this.errors = errors;
    }
}
