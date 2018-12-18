# API Lint Gradle Plugin
Tracks the API of an Android library and helps maintain backward compatibility.

## Tasks
The apilint plugin provies the following tasks.

<code><b>apiLint<i>VariantName</i></b></code> Checks that the current API is
backward compatible with the API specified in a file, by default located in
`//api.txt`. This task also checks for various lints as specified in
[apilint.py](apilint/src/main/resources/apilint.py).

<code><b>apiUpdateFile<i>VariantName</i></b></code> Updates the API file from
the local state of the project

<code><b>apiChangelogCheck<i>VariantName</i></b></code> Only if
`apiLint.changelogFileName` is set. Checks that the changelog file is updated
every time the api.txt file changes.

## Usage
##### build.gradle
```gradle
buildscript {
    repositories {
        maven {
            url "https://plugins.gradle.org/m2/"
        }
    }
    dependencies {
        classpath 'org.mozilla.apilint:apilint:$apilintVersion'
    }
}

apply plugin: 'org.mozilla.apilint'
apiLint.packageFilter = 'org.your.package.api'
```

Make sure that the `apply plugin` line appears after the `com.android.library`
plugin has been applied.

Then run
<pre>
$ ./gradlew apiUpdateFile<i>VariantName</i>
</pre>

This will create an API file that contains a description of your library's API,
by default this file will be called `api.txt`.

After you make changes to your code, run:

<pre>
$ ./gradlew apiLint<i>VariantName</i>
</pre>

to check that the API of your library did not change. You can always run
<code>apiUpdateFile<i>VariantName</i></code> to update the API file (e.g. when
adding new APIs).

### Dependencies

The apilint plugin adds the following dependencies.

| Task Name        | Depends on                                 |
| ---------------- |:------------------------------------------:|
| `check`          | <code>apiLint<i>VariantName</i></code>     |

### Changelog

Optionally, `apilint` can track your API changelog file and fail the build if
the changelog file has not been updated after an API change. To enable this
feature, add the following:

##### build.gradle
```gradle
apiLint.changelogFileName = 'CHANGELOG.md'
```

And then add the following line somewhere in your changelog file:

##### CHANGELOG.md
```markdown
[api-version]: <api-txt-sha1>
```

where `<api-txt-sha1>` is the SHA1 of the current api file. You can obtain this
value by running:

<pre>
$ ./gradlew apiChangelogCheck<i>VariantName</i>
</pre>

Now, whenever <code>./gradlew apiLint<i>VariantName</i></code> is invoked, it
will check that the changelog file version matches the generated api file
version.

### Configuration

The apilint plugin can be configured using the gradle extension `apiLint`.
Default values are as follows

##### build.gradle
```gradle
apiLint {
    packageFilter = '.'
    apiOutputFileName = 'api.txt'
    currentApiRelativeFilePath = 'api.txt'
    jsonResultFileName = 'apilint-result.json'
    changelogFileName = null
    lintFilters = null
}
```

<code><b>packageFilter</b></code> Filters packages that make up the api. By
default all packages are included in the `api.txt` file.

<code><b>apiOutputFileName</b></code> Relative path to the `api.txt` file in
the source folder.

<code><b>currentApiRelativeFilePath</b></code> Relative path of the generated
`api.txt` file in the build folder.

<code><b>jsonResultFileName</b></code> Relative path to the JSON file name that
contains the result of apilint.

<code><b>changelogFileName</b></code> Relative path to the changelog file,
optional. See also [Changelog](#changelog).

<code><b>lintFilters</b></code> List of lints that fail the build, by default
all lints can fail the build. Filters will match any error code that starts
with the string specified, e.g. `GV` will match `GV1`, `GV2`, ...
