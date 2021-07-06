/*
 * Free software. Use at your own risk. Okay to modify and re-distribute.
 * Project is at https://code.google.com/p/sfnrpc
 * 
 */
package sfn.core.rpc.dto;
import java.io.Serializable;

public class RPCData implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public String URN;
	public String methodName;
	public Object [] parameters;
	public Class [] classArgs;
	public long serialNumber=-1L;
	public boolean waitForResponse;
	public String trace;
	private static long dataSerialNumber=0L;
	/** Creates a new instance of RPCClass */
	public RPCData(String URN, String methodName, boolean waitForResponse, String trace, Object [] parameters, Class [] classArgs){
		this.URN = URN;
		this.methodName = methodName;
		this.waitForResponse = waitForResponse;
		this.trace = trace;
		this.parameters = parameters;
		this.classArgs = classArgs;
		synchronized(RPCData.class){
			if(dataSerialNumber+1>=Long.MAX_VALUE){
				dataSerialNumber=0L;
			}
			serialNumber = dataSerialNumber++;
		}
	}
	public String parametersToString(Object [] args){
		String result="null";
		if(args!=null&&args.length>0){
			for(int i=0;i<args.length;i++){
				Object obj = args[i];
				if(obj!=null){
					result += obj.toString();
				}else result += "null";
			}
		}
		return result.trim();
	}
	public String toPrint(){
		String result ="";
		result += this.serialNumber+"\n";
		result += URN;
		result += "\n";
		result += methodName;
		result += "\n";
		result += "WaitForResponse:"+waitForResponse;
		result += "\n";
		result += parametersToString(parameters);
		result += "\n";
		result += parametersToString(classArgs);
		return result.trim();
	}
}
