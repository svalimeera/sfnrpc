/*
 * Free software. Use at your own risk. Okay to modify and re-distribute.
 * Project is at https://code.google.com/p/sfnrpc
 * 
 */
package sfn.core.rpc.server;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

import sfn.core.rpc.dto.RPCData;

public class HandlerCoreProcessor {
	private HashMap<String,Object> urn_obj = new HashMap<String,Object>();
	HashMap<String,Object> getUrn_obj(){
		return urn_obj;
	}
	public HandlerCoreProcessor(){
		
	}
    public Object process(RPCData rpcData){		
        Object result = null;
        try{
            Object instance = urn_obj.get(rpcData.URN);
            if(instance==null){
                throw new RuntimeException(
                        "Reached server "+
                        " but no instance corresponding to URN "+rpcData.URN
                );
            }
            String methodName = rpcData.methodName;
            Class c = instance.getClass();
            Class [] classArr = getClassArr(rpcData.parameters,rpcData.classArgs);         
            Method method = c.getMethod(rpcData.methodName,classArr);
            result = method.invoke(instance, rpcData.parameters);
            return result;
        }catch(InvocationTargetException ite){
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			Throwable t = ite.getTargetException();
			t.printStackTrace(pw);
			RuntimeException re = new RuntimeException(sw.toString());
            return re;
        }catch(Throwable t){
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			t.printStackTrace(pw);
			RuntimeException re = new RuntimeException(sw.toString());
            return re;
        }
    }
    private Class [] getClassArr(Object [] parameters, Class [] classArr){
        if(classArr!=null){
            return classArr;
        }else{
            if(parameters!=null){
                classArr = new Class[parameters.length];          
                for(int i=0;i<parameters.length;i++){
                    Object obj = parameters[i];
                    if(obj!=null){
                        classArr[i] = obj.getClass();
                    }
                }
                return classArr;
            }else{
                return null;
            }
        }        
    }
}