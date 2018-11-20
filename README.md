# API Lint Gradle Plugin
Tracks the API of an Android library and helps maintain backward compatibility.

## Tasks

This gradle plugin adds the following tasks for each variant:

- `apiLint${variantName}`
    Checks that the current API is backwards compatible with the api specified
    in a file, by default located in `//api.txt`. This task also checks for
    various lints as specified in [apilint.py](apilint/src/main/resources/apilint.py).

- `apiGenerate${variantName}`
    Generates the current API file and stores it in the `build/` folder

- `apiUpdateFile${variantName}`
    Updates the API file from the local state of the project

- `apiDiff${variantName}`
    Prints a diff between the current API and the local API

- `apiLintHelp${variantName}`
    Prints an help text whenever an API change is detected

- `apiChangelogCheck${variantName}`
    Only if apiLint.changelogFileName is set. Checks that the changelog file is
    updated every time the api.txt file changes.

## Usage
Add the following to your project's `build.gradle`:

```
buildscript {
    repositories {
        maven {
            url "https://plugins.gradle.org/m2/"
        }
    }
    dependencies {
        classpath 'org.mozilla.apilint:apilint:0.1.3'
    }
}

apply plugin: 'org.mozilla.apilint'
apiLint.packageFilter = 'org.your.package.api'
```

And make sure that the `apply plugin` line appears after the
`com.android.library` plugin has been applied.

Then run
```
$ ./gradlew apiUpdateFile${variantName}
```

Where `${variantName}` is a variant for your library. This will create an API
file that contains a description of your library's API, by default this file
will be called `api.txt`.

After you make changes to your code, run:

```
$ ./gradlew apiLint${variantName}
```

to check that the API of your library did not change. You can always run
`apiUpdateFile${variantName}` to update the API file (e.g. when adding new
APIs).

### Changelog

Optionally, `apilint` can track your API changelog file and fail the build if
the changelog file has not been updated after an API change. To enable this
feature, add the following to your `build.gradle`:

```
apiLint.changelogFileName = 'CHANGELOG.md'
```

And then add the following line somewhere in your changelog file:

```
[api-version]: <api-txt-sha1>
```

where `<api-txt-sha1>` is the SHA1 of the current api file. You can obtain this
value by running:

```
./gradlew apiChangelogCheck${variantName}
```

Now, whenever `./gradlew apiLint${variantName}` is invoked, it will check that
the changelog file version matches the generated api file version.
