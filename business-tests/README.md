# Testes das regras de negócio

Projeto Maven independente para executar os testes unitários dos serviços com JUnit 5 e Mockito. Não inicia a aplicação Spring Boot, não conecta ao MySQL e não utiliza credenciais, Google Cloud ou rede durante os testes (o Maven pode precisar baixar dependências na primeira execução).

## Executar

Na raiz do repositório, com Java 17 ou superior:

```bash
./mvnw -f business-tests/pom.xml clean test
```

Executar apenas um conjunto de regras:

```bash
./mvnw -f business-tests/pom.xml -Dtest=NoteServiceTest test
```

Os relatórios ficam em `business-tests/target/surefire-reports`.

## Organização

Este projeto compila diretamente o código atual de `../src/main/java` e reutiliza os testes de `../src/test/java`, selecionando apenas `service/*Test.java`. Assim, os mesmos testes também podem ser executados no projeto principal, sem duplicar código ou precisar instalar o artefato da aplicação. O teste de inicialização `SchoolAgendaApplicationTests` fica fora desta execução porque depende da infraestrutura do Spring.

As dependências de compilação são declaradas neste POM; alterações nas dependências da aplicação que afetem os serviços também devem ser refletidas aqui.

## Regras cobertas

| Área | Comportamentos |
| --- | --- |
| Autenticação | Token e perfil após autenticação; credenciais inválidas e usuário ausente/inativo não recebem token |
| Turmas da direção | Cadastro, conflito por nome ativo, listagem e exclusão lógica; turma inexistente não pode ser excluída |
| Acesso às turmas | Professor acessa alunos da própria turma; acesso externo é bloqueado; turmas excluídas não aparecem na consulta do professor |
| Alunos | Permissões por perfil, vínculo com professor/responsável e bloqueio de alunos excluídos |
| Comunicados | Permissões de criação, consulta, edição e exclusão; visibilidade para responsáveis; acesso da direção; responsável não pode excluir; tipo inválido e mais de cinco anexos são rejeitados antes de gravar |
| Professores | Cadastro, e-mail duplicado, listagem e exclusão lógica |
| Famílias | Cadastro de responsável, filhos e vínculos; e-mail duplicado e turma inexistente |
| Vínculos familiares | Criar/desfazer vínculo, rejeitar duplicidade, vínculo inexistente e aluno excluído |

## Limites

A suíte inclui testes unitários e testes locais de persistência/transação com H2 em memória (modo MySQL para leituras): consultas JPA no MySQL real, filtros HTTP, validação de DTOs e assinatura JWT precisam de testes de integração próprios. Os testes de transação usam banco local real e repositórios simulados para provocar falhas de upload e verificar rollback; não substituem testes com MySQL e Google Cloud. Os cenários com registros ausentes/inativos simulam o retorno dos repositórios; não validam o SQL.

Para novas regras, adicione testes em `src/test/java/com/schoolagenda/service`, com casos de sucesso, negação de acesso e prevenção de gravações indevidas. Use mocks apenas para dependências externas ao serviço.

O projeto usa o modo `mock-maker-subclass` do Mockito, suficiente para os serviços atuais e compatível com ambientes que bloqueiam a instalação dinâmica de agentes na JVM.

## Antes do build

O fluxo `.github/workflows/docker.yml` executa esta suíte em pull requests para `main` e em pushes para `main`. O job de build e publicação depende do sucesso dos testes e só executa em pushes. Os relatórios são anexados à execução do GitHub Actions.

O `Dockerfile` também executa a suíte antes de empacotar a aplicação, protegendo builds locais. Após a aprovação da suíte, o empacotamento pula os testes do projeto principal, incluindo o teste de contexto que exige infraestrutura.

### Regressões de bilhetes e leituras

`NotePermissionsTest` valida status versus autoria, visibilidade, consulta individual, vínculos e bloqueio de registros excluídos. `NoteReadPersistenceTest` executa o SQL em H2, verifica inicialização repetida, leitura por usuário e gravações simultâneas. `NoteUploadTransactionTest` exercita o serviço através do interceptor transacional do Spring, com banco H2 real, falhas nos anexos, rollback externo e limpeza após commit/rollback.

`NoteHttpCompatibilityTest` verifica os caminhos HTTP e a compatibilidade da listagem e do PATCH existentes com MockMvc; as permissões são verificadas nos testes dos serviços, não nesse MockMvc sem filtros de segurança.
