import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns
import numpy as np
import os

case_descriptions = {
    "case1": "frequency 400",
    "case2": "frequency 200",
    "case3": "frequency 100",
}

# Temps moyen de recouvrement:
df = pd.read_csv("../metricsCheckpoint.txt", sep=";")
groupedbycase = df.groupby(['Case', 'P']).mean().reset_index()

plt.figure(figsize=(10, 6))

# Obtenir les valeurs uniques de Beta et les trier
unique_beta_values = sorted(groupedbycase['P'].unique())
x_positions = range(len(unique_beta_values))  # Créer des indices uniformément espacés pour les valeurs de Beta

# Tracer les courbes pour chaque cas
for case in groupedbycase['Case'].unique():
    subset = groupedbycase[groupedbycase['Case'] == case]
    plt.plot([unique_beta_values.index(P) for P in subset['P']], subset['AverageTimeRecovery'], label=f"{case} {case_descriptions[case]}")

# Appliquer des labels de Beta uniformément espacés
plt.xticks(ticks=x_positions, labels=[f"{P:.2f}" for P in unique_beta_values])

# Titre et légendes
plt.title("Temps Moyen de Recouvrement")
plt.xlabel("Charge ρ = (α+γ)/β")
plt.ylabel("Temps")
plt.legend()
plt.grid(True)

# Sauvegarde de l'image
plt.tight_layout()
# Define output directory
output_dir = "outputs/Etude2"
os.makedirs(output_dir, exist_ok=True)  # Create the directory if it doesn't exist

# Save the figure
plt.savefig(os.path.join(output_dir, "TempsRecouvrement.png"))








# Nombre moyen de messages échangés par recouvrement

plt.figure(figsize=(10, 6))

# Obtenir les valeurs uniques de P
unique_p_values = sorted(groupedbycase['P'].unique())
x_positions = range(len(unique_p_values))  # Créer des indices uniformément espacés pour les valeurs de P

# Tracer les courbes pour chaque cas
for case in groupedbycase['Case'].unique():
    subset = groupedbycase[groupedbycase['Case'] == case]
    plt.plot([unique_p_values.index(p) for p in subset['P']], subset['TotalMsgRecovery'], label=f"{case} {case_descriptions[case]}")

# Appliquer des labels de P uniformément espacés
plt.xticks(ticks=x_positions, labels=[f"{p:.2f}" for p in unique_p_values])

# Titre et légendes
plt.title("Nombre Moyen de Messages Échangés par Recouvrement")
plt.xlabel("Charge ρ = (α+γ)/β")
plt.ylabel("Messages Échangés")
plt.legend()
plt.grid(True)

# Ajuster la disposition et sauvegarder
plt.tight_layout()
plt.savefig("outputs/Etude2/NombreMessage.png")
plt.close()















# Ancienneté moyenne de la ligne de recouvrement

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
        subset['AgeRecoveryLine'],
        label=f"{case} {case_descriptions[case]}"
    )

# Appliquer des labels de P uniformément espacés
plt.xticks(ticks=x_positions, labels=[f"{p:.2f}" for p in unique_p_values])

# Titre et légendes
plt.title("Ancienneté Moyenne de la Ligne de Recouvrement")
plt.xlabel("Charge ρ = (α+γ)/β")
plt.ylabel("Nombre de Recouvrement")
plt.legend()
plt.grid(True)

# Ajuster la disposition et sauvegarder
plt.tight_layout()
plt.savefig("outputs/Etude2/Ancienneté.png")
plt.close()





# Coût moyen en mémoire des points de reprise

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
        subset['SizeCheckpoint'],
        label=f"{case} {case_descriptions[case]}"
    )

# Appliquer des labels de P uniformément espacés
plt.xticks(ticks=x_positions, labels=[f"{p:.2f}" for p in unique_p_values])

# Titre et légendes
plt.title("Coût Moyen en Mémoire des Points de Reprise")
plt.xlabel("Charge ρ = (α+γ)/β")
plt.ylabel("Taille Octet")
plt.legend()
plt.grid(True)

# Ajuster la disposition et sauvegarder
plt.tight_layout()
plt.savefig("outputs/Etude2/SizeCheckpoint.png")
plt.close()