## Gate across the top of an outlane. While closed it is a solid bar that
## rolls the ball into the inlane next to it. It closes for closed_seconds
## each time a ball is launched and again each time a drop target bank is
## completed, then swings open and the outlane becomes a drain. It blinks for
## the last few seconds as a warning.
extends StaticBody2D

@export var closed_seconds := 20.0
@export var warn_seconds := 4.0
@export var color := Color(1.0, 0.85, 0.2)

var _time_left := 0.0


func _ready() -> void:
	Events.bank_completed.connect(close)
	Events.ball_launched.connect(close)
	close()


func close() -> void:
	_time_left = closed_seconds
	collision_layer = 1
	visible = true
	queue_redraw()


func is_closed() -> bool:
	return _time_left > 0.0


func _physics_process(delta: float) -> void:
	if _time_left <= 0.0:
		return
	_time_left -= delta
	if _time_left <= 0.0:
		collision_layer = 0
		visible = false
	elif _time_left < warn_seconds:
		visible = fmod(_time_left, 0.4) > 0.2


func _draw() -> void:
	draw_colored_polygon($CollisionPolygon2D.polygon, color)
