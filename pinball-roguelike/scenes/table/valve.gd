## Objective node. One hit lights it. When every node in a sector is lit the
## gate opens. In the Sump these are pump valves. Unlit valves are the
## targets, so they pulse brightly; lit valves go dim and steady.
extends "res://scenes/table/greybox_body.gd"

@export var pulse_dim := Color(0.3, 0.7, 0.35)
@export var pulse_bright := Color(0.7, 1.0, 0.6)
@export var lit_color := Color(0.16, 0.3, 0.2)
## Pulses per second.
@export var pulse_rate := 1.5

var lit := false
var _t := 0.0


func _ready() -> void:
	add_to_group("objective_nodes")
	color = pulse_dim
	super()


func _process(delta: float) -> void:
	if lit:
		return
	_t += delta
	var k := 0.5 + 0.5 * sin(_t * TAU * pulse_rate)
	color = pulse_dim.lerp(pulse_bright, k)
	queue_redraw()


func on_ball_hit(_ball: Ball) -> void:
	flash()
	if lit:
		return
	lit = true
	color = lit_color
	queue_redraw()
	Events.node_lit.emit(self)
