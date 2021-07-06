/*
 * Free software. Use at your own risk. Okay to modify and re-distribute.
 * Project is at https://code.google.com/p/sfnrpc
 * 
 */
package sfn.core.rpc.dto;

public class RPCMarker {
	public static String markerS="?";
	public static byte [] markerSB = markerS.getBytes();
	public static String markerE="_rpcE__";
	public static byte [] markerEB = markerE.getBytes();
	public static int maxTransferBytes = 1048576; 
	public static int contentByteLength = 4;
	public static int RPCHeaderByteLength = 
			markerSB.length+//1
			markerEB.length+//7
			contentByteLength;//4
			//=12 bytes
}
