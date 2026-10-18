## Slingshot. Sends the ball out along its normal at a fixed speed with a
## little angular scatter. A fixed exit speed, like a real rubber slingshot,
## means a volley between two facing slingshots loses height to gravity and
## dies out instead of repeating forever.
## Set the normal in the inspector to point into the play field.
extends "res://scenes/table/greybox_body.gd"

@export var normal := Vector2(1, -0.6)
@export var kick_speed := 240.0
## Max random deviation of the exit direction, in degrees.
@export var scatter_degrees := 12.0
@export var score := 25


func on_ball_hit(ball: Ball) -> void:
	var n := normal.normalized().rotated(deg_to_rad(randf_range(-scatter_degrees, scatter_degrees)))
	ball.linear_velocity = n * kick_speed
	Events.score_added.emit(score)
	flash()
