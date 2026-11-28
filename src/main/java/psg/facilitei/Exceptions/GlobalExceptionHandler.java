package psg.facilitei.Exceptions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Toda resposta de erro sai como { status, error, message, timestamp, errors? }.
 * "message" já vem em português para mostrar na tela e "errors" lista os campos com problema.
 * Conta suspensa é a exceção: usa o corpo de AccountModerationService.suspensionBody.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ---------------------------------------------------------------- helpers

    private static ResponseEntity<ErrorResponseDTO> respond(HttpStatus status, String message) {
        return respond(status, message, null);
    }

    private static ResponseEntity<ErrorResponseDTO> respond(HttpStatus status, String message,
                                                            List<ErrorResponseDTO.FieldErrorDTO> errors) {
        return ResponseEntity.status(status).body(new ErrorResponseDTO(
                status.value(), status.getReasonPhrase(), message, LocalDateTime.now(), errors));
    }

    private static ResponseEntity<ErrorResponseDTO> validationResponse(List<ErrorResponseDTO.FieldErrorDTO> errors) {
        String message;
        if (errors.isEmpty()) {
            message = "Alguns dados estão inválidos. Confira o formulário.";
        } else if (errors.size() == 1) {
            message = errors.get(0).message();
        } else {
            message = "Corrija os campos: " + String.join(" ",
                    errors.stream().map(ErrorResponseDTO.FieldErrorDTO::message).toList());
        }
        return respond(HttpStatus.BAD_REQUEST, message, errors);
    }

    private static String defaultReason(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "Alguns dados estão inválidos. Confira e tente novamente.";
            case UNAUTHORIZED -> "Sua sessão expirou. Entre novamente para continuar.";
            case FORBIDDEN -> "Você não tem permissão para fazer isso.";
            case NOT_FOUND -> "Não encontramos o que você procurou.";
            case CONFLICT -> "Essa ação entra em conflito com dados já existentes.";
            case PAYLOAD_TOO_LARGE -> "Arquivo muito grande.";
            case TOO_MANY_REQUESTS -> "Muitas tentativas seguidas. Aguarde um pouco e tente de novo.";
            default -> status.is5xxServerError()
                    ? "Ocorreu um erro no servidor. Tente novamente em instantes."
                    : "Não foi possível concluir a ação.";
        };
    }

    /** Usa a mensagem escrita no DTO; se for a padrão do Bean Validation, gera uma em português. */
    static String friendlyMessage(String code, String label, String defaultMessage, String template,
                                  Map<String, Object> attributes) {
        boolean customMessage = template != null && !template.startsWith("{jakarta.validation")
                && !template.startsWith("{org.hibernate.validator");
        if (customMessage && defaultMessage != null && !defaultMessage.isBlank()) {
            return withPeriod(defaultMessage);
        }
        if (code == null) return label + " é inválido.";
        return switch (code) {
            case "NotBlank", "NotNull", "NotEmpty" -> "Preencha o campo " + label + ".";
            case "Email" -> "Informe um e-mail válido.";
            case "Size", "Length" -> sizeMessage(label, attributes);
            case "Min", "DecimalMin" -> label + " deve ser no mínimo " + attr(attributes, "value") + ".";
            case "Max", "DecimalMax" -> label + " deve ser no máximo " + attr(attributes, "value") + ".";
            case "Positive" -> label + " deve ser maior que zero.";
            case "PositiveOrZero" -> label + " não pode ser negativo.";
            case "Pattern" -> label + " está em um formato inválido.";
            case "Past", "PastOrPresent" -> label + " deve ser uma data no passado.";
            case "Future", "FutureOrPresent" -> label + " deve ser uma data futura.";
            case "AssertTrue" -> "Confirme o campo " + label + ".";
            default -> label + " é inválido.";
        };
    }

    private static String sizeMessage(String label, Map<String, Object> attributes) {
        Object min = attributes == null ? null : attributes.get("min");
        Object max = attributes == null ? null : attributes.get("max");
        int minValue = min instanceof Number n ? n.intValue() : 0;
        int maxValue = max instanceof Number n ? n.intValue() : Integer.MAX_VALUE;
        if (minValue > 0 && maxValue < Integer.MAX_VALUE) {
            return label + " deve ter entre " + minValue + " e " + maxValue + " caracteres.";
        }
        if (maxValue < Integer.MAX_VALUE) return label + " deve ter no máximo " + maxValue + " caracteres.";
        if (minValue > 0) return label + " deve ter pelo menos " + minValue + " caracteres.";
        return label + " tem um tamanho inválido.";
    }

    private static String attr(Map<String, Object> attributes, String key) {
        Object value = attributes == null ? null : attributes.get(key);
        return value == null ? "?" : String.valueOf(value);
    }

    private static String withPeriod(String message) {
        String trimmed = message.trim();
        return trimmed.matches(".*[.!?]$") ? trimmed : trimmed + ".";
    }

    private static ErrorResponseDTO.FieldErrorDTO toFieldError(FieldError error) {
        String template = null;
        Map<String, Object> attributes = null;
        try {
            ConstraintViolation<?> violation = error.unwrap(ConstraintViolation.class);
            template = violation.getMessageTemplate();
            attributes = violation.getConstraintDescriptor().getAttributes();
        } catch (IllegalArgumentException ignored) {
            // erro de binding (tipo errado), não de Bean Validation
        }
        String label = FieldLabels.of(error.getField());
        String message = "typeMismatch".equals(error.getCode())
                ? "O valor informado em " + label + " é inválido."
                : friendlyMessage(error.getCode(), label, error.getDefaultMessage(), template, attributes);
        return new ErrorResponseDTO.FieldErrorDTO(error.getField(), label, message);
    }

    private static String jsonPath(JsonMappingException ex) {
        StringBuilder path = new StringBuilder();
        for (JsonMappingException.Reference ref : ex.getPath()) {
            if (ref.getFieldName() != null) {
                if (!path.isEmpty()) path.append('.');
                path.append(ref.getFieldName());
            } else if (ref.getIndex() >= 0) {
                path.append('[').append(ref.getIndex()).append(']');
            }
        }
        return path.toString();
    }

    // ---------------------------------------------------------------- handlers

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponseDTO> handleResponseStatus(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;
        String reason = ex.getReason();
        return respond(status, reason == null || reason.isBlank() ? defaultReason(status) : reason);
    }

    @ExceptionHandler(AccountSuspendedException.class)
    public ResponseEntity<Map<String, Object>> handleAccountSuspended(AccountSuspendedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(psg.facilitei.Services.AccountModerationService.suspensionBody(ex));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleNotFound(ResourceNotFoundException ex) {
        return respond(HttpStatus.NOT_FOUND, ex.getMessage() == null ? defaultReason(HttpStatus.NOT_FOUND) : ex.getMessage());
    }

    @ExceptionHandler(jakarta.persistence.EntityNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleEntityNotFound(jakarta.persistence.EntityNotFoundException ex) {
        String message = ex.getMessage() == null ? "" : ex.getMessage();
        // Não expõe IDs internos: "Trabalhador não encontrado com ID: 9" -> "Trabalhador não encontrado."
        int idx = message.indexOf(" com ID");
        if (idx > 0) message = message.substring(0, idx) + ".";
        return respond(HttpStatus.NOT_FOUND, message.isBlank() ? defaultReason(HttpStatus.NOT_FOUND) : message);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidationErrors(MethodArgumentNotValidException ex) {
        List<ErrorResponseDTO.FieldErrorDTO> errors = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            if (seen.add(fieldError.getField())) errors.add(toFieldError(fieldError));
        }
        for (ObjectError globalError : ex.getBindingResult().getGlobalErrors()) {
            String message = globalError.getDefaultMessage();
            if (message != null && !message.isBlank()) {
                errors.add(new ErrorResponseDTO.FieldErrorDTO(null, null, withPeriod(message)));
            }
        }
        return validationResponse(errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponseDTO> handleConstraintViolation(ConstraintViolationException ex) {
        List<ErrorResponseDTO.FieldErrorDTO> errors = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String path = String.valueOf(violation.getPropertyPath());
            if (!seen.add(path)) continue;
            String label = FieldLabels.of(path);
            String code = violation.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName();
            errors.add(new ErrorResponseDTO.FieldErrorDTO(path, label, friendlyMessage(code, label,
                    violation.getMessage(), violation.getMessageTemplate(),
                    violation.getConstraintDescriptor().getAttributes())));
        }
        return validationResponse(errors);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponseDTO> handleBusinessRuleException(BusinessRuleException ex) {
        return respond(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDTO> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        Throwable cause = ex.getCause();
        if (cause instanceof JsonMappingException mapping && !mapping.getPath().isEmpty()) {
            String path = jsonPath(mapping);
            String label = FieldLabels.of(path);
            String message;
            if (cause instanceof UnrecognizedPropertyException) {
                message = "O campo " + label + " não é aceito aqui.";
            } else if (cause instanceof InvalidFormatException invalid) {
                message = invalidFormatMessage(label, invalid);
            } else if (cause instanceof MismatchedInputException mismatched) {
                message = mismatchedMessage(label, mismatched.getTargetType());
            } else {
                message = "O valor informado em " + label + " é inválido.";
            }
            return validationResponse(List.of(new ErrorResponseDTO.FieldErrorDTO(path, label, message)));
        }
        if (cause instanceof JsonProcessingException) {
            return respond(HttpStatus.BAD_REQUEST,
                    "Os dados enviados estão em um formato inválido. Atualize a página e tente de novo.");
        }
        return respond(HttpStatus.BAD_REQUEST, "Nenhum dado foi enviado. Preencha o formulário e tente de novo.");
    }

    private static String invalidFormatMessage(String label, InvalidFormatException ex) {
        Class<?> target = ex.getTargetType();
        Object value = ex.getValue();
        boolean empty = value == null || String.valueOf(value).isBlank();
        if (target != null && target.isEnum()) {
            return empty ? "Selecione uma opção em " + label + "." : "Selecione uma opção válida em " + label + ".";
        }
        return mismatchedMessage(label, target);
    }

    private static String mismatchedMessage(String label, Class<?> target) {
        if (target == null) return "O valor informado em " + label + " é inválido.";
        if (target.isEnum()) return "Selecione uma opção válida em " + label + ".";
        if (Number.class.isAssignableFrom(target) || target == int.class || target == long.class
                || target == double.class || target == float.class || target == BigDecimal.class
                || target == BigInteger.class) {
            return label + " deve ser um número.";
        }
        if (Temporal.class.isAssignableFrom(target) || Date.class.isAssignableFrom(target)) {
            return label + " tem uma data inválida.";
        }
        if (target == Boolean.class || target == boolean.class) {
            return label + " deve ser sim ou não.";
        }
        if (java.util.Collection.class.isAssignableFrom(target) || target.isArray()) {
            return label + " deve ser uma lista.";
        }
        return "O valor informado em " + label + " é inválido.";
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseDTO> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String label = FieldLabels.of(ex.getName());
        return validationResponse(List.of(new ErrorResponseDTO.FieldErrorDTO(ex.getName(), label,
                "O valor informado em " + label + " é inválido.")));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponseDTO> handleMissingParameter(MissingServletRequestParameterException ex) {
        String label = FieldLabels.of(ex.getParameterName());
        return validationResponse(List.of(new ErrorResponseDTO.FieldErrorDTO(ex.getParameterName(), label,
                "Preencha o campo " + label + ".")));
    }

    @ExceptionHandler(NumberFormatException.class)
    public ResponseEntity<ErrorResponseDTO> handleNumberFormat(NumberFormatException ex) {
        return respond(HttpStatus.BAD_REQUEST, "Um dos valores numéricos informados é inválido.");
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErrorResponseDTO> handleMissingPart(MissingServletRequestPartException ex) {
        String label = FieldLabels.of(ex.getRequestPartName());
        return validationResponse(List.of(new ErrorResponseDTO.FieldErrorDTO(ex.getRequestPartName(), label,
                "Envie o " + label.toLowerCase(Locale.ROOT) + ".")));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponseDTO> handleMaxUpload(MaxUploadSizeExceededException ex) {
        return respond(HttpStatus.PAYLOAD_TOO_LARGE, "Arquivo muito grande. Envie um arquivo de até 10 MB.");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponseDTO> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return respond(HttpStatus.METHOD_NOT_ALLOWED, "Essa operação não é permitida neste endereço.");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponseDTO> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        return respond(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Formato de envio não suportado. Atualize a página e tente de novo.");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleNoResource(NoResourceFoundException ex) {
        return respond(HttpStatus.NOT_FOUND, "Endereço não encontrado.");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDTO> handleAccessDenied(AccessDeniedException ex) {
        return respond(HttpStatus.FORBIDDEN, defaultReason(HttpStatus.FORBIDDEN));
    }

    private static final Pattern DATA_TOO_LONG = Pattern.compile("(?i)data too long for column '([^']+)'");
    private static final Pattern NOT_NULL = Pattern.compile("(?i)column '([^']+)' cannot be null");
    private static final Pattern H2_NOT_NULL = Pattern.compile("(?i)NULL not allowed for column \"([^\"]+)\"");
    private static final Pattern H2_TOO_LONG = Pattern.compile("(?i)value too long for column \"([^\" ]+)");

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseDTO> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        String detail = String.valueOf(ex.getMostSpecificCause().getMessage());
        String lower = detail.toLowerCase(Locale.ROOT);

        if (lower.contains("duplicate entry") || lower.contains("unique index") || lower.contains("unique constraint")) {
            if (lower.contains("email")) {
                return validationResponse(List.of(new ErrorResponseDTO.FieldErrorDTO("email", "E-mail",
                        "Este e-mail já está cadastrado. Faça login ou use outro e-mail.")));
            }
            return respond(HttpStatus.CONFLICT, "Esse registro já existe.");
        }
        Matcher tooLong = DATA_TOO_LONG.matcher(detail);
        if (!tooLong.find()) tooLong = H2_TOO_LONG.matcher(detail);
        if (tooLong.reset().find()) {
            String column = tooLong.group(1);
            String label = FieldLabels.of(column);
            return validationResponse(List.of(new ErrorResponseDTO.FieldErrorDTO(FieldLabels.snakeToCamel(column),
                    label, label + " passou do tamanho permitido. Encurte o texto.")));
        }
        Matcher notNull = NOT_NULL.matcher(detail);
        if (!notNull.find()) notNull = H2_NOT_NULL.matcher(detail);
        if (notNull.reset().find()) {
            String column = notNull.group(1);
            String label = FieldLabels.of(column);
            return validationResponse(List.of(new ErrorResponseDTO.FieldErrorDTO(FieldLabels.snakeToCamel(column),
                    label, "Preencha o campo " + label + ".")));
        }
        if (lower.contains("foreign key")) {
            return respond(HttpStatus.CONFLICT,
                    "Não foi possível concluir: este item está ligado a outros registros.");
        }
        log.warn("Violação de integridade não mapeada: {}", detail);
        return respond(HttpStatus.CONFLICT,
                "Não foi possível salvar: algum dado está repetido, faltando ou grande demais.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGeneralError(Exception ex) {
        log.error("Erro inesperado", ex);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, defaultReason(HttpStatus.INTERNAL_SERVER_ERROR));
    }
}
