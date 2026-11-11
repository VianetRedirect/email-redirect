## A bank of drop targets. When every target is down the bank pays a bonus,
## tells the table (which re-arms the outlane saves), and after a short
## pause pops all its targets back up.
extends Node2D

@export var bonus_scrap := 10
@export var reset_delay := 1.0


func _ready() -> void:
	for target in get_children():
		target.dropped.connect(_on_target_dropped)


func _on_target_dropped() -> void:
	for target in get_children():
		if not target.down:
			return
	Events.scrap_added.emit(bonus_scrap)
	Events.bank_completed.emit()
	await get_tree().create_timer(reset_delay, false).timeout
	for target in get_children():
		target.raise()
