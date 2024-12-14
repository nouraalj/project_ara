package ara.util;

import static ara.util.Constantes.log;

import ara.projet.mutex.NaimiTrehelAlgo;
import peersim.config.Configuration;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class StatsCollector implements Control {
    private static final String PAR_PROTO_NT = "proto";
    private static final String PAR_BETA = "beta";
    private static final String PAR_CASE = "case"; // cas étudié (gamma << alpha ..)


    private final int protoNTpid;
    private final long beta;
    private final String cases;
    private String filename = "metrics.txt";

    public StatsCollector(String prefix) {
        this.protoNTpid = Configuration.getPid(prefix + "." + PAR_PROTO_NT);
        this.beta = Configuration.getLong(prefix + "." + PAR_BETA);
        this.cases = Configuration.getString(prefix + "." + PAR_CASE);
    }

    @Override
    public boolean execute() {
        try(BufferedWriter bw = new BufferedWriter(new FileWriter(filename, true))) {
            if (new java.io.File(filename).length() == 0) {
                bw.write("Case,Beta,NodeId,AppMessagesPerCS,MessagesRequestPerNode,AverageWaitingTime,TimeU,TimeT,TimeN\n");
                bw.flush();
            }

            Node node = Network.get(0);
            NaimiTrehelAlgo proto = (NaimiTrehelAlgo) node.getProtocol(protoNTpid);
            long tokenTime = proto.getTimeInN() + proto.getTimeInT() + proto.getTimeInU();
            double percentU = (proto.getTimeInU() / (double) tokenTime) * 100.0;
            double percentT = (proto.getTimeInT() / (double) tokenTime) * 100.0;
            double percentN = (proto.getTimeInT() / (double) tokenTime) * 100.0;

            for (int i = 0; i < Network.size(); i++){
                Node n= Network.get(i);
                NaimiTrehelAlgo nTpro = (NaimiTrehelAlgo) n.getProtocol(protoNTpid);
                int nb_request = nTpro.getNbRequest();
                double nbAppMsgPerCS = (double) nTpro.getNbMsgPerCS().stream().mapToInt(Integer::intValue).sum() / nTpro.getNbCs();
                double avgWaitingTime = (double) nTpro.getRequest_time() / nb_request;
                bw.write(String.format("%s;%d;%d;%.2f;%d;%.2f;%.2f;%.2f;%.2f\n",
                        cases,
                        beta,
                        n.getID(),
                        nbAppMsgPerCS,
                        nb_request,
                        avgWaitingTime,
                        percentU,
                        percentT,
                        percentN));
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
