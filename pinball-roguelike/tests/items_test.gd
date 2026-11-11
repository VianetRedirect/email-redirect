## Checks each power-up and the upgrade flow. Run:
##   godot --headless --fixed-fps 60 --path . -s res://tests/items_test.gd
## Prints one CHECK line per check and a RESULT line. Any FAIL fails it.
extends SceneTree

var _events: Node
var _state: Node
var _main: Node
var _step := 0
var _wait := 0.0
var _fails := 0
var _checks := 0
var _drained := 0
## Shared scratch values between steps.
var _ball: RigidBody2D
var _drone: Node2D
var _peak := 0.0
var _peak_plain := 0.0
var _peak_grip_shot := 0.0


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


func _check(name: String, ok: bool, detail := "") -> void:
	_checks += 1
	if not ok:
		_fails += 1
	print("CHECK %s %s %s" % ["PASS" if ok else "FAIL", name, detail])


func _sector() -> Node2D:
	return _main.get_node("Sector")


func _item(id: String) -> Resource:
	return load("res://items/%s.tres" % id)


func _fresh(items: Array) -> void:
	_state.start_run()
	_state.start_sector(3)
	for id in items:
		_state.add_item(_item(id))


func _new_ball(pos: Vector2) -> RigidBody2D:
	var b: RigidBody2D = load("res://scenes/table/Ball.tscn").instantiate()
	b.position = pos
	_sector().get_node("Balls").add_child(b)
	return b


func _clear_balls() -> void:
	for b in _sector().get_node("Balls").get_children():
		b.free()


func _flipper() -> Node2D:
	return _sector().get_node("FlipperLeft")


## A point on the left flipper, as a fraction of its length, just above it.
func _on_flipper(fraction: float) -> Vector2:
	var f := _flipper()
	return f.position + Vector2(f.length * fraction, -10.0).rotated(f.rotation)


