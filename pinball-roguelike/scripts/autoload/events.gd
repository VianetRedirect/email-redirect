## Global signal bus. Any node can emit or listen without holding a reference
## to the other side. Keep the signal list small and name every argument.
extends Node

signal scrap_added(amount: int)
signal ball_launched
signal ball_drained
## A ball left the table through a physics fault and was respawned.
signal ball_escaped
signal node_lit(node: Node2D)
signal gate_opened
signal sector_cleared
signal enemy_destroyed(enemy: Node2D)
## -1 the objective is above the screen, 1 below, 0 visible.
signal objective_hint(direction: int)
signal upgrade_offered(offers: Array)
signal item_picked(item: ItemData)
signal insurance_used
signal run_started
signal run_ended(won: bool, scrap: int, run_time: float)
