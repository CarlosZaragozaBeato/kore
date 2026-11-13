import unittest

from suunto_ingest.decode import semicircles_to_degrees


class SuuntoDecodeTest(unittest.TestCase):
    def test_fit_semicircle_coordinates_are_degrees(self):
        self.assertEqual(semicircles_to_degrees(2 ** 30), 90.0)
        self.assertEqual(semicircles_to_degrees(-(2 ** 31)), -180.0)
        self.assertIsNone(semicircles_to_degrees(None))


if __name__ == "__main__":
    unittest.main()
