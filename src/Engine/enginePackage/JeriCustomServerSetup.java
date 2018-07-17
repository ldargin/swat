package Engine.enginePackage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.Remote;
import java.rmi.server.ExportException;
import java.security.Permission;
import java.util.Collection;
import java.util.Collections;
import java.util.Hashtable;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;

import net.jini.core.constraint.InvocationConstraints;
import net.jini.io.MarshalInputStream;
import net.jini.io.UnsupportedConstraintException;
import net.jini.jeri.BasicILFactory;
import net.jini.jeri.BasicInvocationDispatcher;
import net.jini.jeri.BasicJeriExporter;
import net.jini.jeri.Endpoint;
import net.jini.jeri.InboundRequest;
import net.jini.jeri.InvocationDispatcher;
import net.jini.jeri.ServerCapabilities;
import net.jini.jeri.ServerEndpoint;
import net.jini.jeri.http.HttpEndpoint;
import net.jini.jeri.http.HttpServerEndpoint;
import net.jini.jeri.tcp.TcpEndpoint;
import net.jini.jeri.tcp.TcpServerEndpoint;

import com.storytron.enginecommon.JeriCustomSetup;
import com.storytron.enginecommon.SharedConstants;
import com.storytron.enginecommon.StorytellerRemote;
import com.sun.jini.jeri.internal.runtime.Util;
import com.sun.jini.thread.Executor;
import com.sun.jini.thread.GetThreadPoolAction;

/** 
 * A class for exporting the server using Jeri.
 * <p>
 * Use the {@link #export(int,StorytellerRemote)} or {@link #export(int,int,StorytellerRemote)}
 * method to do the export.
 * <p>
 * Tweak the {@link #getSerializedLengthLimit(Method, int, Class)} method
 * to adjust the limits accepted for different types in the parameters of
 * remote calls. 
 * <p>
 * The setup implements some restrictions on the use of the remote interface.
 * It limits the amount of data per method parameter, limits the amount of socket
 * connections that can be opened from a given IP, limits the amount of time that
 * a socket remains opened without receiving any data (set through {@link Socket#setSoTimeout(int)}),
 * and limits the amount of threads to be used in the server to dispatch rmi requests.
 * <p>
 * These measures are to prevent clients from taking all of the server memory,
 * or all of the server sockets.
 * <p>
 * As the above implies replacing the default RMI factory we can not benefit from
 * the super socket power implemented there to connect to clients behind proxies using 
 * http tunneling. Therefore, here we also setup things so clients can connect either
 * directly through JRMP or through JRMP using http tunneling.   
 * */
public final class JeriCustomServerSetup {
	
	/** Milliseconds a socket can be reserved for a client without receiving any data from it. */
	private static final int SERVER_SO_TIMEOUT_VALUE = 15000;
	/** Maximum amount of connections allowed to this server from a single IP address. */
	private static final int MAX_CONNECTIONS_PER_IP = 30;
	/** Maximum amount of connections allowed to clients for this server. */
	private static final int MAX_CONNECTION_TASK_QUEUE_SIZE = 2000;
	/** Maximum amount of connection dispatching threads. */
	private static final int MAX_DISPATCHING_THREADS = 30;
	static {
		/** Thread pool to use for dispatching connections.  */
		GetThreadPoolAction.systemThreadPool = GetThreadPoolAction.userThreadPool = new Executor(){
			private ExecutorService ex = 
				new ThreadPoolExecutor(MAX_DISPATCHING_THREADS,MAX_DISPATCHING_THREADS,10*60*1000,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<Runnable>(MAX_CONNECTION_TASK_QUEUE_SIZE,true)); 
			public void execute(Runnable runnable, String name) {
				ex.execute(runnable);
			}
		};
	}
	
	static {
		System.setProperty("com.sun.jini.jeri.http.idleServerConnectionTimeout", String.valueOf(SERVER_SO_TIMEOUT_VALUE));
	}

	/** 
	 * Returns the maximum length in bytes allowed for a given parameter in a remote invocation.
	 * @param method is the method the parameter belongs to.
	 * @param argIndex if the parameter index.
	 * @param type is the parameter type.  
	 * */
	@SuppressWarnings("unchecked")
	private static int getSerializedLengthLimit(Method method,int argIndex,Class type){
		if (type == String.class)
			return 500;
		else if (type == byte[].class)
			// can be a compressed Deikto, or a saved state    
			return 500000; // 500k
		else if (type == int[].class)
			// can be a trace
			return 50000; // 50k
		else
			return 500; // 0.5k
	}

