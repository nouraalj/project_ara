import os
import subprocess

output_dirs = {
    "case1": "./config/configs1",
    "case2": "./config/configs2",
    "case3": "./config/configs3",
}
metrics_file = "metrics.txt"

src_dir = "./src"
classpath = (
    "./out/production/project_ara:" # répertoire contenant les .class
    "./:./peersim-1.0.5/peersim-1.0.5.jar:"
    "./peersim-1.0.5/jep-2.3.0.jar:./peersim-1.0.5/djep-1.0.0.jar"
) # à modifier selon le chemin vers les jar peerSim et les fichiers class

# valeurs des paramètres pour chacun des cas étudiés
cases = {
    "case1": {"gamma_min": 10, "gamma_max": 30, "alpha": 250},
    "case2": {"gamma_min": 50, "gamma_max": 100, "alpha": 250},
    "case3": {"gamma_min": 300, "gamma_max": 500, "alpha": 250},
}
beta_min = 10
beta_max = 100
step = 10

# mise en place des directories pour la configuration
for case, dir_path in output_dirs.items():
    if os.path.exists(dir_path):
        # effacer les fichiers de configs existants :
        for file in os.listdir(dir_path):
            os.remove(os.path.join(dir_path, file))
    else:
        os.makedirs(dir_path, exist_ok=True)

# si le fichier metrics existe, effacer son contenu
if os.path.exists(metrics_file):
    with open(metrics_file, "w") as f:
        f.truncate(0)

# Configuration template
template = """
network.size 10
simulation.endtime 10000
random.seed 20

protocol.transport UniformRandomTransport
protocol.transport.mindelay {gamma_min} # gamma
protocol.transport.maxdelay {gamma_max}

protocol.naimitrehel ara.projet.mutex.NaimiTrehelAlgo
protocol.naimitrehel.transport transport
protocol.naimitrehel.timeCS {alpha} # alpha
protocol.naimitrehel.timeBetweenCS {beta} # beta

init.i ara.util.Initialisateur
init.i.protoNTpid naimitrehel

control.statscollector ara.util.StatsCollector
control.statscollector.at -1
control.statscollector.FINAL
control.statscollector.outputFile metrics.txt
control.statscollector.proto naimitrehel
control.statscollector.beta {beta}
control.statscollector.case {case}
"""

# generer les fichiers de config
for case, params in cases.items():
    for beta in range(beta_min, beta_max + step, step):
        dir_path = output_dirs[case]
        config_content = template.format(
            gamma_min=params["gamma_min"],
            gamma_max=params["gamma_max"],
            alpha=params["alpha"],
            beta=beta,
            case=case
        )
        config_filename = os.path.join(dir_path, f"config_beta_{beta}.txt")
        with open(config_filename, "w") as f:
            f.write(config_content)
        # print(f"Generated: {config_filename}")

# executer les simulations
for case, dir_path in output_dirs.items():
    for config_file in sorted(os.listdir(dir_path)):
        config_path = os.path.join(dir_path, config_file)
        print(f"Running simulation for: {config_path}")
        subprocess.run(["java", "-cp", classpath, "peersim.Simulator", config_path])
