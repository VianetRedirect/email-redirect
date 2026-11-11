## Kickout hole (saucer). A ball that rolls in slowly enough is held for a
## moment, pays scrap, and is then kicked out in a fixed direction. A fast
## ball skips over the top, as on a real table.
extends Area2D

@export var capture_speed := 450.0
@export var hold_seconds := 0.8
@export var eject_velocity := Vector2(260, 40)
@export var scrap := 5
## Seconds after a kickout before the hole can catch again.
@export var rearm_seconds := 1.0

var _busy := false


func _ready() -> void:
	body_entered.connect(_on_body_entered)
	queue_redraw()


func _on_body_entered(body: Node) -> void:
	if _busy or not body is Ball or body.freeze:
		return
	if body.linear_velocity.length() > capture_speed:
		return
	_busy = true
	_hold.call_deferred(body)


func _hold(ball: Ball) -> void:
	if not is_instance_valid(ball):
		_busy = false
		return
	ball.freeze = true
	ball.global_position = global_position
	Events.scrap_added.emit(scrap)
	queue_redraw()
	await get_tree().create_timer(hold_seconds, false).timeout
	if is_instance_valid(ball):
		ball.freeze = false
		ball.linear_velocity = eject_velocity
	await get_tree().create_timer(rearm_seconds, false).timeout
	_busy = false
	queue_redraw()


func _draw() -> void:
	draw_circle(Vector2.ZERO, 8.0, Color(0.02, 0.02, 0.03))
	draw_arc(Vector2.ZERO, 8.0, 0.0, TAU, 20, Color(0.9, 0.5, 1.0) if not _busy else Color(0.4, 0.25, 0.45), 1.0)
