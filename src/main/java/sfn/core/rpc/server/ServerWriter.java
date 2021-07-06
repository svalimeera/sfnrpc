/*
 * Free software. Use at your own risk. Okay to modify and re-distribute.
 * Project is at https://code.google.com/p/sfnrpc
 * 
 */
package sfn.core.rpc.server;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

import sfn.core.rpc.dto.RPCDataResponse;
import sfn.core.rpc.dto.RPCDataWriteUtil;
import sfn.core.rpc.log.Slog;

public class ServerWriter extends Thread{
	private Slog sl = null;
	private List<ServerCommunicationChannel> finshedActing = new ArrayList<ServerCommunicationChannel>();
	private ServerCommunicationsChannelsHolder scch;
	private RPCDataWriteUtil rpcDataWriteUtil = null;
	public ServerWriter(ServerCommunicationsChannelsHolder scch, Slog sl){
		this.scch = scch;
		this.rpcDataWriteUtil = new RPCDataWriteUtil(sl);
		this.sl = sl;
	}
	 
	public synchronized void addActiveChannel(ServerCommunicationChannel scc){
		scch.getActiveChannels().add(scc);
		if(sl!=null&&sl.isDebugEnabled())
			sl.debug("Serverwriter added activechannel");
		this.notify();
	}
	public void run(){
		while(true){
			if(sl!=null&&sl.isDebugEnabled())
				sl.debug("Serverwriter thread active");
			try{
				if(scch.getActiveChannels().size()>0){
					sendActives();
				}
			}catch(Throwable t){
				if(sl!=null)
					sl.error(t,t.toString());
			}
			if(scch.getActiveChannels().size()==0){
				synchronized(this){
					if(scch.getActiveChannels().size()==0){
						try {
							if(sl!=null&&sl.isDebugEnabled())
								sl.debug("Serverwriter thread waiting for notify");
							this.wait();
						} catch (InterruptedException e) {
							if(sl!=null)
								sl.error(e,e.toString());
						}
					}
				}
			}
		}
	}
	private void sendActives(){
		for(int i=0;i<scch.getActiveChannels().size();i++){			
			ServerCommunicationChannel serverCommunicationChannel = scch.getActiveChannels().get(i);
			if(serverCommunicationChannel!=null){
				finshedActing.add(serverCommunicationChannel);
				respondChannel(serverCommunicationChannel);
			}
		}
		for(int i=0;i<finshedActing.size();i++){
			synchronized (this) {
				scch.getActiveChannels().remove(finshedActing.get(i));				
			}
		}
		finshedActing.clear();
	}
	private void respondChannel(ServerCommunicationChannel serverCommunicationChannel){
		if(sl!=null&&sl.isDebugEnabled())
			sl.debug("Responding... on:"+serverCommunicationChannel);
		if(serverCommunicationChannel!=null){
			SocketChannel socketChannel = serverCommunicationChannel.getSocketChannel();
			List<RPCDataResponse> list = serverCommunicationChannel.getDataToRespond();
			while(true){
				if(sl!=null&&sl.isDebugEnabled())
					sl.debug("hnio:rc:a");
				RPCDataResponse rpcDataResponse = null;
				try{
					rpcDataResponse = list.remove(0);
				}catch(Throwable ioobe){
					break;
				}
				try {
					if(rpcDataResponse!=null){
						this.rpcDataWriteUtil.writeRPCDataResponse(socketChannel,rpcDataResponse);						
					}else{
						break;
					}
				} catch (IOException e) {
					if(sl!=null)
						sl.error(e,e.toString());
				}
			}
		}
	}
}