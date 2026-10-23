// Publishing metadata for `scala-cli --power publish`.  Public metadata only.
//
// Credentials and the signing key live in scala-cli's user config, not here:
//   scala-cli --power config --create-pgp-key --pgp-password random
//   scala-cli --power config publish.credentials central.sonatype.com env:SONATYPE_USER env:SONATYPE_PASSWORD
// (SONATYPE_USER/PASSWORD = a Central Portal user token from central.sonatype.com)
//
// Rehearse on this machine only:   scala-cli --power publish local .
// Publish for real (after `git tag v0.1.0`):  scala-cli --power publish .

//> using publish.organization io.github.smksyj
//> using publish.name stol
//> using publish.computeVersion git:tag
//> using publish.license MIT
//> using publish.url https://github.com/smksyj/scala-type-operator-library
//> using publish.vcs github:smksyj/scala-type-operator-library
//> using publish.developer "smksyj|smksyj|https://github.com/smksyj"
//> using publish.repository central
