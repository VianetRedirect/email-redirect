## Saves screenshots of the game for checking layout without a phone.
## Needs a display, so on a server run it under a virtual one:
##   xvfb-run -a godot --rendering-driver opengl3 --path . -s res://tests/screenshot.gd -- OUT_DIR
## Writes play.png (sector start), top.png (camera at the top of the
## sector, gate open) and upgrade.png (upgrade screen with three offers).
extends SceneTree

var _main: Node
var _out := "user://"
var _step := 0
var _frames := 0


func _initialize() -> void:
	var args := OS.get_cmdline_user_args()
	if args.size() > 0:
		_out = args[0]
	_main = load("res://scenes/main/Main.tscn").instantiate()
	root.add_child(_main)


func _save(name: String) -> void:
	var img := root.get_viewport().get_texture().get_image()
	img.save_png(_out.path_join(name))
	print("saved ", _out.path_join(name), " ", img.get_size())


func _process(_delta: float) -> bool:
	_frames += 1
	if _frames < 20:
		return false
	_frames = 0
	var gs := root.get_node("GameState")
	var ev := root.get_node("Events")
	match _step:
		0:
			_save("play.png")
			for item in ["arc_ball", "overclock", "insurance"]:
				gs.add_item(load("res://items/%s.tres" % item))
			var sector: Node = _main.get_node("Sector")
			for v in sector.get_node("Valves").get_children():
				v.on_ball_hit(sector.get("_ball"))
			var cam: Camera2D = sector.get_node("Camera")
			sector.set_process(false)
			cam.position.y = 240.0
		1:
			_save("top.png")
			gs.items.clear()
			ev.sector_cleared.emit()
		2:
			_save("upgrade.png")
		_:
			return true
	_step += 1
	return false
