## Slingshot. On a real table a hit on the rubber face closes leaf switches
## that fire a kicker arm, which slaps the ball away. Like a pop bumper,
## the ball leaves faster than it arrived. We set a fixed exit speed with a
## little scatter, so two facing slingshots still cannot trap the ball in
## an endless volley: the ball always loses height to gravity between them.
## Set the normal in the inspector to point into the play field.
extends "res://scenes/table/greybox_body.gd"

@export var normal := Vector2(1, -0.6)
@export var kick_speed := 470.0
## Max random deviation of the exit direction, in degrees.
@export var scatter_degrees := 10.0
## Seconds the kicker needs before it can fire again.
@export var cooldown := 0.08
@export var scrap := 1

var _rest := 0.0
var _arm := 0.0


func _physics_process(delta: float) -> void:
	_rest = maxf(_rest - delta, 0.0)
	if _arm > 0.0:
		_arm = maxf(_arm - delta * 8.0, 0.0)
		queue_redraw()


func on_ball_hit(ball: Ball) -> void:
	if _rest > 0.0:
		return
	_rest = cooldown
	var n := normal.normalized().rotated(deg_to_rad(randf_range(-scatter_degrees, scatter_degrees)))
	ball.linear_velocity = n * kick_speed
	Events.scrap_added.emit(scrap)
	_arm = 1.0
	flash()


func _draw() -> void:
	super()
	# The kicker arm pops out from the rubber face for a moment.
	if _arm > 0.0:
		var poly: PackedVector2Array = $CollisionPolygon2D.polygon
		var local_n := (normal.normalized() * Vector2(scale.x, 1.0)).normalized()
		var a: Vector2 = poly[0].lerp(poly[1], 0.35)
		var b: Vector2 = poly[0].lerp(poly[1], 0.65)
		var push := local_n * 3.0 * _arm
		draw_line(a + push, b + push, Color.WHITE, 2.0)
