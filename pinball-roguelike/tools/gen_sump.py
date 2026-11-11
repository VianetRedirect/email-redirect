#!/usr/bin/env python3
"""Generates scenes/sectors/Sump.tscn.

The Sump is symmetric about x = 124 (the centre of the main field, which
runs from x 4 to x 244; the plunger lane is x 248 to 266). Most parts are
written once for the left side and mirrored, so both halves always match.
Edit the layout here, run `python3 tools/gen_sump.py`, then run the tests.
"""
from pathlib import Path

CX = 124.0          # mirror axis
OUT = Path(__file__).resolve().parent.parent / "scenes/sectors/Sump.tscn"

ext = {}            # path -> id
subs = []           # sub-resource text blocks
nodes = []          # node text blocks


def res(path, kind):
    if path not in ext:
        ext[path] = (f"{len(ext) + 1}_r", kind)
    return f'ExtResource("{ext[path][0]}")'


def sub(kind, sid, **props):
    lines = [f'[sub_resource type="{kind}" id="{sid}"]']
    lines += [f"{k} = {v}" for k, v in props.items()]
    subs.append("\n".join(lines))
    return f'SubResource("{sid}")'


def v2(x, y):
    return f"Vector2({x:g}, {y:g})"


def poly(points):
    return "PackedVector2Array(" + ", ".join(f"{x:g}, {y:g}" for x, y in points) + ")"


def mx(x):
    return 2 * CX - x


def mpoly(points):
    return [(mx(x), y) for x, y in points]


def node(name, kind=None, parent=".", instance=None, **props):
    head = f'[node name="{name}"'
    if kind:
        head += f' type="{kind}"'
    head += f' parent="{parent}"'
    if instance:
        head += f" instance={instance}"
    head += "]"
    nodes.append("\n".join([head] + [f"{k} = {v}" for k, v in props.items()]))


# ---------------------------------------------------------------- resources
SECTOR = res("res://scenes/sectors/sector.gd", "Script")
WALLS = res("res://scenes/table/greybox_body.gd", "Script")
FLIPPER = res("res://scenes/table/Flipper.tscn", "PackedScene")
BUMPER = res("res://scenes/table/Bumper.tscn", "PackedScene")
SLING = res("res://scenes/table/Slingshot.tscn", "PackedScene")
VALVE = res("res://scenes/table/Valve.tscn", "PackedScene")
DRAIN = res("res://scenes/table/drain.gd", "Script")
DRONE = res("res://scenes/table/Drone.tscn", "PackedScene")
GATE = res("res://scenes/table/gate.gd", "Script")
LIFT = res("res://scenes/table/lift.gd", "Script")
SPINNER = res("res://scenes/table/spinner.gd", "Script")
DROP = res("res://scenes/table/DropTarget.tscn", "PackedScene")
DROPBANK = res("res://scenes/table/drop_bank.gd", "Script")
KICK = res("res://scenes/table/kickback.gd", "Script")
OGATE = res("res://scenes/table/outlane_gate.gd", "Script")
RAIL = res("res://scenes/table/rail.gd", "Script")
SAUCER = res("res://scenes/table/saucer.gd", "Script")
STANDUP = res("res://scenes/table/Standup.tscn", "PackedScene")
ROLLOVER = res("res://scenes/table/Rollover.tscn", "PackedScene")
BANK = res("res://scenes/table/target_bank.gd", "Script")
INSERT = res("res://scenes/table/insert.gd", "Script")
POST = res("res://scenes/table/Post.tscn", "PackedScene")
RULES = res("res://scenes/table/table_rules.gd", "Script")

S_DRAIN = sub("RectangleShape2D", "drain", size=v2(240, 8))
S_GATE = sub("RectangleShape2D", "gate", size=v2(60, 6))
S_LIFT = sub("RectangleShape2D", "lift", size=v2(60, 22))
S_SPIN_MID = sub("RectangleShape2D", "spin_mid", size=v2(20, 6))
S_SPIN_SIDE = sub("RectangleShape2D", "spin_side", size=v2(30, 6))
S_KICK = sub("RectangleShape2D", "kick", size=v2(16, 8))
S_RAILTOP = sub("RectangleShape2D", "railtop", size=v2(26, 10))
S_SAUCER = sub("CircleShape2D", "saucer", radius="5.0")

# ------------------------------------------------------------------ the room
node("Sump", "Node2D", parent=None, script=SECTOR)
nodes[-1] = nodes[-1].replace(' parent="None"', "")
node("Background", "ColorRect", offset_right="270.0", offset_bottom="720.0",
     mouse_filter="2", color="Color(0.07, 0.1, 0.09, 1)")