	/** 
	 * Exports the server using JERI to listen on the given port for jrmp over http.
	 * @param httpport the port to listen for remote invocations.
	 * @param r the remote object to export.  
	 * */
	public static StorytellerRemote export(StorytellerRemote r,int httpport) throws ExportException {
		return export(r, HttpServerEndpoint.getInstance(
														SharedConstants.getHostName(),
														httpport,
														new JeriCustomSetup.CustomSocketFactory(),
														new CustomServerSocketFactory(new ServerSocketSynchronizer(),false)
													   )
					);
	}
	
	/** 
	 * Exports the server using JERI to listen on a given port for jrmp over tcp, and
	 * for jrmp over http on another port.
	 * @param port the port to listen for remote invocations using jrmp over tcp.
	 * @param httpport the port to listen for remote invocations using jrmp over http.
	 * @param r the remote object to export.  
	 * */
	public static StorytellerRemote export(StorytellerRemote r,int port,int httpport) throws ExportException {
		return export(r,new CustomServerEnpoint(SharedConstants.getHostName(),port,httpport));
	}
	
	/** Exports the server using the given server end-point. */
	private static StorytellerRemote export(StorytellerRemote r,ServerEndpoint se) throws ExportException {
		return (StorytellerRemote)new BasicJeriExporter(se,new CustomILFactory(),false,true).export(r);
	}
	
	/** A custom server end-point that supports both jrmp over tcp and jrmp over http. */
	private static final class CustomServerEnpoint implements ServerEndpoint {

		TcpServerEndpoint tse;
		HttpServerEndpoint hse;
		
		public CustomServerEnpoint(String host,int port,int httpport) {
			SocketFactory csf = new JeriCustomSetup.CustomSocketFactory();
			ServerSocketSynchronizer sss = new ServerSocketSynchronizer();
			tse = TcpServerEndpoint.getInstance(host,port,csf,new CustomServerSocketFactory(sss,true));
			hse = HttpServerEndpoint.getInstance(host,httpport,csf,new CustomServerSocketFactory(sss,false));
		}
		
		public Endpoint enumerateListenEndpoints(ListenContext listenContext)
				throws IOException {
			TcpEndpoint te = (TcpEndpoint)tse.enumerateListenEndpoints(listenContext);
			HttpEndpoint he = (HttpEndpoint)hse.enumerateListenEndpoints(listenContext);
			return new JeriCustomSetup.CustomEndpoint(te,he);
		}

		public InvocationConstraints checkConstraints(
				InvocationConstraints constraints)
				throws UnsupportedConstraintException {
			return InvocationConstraints.combine(tse.checkConstraints(constraints),hse.checkConstraints(constraints));
		}
	}

	/** A container for synchronization structures for server sockets. */
	private static final class ServerSocketSynchronizer {
		/** List of opened connections indexed by InetAddress. */
		public Hashtable<InetAddress,Integer> openedConnections = new Hashtable<InetAddress,Integer>();
	}

	/** 
	 * A custom server socket factory just to produce our custom server sockets.
	 * <p>
	 * We need to implement custom server sockets to setup the timeout when reading
	 * from the accepted connections in the accept() method.
	 * <p>
	 * The server sockets also limit the amount of sockets they are allowed to open per 
	 * ip address.
	 * */
    private static final class CustomServerSocketFactory extends ServerSocketFactory {
    	public ServerSocketSynchronizer sss;
    	public boolean limitSockets;
    	/** 
    	 * @param sss the structures to synchronize different server sockets.
    	 * @param limitSockets tells if a SO_TIMEOUT should be set to connections established
    	 *                     through server sockets produced by this factory. 
    	 * */
    	public CustomServerSocketFactory(ServerSocketSynchronizer sss, boolean limitSockets){
    		this.sss = sss;
    		this.limitSockets = limitSockets;
    	}
		public ServerSocket createServerSocket(int port, int backlog, InetAddress ifAddress) throws IOException {
			return new CustomServerSocket(port,backlog,ifAddress);
		}

		public ServerSocket createServerSocket(int port, int backlog)	throws IOException {
			return new CustomServerSocket(port,backlog);
		}

		public ServerSocket createServerSocket(int port) throws IOException {
			return new CustomServerSocket(port);
		}

		public ServerSocket createServerSocket() throws IOException {
			return new CustomServerSocket();
		}
		
	    private final class CustomServerSocket extends ServerSocket {

	    	public CustomServerSocket() throws IOException { super();  	}
	    	public CustomServerSocket(int port) throws IOException { super(port);  	}
			public CustomServerSocket(int port, int backlog,
					InetAddress ifAddress) throws IOException {
				super(port,backlog,ifAddress);
			}
			public CustomServerSocket(int port, int backlog) throws IOException {	super(port,backlog);	}
			
