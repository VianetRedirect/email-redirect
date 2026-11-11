## A bank of standups or rollovers. When every member is lit, the bank
## pays scrap, announces itself on the signal bus, and resets after a
## pause. With lane_change on, pressing a flipper shifts the lit pattern
## one lane toward that side, as on a real table, so the player can steer
## the unlit lane under the ball.
extends Node2D

## Name sent with Events.bank_done, so table rules know which bank it was.
@export var bank_id := &"lanes"
@export var bonus_scrap := 10
@export var reset_delay := 1.0
@export var lane_change := false
@export var toast_text := ""

var _resetting := false


func _ready() -> void:
	for member in get_children():
		member.hit.connect(_on_member_hit)


func _process(_delta: float) -> void:
	if not lane_change or _resetting:
		return
	if Input.is_action_just_pressed("flipper_left"):
		_shift(-1)
	elif Input.is_action_just_pressed("flipper_right"):
		_shift(1)


func _shift(direction: int) -> void:
	var members := get_children()
	var states := []
	for m in members:
		states.append(m.lit)
	for i in members.size():
		members[i].set_lit(states[posmod(i - direction, members.size())])


func _on_member_hit() -> void:
	for m in get_children():
		if not m.lit:
			return
	_resetting = true
	Events.scrap_added.emit(bonus_scrap)
	Events.bank_done.emit(bank_id)
	if toast_text != "":
		Events.toast.emit(toast_text)
	await get_tree().create_timer(reset_delay, false).timeout
	for m in get_children():
		m.set_lit(false)
	_resetting = false