node("LiftShaft", "ColorRect", offset_left="105.0", offset_top="4.0",
     offset_right="165.0", offset_bottom="30.0", mouse_filter="2",
     color="Color(0.2, 0.5, 0.3, 1)")

# Ramps: the right ramp is fed by the left flipper, the left ramp by the
# right flipper. Lane is 27 px wide (2.7 balls), which builders rate easy.
R_FLOOR = [(152.5, 430), (179.5, 430), (209.5, 300), (182.5, 300)]
for side, pts in (("Right", R_FLOOR), ("Left", mpoly(R_FLOOR))):
    node(f"RampFloor{side}", "Polygon2D", color="Color(0.13, 0.19, 0.24, 1)",
         polygon=poly(pts))
node("Camera", "Camera2D", position=v2(135, 480))

walls = {
    "Left": [(0, 0), (4, 0), (4, 720), (0, 720)],
    "Right": [(266, 0), (270, 0), (270, 720), (266, 720)],
    "Top": [(0, 0), (270, 0), (270, 4), (0, 4)],
    "Bottom": [(0, 716), (270, 716), (270, 720), (0, 720)],
    "FunnelLeft": [(4, 4), (105, 4), (105, 36), (4, 84)],
    "FunnelRight": [(266, 4), (266, 84), (165, 36), (165, 4)],
    # Upper ledges end at the upper flippers' pivots, 6 px above.
    "LeftLedge": [(4, 222), (85, 240), (85, 250), (4, 250)],
    "RightLedgeArc": [(163, 240), (266, 222), (266, 336), (263.1, 313.7),
                      (254.5, 293), (240.8, 275.2), (223, 261.5),
                      (202.3, 252.9), (180, 250), (163, 250)],
    "PlungerDivider": [(244, 350), (248, 336), (248, 720), (244, 720)],
    "LeftLaneGuide": [(22, 566), (24, 556), (26, 566), (26, 612), (78, 658),
                      (78, 720), (22, 720)],
    "LeftSlingBase": [(44, 524), (44, 600), (72, 622)],
    # Guard at the foot of the side lane: a ball running down the outer
    # wall is steered into the inlane, so lane exits feed the flipper
    # instead of the outlane. Balls thrown by the slingshots can still
    # reach the outlane underneath it.
    "LeftOutlaneGuard": [(4, 470), (22, 498), (4, 505)],
}
for name, pts in list(walls.items()):
    if name in ("LeftLaneGuide", "LeftSlingBase", "LeftOutlaneGuard"):
        walls[name.replace("Left", "Right")] = mpoly(pts)
# Ramp walls and the flared mouths that steer near misses in.
ramp_r = {
    "RampRWallIn": [(149.5, 430), (152.5, 430), (182.5, 300), (179.5, 300)],
    "RampRWallOut": [(179.5, 430), (182.5, 430), (212.5, 300), (209.5, 300)],
    "RampRFlareIn": [(141.5, 440), (144, 442), (152.5, 431), (149.5, 429)],
    "RampRRoof": [(177, 300), (196, 286), (215, 300)],
}
for name, pts in ramp_r.items():
    walls[name] = pts
    walls[name.replace("RampR", "RampL")] = mpoly(pts)
# Top lane guides on the upper right, under the funnel.
for x in (207, 229):
    walls[f"LaneGuide{x}"] = [(x - 1.5, 90), (x, 84), (x + 1.5, 90),
                              (x + 1.5, 112), (x - 1.5, 112)]

node("Walls", "StaticBody2D", collision_layer="1", collision_mask="0",
     script=WALLS, color="Color(0.28, 0.34, 0.33, 1)")
for name, pts in walls.items():
    node(name, "CollisionPolygon2D", parent="Walls", polygon=poly(pts))

# Rubber posts: on the inner ramp flares, at the drop bank ends. The outer
# flares were removed: they sat on the shot line into the side lanes.
posts = [(141, 443), (88, 370), (160, 370), (mx(141), 443)]
node("Posts", "Node2D")
for i, (x, y) in enumerate(posts):
    node(f"Post{i + 1}", parent="Posts", instance=POST, position=v2(x, y))

# ------------------------------------------------------------- top: the lift
node("Gate", "StaticBody2D", position=v2(135, 33), collision_layer="1",
     collision_mask="0", script=GATE)
node("CollisionShape2D", "CollisionShape2D", parent="Gate", shape=S_GATE)
node("Lift", "Area2D", position=v2(135, 16), collision_layer="0",
     collision_mask="2", script=LIFT)
