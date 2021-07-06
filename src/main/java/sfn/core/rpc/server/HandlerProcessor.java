/*
 * Free software. Use at your own risk. Okay to modify and re-distribute.
 * Project is at https://code.google.com/p/sfnrpc
 * 
 */
package sfn.core.rpc.server;

import java.io.IOException;
import java.nio.channels.SocketChannel;

import sfn.core.rpc.dto.RPCData;
import sfn.core.rpc.dto.RPCDataResponse;
import sfn.core.rpc.log.Slog;

public class HandlerProcessor implements Runnable{
	private Slog sl;
	private RPCData rpcData;
	private SocketChannel socketChannel;
	private ServerWriter sw;
	private ServerCommunicationsChannelsHolder scch;
	private HandlerCoreProcessor hcp;
	public HandlerProcessor(
			SocketChannel socketChannel, 
			RPCData rpcData, 
			ServerWriter sw, 
			ServerCommunicationsChannelsHolder scch, 
			HandlerCoreProcessor hcp,
			Slog sl) throws IOException{
		this.rpcData = rpcData;
		this.sl = sl;
		this.socketChannel = socketChannel;
		this.sw = sw;
		this.scch = scch;
		this.hcp = hcp;
	}
	@Override
	public void run() {
		try{
			if(sl!=null&&sl.isDebugEnabled())
				sl.debug("Handler processor called:"+rpcData);
			Object result = hcp.process(rpcData);
			if(sl!=null&&sl.isDebugEnabled())
				sl.debug("Handler processor response got is:"+result);
			if(rpcData.waitForResponse){//client is expecting a response
				if(sl!=null&&sl.isDebugEnabled())
					sl.debug("Handler processor will send response to client");
				response(result);
			}else{
				if(sl!=null&&sl.isDebugEnabled())
					sl.debug("Handler processor response call was asynch so no response sent");
			}
		}catch(Throwable t){
			if(sl!=null)
				sl.error(t, t.toString());
		}
	}
	public void response(Object object){
		RPCDataResponse rpcDataResponse = new RPCDataResponse();
		rpcDataResponse.serialNumber=rpcData.serialNumber;
		rpcDataResponse.object = object;
		ServerCommunicationChannel serverCommunicationChannel= scch.get(socketChannel);
		if(sl!=null&&sl.isDebugEnabled())
			sl.debug("Handler processor got serverCommunicationChannel:"+serverCommunicationChannel);
		serverCommunicationChannel.addDataToRespond(rpcDataResponse);
		if(sl!=null&&sl.isDebugEnabled())
			sl.debug("Handler processor - handed serverCommunicationChannel to serverwriter:"+serverCommunicationChannel+"<>"+sw);
		sw.addActiveChannel(serverCommunicationChannel);
	}
}