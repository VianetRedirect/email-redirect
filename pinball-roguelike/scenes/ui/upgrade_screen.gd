## Pick one of up to three power-ups. Shown by main.gd while the game is
## paused, so this node keeps processing while paused.
extends CanvasLayer

const CARD_HEIGHT := 92.0

@onready var _cards: VBoxContainer = $Panel/Cards

var _offers: Array = []


func _ready() -> void:
	process_mode = Node.PROCESS_MODE_ALWAYS
	visible = false
	Events.upgrade_offered.connect(show_offers)


func show_offers(offers: Array) -> void:
	_offers = offers
	for child in _cards.get_children():
		child.queue_free()
	for i in offers.size():
		_cards.add_child(_make_card(offers[i], i))
	visible = true


## Picks offer `index`. Called by the card buttons, and by tests.
func choose(index: int) -> void:
	if not visible or index < 0 or index >= _offers.size():
		return
	visible = false
	Events.item_picked.emit(_offers[index])


func _make_card(item: ItemData, index: int) -> Button:
	var accent := ItemData.family_color(item.family)
	var card := Button.new()
	card.custom_minimum_size = Vector2(0, CARD_HEIGHT)
	card.focus_mode = Control.FOCUS_NONE
	card.pressed.connect(choose.bind(index))
	for state in ["normal", "hover", "pressed"]:
		var box := StyleBoxFlat.new()
		box.bg_color = Color(0.08, 0.08, 0.12) if state != "pressed" else Color(0.18, 0.18, 0.25)
		box.border_color = accent
		box.set_border_width_all(1)
		box.set_content_margin_all(6)
		card.add_theme_stylebox_override(state, box)

	var text := VBoxContainer.new()
	text.set_anchors_preset(Control.PRESET_FULL_RECT)
	text.offset_left = 6
	text.offset_top = 4
	text.offset_right = -6
	text.offset_bottom = -4
	text.mouse_filter = Control.MOUSE_FILTER_IGNORE
	card.add_child(text)

	var title := Label.new()
	title.text = item.display_name.to_upper()
	title.add_theme_font_size_override("font_size", 11)
	title.add_theme_color_override("font_color", accent)
	text.add_child(title)

	var meta := Label.new()
	var tag_text := (" · " + " ".join(item.tags).to_upper()) if not item.tags.is_empty() else ""
	meta.text = ItemData.family_name(item.family) + tag_text
	meta.add_theme_font_size_override("font_size", 8)
	meta.add_theme_color_override("font_color", Color(0.6, 0.6, 0.7))
	text.add_child(meta)

	var body := Label.new()
	body.text = item.description
	body.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	body.add_theme_font_size_override("font_size", 9)
	body.size_flags_vertical = Control.SIZE_EXPAND_FILL
	text.add_child(body)

	for label in [title, meta, body]:
		label.mouse_filter = Control.MOUSE_FILTER_IGNORE
	return card
