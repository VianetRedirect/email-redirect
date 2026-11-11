## Kickback at the bottom of an outlane. While lit, a ball that reaches it
## is fired straight back up the lane into play, and the kickback goes
## dark. Completing the bank named in relight_bank relights it: &"drop"
## for the drop target bank, or a TargetBank id such as &"lanes_low". A
## dark kickback lets the ball drain.
extends Area2D

@export var kick_velocity := Vector2(25, -850)
@export var lit := true
@export var relight_bank := &"lanes_low"


func _ready() -> void:
	body_entered.connect(_on_body_entered)
	if relight_bank == &"drop":
		Events.bank_completed.connect(relight)
	else:
		Events.bank_done.connect(func(id): if id == relight_bank: relight())
	queue_redraw()


func relight() -> void:
	lit = true
	queue_redraw()


func _on_body_entered(body: Node) -> void:
	if not body is Ball or not lit:
		return
	lit = false
	queue_redraw()
	_kick.call_deferred(body)
	Events.toast.emit("KICKBACK")


func _kick(ball: Ball) -> void:
	if not is_instance_valid(ball):
		return
	ball.global_position.x = global_position.x
	ball.linear_velocity = kick_velocity


func _draw() -> void:
	var c := Color(1.0, 0.85, 0.2) if lit else Color(0.3, 0.27, 0.15)
	draw_rect(Rect2(-7, -3, 14, 6), c)
	draw_colored_polygon(PackedVector2Array([Vector2(-4, -5), Vector2(4, -5), Vector2(0, -10)]), c)
