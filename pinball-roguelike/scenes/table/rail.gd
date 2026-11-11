## Ramp top plus wire rail. A ball that reaches the top of a ramp while
## still moving upward is caught and carried along the rail, above the
## rest of the table, and dropped at the far end. While it rides it cannot
## collide with anything, as a real ball on a wire rail cannot.
##
## The Area2D shape is the catch zone at the ramp top. `points` is the rail
## in this node's local coordinates, starting at the catch zone.
extends Area2D

@export var points := PackedVector2Array()
## A ball keeps the speed it reached the top with, never less than this.
@export var min_speed := 220.0
## Wire rails run downhill, so the ball speeds up along them.
@export var rail_accel := 250.0
@export var max_speed := 650.0
@export var exit_velocity := Vector2(-30, 20)
@export var scrap := 3
@export var color := Color(0.75, 0.8, 0.9, 0.8)

## Ball -> [distance travelled, current speed].
var _riders := {}
var _length := 0.0


func _ready() -> void:
	body_entered.connect(_on_body_entered)
	for i in range(1, points.size()):
		_length += points[i - 1].distance_to(points[i])
	queue_redraw()


func _on_body_entered(body: Node) -> void:
	if body is Ball and body.linear_velocity.y < 0.0 and not _riders.has(body):
		_catch.call_deferred(body)


func _catch(ball: Ball) -> void:
	if not is_instance_valid(ball) or _riders.has(ball):
		return
	var entry := clampf(ball.linear_velocity.length(), min_speed, max_speed)
	ball.freeze = true
	ball.collision_layer = 0
	ball.collision_mask = 0
	ball.z_index = 5
	_riders[ball] = [0.0, entry]
	Events.scrap_added.emit(scrap)


func _physics_process(delta: float) -> void:
	for ball in _riders.keys():
		if not is_instance_valid(ball):
			_riders.erase(ball)
			continue
		var v: float = minf(_riders[ball][1] + rail_accel * delta, max_speed)
		var d: float = _riders[ball][0] + v * delta
		if d >= _length:
			_release(ball)
			continue
		_riders[ball] = [d, v]
		ball.global_position = to_global(_point_at(d))


func _release(ball: Ball) -> void:
	_riders.erase(ball)
	ball.global_position = to_global(points[points.size() - 1])
	ball.collision_layer = 2
	ball.collision_mask = 5
	ball.z_index = 0
	ball.freeze = false
	ball.linear_velocity = exit_velocity


func _point_at(distance: float) -> Vector2:
	var left := distance
	for i in range(1, points.size()):
		var seg := points[i - 1].distance_to(points[i])
		if left <= seg:
			return points[i - 1].lerp(points[i], left / seg)
		left -= seg
	return points[points.size() - 1]


func _draw() -> void:
	# Two parallel wires.
	for side in [-3.0, 3.0]:
		var line := PackedVector2Array()
		for i in points.size():
			var dir: Vector2
			if i < points.size() - 1:
				dir = (points[i + 1] - points[i]).normalized()
			else:
				dir = (points[i] - points[i - 1]).normalized()
			line.append(points[i] + dir.orthogonal() * side)
		draw_polyline(line, color, 1.0)
