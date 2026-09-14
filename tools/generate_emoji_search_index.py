#!/usr/bin/env python3

import json
import re
import unicodedata
import urllib.request
from pathlib import Path


CLDR_VERSION = "48.2.0"
EMOJI_VERSION = "16.0"

RAW_CLDR = (
    "https://raw.githubusercontent.com/"
    "unicode-org/cldr-json/"
    + CLDR_VERSION
    + "/cldr-json/"
)

EMOJI_TEST_URL = (
    "https://www.unicode.org/Public/emoji/"
    + EMOJI_VERSION
    + "/emoji-test.txt"
)

LICENSE_URL = (
    RAW_CLDR
    + "cldr-annotations-derived-full/LICENSE"
)

OUTPUT_DIR = Path(
    "app/src/main/assets"
)

OUTPUT_FILE = (
    OUTPUT_DIR
    / "emoji_search.json"
)

LICENSE_FILE = (
    OUTPUT_DIR
    / "emoji_search_LICENSE.txt"
)

NOTICE_FILE = (
    OUTPUT_DIR
    / "emoji_search_NOTICE.txt"
)


def download_text(url):
    request = urllib.request.Request(
        url,
        headers={
            "User-Agent":
                "AppStow emoji search index generator"
        },
    )

    with urllib.request.urlopen(
        request,
        timeout=60,
    ) as response:

        return response.read().decode(
            "utf-8"
        )


def download_json(url):
    return json.loads(
        download_text(url)
    )


def full_annotations_url(language):
    return (
        RAW_CLDR
        + "cldr-annotations-full/"
        + "annotations/"
        + language
        + "/annotations.json"
    )


def derived_annotations_url(language):
    return (
        RAW_CLDR
        + "cldr-annotations-derived-full/"
        + "annotationsDerived/"
        + language
        + "/annotations.json"
    )


def merge_language(language):
    full = download_json(
        full_annotations_url(
            language
        )
    )["annotations"]["annotations"]

    derived = download_json(
        derived_annotations_url(
            language
        )
    )["annotationsDerived"]["annotations"]

    merged = {}

    for source in (
        full,
        derived,
    ):
        for emoji, record in source.items():
            target = merged.setdefault(
                emoji,
                {
                    "default": [],
                    "tts": [],
                },
            )

            for field in (
                "default",
                "tts",
            ):
                for value in record.get(
                    field,
                    [],
                ):
                    if value not in target[field]:
                        target[field].append(
                            value
                        )

    # CLDR verwendet bei einigen Sequenzen eine Form
    # ohne Variation Selector. Beide Formen auffindbar
    # machen.
    lookup = dict(merged)

    for emoji, record in merged.items():
        stripped = emoji.replace(
            "\ufe0f",
            "",
        )

        lookup.setdefault(
            stripped,
            record,
        )

    return lookup


def normalize(value):
    value = value.casefold()

    value = unicodedata.normalize(
        "NFKD",
        value,
    )

    result = []
    previous_space = True

    for character in value:
        if unicodedata.category(
            character
        ).startswith("M"):
            continue

        if character.isalnum():
            result.append(
                character
            )
            previous_space = False
        elif not previous_space:
            result.append(
                " "
            )
            previous_space = True

    return "".join(
        result
    ).strip()


def unique_tokens(values):
    result = []
    seen = set()

    for value in values:
        normalized = normalize(
            value
        )

        for token in normalized.split():
            if token in seen:
                continue

            seen.add(
                token
            )

            result.append(
                token
            )

    return result


def find_record(
        annotations,
        emoji):

    record = annotations.get(
        emoji
    )

    if record is not None:
        return record

    return annotations.get(
        emoji.replace(
            "\ufe0f",
            "",
        )
    )


def record_values(record):
    if record is None:
        return []

    values = []

    for field in (
        "tts",
        "default",
    ):
        for value in record.get(
            field,
            [],
        ):
            if value not in values:
                values.append(
                    value
                )

    return values


def record_label(
        record,
        fallback):

    if record is None:
        return fallback

    tts = record.get(
        "tts",
        [],
    )

    if tts:
        return tts[0]

    defaults = record.get(
        "default",
        [],
    )

    if defaults:
        return defaults[0]

    return fallback


