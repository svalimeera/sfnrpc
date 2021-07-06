/*
 * Free software. Use at your own risk. Okay to modify and re-distribute.
 * Project is at https://code.google.com/p/sfnrpc
 * 
 */
package sfn.core.rpc.dto;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import sfn.core.rpc.log.Slog;

public class RPCDataReadUtil {
	private Slog sl;
	public RPCDataReadUtil(Slog sl){
		this.sl = sl;
	}
	public boolean gotMarker(SocketChannel socketChannel) throws IOException{
		if(sl!=null&&sl.isDebugEnabled())
			sl.debug("gotmarker intiated");
		ByteBuffer rpcDataLengthBBMarker = ByteBuffer.allocate(1);
		int read = oneByteRead(socketChannel,rpcDataLengthBBMarker);
		if(read==1){
			rpcDataLengthBBMarker.flip();
			while(rpcDataLengthBBMarker.get()!=RPCMarker.markerSB[0]){
				rpcDataLengthBBMarker.clear();
				read = oneByteRead(socketChannel,rpcDataLengthBBMarker);
				if(read==0){
					if(sl!=null&&sl.isDebugEnabled())
						sl.debug("Nothing to read B");
					return false;
				}else{
					rpcDataLengthBBMarker.flip();
				}
			}
			if(sl!=null&&sl.isDebugEnabled())
				sl.debug("ok got startMarker:"+read);
			rpcDataLengthBBMarker = ByteBuffer.allocate(7);
			read = readChunk(socketChannel,rpcDataLengthBBMarker);
			rpcDataLengthBBMarker.flip();
			if(read==7&&
					(new String(rpcDataLengthBBMarker.array()).equals(RPCMarker.markerE))
			){
				if(sl!=null&&sl.isDebugEnabled())
					sl.debug("ok got markerE");
				return true;
			}else{
				if(sl!=null&&sl.isDebugEnabled())
					sl.debug("did not get marker A");
				return false;
			}
		}else{
			if(sl!=null&&sl.isDebugEnabled())
				sl.debug("Nothing to read");
			return false;
		}
	}
	public RPCData readRPCData(SelectionKey selectionKey, SocketChannel socketChannel) throws IOException{
		boolean marker = gotMarker(socketChannel);
		if(!marker) return null;
		ByteBuffer rpcDataLengthBB = ByteBuffer.allocate(4);
		int read = readChunk(socketChannel,rpcDataLengthBB);//should be 4
		if(sl!=null&&sl.isDebugEnabled())
			sl.debug("SERVER Read A rpcData :"+read);
		rpcDataLengthBB.flip();
		int rpcDataLength = rpcDataLengthBB.getInt();
		if(sl!=null&&sl.isDebugEnabled())
			sl.debug("SERVER Got length as:"+read+"<>"+rpcDataLength);
		
		ByteArrayOutputStream baos = read(socketChannel,rpcDataLength);		
		ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
		
		RPCData rpcData;
		try {
			rpcData = (RPCData)ois.readObject();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			throw new RuntimeException(e);
		}
		if(sl!=null&&sl.isDebugEnabled())
			sl.debug("SERVER Got rpcData:"+rpcData);
		return rpcData;
	}
	public RPCDataResponse readRPCDataResponse(SelectionKey selectionKey, SocketChannel socketChannel) throws IOException{
		boolean marker = gotMarker(socketChannel);
		if(!marker) return null;
		ByteBuffer rpcDataLengthBB = ByteBuffer.allocate(4);
		int read = readChunk(socketChannel,rpcDataLengthBB);//should be 4
		if(sl!=null&&sl.isDebugEnabled())
			sl.debug("Read A rpcDataResponse :"+read);
		rpcDataLengthBB.flip();
		int rpcDataLength = rpcDataLengthBB.getInt();
		if(sl!=null&&sl.isDebugEnabled())
			sl.debug("Got length as:"+read+"<>"+rpcDataLength);
		ByteArrayOutputStream baos = read(socketChannel,rpcDataLength);
		byte [] readBytes = baos.toByteArray();
		ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(readBytes));		
		RPCDataResponse rpcDataResponse = null;
		try {			
			rpcDataResponse = (RPCDataResponse)ois.readObject();
		} catch (Throwable t) {
			throw new RuntimeException(t);
		}
		if(sl!=null&&sl.isDebugEnabled())
			sl.debug("Client Got 1a rpcDataResponse:"+rpcDataResponse+"<>"+rpcDataResponse.object);
		return rpcDataResponse;
	}
	private int oneByteRead(SocketChannel socketChannel,ByteBuffer dst) throws IOException{
		int read = socketChannel.read(dst);
		if(read==-1){
			throw new RPCEOFException("EOF occurredduring oneByteRead");
		}else{
			return read;
		}
	}
	private ByteArrayOutputStream read(SocketChannel socketChannel,int totalBytesToRead) throws IOException{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		if(totalBytesToRead<=RPCMarker.maxTransferBytes){
			ByteBuffer dst = ByteBuffer.allocate(totalBytesToRead);
			int read = readChunk(socketChannel,dst);
			if(sl!=null&&sl.isDebugEnabled())
				sl.debug("read a total of:"+read+"<>expected:"+totalBytesToRead);
			dst.flip();
			baos.write(dst.array());
		}else{
			//needs chunking.
			int chunks = totalBytesToRead/RPCMarker.maxTransferBytes;
			int remain = totalBytesToRead%RPCMarker.maxTransferBytes;
			int read = 0;
			ByteBuffer dst = null;
			if(chunks>0){
				dst = ByteBuffer.allocate(RPCMarker.maxTransferBytes);
			}
			for(int i=0;i<chunks;i++){
				dst.clear();
				int thisread = readChunk(socketChannel,dst);
				if(sl!=null&&sl.isDebugEnabled())
					sl.debug("Read chunk "+i+" as: "+thisread);
				read += thisread;
				dst.flip();
				baos.write(dst.array());
			}
			if(remain>0){
				dst.clear();
				dst.limit(remain);
				int thisread = readChunk(socketChannel,dst);
				if(sl!=null&&sl.isDebugEnabled())
					sl.debug("Read remain as: "+thisread);
				read += thisread;
				dst.flip();
				baos.write(dst.array());
			}
			if(sl!=null&&sl.isDebugEnabled())
				sl.debug("A total of read got after aggregating chunks:"+read+"<>expected:"+totalBytesToRead);
		}
		return baos;
	}
	private int readChunk(SocketChannel socketChannel,ByteBuffer dst) throws IOException{
		int read = socketChannel.read(dst);
		if(read==-1){
			throw new RPCEOFException("EOF A occurred");
		}else if(read==dst.limit()){
			return read;
		}
		int attempts=0;
		int sleepct = 0;
		while(true){
			if(sl!=null&&sl.isDebugEnabled())
				sl.debug("into attempts for read:"+attempts);
			if(attempts++>100000){
				throw new RPCEOFException("EOF max attempts exceeded");
			}
			sleepct++;
			if((sleepct/10)>0){
				try {
					Thread.sleep(10);
					sleepct=0;
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					if(sl!=null)
						sl.error(e,e.toString());
				}
				if(sl!=null&&sl.isDebugEnabled())
					sl.debug("Too many read attempts are happening. monitor closely..."+dst.limit());
			}	
			int thisread = socketChannel.read(dst);
			if(thisread==-1){
				throw new RPCEOFException("EOF B occurred");
			}else{
				read += thisread;
			}
			if(read==dst.limit()){
				if((attempts/10)>0){
					if(sl!=null&&sl.isDebugEnabled())
						sl.debug("Success read in attempts... "+attempts+"<>"+dst.limit());
				}
				return read;
			}
		}
	}
}