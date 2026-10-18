## Score, balls and run timer. Listens to GameState and Events only, so the
## table never needs to know the HUD exists.
extends CanvasLayer

@onready var _score: Label = $Score
@onready var _balls: Label = $Balls
@onready var _timer: Label = $Timer
@onready var _message: Label = $Message


func _ready() -> void:
	GameState.score_changed.connect(_on_score)
	GameState.balls_changed.connect(_on_balls)
	GameState.time_changed.connect(_on_time)
	Events.run_started.connect(func(): _message.visible = false)
	Events.run_ended.connect(_on_run_ended)
	_on_score(GameState.score)
	_on_balls(GameState.balls_left)
	_on_time(GameState.run_time)


func _on_score(score: int) -> void:
	_score.text = "%06d" % score


func _on_balls(balls_left: int) -> void:
	_balls.text = "BALLS %d" % balls_left


func _on_time(run_time: float) -> void:
	var total := int(run_time)
	_timer.text = "%02d:%02d" % [total / 60, total % 60]


func _on_run_ended(score: int, run_time: float) -> void:
	_message.text = "RUN OVER\n%d pts in %s\n\ntap to restart" % [score, _timer.text]
	_message.visible = true
