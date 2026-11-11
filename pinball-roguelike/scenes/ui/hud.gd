## Shield, scrap, objective progress, run timer and the off-screen
## objective hint. Listens to GameState and Events only, so sectors never
## need to know the HUD exists.
extends CanvasLayer

@onready var _shield: Label = $Shield
@onready var _scrap: Label = $Scrap
@onready var _timer: Label = $Timer
@onready var _nodes: Label = $Nodes
@onready var _hint: Label = $Hint
@onready var _message: Label = $Message

var _gate_open := false


func _ready() -> void:
	GameState.shield_changed.connect(_on_shield)
	GameState.scrap_changed.connect(_on_scrap)
	GameState.time_changed.connect(_on_time)
	GameState.nodes_changed.connect(_on_nodes)
	Events.gate_opened.connect(_on_gate_opened)
	Events.objective_hint.connect(_on_hint)
	Events.run_started.connect(func(): _message.visible = false)
	Events.run_ended.connect(_on_run_ended)
	_on_shield(GameState.shield)
	_on_scrap(GameState.scrap)
	_on_time(GameState.run_time)
	_on_nodes(GameState.nodes_lit, GameState.nodes_total)
	_on_hint(0)


func _on_shield(shield: int) -> void:
	_shield.text = "SHIELD %d" % shield


func _on_scrap(scrap: int) -> void:
	_scrap.text = "SCRAP %d" % scrap


func _on_time(run_time: float) -> void:
	var total := int(run_time)
	_timer.text = "%02d:%02d" % [total / 60, total % 60]


func _on_nodes(lit: int, total: int) -> void:
	if not _gate_open:
		_nodes.text = "VALVES %d/%d" % [lit, total]


func _on_gate_opened() -> void:
	_gate_open = true
	_nodes.text = "LIFT OPEN"


func _on_hint(direction: int) -> void:
	_hint.visible = direction != 0
	var what := "LIFT" if _gate_open else "VALVE"
	_hint.text = ("^ %s ^" if direction < 0 else "v %s v") % what
	_hint.position.y = 34.0 if direction < 0 else 452.0


func _on_run_ended(won: bool, scrap: int, _run_time: float) -> void:
	var title := "LIFT REACHED" if won else "PURGED"
	_message.text = "%s\n%d scrap in %s\n\ntap to restart" % [title, scrap, _timer.text]
	_message.visible = true
