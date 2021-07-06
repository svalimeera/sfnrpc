/*
 * Free software. Use at your own risk. Okay to modify and re-distribute.
 * Project is at https://code.google.com/p/sfnrpc
 * 
 */
package sfn.core.rpc.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import sfn.core.rpc.dto.RPCData;
import sfn.core.rpc.dto.RPCDataResponse;
import sfn.core.rpc.log.Slog;

public class ClientSocketChannel {
	private String address;
	private SocketChannel socketChannel;
	private Map<Long,RPCData> dataToWrite = new ConcurrentHashMap<Long,RPCData>();
	private Map<Long,RPCDataResponse> responsesWaitingFor = new HashMap<Long,RPCDataResponse>();
	int closeRequestTime = -1;
	boolean valid = true;
	private ClientWriter cw = null;
	public boolean isValid(){
		return valid;
	}
	public ClientSocketChannel(ClientWriter cw){
		this.cw = cw;
	}
	public boolean isLoaded(){
		if(socketChannel.isConnected()&&socketChannel.isOpen()){
			if(responsesWaitingFor.size()>10||dataToWrite.size()>10){
				return true;
			}else{
				return false;
			}
		}else{
			valid = false;
			return false;
		}
	}
	public int [] loadSize(){
		return new int [] {responsesWaitingFor.size(), dataToWrite.size()};
	}
	public SocketChannel getSocketChannel(){
		return this.socketChannel;
	}
	public void setSocketChannel(SocketChannel socketChannel){
		this.socketChannel = socketChannel;
	}
	public void setAddress(String address){
		this.address = address;
	}
	public String getAddress(){
		return this.address;
	}
	public void addDataToWrite(RPCData rpcData, Slog sl) throws ClosedChannelException{
		dataToWrite.put(rpcData.serialNumber,rpcData);
		cw.addActiveChannel(this);
	}
	public Map<Long,RPCData> getDataToWrite(){
		return this.dataToWrite;
	}
	public void connect(Slog sl){
		String [] serverIPAddressPort = address.split("[:]");
		String serverIPAddress = serverIPAddressPort[0];
		Integer port = Integer.parseInt(serverIPAddressPort[1]);
		SocketAddress socketAddress = new InetSocketAddress(serverIPAddress, port);
		try {
			socketChannel.connect(socketAddress);
		} catch (IOException e) {
			if(sl!=null)
				sl.error(e,e.toString());
		}
	}
	public void assignResponse(RPCDataResponse responseFromServer, Slog sl){
		try{
			Long serialNumber = responseFromServer.serialNumber;
			RPCDataResponse waitingResponse = responsesWaitingFor.get(serialNumber);
			if(waitingResponse!=null){
				waitingResponse.serialNumber = responseFromServer.serialNumber;
				waitingResponse.object = responseFromServer.object;
				synchronized(waitingResponse){
					waitingResponse.notify();
				}
			}
		}catch(Throwable t){
			if(sl!=null)
				sl.error(t,t.toString());
		}
	}
	public void assignWaitForResponseFromServer(RPCData rpcData, Slog sl){
		RPCDataResponse rpcDataResponse = new RPCDataResponse();		
		responsesWaitingFor.put(rpcData.serialNumber,rpcDataResponse);
	}
	public boolean waitTillDataWritten(RPCData rpcData, Slog sl){
		synchronized(rpcData){
			while(dataToWrite.containsKey(rpcData.serialNumber)){
				try {
					rpcData.wait(100000);
					if(dataToWrite.containsKey(rpcData.serialNumber)){
						RPCData removed = dataToWrite.remove(rpcData.serialNumber);
						if(removed!=null){
							if(sl!=null&&sl.isDebugEnabled())
								sl.debug("waitTillDataWritten failed");
							return false;
						}else{
							return true;
						}
					}else{
						return true;
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			return true;
		}
	}
	public Object getResponseFromServer(RPCData rpcData, int timeoutInSecs, Slog sl){
		RPCDataResponse waitingResponse = responsesWaitingFor.get(rpcData.serialNumber);
		if(waitingResponse==null){
			return null;
		}
		synchronized(waitingResponse){
			if(waitingResponse.serialNumber!=-1) return waitingResponse.object;
			try{waitingResponse.wait(timeoutInSecs*1000);} catch (InterruptedException e) {}
		}
		responsesWaitingFor.remove(rpcData.serialNumber);
		if(waitingResponse.serialNumber==-1L){
			RuntimeException re = new RuntimeException("RPC Call timed out for serial number:"+rpcData.serialNumber);
			if(sl!=null)
				sl.error(re,re.toString());
			throw re;
		}else{			
			return waitingResponse.object;
		}
	}
	public boolean closeIfPossible(Slog sl){
		valid = false;
		if(closeRequestTime==-1){
			closeRequestTime=(int)(System.currentTimeMillis()/1000);
		}
		if(
				isLoaded()&&
				(((int)(System.currentTimeMillis()/1000)-closeRequestTime)>5*60)
		){
			return false;
		}else{
			closeCsc(sl);
			return true;
		}
	}
	public void close(Slog sl){
		closeCsc(sl);
	}
	private void closeCsc(Slog sl){
		valid = false;
		try {
			if(sl!=null)
				if(sl!=null&&sl.isDebugEnabled())
					sl.debug("Closing channel: "+address);
			int attempts = 0;
			while(responsesWaitingFor.size()>0||dataToWrite.size()>0){
				if(attempts++>1000){
					if(sl!=null&&sl.isDebugEnabled())
						sl.debug("Forcibly clearing responsesWaitinfFor since close could not be completed in time");
					responsesWaitingFor.clear();
					dataToWrite.clear();
				}
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			socketChannel.close();
			if(sl!=null)
				if(sl!=null&&sl.isDebugEnabled())
					sl.debug("CLOSED channel: "+address+"<>"+socketChannel.hashCode()+"<>"+socketChannel.socket().hashCode());
		} catch (IOException e) {
			//nothing to do.
		}
	}
}