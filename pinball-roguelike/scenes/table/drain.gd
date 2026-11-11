## The drain under the flippers. A ball that enters is lost. Losing a
## Splitter clone costs nothing.
extends Area2D


func _ready() -> void:
	body_entered.connect(_on_body_entered)


func _on_body_entered(body: Node) -> void:
	if body is Ball:
		body.queue_free()
		if not body.is_clone:
			Events.ball_drained.emit()
