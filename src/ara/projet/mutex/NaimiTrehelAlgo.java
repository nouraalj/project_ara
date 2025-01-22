package ara.projet.mutex;

import static ara.util.Constantes.log;

import java.sql.Time;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.print.DocFlavor.INPUT_STREAM;

import ara.projet.mutex.InternalEvent.TypeEvent;
import ara.util.Message;
import ara.util.MyRandom;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
import peersim.edsim.EDSimulator;
import peersim.transport.Transport;


public class NaimiTrehelAlgo implements EDProtocol {

	// Nom des arguments du fichiers de configuration
	private static final String PAR_TRANSPORT = "transport";
	private static final String PAR_TIME_CS = "timeCS";
	private static final String PAR_TIME_BETWEEN_CS = "timeBetweenCS";

	// constantes de l'algorithme
	public static final long initial_owner = 0L;
	public static final long nil = -2L;

	// tag des messages
	// public static final String REQUEST_TAG = "request";
	// public static final String TOKEN_TAG = "token";

	// etats possibles du noeud dans l'application
	public static enum State {
		tranquil, requesting, inCS
	}

	// paramètres de l'algorithme lus depuis le fichier de configuration
	protected final long timeCS;
	protected final long timeBetweenCS;
	protected final int transport_id;
	protected final int protocol_id;

	// variables d'état de l'application
	protected State state;
	protected Queue<Long> next;
	protected long last;
	protected int nb_cs = 0;// permet de compter le nombre de section critiques
	// exécutées par le noeud

	protected int global_counter = 0; // compteur qui sera inclu dans le message
	// jeton, sa valeur est égale à la
	// dernière valeur connue
	// (i.e. depuis la dernière fois où le
	// noeud a vu passer le jeton)
	// ATTENTION, cette variable n'est pas
	// globale, elle est propre à chaque
	// noeud
	// mais ils ne peuvent
	// la modifier uniquement lorsqu'ils
	// possèdent le jeton

	protected int id_execution;// permet d'identifier l'id d'exécution,
	// incrémenté si l'application est
	// suspendue
	// (toujours constant dans cette classe mais
	// peut être incrémenté dans les sous-classes)



	/* ________________________________ Metric ________________________________ */

	private static int totalApplicatiftMessages = 0; // Nombre de messages applicatif (Token et Request) par SC
	private static int total_nb_cs = 0;

	private static int nb_request_par_noeud = 0; // Nombre de messages request par nœud


	private static long request_time_par_noeud=0;// Temps passé dans l'état requesting
	private long startReq = 0;

	public static long timeInU = 0;  // temps total passé par le jeton dans l'état "Utilisé"
	public static long timeInT = 0;  // temps total passé par le jeton dans l'état "Transit"
	public static long timeInN = 0;  // temps total passé par le jeton dans l'état "Non utilisé"
	private long startU=0;
	public static  long startT=0;
	private long startN=0;


	public NaimiTrehelAlgo(String prefix) {
		String tmp[] = prefix.split("\\.");
		protocol_id = Configuration.lookupPid(tmp[tmp.length - 1]);

		transport_id = Configuration.getPid(prefix + "." + PAR_TRANSPORT);
		timeCS = Configuration.getLong(prefix + "." + PAR_TIME_CS);
		timeBetweenCS = Configuration.getLong(prefix + "." + PAR_TIME_BETWEEN_CS);

	}

	public Object clone() {
		NaimiTrehelAlgo res = null;
		try {
			res = (NaimiTrehelAlgo) super.clone();
		} catch (CloneNotSupportedException e) {
		} // never happens
		res.initialisation(CommonState.getNode());

		return res;
	}

