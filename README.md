# Todo API

API REST para gerenciar tarefas (to-do list), feita com **Java 21** e **Spring Boot**.
Cada pessoa cria uma conta, faz login e passa a ter a sua própria lista de tarefas, que ninguém mais consegue ver.

Este é um projeto de estudo, criado para praticar autenticação, segurança e organização de código em camadas.

## O que a API faz

- Cadastro de usuários e login
- Autenticação com **JWT** (um "crachá digital" que o usuário envia a cada requisição)
- CRUD de tarefas: criar, listar, buscar, atualizar e apagar
- Cada usuário só acessa as **próprias** tarefas
- Validação dos dados enviados (por exemplo, título vazio é recusado)

## Tecnologias

| Tecnologia | Para que serve no projeto |
|---|---|
| Java 21 | Linguagem |
| Spring Boot 4.1.1 | Framework que monta a aplicação web |
| Spring Security | Protege as rotas |
| Spring Data JPA | Conversa com o banco sem escrever SQL na mão |
| H2 | Banco de dados em memória (os dados somem ao reiniciar) |
| jjwt | Cria e valida os tokens JWT |
| BCrypt | Embaralha as senhas antes de salvar |
| Maven | Gerencia dependências e roda o projeto |

## Como rodar

**Você precisa ter:** Java 21 instalado. O Maven já vem junto do projeto (`mvnw`), então não precisa instalar.

```bash
# Windows (PowerShell)
.\mvnw.cmd spring-boot:run

# Linux / Mac / Git Bash
./mvnw spring-boot:run
```

A API sobe em **http://localhost:8080**.

> O banco é em memória: ao parar a aplicação, todos os usuários e tarefas são apagados.

## Endpoints

| Método | Rota | Precisa de login? | O que faz |
|---|---|---|---|
| POST | `/auth/register` | Não | Cria um usuário |
| POST | `/auth/login` | Não | Devolve o token JWT |
| GET | `/tasks` | Sim | Lista as suas tarefas |
| GET | `/tasks/{id}` | Sim | Busca uma tarefa |
| POST | `/tasks` | Sim | Cria uma tarefa |
| PUT | `/tasks/{id}` | Sim | Atualiza uma tarefa |
| DELETE | `/tasks/{id}` | Sim | Apaga uma tarefa |

### Exemplos de corpo (JSON)

**Registro e login** (`username` de 3 a 50 caracteres, `password` de 6 a 100):

```json
{ "username": "agatha", "password": "123456" }
```

O login responde com o token:

```json
{ "token": "eyJhbGciOi..." }
```

**Criar ou atualizar tarefa** (`title` é obrigatório, até 255 caracteres; `description` vai até 1000):

```json
{ "title": "Estudar Spring", "description": "Autenticação com JWT", "done": false }
```

### Como usar o token

Nas rotas `/tasks`, envie o token no cabeçalho:

```
Authorization: Bearer SEU_TOKEN_AQUI
```

### Códigos de resposta

| Código | Quando acontece |
|---|---|
| 200 | Deu certo |
| 201 | Algo foi criado (usuário ou tarefa) |
| 204 | Tarefa apagada (resposta sem corpo) |
| 400 | Dados inválidos (título vazio, senha curta...) |
| 401 | Sem token, token inválido ou senha errada |
| 404 | Tarefa não existe **ou pertence a outro usuário** |
| 409 | Nome de usuário já está em uso |

## Como testar

### 1. Testes automatizados

```bash
./mvnw test
```

Os testes (`AuthFlowTests`) sobem a aplicação e conferem que:
- sem token, `/tasks` retorna 401;
- senha errada é recusada;
- um usuário não enxerga as tarefas de outro.

### 2. Manualmente com o Postman

1. Suba a API.
2. Crie uma collection com a variável `base_url` = `http://localhost:8080` e a variável `token` vazia.
3. Faça `POST {{base_url}}/auth/register` e depois `POST {{base_url}}/auth/login`.
4. Copie o `token` da resposta do login e configure na collection: **Authorization → Bearer Token → `{{token}}`**.
5. Teste as rotas de `/tasks`.

Dica: para preencher o token automaticamente, coloque este script na aba **Scripts (Post-response)** do login:

```javascript
pm.collectionVariables.set("token", pm.response.json().token);
```

### 3. Vendo o banco de dados (console do H2)

Com a API rodando, abra **http://localhost:8080/h2-console** e use:

- **JDBC URL:** `jdbc:h2:mem:tododb`
- **User Name:** `sa`
- **Password:** (vazio)

As tabelas são `USERS` e `TASK`. Na tabela `USERS` dá para ver que a senha é guardada como hash BCrypt (`$2a$...`), nunca em texto puro.

## Como o projeto está organizado

O código é dividido em **camadas**, cada uma com uma responsabilidade:

```
controller  →  service  →  repository  →  entity
(recebe as     (regras     (acessa o      (representa as
 requisições)   de negócio)  banco)         tabelas)
```

```
src/main/java/com/agatha/todo_api/
├── controller/   AuthController, TaskController
├── service/      AuthService, TaskService, JwtService
├── repository/   UserRepository, TaskRepository
├── entity/       User, Task
├── dto/          objetos de entrada e saída da API
└── security/     SecurityConfig, JwtAuthenticationFilter
```

**DTO** é um objeto usado só para entrada e saída de dados. Assim as entidades (que espelham o banco) nunca são expostas direto na API.

## Como a segurança funciona

1. **Cadastro:** a senha é transformada em hash com BCrypt antes de ser salva.
2. **Login:** a API confere a senha e devolve um token JWT, válido por 1 hora.
3. **Requisições seguintes:** o `JwtAuthenticationFilter` lê o token do cabeçalho, valida e identifica o usuário.
4. **Sem sessão:** a API não guarda estado no servidor (*stateless*). Toda a informação vem no token.

### Cada usuário só vê as suas tarefas

O dono de uma tarefa é sempre descoberto pelo **token**, nunca por algo que o cliente envie no corpo da requisição. Toda busca filtra pelo dono.

Se você pedir a tarefa de outra pessoa, recebe **404** (e não 403). Assim a API nem revela que essa tarefa existe.

## Configuração

O segredo usado para assinar os tokens fica em `application.yaml` e pode ser trocado pela variável de ambiente `JWT_SECRET` (em Base64, com pelo menos 32 bytes). O valor padrão serve **apenas para desenvolvimento**.

## Limitações conhecidas

Por ser um projeto de estudo, algumas coisas ficaram de fora:

- Banco em memória: os dados não persistem. Em um projeto real, eu usaria PostgreSQL ou MySQL.
- O console do H2 está aberto para facilitar os estudos e não deve ir para produção.
- Não há tratamento global de erros: as mensagens de erro são as padrão do Spring.
- Não há refresh token nem papéis de usuário (todos têm as mesmas permissões).
- Os testes automatizados cobrem autenticação e isolamento entre usuários, mas não há testes unitários por camada.

## Próximos passos (ideias)

- Trocar o H2 por PostgreSQL
- Adicionar um tratamento global de erros com respostas em JSON padronizado
- Documentar a API com Swagger/OpenAPI
- Ampliar os testes
- Filtrar tarefas por status (`done`) e adicionar paginação

## Autora

**Agatha** — [GitHub](https://github.com/Agathapsm) · [LinkedIn](https://www.linkedin.com/in/%C3%A1gatha-paiva-97574b251/)