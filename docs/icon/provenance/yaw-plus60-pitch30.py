"""Render exact approved scene with yaw +60, pitch 30; local-only, no Git commit."""
import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent))
from render_scenes import main
main(['yaw-plus60-pitch30'])
