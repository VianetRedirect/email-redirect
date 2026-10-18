## Owns the ball, the plunger and touch input for one table.
## Touch: left half of the screen is the left flipper, right half the right.
## While the ball sits in the plunger lane, any touch charges the launch.
extends Node2D

const BallScene := preload("res://scenes/table/Ball.tscn")
## Anything right of this x is the plunger lane.
const LANE_X := 248.0

@export var launch_min := 620.0
@export var launch_max := 780.0
## Seconds of holding to reach launch_max.
@export var charge_time := 0.7
@export var respawn_delay := 0.8

@onready var _spawn: Marker2D = $PlungerSpawn
@onready var _balls: Node2D = $Balls

var _ball: Ball
var _charge := 0.0
var _charging := false
var _touches := {}
var _run_over := false


func _ready() -> void:
	Events.ball_drained.connect(_on_ball_drained)
	GameState.start_run()
	_spawn_ball()


func _process(delta: float) -> void:
	if _charging:
		_charge = minf(_charge + delta / charge_time, 1.0)
		queue_redraw()


func _unhandled_input(event: InputEvent) -> void:
	if _run_over:
		if event.is_pressed():
			get_tree().reload_current_scene()
		return
	if event is InputEventScreenTouch:
		_handle_touch(event)
	elif event.is_action_pressed("launch"):
		_begin_charge()
	elif event.is_action_released("launch"):
		_release_launch()
	elif event.is_action_pressed("nudge"):
		_nudge()


func _draw() -> void:
	if _charging:
		var h := 40.0 * _charge
		draw_rect(Rect2(251, 472 - h, 12, h), Color(0.95, 0.85, 0.3, 0.8))


func _handle_touch(event: InputEventScreenTouch) -> void:
	if event.pressed:
		var action := "launch" if _ball_in_lane() else _side_action(event.position)
		_touches[event.index] = action
		if action == "launch":
			_begin_charge()
		else:
			Input.action_press(action)
	else:
		var action: String = _touches.get(event.index, "")
		_touches.erase(event.index)
		if action == "launch":
			_release_launch()
		elif action != "":
			Input.action_release(action)


func _side_action(pos: Vector2) -> String:
	var half := get_viewport_rect().size.x * 0.5
	return "flipper_left" if pos.x < half else "flipper_right"


func _ball_in_lane() -> bool:
	return is_instance_valid(_ball) and _ball.position.x > LANE_X


func _begin_charge() -> void:
	if _ball_in_lane():
		_charging = true
		_charge = 0.0


func _release_launch() -> void:
	if not _charging:
		return
	_charging = false
	queue_redraw()
	if _ball_in_lane():
		var power: float = lerpf(launch_min, launch_max, _charge)
		_ball.linear_velocity = Vector2(0, -power)
		Events.ball_launched.emit()


func _nudge() -> void:
	if is_instance_valid(_ball):
		_ball.apply_central_impulse(Vector2(randf_range(-40, 40), -70))


func _spawn_ball() -> void:
	_ball = BallScene.instantiate()
	_ball.position = _spawn.position
	_balls.add_child(_ball)


func _on_ball_drained() -> void:
	_charging = false
	for action in _touches.values():
		if action != "launch":
			Input.action_release(action)
	_touches.clear()
	if GameState.lose_ball():
		await get_tree().create_timer(respawn_delay).timeout
		_spawn_ball()
	else:
		_run_over = true
