package de.pritcloud.appstow;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

final class BackupCrypto {

    static final String FORMAT_ID =
            "appstow-backup";

    static final int FORMAT_VERSION = 3;

    private static final String ENCRYPTION =
            "AES-256-GCM";

    private static final String KDF =
            "PBKDF2-HMAC-SHA256";

    private static final int KDF_ITERATIONS =
            150_000;

    private static final int KEY_BITS =
            256;

    private static final int SALT_BYTES =
            16;

    private static final int IV_BYTES =
            12;

    private static final int GCM_TAG_BITS =
            128;

    /*
     * AppStow bleibt bewusst lokal und Open Source.
     * Dieser Wert ist daher kein unüberwindbares Geheimnis.
     * Er erschwert zusammen mit zufälligem Salt und AES-GCM
     * das manuelle Lesen/Ändern einer Sicherung und sorgt
     * für eine Integritätsprüfung.
     */
    private static final char[] LOCAL_PROTECTION_SECRET =
            ("AppStow|BackupV3|LocalProtection|"
                    + "de.pritcloud.appstow")
                    .toCharArray();

    private static final byte[] AAD =
            ("AppStowBackup|3|AES-256-GCM|"
                    + "PBKDF2-HMAC-SHA256|150000")
                    .getBytes(
                            StandardCharsets.UTF_8);

    private BackupCrypto() {
    }

    static JSONObject encrypt(
            JSONObject payload)
            throws IOException, JSONException {

        if (payload == null) {
            throw new IllegalArgumentException(
                    "Backup payload is required.");
        }

        byte[] salt =
                new byte[SALT_BYTES];

        byte[] iv =
                new byte[IV_BYTES];

        SecureRandom random =
                new SecureRandom();

        random.nextBytes(
                salt);

        random.nextBytes(
                iv);

        byte[] plaintext =
                payload.toString()
                        .getBytes(
                                StandardCharsets.UTF_8);

        try {
            SecretKeySpec key =
                    deriveKey(
                            salt);

            Cipher cipher =
                    Cipher.getInstance(
                            "AES/GCM/NoPadding");

            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    key,
                    new GCMParameterSpec(
                            GCM_TAG_BITS,
                            iv));

            cipher.updateAAD(
                    AAD);

            byte[] ciphertext =
                    cipher.doFinal(
                            plaintext);

            return new JSONObject()
                    .put(
                            "format",
                            FORMAT_ID)
                    .put(
                            "formatVersion",
                            FORMAT_VERSION)
                    .put(
                            "encryption",
                            ENCRYPTION)
                    .put(
                            "kdf",
                            KDF)
                    .put(
                            "iterations",
                            KDF_ITERATIONS)
                    .put(
                            "salt",
                            encode(
                                    salt))
                    .put(
                            "iv",
                            encode(
                                    iv))
                    .put(
                            "ciphertext",
                            encode(
                                    ciphertext));

        } catch (GeneralSecurityException exception) {

            throw new IOException(
                    "Backup konnte nicht verschlüsselt werden.",
                    exception);
        }
    }

    static JSONObject decrypt(
            JSONObject envelope)
            throws IOException, JSONException {

        validateEnvelope(
                envelope);

        byte[] salt =
                decode(
                        envelope.getString(
                                "salt"));

        byte[] iv =
                decode(
                        envelope.getString(
                                "iv"));

        byte[] ciphertext =
                decode(
                        envelope.getString(
                                "ciphertext"));

        if (salt.length
                != SALT_BYTES
                || iv.length
                != IV_BYTES
                || ciphertext.length
                <= GCM_TAG_BITS / 8) {

            throw new JSONException(
                    "Ungültige Verschlüsselungsdaten im Backup.");
        }

        try {
            SecretKeySpec key =
                    deriveKey(
                            salt);

            Cipher cipher =
                    Cipher.getInstance(
                            "AES/GCM/NoPadding");

            cipher.init(
                    Cipher.DECRYPT_MODE,
                    key,
                    new GCMParameterSpec(
                            GCM_TAG_BITS,
                            iv));

            cipher.updateAAD(
                    AAD);

            byte[] plaintext =
                    cipher.doFinal(
                            ciphertext);

            return new JSONObject(
                    new String(
                            plaintext,
                            StandardCharsets.UTF_8));

        } catch (GeneralSecurityException exception) {

            throw new IOException(
                    "Backup ist beschädigt oder wurde verändert.",
                    exception);
        }
    }

    private static void validateEnvelope(
            JSONObject envelope)
            throws JSONException {

        if (envelope == null
                || !FORMAT_ID.equals(
                        envelope.optString(
                                "format",
                                ""))) {

            throw new JSONException(
                    "Unbekanntes Backup-Format.");
        }

        if (envelope.optInt(
                "formatVersion",
                -1) != FORMAT_VERSION) {

            throw new JSONException(
                    "Diese Backup-Version wird nicht unterstützt.");
        }

        if (!ENCRYPTION.equals(
                envelope.optString(
                        "encryption",
                        ""))
                || !KDF.equals(
                        envelope.optString(
                                "kdf",
                                ""))
                || envelope.optInt(
                        "iterations",
                        -1)
                != KDF_ITERATIONS) {

            throw new JSONException(
                    "Ungültige Verschlüsselungsparameter im Backup.");
        }

        if (!envelope.has(
                "salt")
                || !envelope.has(
                        "iv")
                || !envelope.has(
                        "ciphertext")) {

            throw new JSONException(
                    "Unvollständiges verschlüsseltes Backup.");
        }
    }

    private static SecretKeySpec deriveKey(
            byte[] salt)
            throws GeneralSecurityException {

        PBEKeySpec keySpec =
                new PBEKeySpec(
                        LOCAL_PROTECTION_SECRET,
                        salt,
                        KDF_ITERATIONS,
                        KEY_BITS);

        try {
            SecretKeyFactory factory =
                    SecretKeyFactory.getInstance(
                            "PBKDF2WithHmacSHA256");

            byte[] keyBytes =
                    factory.generateSecret(
                                    keySpec)
                            .getEncoded();

            return new SecretKeySpec(
                    keyBytes,
                    "AES");

        } finally {
            keySpec.clearPassword();
        }
    }

    private static String encode(
            byte[] data) {

        return Base64.getEncoder()
                .encodeToString(
                        data);
    }

    private static byte[] decode(
            String value)
            throws JSONException {

        try {
            return Base64.getDecoder()
                    .decode(
                            value);

        } catch (IllegalArgumentException exception) {

            throw new JSONException(
                    "Ungültige Verschlüsselungsdaten im Backup.");
        }
    }
}
