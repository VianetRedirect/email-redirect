## A flipper. The body is an AnimatableBody2D so the physics engine sees it
## move, but that alone gives a weak shot (the ball only inherits surface
## speed). So each upswing also gives any ball resting on or near the
## flipper one explicit kick along the flipper's normal. The kick grows
## from base to tip, like a real flipper, and its direction depends on the
## flipper's angle at that instant, so timing aims the shot.
extends AnimatableBody2D

@export var mirrored := false
@export var action := "flipper_left"
@export var length := 28.0
@export var rest_degrees := 25.0
@export var active_degrees := -30.0
## Radians per second. 20 gets rest to active in about 50 ms.
@export var angular_speed := 20.0
## Ball speed along the flipper normal for a hit at the pivot end.
@export var kick_base := 520.0
## Ball speed along the flipper normal for a hit at the tip.
@export var kick_tip := 900.0
@export var color := Color(0.91, 0.77, 0.28)

@onready var _poly: CollisionPolygon2D = $CollisionPolygon2D

## Balls already kicked during the current press. Cleared on release.
var _kicked := {}


func _ready() -> void:
	var d := _dir()
	_poly.polygon = PackedVector2Array([
		Vector2(-3 * d, -4), Vector2(0, -5), Vector2(length * d, -2),
		Vector2(length * d, 2), Vector2(0, 5), Vector2(-3 * d, 4),
	])
	rotation = deg_to_rad(rest_degrees) * d
	queue_redraw()


func _physics_process(delta: float) -> void:
	var pressed := Input.is_action_pressed(action)
	var target := deg_to_rad(active_degrees if pressed else rest_degrees) * _dir()
	var swinging_up := pressed and not is_equal_approx(rotation, target)
	rotation = move_toward(rotation, target, angular_speed * delta)
	if not pressed:
		_kicked.clear()
	elif swinging_up:
		_kick_balls()


func _kick_balls() -> void:
	var d := _dir()
	var normal := Vector2.UP.rotated(global_rotation)
	for ball in get_tree().get_nodes_in_group("balls"):
		if _kicked.has(ball.get_instance_id()):
			continue
		var local: Vector2 = to_local(ball.global_position)
		var along := local.x * d
		if along < -3.0 or along > length + 4.0:
			continue
		if local.y > 2.0 or local.y < -(ball.radius + 8.0):
			continue
		var t := clampf(along / length, 0.0, 1.0)
		var speed := lerpf(kick_base, kick_tip, t)
		var v: Vector2 = ball.linear_velocity
		var vn := v.dot(normal)
		if vn < speed:
			ball.linear_velocity = v + normal * (speed - vn)
		_kicked[ball.get_instance_id()] = true


func _draw() -> void:
	draw_colored_polygon(_poly.polygon, color)
	draw_circle(Vector2.ZERO, 3.0, color.darkened(0.3))


func _dir() -> float:
	return -1.0 if mirrored else 1.0
