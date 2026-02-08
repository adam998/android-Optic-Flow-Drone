# Overview

Android (Downward camera)
  -> OpenCV LK optical flow (flow_x, flow_y, quality)
  -> MAVLink OPTICAL_FLOW_RAD (UDP/Serial)
  -> PX4 EKF2 uses optical flow + rangefinder height
  -> Gazebo Sim (gz) + PX4 SITL: No-GPS position hold
