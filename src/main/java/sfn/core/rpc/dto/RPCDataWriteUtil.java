/*
 * Free software. Use at your own risk. Okay to modify and re-distribute.
 * Project is at https://code.google.com/p/sfnrpc
 * 
 */
package sfn.core.rpc.dto;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import sfn.core.rpc.log.Slog;

public class RPCDataWriteUtil {
	private Slog sl = null;
	public RPCDataWriteUtil(Slog sl){
		this.sl = sl;
	}
	public void writeRPCData(SocketChannel socketChannel, RPCData rpcData) throws IOException{
		ByteArrayOutputStream bStream = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(bStream);
		oos.writeObject(rpcData);
		byte[] byteVal = bStream.toByteArray();
		write(byteVal,socketChannel);
	}
	public void writeRPCDataResponse(SocketChannel socketChannel, RPCDataResponse rpcDataResponse) throws IOException{
		if(sl!=null&&sl.isDebugEnabled())
			sl.debug("The markerS is:"+RPCMarker.markerSB.length+"<>"+RPCMarker.markerEB.length);
		ByteArrayOutputStream bStream = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(bStream);
		try{
			oos.writeObject(rpcDataResponse);
		}catch(Throwable t){			
			rpcDataResponse.object = t;
			bStream = new ByteArrayOutputStream();
			oos = new ObjectOutputStream(bStream);
			oos.writeObject(rpcDataResponse);
		}
		byte[] byteVal = bStream.toByteArray();
		write(byteVal,socketChannel);
	}
	private void write(byte [] byteVal,SocketChannel socketChannel) throws IOException{
		int allocatedLength = 
			RPCMarker.RPCHeaderByteLength+
			byteVal.length;
		ByteBuffer rpcDataLengthBB = null;
		if(allocatedLength<=RPCMarker.maxTransferBytes){
			rpcDataLengthBB = ByteBuffer.allocate(allocatedLength);
			rpcDataLengthBB.put(RPCMarker.markerSB);
			rpcDataLengthBB.put(RPCMarker.markerEB);
			rpcDataLengthBB.putInt(byteVal.length);
			rpcDataLengthBB.put(byteVal);
			rpcDataLengthBB.flip();			
			writeChunk(rpcDataLengthBB,socketChannel);		
		}else{
			int chunks = allocatedLength/RPCMarker.maxTransferBytes;
			int remain = allocatedLength%RPCMarker.maxTransferBytes;
			if(sl!=null&&sl.isDebugEnabled())
				sl.debug("Data is too large:"+allocatedLength+" will chunk..."+chunks+"<>"+remain);
			//0th chunk
			rpcDataLengthBB = ByteBuffer.allocate(RPCMarker.maxTransferBytes);
			rpcDataLengthBB.put(RPCMarker.markerSB);
			rpcDataLengthBB.put(RPCMarker.markerEB);
			rpcDataLengthBB.putInt(byteVal.length);
			int byteValLengthToWrite = RPCMarker.maxTransferBytes-RPCMarker.RPCHeaderByteLength;
			rpcDataLengthBB.put(byteVal,0,byteValLengthToWrite);
			rpcDataLengthBB.flip();			
			int written = writeChunk(rpcDataLengthBB,socketChannel);
			if(sl!=null&&sl.isDebugEnabled())
				sl.debug("Written chunk 0: "+written);
			//intermediate chunks
			int byteValOffset = byteValLengthToWrite;
			for(int i=1;i<chunks;i++){
				rpcDataLengthBB.clear();
				if(sl!=null&&sl.isDebugEnabled())
					sl.debug("Chunk:"+i+" The starting offset is:"+byteValOffset);
				rpcDataLengthBB.put(byteVal,byteValOffset,RPCMarker.maxTransferBytes);
				if(sl!=null&&sl.isDebugEnabled())
					sl.debug("Chunk:"+i+" the rpcDataLengthBB.limit() is:"+rpcDataLengthBB.limit());
				byteValOffset += RPCMarker.maxTransferBytes;
				rpcDataLengthBB.flip();			
				int thiswritten = writeChunk(rpcDataLengthBB,socketChannel);
				if(sl!=null&&sl.isDebugEnabled())
					sl.debug("Written chunk "+i+": "+thiswritten);
				written += thiswritten;
			}
			//remainder chunk
			if(remain>0){
				rpcDataLengthBB.clear();
				rpcDataLengthBB.limit(remain);
				if(sl!=null&&sl.isDebugEnabled())
					sl.debug("Remain: The starting offset is:"+byteValOffset);
				rpcDataLengthBB.put(byteVal,byteValOffset,remain);
				if(sl!=null&&sl.isDebugEnabled())
					sl.debug("Remain: the rpcDataLengthBB.limit() is:"+rpcDataLengthBB.limit());
				rpcDataLengthBB.flip();			
				int thiswritten = writeChunk(rpcDataLengthBB,socketChannel);
				if(sl!=null&&sl.isDebugEnabled())
					sl.debug("Written remain: "+thiswritten);
				written += thiswritten;
			}
			if(sl!=null&&sl.isDebugEnabled())
				sl.debug("Total written="+written+"<>expected:"+allocatedLength);
		}
	}
	private int writeChunk(ByteBuffer dst, SocketChannel socketChannel) throws IOException{
		int written = socketChannel.write(dst);
		int attempts=0;
		int sleepct=0;
		while(written<dst.limit()){
			if(sl!=null&&sl.isDebugEnabled())
				sl.debug("the write attempts is:"+attempts);
			if(attempts++>100000){
				throw new IOException("cannot fully right");
			}
			sleepct++;
			if((sleepct/10)>0){
				try {
					Thread.sleep(10);
					sleepct=0;
				} catch (InterruptedException e) {
					if(sl!=null)
						sl.error(e,e.toString());
				}
				if(sl!=null&&sl.isDebugEnabled())
					sl.debug("Too many attempts are happening for write. Monitor closely:"+attempts);
			}
			written += socketChannel.write(dst);
		}
		if((attempts/100)>0){
			if(sl!=null&&sl.isDebugEnabled())
				sl.debug("written success in attempts:"+attempts+"<>"+dst);
		}
		if(sl!=null&&sl.isDebugEnabled())
			sl.debug("Writing: "+written+"<>"+(written/(1024))+"KB"+"<>"+dst.limit());
		return written;
	}
}