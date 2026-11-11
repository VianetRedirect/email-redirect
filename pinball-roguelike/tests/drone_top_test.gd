## Drops a ball onto the top of a stationary drone from several offsets and
## checks one landing cannot take more than one hit point, and that the
## ball slides off instead of sitting on top. Run:
##   godot --headless --fixed-fps 60 --path . -s res://tests/drone_top_test.gd
extends SceneTree

const OFFSETS := [-6.0, -2.0, 0.0, 2.0, 6.0]
const WATCH_SECONDS := 2.0

var _sector: Node2D
var _drone: Node2D
var _ball: RigidBody2D
var _case := -1
var _t := 0.0
var _results := []
var _fail := false


func _initialize() -> void:
	for n in ["Events", "GameState"]:
		if not root.has_node(n):
			var node: Node = load("res://scripts/autoload/%s.gd" % n.to_snake_case()).new()
			node.name = n
			root.add_child(node)
	var main: Node = load("res://scenes/main/Main.tscn").instantiate()
	root.add_child(main)
	_sector = main.get_node("Sector")


func _next_case() -> void:
	_case += 1
	_t = 0.0
	for b in _sector.get_node("Balls").get_children():
		b.free()
	if is_instance_valid(_drone):
		_drone.free()
	if _case >= OFFSETS.size():
		return
	_drone = load("res://scenes/table/Drone.tscn").instantiate()
	_drone.position = Vector2(135, 300)
	_drone.speed = 0.0
	_sector.add_child(_drone)
	_ball = load("res://scenes/table/Ball.tscn").instantiate()
	_ball.position = Vector2(135 + OFFSETS[_case], 270)
	_sector.get_node("Balls").add_child(_ball)
	_sector.set("_ball", _ball)


func _process(delta: float) -> bool:
	if _case == -1:
		_t += delta
		if _t > 0.3:
			for d in _sector.get_node("Drones").get_children():
				d.free()
			_next_case()
		return false
	if _case >= OFFSETS.size():
		print("RESULT drone_top=%s %s" % [str(_results), "FAIL" if _fail else "PASS"])
		return true
	_t += delta
	if _t >= WATCH_SECONDS:
		var hp_lost: int = 3 - (_drone.hp if is_instance_valid(_drone) else 0)
		var on_top: bool = is_instance_valid(_ball) and absf(_ball.position.x - 135.0) < 12.0 and _ball.position.y < 300.0 and _ball.position.y > 280.0
		_results.append([OFFSETS[_case], hp_lost, on_top])
		if hp_lost > 1 or on_top:
			_fail = true
		_next_case()
	return false
