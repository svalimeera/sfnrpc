/*
 * Free software. Use at your own risk. Okay to modify and re-distribute.
 * Project is at https://code.google.com/p/sfnrpc
 * 
 */
package sfn.core.rpc.server;

import java.nio.channels.SocketChannel;
import java.util.Map;

import sfn.core.rpc.log.Slog;

public class ServerCommunicationChannelsHealth extends Thread{
	private Slog sl;
	private ServerCommunicationsChannelsHolder scch;
	public ServerCommunicationChannelsHealth(ServerCommunicationsChannelsHolder scch,Slog sl){
		this.sl = sl;
		this.scch = scch;
	}
	public void run(){
		while(true){
			try{
				Map<SocketChannel,ServerCommunicationChannel> sccMap = scch.getSocketChannelServerCommunicationsChannelMap();
				Object [] it = sccMap.keySet().toArray();
				for(int i=0;i<it.length;i++){
					try{
						SocketChannel socketChannel = (SocketChannel)it[i];
						if(!socketChannel.isConnected()&&!socketChannel.isConnectionPending()){
							ServerCommunicationChannel scc = scch.get(socketChannel);
							scc.close(sl);
							if(sl!=null&&sl.isDebugEnabled())
								sl.debug("Will remove this socketchannel:"+scc.hashCode());
							sccMap.remove(socketChannel);
						}
					}catch(Exception e){
						if(sl!=null)
							sl.error(e,e.toString());
					}
				}
			}catch(Throwable t){
				if(sl!=null)
					sl.error(t,t.toString());
			}
			try{				
				Thread.sleep(60000);
			}catch(Exception e){
				if(sl!=null)
					sl.error(e,e.toString());
			}			
		}
	}
}