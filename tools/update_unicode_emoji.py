#!/usr/bin/env python3
"""Synchronize the app emoji catalog with the current Unicode Emoji test data."""
from __future__ import annotations

import hashlib
import http.client
import json
import re
import sys
import time
import urllib.error
import urllib.request
import xml.etree.ElementTree as ET
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
EMOJI_DIR = ROOT / "android/app/src/main/assets/emoji"
CATALOG_PATH = EMOJI_DIR / "emoji_catalog_pt.tsv"
MANIFEST_PATH = EMOJI_DIR / "emoji_catalog_manifest.json"

UNICODE_SOURCE = "https://www.unicode.org/Public/emoji/latest/emoji-test.txt"
# The app is a shopping list: keep emoji that depict physical goods, not the entire
# Unicode communication palette.  This subgroup allowlist is refreshed from the
# official Unicode test file so new product emojis can appear automatically.
PRODUCT_SUBGROUPS = {
    "Food & Drink": {"food-fruit", "food-vegetable", "food-prepared", "food-asian",
                     "food-marine", "food-sweet", "drink", "dishware"},
    "Objects": {"clothing", "sound", "music", "musical-instrument", "phone",
                "computer", "light & video", "book-paper", "writing", "office", "mail",
                "lock", "tool", "medical", "household"},
}

CLDR_SOURCES = (
    "https://raw.githubusercontent.com/unicode-org/cldr/main/common/annotations/pt.xml",
    "https://raw.githubusercontent.com/unicode-org/cldr/main/common/annotationsDerived/pt.xml",
)


def download(url: str) -> bytes:
    request = urllib.request.Request(
        url,
        headers={
            "User-Agent": "ShoppingList-Unicode-Catalog-Updater/1.0",
            "Accept-Encoding": "identity",
            "Connection": "close",
        },
    )
    last_error: Exception | None = None
    for attempt in range(4):
        try:
            with urllib.request.urlopen(request, timeout=60) as response:
                payload = response.read()
                if not payload:
                    raise OSError(f"Resposta vazia ao baixar {url}")
                return payload
        except (urllib.error.URLError, http.client.HTTPException, TimeoutError, OSError) as error:
            last_error = error
            if attempt == 3:
                break
            delay = 2 ** attempt
            print(
                f"Download interrompido ({url}); nova tentativa em {delay}s "
                f"({attempt + 2}/4): {error}",
                file=sys.stderr,
            )
            time.sleep(delay)
    raise RuntimeError(f"Não foi possível baixar {url} após 4 tentativas.") from last_error


def read_existing_catalog() -> dict[str, str]:
    existing: dict[str, str] = {}
    if not CATALOG_PATH.exists():
        return existing
    for line in CATALOG_PATH.read_text(encoding="utf-8").splitlines():
        if "\t" not in line:
            continue
        glyph, name = line.split("\t", 1)
        if glyph and name.strip():
            existing[glyph] = name.strip()
    return existing


def read_portuguese_cldr() -> dict[str, str]:
    names: dict[str, str] = {}
    for url in CLDR_SOURCES:
        try:
            root = ET.fromstring(download(url))
            for node in root.iter("annotation"):
                if node.attrib.get("type") == "tts":
                    glyph = node.attrib.get("cp", "").strip()
                    value = (node.text or "").strip()
                    if glyph and value:
                        names[glyph] = value
        except Exception as error:
            print(f"CLDR indisponível ({url}): {error}", file=sys.stderr)
    return names


