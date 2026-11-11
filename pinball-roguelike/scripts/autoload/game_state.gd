## State of the current run: shield, scrap, objective progress and timer.
## Later this is also where meta progression (scrap banked at the hideout,
## unlocked crew) will live.
extends Node

signal shield_changed(shield: int)
signal scrap_changed(scrap: int)
signal nodes_changed(lit: int, total: int)
signal time_changed(run_time: float)

const STARTING_SHIELD := 3

var shield := STARTING_SHIELD
var max_shield := STARTING_SHIELD
var scrap := 0
var nodes_lit := 0
var nodes_total := 3
var run_time := 0.0
var run_active := false


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
	run_time = 0.0
	run_active = true
	shield_changed.emit(shield)
	scrap_changed.emit(scrap)
	time_changed.emit(run_time)
	Events.run_started.emit()


func start_sector(total_nodes: int) -> void:
	nodes_lit = 0
	nodes_total = total_nodes
	nodes_changed.emit(nodes_lit, nodes_total)


func add_scrap(amount: int) -> void:
	if not run_active:
		return
	scrap += amount
	scrap_changed.emit(scrap)


## Returns true when every node in the sector is lit.
func light_node() -> bool:
	nodes_lit += 1
	nodes_changed.emit(nodes_lit, nodes_total)
	return nodes_lit >= nodes_total


## Handles a drain. Returns true while the run continues. Only a drain at
## zero shield ends the run.
func take_drain() -> bool:
	if shield <= 0:
		end_run(false)
		return false
	shield -= 1
	shield_changed.emit(shield)
	return true


func end_run(won: bool) -> void:
	run_active = false
	Events.run_ended.emit(won, scrap, run_time)