def load_emoji_order():
    text = download_text(
        EMOJI_TEST_URL
    )

    result = []
    seen = set()
    current_group = ""

    for line in text.splitlines():
        if not line:
            continue

        if line.startswith(
            "# group:"
        ):
            current_group = (
                line.split(
                    ":",
                    1,
                )[1].strip()
            )

            continue

        if line.startswith(
            "#"
        ):
            continue

        if ";" not in line:
            continue

        code_field, right = line.split(
            ";",
            1,
        )

        status = right.split(
            "#",
            1,
        )[0].strip()

        if status != "fully-qualified":
            continue

        code_points = [
            int(
                value,
                16,
            )
            for value
            in code_field.strip().split()
        ]

        emoji = "".join(
            chr(value)
            for value
            in code_points
        )

        # Hautfarbenvarianten bleiben aus dem normalen
        # Hauptraster heraus, damit die Übersicht kompakt bleibt.
        if any(
            0x1F3FB <= value <= 0x1F3FF
            for value
            in code_points
        ):
            continue

        if emoji in seen:
            continue

        seen.add(
            emoji
        )

        official_name = ""

        if "#" in right:
            comment = right.split(
                "#",
                1,
            )[1].strip()

            match = re.match(
                r"^\S+\s+E[0-9.]+\s+(.+)$",
                comment,
            )

            if match:
                official_name = (
                    match.group(1)
                )

        result.append(
            (
                emoji,
                official_name,
                current_group,
            )
        )

    return result


def main():
    OUTPUT_DIR.mkdir(
        parents=True,
        exist_ok=True
    )

    print(
        "Lade deutsche CLDR-Annotationen ..."
    )

    german = merge_language(
        "de"
    )

    print(
        "Lade englische CLDR-Annotationen ..."
    )

    english = merge_language(
        "en"
    )

    print(
        "Lade Unicode Emoji "
        + EMOJI_VERSION
        + " ..."
    )

    emoji_order = load_emoji_order()

    entries = []

    for emoji, official_name, group in emoji_order:
        german_record = find_record(
            german,
            emoji,
        )

        english_record = find_record(
            english,
            emoji,
        )

        german_values = record_values(
            german_record
        )

        english_values = record_values(
            english_record
        )

        german_label = record_label(
            german_record,
            official_name,
        )

        english_label = record_label(
            english_record,
            official_name,
        )

        searchable_values = []

        searchable_values.extend(
            german_values
        )

        searchable_values.extend(
            english_values
        )

        if official_name:
            searchable_values.append(
                official_name
            )

        tokens = unique_tokens(
            searchable_values
        )

        if not tokens:
            continue

        entries.append(
            {
                "e": emoji,
                "de": german_label,
                "en": english_label,
                "g": group,
                "q": " ".join(tokens),
            }
        )

    with OUTPUT_FILE.open(
        "w",
        encoding="utf-8",
    ) as output:

        output.write(
            "[\n"
        )

        for index, entry in enumerate(
            entries
        ):
            output.write(
                json.dumps(
                    entry,
                    ensure_ascii=False,
                    separators=(
                        ",",
                        ":",
                    ),
                )
            )

            if index + 1 < len(entries):
                output.write(
                    ","
                )

            output.write(
                "\n"
            )

        output.write(
            "]\n"
        )

    LICENSE_FILE.write_text(
        download_text(
            LICENSE_URL
        ),
        encoding="utf-8",
    )

    NOTICE_FILE.write_text(
        (
            "AppStow Emoji Search Data\n"
            "\n"
            "Generated from:\n"
            "- Unicode Emoji "
            + EMOJI_VERSION
            + " emoji-test.txt\n"
            "- Unicode CLDR "
            + CLDR_VERSION
            + " German and English annotations\n"
            "\n"
            "Unicode data files are licensed under "
            "Unicode License v3 (SPDX: Unicode-3.0).\n"
            "See emoji_search_LICENSE.txt.\n"
        ),
        encoding="utf-8",
    )

    print(
        "Emoji-Sucheinträge:",
        len(entries),
    )

    print(
        "Ausgabe:",
        OUTPUT_FILE,
    )


if __name__ == "__main__":
    main()
