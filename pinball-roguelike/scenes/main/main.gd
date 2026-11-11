## Run manager. Owns the current sector, the HUD and the upgrade screen.
## Reaching a lift pauses play and offers upgrades; picking one restores a
## shield charge and loads the next sector. Until Night Market exists, the
## next sector is the Sump again. In Phase 3 this also routes to the
## hideout.
extends Node

## One entry per depth. Depths past the end repeat the last entry.
const SECTOR_SCENES: Array[PackedScene] = [
	preload("res://scenes/sectors/Sump.tscn"),
]
const SHIELD_PER_GATE := 1

@onready var _upgrade: CanvasLayer = $UpgradeScreen

var _sector: Node2D
var _run_over := false


func _ready() -> void:
	_sector = $Sector
	Events.sector_cleared.connect(_on_sector_cleared)
	Events.item_picked.connect(_on_item_picked)
	Events.run_ended.connect(func(_w, _s, _t): _run_over = true)
	GameState.start_run()


func _unhandled_input(event: InputEvent) -> void:
	if _run_over and event.is_pressed() and not event.is_echo():
		_run_over = false
		get_tree().paused = false
		GameState.start_run()
		_load_sector()


func _on_sector_cleared() -> void:
	var offers := GameState.roll_offers(3)
	if offers.is_empty():
		_next_sector.call_deferred()
		return
	get_tree().paused = true
	Events.upgrade_offered.emit(offers)


func _on_item_picked(item: ItemData) -> void:
	GameState.add_item(item)
	get_tree().paused = false
	_next_sector()


func _next_sector() -> void:
	GameState.advance_depth()
	GameState.restore_shield(SHIELD_PER_GATE)
	_load_sector()


func _load_sector() -> void:
	var index := mini(GameState.depth - 1, SECTOR_SCENES.size() - 1)
	var old := _sector
	remove_child(old)
	old.queue_free()
	_sector = SECTOR_SCENES[index].instantiate()
	_sector.name = "Sector"
	add_child(_sector)
	move_child(_sector, 0)
