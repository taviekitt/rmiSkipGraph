public class SimpleSkipNode implements SkipNode {
	private int key;		private int key;
	private String next; //look at naming again		private int nextKey; //look at naming again
	private String prev;		private int prevKey;
	private SkipNode nextStub;		private SkipNode nextStub;
	private SkipNode prevStub;		private SkipNode prevStub;


	public SimpleSkipNode(int simpleKey) {		public SimpleSkipNode(int simpleKey) {
		//super(); //instantiates like parent -- is this mandatory?			//super(); //instantiates like parent -- is this mandatory?
		key = simpleKey;			key = simpleKey;
		next = null;			nextKey = 0; //0 is null in this context
		prev = null;			prevKey = 0;
		nextStub = null;			nextStub = null;
		prevStub = null;			prevStub = null;
		//register new node			//register new node
		try {			try {
			Registry registry = utils.RegistryHelper.registry;				Registry registry = utils.RegistryHelper.getRegistry();
			SkipNode selfStub = (SkipNode) UnicastRemoteObject.exportObject(this, 0);				SkipNode selfStub = (SkipNode) UnicastRemoteObject.exportObject(this, 0);
			registry.bind("Node-" + key, selfStub); //bind means that if no exists, will give error, not register, dead object				registry.bind("Node-" + key, selfStub); //bind means that if no exists, will give error, not register, dead object
		}			}
@@ -36,66 +36,54 @@ public SimpleSkipNode(int simpleKey) {
		}			}
	}		}


	public int getKey() {		// killall rmiregistry
	public synchronized int getKey() {
		return this.key;			return this.key;
	}		}


	public SkipNode getNext() {		public SkipNode getNextStub() {
		try {			nextStub = RegistryHelper.getNode(nextKey);
			if(nextStub == null) {			return nextStub;
				nextStub = (SkipNode) RegistryHelper.registry.lookup(next);	
			}	
			return nextStub;	
		}	
		catch (Exception exception) {	
			return null;	
		}	
	}		}


	public SkipNode getPrev() {		public SkipNode getPrevStub() {
		try {			prevStub = RegistryHelper.getNode(prevKey);
			if(prevStub == null) {			return prevStub;
				prevStub = (SkipNode) RegistryHelper.registry.lookup(prev);	
			}	
			return prevStub;	
		}	
		catch (Exception exception) {	
			return null;	
		}	
	}		}


	public void setNext(String next){		public void setNext(int nextKey){
		this.next = next;			this.nextKey = nextKey;
		nextStub = null;			nextStub = null;
	}		}


	public void setPrev(String prev){		public void setPrev(int prevKey){
		this.prev = prev;			this.prevKey = prevKey;
		prevStub = null;			prevStub = null;
	}		}


	public void setup() {		public void setup() {
		registry = utils.RegistryHelper.registry; //why doesn't registiry work???? See problems with gettings stubs below too			try {
		String[] boundNodes = registry.list();			SkipNode randomNode = RegistryHelper.getRandomNode();
		Random rand = new Random();			int closestKey = randomNode.find(this.key, randomNode.getKey());
		int randIndexInBound = rand.nextInt(boundNodes.length);			SkipNode closestStub = RegistryHelper.getNode(closestKey); //how to actually get stub?
		SkipNode randomNode = (SkipNode) registry.lookup(boundNodes[randIndexInBound]);	
		int closestKey = randomNode.find(this.key, randomNode.key);			if(closestStub.getKey() < this.key) { //if stub before this
		SkipNode closestStub = register.lookup("Node-" + closestKey); //how to actually get stub?				this.setPrev(closestStub.getKey()); //this prev is closestStub

			SkipNode oldNext = closestStub.getNextStub();
		if(closestStub.key < this.key) { //if stub before this				closestStub.setNext(this.key);//closestStub next is this
			this.setPrev("Node-" + closestStub.key); //this prev is closestStub				oldNext.setPrev(this.key); //oldNext prev is this
			SkipNode oldNext = closestStub.getNext();				this.setNext(oldNext.getKey());	//this next is OldNext
			closestStub.setNext("Node-" + this.key);//closestStub next is this	
			oldNext.setPrev("Node-" + this.key); //oldNext prev is this	
			this.setNext("Node" + oldNext.key);	//this next is OldNext	
		}			}
		else {//if stub after this			else {//if stub after this
			this.setNext("Node-" + closestStub.key);//this next is closestStub				this.setNext(closestStub.getKey());//this next is closestStub
			SkipNode oldPrev = closestStub.getPrev();				SkipNode oldPrev = closestStub.getPrevStub();
			closestStub.setPrev("Node-" + this.key);//closestStub prev is this				closestStub.setPrev(this.key);//closestStub prev is this
			oldPrev.setNext("Node-"+this.key);//oldPrev next is this				oldPrev.setNext(this.key);//oldPrev next is this
			this.setPrev("Node" + oldPrev.key);//this prev is oldPrev				this.setPrev(oldPrev.getKey());//this prev is oldPrev
		}
		} catch(Exception exception) {
			System.out.println("Setup failed due to: " + exception);
			System.exit(2);
		}			}
	}		}


