# API Lint Gradle Plugin

Tracks the API of an android library and helps maintaining backwards compatibility.

## Tasks

This gradle plugin adds the following tasks for each variant:

- `apiLint${variantName}`
    This task will check that the current API is backwards compatible with the 
    api specified in a file (by default called 'api.txt').  This task also
    checks for various lints as specified in //apilint/src/main/resources/apilint.py.

- `apiDiff${variantName}`
    This task will print a diff between the current API and the local API 

- `apiGenerate${variantName}`
    This task will generate the current API file and store it in the `build/`
    folder

- `apiLintHelp${variantName}`
    This task will print an help text whenever an API change is detected

- `apiUpdateFile${variantName}`
    This task will update the current API file from the local API file whenever
    an API change is ready.

## Usage

Just add the following to your project:

```
dependencies {
    classpath 'org.mozilla.android:apilint:0.1'
}

apply plugin: 'org.mozilla.apilint'
apiLint.packageFilter = 'org.your.package.api'
```
