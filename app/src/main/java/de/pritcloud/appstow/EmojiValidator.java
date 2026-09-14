package de.pritcloud.appstow;

import android.icu.lang.UCharacter;

final class EmojiValidator {

    private static final int PROPERTY_EMOJI =
            UCharacter.getPropertyEnum("Emoji");

    private static final int PROPERTY_EMOJI_PRESENTATION =
            UCharacter.getPropertyEnum("Emoji_Presentation");

    private static final int PROPERTY_EMOJI_MODIFIER =
            UCharacter.getPropertyEnum("Emoji_Modifier");

    private static final int PROPERTY_EMOJI_MODIFIER_BASE =
            UCharacter.getPropertyEnum("Emoji_Modifier_Base");

    private static final int ZERO_WIDTH_JOINER = 0x200D;
    private static final int VARIATION_SELECTOR_16 = 0xFE0F;
    private static final int COMBINING_KEYCAP = 0x20E3;

    private static final int BLACK_FLAG = 0x1F3F4;

    private static final int REGIONAL_INDICATOR_START = 0x1F1E6;
    private static final int REGIONAL_INDICATOR_END = 0x1F1FF;

    private static final int TAG_START = 0xE0020;
    private static final int TAG_END = 0xE007E;
    private static final int CANCEL_TAG = 0xE007F;

    private EmojiValidator() {
    }

    static boolean isValid(String value) {
        if (value == null
                || value.isEmpty()
                || !value.equals(value.trim())) {

            return false;
        }

        int[] codePoints =
                value.codePoints()
                        .toArray();

        if (isRegionalFlag(codePoints)
                || isKeycapSequence(codePoints)
                || isTagFlag(codePoints)) {

            return true;
        }

        for (int codePoint : codePoints) {
            if (isRegionalIndicator(codePoint)
                    || codePoint == COMBINING_KEYCAP
                    || isTagCharacter(codePoint)
                    || codePoint == CANCEL_TAG) {

                return false;
            }
        }

        return isEmojiSequence(codePoints);
    }

    private static boolean isEmojiSequence(
            int[] codePoints) {

        int index = 0;
        boolean hasComponent = false;

        while (index < codePoints.length) {
            int base =
                    codePoints[index++];

            if (isKeycapBase(base)
                    || !hasProperty(
                            base,
                            PROPERTY_EMOJI)) {

                return false;
            }

            boolean emojiPresentation =
                    hasProperty(
                            base,
                            PROPERTY_EMOJI_PRESENTATION);

            if (index < codePoints.length
                    && codePoints[index]
                    == VARIATION_SELECTOR_16) {

                emojiPresentation = true;
                index++;
            }

            if (index < codePoints.length
                    && hasProperty(
                            codePoints[index],
                            PROPERTY_EMOJI_MODIFIER)) {

                if (!hasProperty(
                        base,
                        PROPERTY_EMOJI_MODIFIER_BASE)) {

                    return false;
                }

                emojiPresentation = true;
                index++;
            }

            if (!emojiPresentation) {
                return false;
            }

            hasComponent = true;

            if (index == codePoints.length) {
                return hasComponent;
            }

            if (codePoints[index]
                    != ZERO_WIDTH_JOINER) {

                return false;
            }

            index++;

            if (index == codePoints.length) {
                return false;
            }
        }

        return false;
    }

    private static boolean isRegionalFlag(
            int[] codePoints) {

        return codePoints.length == 2
                && isRegionalIndicator(
                        codePoints[0])
                && isRegionalIndicator(
                        codePoints[1]);
    }

    private static boolean isRegionalIndicator(
            int codePoint) {

        return codePoint
                >= REGIONAL_INDICATOR_START
                && codePoint
                <= REGIONAL_INDICATOR_END;
    }

    private static boolean isKeycapSequence(
            int[] codePoints) {

        if (codePoints.length == 2) {
            return isKeycapBase(
                    codePoints[0])
                    && codePoints[1]
                    == COMBINING_KEYCAP;
        }

        return codePoints.length == 3
                && isKeycapBase(
                        codePoints[0])
                && codePoints[1]
                == VARIATION_SELECTOR_16
                && codePoints[2]
                == COMBINING_KEYCAP;
    }

    private static boolean isKeycapBase(
            int codePoint) {

        return codePoint == 0x23
                || codePoint == 0x2A
                || (codePoint >= 0x30
                && codePoint <= 0x39);
    }

    private static boolean isTagFlag(
            int[] codePoints) {

        if (codePoints.length < 3
                || codePoints[0] != BLACK_FLAG
                || codePoints[codePoints.length - 1]
                != CANCEL_TAG) {

            return false;
        }

        for (int i = 1;
             i < codePoints.length - 1;
             i++) {

            if (!isTagCharacter(
                    codePoints[i])) {

                return false;
            }
        }

        return true;
    }

    private static boolean isTagCharacter(
            int codePoint) {

        return codePoint >= TAG_START
                && codePoint <= TAG_END;
    }

    private static boolean hasProperty(
            int codePoint,
            int property) {

        return UCharacter.hasBinaryProperty(
                codePoint,
                property);
    }
}
