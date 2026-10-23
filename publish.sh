#!/usr/bin/env bash
# Publish stol with scala-cli.
#
#   ./publish.sh            publish to Maven Central (version = current git tag, e.g. v0.1.0 -> 0.1.0)
#   ./publish.sh --dummy    go through signing and validation without uploading
#   ./publish.sh local      rehearse into ~/.ivy2/local (no credentials or signing needed)
#
# Metadata is passed as flags rather than `//> using publish.*` directives on purpose:
# those directives are experimental, and having them anywhere in the project makes even
# plain `scala-cli test .` demand `--power`.
#
# One-time setup on the publishing machine (nothing secret is stored in this repo):
#   scala-cli --power config --create-pgp-key --pgp-password none --email <email>
#   scala-cli --power pgp push <public-key.asc>
#   scala-cli --power config publish.credentials ossrh-staging-api.central.sonatype.com env:SONATYPE_USER env:SONATYPE_PASSWORD
# and export SONATYPE_USER / SONATYPE_PASSWORD (a Central Portal user token) before publishing.
set -euo pipefail
cd "$(dirname "$0")"

args=(
  --organization io.github.smksyj
  --name stol
  --compute-version git:tag
  --license MIT
  --url https://github.com/smksyj/scala-type-operator-library
  --vcs github:smksyj/scala-type-operator-library
  --developer "smksyj|smksyj|https://github.com/smksyj"
)

if [ "${1:-}" = "local" ]; then
  shift
  exec scala-cli --power publish local . "${args[@]}" "$@"
else
  exec scala-cli --power publish . "${args[@]}" --publish-repository central "$@"
fi
