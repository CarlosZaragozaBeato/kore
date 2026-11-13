from pathlib import Path
from tempfile import TemporaryDirectory
import unittest
from unittest.mock import MagicMock, patch

from suunto_ingest import sync


class SuuntoSyncTest(unittest.TestCase):
    def test_failed_second_page_does_not_advance_cursor(self):
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
            offsets = []

            def fetcher(_access, _key, _since, _until, _limit, offset):
                offsets.append(offset)
                if offset == 0:
                    return b'{"payload":[{"workoutKey":"one"},{"workoutKey":"two"}]}'
                raise sync.ProbeError("simulated page failure")

            with patch.object(sync.psycopg, "connect", return_value=connection), \
                 patch.object(sync, "ensure_schema"), \
                 patch.object(sync, "read_cursor", return_value=0), \
                 patch.object(sync, "renew_token", return_value="access"), \
                 patch.object(sync, "store_page") as store, \
                 patch.object(sync, "advance_cursor") as advance:
                with self.assertRaisesRegex(sync.ProbeError, "simulated"):
                    sync.run(2, root, fetcher)

            self.assertEqual(offsets, [0, 2])
            store.assert_called_once()
            advance.assert_not_called()

    def test_repeated_page_aborts_without_advancing_cursor(self):
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
            raw = b'{"payload":[{"workoutKey":"one"}]}'

            with patch.object(sync.psycopg, "connect", return_value=connection), \
                 patch.object(sync, "ensure_schema"), \
                 patch.object(sync, "read_cursor", return_value=0), \
                 patch.object(sync, "renew_token", return_value="access"), \
                 patch.object(sync, "store_page") as store, \
                 patch.object(sync, "advance_cursor") as advance:
                with self.assertRaisesRegex(sync.ProbeError, "same page twice"):
                    sync.run(1, root, lambda *_: raw)

            store.assert_called_once()
            advance.assert_not_called()


if __name__ == "__main__":
    unittest.main()
