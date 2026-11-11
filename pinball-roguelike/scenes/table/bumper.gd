## Pop bumper. Kicks the ball away from its centre at a fixed speed and
## drops a little scrap.
extends "res://scenes/table/greybox_body.gd"

@export var kick_speed := 340.0
@export var scrap := 1


func _ready() -> void:
	add_to_group("bumpers")
	super()


func on_ball_hit(ball: Ball) -> void:
	var away := (ball.global_position - global_position).normalized()
	ball.linear_velocity = away * kick_speed
	Events.scrap_added.emit(scrap)
	flash()
