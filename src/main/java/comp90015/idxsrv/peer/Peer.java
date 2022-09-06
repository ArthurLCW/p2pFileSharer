package comp90015.idxsrv.peer;


import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.LinkedBlockingDeque;

import comp90015.idxsrv.filemgr.FileDescr;
import comp90015.idxsrv.filemgr.FileMgr;
import comp90015.idxsrv.message.*;
import comp90015.idxsrv.server.IOThread;
import comp90015.idxsrv.textgui.ISharerGUI;

/**
 * Skeleton Peer class to be completed for Project 1.
 * @author aaron
 *
 */
public class Peer implements IPeer {

	private IOThread ioThread;
	
	private LinkedBlockingDeque<Socket> incomingConnections;
	
	private ISharerGUI tgui;
	
	private String basedir;
	
	private int timeout;
	
	private int port;

	private int serverPort;

	private ServerSocket serverSocket;
	
	public Peer(int port, String basedir, int socketTimeout, ISharerGUI tgui) throws IOException {
		this.tgui=tgui;
		this.port=port;
		this.timeout=socketTimeout;
		this.basedir=new File(basedir).getCanonicalPath();
		incomingConnections = new LinkedBlockingDeque<Socket>();
		ioThread = new IOThread(port,incomingConnections,socketTimeout,tgui);
		ioThread.start();
		serverSocket = new ServerSocket(0);
		serverPort = serverSocket.getLocalPort();
	}
	
	public void shutdown() throws InterruptedException, IOException {
		ioThread.shutdown();
		ioThread.interrupt();
		ioThread.join();
	}
	
	/*
	 * Students are to implement the interface below.
	 */
	
	@Override
	public void shareFileWithIdxServer(File file, InetAddress idxAddress, int idxPort, String idxSecret,
			String shareSecret) {
		try (Socket socket = new Socket(idxAddress, idxPort)){
			InputStream inputStream = socket.getInputStream();
			OutputStream outputStream = socket.getOutputStream();
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
			BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));

			readMsg(bufferedReader);
			writeMsg(bufferedWriter, new AuthenticateRequest(idxSecret));
			AuthenticateReply authenticateReply = (AuthenticateReply) readMsg(bufferedReader);

			RandomAccessFile randomAccessFile = new RandomAccessFile(file,"rw");
			FileDescr fileDescr = new FileDescr(randomAccessFile);
			String filename = file.getName();
			ShareRequest shareRequest = new ShareRequest(fileDescr, filename,shareSecret,serverPort);
			writeMsg(bufferedWriter, shareRequest);
			ShareReply shareReply = (ShareReply) readMsg(bufferedReader);
			tgui.logDebug(shareReply.toString());
			ShareRecord shareRecord = new ShareRecord(new FileMgr(filename, fileDescr), shareReply.numSharers,
					"status", idxAddress, idxPort, idxSecret, shareSecret);
			tgui.addShareRecord(file.getCanonicalPath(), shareRecord);

		}catch (Exception e){
			tgui.logError("share failed...");
			e.printStackTrace();
		}finally {
			tgui.logInfo("share file done");
		}
	}

	@Override
	public void searchIdxServer(String[] keywords, 
			int maxhits, 
			InetAddress idxAddress, 
			int idxPort, 
			String idxSecret) {
		tgui.logError("searchIdxServer unimplemented");
	}

	@Override
	public boolean dropShareWithIdxServer(String relativePathname, ShareRecord shareRecord) {
		tgui.logError("dropShareWithIdxServer unimplemented");
		return false;
	}

	@Override
	public void downloadFromPeers(String relativePathname, SearchRecord searchRecord) {
		tgui.logError("downloadFromPeers unimplemented");
	}

	private void writeMsg(BufferedWriter bufferedWriter, Message msg) throws IOException {
		tgui.logDebug("sending: "+msg.toString());
		bufferedWriter.write(msg.toString());
		bufferedWriter.newLine();
		bufferedWriter.flush();
	}

	private Message readMsg(BufferedReader bufferedReader) throws IOException, JsonSerializationException {
		String jsonStr = bufferedReader.readLine();
		if(jsonStr!=null) {
			Message msg = (Message) MessageFactory.deserialize(jsonStr);
			tgui.logDebug("received: "+msg.toString());
			return msg;
		} else {
			throw new IOException();
		}
	}
}
