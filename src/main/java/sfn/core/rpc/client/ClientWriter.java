/*
 * Free software. Use at your own risk. Okay to modify and re-distribute.
 * Project is at https://code.google.com/p/sfnrpc
 * 
 */
package sfn.core.rpc.client;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import sfn.core.rpc.dto.RPCData;
import sfn.core.rpc.dto.RPCDataWriteUtil;
import sfn.core.rpc.log.Slog;

public class ClientWriter extends Thread{
	private List<ClientSocketChannel> finshedActing = new ArrayList<ClientSocketChannel>();
	private ClientSocketChannelsHolder csch;
	private List<ClientSocketChannel> activeChannels = null;
	private Slog sl;
	private RPCDataWriteUtil rpcDataWriteUtil = null;
	public ClientWriter(ClientSocketChannelsHolder csch, Slog sl){
		this.sl = sl;
		this.csch=csch;
		this.activeChannels = csch.getActiveChannels();
		this.rpcDataWriteUtil = new RPCDataWriteUtil(sl);
	}

	public synchronized void addActiveChannel(ClientSocketChannel csc){
		activeChannels.add(csc);
		this.notify();
	}
	public void run(){
		while(true){
			try{
				if(activeChannels.size()>0){
					sendActives();
				}
			}catch(Throwable t){
				if(sl!=null)
					sl.error(t,t.toString());
			}
			if(activeChannels.size()==0){
				synchronized(this){
					if(activeChannels.size()==0){
						try {
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
		for(int i=0;i<activeChannels.size();i++){
			ClientSocketChannel csc = activeChannels.get(i);
			if(csc!=null){
				finshedActing.add(csc);
				askChannel(csc);
			}
		}
		for(int i=0;i<finshedActing.size();i++){
			synchronized(this){
				activeChannels.remove(finshedActing.get(i));
			}
		}
		finshedActing.clear();
	}
	private void askChannel(ClientSocketChannel csc){
		Map<Long,RPCData> allDataToWrite = csc.getDataToWrite();
		Iterator<Long> it = allDataToWrite.keySet().iterator();
		while(it.hasNext()){
			try{
				SocketChannel socketChannel = csc.getSocketChannel();
				Long serialNumber = it.next();
				RPCData rpcData = allDataToWrite.get(serialNumber);
				it.remove();
				if(rpcData.trace!=null&&rpcData.trace.length()>0){
					if(sl!=null&&sl.isDebugEnabled())
						sl.debug("write:"+rpcData.trace);
				}
				this.rpcDataWriteUtil.writeRPCData(socketChannel,rpcData);
				if(rpcData.trace!=null&&rpcData.trace.length()>0){
					if(sl!=null&&sl.isDebugEnabled())
						sl.debug("Written:"+rpcData.trace);
				}
				synchronized(rpcData){
					rpcData.notify();
				}
			}catch(IOException ioe){
				if(sl!=null)
					sl.error(ioe,"Askchannel will close csc due to:"+ioe.toString());
				csc.close(sl);
			}catch(Throwable t){
				if(sl!=null)
					sl.error(t,"Not sure what to do with this error in askChannel:"+t.toString());
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					if(sl!=null)
						sl.error(e,e.toString());
				}
			}
		}
	}
}