def is_product_emoji(group: str, subgroup: str, name: str) -> bool:
    normalized = name.casefold()
    # Never show status/action symbols or the shopping-bag glyph that was
    # previously mistaken for the product itself.
    excluded = ("shopping bags", "shopping cart", "clinking", "pouring liquid", "with slash",
                "muted speaker", "speaker low volume", "speaker medium volume",
                "speaker high volume", "with arrow", "cooking", "dashing away")
    if any(term in normalized for term in excluded):
        return False

    # Food, drinks and tableware are all purchasable groceries/household goods.
    if group == "Food & Drink":
        return subgroup in PRODUCT_SUBGROUPS[group]

    # Keep seafood, flowers and plants, but not ordinary wild/pet animals.
    if group == "Animals & Nature":
        if subgroup == "animal-marine":
            return normalized in {
                "fish", "tropical fish", "blowfish", "crab", "lobster",
                "shrimp", "squid", "oyster"
            }
        if subgroup == "plant-flower":
            return any(term in normalized for term in
                       ("bouquet", "rose", "tulip", "hibiscus", "sunflower",
                        "blossom", "cherry blossom", "flower"))
        if subgroup == "plant-other":
            return any(term in normalized for term in
                       ("seedling", "potted plant", "evergreen tree", "deciduous tree",
                        "palm tree", "cactus", "sheaf of rice", "herb", "mushroom"))
        return False

    # Transport icons are limited to actual vehicles/parts, not transit services,
    # emergency vehicles, signs or travel actions.
    if group == "Travel & Places":
        if subgroup == "transport-ground":
            return any(term in normalized for term in
                       ("automobile", "motorcycle", "motor scooter", "scooter",
                        "bicycle", "skateboard", "roller skate", "kick scooter",
                        "wheel", "pickup truck", "fuel pump", "wheelchair"))
        if subgroup == "hotel":
            return normalized == "luggage"
        return normalized == "droplet"

    # Activity symbols are included only when the glyph is itself a physical toy,
    # game component or sporting good.
    if group == "Activities":
        if subgroup == "sport":
            return normalized in {
                "soccer ball", "baseball", "softball", "basketball", "volleyball",
                "american football", "rugby football", "tennis", "flying disc",
                "bowling", "cricket game", "field hockey", "ice hockey", "lacrosse",
                "ping pong", "badminton", "boxing glove", "martial arts uniform",
                "goal net", "ice skate", "fishing pole", "diving mask",
                "running shirt", "skis", "sled", "curling stone"
            }
        if subgroup == "game":
            return normalized in {
                "game die", "pool 8 ball", "puzzle piece", "joystick", "chess pawn",
                "playing cards", "joker", "mahjong", "mahjong red dragon",
                "yo-yo", "kite", "video game", "teddy bear", "nesting dolls",
                "piñata", "mirror ball"
            }
        if subgroup == "event":
            return any(term in normalized for term in
                       ("jack-o-lantern", "christmas tree", "fireworks", "sparkler",
                        "balloon", "ribbon", "wrapped gift", "red envelope",
                        "wind chime", "japanese dolls", "carp streamer", "pine decoration"))
        if subgroup == "arts & crafts":
            return any(term in normalized for term in
                       ("framed picture", "artist palette", "thread", "sewing needle", "yarn"))
        return False

    # A bone is retained as a recognizable dog-chew/pet-shop product. Other
    # body-part emoji (including brain) are not products.
    if group == "People & Body":
        return normalized == "bone"

    if group == "Objects":
        if subgroup not in PRODUCT_SUBGROUPS[group]:
            return False
        # Object subgroups still contain a few pictograms that express a state,
        # rather than a thing that can be put in a shopping basket.
        if subgroup == "phone":
            return normalized in {"mobile phone", "telephone", "telephone receiver",
                                  "pager", "fax machine"}
        if subgroup == "sound":
            return any(term in normalized for term in
                       ("speaker", "loudspeaker", "megaphone", "postal horn", "bell"))
        if subgroup == "music":
            return any(term in normalized for term in
                       ("microphone", "headphone", "radio", "musical keyboard"))
        if subgroup == "mail":
            return normalized in {
                "package", "envelope", "closed mailbox with raised flag",
                "closed mailbox with lowered flag", "open mailbox with raised flag",
                "open mailbox with lowered flag", "postbox"
            }
        if subgroup == "lock":
            return any(term in normalized for term in ("lock", "key"))
        if subgroup == "light & video":
            return any(term in normalized for term in
                       ("camera", "television", "light bulb", "flashlight", "lantern",
                        "candle", "film projector", "video camera", "lamp"))
        if subgroup == "tool":
            return not any(term in normalized for term in
                           ("dagger", "sword", "bomb", "boomerang", "bow and arrow", "shield"))
        return True

    return False


def main() -> None:
    raw = download(UNICODE_SOURCE).decode("utf-8")
    version_match = re.search(r"^# Version:\s*([0-9.]+)", raw, re.MULTILINE)
    if not version_match:
        raise SystemExit("Não foi possível identificar a versão do arquivo oficial Unicode.")
    version = version_match.group(1)
    source_date_match = re.search(r"^# Date:\s*(.+)$", raw, re.MULTILINE)
    source_date = source_date_match.group(1).strip() if source_date_match else ""

    existing = read_existing_catalog()
    cldr_names = read_portuguese_cldr()
    rows: list[tuple[str, str]] = []
    seen: set[str] = set()
    current_group = ""
    current_subgroup = ""

    for line in raw.splitlines():
        if line.startswith("# group:"):
            current_group = line.split(":", 1)[1].strip()
            continue
        if line.startswith("# subgroup:"):
            current_subgroup = line.split(":", 1)[1].strip()
            continue
        if not line or line.startswith("#") or ";" not in line or "#" not in line:
            continue
        data, comment = line.split("#", 1)
        fields = data.split(";")
        if len(fields) < 2 or fields[1].strip() != "fully-qualified":
            continue

        codepoints = fields[0].strip().split()
        if not codepoints:
            continue
        glyph = "".join(chr(int(codepoint, 16)) for codepoint in codepoints)
        if glyph in seen:
            continue
        seen.add(glyph)

        unicode_name = re.sub(r"^\s*\S+\s+E[0-9.]+\s+", "", comment).strip()
        if not is_product_emoji(current_group, current_subgroup, unicode_name):
            continue
        name = existing.get(glyph) or cldr_names.get(glyph) or unicode_name or glyph
        rows.append((glyph, name))

    if not 100 <= len(rows) <= 1000:
        raise SystemExit(f"Catálogo de produtos fora do intervalo esperado: {len(rows)} emojis.")
    names = {name.casefold() for _, name in rows}
    if any("brain" in name or "cérebro" in name for name in names):
        raise SystemExit("O catálogo filtrado incluiu indevidamente o emoji de cérebro.")

    tsv = "".join(f"{glyph}\t{name}\n" for glyph, name in rows).encode("utf-8")
    digest = hashlib.sha256(tsv).hexdigest()
    CATALOG_PATH.write_bytes(tsv)

    manifest = {
        "unicodeVersion": version,
        "emojiCount": len(rows),
        "catalogSchema": 1,
        "catalogType": "shopping-products",
        "sha256": digest,
        "source": UNICODE_SOURCE,
        "sourceDate": source_date,
        "generatedAt": datetime.now(timezone.utc).isoformat(),
    }
    MANIFEST_PATH.write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(f"Unicode Emoji {version}: {len(rows)} emojis; SHA-256 {digest}")


if __name__ == "__main__":
    main()
