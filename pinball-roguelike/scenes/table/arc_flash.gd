## A short-lived lightning line drawn by Arc Ball between the ball and the
## enemy it shocked.
extends Node2D

var from := Vector2.ZERO
var to := Vector2.ZERO
var _t := 0.0


func _ready() -> void:
	top_level = true
	queue_redraw()


func _process(delta: float) -> void:
	_t += delta
	modulate.a = 1.0 - _t / 0.15
	if _t >= 0.15:
		queue_free()


func _draw() -> void:
	var mid := (from + to) * 0.5 + (to - from).orthogonal().normalized() * 4.0
	draw_polyline(PackedVector2Array([from, mid, to]), Color(0.5, 0.9, 1.0), 1.0)
