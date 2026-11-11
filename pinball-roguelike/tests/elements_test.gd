## Checks each table element behaves like the real mechanism it copies.
##   godot --headless --fixed-fps 60 --path . -s res://tests/elements_test.gd
## Prints one CHECK line per check and a RESULT line.
extends SceneTree

var _events: Node
var _state: Node
var _main: Node
var _step := 0
var _wait := 0.0
var _fails := 0
var _checks := 0
var _drained := 0
var _banks := 0
var _ball: RigidBody2D
var _min_y := 9999.0
var _scrap0 := 0


func _initialize() -> void:
	for n in ["Events", "GameState"]:
		if not root.has_node(n):
			var node: Node = load("res://scripts/autoload/%s.gd" % n.to_snake_case()).new()
			node.name = n
			root.add_child(node)
	_events = root.get_node("Events")
	_state = root.get_node("GameState")
	_main = load("res://scenes/main/Main.tscn").instantiate()
	root.add_child(_main)
	_events.ball_drained.connect(func(): _drained += 1)
	_events.bank_completed.connect(func(): _banks += 1)


func _check(name: String, ok: bool, detail := "") -> void:
	_checks += 1
	if not ok:
		_fails += 1
	print("CHECK %s %s %s" % ["PASS" if ok else "FAIL", name, detail])


func _s() -> Node2D:
	return _main.get_node("Sector")


func _new_ball(pos: Vector2, vel := Vector2.ZERO) -> void:
	for b in _s().get_node("Balls").get_children():
		b.free()
	_ball = load("res://scenes/table/Ball.tscn").instantiate()
	_ball.position = pos
	_s().get_node("Balls").add_child(_ball)
	_ball.linear_velocity = vel
	_s().set("_ball", _ball)
	_min_y = pos.y


## Ball position, or far below the table if the drain has taken it.
func _bpos() -> Vector2:
	return _ball.position if is_instance_valid(_ball) else Vector2(-1, 9999)


func _bvel() -> Vector2:
	return _ball.linear_velocity if is_instance_valid(_ball) else Vector2.ZERO


func _on_flipper(f: Node2D, fraction: float) -> Vector2:
	var d := -1.0 if f.mirrored else 1.0
	return f.position + Vector2(f.length * fraction * d, -11.0).rotated(f.rotation)


