package de.pritcloud.appstow;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.util.Base64;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class BackupCryptoTest {

    @Test
    public void encryptedEnvelopeRoundTripsWithoutPlaintext()
            throws Exception {

        JSONObject payload =
                new JSONObject()
                        .put(
                                "format",
                                "appstow-backup")
                        .put(
                                "formatVersion",
                                3)
                        .put(
                                "secretValue",
                                "private-category-name");

        JSONObject envelope =
                BackupCrypto.encrypt(
                        payload);

        assertEquals(
                3,
                envelope.getInt(
                        "formatVersion"));

        assertFalse(
                envelope.toString()
                        .contains(
                                "private-category-name"));

        JSONObject restored =
                BackupCrypto.decrypt(
                        envelope);

        assertEquals(
                "private-category-name",
                restored.getString(
                        "secretValue"));
    }

    @Test
    public void changedCiphertextIsRejected()
            throws Exception {

        JSONObject payload =
                new JSONObject()
                        .put(
                                "format",
                                "appstow-backup")
                        .put(
                                "formatVersion",
                                3)
                        .put(
                                "value",
                                "unchanged");

        JSONObject envelope =
                BackupCrypto.encrypt(
                        payload);

        byte[] ciphertext =
                Base64.getDecoder()
                        .decode(
                                envelope.getString(
                                        "ciphertext"));

        ciphertext[0] =
                (byte) (ciphertext[0] ^ 1);

        envelope.put(
                "ciphertext",
                Base64.getEncoder()
                        .encodeToString(
                                ciphertext));

        try {
            BackupCrypto.decrypt(
                    envelope);

            fail(
                    "Manipuliertes Backup wurde akzeptiert.");

        } catch (IOException expected) {
            // AES-GCM muss jede Änderung erkennen.
        }
    }

    @Test
    public void versionTwoEnvelopeIsRejected()
            throws Exception {

        JSONObject envelope =
                new JSONObject()
                        .put(
                                "format",
                                "appstow-backup")
                        .put(
                                "formatVersion",
                                2);

        try {
            BackupCrypto.decrypt(
                    envelope);

            fail(
                    "Backup v2 wurde akzeptiert.");

        } catch (JSONException expected) {
            // AppStow 3.0 akzeptiert bewusst keine v2-Sicherung.
        }
    }
}
