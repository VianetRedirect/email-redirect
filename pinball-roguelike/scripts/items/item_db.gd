## Every item that can be offered. Listed by hand rather than scanned from
## the folder because exported Android builds rename .tres files.
## To add an item: create its .tres in res://items/, add it here, and add
## its behaviour where the family's hook lives (see item_data.gd).
extends RefCounted

const ALL: Array[ItemData] = [
	preload("res://items/arc_ball.tres"),
	preload("res://items/splitter.tres"),
	preload("res://items/overclock.tres"),
	preload("res://items/mag_grip.tres"),
	preload("res://items/insurance.tres"),
	preload("res://items/scrap_magnet.tres"),
]
