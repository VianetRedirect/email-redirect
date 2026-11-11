## Standup target. A fixed target that lights when hit and stays lit until
## its bank completes and resets. Belongs to a TargetBank.
extends "res://scenes/table/greybox_body.gd"

signal hit

@export var unlit_color := Color(0.85, 0.85, 0.3)
@export var lit_color := Color(1.0, 1.0, 0.75)
@export var scrap := 1

var lit := false


func _ready() -> void:
	color = unlit_color.darkened(0.35)
	super()


func on_ball_hit(_ball: Ball) -> void:
	flash()
	Events.scrap_added.emit(scrap)
	if lit:
		return
	set_lit(true)
	hit.emit()


func set_lit(value: bool) -> void:
	lit = value
	color = lit_color if lit else unlit_color.darkened(0.35)
	queue_redraw()
