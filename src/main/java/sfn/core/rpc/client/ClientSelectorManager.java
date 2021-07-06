/*
 * Free software. Use at your own risk. Okay to modify and re-distribute.
 * Project is at https://code.google.com/p/sfnrpc
 * 
 */
package sfn.core.rpc.client;

import java.io.EOFException;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.List;

import sfn.core.rpc.dto.RPCDataReadUtil;
import sfn.core.rpc.dto.RPCDataResponse;
import sfn.core.rpc.log.Slog;

public abstract class ClientSelectorManager extends Thread{
	private Slog sl;
	private Selector selector;
	private ClientSocketChannelsHolder csch;
	private ClientWriter cw;
	private ClientSocketChannelPoolPendingDestroys  cscppd;
	private ClientSocketChannelPoolableObjectFactory ccpof;
	protected ClientSocketChannelsPool cscp;
	protected ClientSocketChannelPoolPendingReturns cscppr;
	private RPCDataReadUtil rpcDataReadUtil = null;
	public ClientSelectorManager(Slog sl){
		this.sl = sl;
		try {
			this.selector = Selector.open();
			this.rpcDataReadUtil = new RPCDataReadUtil(sl);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void start(){
		csch = new ClientSocketChannelsHolder();
		cw = new ClientWriter(csch,sl);
		cw.start();
		cscppd = new ClientSocketChannelPoolPendingDestroys(csch,sl);
		cscppd.start();
		ccpof = 
			new ClientSocketChannelPoolableObjectFactory(
					cw,csch,cscppd,this,sl
			);
		cscp = new ClientSocketChannelsPool(ccpof,sl);
		cscppr = new ClientSocketChannelPoolPendingReturns(sl, cscp);
		cscppr.start();
		super.start();
	}
	public Selector getSelector(){
		return this.selector;
	}
	public void wakeup(){
		this.selector.wakeup();
	}
	public void run(){
		int zeroSelect=0;
		while(true){
			try {
				int selected = 0;
				connect();
				selected = this.selector.select();
				if(selected>0){
					zeroSelect=0;
					Iterator<SelectionKey> selectedKeys = this.selector.selectedKeys().iterator();
					while (selectedKeys.hasNext()) {
						SelectionKey key = selectedKeys.next();						
						selectedKeys.remove();
						if (!key.isValid()) {
							key.cancel();
							continue;
						}
						if(key.isConnectable()){
							SocketChannel socketChannel = (SocketChannel)(key.channel()); 
							socketChannel.finishConnect();
							socketChannel.register(selector,SelectionKey.OP_READ);//required to stop zero selects
						}else if (key.isReadable()) {
							handleRead(key);
						}
					}				
				}else{
					zeroSelect++;
					if(zeroSelect>100){
						zeroSelect=0;
						synchronized(this){this.wait(1000);}
						if(sl!=null&&sl.isDebugEnabled())
							sl.debug("Hello for zero selected");
					}
				}
			}catch (Throwable e) {
				if(sl!=null)
					sl.error(e,e.toString());
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
					if(sl!=null)
						sl.error(e1,e1.toString());
				}
			}//blocks till something is available
		}
	}
	private void connect(){
		List<ClientSocketChannel> unconnected = csch.getUnconnected();
		for(int i=0;i<unconnected.size();i++){
			ClientSocketChannel csc = unconnected.remove(0);
			try {
				csc.getSocketChannel().register(selector, SelectionKey.OP_CONNECT);
			} catch (ClosedChannelException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			csc.connect(sl);
		}
	}
	private void handleRead(SelectionKey key){
		SocketChannel socketChannel = (SocketChannel)key.channel();
		ClientSocketChannel csc = csch.get(socketChannel);
		try {
			RPCDataResponse rpcDataResponse = null;
			if(socketChannel.isConnected()){
				if(socketChannel.isOpen()){
					while(((rpcDataResponse = this.rpcDataReadUtil.readRPCDataResponse(key,socketChannel))!=null)){
						csc.assignResponse(rpcDataResponse, sl);
					}
				}else{
					if(sl!=null&&sl.isDebugEnabled())
						sl.debug("SocketChannel is not open");
				}
			}else{
				if(sl!=null&&sl.isDebugEnabled())
					sl.debug("SocketChannel is not connected");
			}
		}catch(EOFException eofe){
			key.cancel();
			csc.close(sl);
		}catch (IOException e){
			key.cancel();
			csc.close(sl);
			if(sl!=null)
				sl.error(e,"not sure what to do with this:"+e.toString());
		}catch (Throwable e){
			key.cancel();
			csc.close(sl);
			if(sl!=null)
				sl.error(e,"not sure 2 what to do with this:"+e.toString());
		}
	}
	
	public abstract Object invoke(
			String serverURN, 
			String address,
			String methodName,
			boolean waitForResponse,
			int timeoutInSecs,
			String trace,
			Object [] parameters,
			Class [] classArr
	);
}