## Measures how hard a flip launches the ball from several points on the
## left flipper. It records the peak upward speed and converts it to the
## height the ball would climb in open space (v^2 / 2g), because ledges
## above the flippers block a direct height measurement. Run:
##   godot --headless --fixed-fps 60 --path . -s res://tests/flipper_power_test.gd
## Pass: the best shot would climb at least MIN_BEST_CLIMB px. The screen
## is 480 px tall and the flippers sit 55 px above the bottom.
extends SceneTree

const MIN_BEST_CLIMB := 430.0
const FRACTIONS := [0.3, 0.5, 0.7, 0.9]

var _table: Node2D
var _flipper: Node2D
var _ball: RigidBody2D
var _case := -1
var _t := 0.0
var _flipped := false
var _start_y := 0.0
var _peak_up := 0.0
var _results := []


func _initialize() -> void:
	for n in ["Events", "GameState"]:
		if not root.has_node(n):
			var node: Node = load("res://scripts/autoload/%s.gd" % n.to_snake_case()).new()
			node.name = n
			root.add_child(node)
	var main: Node = load("res://scenes/main/Main.tscn").instantiate()
	root.add_child(main)
	_table = main.get_node("Sector")
	_flipper = _table.get_node("FlipperLeft")


func _next_case() -> void:
	_case += 1
	_t = 0.0
	_flipped = false
	Input.action_release("flipper_left")
	for b in _table.get_node("Balls").get_children():
		b.free()
	if _case >= FRACTIONS.size():
		return
	_ball = load("res://scenes/table/Ball.tscn").instantiate()
	var length: float = _flipper.length
	# drop the ball just above the resting flipper at the chosen fraction
	var local := Vector2(length * FRACTIONS[_case], -10.0)
	_ball.position = _flipper.position + local.rotated(_flipper.rotation)
	_table.get_node("Balls").add_child(_ball)
	_table.set("_ball", _ball)


func _process(delta: float) -> bool:
	if _case == -1:
		_t += delta
		if _t > 0.5:
			_next_case()
		return false
	if _case >= FRACTIONS.size():
		var best := 0.0
		for r in _results:
			best = maxf(best, r)
		print("RESULT climbs=%s best=%.0f need>=%.0f %s" % [str(_results), best, MIN_BEST_CLIMB, "PASS" if best >= MIN_BEST_CLIMB else "FAIL"])
		return true
	_t += delta
	if not is_instance_valid(_ball):
		_results.append(0.0)
		_next_case()
		return false
	if not _flipped and _t > 0.3:
		# ball has settled and is rolling down the flipper: flip now
		_flipped = true
		_start_y = _ball.position.y
		_peak_up = 0.0
		Input.action_press("flipper_left")
	if _flipped:
		_peak_up = maxf(_peak_up, -_ball.linear_velocity.y)
		if _t > 0.8:
			var g: float = ProjectSettings.get_setting("physics/2d/default_gravity")
			_results.append(round(_peak_up * _peak_up / (2.0 * g)))
			_next_case()
	return false
