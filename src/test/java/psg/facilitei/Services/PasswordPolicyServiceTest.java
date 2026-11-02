package psg.facilitei.Services;

import org.junit.jupiter.api.Test;
import psg.facilitei.Exceptions.BusinessRuleException;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PasswordPolicyServiceTest {

    private final PasswordPolicyService service = new PasswordPolicyService();

    @Test
    void aceitaLimitesEFraseSenha() {
        assertAll(
                () -> assertDoesNotThrow(() -> service.validate("LuaVerdeAzul")),
                () -> assertDoesNotThrow(() -> service.validate("x".repeat(71) + "9")),
                () -> assertDoesNotThrow(() -> service.validate("cafe azul perto da janela")));
    }

    @Test
    void rejeitaComprimentoForaDoIntervalo() {
        BusinessRuleException curta = assertThrows(
                BusinessRuleException.class,
                () -> service.validate("a".repeat(11)));
        BusinessRuleException longa = assertThrows(
                BusinessRuleException.class,
                () -> service.validate("a".repeat(73)));

        assertEquals("A senha deve ter entre 12 e 72 caracteres.", curta.getMessage());
        assertEquals("A senha deve ter entre 12 e 72 caracteres.", longa.getMessage());
    }

    @Test
    void rejeitaSenhasComuns() {
        assertAll(
                () -> assertThrows(BusinessRuleException.class,
                        () -> service.validate("senha12345678")),
                () -> assertThrows(BusinessRuleException.class,
                        () -> service.validate("qwertyuiop12")));
    }

    @Test
    void rejeitaVariacoesDoNomeDoProduto() {
        BusinessRuleException simples = assertThrows(
                BusinessRuleException.class,
                () -> service.validate("facilitei1234"));
        BusinessRuleException leet = assertThrows(
                BusinessRuleException.class,
                () -> service.validate("F@c1l1t31-123"));

        assertEquals("Escolha uma senha menos comum e mais difícil de adivinhar.", simples.getMessage());
        assertEquals("Escolha uma senha menos comum e mais difícil de adivinhar.", leet.getMessage());
    }
}