func _process(delta: float) -> bool:
	if _wait > 0.0:
		_wait -= delta
		if _ball and is_instance_valid(_ball):
			_peak = maxf(_peak, -_ball.linear_velocity.y)
		return false
	match _step:
		0:
			_wait = 0.3
		1:
			for d in _sector().get_node("Drones").get_children():
				d.free()
			_clear_balls()
			# Offers and slots
			_fresh([])
			var offers: Array = _state.roll_offers(3)
			var ids := {}
			for o in offers:
				ids[o.id] = true
			_check("offers_three_distinct", offers.size() == 3 and ids.size() == 3, str(ids.keys()))
			var fakes := []
			for i in 3:
				var fake := ItemData.new()
				fake.id = StringName("fake_%d" % i)
				fake.family = ItemData.Family.FLIPPER
				fakes.append(fake)
			_state.add_item(fakes[0])
			_state.add_item(fakes[1])
			_check("flipper_slots_limit_two", not _state.can_take(fakes[2]))
			_fresh(["overclock"])
			var offers2: Array = _state.roll_offers(6)
			var has_owned := false
			for o in offers2:
				if o.id == &"overclock":
					has_owned = true
			_check("offers_exclude_owned", not has_owned and offers2.size() == 5, str(offers2.size()))
			# Insurance
			_fresh(["insurance"])
			var s0: int = _state.shield
			_state.take_drain()
			var s1: int = _state.shield
			_state.take_drain()
			var s2: int = _state.shield
			_state.start_sector(3)
			_state.take_drain()
			var s3: int = _state.shield
			_check("insurance", s1 == s0 and s2 == s0 - 1 and s3 == s0 - 1, "%d %d %d %d" % [s0, s1, s2, s3])
			# Scrap Magnet
			_fresh(["scrap_magnet"])
			for i in 4:
				_state.add_scrap(1)
			_check("scrap_magnet", _state.scrap == 5, "scrap=%d" % _state.scrap)
			_fresh([])
			for i in 4:
				_state.add_scrap(1)
			_check("scrap_plain", _state.scrap == 4, "scrap=%d" % _state.scrap)
		2:
			# Arc Ball: a hit on a bumper shocks a drone 30 px away.
			_fresh(["arc_ball"])
			_drone = load("res://scenes/table/Drone.tscn").instantiate()
			_drone.position = Vector2(135, 300)
			_drone.speed = 0.0
			_sector().add_child(_drone)
			_ball = _new_ball(Vector2(165, 300))
			_wait = 0.05
		3:
			var bumper: Node = _sector().get_node("Bumpers/Bumper1")
			_ball.note_bumper_hit(bumper)
			_check("arc_ball_damages_nearby_enemy", _drone.hp == 2, "hp=%d" % _drone.hp)
			_fresh([])
			_drone._cooldown = 0.0
			_ball.note_bumper_hit(bumper)
			_check("no_arc_without_item", _drone.hp == 2, "hp=%d" % _drone.hp)
			_drone.free()
			_clear_balls()
			# Splitter
			_fresh(["splitter"])
			_ball = _new_ball(Vector2(135, 300))
			_ball.linear_velocity = Vector2(0, -200)
			_wait = 0.05
		4:
			var bumper: Node = _sector().get_node("Bumpers/Bumper1")
			for i in 3:
				_ball.note_bumper_hit(bumper)
			_wait = 0.05
		5:
			var clones := []
			for b in get_nodes_in_group("balls"):
				if b.is_clone:
					clones.append(b)
			_check("splitter_spawns_one_clone", clones.size() == 1, "clones=%d" % clones.size())
			if clones.size() >= 1:
				var shield_before: int = _state.shield
				var drained_before := _drained
				_sector().get_node("Drain")._on_body_entered(clones[0])
				_check("clone_drain_is_free", _state.shield == shield_before and _drained == drained_before)
			_clear_balls()
			# Flipper baseline: no items, ball at mid flipper.
			_fresh([])
			_ball = _new_ball(_on_flipper(0.5))
			_wait = 0.3
		6:
			_peak = 0.0
			Input.action_press("flipper_left")
			_wait = 0.5
		7:
			_peak_plain = _peak
			Input.action_release("flipper_left")
			_clear_balls()
			# Overclock: hold raised for 1.1 s to charge, release, then flip.
			_fresh(["overclock"])
			Input.action_press("flipper_left")
			_wait = 1.1
		8:
			_check("overclock_charges", _flipper().overclocked)
			Input.action_release("flipper_left")
			_wait = 0.3
		9:
			_ball = _new_ball(_on_flipper(0.5))
			_wait = 0.3
		10:
			_peak = 0.0
			Input.action_press("flipper_left")
			_wait = 0.5
		11:
			_check("overclock_stronger", _peak > _peak_plain * 1.2, "plain=%.0f overclocked=%.0f" % [_peak_plain, _peak])
			_check("overclock_spent", not _flipper().overclocked)
			Input.action_release("flipper_left")
			_clear_balls()
			# Mag Grip must not swallow its own shot.
			_fresh(["mag_grip"])
			_wait = 0.2
		12:
			_ball = _new_ball(_on_flipper(0.5))
			_wait = 0.3
		13:
			_peak = 0.0
			Input.action_press("flipper_left")
			_wait = 0.5
		14:
			_peak_grip_shot = _peak
			_check("mag_grip_keeps_shot", _peak_grip_shot > _peak_plain * 0.9, "plain=%.0f grip=%.0f" % [_peak_plain, _peak_grip_shot])
			_clear_balls()
			# Mag Grip catch: flipper already raised, ball dropped onto it.
			_wait = 0.2
		15:
			_ball = _new_ball(_on_flipper(0.6) + Vector2(0, -20))
			_wait = 1.0
		16:
			var speed: float = _ball.linear_velocity.length()
			var near: bool = _ball.position.distance_to(_flipper().position) < 34.0
			_check("mag_grip_holds_ball", speed < 15.0 and near, "speed=%.1f pos=%s" % [speed, str(_ball.position.round())])
			Input.action_release("flipper_left")
			_clear_balls()
			# Upgrade flow.
			_fresh([])
			_state.shield = 1
			_events.sector_cleared.emit()
			_wait = 0.1
		17:
			var screen: CanvasLayer = _main.get_node("UpgradeScreen")
			var cards: int = screen.get_node("Panel/Cards").get_child_count()
			_check("lift_pauses_and_offers", paused and screen.visible and cards == 3, "paused=%s visible=%s cards=%d" % [str(paused), str(screen.visible), cards])
			screen.choose(0)
			_wait = 0.1
		18:
			_check("pick_resumes_and_advances", not paused and _state.items.size() == 1 and _state.depth == 2, "paused=%s items=%d depth=%d" % [str(paused), _state.items.size(), _state.depth])
			_check("pick_restores_shield", _state.shield == 2, "shield=%d" % _state.shield)
			_check("next_sector_loaded", _main.has_node("Sector") and _main.get_node("Sector").get_node("Balls").get_child_count() == 1)
		_:
			print("RESULT items checks=%d fails=%d %s" % [_checks, _fails, "PASS" if _fails == 0 else "FAIL"])
			return true
	_step += 1
	return false
