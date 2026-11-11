## Plays a run with a crude bot that flips whenever a ball is over a
## flipper and picks a random upgrade at each lift. Proves the run loop
## works end to end with items active: launch, valves, gate, lift, upgrade
## screen, next sector, drains, shield, clones. Run:
##   godot --headless --fixed-fps 60 --path . -s res://tests/sump_bot_test.gd
## Prints one RESULT line. It is a smoke test, not a balance test: the bot
## cannot aim, so it is much slower than a person.
extends SceneTree

const SECTORS_TO_CLEAR := 3
const MAX_SECONDS := 900.0
## Set true to print the main ball's position every 10 seconds.
const TRACE := false

var _main: Node
var _events: Node
var _state: Node
var _t := 0.0
var _hold := 0.0
var _launch_hold := -1.0
var _drains := 0
var _escapes := 0
var _clear_times := []
var _picked := []
var _pick_in := -1.0
var _ended := false
var _next_trace := 0.0
## Resting spots: main ball slower than 4 px/s for over 1 s outside the
## plunger lane. Keyed by position rounded to 10 px, value is seconds.
var _still := 0.0
var _rest_spots := {}
var _max_clones := 0
## Where each drain happened: "centre", "left_out" or "right_out".
var _drain_where := {"centre": 0, "left_out": 0, "right_out": 0}
var _last_pos := Vector2.ZERO


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
	_events.ball_drained.connect(_on_drained)
	_events.ball_escaped.connect(func(): _escapes += 1)
	_events.sector_cleared.connect(_on_cleared)
	_events.upgrade_offered.connect(func(_o): _pick_in = 0.3)
	_events.run_ended.connect(func(_w, _s, _rt): _ended = true)


func _on_drained() -> void:
	_drains += 1
	var where := "centre"
	if _last_pos.x < 26.0:
		where = "left_out"
	elif _last_pos.x > 222.0:
		where = "right_out"
	_drain_where[where] += 1


func _on_cleared() -> void:
	_clear_times.append(roundi(_t))
	if _clear_times.size() >= SECTORS_TO_CLEAR:
		_ended = true


func _action(name: String, pressed: bool) -> void:
	if pressed:
		Input.action_press(name)
	else:
		Input.action_release(name)
	var ev := InputEventAction.new()
	ev.action = name
	ev.pressed = pressed
	Input.parse_input_event(ev)


func _process(delta: float) -> bool:
	_t += delta
	if _pick_in >= 0.0:
		_pick_in -= delta
		if _pick_in < 0.0:
			var screen: Node = _main.get_node("UpgradeScreen")
			var index: int = randi() % screen._offers.size()
			_picked.append(String(screen._offers[index].id))
			screen.choose(index)
	var sector: Node = _main.get_node("Sector")
	var b = sector.get("_ball")
	var clones := 0
	var any_over := false
	for ball in get_nodes_in_group("balls"):
		if ball.is_clone:
			clones += 1
		if ball.freeze or ball.linear_velocity.y < -40.0:
			continue
		for f in get_nodes_in_group("flippers"):
			# Let the ball roll onto the bat before flipping, as a player
			# would. Flipping at the pivot just pops it straight up.
			if f._contact_fraction(ball) >= 0.3:
				any_over = true
	_max_clones = maxi(_max_clones, clones)
	if is_instance_valid(b):
		var p: Vector2 = b.position
		_last_pos = p
		var in_lane: bool = p.x > 248.0 and p.y > 336.0
		if in_lane and b.linear_velocity.length() < 5.0 and _launch_hold < 0.0:
			_action("launch", true)
			_launch_hold = randf_range(0.1, 0.7)
		if not in_lane and b.linear_velocity.length() < 4.0:
			_still += delta
			if _still > 1.0:
				var key := Vector2i(roundi(p.x / 10.0) * 10, roundi(p.y / 10.0) * 10)
				_rest_spots[key] = _rest_spots.get(key, 0.0) + delta
		else:
			_still = 0.0
		if TRACE and _t >= _next_trace:
			_next_trace += 10.0
			print("t=%4.0f ball=%s v=%s" % [_t, str(p.round()), str(b.linear_velocity.round())])
	if _launch_hold >= 0.0:
		_launch_hold -= delta
		if _launch_hold < 0.0:
			_action("launch", false)
	if _hold <= 0.0 and any_over:
		_action("flipper_left", true)
		_action("flipper_right", true)
		_hold = 0.18
	if _hold > 0.0:
		_hold -= delta
		if _hold <= 0.0:
			_action("flipper_left", false)
			_action("flipper_right", false)
	if _ended or _t > MAX_SECONDS:
		var spots := []
		for k in _rest_spots:
			if _rest_spots[k] > 0.5:
				spots.append("%s:%.0fs" % [str(k), _rest_spots[k]])
		print("RESULT sectors_cleared=%d clear_at=%s items=%s escapes=%d drains=%d %s shield=%d run_over=%s max_clones=%d scrap=%d t=%.0f rest_spots=[%s]" % [
			_clear_times.size(), str(_clear_times), str(_picked), _escapes, _drains, str(_drain_where),
			_state.shield, str(not _state.run_active), _max_clones, _state.scrap, _t, ", ".join(spots)])
		return true
	return false
