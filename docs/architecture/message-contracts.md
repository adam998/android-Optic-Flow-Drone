# Message Contracts

## Optical Flow Sample (Android internal contract)

Fields (proposed):
- timestamp_ns: monotonic timestamp in nanoseconds
- frame_id: incremental frame counter
- dt_s: delta time between frames (seconds)
- flow_x_px: image-plane flow in pixels (x)
- flow_y_px: image-plane flow in pixels (y)
- quality: 0..255 quality score (proposed normalization)
- feature_count: number of tracked features
- valid: boolean validity flag
- note: optional debug text (not for final runtime path)

## Notes
- This is an internal contract for Android pipeline first.
- MAVLink OPTICAL_FLOW_RAD mapping will be defined after LK output is stable.
- Height/range is NOT produced by Android in M1.
