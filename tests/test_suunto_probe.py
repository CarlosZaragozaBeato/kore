import json
import os
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest
from unittest.mock import patch

from suunto_ingest import probe


class SuuntoProbeTest(unittest.TestCase):
    def test_rotated_token_is_saved_before_workout_request_and_reused(self):
        with TemporaryDirectory() as temp:
            root = Path(temp)
            (root / ".env").write_text(
                "SUUNTO_CLIENT_ID=client\n"
                "SUUNTO_CLIENT_SECRET=secret|,?\n"
                "SUUNTO_SUBSCRIPTION_KEY=subscription\n"
                "SUUNTO_REFRESH_TOKEN=initial\n",
                encoding="utf-8",
            )
            token_path = root / "data" / "secrets" / "suunto-token.json"
            calls = []

            def fake_request(request):
                if request.full_url == probe.OAUTH_URL:
                    body = request.data.decode()
                    refresh_number = calls.count("refresh") + 1
                    expected = "initial" if refresh_number == 1 else "rotated-1"
                    self.assertIn(f"refresh_token={expected}", body)
                    calls.append("refresh")
                    return json.dumps({
                        "access_token": "access",
                        "refresh_token": f"rotated-{refresh_number}",
                    }).encode()
                self.assertEqual(json.loads(token_path.read_text())["refresh_token"], f"rotated-{calls.count('refresh')}")
                calls.append("workouts")
                return b'{"payload": [{"workoutKey": "example"}]}'

            with patch.object(probe, "request_bytes", side_effect=fake_request):
                first_count, first_path = probe.run(30, 1, root)
                second_count, second_path = probe.run(30, 1, root)

            self.assertEqual((first_count, second_count), (1, 1))
            self.assertNotEqual(first_path, second_path)
            self.assertEqual(calls, ["refresh", "workouts", "refresh", "workouts"])
            self.assertEqual(json.loads(token_path.read_text())["refresh_token"], "rotated-2")
            self.assertEqual(first_path.read_bytes(), b'{"payload": [{"workoutKey": "example"}]}')
            self.assertEqual(json.loads(first_path.with_suffix(".meta.json").read_text())["count"], 1)
            self.assertEqual(os.stat(root / "data").st_mode & 0o777, 0o700)
            self.assertEqual(os.stat(token_path).st_mode & 0o777, 0o600)
            self.assertEqual(os.stat(first_path).st_mode & 0o777, 0o600)

    def test_invalid_workout_response_does_not_create_raw_file_but_preserves_rotated_token(self):
        with TemporaryDirectory() as temp:
            root = Path(temp)
            (root / ".env").write_text(
                "SUUNTO_CLIENT_ID=client\nSUUNTO_CLIENT_SECRET=secret\n"
                "SUUNTO_SUBSCRIPTION_KEY=subscription\nSUUNTO_REFRESH_TOKEN=initial\n",
                encoding="utf-8",
            )
            with patch.object(probe, "request_bytes", side_effect=[
                b'{"access_token":"access","refresh_token":"rotated"}',
                b'{"unexpected":[]}',
            ]):
                with self.assertRaisesRegex(probe.ProbeError, "payload"):
                    probe.run(30, 1, root)
            self.assertEqual(json.loads((root / "data/secrets/suunto-token.json").read_text())["refresh_token"], "rotated")
            self.assertFalse((root / "data/raw").exists())


if __name__ == "__main__":
    unittest.main()