	@Override
	public void processEvent(Node node, int pid, Object event) {
		if (protocol_id != pid) {
			throw new RuntimeException("Receive an event for wrong protocol");
		}

		if (event instanceof InternalEvent) {
			InternalEvent ev = (InternalEvent) event;
			if (ev.getDate() == id_execution) {
				switch (ev.getType()) {
					case release_cs:
						nb_cs++;
						total_nb_cs++;
						this.releaseCS(node);
						break;
					case request_cs:
						this.requestCS(node);
						break;
					default:
						throw new RuntimeException("Receive unknown type event");
				}
			} else {
				//log.warning(node.getID() + " : ignoring obsolete event " + ev);
			}
		} else if (event instanceof Message) {
			Message m = (Message) event;
			if (m instanceof RequestMessage) {
				RequestMessage rm = (RequestMessage) m;
				this.receive_request(node, m.getIdSrc(), rm.getRequester());
			} else if (m instanceof TokenMessage) {
				TokenMessage tm = (TokenMessage) m;
				this.receive_token(node, tm.getIdSrc(), tm.getNext(), tm.getCounter());
			} else {
				throw new RuntimeException("Receive unknown type Message");
			}

		} else {
			throw new RuntimeException("Receive unknown type event");
		}

	}

	/////////////////////////////////////////// METHODES DE
	/////////////////////////////////////////// L'ALGORITHME////////////////////////////////////////////

	private void initialisation(Node host) {
		changestate(host, State.tranquil);
		next = new ArrayDeque<Long>();
		if (host.getID() == initial_owner) {
			last = nil;
		} else {
			last = initial_owner;
		}
	}

	private void executeCS(Node host) {
		//log.info("Node " + host.getID() + " executing its CS num " + nb_cs + " : next= " + next.toString());
		global_counter++;
		//log.info("Node " + host.getID() + " global counter = " + global_counter);
	}

	private void requestCS(Node host) {

		//log.fine("Node " + host.getID() + " requestCS");
		changestate(host, State.requesting);

		startReq = CommonState.getTime(); // Debut de request


		if (last != nil) {
			Transport tr = (Transport) host.getProtocol(transport_id);
			Node dest = Network.get((int) last);
			tr.send(host, dest, new RequestMessage(host.getID(), dest.getID(), protocol_id, host.getID()), protocol_id);
			totalApplicatiftMessages++;
			nb_request_par_noeud++;
			last = nil;
			return;// on simule un wait ici

		}else{ // j'ai deja le jeton
			timeInN += (CommonState.getTime()-startN); // Fin jeton N
			startN=0;
		}


		changestate(host, State.inCS);
		// DEBUT CS
	}

	private void releaseCS(Node host) {
		//log.fine("Node " + host.getID() + " releaseCS next=" + next);
		changestate(host, State.tranquil);
		if (!next.isEmpty()) {
			last = getLast(next);
			long next_holder = next.poll();// dequeue
			Transport tr = (Transport) host.getProtocol(transport_id);
			Node dest = Network.get((int) next_holder);
			//log.fine("Node " + host.getID() + " send token( counter = " + global_counter + " next =" + next + ") to "
			//		+ dest.getID());

			startT = CommonState.getTime(); // send Jeton
			timeInN += (CommonState.getTime()-startN); // Fin jeton N
			startN=0;

			tr.send(host, dest, new TokenMessage(host.getID(), dest.getID(), protocol_id, new ArrayDeque<Long>(next),
					global_counter), protocol_id);
			totalApplicatiftMessages++;

			next.clear();
		}
	}

	private void receive_request(Node host, long from, long requester) {
		//totalApplicatiftMessages++;

		//log.fine("Node " + host.getID() + " receive request message from Node " + from + " for Node " + requester);
		Transport tr = (Transport) host.getProtocol(transport_id);
		if (last == nil) {
			if (state != State.tranquil) {
				next.add(requester);
			} else {
				timeInN += (CommonState.getTime()-startN); // Fin jeton N
				startN=0;
				Node dest = Network.get((int) requester);
				//log.fine("Node " + host.getID() + " send token( counter = " + global_counter + " next =" + next
				//		+ ") to " + dest.getID() + " (no need)");
				startT = CommonState.getTime(); // send Jeton
				tr.send(host, dest, new TokenMessage(host.getID(), dest.getID(), protocol_id, new ArrayDeque<Long>(),
						global_counter), protocol_id);
				totalApplicatiftMessages++;
				last = requester;
			}
		} else {
			Node dest = Network.get((int) last);
			tr.send(host, dest, new RequestMessage(host.getID(), dest.getID(), protocol_id, requester), protocol_id);
			totalApplicatiftMessages++;
			nb_request_par_noeud++;
			last = requester;
		}
	}

