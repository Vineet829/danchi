# Contributing

Bug reports and questions are welcome as
[issues](https://github.com/Vineet829/danchi/issues).

To work on the code, you need Docker, Java 21, Maven, and Node.js 22 (22.13 or
newer). The commands below are for macOS and Linux. On Windows, use WSL. Set
up `.envs/.env.local` first, as described under
[Run with Docker](README.md#run-with-docker).

## Architecture

- **Client**: A Next.js 15 app with React 19, Redux Toolkit, and Tailwind CSS.
- **Backend**: A Java 21 and Spring Boot 3.5 API on PostgreSQL 16, with Flyway
  migrations. It sends email over SMTP and serves the admin console, built
  with Vaadin.
- **Docker**: nginx sits in front of both. It sends the API (`/api/v1`), the
  admin console (`/admin` and `/VAADIN`), and uploaded files (`/media`) to the
  backend, and the site's pages to the client.

## Run the Backend and Client Directly

Use this when working on the code, so you don't rebuild the backend's Docker
image after each change. If the full stack from `make build` is running, stop
it first with `make down`, because its client container also writes to
`client/.next`. PostgreSQL and Mailpit still run in Docker, with the same
`POSTGRES_PASSWORD` and `SIGNING_KEY` as in `.envs/.env.local`. The first
start of the backend also installs the admin console's frontend packages, so
it takes longer.

```bash
# Terminal 1: PostgreSQL and Mailpit, then the API on port 8000
docker compose -f local.yml up -d postgres mailpit
export POSTGRES_USER=tenantsphere POSTGRES_PASSWORD="your-database-password"
export SIGNING_KEY="your-signing-key"
export COOKIE_SECURE=false DOMAIN=http://localhost:3000
mvn spring-boot:run
```

```bash
# Terminal 2: the client on port 3000
cd client
npm install
npm run dev
```

Open <http://localhost:3000>. The client passes API and admin requests to the
backend on port 8000.

## Tests

Stop the client's dev server first, or the Docker stack with `make down`:
`npm run build` writes to `client/.next` too.

```bash
mvn test                  # backend, needs Docker for PostgreSQL

cd client                 # after npm install
npm run lint
npx tsc --noEmit
npm run test:regressions
npm run build
```

## Project Structure

- `src/`: The Spring Boot backend and the admin console.
- `client/`: The Next.js client.
- `package.json`, `vite.config.ts`, and `tsconfig.json` at the root: The admin
  console's frontend, which Vaadin builds.
- `docker/`: The backend, PostgreSQL, and nginx Dockerfiles and the nginx
  settings for `local.yml` and `production.yml`. The client's Dockerfiles are
  in `client/docker/`.
- `scripts/`: Scripts that fill a community with sample data.
- `docs/`: The guides to the admin console, configuration, and roles and
  privacy.
