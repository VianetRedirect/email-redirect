## The ball. Caps its own speed so it never tunnels through a flipper, and
## tells whatever it hits about the hit so bumpers and targets can react.
class_name Ball
extends RigidBody2D

@export var max_speed := 850.0
@export var radius := 4.0
@export var damage := 1
@export var color := Color(0.95, 0.95, 0.98)


func _ready() -> void:
	body_entered.connect(_on_body_entered)
	queue_redraw()


func _integrate_forces(state: PhysicsDirectBodyState2D) -> void:
	var v := state.linear_velocity
	if v.length() > max_speed:
		state.linear_velocity = v.normalized() * max_speed


func _draw() -> void:
	draw_circle(Vector2.ZERO, radius, color)
	draw_circle(Vector2(-1.2, -1.2), radius * 0.35, Color.WHITE)


func _on_body_entered(body: Node) -> void:
	if body.has_method("on_ball_hit"):
		body.on_ball_hit(self)
