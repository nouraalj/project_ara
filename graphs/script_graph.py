import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns


case_descriptions = {
    "case1": "γ << α",
    "case2": "γ ≈ α",
    "case3": "γ >> α",
}

# Messages applicatifs par Section Critique (on le fait pour chaque cas séparément):

df = pd.read_csv("../metrics.txt", sep=";")
groupedbycase = df.groupby(['Case', 'Beta']).mean().reset_index()
for case in groupedbycase['Case'].unique():
    subset = groupedbycase[groupedbycase['Case'] == case]
    plt.figure(figsize=(10, 6))

    plt.plot(
        subset['Beta'], subset['TokenMessagesPerCS'],
        label="Messages token par CS", color='blue'
    )
    plt.plot(
        subset['Beta'], subset['RequestMessagePerCs'],
        label="Messages request par CS",color='orange'
    )
    plt.plot(
        subset['Beta'],
        subset['TokenMessagesPerCS'] + subset['RequestMessagePerCs'],
        label="nombre de messages applicatifs par CS", color='green'
    )
    # Titre et légendes
    plt.title(f"Messages Applicatifs par Section Critique pour le cas {case_descriptions[case]} ")
    plt.xlabel("Beta (ms)")
    plt.ylabel("Nombre de messages")
    plt.legend()
    plt.tight_layout()
    plt.savefig(f"./outputs/MessagesPerCS_{case}.png")
    plt.close()

# Nombre de messages request par noeud :
plt.figure(figsize=(10, 6))
for case in groupedbycase['Case'].unique():
    subset = groupedbycase[groupedbycase['Case'] == case]
    plt.plot(subset['Beta'], subset['MessagesRequestPerNode'], label=f"{case} {case_descriptions[case]}")

plt.title("Nombre moyen de messages Request par noeud")
plt.xlabel("Beta (ms)")
plt.ylabel("Nombre de messages request moyen par noeud")
plt.legend()


plt.grid(True)
# à décommenter si on veut afficher le graphique à l'exécution
#plt.show()

plt.savefig("./outputs/NombreRequestParNode.png")
plt.close()

# Temps moyen d'attente (requesting) avant d'entrer en SC

plt.figure(figsize=(14, 6))
sns.boxplot(x='Beta', y='AverageWaitingTime', hue='Case', data=df, palette='Set3', showfliers=False)
plt.title("Temps moyen d'attente avant d'accèder la Section Critique ")
plt.xlabel("Beta")
plt.ylabel("Temps moyen d'attente (ms)")
handles, labels = plt.gca().get_legend_handles_labels()

# Create a custom legend where each case is associated with a description
new_labels = [f"{label} ({case_descriptions[label]})" for label in labels]
plt.legend(title="Cas", labels=new_labels, handles=handles, bbox_to_anchor=(1, 1), loc='upper left')

plt.grid(True)
plt.tight_layout()

# à décommenter si on veut afficher le graphique
#plt.show()

plt.savefig("./outputs/TempsMoyenRequesting.png")
plt.close()


# Temps moyen d'attente (requesting) avant d'entrer en SC pour le cas 1

df = pd.read_csv("../metrics.txt", sep=";")
states = ['TimeU', 'TimeT', 'TimeN']
state_labels = ['Used', 'Transit', 'Not Used']
groupedbycase = df.groupby(['Case', 'Beta']).mean().reset_index()

# on boucle sur chaque cas (3 fichiers diff)
for case in groupedbycase['Case'].unique():
    subset = groupedbycase[groupedbycase['Case'] == case]
    plt.figure(figsize=(10, 6))
    bottom = None
    for i, state in enumerate(states):
        plt.bar(
            subset['Beta'], subset[state], label=f"{state_labels[i]}",
            bottom=bottom, width=3
        )
        bottom = subset[state] if bottom is None else bottom + subset[state]

    plt.title(f"Répartition des États du Jeton pour {case_descriptions[case]} (Proportion)")
    plt.xlabel("Beta (ms)")
    plt.ylabel("Pourcentage du Temps (%)")
    plt.legend(title="État du Jeton")
    plt.grid(axis='y', linestyle='--', alpha=0.7)
    plt.tight_layout()

    # à décommenter si on veut afficher le graphique
    #plt.show()

    plt.savefig(f"./outputs/ProportionTokenStates_{case}.png")
