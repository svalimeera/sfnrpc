/*
 * Free software. Use at your own risk. Okay to modify and re-distribute.
 * Project is at https://code.google.com/p/sfnrpc
 * 
 */
package sfn.core.rpc.server;

import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import sfn.core.rpc.dto.RPCData;
import sfn.core.rpc.dto.RPCDataReadUtil;
import sfn.core.rpc.log.Slog;

public class HandlerNio extends Thread{
	private String serverFqdn;
	private int port = -1;
	private Slog sl;
	private ThreadPoolExecutor rpcServerPool;
	private Selector selector;
	private ServerCommunicationsChannelsHolder scch;
	private ServerCommunicationChannelsHealth scchealth;
	private ServerWriter sw;
	private HandlerCoreProcessor hcp;
	private RPCDataReadUtil rpcReadDataUtil = null;
	public HandlerNio(			
			String serverFqdn,
			Integer portI,
			ServerCommunicationsChannelsHolder scch,
			ServerCommunicationChannelsHealth scchealth,
			ServerWriter sw,
			HandlerCoreProcessor hcp,
			Slog sl){
		this.sl = sl;
		this.serverFqdn = serverFqdn;
		this.port = portI.intValue();
		this.scch = scch;
		this.scchealth = scchealth;
		this.sw = sw;
		this.hcp = hcp;
		this.rpcReadDataUtil = new RPCDataReadUtil(sl);
	}	
	public Integer getPort(){
		return this.port;
	}
	public void run(){
		try {
			acceptConnections();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private void acceptConnections() throws IOException{
		ServerSocket ss = null;
		ServerSocketChannel ssc = null;
		SynchronousQueue<Runnable> rpcServerPool_abq = new SynchronousQueue<Runnable>();
		rpcServerPool = new ThreadPoolExecutor(200,1000, 50L, TimeUnit.MINUTES,rpcServerPool_abq,new ThreadPoolExecutor.AbortPolicy());
		selector = SelectorProvider.provider().openSelector();
		ssc = ServerSocketChannel.open();
		ssc.configureBlocking(false);		
		if(sl!=null&&sl.isDebugEnabled())
			sl.debug("Connecting to serverHostName:"+serverFqdn+"<>"+port);
		InetSocketAddress isa = new InetSocketAddress(serverFqdn, port);

		ss = ssc.socket();
		//ss.setReuseAddress(true);
		ss.bind(isa);
		while(!ss.isBound()){
			if(sl!=null&&sl.isDebugEnabled())
				sl.debug("ss is not bound. will wait again");
			synchronized(this){try {this.wait(2000);} catch (InterruptedException e) {e.printStackTrace();}}
		}
		if(sl!=null&&sl.isDebugEnabled())
			sl.debug("ss is bound !!!");
		ssc.register(selector, 
				SelectionKey.OP_ACCEPT);
		int zeroSelect=0;
		while(true){
			if(sl!=null&&sl.isDebugEnabled())
				sl.debug("hnio:loop started");
			try{
				int selected= selector.select();
				if(selected>0){
					zeroSelect=0;
					try{
						Set<SelectionKey> readyKeys = selector.selectedKeys();
						Iterator<SelectionKey> i = readyKeys.iterator();
						while (i.hasNext()) {
							SelectionKey key = i.next();
							i.remove();
							if (!key.isValid()) {
								key.cancel();
								continue;
							}
							if(key.isAcceptable()){
								ServerSocketChannel nextReady = 
									(ServerSocketChannel)key.channel();
								SocketChannel socketChannel = nextReady.accept();
								socketChannel.configureBlocking(false);
								socketChannel.socket().setReuseAddress(true);
								scch.put(socketChannel, new ServerCommunicationChannel(socketChannel,scch));
								socketChannel.register(this.selector, SelectionKey.OP_READ);
							}else if (key.isReadable()) {
								if(sl!=null&&sl.isDebugEnabled())
									sl.debug("Got readable on Server");
								handleRead(key);
							}
						}					
					}catch(Exception e){
						if(sl!=null)
							sl.error(e,"Exception during handler processor, execute call. Is this rejectedhandler due to queue size?: "+rpcServerPool.getActiveCount()+"<>"+rpcServerPool_abq.size());
					}
				}else{
					zeroSelect++;
					if(zeroSelect>100){
						zeroSelect=0;
						synchronized(this){this.wait(1000);}
						if(sl!=null&&sl.isDebugEnabled())
							sl.debug("SERVER: the selected is 0");
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
			}
		}
	}
	private void handleRead(SelectionKey key){
		SocketChannel socketChannel = (SocketChannel)key.channel();
		ServerCommunicationChannel scc = scch.get(socketChannel);
		try {
			RPCData rpcData = null;
			if(socketChannel.isConnected()){
				if(socketChannel.isOpen()){
					while(((rpcData = this.rpcReadDataUtil.readRPCData(key,socketChannel))!=null)){
						if(rpcData.trace!=null&&rpcData.trace.length()>0){
							if(sl!=null&&sl.isDebugEnabled())
								sl.debug("received:"+rpcData.trace);
						}
						HandlerProcessor handlerProcessor = new HandlerProcessor(socketChannel, rpcData,sw,scch,hcp,sl);
						try{
							rpcServerPool.submit(handlerProcessor);
						}catch(Throwable t){
							if(sl!=null)
								sl.error(t,t.toString());
							handlerProcessor.response(t);
						}
					}			
				}else{
					if(sl!=null&&sl.isDebugEnabled())
						sl.debug("The socketChannel is not open");
				}
			}else{
				if(sl!=null&&sl.isDebugEnabled())
					sl.debug("The socketChannel is not connected");
			}
		}catch(EOFException eofe){
			if(sl!=null)
				sl.error(eofe,eofe.toString());
			key.cancel();
			scc.close(sl);
		}catch (IOException e){
			if(sl!=null)
				sl.error(e,"not sure what to do with this:"+e.toString());
			key.cancel();
			scc.close(sl);
		}catch (Throwable e){
			if(sl!=null)
				sl.error(e,"not sure 2 what to do with this:"+e.toString());
			key.cancel();
			scc.close(sl);
		}
	}
}