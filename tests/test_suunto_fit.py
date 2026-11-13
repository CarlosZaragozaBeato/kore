import os
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest
from unittest.mock import MagicMock, patch

from suunto_ingest import fit


def sample_fit() -> bytes:
    data = bytearray(16)
    data[0] = 14
    data[8:12] = b".FIT"
    return bytes(data)


class SuuntoFitTest(unittest.TestCase):
    def test_saved_file_is_reused_after_interruption(self):
        with TemporaryDirectory() as temp:
            root = Path(temp)
            (root / ".env").write_text(
                "SUUNTO_CLIENT_ID=client\nSUUNTO_CLIENT_SECRET=secret\n"
                "SUUNTO_SUBSCRIPTION_KEY=subscription\nSUUNTO_REFRESH_TOKEN=initial\n",
                encoding="utf-8",
            )
            connection = MagicMock()
            connection.__enter__.return_value = connection
            connection.execute.return_value.fetchone.return_value = (True,)
            key = "a" * 24
            fetched = []

            def fetcher(*_args):
                fetched.append(1)
                return sample_fit()

            with patch.object(fit.psycopg, "connect", return_value=connection), \
                 patch.object(fit, "ensure_fit_columns"), \
                 patch.object(fit, "pending_keys", return_value=[key]), \
                 patch.object(fit, "renew_token", return_value="access"), \
                 patch.object(fit, "mark_fit") as mark, \
                 patch.object(fit.time, "sleep"):
                self.assertEqual(fit.run(1, root, fetcher), (1, 0))
                self.assertEqual(fit.run(1, root, fetcher), (1, 0))

            self.assertEqual(len(fetched), 1)
            self.assertEqual(mark.call_count, 2)
            path = root / "data/raw/suunto/fit" / f"{key}.fit"
            self.assertEqual(path.read_bytes(), sample_fit())
            self.assertEqual(os.stat(path).st_mode & 0o777, 0o600)

    def test_truncated_fit_is_rejected(self):
        with self.assertRaisesRegex(fit.ProbeError, "truncated"):
            data = bytearray(sample_fit())
            data[4:8] = (100).to_bytes(4, "little")
            fit.validate_fit(bytes(data))

    def test_unauthorized_fit_refreshes_once_and_retries_same_workout(self):
        with TemporaryDirectory() as temp:
            root = Path(temp)
            (root / ".env").write_text(
                "SUUNTO_CLIENT_ID=client\nSUUNTO_CLIENT_SECRET=secret\n"
                "SUUNTO_SUBSCRIPTION_KEY=subscription\nSUUNTO_REFRESH_TOKEN=initial\n",
                encoding="utf-8",
            )
            connection = MagicMock()
            connection.__enter__.return_value = connection
            connection.execute.return_value.fetchone.return_value = (True,)
            seen_tokens = []

            def fetcher(token, _subscription, _key):
                seen_tokens.append(token)
                if token == "expired":
                    raise fit.UnauthorizedFit("unauthorized")
                return sample_fit()

            with patch.object(fit.psycopg, "connect", return_value=connection), \
                 patch.object(fit, "ensure_fit_columns"), \
                 patch.object(fit, "pending_keys", return_value=["b" * 24]), \
                 patch.object(fit, "renew_token", side_effect=["expired", "fresh"]) as renew, \
                 patch.object(fit, "mark_fit") as mark, \
                 patch.object(fit.time, "sleep") as sleep:
                self.assertEqual(fit.run(1, root, fetcher), (1, 0))

            self.assertEqual(seen_tokens, ["expired", "fresh"])
            self.assertEqual(renew.call_count, 2)
            sleep.assert_called_once_with(fit.UNAUTHORIZED_COOLDOWN_SECONDS)
            mark.assert_called_once()


if __name__ == "__main__":
    unittest.main()
