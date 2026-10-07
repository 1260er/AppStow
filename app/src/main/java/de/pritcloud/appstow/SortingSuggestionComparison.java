package de.pritcloud.appstow;

final class SortingSuggestionComparison {

    private SortingSuggestionComparison() {
    }

    static boolean[] changedTargetLines(
            String currentText,
            String targetText) {

        String[] currentLines =
                splitLines(
                        currentText);

        String[] targetLines =
                splitLines(
                        targetText);

        boolean[] changed =
                new boolean[
                        targetLines.length];

        for (int index = 0;
             index < targetLines.length;
             index++) {

            String targetLine =
                    targetLines[index];

            if (targetLine.isEmpty()
                    || "\u2014".equals(
                            targetLine)) {

                changed[index] =
                        false;

                continue;
            }

            changed[index] =
                    index >= currentLines.length
                            || !targetLine.equals(
                                    currentLines[index]);
        }

        return changed;
    }

    private static String[] splitLines(
            String text) {

        return text.split(
                "\\n",
                -1);
    }
}
