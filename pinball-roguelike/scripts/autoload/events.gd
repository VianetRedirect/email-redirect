## Global signal bus. Any node can emit or listen without holding a reference
## to the other side. Keep the signal list small and name every argument.
extends Node

signal score_added(amount: int)
signal ball_launched
signal ball_drained
signal target_hit(target: Node2D, remaining_hp: int)
signal target_destroyed(target: Node2D)
signal run_started
signal run_ended(score: int, run_time: float)
