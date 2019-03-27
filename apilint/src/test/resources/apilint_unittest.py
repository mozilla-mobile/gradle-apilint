import unittest
import sys
import os

sys.path.append(os.path.join(os.path.dirname(__file__), '../../main/resources/'))

from apilint import collect_chunks
from apilint import Type

class ApilintUnittest(unittest.TestCase):
    def collect_chunks(self, search, separator, expected):
        self.assertEqual(collect_chunks(search, separator, len(separator)),
                         expected)

    def test_collect_chunks(self):
        self.collect_chunks("T extends F", "extends", ["T", "F"])
        self.collect_chunks("java.lang.Map<T extends F, K>", "extends", ["java.lang.Map<T extends F, K>"])
        self.collect_chunks("java.lang.Map<T extends F, H extends G>", "extends", ["java.lang.Map<T extends F, H extends G>"])
        self.collect_chunks("public <T>", " ", ["public", "<T>"])

    def test_type_extends(self):
        typ = Type(None, None, "java.lang.Map<T extends F>", None, None)

        self.assertEqual(typ.name, "java.lang.Map")

        self.assertEqual(len(typ.generics), 1)
        self.assertEqual(typ.generics[0].name, "T")

        self.assertEqual(len(typ.generics[0].extends), 1)
        self.assertEqual(typ.generics[0].extends[0].name, "F")

    def test_type_extends_multiple(self):
        typ = Type(None, None, "java.lang.Map<T extends a.b.F & a.d.G>", None, None)

        self.assertEqual(len(typ.generics), 1)

        self.assertEqual(len(typ.generics[0].extends), 2)
        self.assertEqual(typ.generics[0].extends[0].name, "a.b.F")
        self.assertEqual(typ.generics[0].extends[1].name, "a.d.G")

    def test_type_nested_generic_extends(self):
        typ = Type(None, None, "java.lang.Map<T extends F, H extends G>", None, None)
        self.assertEqual(typ.name, "java.lang.Map")

        self.assertEqual(len(typ.generics), 2)

        self.assertEqual(typ.generics[0].name, "T")
        self.assertEqual(len(typ.generics[0].extends), 1)
        self.assertEqual(typ.generics[0].extends[0].name, "F")

        self.assertEqual(typ.generics[1].name, "H")
        self.assertEqual(len(typ.generics[1].extends), 1)
        self.assertEqual(typ.generics[1].extends[0].name, "G")

    def test_type_nested_generic(self):
        typ = Type(None, None, "A<B<C<D,F>, C<G,H>>>", None, None)

        self.assertEqual(typ.name, "A")

        self.assertEqual(len(typ.generics), 1)
        self.assertEqual(typ.generics[0].name, "B")

        self.assertEqual(len(typ.generics[0].generics), 2)
        self.assertEqual(typ.generics[0].generics[0].name, "C")
        self.assertEqual(typ.generics[0].generics[1].name, "C")

        self.assertEqual(len(typ.generics[0].generics[0].generics), 2)
        self.assertEqual(typ.generics[0].generics[0].generics[0].name, "D")
        self.assertEqual(typ.generics[0].generics[0].generics[1].name, "F")

        self.assertEqual(len(typ.generics[0].generics[1].generics), 2)
        self.assertEqual(typ.generics[0].generics[1].generics[0].name, "G")
        self.assertEqual(typ.generics[0].generics[1].generics[1].name, "H")

if __name__ == '__main__':
    unittest.main()
