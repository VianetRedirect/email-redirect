## One power-up. The data lives in res://items/*.tres and the behaviour lives
## in the script that owns that family's hook: ball.gd for ball mods,
## flipper.gd for flipper mods, game_state.gd for passives. Code checks for
## an item with GameState.has_item(&"id").
class_name ItemData
extends Resource

enum Family { BALL, FLIPPER, PASSIVE }
enum Rarity { COMMON, UNCOMMON, RARE }

@export var id: StringName
@export var display_name := ""
@export var family := Family.BALL
@export var rarity := Rarity.COMMON
## From the fixed set: Shock, Burn, Hack, Magnet, Split.
@export var tags: PackedStringArray = []
@export_multiline var description := ""
## Short label for the HUD item strip, 5 characters or fewer.
@export var short_name := ""


static func family_name(f: Family) -> String:
	return ["BALL", "FLIPPER", "PASSIVE"][f]


static func family_color(f: Family) -> Color:
	return [Color(0.35, 0.85, 1.0), Color(1.0, 0.82, 0.3), Color(1.0, 0.4, 0.85)][f]
