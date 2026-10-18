## Headless Android export that bypasses the editor's pre-flight check.
## Useful on build machines that have the export templates but not a full
## Android SDK. Produces an UNSIGNED apk; sign it afterwards with apksigner
## or uber-apk-signer.
##
##   godot --headless --editor -s res://tools/export_android.gd --path .
##
## Output: export/pinball-roguelike-unsigned.apk
extends SceneTree

const OUT_PATH := "res://export/pinball-roguelike-unsigned.apk"
## Frames to wait so the editor file system finishes its first scan.
const WARMUP_FRAMES := 30

var _frames := 0


func _process(_delta: float) -> bool:
	_frames += 1
	if _frames < WARMUP_FRAMES:
		return false
	var platform := EditorExportPlatformAndroid.new()
	var preset: EditorExportPreset = platform.create_preset()
	preset.set("package/unique_name", "com.vianet.pinballroguelike")
	preset.set("package/name", "Pinball Roguelike")
	preset.set("package/signed", false)
	preset.set("architectures/arm64-v8a", true)
	preset.set("architectures/armeabi-v7a", false)
	preset.set("version/code", 1)
	preset.set("version/name", "0.1.0")
	DirAccess.make_dir_recursive_absolute(ProjectSettings.globalize_path("res://export"))
	var err := platform.export_project(preset, true, ProjectSettings.globalize_path(OUT_PATH))
	print("export_project returned %d (0 is OK)" % err)
	for i in platform.get_message_count():
		print("export message: ", platform.get_message_text(i))
	return true
