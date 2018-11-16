# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

import argparse
import json
import os
import subprocess as sp
import sys

TEST_DIR = "src/test/resources/apilint_test/"

ERROR_CODES = {
    'NO_CHANGE': 0,
    'API_CHANGE': 10,
    'INCOMPATIBLE': 131,
}

parser = argparse.ArgumentParser(description="Tests for apilint.py.")
parser.add_argument("--build-dir", help="Build directory location")
args = parser.parse_args()

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

    json_file = "{}/{}-result.json".format(args.build_dir, t["test"])
    test = ["python", "src/main/resources/apilint.py",
            "--result-json", json_file,
            after_api, before_api, "--show-noticed"]

    error_code = sp.call(test)

    expected_error_code = ERROR_CODES[t["expected"]]
    if error_code != expected_error_code:
         print("The following test is expected to fail with {} "
               "but the error code is {} ".format(expected_error_code,
                                                  error_code))
         print(" ".join(test))
         sys.exit(1)

    with open(json_file) as f:
        json_result = json.load(f)

    print(json.dumps(json_result, indent=2))

    assert len(json_result['failures']) == 0

    if t['expected'] == 'INCOMPATIBLE':
        assert len(json_result['compat_failures']) == 1
    else:
        assert len(json_result['compat_failures']) == 0

    if t['expected'] == 'API_CHANGE':
        assert len(json_result['api_changes']) > 0

    if t['expected'] == 'NO_CHANGE':
        assert len(json_result['api_changes']) == 0
        assert json_result['failure'] == False
