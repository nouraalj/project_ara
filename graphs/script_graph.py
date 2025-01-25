import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns
import numpy as np



case_descriptions = {
    "case1": "γ << α",
    "case2": "γ ≈ α",
    "case3": "γ >> α",
}

# Messages applicatifs par Section Critique :
df = pd.read_csv("../metrics.txt", sep=";")
groupedbycase = df.groupby(['Case', 'P']).mean().reset_index()

plt.figure(figsize=(10, 6))

# Obtenir les valeurs uniques de Beta et les trier
unique_beta_values = sorted(groupedbycase['P'].unique())
x_positions = range(len(unique_beta_values))  # Créer des indices uniformément espacés pour les valeurs de Beta

# Tracer les courbes pour chaque cas
for case in groupedbycase['Case'].unique():
    subset = groupedbycase[groupedbycase['Case'] == case]
    plt.plot([unique_beta_values.index(beta) for beta in subset['P']], subset['AppMessagesPerCS'], label=f"{case} {case_descriptions[case]}")

# Appliquer des labels de Beta uniformément espacés
plt.xticks(ticks=x_positions, labels=[f"{beta:.2f}" for beta in unique_beta_values])

# Titre et légendes
plt.title("Messages Applicatifs par Section Critique")
plt.xlabel("P")
plt.ylabel("Nombre de messages applicatifs")
plt.legend()
plt.grid(True)

# Sauvegarde de l'image
plt.tight_layout()
plt.savefig("outputs/Etude1/MessagesApplicatifsParCS.png")










# Nombre de messages request par nœud

plt.figure(figsize=(10, 6))

# Obtenir les valeurs uniques de P
unique_p_values = sorted(groupedbycase['P'].unique())
x_positions = range(len(unique_p_values))  # Créer des indices uniformément espacés pour les valeurs de P

# Tracer les courbes pour chaque cas
for case in groupedbycase['Case'].unique():
    subset = groupedbycase[groupedbycase['Case'] == case]
    plt.plot([unique_p_values.index(p) for p in subset['P']], subset['MessagesRequestPerNode'], label=f"{case} {case_descriptions[case]}")

# Appliquer des labels de P uniformément espacés
plt.xticks(ticks=x_positions, labels=[f"{p:.2f}" for p in unique_p_values])

# Titre et légendes
plt.title("Nombre moyen de messages Request par noeud")
plt.xlabel("P")
plt.ylabel("Nombre de messages request moyen par noeud")
plt.legend()
plt.grid(True)

# Ajuster la disposition et sauvegarder
plt.tight_layout()
plt.savefig("outputs/Etude1/NombreRequestParNode.png")
plt.close()















# Temps moyen d'attente (requesting) avant d'entrer en SC

# Charger les données
df = pd.read_csv("../metrics.txt", sep=";")

# Calculer la moyenne des temps d'attente par cas et par P
groupedbycase = df.groupby(['Case', 'P']).mean().reset_index()

# Initialisation du graphique
plt.figure(figsize=(10, 6))

# Obtenir les valeurs uniques de P
unique_p_values = sorted(groupedbycase['P'].unique())
x_positions = range(len(unique_p_values))  # Créer des indices uniformément espacés pour les valeurs de P

# Tracer les courbes pour chaque cas
for case in groupedbycase['Case'].unique():
    subset = groupedbycase[groupedbycase['Case'] == case]
    plt.plot(
        [unique_p_values.index(p) for p in subset['P']],
        subset['AverageWaitingTime'],
        label=f"{case} {case_descriptions[case]}"
    )

# Appliquer des labels de P uniformément espacés
plt.xticks(ticks=x_positions, labels=[f"{p:.2f}" for p in unique_p_values])

# Titre et légendes
plt.title("Temps moyen d'attente avant d'accéder à la Section Critique")
plt.xlabel("P")
plt.ylabel("Temps moyen d'attente (ms)")
plt.legend()
plt.grid(True)

# Ajuster la disposition et sauvegarder
plt.tight_layout()
plt.savefig("outputs/Etude1/TempsMoyenRequesting.png")
plt.close()














# Temps moyen de chaque Jeton

df = pd.read_csv("../metrics.txt", sep=";")
states = ['TimeU', 'TimeT', 'TimeN']
state_labels = ['Used', 'Transit', 'Not Used']
groupedbycase = df.groupby(['Case', 'P']).mean().reset_index()

# on boucle sur chaque cas (3 fichiers diff)
for case in groupedbycase['Case'].unique():
    subset = groupedbycase[groupedbycase['Case'] == case]
    plt.figure(figsize=(10, 6))
    bottom = None

    # Obtenir les valeurs uniques de P et les trier
    unique_p_values = sorted(subset['P'].unique())
    x_positions = range(len(unique_p_values))  # Indices uniformément espacés pour les valeurs de P

    # Tracer les barres empilées pour chaque état
    for i, state in enumerate(states):
        plt.bar(
            [unique_p_values.index(p) for p in subset['P']], subset[state], label=f"{state_labels[i]}",
            bottom=bottom, width=1
        )
        bottom = subset[state] if bottom is None else bottom + subset[state]

    # Appliquer des labels uniformément espacés pour P
    plt.xticks(ticks=x_positions, labels=[f"{p:.2f}" for p in unique_p_values])

    plt.title(f"Répartition des États du Jeton pour {case_descriptions[case]} (Proportion)")
    plt.xlabel("P")
    plt.ylabel("Pourcentage du Temps (%)")
    plt.legend(title="État du Jeton")
    plt.grid(axis='y', linestyle='--', alpha=0.7)
    plt.tight_layout()

    plt.savefig(f"outputs/Etude1/ProportionTokenStates_{case}.png")