@@ -119,7 +107,7 @@ public int find(int searchKey, int lastKey) { //lets assume not circular list fo
			}				}
			else { //if searchKey is less than this.key and lastKey				else { //if searchKey is less than this.key and lastKey
				try {					try {
					this.getPrev().find(searchKey, this.key);						this.getPrevStub().find(searchKey, this.key);
				} catch(Exception e) {					} catch(Exception e) {
					System.out.println("Fails to due to exception: " + e);						System.out.println("Fails to due to exception: " + e);
					return -1;						return -1;
@@ -134,7 +122,7 @@ public int find(int searchKey, int lastKey) { //lets assume not circular list fo


			else {// if searchKey greater than this and last				else {// if searchKey greater than this and last
				try {					try {
					this.getNext().find(searchKey, this.key);						this.getNextStub().find(searchKey, this.key);
				} catch(Exception e) {					} catch(Exception e) {
					System.out.println("Fails to due to exception: " + e);						System.out.println("Fails to due to exception: " + e);
					return -1;						return -1;
 BIN +385 Bytes server/SkipNode.class 
Binary file not shown.
  8  server/SkipNode.java 
@@ -4,10 +4,10 @@
import java.rmi.RemoteException;	import java.rmi.RemoteException;


public interface SkipNode extends Remote {	public interface SkipNode extends Remote {
    SkipNode getNext() throws RemoteException;	    SkipNode getNextStub() throws RemoteException;
    SkipNode getPrev() throws RemoteException;	    SkipNode getPrevStub() throws RemoteException;
    void setNext(String nextName) throws RemoteException;	    void setNext(int nextInt) throws RemoteException;
    void setPrev(String prevName) throws RemoteException;	    void setPrev(int prevInt) throws RemoteException;
    int find(int searchKey, int lastKey) throws RemoteException;	    int find(int searchKey, int lastKey) throws RemoteException;
    int getKey() throws RemoteException;	    int getKey() throws RemoteException;
} 	} 
 BIN +1.72 KB utils/RegistryHelper.class 
Binary file not shown.
  42  utils/RegistryHelper.java 
@@ -2,14 +2,52 @@


import java.rmi.registry.LocateRegistry;	import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;	import java.rmi.registry.Registry;
import server.SkipNode;
import java.util.Random;

// RegistryHelper.getRegistry();


public class RegistryHelper {	public class RegistryHelper {
	static private Registry globalRegistry;

	public static Registry getRegistry() {
		try {  //what is the syntax error here????			try {  //what is the syntax error here????
			static public Registry registry  = LocateRegistry.getRegistry("127.0.0.1");				if(globalRegistry == null) {
				globalRegistry  = LocateRegistry.getRegistry("127.0.0.1");
			}
			return globalRegistry;
		}			}
		catch(Exception exception){			catch(Exception exception){
			System.out.println("Failed to locate static registry");				System.out.println("Failed to locate the registry");
			System.exit(1);
			return null; //unreachable code
		}
	}

	public static SkipNode getNode(int key) {
		try {  //what is the syntax error here????
			return (SkipNode) RegistryHelper.getRegistry().lookup("Node-" + key);
		}
		catch(Exception exception){
			System.out.println("Failed to locate the registry");
			System.exit(1);
			return null; //unreachable code
		}
	}

	public static SkipNode getRandomNode() {
		try {
			String[] boundNodes = RegistryHelper.getRegistry().list();
			Random rand = new Random();
			int randIndexInBound = rand.nextInt(boundNodes.length);
			SkipNode randomNode = (SkipNode) RegistryHelper.getRegistry().lookup(boundNodes[randIndexInBound]);
			return randomNode;
		} catch(Exception exception) {
			System.out.println("Failed to locate the registry");
			System.exit(1);
			return null; //unreachable code
		}			}
	}
}	}
//in main get this once, set up global registry -- always look to this helper	//in main get this once, set up global registry -- always look to this helper
//like below 	//like below 
