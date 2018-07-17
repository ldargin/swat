package com.storytron.enginecommon;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.NoSuchElementException;

import javax.net.SocketFactory;

import net.jini.core.constraint.ConnectionAbsoluteTime;
import net.jini.core.constraint.InvocationConstraint;
import net.jini.core.constraint.InvocationConstraints;
import net.jini.jeri.Endpoint;
import net.jini.jeri.OutboundRequest;
import net.jini.jeri.OutboundRequestIterator;
import net.jini.jeri.http.HttpEndpoint;
import net.jini.jeri.tcp.TcpEndpoint;

/** 
 * Client side classes for connecting to the server.
 * <p>
 * These classes enable testing connection through JRMP, and if it fails
 * then attempt to connect through JRMP tunneled through HTTP. 
 * */
public final class JeriCustomSetup {

	/** 
	 * Milliseconds the clients should wait before giving up connecting to the port using JRMP.
	 * This affects linearly the time a client behind a proxy takes to connect to the 
	 * server.  
	 * */
	private static final int TEST_JRMP_TIMEOUT = 5000; 
	
	/** 
	 * Milliseconds a socket can be kept without sending any data from it.
	 * Watch out to keep this timeout smaller than the one used by the server.
	 * Otherwise, remote calls will fail in unexpected ways.   
	 * */
	private static final int CLIENT_TIMEOUT = 5000; 
	static {
		System.setProperty("com.sun.jini.jeri.connectionTimeout", String.valueOf(CLIENT_TIMEOUT));
		System.setProperty("com.sun.jini.jeri.http.idleConnectionTimeout", String.valueOf(CLIENT_TIMEOUT));
	}

	/** A custom endpoint providing connectivity through jrmp over tcp and jrmp over http. */
	public static final class CustomEndpoint implements Endpoint, Serializable {
		private static final long serialVersionUID = 0L;
		private TcpEndpoint te;
		private HttpEndpoint he;
		public CustomEndpoint(TcpEndpoint te, HttpEndpoint he) {
			this.te = te;
			this.he = he;
		}

		public OutboundRequestIterator newRequest(final	InvocationConstraints constraints) {
			OutboundRequestIterator orite=null;
			OutboundRequestIterator orihe=null;
			if (te!=null) {
				orite = te.newRequest(he==null?constraints: InvocationConstraints.combine(constraints,
						new InvocationConstraints(null,new InvocationConstraint[]{
								new ConnectionAbsoluteTime(System.currentTimeMillis()+TEST_JRMP_TIMEOUT)}))
				);
			} 
			if (he!=null)
				orihe = he.newRequest(constraints);
			final OutboundRequestIterator forite=orite;
			final OutboundRequestIterator forihe=orihe;

			// Return the sequential composition of the iterators.
			return new OutboundRequestIterator() {
				private int nextCount = 0;
				
				public boolean hasNext() {
					if (forite!=null && forihe!=null)
						return nextCount<2;
					else if (forite!=null || forihe!=null)
						return nextCount<1;
					else
						return false;
				}

				public OutboundRequest next() throws IOException {
					nextCount++;
					if (nextCount==1)
						if (forite!=null) {
							try {
								return forite.next();
							} catch (IOException e) {
								te = null; // don't try the tcp endpoint if it fails once.
								throw e;
							} catch (RuntimeException e) {
								te = null; // don't try the tcp endpoint if it fails once.
								throw e;
							}
						} else if (forihe!=null)
							return forihe.next();
						else
							throw new NoSuchElementException();
					else if (nextCount==2 && forihe!=null)
						return forihe.next();
					else 
						throw new NoSuchElementException();
				}
			};
		}
	}


	/** 
	 * A default implementation for a {@link SocketFactory}.
	 * <p>
	 * This class just instantiates {@link Socket}s.
	 * */
    public static final class CustomSocketFactory extends SocketFactory implements Serializable {
		private static final long serialVersionUID = 0L;

		public Socket createSocket(InetAddress address, int port,
				InetAddress localAddress, int localPort) throws IOException {
			return new Socket(address,port,localAddress,localPort);
		}

		public Socket createSocket(InetAddress host, int port)
				throws IOException {
			return new Socket(host,port);
		}

		public Socket createSocket(String host, int port,
				InetAddress localHost, int localPort) throws IOException,
				UnknownHostException {
			return new Socket(host,port,localHost,localPort);
		}

		public Socket createSocket(String host, int port) throws IOException,
				UnknownHostException {
			return new Socket(host,port);
		}
		
		public Socket createSocket() throws IOException {
			return new Socket();
		}
    }
    
}
