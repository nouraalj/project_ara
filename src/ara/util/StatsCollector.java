package ara.util;

import static ara.util.Constantes.log;

import peersim.config.Configuration;
import peersim.core.Control;

import java.io.File;

public class StatsCollector implements Control {
    private static final String PAR_PROTO_NT = "protoNTpid";
    private final int protoNTpid;

    private File msgAppPerCs, nbReq, timePerStatePercentage;

    public StatsCollector(String prefix) {
        this.protoNTpid = Configuration.getPid(prefix + "." + PAR_PROTO_NT);

    }

    @Override
    public boolean execute() {
        return false;
    }
}
