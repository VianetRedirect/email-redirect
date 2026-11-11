## Scrap drone. Patrols left and right around its start position. It cannot
## hurt the ball. A ball hitting it from below is deflected downward, so the
## drone guards whatever is above it. A ball landing on top is pushed off
## sideways. Three hits destroy it, with a short cooldown between hits so
## one bounce cannot count several times.
##
## Keep sync_to_physics OFF in Drone.tscn. With it on, moving the drone by
## setting position each physics step desynced its collision body from its
## visible position and fired balls through walls at max speed.
extends AnimatableBody2D

@export var patrol_half_width := 50.0
@export var speed := 40.0
@export var max_hp := 3
@export var scrap_on_kill := 5
## Minimum downward speed given to a ball that hits the drone from below.
@export var deflect_speed := 160.0
## Sideways speed given to a ball that lands on top.
@export var slide_off_speed := 150.0
## Seconds after a hit during which further hits do no damage.
@export var hit_cooldown := 0.35
@export var color := Color(0.9, 0.35, 0.3)

var hp: int
var _origin_x: float
var _direction := 1.0
var _cooldown := 0.0


func _ready() -> void:
	add_to_group("enemies")
	hp = max_hp
	_origin_x = position.x
	queue_redraw()


func _physics_process(delta: float) -> void:
	_cooldown = maxf(_cooldown - delta, 0.0)
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
	var rel := ball.global_position - global_position
	if rel.y < -4.0:
		# Landed on top: slide off the nearer side with a small hop.
		var side := signf(rel.x) if absf(rel.x) > 0.5 else (1.0 if randf() < 0.5 else -1.0)
		ball.linear_velocity = Vector2(side * slide_off_speed, -60.0)
	elif ball.linear_velocity.y < deflect_speed:
		ball.linear_velocity.y = deflect_speed
	take_damage(ball.damage)


## Damage from any source: the ball, or power-ups such as Arc Ball.
## Returns true if the damage landed (not blocked by the hit cooldown).
func take_damage(amount: int) -> bool:
	if _cooldown > 0.0 or hp <= 0:
		return false
	_cooldown = hit_cooldown
	hp -= amount
	modulate = Color(2.5, 2.5, 2.5)
	create_tween().tween_property(self, "modulate", Color.WHITE, 0.12)
	queue_redraw()
	if hp <= 0:
		Events.scrap_added.emit(scrap_on_kill)
		Events.enemy_destroyed.emit(self)
		set_deferred("collision_layer", 0)
		queue_free()
	return true
