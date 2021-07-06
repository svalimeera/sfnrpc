/*
 * Free software. Use at your own risk. Okay to modify and re-distribute.
 * Project is at https://code.google.com/p/sfnrpc
 * 
 */
package sfn.core.rpc.client;

import java.util.ArrayList;
import java.util.List;

import sfn.core.rpc.log.Slog;

public class ClientSocketChannelPoolPendingDestroys extends Thread{
	private Slog sl;
	private ClientSocketChannelsHolder csch;
	public ClientSocketChannelPoolPendingDestroys(ClientSocketChannelsHolder csch,Slog sl){
		this.csch = csch;
		this.sl = sl;
	}
	private List<ClientSocketChannel> destroys = new ArrayList<ClientSocketChannel>();

	public void destroyClientSocketChannel(ClientSocketChannel csc){
		synchronized(this){
			if(csc!=null){
				destroys.add(csc);
				this.notify();
			}
		}
	}
	public void run(){
		while(true){
			if(sl!=null&&sl.isDebugEnabled())
				sl.debug("cscppd:a:"+destroys.size());
			try{
				synchronized(this){
					while(destroys.size()<=0){
						try {
							this.wait(10000);
						} catch (InterruptedException e) {
							if(sl!=null)
								sl.error(e,e.toString());
						}
					}
				}
				boolean removed = false;
				for(int i=0;i<destroys.size();i++){
					if(removed){
						i--;
						removed=false;
					}
					try{
						ClientSocketChannel csc = (ClientSocketChannel)destroys.get(i);
						if(csc!=null){
							if(csc.closeIfPossible(sl)){
								if(sl!=null&&sl.isDebugEnabled())
									sl.debug("destroyed this csc:"+csc.hashCode());						
								csch.remove(csc,sl);
								destroys.remove(csc);
								removed=true;
							}
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
			if(destroys.size()>0){
				if(sl!=null&&sl.isDebugEnabled())
					sl.debug("Did not extinguish all destroys so sleep some");
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}				
			}
		}
	}
}