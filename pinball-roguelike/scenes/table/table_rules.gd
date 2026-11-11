## Ties the table's banks together, the way a real game's rules do.
##   Drop bank complete      -> both outlane gates close (outlane_gate.gd)
##                              and the right kickback relights (kickback.gd)
##   Inlane/outlane lanes    -> relight the left kickback (kickback.gd)
##   Left standups complete  -> light one unlit valve, once per sector
##   Top lanes complete      -> super bumpers for a while (bumper.gd)
## Only the standup rule lives here; the others are wired in each part.
extends Node

var _standup_award_used := false


func _ready() -> void:
	Events.bank_done.connect(_on_bank_done)


func _on_bank_done(bank_id: StringName) -> void:
	if bank_id == &"standups" and not _standup_award_used:
		for valve in get_tree().get_nodes_in_group("objective_nodes"):
			if not valve.lit and valve.is_inside_tree():
				_standup_award_used = true
				Events.toast.emit("STANDUPS: VALVE OPENED")
				valve.on_ball_hit(null)
				return
