/*
 * Free software. Use at your own risk. Okay to modify and re-distribute.
 * Project is at https://code.google.com/p/sfnrpc
 * 
 */
package sfn.core.rpc.server;

import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sfn.core.rpc.log.Slog;


public class ServerCommunicationsChannelsHolder {
	public ServerCommunicationsChannelsHolder(){}	
	private List<ServerCommunicationChannel> activeChannels = new ArrayList<ServerCommunicationChannel>();
	public List<ServerCommunicationChannel> getActiveChannels(){
		return this.activeChannels;
	}
	
	private Map<SocketChannel,ServerCommunicationChannel> socketChannelServerCommunicationsChannelMap = new HashMap<SocketChannel,ServerCommunicationChannel>();
	public void put(SocketChannel socketChannel,ServerCommunicationChannel serverCommunicationChannel){
		socketChannelServerCommunicationsChannelMap.put(socketChannel, serverCommunicationChannel);
	}
	public ServerCommunicationChannel get(SocketChannel socketChannel){
		return socketChannelServerCommunicationsChannelMap.get(socketChannel);
	}
	public ServerCommunicationChannel remove(ServerCommunicationChannel serverCommunicationChannel,Slog sl){
		ServerCommunicationChannel scc = socketChannelServerCommunicationsChannelMap.remove(serverCommunicationChannel.getSocketChannel());
		while(activeChannels.remove(scc)){
			if(sl!=null&&sl.isDebugEnabled())
				sl.debug("Should not really come here, if so see why? Removing scc from activeChannels");
		}
		return scc;
	}
	public Map<SocketChannel,ServerCommunicationChannel> getSocketChannelServerCommunicationsChannelMap(){
		return this.socketChannelServerCommunicationsChannelMap;
	}
}