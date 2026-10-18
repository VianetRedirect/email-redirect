## A flipper. AnimatableBody2D with sync_to_physics so the physics engine
## sees it move and transfers momentum to the ball. Mirror it for the right
## side rather than building a second shape.
extends AnimatableBody2D

@export var mirrored := false
@export var action := "flipper_left"
@export var length := 28.0
@export var rest_degrees := 25.0
@export var active_degrees := -30.0
## Radians per second. Higher is snappier. 16 gets rest to active in ~60 ms.
@export var angular_speed := 16.0
@export var color := Color(0.91, 0.77, 0.28)

@onready var _poly: CollisionPolygon2D = $CollisionPolygon2D


func _ready() -> void:
	var d := _dir()
	_poly.polygon = PackedVector2Array([
		Vector2(-3 * d, -4), Vector2(0, -5), Vector2(length * d, -2),
		Vector2(length * d, 2), Vector2(0, 5), Vector2(-3 * d, 4),
	])
	rotation = deg_to_rad(rest_degrees) * d
	queue_redraw()


func _physics_process(delta: float) -> void:
	var pressed := Input.is_action_pressed(action)
	var target := deg_to_rad(active_degrees if pressed else rest_degrees) * _dir()
	rotation = move_toward(rotation, target, angular_speed * delta)


func _draw() -> void:
	draw_colored_polygon(_poly.polygon, color)
	draw_circle(Vector2.ZERO, 3.0, color.darkened(0.3))


func _dir() -> float:
	return -1.0 if mirrored else 1.0
