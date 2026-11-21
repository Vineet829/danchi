# Danchi

Danchi is a community app for apartment complexes, where tenants report
maintenance issues and troublesome neighbors, technicians handle the issues
assigned to them, and tenants share posts with the community. It is
available at [tenant.confidental.space](https://tenant.confidental.space).

## Why I Built It

In my apartment complex, we reported problems through a shared WhatsApp
account, and messages often got lost. Technicians sometimes ignored requests,
and there was no simple way to report noisy or troublesome neighbors. Danchi
gives each of these a proper place. Issues are tracked and assigned,
technicians are rated on their work, and reports about a tenant lead to a
warning and, if enough neighbors report them, to their account being
deactivated.

## Features

- **Community Posts**: Tenants and staff write posts, and everyone signed in
  can reply and upvote or downvote. Filter posts by tag, see the top posts,
  and bookmark posts to read later.
- **Issue Reporting**: Report a problem with your apartment and follow its
  status. An administrator assigns it to a technician, who updates it as they
  work.
- **Tenant Reports**: Report a neighbor for misconduct. The first report sends
  them a warning email that doesn't say who reported them. Once five different
  people have reported someone, their account is deactivated automatically.
  A superuser can reactivate it after removing enough reports to bring the
  count below five. Reports from guest accounts don't count, and staff
  accounts are never deactivated by reports.
- **Tenants and Technicians**: Browse the tenants and technicians in your
  community, and rate technicians on their work.
- **Profiles**: Edit your profile and avatar, and see your posts, activity,
  and reputation.
- **Accounts**: Sign up with email activation, sign in with Google, and reset
  a forgotten password. Visitors can also look around with a temporary guest
  account that expires after 24 hours.
- **Administration**: Manage users, posts, issues, reports, ratings, tags,
  apartments, and groups in an admin console, with a change history.

## Getting Started

The commands below are for macOS and Linux. On Windows, use WSL. You need
Docker and make.

### Run with Docker

Copy the example settings, then set `POSTGRES_PASSWORD`, `SIGNING_KEY` from
`openssl rand -hex 32`, and `DOMAIN=http://localhost:8080` in
`.envs/.env.local`. The other values can stay as they are, because `local.yml`
sends mail to Mailpit and allows cookies over plain HTTP.

```bash
cp .envs/.env.example .envs/.env.local
make build
```

The first build takes a few minutes. Then open <http://localhost:8080>. Emails
go to Mailpit at <http://localhost:8025>. To stop the app, run `make down`, or
`make down-v` to delete the data as well.

### Create an Administrator

```bash
make superuser EMAIL=admin@example.com USERNAME=community_admin
```

It asks for the password. Then sign in to the admin console at
<http://localhost:8080/admin/login>.

## Documentation

- [Roles and Privacy](docs/ROLES_AND_PRIVACY.md): What each role can do, and
  who can see what.
- [Configuration](docs/CONFIGURATION.md): The settings for production.
- [Administration](docs/ADMINISTRATION.md): The admin console.

## Contributing

Bug reports and questions are welcome as
[issues](https://github.com/Vineet829/danchi/issues).
[CONTRIBUTING.md](CONTRIBUTING.md) covers the architecture, running the backend
and client directly, the tests, and the project structure.

## License

[MIT](LICENSE) © 2024-2026 Vineet Singh.
