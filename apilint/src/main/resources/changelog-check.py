#!/usr/bin/env python

# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

import argparse
import hashlib
import re
import sys

API_VERSION_REGEX = re.compile('^\[api-version\]: ([a-f0-9]{40})$')

class MissingApiVersionError(Exception):
    pass

def findApiVersion(changelog):
    for l in changelog:
        match = API_VERSION_REGEX.match(l)
        if match:
            return match.group(1)

    raise MissingApiVersionError

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Checks that the changelog file has been updated.")
    parser.add_argument("--api-file", type=argparse.FileType('rb'), help="Updated API file.")
    parser.add_argument("--changelog-file", type=argparse.FileType('r'), help="Changelog file of the API.")
    args = parser.parse_args()

    sha1 = hashlib.sha1()
    sha1.update(args.api_file.read())

    currentApiVersion = sha1.hexdigest()

    try:
        expectedApiVersion = findApiVersion(args.changelog_file)
    except MissingApiVersionError:
        print("ERROR: The api changelog file does not have a version pin. Please update")
        print("the file at")
        print("")
        print(args.changelog_file.name)
        print("")
        print("And add the following line:")
        print("")
        print(">>>>")
        print("[api-version]: {}".format(currentApiVersion))
        print("<<<<")
        sys.exit(11)

    if currentApiVersion != expectedApiVersion:
        print("ERROR: The api changelog file is out of date. Please update the file at")
        print("")
        print(args.changelog_file.name)
        print("")
        print("and then modify the [api-version] line as following:")
        print("")
        print(">>>>")
        print("[api-version]: {}".format(currentApiVersion))
        print("<<<<")
        sys.exit(10)
