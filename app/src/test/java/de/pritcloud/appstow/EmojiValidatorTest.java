package de.pritcloud.appstow;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class EmojiValidatorTest {

    @Test
    public void acceptsSimpleEmoji() {
        assertTrue(
                EmojiValidator.isValid(
                        "🚘"));

        assertTrue(
                EmojiValidator.isValid(
                        "⭐"));

        assertTrue(
                EmojiValidator.isValid(
                        "⚡"));
    }

    @Test
    public void acceptsComposedEmojiSequences() {
        assertTrue(
                EmojiValidator.isValid(
                        "👍🏽"));

        assertTrue(
                EmojiValidator.isValid(
                        "🇩🇪"));

        assertTrue(
                EmojiValidator.isValid(
                        "👨‍👩‍👧‍👦"));

        assertTrue(
                EmojiValidator.isValid(
                        "👩‍💻"));

        assertTrue(
                EmojiValidator.isValid(
                        "❤️"));

        assertTrue(
                EmojiValidator.isValid(
                        "1️⃣"));
    }

    @Test
    public void rejectsNonEmojiAndMultipleSymbols() {
        assertFalse(
                EmojiValidator.isValid(
                        ""));

        assertFalse(
                EmojiValidator.isValid(
                        "abc"));

        assertFalse(
                EmojiValidator.isValid(
                        "123"));

        assertFalse(
                EmojiValidator.isValid(
                        "@@@"));

        assertFalse(
                EmojiValidator.isValid(
                        "🚘⭐"));
    }
}
