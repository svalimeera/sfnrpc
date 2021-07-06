/*
 * Free software. Use at your own risk. Okay to modify and re-distribute.
 * Project is at https://code.google.com/p/sfnrpc
 * 
 */
package sfn.core.rpc.client;

import java.io.IOException;
import java.nio.channels.SocketChannel;

import org.apache.commons.pool.KeyedPoolableObjectFactory;

import sfn.core.rpc.log.Slog;

public class ClientSocketChannelPoolableObjectFactory implements KeyedPoolableObjectFactory {
	private Slog sl;
	private ClientWriter cw;
	private ClientSocketChannelsHolder csch;
	private ClientSocketChannelPoolPendingDestroys cscppd;
	private ClientSelectorManager csm;
	public ClientSocketChannelPoolableObjectFactory(
			ClientWriter cw,
			ClientSocketChannelsHolder csch,
			ClientSocketChannelPoolPendingDestroys cscppd,
			ClientSelectorManager csm,
			Slog sl){
		this.cw = cw;
		this.csch = csch;
		this.cscppd = cscppd;
		this.csm = csm;
		this.sl = sl;
	}
	@Override
	public void activateObject(Object arg0, Object arg1) throws Exception {
		// TODO Auto-generated method stub
		//nothing for activate and passivate for clientsocketchannel
	}
	@Override
	public void destroyObject(Object arg0, Object arg1) throws Exception {
		// TODO Auto-generated method stub		
		ClientSocketChannel csc = (ClientSocketChannel)arg1;
		cscppd.destroyClientSocketChannel(csc);		
	}
	@Override
	public Object makeObject(Object arg0) throws Exception {
		// TODO Auto-generated method stub
		String address = (String)arg0;
		ClientSocketChannel csc = allocate(address);
		if(csc.getSocketChannel().isConnected()){		
			return csc;
		}else{
			int attempts = 1;
			while(csc.getSocketChannel().isOpen()&&!csc.getSocketChannel().isConnected()){
				try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}
				if(attempts++>10) break;
			}
			if(!csc.getSocketChannel().isConnected()){
				csch.remove(csc,sl);
				csc.close(sl);
				//validate (TestOnBorrow=true is configured) will should fail this anyway [not sure]
				throw new RuntimeException("Cannot create csc:"+address);
			}else{				
				return csc;
			}
		}
	}
	private ClientSocketChannel allocate(String address) throws IOException{
		SocketChannel sc = SocketChannel.open();
		sc.configureBlocking(false);
		ClientSocketChannel csc = new ClientSocketChannel(cw);
		csc.setAddress(address);
		csc.setSocketChannel(sc);
		csch.getUnconnected().add(csc);
		csch.put(csc);
		csm.wakeup();
		return csc;
	}
	@Override
	public void passivateObject(Object arg0, Object arg1) throws Exception {
		// TODO Auto-generated method stub
		//nothing for activate and passivate for clientsocketchannel
	}
	@Override
	public boolean validateObject(Object arg0, Object arg1) {
		// TODO Auto-generated method stub
		ClientSocketChannel csc = (ClientSocketChannel)arg1;
		return 
			csc.isValid()&&
			csc.getSocketChannel().isConnected()&&
			csc.getSocketChannel().isOpen()&&
			!csc.getSocketChannel().socket().isInputShutdown()&&
			!csc.getSocketChannel().socket().isOutputShutdown()
			;
	}
}