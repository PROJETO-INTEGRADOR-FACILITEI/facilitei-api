package psg.facilitei.Services;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import psg.facilitei.DTO.ClienteResponseDTO;
import psg.facilitei.DTO.LoginResponseDTO;
import psg.facilitei.DTO.TrabalhadorResponseDTO;
import psg.facilitei.Entity.Cliente;
import psg.facilitei.Entity.Trabalhador;
import psg.facilitei.Repository.ClienteRepository;
import psg.facilitei.Repository.TrabalhadorRepository;

import java.util.Optional;

@Service
public class AuthService {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private TrabalhadorRepository trabalhadorRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private PasswordHashService passwordHashService;

    public LoginResponseDTO login(String email, String senha) {
        Optional<Cliente> clienteOpt = clienteRepository.findByEmail(email);
        if (clienteOpt.isPresent()) {
            Cliente cliente = clienteOpt.get();
            if (passwordHashService.matches(senha, cliente.getSenha())) {
                if (!passwordHashService.isHashed(cliente.getSenha())) {
                    cliente.setSenha(passwordHashService.hash(senha));
                    clienteRepository.save(cliente);
                }
                ClienteResponseDTO dto = modelMapper.map(cliente, ClienteResponseDTO.class);
                return new LoginResponseDTO("cliente", dto);
            }
        }

        Optional<Trabalhador> trabalhadorOpt = trabalhadorRepository.findByEmail(email);
        if (trabalhadorOpt.isPresent()) {
            Trabalhador trabalhador = trabalhadorOpt.get();
            if (passwordHashService.matches(senha, trabalhador.getSenha())) {
                if (!passwordHashService.isHashed(trabalhador.getSenha())) {
                    trabalhador.setSenha(passwordHashService.hash(senha));
                    trabalhadorRepository.save(trabalhador);
                }
                TrabalhadorResponseDTO dto = modelMapper.map(trabalhador, TrabalhadorResponseDTO.class);
                return new LoginResponseDTO("trabalhador", dto);
            }
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email ou senha inválidos.");
    }

    public boolean emailExists(String email) {
        return clienteRepository.findByEmail(email).isPresent() ||
               trabalhadorRepository.findByEmail(email).isPresent();
    }

    public LoginResponseDTO restaurarSessao(String role, Long userId) {
        if ("cliente".equals(role)) {
            Cliente cliente = clienteRepository.findById(userId)
                    .orElseThrow(this::sessaoInvalida);
            return new LoginResponseDTO("cliente", modelMapper.map(cliente, ClienteResponseDTO.class));
        }
        if ("trabalhador".equals(role)) {
            Trabalhador trabalhador = trabalhadorRepository.findById(userId)
                    .orElseThrow(this::sessaoInvalida);
            return new LoginResponseDTO(
                    "trabalhador", modelMapper.map(trabalhador, TrabalhadorResponseDTO.class));
        }
        throw sessaoInvalida();
    }

    private ResponseStatusException sessaoInvalida() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão inválida ou expirada.");
    }
}
