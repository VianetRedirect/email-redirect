## A flipper. The body is an AnimatableBody2D so the physics engine sees it
## move, but that alone gives a weak shot (the ball only inherits surface
## speed). So each upswing also gives any ball resting on or near the
## flipper one explicit kick along the flipper's normal. The kick grows
## from base to tip, like a real flipper.
##
## Aim works as on a real table. A ball near the base rides the bat through
## its whole stroke and leaves toward the flipper's own side (a backhand).
## A ball near the tip is struck early in the stroke and leaves across the
## table. So the player aims by choosing when to flip as the ball rolls
## down the bat.
##
## Flipper power-ups hook in here: Overclock and Mag Grip.
extends AnimatableBody2D

const OVERCLOCK_HOLD := 1.0
const OVERCLOCK_POWER := 1.4
const OVERCLOCK_BOOST_SECONDS := 0.6
## Mag Grip leaves alone a ball kicked within this many seconds, so it
## never swallows the shot it just made.
const GRIP_IGNORE_AFTER_KICK := 0.3
## Velocity kept per physics step by Mag Grip. 0.8 stops a ball in ~0.2 s.
const GRIP_DAMPING := 0.8

@export var mirrored := false
@export var action := "flipper_left"
@export var length := 38.0
@export var rest_degrees := 25.0
@export var active_degrees := -30.0
## Radians per second. 20 gets rest to active in about 50 ms.
@export var angular_speed := 20.0
## Ball speed along the flipper normal for a hit at the pivot end.
@export var kick_base := 520.0
## Ball speed along the flipper normal for a hit at the tip.
@export var kick_tip := 900.0
## Shot direction in degrees from straight up for a hit at the pivot end.
## Negative leans toward the flipper's own side.
@export var aim_base_degrees := -2.0
## Shot direction for a hit at the tip. Positive leans across the table.
@export var aim_tip_degrees := 30.0
@export var color := Color(0.91, 0.77, 0.28)

@onready var _poly: CollisionPolygon2D = $CollisionPolygon2D

## Ball instance id -> time it was kicked during the current press.
## Cleared on release.
var _kicked := {}
var _clock := 0.0
var _hold_time := 0.0
var overclocked := false
var _press_power := 1.0
var _was_pressed := false


func _ready() -> void:
	add_to_group("flippers")
	var d := _dir()
	_poly.polygon = PackedVector2Array([
		Vector2(-3 * d, -4), Vector2(0, -5), Vector2(length * d, -2),
		Vector2(length * d, 2), Vector2(0, 5), Vector2(-3 * d, 4),
	])
	rotation = deg_to_rad(rest_degrees) * d
	queue_redraw()


func _physics_process(delta: float) -> void:
	_clock += delta
	var pressed := Input.is_action_pressed(action)
	if pressed and not _was_pressed:
		_on_press()
	_was_pressed = pressed
	var target := deg_to_rad(active_degrees if pressed else rest_degrees) * _dir()
	var swinging_up := pressed and not is_equal_approx(rotation, target)
	rotation = move_toward(rotation, target, angular_speed * delta)
	if not pressed:
		_kicked.clear()
		_hold_time = 0.0
	elif swinging_up:
		_kick_balls()
	else:
		_update_overclock(delta)
		if GameState.has_item(&"mag_grip"):
			_grip_balls()


func _on_press() -> void:
	_press_power = 1.0
	if overclocked:
		_press_power = OVERCLOCK_POWER
		overclocked = false
		queue_redraw()


func _update_overclock(delta: float) -> void:
	if overclocked or not GameState.has_item(&"overclock"):
		return
	_hold_time += delta
	if _hold_time >= OVERCLOCK_HOLD:
		overclocked = true
		queue_redraw()


func _kick_balls() -> void:
	for ball in get_tree().get_nodes_in_group("balls"):
		if _kicked.has(ball.get_instance_id()) or ball.freeze:
			continue
		var t := _contact_fraction(ball)
		if t < 0.0:
			continue
		var aim := deg_to_rad(lerpf(aim_base_degrees, aim_tip_degrees, t)) * _dir()
		var normal := Vector2.UP.rotated(aim)
		var speed := lerpf(kick_base, kick_tip, t) * _press_power
		if _press_power > 1.0:
			ball.boost_time = OVERCLOCK_BOOST_SECONDS
		var v: Vector2 = ball.linear_velocity
		var vn := v.dot(normal)
		if vn < speed:
			ball.linear_velocity = v + normal * (speed - vn)
		_kicked[ball.get_instance_id()] = _clock


func _grip_balls() -> void:
	for ball in get_tree().get_nodes_in_group("balls"):
		var kicked_at: float = _kicked.get(ball.get_instance_id(), -INF)
		if _clock - kicked_at < GRIP_IGNORE_AFTER_KICK:
			continue
		if _contact_fraction(ball) < 0.0:
			continue
		ball.linear_velocity *= GRIP_DAMPING


## How far along the flipper a ball sits, 0 at the pivot and 1 at the tip,
## or -1 if the ball is not on or just above the flipper.
func _contact_fraction(ball: Node2D) -> float:
	# The zone reaches a little behind the pivot and well above the bat, so
	# a ball resting where the inlane meets the flipper still gets a full
	# shot instead of a weak shove from the bat's body.
	var local: Vector2 = to_local(ball.global_position)
	var along := local.x * _dir()
	if along < -(ball.radius + 3.0) or along > length + 4.0:
		return -1.0
	if local.y > 2.0 or local.y < -(ball.radius + 11.0):
		return -1.0
	return clampf(along / length, 0.0, 1.0)


func _draw() -> void:
	var c := color.lightened(0.5) if overclocked else color
	draw_colored_polygon(_poly.polygon, c)
	draw_circle(Vector2.ZERO, 3.0, c.darkened(0.3))


func _dir() -> float:
	return -1.0 if mirrored else 1.0
