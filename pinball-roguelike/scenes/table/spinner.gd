## Spinner: a flat plate hung on a horizontal wire across a lane. A ball
## passing under it sets it spinning, faster for a faster ball. It keeps
## spinning after the ball has gone and pays scrap for every half turn
## until friction stops it.
extends Area2D

## Ball speed in px/s that produces one turn per second.
@export var speed_per_turn := 150.0
## Fraction of spin lost per second.
@export var friction := 1.5
@export var scrap_per_half_turn := 1
@export var half_width := 25.0

var _spin := 0.0
var _angle := 0.0
var _half_turns_paid := 0


func _ready() -> void:
	body_entered.connect(_on_body_entered)


func _on_body_entered(body: Node) -> void:
	if body is Ball:
		_spin = maxf(_spin, absf(body.linear_velocity.y) / speed_per_turn)


func _process(delta: float) -> void:
	if _spin < 0.3:
		_spin = 0.0
		return
	_angle += _spin * TAU * delta
	_spin *= exp(-friction * delta)
	var half_turns := int(_angle / PI)
	if half_turns > _half_turns_paid:
		Events.scrap_added.emit((half_turns - _half_turns_paid) * scrap_per_half_turn)
		_half_turns_paid = half_turns
	queue_redraw()


func _draw() -> void:
	# The plate seen edge-on to face-on as it turns.
	var h := absf(cos(_angle)) * 4.0 + 0.5
	draw_rect(Rect2(-half_width, -h, half_width * 2.0, h * 2.0), Color(0.5, 0.8, 0.9, 0.7))
	draw_line(Vector2(-half_width - 3, 0), Vector2(half_width + 3, 0), Color(0.8, 0.85, 0.9), 1.0)
