## Fires shots from ten points along each flipper and records what each
## shot touches in its first 2.5 seconds. Proves every target on the table
## can be reached by an aimed shot, and prints a map for layout work.
##   godot --headless --fixed-fps 60 --path . -s res://tests/shot_map_test.gd
## Drones are removed so the map shows geometry, not patrol timing.
extends SceneTree

const FRACTIONS := [0.05, 0.15, 0.25, 0.35, 0.45, 0.55, 0.65, 0.75, 0.85, 0.95]
const SHOT_SECONDS := 2.5
## Target -> flippers that must be able to reach it.
const REQUIRED := {
	"ramp_right": ["FlipperLeft"],
	"ramp_left": ["FlipperRight"],
	"drop": ["FlipperLeft", "FlipperRight"],
	"Valve1": ["FlipperLeft", "FlipperRight"],
	"Valve2": ["FlipperLeft", "FlipperRight"],
	"standups": ["FlipperLeft", "FlipperRight", "FlipperUpperLeft", "FlipperUpperRight"],
	"spinner_left": ["FlipperLeft", "FlipperRight"],
	"spinner_right": ["FlipperLeft", "FlipperRight"],
	"lanes_top": ["FlipperUpperLeft", "FlipperUpperRight"],
	"Valve3": ["FlipperUpperLeft", "FlipperUpperRight"],
	"lift": ["FlipperUpperLeft", "FlipperUpperRight"],
	"saucer": ["FlipperUpperLeft", "FlipperUpperRight"],
	"bumper": ["FlipperUpperLeft", "FlipperUpperRight"],
}
const FLIPPERS := ["FlipperLeft", "FlipperRight", "FlipperUpperLeft", "FlipperUpperRight"]

var _main: Node
var _events: Node
var _t := 0.0
var _case := -1
var _cases := []
var _ball: RigidBody2D
var _flipped := false
var _hits: Array[String] = []
var _map := {}
var _drop_was_down := false


func _initialize() -> void:
	seed(1234)
	for n in ["Events", "GameState"]:
		if not root.has_node(n):
			var node: Node = load("res://scripts/autoload/%s.gd" % n.to_snake_case()).new()
			node.name = n
			root.add_child(node)
	_events = root.get_node("Events")
	_main = load("res://scenes/main/Main.tscn").instantiate()
	root.add_child(_main)
	_events.node_lit.connect(func(n): _hit(String(n.name)))
	# Each shot twice: once with the drop bank up, once with it down, because
	# valve 1 sits behind the bank.
	for down in [false, true]:
		for f in FLIPPERS:
			for fr in FRACTIONS:
				_cases.append([f, fr, down])


func _s() -> Node:
	return _main.get_node("Sector")


func _hit(what: String) -> void:
	if not _hits.has(what):
		_hits.append(what)


func _reset_table() -> void:
	var s := _s()
	for v in s.get_node("Valves").get_children():
		v.lit = false
	for t in s.get_node("DropBank").get_children():
		t.raise()
		if _cases[_case][2]:
			t.down = true
			t.collision_layer = 0
	for t in s.get_node("Standups").get_children():
		t.set_lit(false)
	for t in s.get_node("LanesTop").get_children():
		t.set_lit(false)
	root.get_node("GameState").start_sector(3)
	s.get_node("Kickback").lit = false


func _next() -> void:
	_case += 1
	_t = 0.0
	_flipped = false
	_hits.clear()
	for a in ["flipper_left", "flipper_right"]:
		Input.action_release(a)
	for b in _s().get_node("Balls").get_children():
		b.free()
	if _case >= _cases.size():
		return
	_reset_table()
	_drop_was_down = _cases[_case][2]
	var f: Node2D = _s().get_node(_cases[_case][0])
	var d := -1.0 if f.mirrored else 1.0
	_ball = load("res://scenes/table/Ball.tscn").instantiate()
	_ball.position = f.position + Vector2(f.length * _cases[_case][1] * d, -11.0).rotated(f.rotation)
	_s().get_node("Balls").add_child(_ball)


func _process(delta: float) -> bool:
	_t += delta
	if _case == -1:
		if _t > 0.3:
			for d in _s().get_node("Drones").get_children():
				d.free()
			_next()
		return false
	if _case >= _cases.size():
		return _report()
	var s := _s()
	var f: Node2D = s.get_node(_cases[_case][0])
	if not _flipped and _t >= 0.08:
		_flipped = true
		Input.action_press(f.action)
	if is_instance_valid(_ball):
		if s.get_node("RailRight")._riders.has(_ball):
			_hit("ramp_right")
		if s.get_node("RailLeft")._riders.has(_ball):
			_hit("ramp_left")
		if s.get_node("SpinnerLeft")._spin > 0.0:
			_hit("spinner_left")
		if s.get_node("SpinnerRight")._spin > 0.0:
			_hit("spinner_right")
		for t in s.get_node("Standups").get_children():
			if t.lit:
				_hit("standups")
		for t in s.get_node("LanesTop").get_children():
			if t.lit:
				_hit("lanes_top")
		if s.get_node("Saucer")._busy:
			_hit("saucer")
		if s.get_node("Spinner")._spin > 0.0:
			_hit("spinner")
		for b in s.get_node("Bumpers").get_children():
			if b._pulse > 0.9:
				_hit("bumper")
		if not _drop_was_down:
			for t in s.get_node("DropBank").get_children():
				if t.down:
					_hit("drop")
		if _ball.position.distance_to(s.get_node("Gate").position) < 16.0:
			_hit("lift")
	if _t >= SHOT_SECONDS or not is_instance_valid(_ball):
		var key: String = _cases[_case][0] + (" (bank down)" if _cases[_case][2] else "")
		if not _map.has(key):
			_map[key] = []
		_map[key].append("%.2f:%s" % [_cases[_case][1], ",".join(_hits)])
		_next()
	return false


func _report() -> bool:
	var fails := 0
	for key in _map:
		print("MAP %s  %s" % [key, "  ".join(_map[key])])
	for target in REQUIRED:
		var from := []
		for f in REQUIRED[target]:
			for entry in _map.get(f, []) + _map.get(f + " (bank down)", []):
				if (entry.split(":")[1].split(",") as Array).has(target):
					from.append(f)
					break
		var ok := not from.is_empty()
		if not ok:
			fails += 1
		print("CHECK %s reach_%s from %s" % ["PASS" if ok else "FAIL", target, str(from)])
	print("RESULT shot_map fails=%d %s" % [fails, "PASS" if fails == 0 else "FAIL"])
	return true
