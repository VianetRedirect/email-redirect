## The sector gate. A wall until every node is lit, then it disappears and
## the lift behind it becomes reachable.
extends "res://scenes/table/greybox_body.gd"


func _ready() -> void:
	color = Color(0.85, 0.6, 0.2)
	super()
	Events.gate_opened.connect(_open)


func _open() -> void:
	set_deferred("collision_layer", 0)
	visible = false
