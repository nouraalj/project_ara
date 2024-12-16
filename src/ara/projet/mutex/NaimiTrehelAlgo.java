package ara.projet.mutex;

import static ara.util.Constantes.log;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

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

	// Nom des arguments des fichiers de configuration
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
	// etats possibles du token dans l'application
	public static enum TokenState {
		used, transit, not_used
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

	// variables de statistiques pour les métriques
	protected  int nb_request= 0; // Nombre de fois que le noeud a demandé l'accés à la section critique
	//public static Map<Integer, Integer> nb_messToken_per_cs = new HashMap<>(); // Nombre de message token par SC
	//public static Map<Integer, Integer> nb_messRequest_per_cs = new HashMap<>(); // Nombre de message requete par SC
	public static Map<Integer, Integer> nb_messApp_per_cs = new HashMap<>(); // Nombre de requete par SC
	protected long request_time = -1; // Temps passé dans l'état requesting

	// variables globales pour les temps passé dans un des états du jeton :
	protected long timeInU = 0;  // temps total passé par le jeton dans l'état "Utilisé"
	protected long timeInT = 0;  // temps total passé par le jeton dans l'état "Transit"
	protected long timeInN = 0;  // temps total passé par le jeton dans l'état "Non utilisé"
	public static TokenState globalTokenState = TokenState.not_used; // etat du jeton initialement

	protected long lastTokenStateChange = 0; // variable stockant l'heure de la dernière transition du jeton

	// private static BufferedWriter logWriter; 	// Writer pour écrire les logs dans un fichier
	// static {
	// try {
	// logWriter = new BufferedWriter(new FileWriter("simulation_logs.txt"));
	// } catch (IOException e) {
	// e.printStackTrace(); }}
	protected long tokenLastStateChangeTime = 0; // Temps du dernier changement d’état du jeton

	public NaimiTrehelAlgo(String prefix) {
		String tmp[] = prefix.split("\\.");
		protocol_id = Configuration.lookupPid(tmp[tmp.length - 1]);

		transport_id = Configuration.getPid(prefix + "." + PAR_TRANSPORT);
		timeCS = Configuration.getLong(prefix + "." + PAR_TIME_CS); // alpha
		timeBetweenCS = Configuration.getLong(prefix + "." + PAR_TIME_BETWEEN_CS); // beta

	}
	/* public static void writeLog(String message) {
		// fonction d'écriture sur le fichier log des valeurs des métriques
		try {
			logWriter.write(message + "\n");  // écrire le message dans le fichier avec un saut de ligne
			logWriter.flush();  //s'assurer que les données sont écrites immédiatement
		} catch (IOException e) {
			e.printStackTrace();
		}
	} */
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
					this.releaseCS(node);
					break;
				case request_cs:
					nb_request++;
					this.requestCS(node);
					break;
				default:
					throw new RuntimeException("Receive unknown type event");
				}
			} else {
				log.warning(node.getID() + " : ignoring obsolete event " + ev);
			}
		} else if (event instanceof Message) {
			Message m = (Message) event;
			if (m instanceof RequestMessage) {
				RequestMessage rm = (RequestMessage) m;
				this.receive_request(node, m.getIdSrc(), rm.getRequester());
				nb_messApp_per_cs.put(nb_cs, nb_messApp_per_cs.getOrDefault(nb_cs, 0) + 1);			} else if (m instanceof TokenMessage) {
				TokenMessage tm = (TokenMessage) m;
				this.receive_token(node, tm.getIdSrc(), tm.getNext(), tm.getCounter());
				nb_messApp_per_cs.put(nb_cs, nb_messApp_per_cs.getOrDefault(nb_cs, 0) + 1);			} else {
				throw new RuntimeException("Receive unknown type Message");
			}
			// writeLog("Node "+node.getID()+" Section Critique = "+nb_cs+" | Messages applicatifs = "+ nb_messApp_per_cs.get(nb_cs));

		} else {
			throw new RuntimeException("Receive unknown type event");
		}

	}

	/////////////////////////////////////////// METHODES DE
	/////////////////////////////////////////// L'ALGORITHME////////////////////////////////////////////
	private void executeCS(Node host) {
		changeTokenState(TokenState.used); // token etat utilise
		log.info("Node " + host.getID() + " executing its CS num " + nb_cs + " : next= " + next.toString());
		global_counter++;
		log.info("Node " + host.getID() + " global counter = " + global_counter);
	}

	public void initialisation(Node host) {
		changestate(host, State.tranquil);
		next = new ArrayDeque<Long>();
		if (host.getID() == initial_owner) {
			last = nil;
		} else {
			last = initial_owner;
		}

	}

	private void requestCS(Node host) {
		log.fine("Node " + host.getID() + " requestCS");
		changestate(host, State.requesting);
		long requestingStartTime = CommonState.getTime();
		if (last != nil) {
			Transport tr = (Transport) host.getProtocol(transport_id);
			Node dest = Network.get((int) last);
			tr.send(host, dest, new RequestMessage(host.getID(), dest.getID(), protocol_id, host.getID()), protocol_id);
			last = nil;
			return;// on simule un wait ici
		}
		changestate(host, State.inCS);
		// DEBUT CS
		long SCStartTime = CommonState.getEndTime() - requestingStartTime;
		request_time += SCStartTime; // Temps Total = Debut CS - Debut Request
		// moy_request_time=request_time/nbr_request_par_noeud; IL faut le temps total pas le temps passé pour une seule requête
		// writeLog("Node "+host.getID()+": Request = "+nbr_request_par_noeud+" | Time Requesting... = "+SCStartTime+" | Total = "+request_time+" | Moyenne = "+moy_request_time); à mettre autre part.
	}

	private void releaseCS(Node host) {
		log.fine("Node " + host.getID() + " releaseCS next=" + next);
		changestate(host, State.tranquil);
		if (!next.isEmpty()) {
			last = getLast(next);
			long next_holder = next.poll();// dequeue
			Transport tr = (Transport) host.getProtocol(transport_id);
			Node dest = Network.get((int) next_holder);
			log.fine("Node " + host.getID() + " send token( counter = " + global_counter + " next =" + next + ") to "
					+ dest.getID());
			tr.send(host, dest, new TokenMessage(host.getID(), dest.getID(), protocol_id, new ArrayDeque<Long>(next),
					global_counter), protocol_id);
			changeTokenState(TokenState.transit);
			next.clear();
		}
	}

	private void receive_request(Node host, long from, long requester) {
		log.fine("Node " + host.getID() + " receive request message from Node " + from + " for Node " + requester);
		Transport tr = (Transport) host.getProtocol(transport_id);
		if (last == nil) {
			if (state != State.tranquil) {
				next.add(requester);

			} else {
				Node dest = Network.get((int) requester);
				log.fine("Node " + host.getID() + " send token( counter = " + global_counter + " next =" + next
						+ ") to " + dest.getID() + " (no need)");
				tr.send(host, dest, new TokenMessage(host.getID(), dest.getID(), protocol_id, new ArrayDeque<Long>(),
						global_counter), protocol_id);
				changeTokenState(TokenState.transit);
				last = requester;
			}
		} else {
			Node dest = Network.get((int) last);
			tr.send(host, dest, new RequestMessage(host.getID(), dest.getID(), protocol_id, requester), protocol_id);
			last = requester;
		}
	}

	private void receive_token(Node host, long from, Queue<Long> remote_queue, int counter) {
		log.fine("Node " + host.getID() + " receive token message (" + remote_queue.toString() + ", counter = "
				+ counter + ") from Node " + from + " next =" + next.toString());
		global_counter = counter;
		remote_queue.addAll(next);
		next = remote_queue;
		changestate(host, State.inCS);
	}

	/////////////////////////////////////////// METHODES
	/////////////////////////////////////////// UTILITAIRES////////////////////////////////////////////
	protected void changestate(Node host, State s) {
		/* calcul des durées pour l'état courant :
		long currentTime = CommonState.getTime();
		long duration = currentTime - lastTransitionTime;
		switch (state){
			case inCS:
					timeInU += duration;
					break;
			case requesting:
				timeInT += duration;
				break;
			case tranquil:
				timeInN += duration;
				break;
		}
		lastTransitionTime = CommonState.getTime();*/
		this.state = s;
		switch (this.state) {
		case inCS:
			executeCS(host);
			schedule_release(host);
			break;
		case tranquil:
			changeTokenState(TokenState.not_used);
			schedule_request(host);
			break;
		default:
		}
	}

	private void changeTokenState(TokenState newState) {
		long currentTime = CommonState.getTime();
		long elapsedTime = currentTime - lastTokenStateChange;

		// Ajouter le temps écoulé à l'état actuel
		switch (globalTokenState) {
			case used:
				timeInU += elapsedTime;
				break;
			case transit:
				timeInT += elapsedTime;
				break;
			case not_used:
				timeInN += elapsedTime;
				break;
		}

		// Mettre à jour l'état et le moment du dernier changement
		globalTokenState = newState;
		lastTokenStateChange = currentTime;
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
		return nb_cs;
	}

	public int getNbRequest() {
		return nb_request;
	}

	public long getRequest_time() {
		return request_time;
	}

	public long getTimeInT() {
		return timeInT;
	}

	public long getTimeInU() {
		return timeInU;
	}
	public long getTimeInN() {
		return timeInN;
	}

	/*public List<Integer> getNbMsgRequestPerCS(){
		return new ArrayList<Integer>(nb_messRequest_per_cs.values());
	}
	public List<Integer> getNbMsgTokenPerCS(){return new ArrayList<Integer>(nb_messToken_per_cs.values());}
	*/
	public List<Integer> getNbMsgPerCS(){
		return new ArrayList<Integer>(nb_messApp_per_cs.values());}
}
