## Draws every collision shape under this body as a flat colour so grey-box
## tables need no art. Delete this once real sprites replace the shapes.
class_name GreyBoxBody
extends StaticBody2D

@export var color := Color(0.55, 0.58, 0.65)


func _ready() -> void:
	queue_redraw()


func _draw() -> void:
	for child in get_children():
		if child is CollisionPolygon2D:
			draw_colored_polygon(child.transform * child.polygon, color)
		elif child is CollisionShape2D:
			var shape: Shape2D = child.shape
			if shape is CircleShape2D:
				draw_circle(child.position, shape.radius, color)
			elif shape is RectangleShape2D:
				draw_rect(Rect2(child.position - shape.size * 0.5, shape.size), color)


## Short white flash on hit. Modulate affects everything drawn in _draw.
func flash() -> void:
	modulate = Color(2.5, 2.5, 2.5)
	var tween := create_tween()
	tween.tween_property(self, "modulate", Color.WHITE, 0.12)
