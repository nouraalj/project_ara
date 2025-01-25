package ara.util;

import static ara.util.Constantes.log;

import ara.projet.checkpointing.algorithm.JuangVenkatesanAlgo;
import ara.projet.mutex.NaimiTrehelAlgoCheckpointable;
import peersim.config.Configuration;
import peersim.core.Control;
import peersim.core.Network;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;

public class StatsCollectorCheckpoint implements Control {
    private static final String PAR_PROTO_NT = "proto";
    private static final String PAR_BETA = "beta";
    private static final String PAR_CASE = "case"; // cas étudié (freq ou msg)
    private static final String PAR_FREQUENCY = "protocol.juang.timecheckpointing"; // fréquence pour caseFreq

    // on récupère aussi les valeurs d'alpha et gamma ((min delay + max delay)/2) pour les légendes des graphs
    private static final String PAR_ALPHA = "protocol.naimitrehel.timeCS";
    private static final String PAR_MIN_DELAY = "protocol.transport.mindelay";
    private static final String PAR_MAX_DELAY = "protocol.transport.maxdelay";


    private final long beta;
    private final String cases;
    private final long frequency;
    private String filename = "metricsCheckpoint.txt";

    private final long alpha;
    private final long gamma;
    private final double P;

    public StatsCollectorCheckpoint(String prefix) {
        this.beta = Configuration.getLong(prefix + "." + PAR_BETA);
        this.cases = Configuration.getString(prefix + "." + PAR_CASE);
        this.frequency = Configuration.getLong(PAR_FREQUENCY);

        this.alpha = Configuration.getLong(PAR_ALPHA);
        long minDelay = Configuration.getLong(PAR_MIN_DELAY);
        long maxDelay = Configuration.getLong(PAR_MAX_DELAY);
        this.gamma = (minDelay + maxDelay) / 2;

        this.P = ((double) (((double)alpha) + ((double)gamma))) / ((double)beta);

    }

    @Override
    public boolean execute() {
        int nb_nodes = Network.size();

        Map<String, Double> metrics = JuangVenkatesanAlgo.get_metrics();


        double avg_time = metrics.get("avg_time");
        double total_msg = metrics.get("total_msg");
        double anciennete = metrics.get("ancien");
        double total_taille = metrics.get("taille");


        try(BufferedWriter bw = new BufferedWriter(new FileWriter(filename, true))) {
            if (new java.io.File(filename).length() == 0 ) {
                bw.write("Case;P;Frequency;AverageTimeRecovery;TotalMsgRecovery;AgeRecoveryLine;SizeCheckpoint\n");
                bw.flush();
            }
            bw.write(String.format(Locale.US,"%s;%.2f;%.2f;%.2f;%.2f;%.2f;%.2f\n",
                    cases,
                    P,
                    (float) frequency,
                    avg_time,
                    total_msg,
                    anciennete,
                    total_taille));

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

}