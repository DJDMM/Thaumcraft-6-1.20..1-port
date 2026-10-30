"""Check bilingual coverage without relying on Minecraft's English fallback.

Literal Java references, the book catalogue and selected dynamic families are
checked here; the opt-in native Localization client also checks live registries,
actual card interpolation and language reload. Original text provenance is
checked separately by import_original_languages.py --check against the local JAR.
"""
from __future__ import annotations

import argparse
import json
from pathlib import Path
import re


def unique_object(pairs):
    result = {}
    for key, value in pairs:
        if key in result:
            raise ValueError(f'Duplicate JSON translation key: {key}')
        result[key] = value
    return result


def validate(project: Path) -> tuple[list[str], dict]:
    assets = project / 'src/main/resources/assets/thaumcraft'
    errors, words, references = [], {}, {}

    def reference(key, context):
        references.setdefault(key, set()).add(context)

    # These retired scaffold notices are deliberately blank in ordinary play.
    silent = {'thaumcraft.book.archive_notice', 'thaumcraft.book.recipes_notice',
              'thaumcraft.book.image_unavailable', 'thaumcraft.catalog.visual_only',
              'thaumcraft.catalog.original_variant'}
    for locale in ('en_us', 'ru_ru'):
        try:
            path = assets / 'lang' / f'{locale}.json'
            words[locale] = json.loads(path.read_text(encoding='utf-8'), object_pairs_hook=unique_object)
            for key, text in words[locale].items():
                if not isinstance(text, str) or (not text.strip() and key not in silent):
                    errors.append(f'{locale}:{key}: empty/non-string translation')
                elif '\ufffd' in text:
                    errors.append(f'{locale}:{key}: Unicode replacement character')
        except (ValueError, UnicodeError) as failure:
            return [f'{locale}: {failure}'], {}
    for locale in words:
        other = words['ru_ru' if locale == 'en_us' else 'en_us']
        for key in other.keys() - words[locale].keys():
            errors.append(f'{locale}: missing shared key {key}')

    literal = re.compile(r'(?:Component\.translatable|I18n\.(?:get|exists)|\.hasTranslation|new RangedAttribute)'
                         r'\(\s*"([^"\n]+)"\s*(?=[,)])')
    java_count = 0
    for path in (project / 'src/main/java/thaumcraft').rglob('*.java'):
        if any(part in path.name for part in ('GameTest', 'SmokeTest')) or path.name.startswith('Legacy'):
            continue
        java_count += 1
        source = re.sub(r'(?m)^\s*//.*$', '', path.read_text(encoding='utf-8-sig'))
        context = path.relative_to(project).as_posix()
        for match in literal.finditer(source):
            key = match[1]
            # Vanilla owns these keys. Mod-owned gui.thaumcraft.* must not be
            # excluded with a broad gui.* prefix, which would hide regressions.
            if key.startswith(('enchantment.minecraft.', 'enchantment.level.', 'key.keyboard.', 'key.mouse.')):
                continue
            if key in ('container.inventory', 'gui.done', 'gui.cancel', 'gui.yes', 'gui.no', 'gui.back'):
                continue
            reference(key, context)
        helper = re.search(r'(?:String|Component) tr\(.*?translatable\("([^"]+)"\s*\+', source)
        if helper:
            for match in re.finditer(r'\btr\("([^"]+)"\s*(?=[,)])', source):
                reference(helper[1] + match[1], context)

    entries = json.loads((assets / 'research/catalog.json').read_text(encoding='utf-8'))
    for entry in entries:
        reference(entry['title'], f'research:{entry["key"]}:title')
        for stage in entry['stages'] + entry.get('addenda', []):
            if stage.get('text'):
                reference(stage['text'], f'research:{entry["key"]}:stage')
    for category in ('BASICS', 'AUROMANCY', 'ALCHEMY', 'ARTIFICE', 'INFUSION', 'GOLEMANCY', 'ELDRITCH'):
        reference('tc.research_category.' + category, 'research categories')
    aspect_source = re.sub(r'(?m)^\s*//.*$', '', (project / 'src/main/java/thaumcraft/api/aspects/Aspect.java').read_text(encoding='utf-8'))
    for tag in re.findall(r'new Aspect\("([a-z]+)"', aspect_source):
        reference('tc.aspect.' + tag, 'Aspect localized description')
    for page in range(1, 4):
        reference('book.start.' + str(page), 'Strange Dreams written book')
        for locale, data in words.items():
            text = data.get('book.start.' + str(page), '')
            if text.startswith('"') or '\\n' in text or '\\"' in text:
                errors.append(f'{locale}:book.start.{page}: raw JSON page escaping')

    for key, contexts in references.items():
        for locale, data in words.items():
            if key not in data:
                errors.append(f'{locale}: missing {key} ({", ".join(sorted(contexts))})')
    report = {'locales': {locale: len(data) for locale, data in words.items()},
              'productionJavaFilesScanned': java_count, 'checkedTranslationReferences': len(references),
              'researchEntries': len(entries), 'errors': errors,
              'limits': 'Literal/helper/book references and selected families; live registries and formatting need native client.'}
    return errors, report


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--report', type=Path)
    args = parser.parse_args()
    failures, report = validate(Path(__file__).resolve().parents[1])
    if args.report:
        args.report.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    if failures:
        raise SystemExit('\n'.join(failures))
    print(json.dumps(report, ensure_ascii=False))
