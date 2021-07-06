/*
 * Free software. Use at your own risk. Okay to modify and re-distribute.
 * Project is at https://code.google.com/p/sfnrpc
 * 
 */
package sfn.core.rpc.dto;

import java.io.EOFException;

public class RPCEOFException extends EOFException{
	public RPCEOFException(String message){
		super(message);
	}
}
