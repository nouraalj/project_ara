package ara.util;

import static ara.util.Constantes.log;

import ara.projet.mutex.NaimiTrehelAlgo;
import peersim.config.Configuration;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StatsCollector implements Control {
    private static final String PAR_PROTO_NT = "proto";
    private static final String PAR_BETA = "beta";
    private static final String PAR_CASE = "case"; // cas étudié (gamma << alpha ..)

    // on récupère aussi les valeurs d'alpha et gamma ((min delay + max delay)/2) pour les légendes des graphs
    private static final String PAR_ALPHA = "protocol.naimitrehel.timeCS";
    private static final String PAR_MIN_DELAY = "protocol.transport.mindelay";
    private static final String PAR_MAX_DELAY = "protocol.transport.maxdelay";


    private final int protoNTpid;
    private final long beta;
    private final String cases;
    private String filename = "metrics.txt";

    private final long alpha;
    private final long gamma;
    private final double P;

    public StatsCollector(String prefix) {
        this.protoNTpid = Configuration.getPid(prefix + "." + PAR_PROTO_NT);
        this.beta = Configuration.getLong(prefix + "." + PAR_BETA);
        this.cases = Configuration.getString(prefix + "." + PAR_CASE);

        this.alpha = Configuration.getLong(PAR_ALPHA);
        long minDelay = Configuration.getLong(PAR_MIN_DELAY);
        long maxDelay = Configuration.getLong(PAR_MAX_DELAY);
        this.gamma = (minDelay + maxDelay) / 2;

        this.P = ((double) (((double)alpha) + ((double)gamma))) / ((double)beta);

    }

    @Override
    public boolean execute() {
        int nb_nodes = Network.size();

        Map<String, Float> metrics = NaimiTrehelAlgo.get_metrics();

        float total_msg = metrics.get("total_msg");
        float total_request = metrics.get("total_request") / nb_nodes;
        float total_time_request = metrics.get("total_time_request")/ nb_nodes;
        float total_U = metrics.get("total_U");
        float total_T = metrics.get("total_T");
        float total_N = metrics.get("total_N");

        float total_time = total_U + total_T + total_N;
        total_U = (total_U / total_time) * 100;
        total_T = (total_T / total_time) * 100;
        total_N = (total_N / total_time) * 100;


        try(BufferedWriter bw = new BufferedWriter(new FileWriter(filename, true))) {
            if (new java.io.File(filename).length() == 0 ) {
                bw.write("Case;P;Alpha;Gamma;Beta;AppMessagesPerCS;MessagesRequestPerNode;AverageWaitingTime;TimeU;TimeT;TimeN\n");
                bw.flush();
            }
            bw.write(String.format(Locale.US,"%s;%.2f;%d;%d;%d;%.2f;%.2f;%.2f;%.2f;%.2f;%.2f\n",
                    cases,
                    P,
                    alpha,
                    gamma,
                    beta,
                    total_msg,
                    total_request,
                    total_time_request,
                    total_U,
                    total_T,
                    total_N));

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

}