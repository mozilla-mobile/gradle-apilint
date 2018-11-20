# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

import os
import unittest
import subprocess as sp

FOLDER = 'src/test/resources/changelog-check-test'

MISSING_VERSION_CODE = 11
OUT_OF_DATE_CODE = 10
OK_CODE = 0

class ChangelogCheckTest(unittest.TestCase):
    def t(self, changelog, api, expected):
        test = ["python", "src/main/resources/changelog-check.py",
                "--changelog-file", "{}/{}".format(FOLDER, changelog),
                "--api-file", "{}/{}".format(FOLDER, api)]
        with open(os.devnull, 'w') as devnull:
            code = sp.call(test, stdout=devnull)
        self.assertEqual(code, expected)

    def test_changelogWithRightVersionNoError(self):
        self.t("changelog-with-right-version.md", "api-changelog.txt", OK_CODE)

    def test_changelogMissingVersionError(self):
        self.t("changelog-without-version.md", "api-changelog.txt", MISSING_VERSION_CODE)

    def test_changelogWrongVersionError(self):
        self.t("changelog-with-wrong-version.md", "api-changelog.txt", OUT_OF_DATE_CODE)

if __name__ == "__main__":
    unittest.main()
