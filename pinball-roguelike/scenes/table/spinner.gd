## Spinner. Awards scrap each time a ball passes through it moving upward.
extends Area2D

@export var scrap := 1


func _ready() -> void:
	body_entered.connect(_on_body_entered)


func _on_body_entered(body: Node) -> void:
	if body is Ball and body.linear_velocity.y < 0.0:
		Events.scrap_added.emit(scrap)


func _draw() -> void:
	draw_line(Vector2(-40, 0), Vector2(40, 0), Color(0.5, 0.8, 0.9, 0.5), 1.0)
