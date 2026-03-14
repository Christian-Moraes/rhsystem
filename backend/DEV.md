# Rodar o backend em desenvolvimento (sem Docker)

Equivalente ao `npm run dev` do React: sobe a aplicação uma vez; com **Spring Boot DevTools**, ao recompilar o código o app reinicia sozinho (não precisa buildar imagem nem subir o container de novo).

## 1. Subir só o Postgres

Na **raiz do projeto**:

```bash
docker-compose up -d postgres
```

O backend em dev usa `localhost:5440` (mesma porta que o compose expõe).

## 2. Rodar o backend

Dentro da pasta **backend**:

```bash
./run-dev.sh
```

Ou, se preferir Maven direto:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

O app sobe na **porta 8086** (igual ao que o Docker usa), então o frontend continua apontando para o mesmo lugar.

## 3. Durante o desenvolvimento

- **DevTools**: ao alterar código Java e recompilar (salvar no IDE com “Build on save” ou rodar `mvn compile` em outro terminal), o Spring Boot reinicia sozinho. Não é hot-reload de método como no React, mas evita ficar buildando a imagem Docker a cada mudança.
- Variáveis de ambiente: se existir `.env` na raiz do projeto, `run-dev.sh` carrega. Para Groq (importação por imagem), defina `GROQ_API_KEY` no `.env` ou no ambiente.

## Resumo

| Comando / contexto | Uso |
|--------------------|-----|
| `./run-dev.sh` (na pasta backend) | Desenvolvimento no dia a dia, sem build Docker |
| `docker-compose up backend` | Build completo e rodar em container (ex.: CI, produção local) |
