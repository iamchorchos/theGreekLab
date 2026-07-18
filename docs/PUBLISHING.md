# Publishing to Maven Central

TheGreekLab is published through the Sonatype Central Portal with the Maven
coordinates `io.github.iamchorchos:thegreeklab`. Publishing is restricted to
semantic-version tags that point to commits reachable from `main`.

## One-time maintainer setup

1. Sign in to [Central Portal](https://central.sonatype.com/) with the GitHub
   account `iamchorchos` and confirm that the `io.github.iamchorchos` namespace
   is verified.
2. Generate a Central Portal user token.
3. Generate a passphrase-protected OpenPGP signing key and publish its public
   key to a key server supported by Maven Central.
4. Add the following GitHub Actions repository secrets:

   - `MAVEN_CENTRAL_USERNAME`: username from the Central Portal user token,
   - `MAVEN_CENTRAL_PASSWORD`: password from the Central Portal user token,
   - `MAVEN_GPG_PRIVATE_KEY`: ASCII-armored private signing key,
   - `MAVEN_GPG_PASSPHRASE`: passphrase protecting that private key.

Never commit Portal tokens, private keys, passphrases or a generated Maven
`settings.xml` file.

## Release process

Prepare and verify the release on `main`, then create and push an annotated
semantic-version tag:

```bash
git tag -a v2.0.1 -m "Release v2.0.1"
git push origin v2.0.1
```

The release workflow validates the tag, builds the Linux native library, runs
the complete Maven verification lifecycle, creates the source and Javadoc
JARs, signs every required artifact, and publishes them through the Central
Portal. It waits until Central reports the deployment as published before it
creates or updates the corresponding GitHub Release and checksums.
Before uploading, the workflow also verifies that the flattened consumer POM
contains the literal version derived from the release tag.

Maven Central releases are immutable. Never reuse a version number or move a
published release tag. If publishing fails before completion, inspect the
Central Portal validation report before creating a corrected release with a
new version.

## Local packaging check

The Central profile can be loaded and the release artifacts can be assembled
without uploading or requiring signing credentials:

```bash
./mvnw -Pcentral-release -Drevision=2.0.1 -Dgpg.skip=true \
  -Dcentral.skipPublishing=true clean verify
```

The `deploy` phase additionally requires a `central` server entry in Maven
`settings.xml`, even when upload is skipped. This local command therefore does
not replace the signed release workflow.
