# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

import subprocess as sp
import os
import sys
import json

TEST_DIR = "src/test/resources/apilint_test/"

ERROR_CODES = {
    'NO_CHANGE': 0,
    'API_CHANGE': 10,
    'INCOMPATIBLE': 131,
}

with open(TEST_DIR + "tests.py") as f:
    tests = json.load(f)

for t in tests:
    print("Running {}, expected {}:".format(t["test"], t["expected"]))
    test_base = TEST_DIR + t["test"]

    before_api = test_base + ".before.txt"
    after_api = test_base + ".after.txt"

    sp.call(["diff", "-U5",
             before_api, "--label", "before.txt",
             after_api, "--label", "after.txt"])

    print("")

    test = ["python", "src/main/resources/apilint.py",
            after_api, before_api, "--show-noticed"]
    error_code = sp.call(test)

    expected_error_code = ERROR_CODES[t["expected"]]
    if error_code != expected_error_code:
         print("The following test is expected to fail with {} "
               "but the error code is {} ".format(expected_error_code,
                                                  error_code))
         print(" ".join(test))
         sys.exit(1)
