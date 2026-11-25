"""Start an isolated dedicated server with OpenBlocks and Patchouli, then stop.

Usage: python tools/run_server_acceptance.py SERVER_INSTALL JDK
Only reads the existing installation's libraries, dependency JARs and accepted EULA.
All game/config/world/log writes occur under this project's build/acceptance-server.
"""
from pathlib import Path
import re
import shlex
import shutil
import subprocess
import sys
import time

ROOT = Path(__file__).resolve().parents[1]
test_class = sys.argv[sys.argv.index("--test-class") + 1] if "--test-class" in sys.argv else "ReduxGameTests"
expected = int(sys.argv[sys.argv.index("--expected") + 1]) if "--expected" in sys.argv else 5
failure_prefix = "[minecraft/LogTestReporter]: " + test_class.lower() + "."
install = Path(sys.argv[1]).resolve()
jdk = Path(sys.argv[2]).resolve()
runtime_args = sorted((install / "libraries/net/neoforged/neoforge").glob("*/win_args.txt"))[-1]
run = ROOT / "build/acceptance-server"
mods = run / "mods"
mods.mkdir(parents=True, exist_ok=True)
if test_class == "EnchantmentGameTests":
    (run / "config").mkdir(exist_ok=True)
    flag = "true" if "--all-treasure" in sys.argv else "false"
    (run / "config/openblocks_reborn-enchantment-acquisition.toml").write_text(
        "[treasure]\n" + "".join(name + " = " + flag + "\n" for name in ("lastStand", "unstable", "flimFlam")), encoding="utf-8")
for old in mods.glob("openblocks_reborn-*.jar"):
    old.unlink()
for old in mods.glob("*elevatorid*.jar"):
    old.unlink()
props = dict(re.findall(r"^([\w_]+)=(.*)$", (ROOT / "gradle.properties").read_text(), re.M))
for path in [ROOT / "build/libs" / (props["mod_id"] + "-" + props["mod_version"] + ".jar"),
             next((install / "mods").glob("*Patchouli-1.21.1-93-NEOFORGE.jar"))]:
    shutil.copy2(path, mods / path.name)
eula = install / "eula.txt"
if not eula.exists() or "eula=true" not in eula.read_text(encoding="utf-8-sig"):
    raise SystemExit("The existing server has no accepted EULA; no server started.")
shutil.copy2(eula, run / "eula.txt")
(run / "server.properties").write_text(
    "online-mode=false\nserver-ip=127.0.0.1\nserver-port=25793\nlevel-type=minecraft:flat\n"
    "generate-structures=false\nview-distance=2\nsimulation-distance=2\nspawn-protection=0\n"
    "max-tick-time=120000\n", encoding="utf-8")
args = shlex.split(runtime_args.read_text(encoding="utf-8-sig"))
args = [arg.replace("libraries/", install.as_posix() + "/libraries/")
        .replace("-DlibraryDirectory=libraries", "-DlibraryDirectory=" + str(install / "libraries"))
        for arg in args]
if "--userdev" in sys.argv:
    import json
    config_path = Path(sys.argv[sys.argv.index("--userdev") + 1])
    cfg = json.loads(config_path.read_text(encoding="utf-8"))
    runtime = config_path.parent / "build/local-runtime.jar"
    extra = config_path.parent / "build/client-extra.jar"
    if not runtime.exists() or not extra.exists():
        raise RuntimeError("Build the existing named development runtime first")
    cp = [runtime, extra] + [Path(p) for p in cfg["classpath"][2:]
                              if "/libraries/" in p.replace("\\", "/")
                              and not p.endswith("-universal.jar")]
    modules = [p for p in cp if any(s in p.as_posix() for s in
               ("/cpw/mods/bootstraplauncher/", "/cpw/mods/securejarhandler/", "/org/ow2/asm/", "/net/neoforged/JarJarFileSystems/"))]
    args = ["-DignoreList=local-runtime.jar,client-extra.jar", "-DlibraryDirectory=" + str(Path(cfg["minecraftDir"]) / "libraries"),
            "-DlegacyClassPath=" + ";".join(map(str, cp)), "-p", ";".join(map(str, modules)),
            "--add-modules", "ALL-MODULE-PATH", "--add-opens", "java.base/java.util.jar=cpw.mods.securejarhandler",
            "--add-opens", "java.base/java.lang.invoke=cpw.mods.securejarhandler",
            "--add-exports", "java.base/sun.security.util=cpw.mods.securejarhandler",
            "--add-exports", "jdk.naming.dns/com.sun.jndi.dns=java.naming", "-cp", ";".join(map(str, cp)),
            "cpw.mods.bootstraplauncher.BootstrapLauncher", "--launchTarget", "forgeserveruserdev",
            "--fml.neoForgeVersion", "21.1.262", "--fml.fmlVersion", "4.0.45", "--fml.mcVersion", "1.21.1",
            "--fml.neoFormVersion", "20240808.144430", "--gameDir", str(run)]
args = ["-Xmx3G", "-Dfile.encoding=UTF-8", "-Dfml.earlyWindowControl=false",
        "-Dneoforge.enableGameTest=true", "-Dneoforge.enabledGameTestNamespaces=openblocks_reborn"] + args + ["--nogui"]
argfile = run / "launch.args"
argfile.write_text("\n".join('"' + a.replace("\\", "/") + '"' for a in args), encoding="utf-8")
log_path = run / ("console-production.log" if "--startup-only" in sys.argv else "console-gametest.log")
start = time.monotonic()
triggered = False
stopped = False
failed = False
with log_path.open("w", encoding="utf-8") as log:
    process = subprocess.Popen([str(jdk / "bin/java.exe"), "@" + str(argfile)], cwd=run,
                               stdin=subprocess.PIPE, stdout=log, stderr=subprocess.STDOUT)
    print("Started isolated acceptance server PID", process.pid, flush=True)
    try:
        while process.poll() is None:
            time.sleep(1)
            text = log_path.read_text(encoding="utf-8", errors="replace")
            if not triggered and 'Done (' in text:
                process.stdin.write(b"stop\n" if "--startup-only" in sys.argv else ("test runall " + test_class + "\n").encode())
                process.stdin.flush()
                triggered = True
                stopped = "--startup-only" in sys.argv
                print("Production startup verified" if stopped else "Started redux game tests", flush=True)
            test_results = text.count("REDUX_TEST_PASS ")
            if triggered and not stopped and (test_results >= expected or failure_prefix in text or any(s in text.lower() for s in
                    ("tests complete", "no tests found", "no test functions", "unknown or incomplete command", "incorrect argument for command"))):
                failed = any(s in text.lower() for s in ("required tests failed", "no tests found", "no test functions", "unknown or incomplete command", "incorrect argument for command")) or failure_prefix in text or test_results < expected
                process.stdin.write(b"stop\n")
                process.stdin.flush()
                stopped = True
            if time.monotonic() - start > 180:
                process.stdin.write(b"stop\n")
                process.stdin.flush()
                process.wait(timeout=30)
                raise RuntimeError("Acceptance timeout; inspect " + str(log_path))
        print("Server exit", process.returncode, "tests triggered", triggered, flush=True)
    finally:
        if process.poll() is None:
            process.terminate()
            process.wait(timeout=20)
text = log_path.read_text(encoding="utf-8", errors="replace")
print("\n".join(text.splitlines()[-35:]))
if process.returncode or failed or not triggered or not stopped:
    raise SystemExit(1)
