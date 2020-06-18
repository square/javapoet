Releasing
=========

1. Update the CHANGELOG.md for the impending release.
2. Update the README.md with the new version.
3. `git commit -am "Update changelog for X.Y.Z."` (where X.Y.Z is the new version).
4. `mvn-release`.
    * `What is the release version for "JavaPoet"? (com.squareup.javapoet) X.Y.Z:` - hit Enter.
    * `What is SCM release tag or label for "JavaPoet"? (com.squareup.javapoet) javapoet-X.Y.Z:` - hit Enter.
    * `What is the new development version for "JavaPoet"? (com.squareup.javapoet) X.Y.(Z + 1)-SNAPSHOT:` - enter `X.(Y + 1).0-SNAPSHOT`.
    * Enter your GPG Passphrase when prompted.
5. Visit Sonatype Nexus and promote the artifact.

If step 4 or 5 fails:

  * Drop the Sonatype repo, 
  * Fix the problem,
  * Manully revert the version change in `pom.xml` made by `mvn-release`,
  * Commit,
  * And start again at step 4.

Prerequisites
-------------

In `~/.m2/settings.xml`, set the following:

```xml
<settings>
  <servers>
    <server>
      <id>sonatype-nexus-staging</id>
      <username>your-nexus-username</username>
      <password>your-nexus-password</password>
    </server>
  </servers>
</settings>
```

In your shell's `.rc` file, set the following:

```
alias mvn-release='mvn clean source:jar javadoc:jar verify && mvn clean release:clean && mvn release:prepare release:perform'
```

Refer to the [GPG Keys][gpg_keys] guide if you need to set up GPG keys for signing.

 [gpg_keys]: https://square.github.io/okio/releasing/#prerequisite-gpg-keys