node("CollisionShape2D", "CollisionShape2D", parent="Lift", shape=S_LIFT)

# ------------------------------------------------------------------ spinners
for name, pos, shape, half in (("Spinner", (124, 264), S_SPIN_MID, 10),
                               ("SpinnerLeft", (22, 385), S_SPIN_SIDE, 15),
                               ("SpinnerRight", (226, 385), S_SPIN_SIDE, 15)):
    node(name, "Area2D", position=v2(*pos), collision_layer="0",
         collision_mask="2", script=SPINNER, half_width=f"{half:.1f}")
    node("CollisionShape2D", "CollisionShape2D", parent=name, shape=shape)

# ------------------------------------------------------- ramps and the rails
ramp_zone = sub("ConvexPolygonShape2D", "rampzone_r", points=poly(R_FLOOR))
ramp_zone_l = sub("ConvexPolygonShape2D", "rampzone_l",
                  points=poly(list(reversed(mpoly(R_FLOOR)))))
for side, shape in (("Right", ramp_zone), ("Left", ramp_zone_l)):
    node(f"RampZone{side}", "Area2D", collision_layer="0", collision_mask="2",
         gravity_space_override="1", gravity_direction=v2(0, 1),
         gravity="250.0")
    node("CollisionShape2D", "CollisionShape2D", parent=f"RampZone{side}",
         shape=shape)
rail_r = [(0, -3), (8, -38), (26, -72), (47.5, -103)]
node("RailRight", "Area2D", position=v2(196, 308), collision_layer="0",
     collision_mask="2", script=RAIL, points=poly(rail_r),
     exit_velocity=v2(-30, 20))
node("CollisionShape2D", "CollisionShape2D", parent="RailRight", shape=S_RAILTOP)
rail_l = [(-x, y) for x, y in rail_r]
rail_l[-1] = (-40, -105)
node("RailLeft", "Area2D", position=v2(mx(196), 308), collision_layer="0",
     collision_mask="2", script=RAIL, points=poly(rail_l),
     exit_velocity=v2(30, 20))
node("CollisionShape2D", "CollisionShape2D", parent="RailLeft", shape=S_RAILTOP)

node("Saucer", "Area2D", position=v2(40, 200), collision_layer="0",
     collision_mask="2", script=SAUCER)
node("CollisionShape2D", "CollisionShape2D", parent="Saucer", shape=S_SAUCER)

# ---------------------------------------------------- outlanes and their saves
# Left kickback relit by the inlane/outlane rollovers, right kickback by
# the drop bank. Black Knight had saves on both outlanes.
node("Kickback", "Area2D", position=v2(13, 690), collision_layer="0",
     collision_mask="2", script=KICK, relight_bank='&"lanes_low"')
node("CollisionShape2D", "CollisionShape2D", parent="Kickback", shape=S_KICK)
node("KickbackRight", "Area2D", position=v2(mx(13), 690), collision_layer="0",
     collision_mask="2", script=KICK, relight_bank='&"drop"',
     kick_velocity=v2(-25, -850))
node("CollisionShape2D", "CollisionShape2D", parent="KickbackRight", shape=S_KICK)
gate_r = [(244, 538), (244, 545), (218, 568), (218, 561)]
for side, pts in (("Right", gate_r), ("Left", mpoly(gate_r))):
    node(f"OutlaneGate{side}", "StaticBody2D", collision_layer="1",
         collision_mask="0", script=OGATE)
    node("CollisionPolygon2D", "CollisionPolygon2D",
         parent=f"OutlaneGate{side}", polygon=poly(pts))

# Inlane and outlane rollovers, left to right, with lane change.
node("LanesLow", "Node2D", script=BANK, bank_id='&"lanes_low"',
     bonus_scrap="10", lane_change="true", toast_text='"KICKBACK LIT"')
for i, (x, y) in enumerate([(13, 610), (50, 619), (mx(50), 619), (mx(13), 610)]):
    node(f"Lane{i + 1}", parent="LanesLow", instance=ROLLOVER, position=v2(x, y))

# ------------------------------------------------------------------ flippers
node("FlipperLeft", parent=".", instance=FLIPPER, position=v2(78, 664))
node("FlipperRight", parent=".", instance=FLIPPER, position=v2(mx(78), 664),
     mirrored="true", action='"flipper_right"')
for name, x, extra in (("FlipperUpperLeft", 85, {}),
                       ("FlipperUpperRight", mx(85),
                        {"mirrored": "true", "action": '"flipper_right"'})):
    node(name, parent=".", instance=FLIPPER, position=v2(x, 246),
         length="32.0", kick_base="500.0", kick_tip="780.0", **extra)

