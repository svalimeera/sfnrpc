/*
 * Free software. Use at your own risk. Okay to modify and re-distribute.
 * Project is at https://code.google.com/p/sfnrpc
 * 
 */
package sfn.core.rpc.client;

public class RPCInvokeException extends RuntimeException {
	String message;
	public RPCInvokeException(Throwable t){
		super(t);
	}
	public void setMessage(String message){
		this.message = message;
	}
	public String getMessage(){
		return super.getMessage()+"<>"+message;		
	}
	public String toString(){
		return super.toString()+"<>"+message;
	}
}
