## The ball. Caps its own speed so it never tunnels through a flipper, and
## tells whatever it hits about the hit so bumpers, nodes and enemies react.
## Ball power-ups hook in here: Arc Ball and Splitter.
class_name Ball
extends RigidBody2D

const ARC_RANGE := 60.0
const SPLITTER_STREAK := 3
const CLONE_SECONDS := 6.0
const ArcScene := preload("res://scenes/table/arc_flash.gd")

@export var max_speed := 950.0
@export var radius := 5.0
@export var damage := 1
@export var color := Color(0.95, 0.95, 0.98)

## A clone from Splitter. Clones never cost shield and expire on their own.
var is_clone := false
var lifetime := -1.0
## Seconds left during which the speed cap is raised (Overclock shots).
var boost_time := 0.0
var boost_max_speed := 1150.0

var _bumper_streak := 0


func _ready() -> void:
	add_to_group("balls")
	body_entered.connect(_on_body_entered)
	if is_clone:
		color = Color(0.4, 0.95, 1.0)
	queue_redraw()


func _physics_process(delta: float) -> void:
	boost_time = maxf(boost_time - delta, 0.0)
	if lifetime > 0.0:
		lifetime -= delta
		if lifetime <= 0.0:
			queue_free()


func _integrate_forces(state: PhysicsDirectBodyState2D) -> void:
	var cap := boost_max_speed if boost_time > 0.0 else max_speed
	var v := state.linear_velocity
	if v.length() > cap:
		state.linear_velocity = v.normalized() * cap


func _draw() -> void:
	draw_circle(Vector2.ZERO, radius, color)
	draw_circle(Vector2(-1.2, -1.2), radius * 0.35, Color.WHITE)


func _on_body_entered(body: Node) -> void:
	if body.has_method("on_ball_hit"):
		body.on_ball_hit(self)
	if body.is_in_group("flippers"):
		_bumper_streak = 0
	if GameState.has_item(&"arc_ball") and _is_target(body) and not body.is_in_group("bumpers"):
		_arc(body)


## Called by a pop bumper when it fires at this ball. Bumpers trigger on
## their skirt, slightly before contact, so they report hits themselves.
func note_bumper_hit(bumper: Node) -> void:
	if GameState.has_item(&"arc_ball"):
		_arc(bumper)
	_bumper_streak += 1
	if _bumper_streak >= SPLITTER_STREAK and not is_clone and GameState.has_item(&"splitter"):
		_bumper_streak = 0
		_split.call_deferred()


## Targets are hittable table objects (layer 4) and flippers, not walls.
func _is_target(body: Node) -> bool:
	if body.is_in_group("flippers"):
		return true
	return body is CollisionObject2D and (body.collision_layer & 4) != 0


func _arc(hit: Node) -> void:
	var best: Node2D = null
	var best_d := ARC_RANGE
	for enemy in get_tree().get_nodes_in_group("enemies"):
		if enemy == hit or not is_instance_valid(enemy):
			continue
		var d := global_position.distance_to(enemy.global_position)
		if d < best_d:
			best_d = d
			best = enemy
	if best and best.take_damage(1):
		var flash := ArcScene.new()
		flash.from = global_position
		flash.to = best.global_position
		get_parent().add_child(flash)


func _split() -> void:
	if not is_inside_tree():
		return
	var clone: Ball = load("res://scenes/table/Ball.tscn").instantiate()
	clone.is_clone = true
	clone.lifetime = CLONE_SECONDS
	clone.position = position + Vector2(6, 0)
	clone.linear_velocity = linear_velocity.rotated(deg_to_rad(30))
	linear_velocity = linear_velocity.rotated(deg_to_rad(-30))
	get_parent().add_child(clone)
