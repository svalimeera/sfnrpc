/*
 * Free software. Use at your own risk. Okay to modify and re-distribute.
 * Project is at https://code.google.com/p/sfnrpc
 * 
 */
package sfn.core.rpc.client;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import sfn.core.rpc.dto.RPCData;
import sfn.core.rpc.log.Slog;
import sfn.core.rpc.server.HandlerCoreProcessor;

public class RPCClient extends ClientSelectorManager{
	/** Creates a new instance of RPCClient 
	 * @throws IOException */
	private Slog sl=null;
	private Set<String> localAddresses = new HashSet<String>();
	private HandlerCoreProcessor hcp = null;
	public RPCClient(Slog sl){
		super(sl);
		this.sl = sl;
	}
	public void addLocalAddress(String address){
		this.localAddresses.add(address);
	}
	public void setHandlerCoreProcessor(HandlerCoreProcessor hcp){
		this.hcp = hcp;
	}
	public Object invoke(
			String serverURN, 
			String address,
			String methodName,
			boolean waitForResponse,
			int timeoutInSecs,
			String trace,
			Object [] parameters,
			Class [] classArr
	){
		if(address==null){
			RuntimeException rpce = new RuntimeException("Specify a valid address: "+address);
			throw rpce;            
		}
		ClientSocketChannel csc = null;
		Object object = null;
		RPCData rpcData = new RPCData(serverURN,methodName,waitForResponse,trace,parameters,classArr);
		if(!localAddresses.contains(address)){
			try{
				csc = (ClientSocketChannel)cscp.borrowObject(address);				
				if(serverURN==null){
					RuntimeException rpce = new RuntimeException("Specify a valid URN");
					throw rpce;            
				}
				if(methodName==null){
					RuntimeException rpce = new RuntimeException("Specify a non null methodName");
					throw rpce;            
				}	
				if(csc!=null)
					csc.addDataToWrite(rpcData,sl);
				if(rpcData.waitForResponse){//client will wait for server to respond
					csc.assignWaitForResponseFromServer(rpcData,sl);
				}
			}catch(Exception e){
				if(sl!=null)
					sl.error(e,e.toString());
				RPCInvokeException rie = new RPCInvokeException(e);
				rie.setMessage(address);
				throw rie;
			}finally{
				if(csc!=null){
					cscppr.returnToPool(csc);
				}
				this.wakeup();
				if(csc!=null){
					if(rpcData.waitForResponse){//client will wait for server to respond
						object = csc.getResponseFromServer(rpcData,timeoutInSecs,sl);
					}else{
						boolean written = csc.waitTillDataWritten(rpcData, sl);
						if(!written){
							RuntimeException re = new RuntimeException("wait till data written failed: "+rpcData.serialNumber);
							if(sl!=null)
								sl.error(re,re.toString());
							throw re;
						}
					}
				}
			}
		}else{
			if(hcp!=null){
				if(sl!=null&&sl.isDebugEnabled()){
					sl.debug("in process communication - bypassed rpc: "+rpcData+"<>"+address);
				}
				object = hcp.process(rpcData);
			}else{
				throw new RuntimeException("Detected as local address but server's HandlerCoreProcessor not set: "+hcp);
			}
		}
		if(object!=null && object instanceof Throwable){
			Throwable rpce = (Throwable)object;
			throw new RuntimeException(rpce);
		}else{
			return object;
		}
	}

}