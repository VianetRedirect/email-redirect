## Holds the state of the current run. Later this is also where meta
## progression (currency carried home, unlocked characters) will live.
extends Node

signal score_changed(score: int)
signal balls_changed(balls_left: int)
signal time_changed(run_time: float)

const STARTING_BALLS := 3

var score := 0
var balls_left := STARTING_BALLS
var run_time := 0.0
var run_active := false


func _ready() -> void:
	Events.score_added.connect(add_score)


func _process(delta: float) -> void:
	if run_active:
		run_time += delta
		time_changed.emit(run_time)


func start_run() -> void:
	score = 0
	balls_left = STARTING_BALLS
	run_time = 0.0
	run_active = true
	score_changed.emit(score)
	balls_changed.emit(balls_left)
	time_changed.emit(run_time)
	Events.run_started.emit()


func add_score(amount: int) -> void:
	if not run_active:
		return
	score += amount
	score_changed.emit(score)


## Removes one ball. Returns true while the run can continue.
func lose_ball() -> bool:
	balls_left -= 1
	balls_changed.emit(balls_left)
	if balls_left <= 0:
		end_run()
		return false
	return true


func end_run() -> void:
	run_active = false
	Events.run_ended.emit(score, run_time)
