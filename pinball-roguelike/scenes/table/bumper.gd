## Pop bumper. Kicks the ball away from its centre at a fixed speed, so every
## bumper hit feels the same regardless of how fast the ball arrived.
extends "res://scenes/table/greybox_body.gd"

@export var kick_speed := 340.0
@export var score := 100


func on_ball_hit(ball: Ball) -> void:
	var away := (ball.global_position - global_position).normalized()
	ball.linear_velocity = away * kick_speed
	Events.score_added.emit(score)
	flash()
