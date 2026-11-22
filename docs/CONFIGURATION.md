# Configuration

Start from `.envs/.env.example`. Copy it to `.envs/.env.local` for
`local.yml`, or to `.envs/.env.production` for `production.yml`. For
production:

- **Keys**: Set a strong `POSTGRES_PASSWORD` and a `SIGNING_KEY` from
  `openssl rand -hex 32`. Changing the signing key signs everyone out.
- **Domain**: Set `DOMAIN` to the site's https address, which emails use for
  their links, and keep `COOKIE_SECURE=true`.
- **HTTPS**: `production.yml` is a starting point that serves HTTPS with Let's
  Encrypt certificates. It doesn't request them, so get them first, for
  example with certbot, into `/etc/letsencrypt/live/` on the server. Set
  `SERVER_NAME` to the site's host name, such as `example.com`, in the shell
  or in a `.env` file beside `production.yml`. nginx uses it for
  `server_name` and the certificate paths.
- **Email**: Set the `EMAIL_*` settings and `DEFAULT_FROM_EMAIL` for a real
  mail server, such as Gmail with an app password on port 587. The settings
  cover STARTTLS; `EMAIL_USE_SSL` has no effect.
- **Google Sign-In**: Set `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET`, and
  set `REDIRECT_URIS` to your site's address followed by `/google`.
- **Guest Access**: Set `GUEST_LOGIN_ENABLED=true` to give each visitor a
  temporary account that expires after 24 hours.
