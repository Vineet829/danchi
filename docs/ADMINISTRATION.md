# Administration

The admin console is a Vaadin app that runs inside the Spring backend,
separate from the Next.js site and its API.

## Opening the admin

- `mvn spring-boot:run`: <http://localhost:8000/admin/login>
- Docker (`make build`, `local.yml`): <http://localhost:8080/admin/login>,
  through the local nginx container
- `production.yml`: `/admin/login` on the main site

Both nginx configs send `/admin`, `/admin-assets/` and `/VAADIN/` to the
Spring container (built from `docker/spring/Dockerfile`). Old `/supersecret`
bookmarks redirect to `/admin/`. The client's dev server (`npm run dev`)
passes `/admin` to Spring too. If you put another proxy in front of the admin,
keep the trailing slash on `/admin/`, because Vaadin uses that exact URL for
startup and UI requests.

You need an active staff or superuser account. Guest and resident accounts
can't get in.

## Setup

For Docker, copy `.envs/.env.example` to `.envs/.env.local` or
`.envs/.env.production`. `local.yml` already sets `COOKIE_SECURE=false`, since
the local nginx is plain HTTP, and sends mail to its Mailpit container. If you
run `mvn spring-boot:run` on `http://localhost`, set `COOKIE_SECURE=false`
yourself. Safari drops `Secure` cookies even on localhost.

You need Java 21 and PostgreSQL. To build and run the jar:

```sh
mvn -Pproduction package
java -jar target/danchi-0.1.0.jar
```

It uses the same database, signing, mail and cookie variables as the Docker
setup. Set `DOMAIN` to the resident-facing site URL, and keep secure cookies
on when you serve over HTTPS. The production build includes the admin
frontend, so the server doesn't need Node or Vite at runtime.

SMTP connect, read and write timeouts are 5 seconds. `EMAIL_CLIENT_NAME` is
the hostname sent in the SMTP greeting. It defaults to `localhost`, which is
fine for Mailpit. With an external relay, set it to your mail client's
hostname.

