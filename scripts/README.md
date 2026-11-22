# Sample data (Cedar Court)

`seed_society.py` fills a local install with a sample community. Run it
without `--apply` first to see what it would add:

```bash
python3 scripts/seed_society.py --container tenant_prod_postgres
python3 scripts/seed_society.py --container tenant_prod_postgres --apply
```

`--container` is the local PostgreSQL container. `tenant_prod_postgres` is the
one `local.yml` starts. There's no default, so it can't write to a database
you didn't name. With `--apply`, the Danchi backend has to be running on port
8000. The script uses its Argon2 encoder to give each sample account a random
password that isn't saved anywhere.

It fills three blocks, each with six floors of six homes (108 residents). It
keeps the existing residents and the guest account, and adds technicians for
all seven trades, plus posts, replies, votes, bookmarks, maintenance history
and reviews. Some of the activity shows up in the guest account's own
apartment and profile. Names and situations are made up, and email addresses
use the reserved `.example` domain. It expects an existing install with the
`testuser` account in one of these homes and at least one plumber,
electrician, carpenter and HVAC technician.

Everything is written in one transaction. UUIDs are fixed and records are only
ever inserted, so running it again is safe. Votes, replies, bookmarks and
views are only added to newly inserted posts, so a second run won't bring back
a vote or bookmark someone removed. Existing content, passwords and profile
edits are left alone. It doesn't send email, doesn't run at startup and isn't
part of the migrations.

`python3 scripts/verify_guest_society.py` checks the guest-facing API through
the frontend on port 3000. It signs in with a new temporary guest account,
like "Try as a guest" does, so each run uses one of the five guest accounts
allowed per address per hour. `--check-voting` also checks vote permissions,
validation, transitions, retries and saved counts, and puts back the post's
original vote afterwards. The frontend voting tests are `npm run test:voting`
in `client`.

## Profile photos

`python3 scripts/seed_avatars.py --container tenant_prod_postgres --with-identities`
previews the portraits and profile details for the 122 sample accounts in
`demo_avatars.json`. Add `--apply` to write them. The 32 resident identities
in `demo_profile_identities.json` are applied in the same transaction as the
photos, and everyone else keeps their name. Leave out `--with-identities` if
the database already has the identities and you only want to assign photos.
Run it after `seed_society.py`.

An identity is only changed if the account's sample UUID, home, gender and
original details still match what's recorded. Passwords, account IDs and
activity aren't touched. A photo is only set if the field is empty or still
holds the previous bundled image from the manifest, so names and photos people
changed later are kept. Running it again changes nothing.

The avatars are 384x384 JPEGs in `client/public/assets/avatars/cedar-court/`,
served by the frontend, so the app never calls an image service. The backend
stores the path in the existing `avatar` field, which the directory, profiles,
posts, replies and reviews already use. Each file has its own name and a
SHA-256 checksum in the manifest. If a file is missing, the script stops
instead of fetching a replacement. The photos show fictional people, not real
Cedar Court residents. Names, home countries and photos were picked to go
together. The guest account uses a robot avatar, `testuser-bot-v5.jpg`. Older
versions are kept for cached responses.

`python3 scripts/verify_guest_society.py --check-avatars` checks names and
hometowns, that photos match across the profile and activity APIs, that every
portrait loads through the frontend, and that no two images are the same.

The older portraits are still bundled so cached responses can load them, but
the manifest no longer uses them. They came from
[Random User Generator](https://randomuser.me/), whose
[copyright notice](https://randomuser.me/copyright) lists them as the
authorized UI Faces collection. None of the portraits imply an endorsement.