func _process(delta: float) -> bool:
	if is_instance_valid(_ball):
		_min_y = minf(_min_y, _ball.position.y)
	if _wait > 0.0:
		_wait -= delta
		return false
	var s := _s()
	if _step > 30:
		return true
	match _step:
		0:
			_wait = 0.3
		1:
			for d in s.get_node("Drones").get_children():
				d.free()
			_state.shield = 99
			# Pop bumper: a ball that rolls gently onto it is thrown hard.
			var b: Node2D = s.get_node("Bumpers/Bumper1")
			_new_ball(b.position + Vector2(0, -22), Vector2(0, 10))
			_wait = 0.2
		2:
			var d: float = _bpos().distance_to(s.get_node("Bumpers/Bumper1").position)
			_check("pop_bumper_throws_ball", d > 60.0, "moved=%.0f px in 0.2 s, speed=%.0f" % [d, _bvel().length()])
			# Kickback: lit, saves a ball in the left outlane. Open the left
			# gate first so the ball can reach it.
			s.get_node("OutlaneGateLeft")._time_left = 0.01
			_drained = 0
			_new_ball(Vector2(13, 600))
			_wait = 1.2
		3:
			var kb: Node = s.get_node("Kickback")
			_check("kickback_saves_ball", _drained == 0 and _min_y < 560.0 and not kb.lit, "drained=%d min_y=%.0f lit=%s" % [_drained, _min_y, str(kb.lit)])
			s.get_node("OutlaneGateLeft")._time_left = 0.01
			_new_ball(Vector2(13, 600))
			_wait = 1.5
		4:
			_check("dark_kickback_drains", _drained == 1, "drained=%d" % _drained)
			_wait = 1.2
		5:
			# Outlane gate closed: ball over the right outlane is steered inside.
			_drained = 0
			var g: Node = s.get_node("OutlaneGateRight")
			_check("gate_closed_at_start", g.is_closed())
			_new_ball(Vector2(236, 510))
			_wait = 1.5
		6:
			_check("closed_gate_feeds_inlane", _drained == 0 and _bpos().x < 224.0, "drained=%d pos=%s" % [_drained, str(_bpos().round())])
			var g: Node = s.get_node("OutlaneGateRight")
			g._time_left = 0.01
			_wait = 0.2
		7:
			_drained = 0
			_new_ball(Vector2(236, 510))
			_wait = 1.5
		8:
			_check("open_gate_drains", _drained == 1, "drained=%d" % _drained)
			_wait = 1.2
		9:
			# Drop bank: all three down re-arms both outlane saves, then resets.
			_banks = 0
			_scrap0 = _state.scrap
			_new_ball(Vector2(120, 400))
			_ball.freeze = true
			for t in s.get_node("DropBank").get_children():
				t.on_ball_hit(_ball)
			_wait = 0.2
		10:
			var kb: Node = s.get_node("Kickback")
			var g: Node = s.get_node("OutlaneGateRight")
			var all_down := true
			for t in s.get_node("DropBank").get_children():
				if not t.down:
					all_down = false
			_check("bank_complete_rearms_saves", _banks == 1 and kb.lit and g.is_closed() and all_down, "banks=%d lit=%s closed=%s" % [_banks, str(kb.lit), str(g.is_closed())])
			_check("bank_pays_scrap", _state.scrap - _scrap0 == 16, "scrap=%d" % (_state.scrap - _scrap0))
			_wait = 1.3
		11:
			var up := true
			for t in s.get_node("DropBank").get_children():
				if t.down:
					up = false
			_check("bank_resets", up)
			# Ramp: a strong shot rides the rail to the upper level.
			var dir := (Vector2(196, 340) - Vector2(160, 490)).normalized()
			_new_ball(Vector2(161, 486), dir * 650.0)
			_wait = 2.2
		12:
			_check("strong_ramp_shot_reaches_upper_level", _bpos().y < 250.0 and _bpos().x > 170.0 and not _ball.freeze, "pos=%s" % str(_bpos().round()))
			var dir := (Vector2(196, 340) - Vector2(160, 490)).normalized()
			_new_ball(Vector2(161, 486), dir * 330.0)
			_wait = 2.5
		13:
			_check("weak_ramp_shot_rolls_back", _min_y > 345.0 and _bpos().y > 480.0, "min_y=%.0f pos=%s" % [_min_y, str(_bpos().round())])
			# Saucer: holds a slow ball, pays, kicks it out.
			_scrap0 = _state.scrap
			var sc: Node2D = s.get_node("Saucer")
			_new_ball(sc.position + Vector2(8, -6), Vector2(-40, 30))
			_wait = 0.4
		14:
			_check("saucer_holds_ball", _ball.freeze and _state.scrap - _scrap0 == 5, "frozen=%s scrap=%d" % [str(_ball.freeze), _state.scrap - _scrap0])
			_wait = 0.9
		15:
			_check("saucer_kicks_out", not _ball.freeze and _bpos().x > 40.0, "pos=%s" % str(_bpos().round()))
			# Spinner: a fast pass pays more than a slow one.
			_scrap0 = _state.scrap
			_new_ball(Vector2(125, 300), Vector2(0, -600))
			_wait = 2.0
		16:
			var fast: int = _state.scrap - _scrap0
			_check("spinner_pays_for_spins", fast >= 3, "scrap=%d" % fast)
			# Flipper aim: tip shoots across, base shoots up its own side.
			_new_ball(_on_flipper(s.get_node("FlipperLeft"), 0.9))
			_wait = 0.12
		17:
			Input.action_press("flipper_left")
			_wait = 0.1
		18:
			var vx: float = _ball.linear_velocity.x
			_check("tip_shot_goes_across", vx > 100.0 and _ball.linear_velocity.y < -500.0, "v=%s" % str(_ball.linear_velocity.round()))
			Input.action_release("flipper_left")
			_wait = 0.3
		19:
			_new_ball(_on_flipper(s.get_node("FlipperLeft"), 0.12))
			_wait = 0.12
		20:
			Input.action_press("flipper_left")
			_wait = 0.1
		21:
			var v := _bvel()
			_check("base_shot_goes_up_its_own_side", v.y < -400.0 and v.x < 60.0, "v=%s" % str(v.round()))
			Input.action_release("flipper_left")
		_:
			print("RESULT elements checks=%d fails=%d %s" % [_checks, _fails, "PASS" if _fails == 0 else "FAIL"])
			return true
	_step += 1
	return false
