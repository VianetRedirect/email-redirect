## Shield, scrap, sector, objective progress, run timer, items and the
## off-screen objective hint. Listens to GameState and Events only, so
## sectors never need to know the HUD exists.
extends CanvasLayer

@onready var _shield: Label = $Shield
@onready var _scrap: Label = $Scrap
@onready var _timer: Label = $Timer
@onready var _nodes: Label = $Nodes
@onready var _depth: Label = $Depth
@onready var _items: HBoxContainer = $Items
@onready var _hint: Label = $Hint
@onready var _toast: Label = $Toast
@onready var _message: Label = $Message

var _gate_open := false


func _ready() -> void:
	GameState.shield_changed.connect(_on_shield)
	GameState.scrap_changed.connect(_on_scrap)
	GameState.time_changed.connect(_on_time)
	GameState.nodes_changed.connect(_on_nodes)
	GameState.depth_changed.connect(_on_depth)
	GameState.items_changed.connect(_on_items)
	Events.gate_opened.connect(_on_gate_opened)
	Events.objective_hint.connect(_on_hint)
	Events.insurance_used.connect(func(): _show_toast("INSURED: NO SHIELD LOST"))
	Events.toast.connect(_show_toast)
	Events.run_started.connect(func(): _message.visible = false)
	Events.run_ended.connect(_on_run_ended)
	_on_shield(GameState.shield)
	_on_scrap(GameState.scrap)
	_on_time(GameState.run_time)
	_on_nodes(GameState.nodes_lit, GameState.nodes_total)
	_on_depth(GameState.depth)
	_on_items(GameState.items)
	_on_hint(0)


func _on_shield(shield: int) -> void:
	_shield.text = "SHIELD %d" % shield


func _on_scrap(scrap: int) -> void:
	_scrap.text = "SCRAP %d" % scrap


func _on_time(run_time: float) -> void:
	var total := int(run_time)
	_timer.text = "%02d:%02d" % [total / 60, total % 60]


func _on_nodes(lit: int, total: int) -> void:
	# A new sector resets the node count, so the gate is closed again.
	if lit == 0:
		_gate_open = false
	if not _gate_open:
		_nodes.text = "VALVES %d/%d" % [lit, total]


func _on_depth(depth: int) -> void:
	_depth.text = "SECTOR %d" % depth


func _on_items(items: Array) -> void:
	for child in _items.get_children():
		child.queue_free()
	for item in items:
		var label := Label.new()
		label.text = item.short_name
		label.add_theme_font_size_override("font_size", 8)
		label.add_theme_color_override("font_color", ItemData.family_color(item.family))
		_items.add_child(label)


func _on_gate_opened() -> void:
	_gate_open = true
	_nodes.text = "LIFT OPEN"


func _on_hint(direction: int) -> void:
	_hint.visible = direction != 0
	var what := "LIFT" if _gate_open else "VALVE"
	_hint.text = ("^ %s ^" if direction < 0 else "v %s v") % what
	_hint.position.y = 34.0 if direction < 0 else 440.0


func _show_toast(text: String) -> void:
	_toast.text = text
	_toast.visible = true
	_toast.modulate.a = 1.0
	var tween := create_tween()
	tween.tween_interval(1.2)
	tween.tween_property(_toast, "modulate:a", 0.0, 0.4)
	tween.tween_callback(func(): _toast.visible = false)


func _on_run_ended(won: bool, scrap: int, _run_time: float) -> void:
	var title := "CLIMB COMPLETE" if won else "PURGED"
	_message.text = "%s\nreached sector %d\n%d scrap in %s\n\ntap to restart" % [title, GameState.depth, scrap, _timer.text]
	_message.visible = true
