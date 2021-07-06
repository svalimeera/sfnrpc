/*
 * Free software. Use at your own risk. Okay to modify and re-distribute.
 * Project is at https://code.google.com/p/sfnrpc
 * 
 */
package sfn.core.rpc.test;

import sfn.core.rpc.client.RPCClient;
import sfn.core.rpc.log.Slog;

public class TestClient {
	private static TestClient ts = new TestClient();
	private Slog sl = Slog.getSlog(TestClient.class.getName());
	private RPCClient rpcc = null;
	public static TestClient createInstance(){
		return ts;
	}
	private TestClient(){
		rpcc = new RPCClient(sl);
		rpcc.start();
	}
	public void invoke(){
		Object [] objArr = new Object[1];
		TestClientObject to = new TestClientObject();
		to.data = "HELLO,";
		objArr[0] = to;
		Class [] classArr = new Class[1];
		classArr[0] = TestClientObject.class;
		TestServerObject response = (TestServerObject)rpcc.invoke("#URN1","127.0.0.1:7878","echo",true,60,"", objArr,classArr);
		sl.debug("The response was:"+response.data);
	}
	public static void main(String [] args){
		TestClient tc = TestClient.createInstance();
		while(true){
			try{
				tc.invoke();
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
	}
}
