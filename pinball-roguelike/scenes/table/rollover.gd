## Rollover switch in a lane. A ball rolling over it lights it. Lanes with
## lane change can have their lit pattern shifted by the flippers. Belongs
## to a TargetBank.
extends Area2D

signal hit

@export var lamp_offset := Vector2(0, -10)

var lit := false


func _ready() -> void:
	body_entered.connect(_on_body_entered)
	queue_redraw()


func _on_body_entered(body: Node) -> void:
	if not body is Ball:
		return
	Events.scrap_added.emit(1)
	if lit:
		return
	set_lit(true)
	hit.emit()


func set_lit(value: bool) -> void:
	lit = value
	queue_redraw()


func _draw() -> void:
	# The switch wire, and the lamp insert just above it.
	draw_line(Vector2(-3, 0), Vector2(3, 0), Color(0.7, 0.7, 0.75), 1.0)
	var c := Color(0.4, 0.9, 1.0) if lit else Color(0.12, 0.22, 0.28)
	draw_circle(lamp_offset, 3.0, c)
