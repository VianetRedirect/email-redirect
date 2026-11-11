## Plays the Sump with a crude bot that flips whenever the ball is over a
## flipper. Proves the sector can be played end to end: launch, valves,
## gate, lift, drains and shield. Run:
##   godot --headless --fixed-fps 60 --path . -s res://tests/sump_bot_test.gd
## Prints one RESULT line. It is a smoke test, not a balance test.
extends SceneTree

const MAX_SECONDS := 600.0

var _sector: Node2D
var _events: Node
var _state: Node
var _t := 0.0
var _hold := 0.0
var _launch_hold := -1.0
var _drains := 0
var _lit_times := []
var _gate_time := -1.0
var _clear_time := -1.0
var _min_y := 9999.0
var _ended := false
var _next_trace := 0.0
## Resting spots: a ball slower than 4 px/s for over 1 s outside the
## plunger lane. Keyed by position rounded to 10 px, value is seconds.
var _still := 0.0
var _rest_spots := {}
## Seconds spent in each 80 px band of height, after the gate opens.
var _bands := {}
## Last frames of ball state, printed if the ball leaves the table.
var _history := []
var _escaped := false
## Set true to print the ball position every 10 seconds.
const TRACE := false


func _initialize() -> void:
	for n in ["Events", "GameState"]:
		if not root.has_node(n):
			var node: Node = load("res://scripts/autoload/%s.gd" % n.to_snake_case()).new()
			node.name = n
			root.add_child(node)
	_events = root.get_node("Events")
	_state = root.get_node("GameState")
	var main: Node = load("res://scenes/main/Main.tscn").instantiate()
	root.add_child(main)
	_sector = main.get_node("Sector")
	_events.ball_drained.connect(func(): _drains += 1)
	_events.node_lit.connect(func(_n): _lit_times.append(snappedf(_t, 0.1)))
	_events.gate_opened.connect(func(): _gate_time = _t)
	_events.sector_cleared.connect(func(): _clear_time = _t)
	_events.run_ended.connect(func(_w, _s, _rt): _ended = true)


func _action(name: String, pressed: bool) -> void:
	var ev := InputEventAction.new()
	ev.action = name
	ev.pressed = pressed
	Input.parse_input_event(ev)
	if pressed:
		Input.action_press(name)
	else:
		Input.action_release(name)


func _ball() -> RigidBody2D:
	var balls := _sector.get_node("Balls")
	return balls.get_child(0) if balls.get_child_count() > 0 else null


func _process(delta: float) -> bool:
	_t += delta
	var b := _ball()
	if b:
		_min_y = minf(_min_y, b.position.y)
		var in_lane: bool = b.position.x > 248.0 and b.position.y > 336.0
		if in_lane and b.linear_velocity.length() < 5.0 and _launch_hold < 0.0:
			_action("launch", true)
			_launch_hold = randf_range(0.1, 0.7)
		elif _launch_hold >= 0.0:
			_launch_hold -= delta
			if _launch_hold < 0.0:
				_action("launch", false)
		var p := b.position
		var du = _sector.get_node_or_null("Drones/DroneUpper")
		_history.append("%.2f pos=%s v=%s drone=%s" % [_t, str(p.round()), str(b.linear_velocity.round()), str(du.position.round()) if du else "dead"])
		if _history.size() > 12:
			_history.pop_front()
		if not _escaped and (p.x < 0.0 or p.x > 270.0 or p.y < 0.0 or p.y > 720.0):
			_escaped = true
			print("ESCAPE\n" + "\n".join(_history))
			_ended = true
		if _gate_time >= 0.0:
			var band := int(p.y / 80.0) * 80
			_bands[band] = _bands.get(band, 0.0) + delta
		if not in_lane and b.linear_velocity.length() < 4.0:
			_still += delta
			if _still > 1.0:
				var key := Vector2i(roundi(p.x / 10.0) * 10, roundi(p.y / 10.0) * 10)
				_rest_spots[key] = _rest_spots.get(key, 0.0) + delta
		else:
			_still = 0.0
		var over_main: bool = p.y > 636.0 and p.y < 676.0 and p.x > 86.0 and p.x < 162.0
		var over_mini: bool = p.y > 222.0 and p.y < 256.0 and p.x > 62.0 and p.x < 96.0
		if _hold <= 0.0 and (over_main or over_mini) and b.linear_velocity.y > -40.0:
			_action("flipper_left", true)
			_action("flipper_right", true)
			_hold = 0.18
	if _hold > 0.0:
		_hold -= delta
		if _hold <= 0.0:
			_action("flipper_left", false)
			_action("flipper_right", false)
	if TRACE and _t >= _next_trace:
		_next_trace += 10.0
		print("t=%4.0f ball=%s v=%s" % [_t, str(b.position.round()) if b else "none", str(b.linear_velocity.round()) if b else "-"])
	if _ended or _t > MAX_SECONDS:
		print("RESULT escaped=%s cleared=%s clear_t=%.0f gate_t=%.0f valves_lit_at=%s drains=%d shield=%d scrap=%d highest_y=%.0f t=%.0f" % [
			str(_escaped), str(_clear_time >= 0.0), _clear_time, _gate_time, str(_lit_times), _drains,
			_state.shield, _state.scrap, _min_y, _t])
		var spots := []
		for k in _rest_spots:
			if _rest_spots[k] > 0.5:
				spots.append("%s:%.0fs" % [str(k), _rest_spots[k]])
		print("REST_SPOTS %s" % ", ".join(spots))
		var keys := _bands.keys()
		keys.sort()
		var parts := []
		for k in keys:
			parts.append("%d:%.0f" % [k, _bands[k]])
		print("BANDS_AFTER_GATE %s" % " ".join(parts))
		return true
	return false
