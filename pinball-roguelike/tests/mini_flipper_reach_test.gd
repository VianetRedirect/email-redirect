## Checks that valve 3 can be hit with an aimed shot from the mini flipper.
## Drops a ball onto the left ledge, lets it roll onto the mini flipper,
## and flips after a range of delays. A shot "reaches" if the ball comes
## close enough to touch valve 3. The upper drone is removed so the
## test measures geometry, not drone timing. Run:
##   godot --headless --fixed-fps 60 --path . -s res://tests/mini_flipper_reach_test.gd
## Pass: at least MIN_REACHING of the delays reach valve 3.
extends SceneTree

const DELAYS := [0.10, 0.15, 0.20, 0.25, 0.30, 0.35, 0.40, 0.45, 0.50, 0.60, 0.70, 0.80]
const MIN_REACHING := 3
const DROP_POINT := Vector2(30, 205)

var _sector: Node2D
var _valve: Node2D
var _ball: RigidBody2D
var _case := -1
var _t := 0.0
var _flipped := false
var _reached := false
var _closest := 9999.0
var _results := []


func _initialize() -> void:
	for n in ["Events", "GameState"]:
		if not root.has_node(n):
			var node: Node = load("res://scripts/autoload/%s.gd" % n.to_snake_case()).new()
			node.name = n
			root.add_child(node)
	var main: Node = load("res://scenes/main/Main.tscn").instantiate()
	root.add_child(main)
	_sector = main.get_node("Sector")
	_valve = _sector.get_node("Valves/Valve3")


func _next_case() -> void:
	_case += 1
	_t = 0.0
	_flipped = false
	_reached = false
	_closest = 9999.0
	Input.action_release("flipper_left")
	for b in _sector.get_node("Balls").get_children():
		b.free()
	if _case >= DELAYS.size():
		return
	_ball = load("res://scenes/table/Ball.tscn").instantiate()
	_ball.position = DROP_POINT
	_sector.get_node("Balls").add_child(_ball)
	_sector.set("_ball", _ball)


func _process(delta: float) -> bool:
	if _case == -1:
		_t += delta
		if _t > 0.3:
			_sector.get_node("Drones/DroneUpper").free()
			_next_case()
		return false
	if _case >= DELAYS.size():
		var hits := 0
		for r in _results:
			if r[1]:
				hits += 1
		print("RESULT reach=%s hits=%d need>=%d %s" % [str(_results), hits, MIN_REACHING, "PASS" if hits >= MIN_REACHING else "FAIL"])
		return true
	_t += delta
	if not is_instance_valid(_ball):
		_results.append([DELAYS[_case], false, -1])
		_next_case()
		return false
	var mini: Node2D = _sector.get_node("FlipperMini")
	var on_flipper := _ball.position.distance_to(mini.position) < 26.0
	if not _flipped and on_flipper:
		_t = 0.0
		_flipped = true
	if _flipped and _t >= DELAYS[_case] and not Input.is_action_pressed("flipper_left"):
		Input.action_press("flipper_left")
	_closest = minf(_closest, _ball.position.distance_to(_valve.position))
	var touch: float = _valve.get_node("CollisionShape2D").shape.radius + _ball.radius + 1.0
	if _closest < touch:
		_reached = true
	if (_flipped and _t > DELAYS[_case] + 1.5) or _t > 4.0:
		_results.append([DELAYS[_case], _reached, roundi(_closest)])
		_next_case()
	return false
