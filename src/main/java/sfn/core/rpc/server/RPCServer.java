/*
 * Free software. Use at your own risk. Okay to modify and re-distribute.
 * Project is at https://code.google.com/p/sfnrpc
 * 
 */
package sfn.core.rpc.server;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import sfn.core.rpc.client.RPCClient;
import sfn.core.rpc.log.Slog;

public class RPCServer{
	private static Object lock = new Object();
	private Set<Integer> portSelectableChannel = new HashSet<Integer>();
	private HandlerCoreProcessor hcp = new HandlerCoreProcessor();
	private Slog sl;	
	/** Creates a new instance of RPCServer */
	public RPCServer(Slog sl){
		this.sl = sl;
	}
	public void startServer(String serverFqdn, Integer port,RPCClient rpcClient) throws IOException{
		if(!portSelectableChannel.contains(port)){
			portSelectableChannel.add(port);			
			if(serverFqdn==null){
				try{
					throw new RuntimeException("Invalid serverFqdn:"+serverFqdn);
				}catch(Exception e){
					if(sl!=null)
						sl.error(e,e.toString());
					e.printStackTrace();
					System.exit(1);
				}
			}
			ServerCommunicationsChannelsHolder scch = new ServerCommunicationsChannelsHolder();
			ServerCommunicationChannelsHealth scchealth = new ServerCommunicationChannelsHealth(scch, sl);
			scchealth.start();
			
			ServerWriter sw = new ServerWriter(scch,sl);
			sw.start();
			
			String address=serverFqdn+":"+port;
			if(rpcClient!=null){
				rpcClient.addLocalAddress(address);
				rpcClient.setHandlerCoreProcessor(hcp);
			}
			HandlerNio hnio = new HandlerNio(serverFqdn, port, scch, scchealth, sw, hcp, sl);
			hnio.start();
			if(sl!=null&&sl.isInfoEnabled())
				sl.info("Started server at:"+address);
		}else{
			throw new IOException("The port is already included as part of portSelectableChannel:"+portSelectableChannel+"<>"+port);
		}
	}
	public void publish(String URN,Object instance){
		if(sl!=null&&sl.isDebugEnabled())
			sl.debug("RPCServer:publish:Wait to publishing:"+instance.toString());
		synchronized(lock){
			if(sl!=null&&sl.isDebugEnabled())
				sl.debug("RPCServer:publish:Obtained Lock:"+instance.toString());
			if(instance==null){
				throw new RuntimeException("Cannot publish null instance");
			}else if(URN==null){
				throw new RuntimeException("Specify a valid URN");     
			}
			hcp.getUrn_obj().put(URN,instance);
		}
		if(sl!=null&&sl.isDebugEnabled())
			sl.debug("RPCServer:publish:Done publishing:"+instance.toString());
	}
}