"""Build with an existing named NeoForge 1.21.1 installation (no downloads).

Usage: python tools/build_local.py path/to/local-runtime.json path/to/Patchouli.jar
The runtime JSON is a local build input, never included in the distributable JAR.
NeoForge 1.21 uses Mojang runtime names, so no reobfuscation step is required.
"""
import hashlib
import json
import re
import shutil
import subprocess
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
cfg = json.loads(Path(sys.argv[1]).read_text(encoding="utf-8"))
classpath = list(cfg["classpath"])
for name in list(classpath):
    if "neoforge-" in name and name.endswith("-client.jar"):
        universal = Path(name.replace("-client.jar", "-universal.jar"))
        if universal.is_file() and str(universal) not in classpath:
            classpath.append(str(universal))
classpath.append(str(Path(sys.argv[2]).resolve()))
missing = [p for p in classpath if not Path(p).exists()]
if missing:
    raise SystemExit("Missing compile dependencies: " + repr(missing))
build = ROOT / "build/local"
classes = build / "classes"
assert classes.resolve().is_relative_to((ROOT / "build").resolve())
if classes.exists():
    shutil.rmtree(classes)
classes.mkdir(parents=True, exist_ok=True)
sources = sorted((ROOT / "src/main/java").rglob("*.java"))
args = ["--release", "21", "-encoding", "UTF-8", "-proc:none", "-Xmaxerrs", "40",
        "-classpath", ";".join(classpath), "-d", str(classes), *map(str, sources)]
argfile = build / "javac.args"
argfile.write_text("\n".join('"' + a.replace("\\", "/") + '"' for a in args), encoding="utf-8")
proc = subprocess.run([str(Path(cfg["javaHome"]) / "bin/javac.exe"), "@" + str(argfile)],
                      stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
(build / "compile.log").write_bytes(proc.stdout)
print(proc.stdout.decode("utf-8", errors="replace"))
if proc.returncode:
    raise SystemExit(proc.returncode)
props = dict(re.findall(r"^([\w_]+)=(.*)$", (ROOT / "gradle.properties").read_text(), re.M))
metadata = (ROOT / "src/main/templates/META-INF/neoforge.mods.toml").read_text(encoding="utf-8")
for key, value in props.items():
    metadata = metadata.replace("${" + key + "}", value)
if "${" in metadata:
    raise RuntimeError("Unexpanded mod metadata")
out = ROOT / "build/libs" / (props["mod_id"] + "-" + props["mod_version"] + ".jar")
out.parent.mkdir(parents=True, exist_ok=True)
with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as jar:
    for base in (classes, ROOT / "src/main/resources", ROOT / "src/generated/resources"):
        if base.exists():
            for path in sorted(base.rglob("*")):
                if path.is_file():
                    jar.write(path, path.relative_to(base).as_posix())
    jar.writestr("META-INF/neoforge.mods.toml", metadata)
    jar.writestr("META-INF/MANIFEST.MF", "Manifest-Version: 1.0\r\n\r\n")
    for path in ROOT.glob("LICENSE*"):
        jar.write(path, path.name)
    jar.write(ROOT / "THIRD_PARTY_NOTICES.md", "THIRD_PARTY_NOTICES.md")
print(out)
print("SHA256", hashlib.sha256(out.read_bytes()).hexdigest())
