import os
import subprocess
import sys

output_dirs = {
    "case1": "./config/Etude2/configs1",
    "case2": "./config/Etude2/configs2",
    "case3": "./config/Etude2/configs3",
}
metrics_file = "metricsCheckpoint.txt"

jar_file = sys.argv[1]

src_dir = "./src"
classpath = (
    "../out/production/project_arav2:"
    "./:./peersim-1.0.5/peersim-1.0.5.jar:"
    "./peersim-1.0.5/jep-2.3.0.jar:./peersim-1.0.5/djep-1.0.0.jar"
) # à modifier selon le chemin vers les jar peerSim et les fichiers class

# Nombre de noeuds pour notre système
nodes = 30
proba_crash = 0 # 1.0 : avec panne    0.0 : sans panne

# Valeurs des paramètres pour chacun des cas étudiés
cases = {
    "case1": {"gamma_min": 50, "gamma_max": 50, "alpha": 50, "frequency": 400},
    "case2": {"gamma_min": 50, "gamma_max": 50, "alpha": 50, "frequency": 200},
    "case3": {"gamma_min": 50, "gamma_max": 50, "alpha": 50, "frequency": 100},
}


# Mise en place des directories pour la configuration
for case, dir_path in output_dirs.items():
    if os.path.exists(dir_path):
        # Effacer les fichiers de configs existants :
        for file in os.listdir(dir_path):
            os.remove(os.path.join(dir_path, file))
    else:
        os.makedirs(dir_path, exist_ok=True)

# Si le fichier metrics existe, effacer son contenu
if os.path.exists(metrics_file):
    with open(metrics_file, "w") as f:
        f.truncate(0)

# Template du fichier de configuration
template = """
network.size {nodes}
simulation.endtime 1000000
random.seed 20


protocol.transport UniformRandomTransport
protocol.transport.mindelay {gamma_min} # gamma
protocol.transport.maxdelay {gamma_max} # gamma

protocol.fifotransport FIFOTransport
protocol.fifotransport.transport transport

protocol.juang JuangVenkatesanAlgo
protocol.juang.transport fifotransport
protocol.juang.checkpointable naimitrehel
protocol.juang.timecheckpointing {frequency}

protocol.naimitrehel NaimiTrehelAlgoCheckpointable
protocol.naimitrehel.transport juang
protocol.naimitrehel.timeCS {alpha} #alpha
protocol.naimitrehel.timeBetweenCS {beta} #beta

control.crash CrashControler
control.crash.from 10000
control.crash.step 10000
#control.crash.faulty_nodes 1_2_3_4_5_6_7_8_9_10_11_12_13_14_15_16_17_18_19_20_21_22_23_24_25_26_27_28_29_30
control.crash.probacrash {proba_crash}
control.crash.checkpointer juang

control.statscollector ara.util.StatsCollectorCheckpoint
control.statscollector.at -1
control.statscollector.FINAL
control.statscollector.outputFile metrics
control.statscollector.beta {beta}
control.statscollector.case {case}
"""

# Generer les fichiers de config à partir du template
for case, params in cases.items():
    for beta in (1, 5, 10, 30, 60, 100, 500, 1000, 1500, 2000, 2500, 3000, 3500, 4000, 4500, 5000, 5500, 6000, 6500, 7000):
        dir_path = output_dirs[case]
        config_content = template.format(
            gamma_min=params["gamma_min"],
            gamma_max=params["gamma_max"],
            alpha=params["alpha"],
            frequency=params["frequency"],
            beta=beta,
            nodes=nodes,
            proba_crash=proba_crash,
            case=case
        )
        config_filename = os.path.join(dir_path, f"config_beta_{beta}.txt")
        with open(config_filename, "w") as f:
            f.write(config_content)
        # print(f"Generated: {config_filename}")

# Executer les simulations pour chacun des cas et des fichiers de configuration correspondants
for case, dir_path in output_dirs.items():
    for config_file in sorted(os.listdir(dir_path)):
        config_path = os.path.join(dir_path, config_file)
        print(f"Running simulation for: {config_path}")
        subprocess.run(["java", "-jar", jar_file, config_path],stderr=subprocess.STDOUT,check=True)