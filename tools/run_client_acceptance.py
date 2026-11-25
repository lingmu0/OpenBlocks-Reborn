"""Opt-in isolated visual acceptance client. No changes to an installed modpack.

Usage: python tools/run_client_acceptance.py RUNTIME_JSON PATCHOULI_JAR [--run]
First prepares the test mod and launch file; --run opens the client and auto-closes.
"""
from pathlib import Path
import json
import os
import shutil
import subprocess
import sys
import zipfile

ROOT = Path(__file__).resolve().parents[1]
config_path = Path(sys.argv[1]).resolve()
cfg = json.loads(config_path.read_text(encoding="utf-8"))
patchouli = Path(sys.argv[2]).resolve()
cp = list(cfg["classpath"]) + [str(ROOT / "build/local/classes"), str(patchouli)]
out = ROOT / "build/acceptance-classes"
out.mkdir(parents=True, exist_ok=True)
sources = list((ROOT / "src/acceptance/java").rglob("*.java"))
compile_args = ["--release", "21", "-encoding", "UTF-8", "-proc:none", "-cp", ";".join(cp),
                "-d", str(out)] + list(map(str, sources))
argfile = ROOT / "build/client-acceptance-javac.args"
argfile.write_text("\n".join('"' + arg.replace("\\", "/") + '"' for arg in compile_args), encoding="utf-8")
result = subprocess.run([str(Path(cfg["javaHome"]) / "bin/javac.exe"), "@" + str(argfile)])
if result.returncode:
    raise SystemExit(result.returncode)
run = ROOT / "build/acceptance-client"
mods = run / "mods"
mods.mkdir(parents=True, exist_ok=True)
for old in mods.glob("openblocks_reborn-*.jar"):
    old.unlink()
for path in [next((ROOT / "build/libs").glob("*.jar")), patchouli]:
    shutil.copy2(path, mods / path.name)
with zipfile.ZipFile(mods / "openblocks-redux-acceptance.jar", "w", zipfile.ZIP_DEFLATED) as jar:
    for path in out.rglob("*.class"):
        jar.write(path, path.relative_to(out).as_posix())
    jar.writestr("META-INF/neoforge.mods.toml", '''modLoader="javafml"
loaderVersion="[4,)"
license="CC0-1.0"
[[mods]]
modId="openblocks_redux_acceptance"
version="1.0.0"
displayName="OpenBlocks Redux Development Acceptance"
''')
world = run / "saves/ReduxTest"
if not world.exists():
    world.parent.mkdir(parents=True, exist_ok=True)
    shutil.copytree(ROOT / "build/acceptance-server/world", world)
(run / "options.txt").write_text("onboardAccessibility:false\ntutorialStep:none\njoinedFirstServer:true\n"
                                  "lang:zh_cn\nguiScale:3\nrenderDistance:4\nsimulationDistance:5\nmaxFps:60\n",
                                  encoding="utf-8")
home = Path(cfg["minecraftDir"])
version = home / "versions" / cfg["version"]
runtime = config_path.parent / "build/local-runtime.jar"
extra = config_path.parent / "build/client-extra.jar"
libraries = [runtime, extra] + [Path(p) for p in cfg["classpath"][2:]
    if "/libraries/" in p.replace("\\", "/") and not p.endswith("-universal.jar")]
module_jars = [p for p in libraries if any(s in p.as_posix() for s in
    ("/cpw/mods/bootstraplauncher/", "/cpw/mods/securejarhandler/", "/org/ow2/asm/", "/net/neoforged/JarJarFileSystems/"))]
natives = version / "natives-windows-x86_64"
args = ["-Xmx3G", "-Dfile.encoding=UTF-8", "-Dfml.earlyWindowControl=false", "-Dneoforge.enableGameTest=false",
    "-DignoreList=local-runtime.jar,client-extra.jar", "-DlibraryDirectory=" + str(home / "libraries"),
    "-DlegacyClassPath=" + ";".join(map(str, libraries)), "-p", ";".join(map(str, module_jars)),
    "--add-modules", "ALL-MODULE-PATH", "--add-opens", "java.base/java.util.jar=cpw.mods.securejarhandler",
    "--add-opens", "java.base/java.lang.invoke=cpw.mods.securejarhandler", "--add-exports", "java.base/sun.security.util=cpw.mods.securejarhandler",
    "--add-exports", "jdk.naming.dns/com.sun.jndi.dns=java.naming", "-Djava.library.path=" + str(natives),
    "-Dorg.lwjgl.system.SharedLibraryExtractPath=" + str(natives), "-cp", ";".join(map(str, libraries)),
    "cpw.mods.bootstraplauncher.BootstrapLauncher", "--launchTarget", "forgeclientuserdev", "--fml.neoForgeVersion", "21.1.262",
    "--fml.fmlVersion", "4.0.45", "--fml.mcVersion", "1.21.1", "--fml.neoFormVersion", "20240808.144430", "--gameDir", str(run),
    "--username", "OpenBlocksTester", "--version", "OpenBlocksReduxDev", "--accessToken", "0", "--assetsDir", str(home / "assets"),
    "--assetIndex", "17", "--width", "1280", "--height", "720", "--quickPlaySingleplayer", "ReduxTest"]
launch = run / "launch.args"
launch.write_text("\n".join('"' + arg.replace("\\", "/") + '"' for arg in args), encoding="utf-8")
print("Prepared isolated client:", run, flush=True)
if "--run" in sys.argv:
    env = os.environ.copy()
    env.pop("MOD_CLASSES", None)
    env.pop("JAVA_TOOL_OPTIONS", None)
    with (run / "console.log").open("w", encoding="utf-8") as log:
        process = subprocess.Popen([str(Path(cfg["javaHome"]) / "bin/java.exe"), "@" + str(launch)],
                                   cwd=run, env=env, stdout=log, stderr=subprocess.STDOUT)
        print("Started acceptance client PID", process.pid, flush=True)
        try:
            code = process.wait(timeout=180)
        finally:
            if process.poll() is None:
                process.terminate()
                process.wait(timeout=20)
        text = (run / "console.log").read_text(encoding="utf-8", errors="replace")
        print("\n".join(line for line in text.splitlines() if "REDUX_" in line))
        if code or "REDUX_CLIENT_COMPLETE failed=false" not in text:
            raise SystemExit(1)