	private void receive_token(Node host, long from, Queue<Long> remote_queue, int counter) {

		timeInT += (CommonState.getTime()-startT);	// receive Jeton
		startT=0;
		//log.fine("Node " + host.getID() + " receive token message (" + remote_queue.toString() + ", counter = "
		//		+ counter + ") from Node " + from + " next =" + next.toString());
		global_counter = counter;
		remote_queue.addAll(next);
		next = remote_queue;

		changestate(host, State.inCS);
	}

	/////////////////////////////////////////// METHODES
	/////////////////////////////////////////// UTILITAIRES////////////////////////////////////////////
	protected void changestate(Node host, State s) {
		this.state = s;
		switch (this.state) {
			case inCS:
				startU = CommonState.getTime();
				request_time_par_noeud += (CommonState.getTime() - startReq);// duree = fin - debut
				startReq=0;
				executeCS(host);
				schedule_release(host);
				break;
			case tranquil:
				timeInU += (CommonState.getTime()-startU);
				startU=0;
				startN = CommonState.getTime(); // Debut jeton N
				schedule_request(host);
				break;
			default:
		}
	}

	private static long getLast(Queue<Long> q) {
		Object tmp[] = q.toArray();
		return (Long) tmp[tmp.length - 1];
	}

	private void schedule_release(Node host) {
		long res = MyRandom.nextLong(timeCS, 0.1);
		EDSimulator.add(res, new InternalEvent(TypeEvent.release_cs, id_execution), host, protocol_id);

	}

	private void schedule_request(Node host) {
		long res = MyRandom.nextLong(timeBetweenCS, 0.1);
		EDSimulator.add(res, new InternalEvent(TypeEvent.request_cs, id_execution), host, protocol_id);

	}

	////////////////////////////////////////// classe des messages
	////////////////////////////////////////// /////////////////////////////////////

	public static class RequestMessage extends Message {

		private final long requester;

		public RequestMessage(long idsrc, long iddest, int pid, long initiator) {
			super(idsrc, iddest, pid);
			this.requester = initiator;
		}

		public long getRequester() {
			return requester;
		}

	}

	public static class TokenMessage extends Message {

		private final int counter;
		private final Queue<Long> next;

		public TokenMessage(long idsrc, long iddest, int pid, Queue<Long> next, int counter) {
			super(idsrc, iddest, pid);
			this.counter = counter;
			this.next = next;
		}

		public int getCounter() {
			return counter;
		}

		public Queue<Long> getNext() {
			return new ArrayDeque<Long>(next);
		}

		@Override
		public String toString() {
			return "TokenMessage( from=" + getIdSrc() + ", to = " + getIdDest() + "  counter = " + getCounter()
					+ " next = " + getNext() + ")";
		}

	}










	/////////////////////////////////////////// METHODES POUR LES
	/////////////////////////////////////////// STATISTIQUES ////////////////////////////////////////////



	public int getNbCs(){
		//List<Integer> nb_sc = new ArrayList<>(nb_sc_par_noeud.values());
		return nb_cs;
	}

	public int getNbRequest() {
		return nb_request_par_noeud;
	}

	public Long getRequest_time() {
		return request_time_par_noeud;
	}

	private static long getTimeInT() {
		if(startT>0)
			timeInT += (CommonState.getTime()-startT);
		startT=0;
		return timeInT;
	}

	private static long getTimeInU() {
		return timeInU;
	}
	private static long getTimeInN() {
		return timeInN;
	}

	private static  int getNbMsgPerCS(){
		log.info("sc : token : "+totalApplicatiftMessages);
		return totalApplicatiftMessages;

	}

	public static Map<String, Float> get_metrics() {
		Map<String, Float> metrics = new HashMap<>();
		metrics.put("total_msg", (float) (totalApplicatiftMessages) / total_nb_cs);
		metrics.put("total_request", (float) nb_request_par_noeud);
		metrics.put("total_time_request", (float) request_time_par_noeud);
		metrics.put("total_U", (float) getTimeInU());
		metrics.put("total_T", (float) getTimeInT());
		metrics.put("total_N", (float) getTimeInN());
		log.info("sc : "+total_nb_cs+" msg : "+totalApplicatiftMessages);
		return metrics;
	}

}