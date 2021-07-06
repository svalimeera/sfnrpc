/*
 * Free software. Use at your own risk. Okay to modify and re-distribute.
 * Project is at https://code.google.com/p/sfnrpc
 * 
 */
package sfn.core.rpc.client;

import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sfn.core.rpc.log.Slog;

public class ClientSocketChannelsHolder {

	private List<ClientSocketChannel> activeChannels = new ArrayList<ClientSocketChannel>();
	public List<ClientSocketChannel> getActiveChannels(){
		return activeChannels;
	}

	private List<ClientSocketChannel> unconnected = new ArrayList<ClientSocketChannel>();
	public List<ClientSocketChannel> getUnconnected(){
		return unconnected;
	}

	private Map<SocketChannel,ClientSocketChannel> socketChannelClientSocketChannelMap = new HashMap<SocketChannel,ClientSocketChannel>();
	public Map<SocketChannel,ClientSocketChannel> getSocketChannelClientSocketChannelMap(){
		return this.socketChannelClientSocketChannelMap;
	}
	public synchronized ClientSocketChannel get(SocketChannel socketChannel){
		return socketChannelClientSocketChannelMap.get(socketChannel);
	}
	public synchronized void put(ClientSocketChannel clientSocketChannel){
		socketChannelClientSocketChannelMap.put(clientSocketChannel.getSocketChannel(),clientSocketChannel);
	}
	public synchronized ClientSocketChannel remove(ClientSocketChannel clientSocketChannel, Slog sl){
		ClientSocketChannel csc = socketChannelClientSocketChannelMap.remove(clientSocketChannel.getSocketChannel());
		while(activeChannels.remove(csc)){
			if(sl!=null&&sl.isDebugEnabled())
				sl.debug("Should not really come here, if so see why? Removed csc from active channels");
		}
		return csc;
	}
}