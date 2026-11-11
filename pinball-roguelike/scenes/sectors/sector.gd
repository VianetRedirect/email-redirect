## Runs one sector: ball, plunger, touch input, camera, objective and the
## drain rule. The run itself (items, moving between sectors, restarting)
## belongs to main.gd. The Sump is sector one, so a drain here costs a shield and
## relaunches from the plunger. Higher sectors will drop the ball into the
## sector below instead (see docs/02-run-rules-and-sector-one.md).
##
## Touch: left half of the screen is the left flippers, right half the
## right. While the ball sits in the plunger lane, any touch charges the
## launch.
extends Node2D

const BallScene := preload("res://scenes/table/Ball.tscn")

## The plunger lane is everything right of LANE_X and below LANE_TOP_Y.
@export var lane_x := 248.0
@export var lane_top_y := 336.0
@export var sector_height := 720.0
@export var launch_min := 620.0
@export var launch_max := 780.0
## Seconds of holding to reach launch_max.
@export var charge_time := 0.7
@export var respawn_delay := 0.8
## A ball slower than stuck_speed for this many seconds outside the plunger
## lane gets a free nudge, like the ball search on a real table.
@export var stuck_seconds := 4.0
@export var stuck_speed := 4.0
## How quickly the camera catches up with the ball. Higher is tighter.
@export var camera_follow := 8.0

@onready var _spawn: Marker2D = $PlungerSpawn
@onready var _balls: Node2D = $Balls
@onready var _camera: Camera2D = $Camera

var _ball: Ball
var _charge := 0.0
var _charging := false
var _touches := {}
var _run_over := false
var _gate_open := false
var _last_hint := 0
var _view_height := 480.0
var _still_time := 0.0


func _ready() -> void:
	_view_height = get_viewport_rect().size.y
	Events.ball_drained.connect(_on_ball_drained)
	Events.node_lit.connect(_on_node_lit)
	Events.sector_cleared.connect(_on_sector_cleared)
	GameState.start_sector(get_tree().get_nodes_in_group("objective_nodes").size())
	_camera.position = Vector2(get_viewport_rect().size.x * 0.5, sector_height - _view_height * 0.5)
	_spawn_ball()


func _process(delta: float) -> void:
	if _charging:
		_charge = minf(_charge + delta / charge_time, 1.0)
		queue_redraw()
	_update_camera(delta)
	_update_hint()
	_ball_search(delta)
	_escape_check()


func _unhandled_input(event: InputEvent) -> void:
	if _run_over:
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
		draw_rect(Rect2(lane_x + 3, sector_height - 8 - h, 12, h), Color(0.95, 0.85, 0.3, 0.8))


func _update_camera(delta: float) -> void:
	if not is_instance_valid(_ball):
		return
	var half := _view_height * 0.5
	var target := clampf(_ball.position.y, half, sector_height - half)
	_camera.position.y = lerpf(_camera.position.y, target, 1.0 - exp(-camera_follow * delta))


## Tells the HUD whether the current objective is off screen, and where.
func _update_hint() -> void:
	var top := _camera.position.y - _view_height * 0.5
	var bottom := top + _view_height
	var hint := 0
	var targets: Array[float] = []
	if _gate_open:
		targets.append($Lift.position.y)
	else:
		for node in get_tree().get_nodes_in_group("objective_nodes"):
			if not node.lit:
				targets.append(node.position.y)
	var any_visible := false
	for y in targets:
		if y >= top and y <= bottom:
			any_visible = true
	if not any_visible and not targets.is_empty():
		hint = -1 if targets[0] < top else 1
	if hint != _last_hint:
		_last_hint = hint
		Events.objective_hint.emit(hint)


func _ball_search(delta: float) -> void:
	if not is_instance_valid(_ball) or _ball_in_lane():
		_still_time = 0.0
		return
	# A ball cradled on a held flipper is the player's choice, not a jam.
	var cradled := Input.is_action_pressed("flipper_left") or Input.is_action_pressed("flipper_right")
	if _ball.linear_velocity.length() > stuck_speed or cradled:
		_still_time = 0.0
		return
	_still_time += delta
	if _still_time >= stuck_seconds:
		_still_time = 0.0
		_ball.linear_velocity = Vector2(randf_range(-120, 120), -160)


## Safety net. A ball outside the sector is a physics bug, not a player
## mistake, so it respawns at the plunger with no shield lost.
func _escape_check() -> void:
	if not is_instance_valid(_ball):
		return
	var p := _ball.position
	if p.x < -8.0 or p.x > 278.0 or p.y < -8.0 or p.y > sector_height + 8.0:
		push_warning("Ball left the sector at %s, respawning" % str(p))
		Events.ball_escaped.emit()
		_ball.queue_free()
		_ball = null
		_release_all_touches()
		_spawn_ball()


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
	return is_instance_valid(_ball) and _ball.position.x > lane_x and _ball.position.y > lane_top_y


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


func _release_all_touches() -> void:
	_charging = false
	for action in _touches.values():
		if action != "launch":
			Input.action_release(action)
	_touches.clear()


func _on_node_lit(_node: Node2D) -> void:
	if GameState.light_node():
		_gate_open = true
		Events.gate_opened.emit()


func _on_ball_drained() -> void:
	_release_all_touches()
	if GameState.take_drain():
		await get_tree().create_timer(respawn_delay, false).timeout
		if is_inside_tree() and not _run_over:
			_spawn_ball()
	else:
		_run_over = true


func _on_sector_cleared() -> void:
	_release_all_touches()
	_run_over = true
