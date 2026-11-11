## State of the current run: shield, scrap, items, sector depth, objective
## progress and timer. Passive items are applied here. Later this is also
## where meta progression (scrap banked at the hideout) will live.
extends Node

signal shield_changed(shield: int)
signal scrap_changed(scrap: int)
signal nodes_changed(lit: int, total: int)
signal time_changed(run_time: float)
signal items_changed(items: Array)
signal depth_changed(depth: int)

const ItemDB := preload("res://scripts/items/item_db.gd")
const STARTING_SHIELD := 3
const SLOTS := {
	ItemData.Family.BALL: 3,
	ItemData.Family.FLIPPER: 2,
	ItemData.Family.PASSIVE: 4,
}
const SCRAP_MAGNET_BONUS := 1.25

var shield := STARTING_SHIELD
var max_shield := STARTING_SHIELD
var scrap := 0
var nodes_lit := 0
var nodes_total := 3
var run_time := 0.0
var run_active := false
var depth := 1
var items: Array[ItemData] = []

var _insurance_used := false
var _scrap_fraction := 0.0


func _ready() -> void:
	Events.scrap_added.connect(add_scrap)


func _process(delta: float) -> void:
	if run_active:
		run_time += delta
		time_changed.emit(run_time)


func start_run() -> void:
	shield = STARTING_SHIELD
	max_shield = STARTING_SHIELD
	scrap = 0
	_scrap_fraction = 0.0
	run_time = 0.0
	depth = 1
	items.clear()
	run_active = true
	shield_changed.emit(shield)
	scrap_changed.emit(scrap)
	time_changed.emit(run_time)
	items_changed.emit(items)
	depth_changed.emit(depth)
	Events.run_started.emit()


func start_sector(total_nodes: int) -> void:
	nodes_lit = 0
	nodes_total = total_nodes
	_insurance_used = false
	nodes_changed.emit(nodes_lit, nodes_total)


func advance_depth() -> void:
	depth += 1
	depth_changed.emit(depth)


func add_scrap(amount: int) -> void:
	if not run_active:
		return
	var value := float(amount) * (SCRAP_MAGNET_BONUS if has_item(&"scrap_magnet") else 1.0)
	value += _scrap_fraction
	var whole := int(value)
	_scrap_fraction = value - whole
	scrap += whole
	scrap_changed.emit(scrap)


func restore_shield(amount: int) -> void:
	shield = mini(shield + amount, max_shield)
	shield_changed.emit(shield)


## Returns true when every node in the sector is lit.
func light_node() -> bool:
	nodes_lit += 1
	nodes_changed.emit(nodes_lit, nodes_total)
	return nodes_lit >= nodes_total


## Handles a drain. Returns true while the run continues. Only a drain at
## zero shield ends the run.
func take_drain() -> bool:
	if has_item(&"insurance") and not _insurance_used:
		_insurance_used = true
		Events.insurance_used.emit()
		return true
	if shield <= 0:
		end_run(false)
		return false
	shield -= 1
	shield_changed.emit(shield)
	return true


func end_run(won: bool) -> void:
	run_active = false
	Events.run_ended.emit(won, scrap, run_time)


func has_item(id: StringName) -> bool:
	for item in items:
		if item.id == id:
			return true
	return false


func family_count(family: ItemData.Family) -> int:
	var n := 0
	for item in items:
		if item.family == family:
			n += 1
	return n


func can_take(item: ItemData) -> bool:
	return not has_item(item.id) and family_count(item.family) < SLOTS[item.family]


func add_item(item: ItemData) -> void:
	if not can_take(item):
		push_warning("Cannot take item %s" % item.id)
		return
	items.append(item)
	items_changed.emit(items)


## Up to `count` distinct items the player could take right now.
func roll_offers(count := 3) -> Array[ItemData]:
	var pool: Array[ItemData] = []
	for item in ItemDB.ALL:
		if can_take(item):
			pool.append(item)
	pool.shuffle()
	return pool.slice(0, count)
