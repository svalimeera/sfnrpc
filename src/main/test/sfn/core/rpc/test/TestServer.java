/*
 * Free software. Use at your own risk. Okay to modify and re-distribute.
 * Project is at https://code.google.com/p/sfnrpc
 * 
 */
package sfn.core.rpc.test;

import java.io.IOException;

import sfn.core.rpc.log.Slog;
import sfn.core.rpc.server.RPCServer;

public class TestServer {
    private static TestServer ts = new TestServer();
    private Slog sl = Slog.getSlog(TestServer.class.getName());    
    public static TestServer createInstance(){
        return ts;
    }
    private TestServer(){
        try {
        	RPCServer rpcServer = new RPCServer(sl);
        	rpcServer.publish("#URN1",TestService.getInstance());
        	rpcServer.startServer("127.0.0.1",7878,null);
        } catch (IOException e) {
            sl.error(e,e.toString());
        }        
    }
    public static void main(String [] args){
        TestServer.createInstance();
    }
}
