## Placeholder enemy. Takes damage from the ball and disappears at zero hp.
## In Phase 2 this becomes the base class for real enemies.
extends "res://scenes/table/greybox_body.gd"

@export var max_hp := 3
@export var score_per_hit := 50
@export var score_on_kill := 500

var hp: int


func _ready() -> void:
	super()
	hp = max_hp
	color = Color(0.85, 0.3, 0.3)


func on_ball_hit(ball: Ball) -> void:
	hp -= ball.damage
	Events.score_added.emit(score_per_hit)
	Events.target_hit.emit(self, hp)
	flash()
	if hp <= 0:
		Events.score_added.emit(score_on_kill)
		Events.target_destroyed.emit(self)
		set_deferred("collision_layer", 0)
		queue_free()
