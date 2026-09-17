# Facilitei API 🚀

![Badge em Desenvolvimento](http://img.shields.io/static/v1?label=STATUS&message=EM%20DESENVOLVIMENTO&color=GREEN&style=for-the-badge)

## 📝 Descrição do Sistema

O **Facilitei** é uma plataforma desenvolvida para simplificar a contratação de prestadores de serviços, abrangendo desde reformas (pedreiros) até manutenções técnicas (como instalação de ar-condicionado). O sistema soluciona a dificuldade de encontrar mão de obra qualificada, permitindo que o usuário escolha o profissional ideal através de uma interface intuitiva, validada por avaliações da comunidade e portfólio de fotos dos trabalhos realizados.

Este repositório contém exclusivamente o **back-end (Facilitei-Api)**, responsável por toda a regra de negócio, persistência de dados e exposição da API REST consumida pelo front-end (React).

### Funcionalidades Principais
* **Cadastro de Clientes**
* **Cadastro de Trabalhadores**
* **Login** (Cliente ou Trabalhador)
* **Recuperação de senha** (envio de link por e-mail com token de expiração)
* **Criação de Serviços**
* **Solicitação de Serviços** (fluxo cliente → trabalhador)
* **Avaliação de Serviços** (cliente avalia o serviço prestado)
* **Avaliação de Trabalhador** (cliente avalia o profissional)
* **Avaliação de Cliente** (trabalhador avalia o cliente)
* **Chat em tempo real entre Trabalhador e Cliente** (WebSocket/STOMP + histórico)
* **Upload de arquivos/fotos** (Cloudinary)

---

## 🛠 Tecnologias Utilizadas

* **Linguagem:** Java 21
* **Framework:** Spring Boot 3.4.5
  * Spring Web, Spring Data JPA, Spring Data JDBC, Spring WebSocket, Spring Mail, Spring HATEOAS
* **Build:** Maven (com Maven Wrapper `mvnw`)
* **Banco de Dados:** MySQL (produção) · H2 em memória (testes)
* **Mapeamento de Objetos:** ModelMapper
* **Documentação da API:** springdoc-openapi / Swagger UI
* **Armazenamento de Imagens:** Cloudinary
* **Testes:** JUnit 5, Mockito, MockMvc, JaCoCo (cobertura)
* **Infraestrutura:** Docker

---

## 📂 Estrutura de Diretórios

```text
Facilitei-Api/
├── .mvn/wrapper/                    # Maven Wrapper
├── docs/superpowers/plans/          # Planos de implementação (histórico técnico)
├── src/
│   ├── main/
│   │   ├── java/psg/facilitei/
│   │   │   ├── Config/                      # Configurações globais
│   │   │   │   ├── CloudinaryConfig.java
│   │   │   │   ├── ModelMapperConfig.java
│   │   │   │   ├── OpenApiConfig.java
│   │   │   │   ├── Origins.java             # Configuração de CORS
│   │   │   │   └── WebSocketConfig.java      # Broker STOMP (chat)
│   │   │   ├── Controller/                  # Endpoints da API (camada REST)
│   │   │   │   ├── domain/                  # DTOs auxiliares do chat (ChatInput/ChatOutput)
│   │   │   │   ├── ArquivoController.java
│   │   │   │   ├── AuthController.java
│   │   │   │   ├── AvaliacaoClienteController.java
│   │   │   │   ├── AvaliacaoServicoController.java
│   │   │   │   ├── AvaliacaoTrabalhadorController.java
│   │   │   │   ├── ClienteController.java
│   │   │   │   ├── LiveChatController.java
│   │   │   │   ├── ServicoController.java
│   │   │   │   ├── SolicitacaoServicoController.java
│   │   │   │   └── TrabalhadorController.java
│   │   │   ├── DTO/                         # Data Transfer Objects (request/response)
│   │   │   ├── Entity/                      # Entidades JPA
│   │   │   │   └── Enum/                    # StatusServico, StatusSolicitacao, TipoServico
│   │   │   ├── Exceptions/                  # Tratamento de erros (GlobalExceptionHandler)
│   │   │   ├── Repository/                  # Camada de acesso a dados (Spring Data JPA)
│   │   │   ├── Services/                    # Regras de negócio
│   │   │   └── FaciliteiApplication.java
│   │   └── resources/
│   │       ├── static/                      # Página estática de health-check
│   │       └── application.properties
│   └── test/
│       ├── java/psg/facilitei/
│       │   ├── Controller/                  # Testes de controller (ex.: chat)
│       │   ├── Integration/                 # Testes de integração (fluxo completo, H2 + MockMvc)
│       │   ├── Services/                    # Testes unitários das regras de negócio
│       │   └── FaciliteiApplicationTests.java
│       └── resources/
│           └── application-test.properties  # Perfil de teste (H2)
├── .gitattributes
├── .gitignore
├── Dockerfile
├── mvnw & mvnw.cmd
├── pom.xml
└── README.md
```

---

## 🔌 Endpoints da API

> Prefixo base: `/api`. Documentação interativa disponível em `/swagger-ui.html` com a aplicação em execução.

### 🔐 Autenticação — `/api/auth`
| Método | Endpoint | Descrição |
|---|---|---|
| POST | `/api/auth/login` | Autentica um Cliente ou Trabalhador pelo e-mail e senha |
| GET | `/api/auth/check-email?email=` | Verifica se um e-mail já está cadastrado |
| POST | `/api/auth/forgot-password` | Solicita recuperação de senha (gera token e envia e-mail) |
| POST | `/api/auth/reset-password` | Redefine a senha a partir de um token válido |

### 👤 Cliente — `/api/clientes`
| Método | Endpoint | Descrição |
|---|---|---|
| POST | `/api/clientes` | Cria um novo cliente |
| GET | `/api/clientes/id/{id}` | Busca cliente por ID |
| GET | `/api/clientes/avaliacoes/{id}` | Lista avaliações recebidas pelo cliente (feitas por trabalhadores) |
| GET | `/api/clientes/avaliacaoservico/{id}` | Lista avaliações que o cliente fez sobre serviços contratados |
| PUT | `/api/clientes/editar/{id}` | Atualiza os dados do cliente |
| DELETE | `/api/clientes/delete/{id}` | Remove um cliente |
| PATCH | `/api/clientes/{id}` | Atualiza parcialmente (ex.: nota do cliente) |

### 🧑‍🔧 Trabalhador — `/api/trabalhadores`
| Método | Endpoint | Descrição |
|---|---|---|
| POST | `/api/trabalhadores` | Cria um novo trabalhador |
| GET | `/api/trabalhadores/listar` | Lista todos os trabalhadores |
| GET | `/api/trabalhadores/buscarPorId/{id}` | Busca trabalhador por ID |
| PUT | `/api/trabalhadores/atualizar/{id}` | Atualiza os dados do trabalhador |
| DELETE | `/api/trabalhadores/delete/{id}` | Remove um trabalhador |
| PATCH | `/api/trabalhadores/{id}` | Atualiza parcialmente (ex.: nota do trabalhador) |

### 🧰 Serviço — `/api/servicos`
| Método | Endpoint | Descrição |
|---|---|---|
| POST | `/api/servicos` | Cria um novo serviço |
| GET | `/api/servicos/{id}` | Busca serviço por ID |
| PUT | `/api/servicos/{id}` | Atualiza um serviço existente |
| DELETE | `/api/servicos/{id}` | Remove um serviço |
| GET | `/api/servicos/por-cliente/{clienteId}` | Lista serviços de um cliente |
| GET | `/api/servicos?trabalhadorId=&clienteId=` | Lista todos os serviços, com filtro opcional por trabalhador ou cliente |

### 📨 Solicitação de Serviço — `/api/solicitacoes-servico`
| Método | Endpoint | Descrição |
|---|---|---|
| GET | `/api/solicitacoes-servico` | Lista todas as solicitações |
| GET | `/api/solicitacoes-servico/{id}` | Busca solicitação por ID |
| POST | `/api/solicitacoes-servico` | Cria uma nova solicitação (cliente → trabalhador) |
| PUT | `/api/solicitacoes-servico/{id}` | Atualiza uma solicitação |
| PATCH | `/api/solicitacoes-servico/{id}` | Atualiza parcialmente (ex.: status) |
| DELETE | `/api/solicitacoes-servico/{id}` | Remove uma solicitação |

### ⭐ Avaliação de Cliente — `/api/avaliacoes-cliente`
*(trabalhador avalia o cliente)*

| Método | Endpoint | Descrição |
|---|---|---|
| POST | `/api/avaliacoes-cliente` | Cria uma avaliação para o cliente |
| GET | `/api/avaliacoes-cliente/cliente/{clienteId}` | Lista avaliações recebidas por um cliente |
| GET | `/api/avaliacoes-cliente/trabalhador/{trabalhadorId}` | Lista avaliações feitas por um trabalhador |
| DELETE | `/api/avaliacoes-cliente/{avaliacaoId}` | Remove uma avaliação |

### ⭐ Avaliação de Serviço — `/api/avaliacoes-servico`
*(cliente avalia o serviço prestado; recalcula a nota média do trabalhador)*

| Método | Endpoint | Descrição |
|---|---|---|
| POST | `/api/avaliacoes-servico/Criar` | Cria uma avaliação de serviço |
| GET | `/api/avaliacoes-servico/{servicoId}` | Lista avaliações de um serviço |
| GET | `/api/avaliacoes-servico/trabalhador/{trabalhadorId}` | Lista avaliações recebidas por um trabalhador |
| DELETE | `/api/avaliacoes-servico/{id}` | Remove uma avaliação (recalcula a média) |

### ⭐ Avaliação de Trabalhador — `/api/avaliacoes-trabalhador`
*(cliente avalia o trabalhador)*

| Método | Endpoint | Descrição |
|---|---|---|
| POST | `/api/avaliacoes-trabalhador` | Cria uma avaliação para o trabalhador |
| GET | `/api/avaliacoes-trabalhador?trabalhadorId=` | Lista avaliações recebidas por um trabalhador |

### 📎 Arquivos — `/api/arquivos`
| Método | Endpoint | Descrição |
|---|---|---|
| POST | `/api/arquivos/upload` | Faz upload de um arquivo (multipart/form-data) para o Cloudinary e retorna a URL |

### 💬 Chat — `/api/chat` + WebSocket
| Tipo | Endpoint | Descrição |
|---|---|---|
| GET (REST) | `/api/chat/historico/{servicoId}` | Retorna o histórico de mensagens de um serviço |
| WebSocket (STOMP) | `ws://.../buildrun-livechat-websocket` | Endpoint de conexão do chat em tempo real |
| STOMP SEND | `/app/chat/{servicoId}` | Envia uma mensagem para o serviço |
| STOMP SUBSCRIBE | `/topics/chat/{servicoId}` | Recebe mensagens em tempo real do serviço |

---

## ✅ Testes

O projeto conta com testes unitários (JUnit 5 + Mockito) para as principais regras de negócio e um teste de integração (MockMvc + banco H2 em memória) cobrindo o fluxo completo de contratação e avaliação.

```Bash
./mvnw test
```

---

## 🚀 Instruções de Execução

### Pré-requisitos
* Java JDK 21
* Maven (ou utilize o `mvnw` incluso no projeto)
* MySQL (para execução local sem Docker)
* Docker (opcional)

### Passo a Passo

#### 1. Clonar o Repositório
```Bash
git clone https://github.com/jorgearaujor/Facilitei-Api.git
cd Facilitei-Api
```

#### 2. Configurar variáveis de ambiente

O `application.properties` já traz valores padrão, mas para a funcionalidade de recuperação de senha é necessário configurar um servidor SMTP:

```Bash
MAIL_HOST=smtp.seuservidor.com
MAIL_PORT=587
MAIL_USERNAME=seu-usuario
MAIL_PASSWORD=sua-senha
RESET_PASSWORD_URL=http://localhost:5173/reset-password
```

#### 3. Executar com Maven

```Bash
./mvnw spring-boot:run
```
* API: http://localhost:8080
* Swagger UI: http://localhost:8080/swagger-ui.html

#### 4. Executar com Docker

```Bash
docker build -t facilitei-api .
docker run -p 8080:8080 facilitei-api
```

---

## 👥 Contribuições da Equipe

<table>
  <tr>
    <td align="center">
      <a href="https://github.com/ArthurEstrela">
        <img src="https://github.com/ArthurEstrela.png" width="100px;" alt="Foto do Arthur"/><br>
        <sub>
          <p>Arthur</p>
          <b>Função: Back-End, Front-End e Documentação</b>
        </sub>
      </a>
    </td>
    <td align="center">
      <a href="https://github.com/jorgearaujor">
        <img src="https://github.com/jorgearaujor.png" width="100px;" alt="Foto Jorge"/><br>
        <sub>
          <p>Jorge Afonso</p>
          <b>Função: Back-End, Banco de Dados, Infraestrutura Docker e Documentação</b>
        </sub>
      </a>
    </td>
    <td align="center">
      <a href="https://github.com/LDRRosa">
        <img src="https://github.com/LDRRosa.png" width="100px;" alt="Foto Leandro"/><br>
        <sub>
          <p>Leandro</p>
          <b>Função: Back-End, Banco de Dados e Documentação</b>
        </sub>
      </a>
    </td>
  </tr>
  <tr>
    <td align="center">
      <a href="https://github.com/PedroR07">
        <img src="https://github.com/PedroR07.png" width="100px;" alt="Foto Pedro"/><br>
        <sub>
          <p>Pedro Cesar</p>
          <b>Função: Back-End</b>
        </sub>
      </a>
    </td>
    <td align="center">
      <a href="https://github.com/ricardoissadesousa">
        <img src="https://github.com/ricardoissadesousa.png" width="100px;" alt="Foto Ricardo"/><br>
        <sub>
          <p>Ricardo</p>
          <b>Contribuição: Back-End, Front-End e Documentação</b>
        </sub>
      </a>
    </td>
    <td align="center">
      <a href="https://github.com/savioissa21">
        <img src="https://github.com/savioissa21.png" width="100px;" alt="Foto Savio"/><br>
        <sub>
          <p>Savio</p>
          <b>Função: Back-End, Front-End e Documentação</b>
        </sub>
      </a>
    </td>
  </tr>
</table>
