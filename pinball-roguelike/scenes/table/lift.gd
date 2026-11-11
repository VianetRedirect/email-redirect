## The lift behind the gate. A ball that enters has cleared the sector.
extends Area2D


func _ready() -> void:
	body_entered.connect(_on_body_entered)


func _on_body_entered(body: Node) -> void:
	if body is Ball:
		body.queue_free()
		Events.sector_cleared.emit()
