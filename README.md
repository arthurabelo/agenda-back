# Agenda TJPI • Back-End (Spring Boot)

### Link da API (Homologação)
https://tjpi.jus.br

### Link do Projeto (GitLab)
https://tjpi.jus.br

### Descrição
O **Back-End da Agenda TJPI** é uma API RESTful de alta performance desenvolvida em Java com o framework Spring Boot. O sistema utiliza o **PostgreSQL como SGBD (Sistema Gerenciador de Banco de Dados)** oficial para o armazenamento e consistência dos registros. 

Sua função principal é servir como motor de dados para o Front-end, gerenciando regras de negócio, autenticação integrada ao Active Directory, logs de auditoria e geração dinâmica de planilhas.

---

## 1. Estrutura do Projeto e Diretórios (Padrão MVC)

Todo o código-fonte principal da aplicação está localizado no pacote raiz:
`src/main/br/jus/tjpi/agendatelefonica`

Abaixo está a descrição das responsabilidades de cada camada estrutural do projeto:

*   `config/`: Classes de configuração global do sistema (Segurança, filtros CORS, beans do Spring e definições do Active Directory).
*   `controller/` (Exposição): Endpoints REST da API. Recebe as requisições HTTP do React, valida as entradas e retorna os dados (JSON ou streams de arquivos).
*   `dto/` (Data Transfer Object): Objetos de transferência de dados utilizados para moldar e envelopar as payloads de entrada e saída, otimizando o tráfego de rede.
*   `exception/`: Manipuladores globais de erros (`GlobalExceptionHandler`). Captura falhas internas e devolve respostas amigáveis com o status HTTP correto.
*   `model/` (Dados/Mapeamento): Entidades JPA anotadas que representam fisicamente as tabelas no SGBD PostgreSQL (ex: `Contato`, `AuditLog`, `Usuario`).
*   `repository/` (Persistência): Interfaces que estendem o Spring Data JPA para executar consultas SQL brutas ou métodos derivados (JPQL) diretamente no PostgreSQL.
*   `service/` (Regras de Negócio): Camada lógica que processa as regras do sistema, gerenciamento de auditorias e montagem de dados via Apache POI.
*   `AgendaTelefonicaApplication.java`: Classe principal com o método `main` que inicializa o servidor do Spring Boot.

### 1.2 Recursos e Configurações (`src/main/resources`)
*   `application.properties`: Arquivo base e global. Contém configurações locais de localhost (modo de desenvolvimento) e mapeamentos globais do AD do Tribunal.
*   `application-hom.properties`: Configurações ativadas via `SPRING_PROFILES_ACTIVE=hom`. Focado no ambiente de homologação.
*   `application-prod.properties`: Configurações ativadas via `SPRING_PROFILES_ACTIVE=prod`. Focado no ambiente da VM de produção.

---

## 2. Endpoints da API (Consumidos pelo Front-End)

### 2.1 Autenticação
*   `POST /api/auth/login`
    *   **Descrição:** Valida as credenciais do usuário contra o Active Directory (AD).
    *   **Corpo da Requisição (JSON):**
        ```json
        {
          "username": "usuario.usuario",
          "password": "senha_do_ad"
        }
        ```

### 2.2 Gerenciamento de Contatos (CRUD - Restrito a Admins)
*   `GET /api/contatos`
    *   **Descrição:** Retorna a listagem completa de contatos cadastrados.
*   `POST /api/contatos`
    *   **Descrição:** Cadastra um novo contato ou unidade no sistema.
    *   **Corpo da Requisição (JSON):**
        ```json
        {
          "unidade": "FÓRUM CÍVEL",
          "setor": "2ª VARA DA FAMÍLIA",
          "comarca": "teresina",
          "meioDeContato": "VOIP",
          "tipoContato": "FIXO",
          "telefone": "3215-0000"
        }
        ```
*   `PUT /api/contatos/{id}` (ou `PATCH`)
    *   **Descrição:** Atualiza as informações de um contato existente baseado no ID.
    *   **Parâmetro de URL:** `{id}` (Long) - ID do registro no banco.
    *   **Corpo da Requisição (JSON):**
        ```json
        {
          "id": 45,
          "unidade": "FÓRUM CÍVEL OTIMIZADO",
          "telefone": "3215-9999"
        }
        ```
*   `DELETE /api/contatos/{id}`
    *   **Descrição:** Remove um contato do sistema baseado no seu ID identificador.
    *   **Parâmetro de URL:** `{id}` (Long) - ID do registro a ser excluído.

### 2.3 Filtros Avançados e Relatórios
*   `GET /api/contatos/comarcas-ativas`
    *   **Descrição:** Retorna a lista de strings com os nomes das cidades ativas no banco.
*   `GET /api/contatos/relatorio/exportar`
    *   **Descrição:** Exporta a planilha Excel parametrizada. Protegido por segurança.
    *   **Parâmetros de Busca (Query Params - Opcionais):** Passados na URL (`?comarca=altos&meioDeContato=VOIP`).
        *   `comarca` (String): Filtra pela cidade.
        *   `meioDeContato` (String): Filtra pela plataforma.
        *   `tipoContato` (String): Filtra pela linha.
        *   `unidade` (String): Filtra por palavra-chave da unidade.

### 2.4 Auditoria e Segurança
*   `GET /api/auditoria`
    *   **Descrição:** Retorna o histórico de logs gravados na tabela `audit_logs` para visualização dos administradores.

---

## 3. Estrutura de Ambientes e SGBD (Properties)
O projeto utiliza propriedades flexíveis e encadeadas para garantir o funcionamento estável tanto em infraestrutura de contêineres quanto em Máquinas Virtuais (VMs) dedicadas rodando o **PostgreSQL**:

*   `application.properties`: Possui tolerância de contingência para `localhost`. Centraliza as propriedades globais do AD.
*   `application-hom.properties`: Utiliza a propriedade `spring.jpa.hibernate.ddl-auto=update` para sincronizar as tabelas de teste automaticamente na base PostgreSQL de homologação.
*   `application-prod.properties`: Utiliza **`ddl-auto=validate`** para impedir que o Hibernate altere ou apague qualquer tabela de produção por acidente, garantindo a integridade absoluta dos dados reais na VM dedicada do PostgreSQL.

---

## 4. Tecnologias Utilizadas
*   **Java 17 / Spring Boot 3:** Base do ecossistema do servidor.
*   **PostgreSQL:** Sistema Gerenciador de Banco de Dados (SGBD) [1.1].
*   **Spring Security & JWT:** Controle de sessões e proteção das rotas administrativas.
*   **Spring Data JPA / Hibernate:** Abstração e persistência do banco de dados.
*   **Flyway Database Migrations:** Controle de versionamento de scripts SQL.
*   **Apache POI:** Manipulação e escrita estruturada de arquivos de planilhas Excel (.xlsx).
