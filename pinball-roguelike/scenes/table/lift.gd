## The lift behind the gate. The main ball entering it clears the sector.
## A Splitter clone entering it just vanishes.
extends Area2D

var _cleared := false


func _ready() -> void:
	body_entered.connect(_on_body_entered)


func _on_body_entered(body: Node) -> void:
	if not body is Ball:
		return
	body.queue_free()
	if body.is_clone or _cleared:
		return
	_cleared = true
	Events.sector_cleared.emit()
