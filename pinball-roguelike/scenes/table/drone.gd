## Scrap drone. Patrols left and right around its start position. It cannot
## hurt the ball, but it deflects it downward, so it guards whatever is
## above it. Three hits destroy it.
##
## Keep sync_to_physics OFF in Drone.tscn. With it on, moving the drone by
## setting position each physics step desynced its collision body from its
## visible position and fired balls through walls at max speed.
extends AnimatableBody2D

@export var patrol_half_width := 50.0
@export var speed := 40.0
@export var max_hp := 3
@export var scrap_on_kill := 5
## Minimum downward speed given to a ball that touches the drone.
@export var deflect_speed := 160.0
@export var color := Color(0.9, 0.35, 0.3)

var hp: int
var _origin_x: float
var _direction := 1.0


func _ready() -> void:
	hp = max_hp
	_origin_x = position.x
	queue_redraw()


func _physics_process(delta: float) -> void:
	position.x += _direction * speed * delta
	if absf(position.x - _origin_x) >= patrol_half_width:
		position.x = _origin_x + _direction * patrol_half_width
		_direction = -_direction


func _draw() -> void:
	draw_rect(Rect2(-9, -5, 18, 10), color)
	draw_rect(Rect2(-13, -2, 4, 4), color.darkened(0.3))
	draw_rect(Rect2(9, -2, 4, 4), color.darkened(0.3))
	for i in hp:
		draw_rect(Rect2(-7 + i * 5, -9, 3, 2), Color.WHITE)


func on_ball_hit(ball: Ball) -> void:
	hp -= ball.damage
	if ball.linear_velocity.y < deflect_speed:
		ball.linear_velocity.y = deflect_speed
	modulate = Color(2.5, 2.5, 2.5)
	create_tween().tween_property(self, "modulate", Color.WHITE, 0.12)
	queue_redraw()
	if hp <= 0:
		Events.scrap_added.emit(scrap_on_kill)
		Events.enemy_destroyed.emit(self)
		set_deferred("collision_layer", 0)
		queue_free()
