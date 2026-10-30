"""Import TC6 BETA26 English/Russian display text without replacing port-only UI.

The copyrighted original JAR is supplied locally, never downloaded or committed.
Run with --check to verify original text and explicit registry aliases.  A normal
run changes only the two language JSON files; --report additionally records every
replacement and its provenance.  Re-running is idempotent.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
import re
import zipfile

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/thaumcraft'
JAR_SHA256 = '9425f8643581b27ff8845b087c8bc6fc10425a32942f1a3f0e265ce6b38f7b5f'
NUMERIC_FORMAT = re.compile(r'%(\d+\$)?[\d.]*[df]')

# These twelve English keys are absent from the released Russian locale.  They
# are translations of that release's English prose, not replacements of an
# existing Russian translation.  Whitespace/markup mirror the original usage.
RUSSIAN_SUPPLEMENTS = {
    'item.seal.breaker_advanced.name': 'Печать контроля: Продвинутое Разрушение Блоков',
    'tile.inlay.name': 'Редстоуновая инкрустация',
    'enchantment.infusion.LAMPLIGHT': 'Фонарщик',
    'stability.gain': 'прирост / цикл',
    'stability.range': 'от 0 до ',
    'stability.loss': 'потери / цикл',
    'thaumcraft.BOLT.text': 'Обрушьте магию прямо на цель в виде сосредоточенного разряда энергии. Эффект мгновенный, но дальность ограничена 16 блоками.',
    'workbench.available': 'доступно',
    'workbench.discount': 'скидка',
    'workbench.cost': 'вис',
    'golem.prop.silk': 'Использовать Шёлковое касание',
    'research.SEALBREAK.stage.ADV': 'Теперь я могу изготовить продвинутую версию печати, которая позволит фильтровать больше предметов одновременно и даст голему возможность разрушать блоки так, словно он использует Шёлковое касание.<BR>При использовании Шёлкового касания големам потребуется больше времени на разрушение блоков. ',
}

# The released RU locale leaves the English "the" in these two name patterns.
# The second argument is a localized champion descriptor, not an ordinal number.
RUSSIAN_INCOMPLETE_FIXES = {
    'entity.EldritchWarden.name.custom': '%s, %s',
    'entity.CultistLeader.name.custom': '%s, %s Претор',
}

# The Russian stage stops after Essence Harvester.  Keep its whole original
# prefix verbatim and append only the Lamplighter page present in released EN.
RUSSIAN_INCOMPLETE_APPENDICES = {
    'research.INFUSIONENCHANTMENT.stage.2': '<PAGE>§oФонарщик§r<DIV>Когда вы разрушаете блок, это зачарование размещает на его месте невидимый и неосязаемый источник света, если уровень освещения ниже 10.<DIV>§oУровни§r: нет<BR>§oЦель§r: топор, кирка, лопата',
}

# These labels were hardcoded in the original Java, or describe modern narrator
# controls.  The original language files cannot supply them.  English device
# labels retain the original Java wording; Russian uses the original terminology.
PORT_LABELS = {
    'tc.bore.width': ('Width: %s', 'Ширина: %s'),
    'tc.bore.depth': ('Depth: %s', 'Глубина: %s'),
    'tc.bore.speed': ('Speed: +%s', 'Скорость: +%s'),
    'tc.bore.properties': ('Other properties:', 'Другие свойства:'),
    'thaumcraft.device.output': ('Out: %s', 'Выход: %s'),
    'thaumcraft.device.input': ('In: %s', 'Вход: %s'),
    'thaumcraft.device.note': ('Note: %s', 'Нота: %s'),
    'gui.thaumcraft.focal.focus_name': ('Focus name', 'Название линзы'),
    'tooltip.thaumcraft.brain_jar.experience': ('Experience: %s', 'Опыт: %s'),
    'logistics.scrollbar': ('Scroll', 'Прокрутка'),
    'attribute.thaumcraft.champion_mod': ('Champion modifier', 'Модификатор чемпиона'),
    'attribute.thaumcraft.tainted_mod': ('Taint modifier', 'Модификатор заражения'),
}

FORMATTED_ALIASES = {
    'item.thaumcraft.vis_crystal_aer': ('item.crystal_essence.name', 'Aer'),
    'item.thaumcraft.vis_crystal_terra': ('item.crystal_essence.name', 'Terra'),
    'item.thaumcraft.vis_crystal_ignis': ('item.crystal_essence.name', 'Ignis'),
    'item.thaumcraft.vis_crystal_aqua': ('item.crystal_essence.name', 'Aqua'),
    'item.thaumcraft.vis_crystal_ordo': ('item.crystal_essence.name', 'Ordo'),
    'item.thaumcraft.vis_crystal_perditio': ('item.crystal_essence.name', 'Perditio'),
}


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def load_legacy(data: bytes) -> tuple[dict[str, str], list[str]]:
    """Match 1.12 Locale's first '=' split and numeric format normalization."""
    result, duplicates = {}, []
    for line in data.decode('utf-8-sig').splitlines():
        if not line or line.startswith('#') or '=' not in line:
            continue
        key, value = line.split('=', 1)
        if key in result:
            duplicates.append(key)
        value = NUMERIC_FORMAT.sub(lambda m: '%' + (m.group(1) or '') + 's', value)
        # TC6 wrote these strings directly into written-book JSON pages. The
        # modern port creates Components instead; decode the JSON string wrapper
        # so players see the same prose and paragraph breaks, not literal \n.
        if key in ('book.start.1', 'book.start.2', 'book.start.3'):
            value = json.loads(value.strip())
        result[key] = value
    return result, duplicates


