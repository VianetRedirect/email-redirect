## Pop bumper. On a real table the ball presses a skirt around the bumper,
## which closes a switch, which fires a solenoid that slams a metal ring
## down onto the ball and throws it away. So the ball leaves much faster
## than it arrived, even from a gentle touch.
##
## Here the skirt is a radius slightly larger than the body. Any ball
## inside it is thrown straight away from the centre at kick_speed, then
## the coil rests for cooldown seconds.
extends "res://scenes/table/greybox_body.gd"

@export var kick_speed := 560.0
## How far outside the body the skirt reaches, in pixels.
@export var skirt := 2.0
@export var cooldown := 0.1
## Random spread of the throw direction, in degrees.
@export var scatter_degrees := 8.0
@export var scrap := 1
## Completing the top lanes makes every bumper super for this long:
## harder throws and triple scrap.
@export var super_seconds := 20.0

var _rest := 0.0
var _super := 0.0
var _pulse := 0.0
var _body_radius := 11.0


func _ready() -> void:
	add_to_group("bumpers")
	_body_radius = $CollisionShape2D.shape.radius
	Events.bank_done.connect(func(id): if id == &"lanes_top": _super = super_seconds)
	super()


func _physics_process(delta: float) -> void:
	_rest = maxf(_rest - delta, 0.0)
	if _super > 0.0:
		_super = maxf(_super - delta, 0.0)
		queue_redraw()
	if _pulse > 0.0:
		_pulse = maxf(_pulse - delta * 6.0, 0.0)
		queue_redraw()
	if _rest > 0.0:
		return
	for ball in get_tree().get_nodes_in_group("balls"):
		if ball.freeze:
			continue
		var offset: Vector2 = ball.global_position - global_position
		if offset.length() <= _body_radius + ball.radius + skirt:
			_fire(ball, offset)
			return


func _fire(ball: Ball, offset: Vector2) -> void:
	var away := offset.normalized() if offset.length() > 0.01 else Vector2.DOWN
	away = away.rotated(deg_to_rad(randf_range(-scatter_degrees, scatter_degrees)))
	var boost := 1.2 if _super > 0.0 else 1.0
	ball.linear_velocity = away * kick_speed * boost
	ball.note_bumper_hit(self)
	Events.scrap_added.emit(scrap * (3 if _super > 0.0 else 1))
	_rest = cooldown
	_pulse = 1.0
	queue_redraw()


func _draw() -> void:
	# Skirt, body, and the ring that flashes and shrinks when the coil fires.
	draw_circle(Vector2.ZERO, _body_radius + skirt, color.darkened(0.5))
	draw_circle(Vector2.ZERO, _body_radius - 1.0, Color(1.0, 0.45, 0.9) if _super > 0.0 else color)
	var ring := lerpf(_body_radius - 4.0, _body_radius - 7.0, _pulse)
	draw_arc(Vector2.ZERO, ring, 0.0, TAU, 24, Color.WHITE.lerp(color.lightened(0.4), 1.0 - _pulse), 1.5 + _pulse)
