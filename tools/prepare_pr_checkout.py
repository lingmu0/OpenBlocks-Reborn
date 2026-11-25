"""Copy the reviewed source changes into a clean checkout of the exact base revision.

Usage: python tools/prepare_pr_checkout.py PATH_TO_CHECKOUT
Never copies build outputs, private runtime files, logs, or test worlds.
"""
from pathlib import Path
import shutil
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
target = Path(sys.argv[1]).resolve()
expected = "f4265a23b37b6dca146663cffff038983218d636"
revision = subprocess.check_output(["git", "-C", str(target), "rev-parse", "HEAD"], text=True).strip()
if revision != expected:
    raise SystemExit("Checkout must start at the reviewed base revision " + expected)
if subprocess.check_output(["git", "-C", str(target), "status", "--porcelain"], text=True).strip():
    raise SystemExit("Use a clean checkout so unrelated edits cannot be overwritten")
tracked = subprocess.check_output(["git", "-C", str(target), "ls-files", "-z"]).decode().split("\0")
for name in tracked:
    if name and not (ROOT / name).exists():
        path = (target / name).resolve()
        assert path.is_relative_to(target)
        if path.is_file():
            path.unlink()
for source in ROOT.rglob("*"):
    if not source.is_file():
        continue
    rel = source.relative_to(ROOT)
    if rel.parts[0] not in {"src", "tools", "docs"} and len(rel.parts) != 1:
        continue
    if any(part in {"build", ".gradle", ".git", "__pycache__", "run", "runs"} for part in rel.parts):
        continue
    if source.suffix in {".class", ".pyc", ".log", ".jar", ".args"}:
        continue
    dest = target / rel
    dest.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(source, dest)
print("Prepared PR checkout:", target)
print("Base:", expected)
