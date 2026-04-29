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
  - soft delete do professor e das turmas.
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

### Regras de negócio já aplicadas

- Professor só acessa aluno da própria turma.
- Professor só edita/deleta a própria anotação.
- Responsável só acessa aluno vinculado.
- Diretor com acesso administrativo total no escopo implementado.
- Exceções padronizadas:
  - `400`, `401`, `403`, `404`, `409`, `500`.


  

## O que ainda precisa fazer (com base nas telas)
### Professor
- Tela de anotação com “Anexar imagem”:
  - falta suporte de anexo/upload no módulo `notes`.

### Responsável

- Tela “Enviar anotação”:
  - hoje não existe fluxo de escrita de anotação por responsável.
- Tela “Marcar como lida”:
  - falta controle de leitura para `notes`.
- Se mantiver “Anexar imagem” nessa visão:
  - falta suporte de anexo/upload no módulo `notes`.

## Banco de dados

Para o estado atual, `students` precisa de:

```sql
gender ENUM('MALE','FEMALE','OTHER') NOT NULL
```

Com `spring.jpa.hibernate.ddl-auto=validate`, qualquer divergência de schema bloqueia o startup.

## Como rodar

1. Definir variáveis:
   - `DB_USER`
   - `DB_PASSWORD`
   - `JWT_SECRET` (Base64)
2. Subir:

```bash
./mvnw spring-boot:run
```

 


