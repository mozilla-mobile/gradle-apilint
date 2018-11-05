# API Lint Gradle Plugin
Tracks the API of an android library and helps maintaining backwards
compatibility.

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

## Usage
Add the following to your project:

```
buildscript {
    dependencies {
        classpath 'org.mozilla.apilint:apilint:0.1'
    }
}

apply plugin: 'org.mozilla.apilint'
apiLint.packageFilter = 'org.your.package.api'
```

Then run
```
$ ./gradlew apiUpdateFile$VARIANT_NAME
```

Where `$VARIANT_NAME` is a variant for your library. This will create an API
file that contains a description of your library's API, by default this file
will be called `api.txt`.

After you make changes to your code, run:

```
$ ./gradlew apiLint$VARIANT_NAME
```

to check that the API of your library did not change. You can always run
`apiUpdateFile$VARIANT_NAME` to update the API file (e.g. when adding new
APIs).
