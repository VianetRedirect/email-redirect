## One drop target. A hit knocks it below the table: it stops colliding and
## disappears until its bank resets it.
extends "res://scenes/table/greybox_body.gd"

signal dropped

@export var scrap := 2

var down := false


func _ready() -> void:
	color = Color(0.95, 0.6, 0.25)
	super()


func on_ball_hit(_ball: Ball) -> void:
	if down:
		return
	down = true
	set_deferred("collision_layer", 0)
	visible = false
	Events.scrap_added.emit(scrap)
	dropped.emit()


func raise() -> void:
	down = false
	collision_layer = 4
	visible = true
