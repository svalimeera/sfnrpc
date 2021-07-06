/*
 * Free software. Use at your own risk. Okay to modify and re-distribute.
 * Project is at https://code.google.com/p/sfnrpc
 * 
 */
package sfn.core.rpc.log;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

public class Slog {
    private Logger LOG;
    private static Map<String,Slog> slogs = new HashMap<String,Slog>();
    public static Slog getSlog(String categoryName){
        Slog sl = Slog.slogs.get(categoryName);
        if(sl==null){
            synchronized(Slog.class){
                sl = Slog.slogs.get(categoryName);
                if(sl==null){
                    sl = new Slog(categoryName);
                    slogs.put(categoryName,sl);
                }
            }
        }
        return sl;
    }
    private Slog(String categoryName){
        LOG = (Logger)LoggerFactory.getLogger(categoryName);
    }
    public void error(Throwable t, String message){
        LOG.error(message, t);
    }
    public void error(String message){
        LOG.error(message);
    }
    public void warn(String message){
        LOG.warn(message);
    }
    public boolean isInfoEnabled(){
        return LOG.isInfoEnabled();
    }
    public void info(String message){
        LOG.info(message);
    }
    public boolean isDebugEnabled(){
        return LOG.isDebugEnabled();
    }
    public void debug(String message){
        LOG.debug(message);
    }
}