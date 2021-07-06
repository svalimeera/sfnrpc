/*
 * Free software. Use at your own risk. Okay to modify and re-distribute.
 * Project is at https://code.google.com/p/sfnrpc
 * 
 */
package sfn.core.rpc.client;

import java.util.ArrayList;
import java.util.List;

import sfn.core.rpc.log.Slog;

public class ClientSocketChannelPoolPendingReturns extends Thread{
	private Slog sl;
	private ClientSocketChannelsPool cscp;
	public ClientSocketChannelPoolPendingReturns(Slog sl, ClientSocketChannelsPool cscp){
		this.sl = sl;
		this.cscp = cscp;
	}
	private List<ClientSocketChannel> returns = new ArrayList<ClientSocketChannel>();
	public void returnToPool(ClientSocketChannel csc){
		synchronized(this){
			if(csc!=null){
				returns.add(csc);
			}
			this.notify();
		}
	}
	public void run(){
		while(true){
			try{
				synchronized(this){
					while(returns.size()<=0){
						try {
							this.wait(10000);					
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							if(sl!=null)
								sl.error(e,e.toString());
						}
					}
				}
				boolean removed=false;
				for(int i=0;i<returns.size();i++){
					if(removed){
						i--;
						removed=false;
					}
					try{
						ClientSocketChannel csc = (ClientSocketChannel)returns.get(i);
						if(csc!=null){
							if(csc.isLoaded()){
								if(sl!=null&&sl.isDebugEnabled())
									sl.debug("the csc is loaded cannot return:"+csc.loadSize()[0]+"<>"+csc.loadSize()[1]);
								continue;
							}else{
								if(sl!=null&&sl.isDebugEnabled())
									sl.debug("the csc was returned:"+csc.loadSize()[0]+"<>"+csc.loadSize()[1]);
								cscp.returnObject(csc.getAddress(),csc);
								returns.remove(i);
								removed=true;
							}
						}
					}catch(Exception e){
						if(sl!=null)
							sl.error(e,e.toString());
					}
				}				
			}catch(Throwable e){
				if(sl!=null)
					sl.error(e,e.toString());
			}
			
			if(returns.size()>0){
				if(sl!=null&&sl.isDebugEnabled())
					sl.debug("Did not extinguish all returns so sleep some");
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}				
			}
			
		}
	}
}