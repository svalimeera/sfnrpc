/*
 * Free software. Use at your own risk. Okay to modify and re-distribute.
 * Project is at https://code.google.com/p/sfnrpc
 * 
 */
package sfn.core.rpc.client;

import org.apache.commons.pool.impl.GenericKeyedObjectPool;

import sfn.core.rpc.log.Slog;

public class ClientSocketChannelsPool extends GenericKeyedObjectPool{
	private ClientSocketChannelsPool cscp;
	private Slog sl;
	public ClientSocketChannelsPool(ClientSocketChannelPoolableObjectFactory ccpof, Slog sl){
		super(ccpof);
		this.sl=sl;
		setMaxActive(1000);
		setMaxIdle(100);
		setLifo(false);
		setTimeBetweenEvictionRunsMillis(24*60*1000);
		setMinEvictableIdleTimeMillis(24*60*1000);
		setWhenExhaustedAction(WHEN_EXHAUSTED_FAIL);		
		setTestOnBorrow(true);
		setTestOnReturn(true);
		setTestWhileIdle(true);
	}
	
	private ClientSocketChannelsPool(ClientSocketChannelPoolableObjectFactory cscpof){
		super(cscpof);
	}
}