import os

output_dir = "./configs1"
os.makedirs(output_dir, exist_ok=True)

beta_min = 10
beta_max = 100
step = 10

template = """
network.size 10
simulation.endtime 10000
random.seed 20

protocol.transport UniformRandomTransport
protocol.transport.mindelay 10 # gamma
protocol.transport.maxdelay 30

protocol.naimitrehel NaimiTrehelAlgo
protocol.naimitrehel.transport transport
protocol.naimitrehel.timeCS 250 # alpha
protocol.naimitrehel.timeBetweenCS {beta} # beta

init.i Initialisateur
init.i.protoNTpid naimitrehel

control.statscollector StatsCollector
control.statscollector.at -1
control.statscollector.FINAL
control.statscollector.proto naimitrehel
control.statscollector.case 1
control.statscollector.beta {beta} # beta

"""

for beta in range(beta_min, beta_max + step, step):
    config = template.format(beta=beta)
    with open(f"{output_dir}/config_beta_{beta}.txt", "w") as f:
        f.write(config)
