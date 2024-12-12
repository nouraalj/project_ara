package ara.util;
import ara.projet.mutex.NaimiTrehelAlgo;
import peersim.config.Configuration;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;

public class Initialisateur implements Control {

    private static final String PAR_PROTO_NT = "protoNTpid";
    private final int protoNTpid;

    public Initialisateur(String prefix) {
        this.protoNTpid = Configuration.getPid(prefix + "." + PAR_PROTO_NT);
    }

    @Override
    public boolean execute() {
        for(int i = 0; i < Network.size(); i++){
            Node node = Network.get(i);
            NaimiTrehelAlgo nta = (NaimiTrehelAlgo) node.getProtocol(protoNTpid);
            nta.initialisation(node);
        }
        return false;
    }
}