# --------------------------------------------------------------- bumpers etc
node("Bumpers", "Node2D")
for i, pos in enumerate([(72, 125), (114, 108), (98, 162)]):
    node(f"Bumper{i + 1}", parent="Bumpers", instance=BUMPER, position=v2(*pos))

node("Slingshots", "Node2D")
node("SlingshotLeft", parent="Slingshots", instance=SLING, position=v2(44, 524),
     normal=v2(1, -0.29))
node("SlingshotRight", parent="Slingshots", instance=SLING,
     position=v2(mx(44), 524), scale=v2(-1, 1), normal=v2(-1, -0.29))

# Centre drop bank, flat across the centre shot, valve 1 behind it.
node("DropBank", "Node2D", script=DROPBANK)
for i, x in enumerate((104, 124, 144)):
    node(f"Target{i + 1}", parent="DropBank", instance=DROP, position=v2(x, 370),
         rotation="1.5708")

# Left standups at the top of the left lane.
node("Standups", "Node2D", script=BANK, bank_id='&"standups"',
     bonus_scrap="8")
for i, y in enumerate((282, 300, 318)):
    node(f"Standup{i + 1}", parent="Standups", instance=STANDUP,
         position=v2(7, y))

# Top lanes with lane change.
node("LanesTop", "Node2D", script=BANK, bank_id='&"lanes_top"',
     bonus_scrap="10", lane_change="true", toast_text='"SUPER BUMPERS"')
for i, x in enumerate((196, 218, 240)):
    node(f"TopLane{i + 1}", parent="LanesTop", instance=ROLLOVER,
         position=v2(x, 104))

node("Valves", "Node2D")
for name, pos in (("Valve1", (124, 335)), ("Valve2", (236, 310)),
                  ("Valve3", (150, 112))):
    node(name, parent="Valves", instance=VALVE, position=v2(*pos))

node("Drones", "Node2D")
node("DroneLower", parent="Drones", instance=DRONE, position=v2(124, 452),
     patrol_half_width="18.0")
node("DroneUpper", parent="Drones", instance=DRONE, position=v2(135, 75),
     patrol_half_width="45.0")

# --------------------------------------------------------------- inserts
node("Inserts", "Node2D")
inserts = [
    ("ArrowRampRight", (166, 456), 0.23, "PULSE", None, "Color(0.4, 0.8, 1, 1)"),
    ("ArrowRampLeft", (mx(166), 456), -0.23, "PULSE", None, "Color(0.4, 0.8, 1, 1)"),
    ("ArrowCentre", (124, 396), 0.0, "WHILE_UNLIT", "../../Valves/Valve1", None),
    ("ArrowValve2", (228, 420), 0.0, "WHILE_UNLIT", "../../Valves/Valve2", None),
    ("ArrowLeftLane", (20, 420), 0.0, "PULSE", None, "Color(1, 0.9, 0.3, 1)"),
    ("ArrowValve3", (150, 140), 0.0, "WHILE_UNLIT", "../../Valves/Valve3", None),
    ("ArrowLift", (135, 52), 0.0, "WHILE_OPEN", None, "Color(0.4, 1, 0.5, 1)"),
]
modes = {"PULSE": 0, "WHILE_UNLIT": 1, "WHILE_OPEN": 2}
for name, pos, rot, mode, watch, color in inserts:
    props = {"position": v2(*pos), "script": INSERT, "mode": str(modes[mode])}
    if rot:
        props["rotation"] = f"{rot:g}"
    if watch:
        props["watch"] = f'NodePath("{watch}")'
    if color:
        props["color"] = color
    node(name, "Node2D", parent="Inserts", **props)

node("Rules", "Node", script=RULES)

node("Drain", "Area2D", position=v2(124, 710), collision_layer="0",
     collision_mask="2", script=DRAIN)
node("CollisionShape2D", "CollisionShape2D", parent="Drain", shape=S_DRAIN)
node("PlungerSpawn", "Marker2D", position=v2(257, 708))
node("Balls", "Node2D")

# ------------------------------------------------------------------- write
out = [f"[gd_scene load_steps={len(ext) + len(subs) + 1} format=3]", ""]
for path, (eid, kind) in ext.items():
    out.append(f'[ext_resource type="{kind}" path="{path}" id="{eid}"]')
out.append("")
out += [s + "\n" for s in subs]
out += [n + "\n" for n in nodes]
OUT.write_text("\n".join(out))
print(f"wrote {OUT} ({len(nodes)} nodes)")
