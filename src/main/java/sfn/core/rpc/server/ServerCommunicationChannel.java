/*
 * Free software. Use at your own risk. Okay to modify and re-distribute.
 * Project is at https://code.google.com/p/sfnrpc
 * 
 */
package sfn.core.rpc.server;

import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

import sfn.core.rpc.dto.RPCDataResponse;
import sfn.core.rpc.log.Slog;

public class ServerCommunicationChannel {
	private SocketChannel socketChannel;
	private ServerCommunicationsChannelsHolder scch;
	private List<RPCDataResponse> dataToRespond = new ArrayList<RPCDataResponse>();
	public ServerCommunicationChannel(SocketChannel socketChannel,ServerCommunicationsChannelsHolder scch){
		this.socketChannel = socketChannel;
		this.scch = scch;
	}
	public SocketChannel getSocketChannel(){
		return this.socketChannel;
	}
	public void addDataToRespond(RPCDataResponse rpcDataResponse){
		dataToRespond.add(rpcDataResponse);
	}
	public List<RPCDataResponse> getDataToRespond(){
		return this.dataToRespond;
	}
	public void close(Slog sl){
		dataToRespond.clear();
		scch.remove(this,sl);
		try{
			socketChannel.close();
		}catch(Exception e){}
	}
}