def alias_map() -> dict[str, str]:
    """Use the catalogue's audited identities; never guess translated labels."""
    aliases = {}
    for kind, prefix in (('items', 'item'), ('blocks', 'block')):
        rows = json.loads((ASSETS / 'catalog' / f'{kind}.json').read_text(encoding='utf-8'))[kind]
        for row in rows:
            source = row['legacy_name_key']
            if row['id'].startswith('celestial_notes_'):
                # ItemCelestialNotes.getUnlocalizedName deliberately returns one
                # common name; the sun/star/moon distinction is in its tooltip.
                source = 'item.celestial_notes.name'
            if row['id'].startswith('slab_double_'):
                # Double slabs have no item or distinct localized name in TC6.
                source = 'tile.' + row['id'].replace('slab_double_', 'slab_', 1) + '.name'
            aliases[f'{prefix}.thaumcraft.{row["id"]}'] = source
    rows = json.loads((ASSETS / 'catalog/entities.json').read_text(encoding='utf-8'))['entities']
    entity_overrides = {
        'grapple': 'item.grapple_gun_tip.name',
        'causality_collapser': 'item.causality_collapser.name',
        'focus_mine': 'entity.FocusMine.name',
    }
    for row in rows:
        aliases['entity.thaumcraft.' + row['id']] = entity_overrides.get(
            row['id'], 'entity.' + row['legacy_id'] + '.name')
    aliases.update({
        # Compatibility Nitor is physically the native yellow Nitor block.
        'block.thaumcraft.nitor': 'tile.nitor_yellow.name',
        'entity.thaumcraft.focus_cloud_spell': 'entity.FocusCloud.name',
        'entity.thaumcraft.focus_mine_spell': 'entity.FocusMine.name',
        'entity.thaumcraft.spell_bat_spell': 'entity.Spellbat.name',
        # RemainingEffectsModule's extra registry ID ports EntitySpecialItem;
        # BETA26 ConfigEntities registers that class with the name SpecialItem.
        'entity.thaumcraft.focus_exchange_item': 'entity.SpecialItem.name',
        'effect.thaumcraft.warp_ward': 'potion.warpward',
        'effect.thaumcraft.flux_taint': 'potion.flux_taint',
        'effect.thaumcraft.vis_exhaust': 'potion.vis_exhaust',
        'effect.thaumcraft.infectious_vis_exhaust': 'potion.infvisexhaust',
        # WarpAffliction itself returns the original potion.* description IDs.
        # Also translate modern registry-derived keys used by external viewers.
        'effect.thaumcraft.thaumarhia': 'potion.thaumarhia',
        'effect.thaumcraft.unnatural_hunger': 'potion.unhunger',
        'effect.thaumcraft.sun_scorned': 'potion.sunscorned',
        'effect.thaumcraft.death_gaze': 'potion.deathgaze',
        'effect.thaumcraft.blurred_vision': 'potion.blurred',
    })
    for part in ('pearl', 'nodule', 'mote'):
        aliases['item.thaumcraft.primordial_pearl.' + part] = 'item.primordial_pearl.' + part + '.name'
    for entity in ('EldritchWarden', 'EldritchGolem', 'CultistLeader'):
        aliases['entity.Thaumcraft.' + entity + '.name.custom'] = 'entity.' + entity + '.name.custom'
    return aliases


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--jar', type=Path, default=ROOT.parents[1] / 'work/Thaumcraft-1.12.2-6.1.BETA26.jar')
    parser.add_argument('--check', action='store_true')
    parser.add_argument('--report', type=Path)
    args = parser.parse_args()
    if sha256(args.jar.read_bytes()) != JAR_SHA256:
        raise SystemExit('Expected the pinned Thaumcraft 6.1.BETA26 JAR; SHA256 differs')
    aliases = alias_map()
    report = {
        'schema': 1,
        'version': '0.42.1',
        'originalJarSha256': JAR_SHA256,
        'normalization': ['1.12 Locale numeric [df] placeholders become s', 'book.start.1..3 JSON string page decoded to display text'],
        'aliases': aliases,
        'formattedAliases': FORMATTED_ALIASES,
        'russianMissingOriginalKeys': list(RUSSIAN_SUPPLEMENTS),
        'russianIncompleteOriginalKeys': list(RUSSIAN_INCOMPLETE_FIXES) + list(RUSSIAN_INCOMPLETE_APPENDICES),
        'russianIncompleteOriginalAppendices': RUSSIAN_INCOMPLETE_APPENDICES,
        'portOnlyLabels': PORT_LABELS,
        'locales': {},
    }
    all_changes = 0
    with zipfile.ZipFile(args.jar) as jar:
        for locale in ('en_us', 'ru_ru'):
            path = ASSETS / 'lang' / f'{locale}.json'
            before_bytes = path.read_bytes()
            before = json.loads(before_bytes.decode('utf-8-sig'))
            original_bytes = jar.read(f'assets/thaumcraft/lang/{locale}.lang')
            imported, duplicates = load_legacy(original_bytes)
            original_count = len(imported)
            if locale == 'ru_ru':
                if set(RUSSIAN_SUPPLEMENTS).intersection(imported):
                    raise SystemExit('A purported missing Russian key already exists in the pinned locale')
                imported.update(RUSSIAN_SUPPLEMENTS)
                imported.update(RUSSIAN_INCOMPLETE_FIXES)
                for key, suffix in RUSSIAN_INCOMPLETE_APPENDICES.items():
                    imported[key] += suffix
            for target, source in aliases.items():
                if source not in imported:
                    raise SystemExit(f'{locale}: original alias source is missing: {target} -> {source}')
                imported[target] = imported[source]
            for target, (source, aspect_name) in FORMATTED_ALIASES.items():
                # Released ItemCrystalEssence uses String.format with
                # Aspect.getName() (capitalizeFully of the Latin aspect tag).
                imported[target] = imported[source].replace('%s', aspect_name)
            imported.update({key: values[0 if locale == 'en_us' else 1]
                             for key, values in PORT_LABELS.items()})
            merged = dict(before)
            merged.update(imported)
            changes = {key: {'before': before.get(key), 'after': value}
                       for key, value in imported.items() if before.get(key) != value}
            all_changes += len(changes)
            report['locales'][locale] = {
                'originalLangSha256': sha256(original_bytes),
                'originalKeyCount': original_count,
                'duplicateOriginalKeys': duplicates,
                'beforeJsonSha256': sha256(before_bytes),
                'changes': changes,
                'changeCount': len(changes),
                'finalKeyCount': len(merged),
            }
            if not args.check:
                path.write_text(json.dumps(merged, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
                report['locales'][locale]['afterJsonSha256'] = sha256(path.read_bytes())
            print(f'{locale}: original={original_count}, aliases={len(aliases)}, formatted={len(FORMATTED_ALIASES)}, changes={len(changes)}, final={len(merged)}')
    if args.check and all_changes:
        raise SystemExit(f'{all_changes} translations differ from the pinned original/import rules')
    if args.report and not args.check:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')


if __name__ == '__main__':
    main()
