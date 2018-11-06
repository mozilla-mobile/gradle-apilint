# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

import argparse
import subprocess as sp
import sys

# Run the doclet on the test package and verify that the output matches the
# expected output.

parser = argparse.ArgumentParser()
parser.add_argument("--javadoc")
parser.add_argument("--doclet-jar")
parser.add_argument("--java-root")
parser.add_argument("--out-dir")
parser.add_argument("--expected")
args = parser.parse_args()

output = args.out_dir + "/api.txt"

sp.check_call([args.javadoc,
    "-doclet", "org.mozilla.doclet.ApiDoclet",
    "-docletpath", args.doclet_jar,
    "-subpackages", "org.mozilla.test",
    "-sourcepath", args.java_root,
    "-output", output
])

result = sp.call(["diff", '-U5',
    '--label', 'Expected', args.expected,
    '--label', 'Actual', output])

# result == 0 from `diff` means that the files are identical
if result != 0:
    print("")
    print("ERROR: Doclet output differs from expected.")
    sys.exit(1)
