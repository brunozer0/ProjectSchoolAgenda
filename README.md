# School Agenda API

Backend da agenda escolar com JWT, regras por perfil e anotações.

## O que já foi feito

### Base e segurança

- Estrutura em camadas.
- JWT stateless com Spring Security.
- Login único:
  - `POST /api/auth/login`
- Roles ativas:
  - `DIRECTOR`, `TEACHER`, `RESPONSIBLE`
- Swagger:
  - `/swagger-ui.html`
  - `/v3/api-docs`
- Postman atualizado em `postman/`.

### Diretor (alinhado com telas enviadas)

- Cadastrar família:
  - `POST /api/director/families`
  - cria `user` responsável + `responsible` + alunos + vínculos.
- Cadastrar professor:
  - `POST /api/director/teachers`
  - cria `user` professor + `teacher` + turmas.
- Deletar professor:
  - `DELETE /api/director/teachers/{teacherId}`
  - exclusão lógica do usuário do professor e remoção dos vínculos com as turmas, preservando as turmas.
- Listar professores (para tela de deletar):
  - `GET /api/director/teachers`
- Vincular/desvincular responsável-aluno:
  - `POST /api/director/responsibles/{responsibleId}/students/{studentId}`
  - `DELETE /api/director/responsibles/{responsibleId}/students/{studentId}`
- Deletar família (menu já existe na UI):
  - `GET /api/director/families`
  - `DELETE /api/director/families/{responsibleId}`
- Criar, listar  e deletar sala:
  - `GET api/director/classroomss`
  - `DELETE api/director/classrooms{classroomsId}`
  - `POST api/director/classrooms`
  

### Professor

- Gerenciar anotações:
  - `POST /api/notes`
  - `PATCH /api/notes/{id}`
  - `DELETE /api/notes/{id}`
- Visualizar anotações por aluno:
  - `GET /api/notes/student/{studentId}`
- Listar alunos do professor:
  - `GET /api/students/my/teacher`

- Listar salas do professor:
  - `GET /api/classrooms/my/teacher`

- Listar alunos da sala:
  - `GET /api/classrooms/{classroomId}/students`


### Responsável

- Listar alunos vinculados:
  - `GET /api/students/my/responsible`
- Buscar aluno por id (com regra de vínculo):
  - `GET /api/students/{id}`
- Visualizar anotações do aluno vinculado:
  - `GET /api/notes/student/{studentId}`
  - somente `is_visible_to_responsible = true`.


### Notes
- Suporte a upload  de imagem:
- Retorno da role do autor da nota.

### Regras de negócio já aplicadas

- Professor só acessa aluno da própria turma.
- Professor edita o conteúdo da própria anotação e exclui somente as próprias, enquanto tiver acesso ao aluno.
- Professor da turma pode alterar o status de bilhetes recebidos sem alterar seu conteúdo.
- Responsável pode alterar o status de bilhetes visíveis do aluno vinculado e editar apenas conteúdo de sua autoria.
- Diretor pode editar conteúdo e status; bilhetes e alunos excluídos ficam indisponíveis para todos os perfis.
- Responsável só acessa aluno vinculado.
- Diretor com acesso administrativo total no escopo implementado.
- Exceções padronizadas:
  - `400`, `401`, `403`, `404`, `409`, `500`.


## O que ainda precisa fazer (com base nas telas)

### Responsável
- Integrar a interface ao controle de leitura opcional já disponível na API.
- Notes globais (possível funcionalidade futura).

### Correções dos bilhetes

- Consulta individual: `GET /api/notes/{id}`, com as mesmas permissões da listagem.
- O `PATCH /api/notes/{id}` continua aceitando os campos existentes. Alterar apenas `status` é permitido a quem tem acesso ao bilhete; título, conteúdo, tipo e visibilidade exigem autoria ou direção. Uma requisição mista não autorizada é rejeitada por inteiro.
- Bilhetes excluídos ou de alunos excluídos retornam `404` nas operações por ID, evitando operações sobre registros ausentes das listagens.
- Criação com anexos é transacional: falhas desfazem as gravações no banco e tentam remover os arquivos enviados. Falhas na limpeza são registradas no log para recuperação operacional. O banco e o armazenamento externo não compartilham uma transação distribuída.
- Exclusão só remove arquivos externos após o commit do banco.

### Controle de leitura opcional e compatível com a tela atual

Nenhuma consulta marca um bilhete como lido. Os retornos existentes de listagem, criação e edição permanecem com o mesmo formato. `OPEN`/`RESOLVED` continuam sendo status de atendimento, independentes da leitura.

- `PUT /api/notes/{id}/read`: registra a primeira leitura do usuário autenticado. Repetições preservam a data original.
- `GET /api/notes/{id}/read`: consulta somente a leitura do usuário autenticado, sem gravar.
- Ambos retornam `{ "noteId": 123, "read": true, "readAt": "2026-10-30T12:00:00" }`. Antes da leitura, `read` é `false` e `readAt` é `null`.
- Acesso exige aluno ativo e vínculo com a turma (professor) ou vínculo familiar e bilhete visível (responsável); direção tem acesso administrativo.
- A tabela `note_reads` é criada automaticamente pelo script idempotente `src/main/resources/db/note-reads.sql` na inicialização. O usuário do banco precisa de permissão para criar essa tabela. Nenhuma tabela existente é alterada e não há preenchimento retroativo de leituras.
- Professores desvinculados da turma não mantêm acesso ao histórico, mesmo sendo autores; essa regra continua baseada no vínculo atual.


## Banco de dados

Para o estado atual, `students` precisa de:

```sql
gender ENUM('MALE','FEMALE','OTHER') NOT NULL
```

Com `spring.jpa.hibernate.ddl-auto=validate`, qualquer divergência de schema bloqueia o startup.

## Como rodar

1. Definir variáveis:
   - `DB_URL` → URL de conexão com o banco de dados MySQL
   - `DB_USER` → usuário do banco de dados
   - `DB_PASSWORD` → senha do banco de dados
   - `JWT_SECRET` → chave secreta para assinatura dos tokens JWT (Base64)
2. Subir:

```bash
./mvnw spring-boot:run
```

 



## Testes das regras de negócio

Execute o projeto de testes independente com Java 17 ou superior:

```bash
./mvnw -f business-tests/pom.xml clean test
```

Consulte [a documentação dos testes](business-tests/README.md) para cenários cobertos, execução por serviço e limites da suíte.