			@Override
			public Socket accept() throws IOException {
				while(true) {
					CustomSocket s = new CustomSocket();
					if (limitSockets)
						s.setSoTimeout(SERVER_SO_TIMEOUT_VALUE);
					implAccept(s); 
					synchronized(sss) {
						Integer i = sss.openedConnections.get(s.getInetAddress());
						if (i!=null && i>=MAX_CONNECTIONS_PER_IP) {
							s.superclose();
						} else {
							//System.out.println("Accepted connection to "+s.getInetAddress()+":"+s.getPort()+" on: "+s.getLocalAddress()+":"+s.getLocalPort());
							//System.out.flush();
							sss.openedConnections.put(s.getInetAddress(),i==null?Integer.valueOf(1):Integer.valueOf(i.intValue()+1));
							return s;
						}
					}
				}
			}

			/** A custom socket that unregisters itself from opened connections when closing. */
			private class CustomSocket extends Socket {
				private boolean closed = false;
				public void superclose() throws IOException { 
					if (closed)
						return;
					
					closed = true;
					super.close(); 
				}
				@Override
				public synchronized void close() throws IOException {
					if (closed)
						return;
					
					closed = true;
					//System.out.println("closing socket to "+getInetAddress()+":"+getPort());
					synchronized (sss) {
						int i = sss.openedConnections.get(getInetAddress());
						i--;
						if (i>0)
							sss.openedConnections.put(getInetAddress(),Integer.valueOf(i));
						else
							sss.openedConnections.remove(getInetAddress());
						super.close();
					}
				}
			}
	    }

    }

    /** 
     * A custom invocation layer factory to control the length of the parameters read during
     * a remote invocation.
     * */
    private static final class CustomILFactory extends BasicILFactory {
		private static final long serialVersionUID = 0L;
    	/** Cached getClassLoader permission */
        private static final Permission getClassLoaderPermission =
        				new RuntimePermission("getClassLoader");
        
    	@SuppressWarnings("unchecked")
		@Override
    	protected InvocationDispatcher createInvocationDispatcher(Collection methods, Remote impl,ServerCapabilities caps) 
    					throws ExportException {
    		return new BasicInvocationDispatcher(methods, caps,getServerConstraints(),getPermissionClass(),getClassLoader()) {
    			
				@Override
    			protected Object[] unmarshalArguments(Remote impl, Method method,ObjectInputStream in, Collection context) 
    						throws IOException, ClassNotFoundException {
    				// This code is executed by the SERVER for every remote call.
    				if (impl == null || in == null || context == null)
    				    throw new NullPointerException();
    				
    				LimitedObjectInputStream lois = (LimitedObjectInputStream)in;
    				
    				Class[] types = method.getParameterTypes();
    				Object[] args = new Object[types.length];
    				for (int i = 0; i < types.length; i++) {
    					lois.resetByteCount(getSerializedLengthLimit(method,i,types[i]));
   						args[i] = Util.unmarshalValue(types[i], in);
    				}
    				return args;
    			}
    			
				@Override
    			protected ObjectInputStream createMarshalInputStream(
    					Object impl, InboundRequest request, boolean integrity,
    					Collection context) throws IOException {
    				ClassLoader streamLoader;
    				if (getClassLoader() != null) {
    				    streamLoader = getClassLoader();
    				} else {
    				    SecurityManager security = System.getSecurityManager();
    				    if (security != null) {
    				    	security.checkPermission(getClassLoaderPermission);
    				    }
    				    streamLoader = impl.getClass().getClassLoader();
    				}
    				
    				Collection unmodContext = Collections.unmodifiableCollection(context);
    				MarshalInputStream in =
    				    new LimitedObjectInputStream(
    				    		new LimitedInputStream(request.getRequestInputStream()),
    							streamLoader, integrity,
    							streamLoader, unmodContext);
    				in.useCodebaseAnnotations();
    				return in;
    			}
    		};
    		
    	}
    	
    	/** A {@link MarshalInputStream} that wraps a {@link LimitedInputStream}. */
        private static final class LimitedObjectInputStream extends MarshalInputStream {
        	private LimitedInputStream lis;
        	@SuppressWarnings("unchecked")
    		public LimitedObjectInputStream(LimitedInputStream lis, ClassLoader defaultLoader, 
        			boolean verifyCodebaseIntegrity, ClassLoader verifierLoader, Collection context) throws IOException {
        		super(lis,defaultLoader,verifyCodebaseIntegrity,verifierLoader,context);
        		this.lis = lis;
        	}
        	/** Resets the byte count of the underlying {@link LimitedInputStream}. */
    		public void resetByteCount(int limit){ lis.resetByteCount(limit); }
        }
    }

}
