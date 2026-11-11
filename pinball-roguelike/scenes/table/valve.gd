## Objective node. One hit lights it. When every node in a sector is lit the
## gate opens. In the Sump these are pump valves.
extends "res://scenes/table/greybox_body.gd"

@export var unlit_color := Color(0.25, 0.45, 0.3)
@export var lit_color := Color(0.4, 1.0, 0.5)

var lit := false


func _ready() -> void:
	add_to_group("objective_nodes")
	color = unlit_color
	super()


func on_ball_hit(_ball: Ball) -> void:
	flash()
	if lit:
		return
	lit = true
	color = lit_color
	queue_redraw()
	Events.node_lit.emit(self)
