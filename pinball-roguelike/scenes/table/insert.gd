## Lamp insert: a lit arrow on the playfield pointing at a shot. Real tables
## use these to tell the player which shots matter right now. Visual only.
## Modes:
##   PULSE        always pulses (ramps, lanes worth shooting at any time)
##   WHILE_UNLIT  lit while the watched node's `lit` is false (valves)
##   WHILE_OPEN   lit while the gate is open (the lift)
extends Node2D

enum Mode { PULSE, WHILE_UNLIT, WHILE_OPEN }

@export var mode := Mode.PULSE
@export var watch: NodePath
@export var color := Color(1.0, 0.55, 0.2)
@export var size := 6.0

var _t := 0.0
var _gate_open := false


func _ready() -> void:
	z_index = 0
	if mode == Mode.WHILE_OPEN:
		Events.gate_opened.connect(func(): _gate_open = true)


func _process(delta: float) -> void:
	_t += delta
	queue_redraw()


func _is_on() -> bool:
	match mode:
		Mode.WHILE_UNLIT:
			var n := get_node_or_null(watch)
			return n != null and not n.lit
		Mode.WHILE_OPEN:
			return _gate_open
	return true


func _draw() -> void:
	var glow := 0.15
	if _is_on():
		glow = 0.45 + 0.55 * (0.5 + 0.5 * sin(_t * TAU * 1.2))
	var c := Color(0.15, 0.12, 0.1).lerp(color, glow)
	var s := size
	draw_colored_polygon(PackedVector2Array([
		Vector2(0, -s), Vector2(s * 0.8, 0), Vector2(s * 0.3, 0),
		Vector2(s * 0.3, s), Vector2(-s * 0.3, s), Vector2(-s * 0.3, 0), Vector2(-s * 0.8, 0),
	]), c)