Versions: Spring Boot 3.5.16 and Vaadin 24.10.7, the last free Vaadin 24
release. Later 24.x patches need a paid maintenance subscription. Moving to
Vaadin 25 also means moving to Spring Boot 4, so that's a separate job. See
[Vaadin's release policy](https://vaadin.com/roadmap) and
[security advisories](https://vaadin.com/security).

## Creating an administrator

Start the packaged app once so the migrations run. Then run this with the
app's `POSTGRES_HOST`, `POSTGRES_PORT`, `POSTGRES_DB`, `POSTGRES_USER` and
`POSTGRES_PASSWORD` set:

```sh
java -Dloader.main=com.danchi.admin.CreateAdminCommand \
  -cp target/danchi-0.1.0.jar \
  org.springframework.boot.loader.launch.PropertiesLauncher \
  admin@example.com community_admin
```

It asks for the password twice and doesn't echo it. Passwords need 12 to 128
characters, including an uppercase letter, a lowercase letter and a digit. For
automated setup you can set `TENANTSPHERE_ADMIN_PASSWORD` from a secret
manager instead. Don't put passwords on the command line or in the repo.
Optional third and fourth arguments set the first and last name.

The command creates a new active superuser with its profile and audit history,
all in one transaction. It won't take an email or username that already
exists, and it never promotes or overwrites a resident's account. It needs
direct database access. There's no web setup page.

## What you can manage

| Area | What you can do |
| --- | --- |
| Users | Create, edit, delete. Hashed passwords with confirmation, active/staff/superuser flags, groups and direct permissions, join and login dates. New users get a profile automatically. |
| Profiles | Create missing profiles, edit personal and contact details, upload or remove an avatar (validated), delete. Report counts and reputation come from the reports. Deleting a profile deactivates the account. |
| Apartments | Create, edit, assign or reassign a tenant, delete (with a preview of the issues deleted along with it). |
| Issues | Create, edit the issue's fields, assign someone, set status and priority, delete. Bulk status changes and related content views. Resolving an issue sets its resolved date and reopening clears it. Notifications only go out after the change is committed. |
| Posts | Create, edit title/body/author/tags, delete. Related content views. Vote totals only change through real votes. |
| Ratings | Create, edit, delete. A rating goes from a resident to a technician. The list shows each technician's average rating. |
| Reports | Create, edit reporter/target/details, delete. Reporter counts and reputation stay in sync. |
| Content views | Create, edit, delete. You pick a real post or issue, and IP addresses are validated. |
| Tags | Create, rename, delete, see usage. Removing a tag takes it off posts without deleting them. |
| Groups | Create, rename, delete, assign model permissions. Superusers only. |

Every section has search, sorting, database paging, creation-date filters,
record history and bulk delete with confirmation. Where it makes sense there
are filters for active/staff/superuser, gender/occupation, building/floor,
issue status/priority, rating and content type. Post and issue editors show
recent content views. You manage those under Content views.

Record IDs, creation and update times, slugs, votes, report counts and
reputation are set by the app, so they aren't form fields.

## Permissions and sessions

- Superusers have every model permission. Only they can manage groups, grants,
  privileged accounts and permission flags.
- Staff need `resource.view`, `.add`, `.change` or `.delete`. Change also
  allows view. Group grants and direct grants add up.
- Migration V4 creates the **Community administrators** group and puts
  existing staff in it, so they keep their access. New staff need grants.
- Permissions and account status are checked again on every action, including
  callbacks from a page that's already open.
- Staff can't give themselves privileges or change another staff or superuser
  account's credentials.
- Admins can't delete their own account or remove their own admin access. At
  least one active superuser always remains.
- Sign-ins, sign-ups, activation and password-reset emails, and Google sign-in
  starts are rate limited per network address. Sign-ins and emails are also
  limited per email address. Activation and reset emails are capped at 100 an
  hour overall, to stay under the mailbox's sending limit. The counts are
  stored hashed in the database, so all instances share them.
- Changing a password or deactivating an account bumps its token version. Old
  access and refresh tokens then stop working, even after reactivation.
  Sessions that existed when migration V5 first ran were kept.
- JWT auth stays stateless. Vaadin keeps its UI state in a separate HTTP
  session. That session's ID changes on each successful sign-in, not on every
  request.
- Saves compare the record's revision with the one the editor loaded, so
  nobody overwrites someone else's change without noticing.

## Reports and deletion

An account is deactivated once **five different accounts** have reported it.
Guest accounts don't count. Several reports from the same person count once,
and you can't report yourself. When a report is edited, deleted or removed in
a cascade, the counts are recalculated for every account it affected. Removing
reports doesn't reactivate anyone automatically. A superuser has to review the
account and reactivate it after the count drops below five. Staff and
superusers are never deactivated by reports. Reports about them stay for other
admins to review. If a save leaves an account with five or more reporters and
no staff or superuser status, it's deactivated, just as a fifth report would.

Deleting asks for confirmation and shows how many dependent records are
affected and how. The service checks that you can delete every model in the
cascade and that the preview is still current. A delete takes at most 100
selected records and 5,000 affected records in all. Deleting a user also fixes
the vote totals and report counts that remain. Database triggers clean up
orphaned post and issue content views.

Admin history records who did what to which record, with before and after
values, in the same transaction as the change. Passwords and hashes are left
out. Cascade deletes are recorded, and history is kept even after the user or
record is gone. A database trigger blocks edits and deletes of history rows.
History starts at migration V4, so there's none for earlier changes.

## Tests

`AdminManagementTest` runs against PostgreSQL. It covers every resource's
lifecycle, permissions, stale edits, report thresholds, cascades, bulk
rollback, uploads, search and paging, password hashing and append-only
history. The auth, voting, profile and safety tests are in the same Maven
suite. The session tests check that UI sessions stay stable across JWT
requests and that the session rotates on password sign-in. These tests don't
drive a browser, so check form submits and routing through the packaged app
and nginx by hand.

The tests use Testcontainers and need Docker running. If your Docker rejects
the client's default API version, run Maven with `-Dapi.version=1.44`.
