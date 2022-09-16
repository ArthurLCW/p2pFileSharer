package comp90015.idxsrv.peer;


import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.LinkedBlockingDeque;

import comp90015.idxsrv.Filesharer;
import comp90015.idxsrv.filemgr.FileDescr;
import comp90015.idxsrv.filemgr.FileMgr;
import comp90015.idxsrv.message.*;
import comp90015.idxsrv.server.IOThread;
import comp90015.idxsrv.server.IndexElement;
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

	private int blockLength;
	
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

		blockLength = 16 * 1024 * 1024;
		P2PServerRequestThread requestThread = new P2PServerRequestThread(incomingConnections, tgui, port,
				timeout, blockLength);
		requestThread.start();
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
		tgui.clearSearchHits();
		try (Socket socket = new Socket(idxAddress, idxPort)){
			InputStream inputStream = socket.getInputStream();
			OutputStream outputStream = socket.getOutputStream();
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
			BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
			readMsg(bufferedReader);
			writeMsg(bufferedWriter, new AuthenticateRequest(idxSecret));
			AuthenticateReply authenticateReply = (AuthenticateReply) readMsg(bufferedReader);
			tgui.logDebug(authenticateReply.toString());

			SearchRequest searchRequest = new SearchRequest(maxhits, keywords);
			writeMsg(bufferedWriter, searchRequest);
			SearchReply searchReply = (SearchReply) readMsg(bufferedReader);

			for (int i = 0; i < searchReply.hits.length; i++){
				IndexElement hit = searchReply.hits[i];
				InetAddress hitip = InetAddress.getByName(hit.ip);
				SearchRecord searchRecord = new SearchRecord(hit.fileDescr, searchReply.seedCounts[i], idxAddress, idxPort,
						idxSecret, hit.secret);
				tgui.addSearchHit(searchReply.hits[i].filename, searchRecord);
			}
		}catch (Exception e){
			tgui.logError("search failed...");
			e.printStackTrace();
		}finally {
			tgui.logInfo("search file done");
		}

	}

	@Override
	public boolean dropShareWithIdxServer(String relativePathname, ShareRecord shareRecord) {
		InetAddress idxAddress = shareRecord.idxSrvAddress;
		int idxPort = shareRecord.idxSrvPort;
		String idxSecret = shareRecord.idxSrvSecret;
		try (Socket socket = new Socket(idxAddress, idxPort)) {
			InputStream inputStream = socket.getInputStream();
			OutputStream outputStream = socket.getOutputStream();
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
			BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));

			readMsg(bufferedReader);
			writeMsg(bufferedWriter, new AuthenticateRequest(idxSecret));
			AuthenticateReply authenticateReply = (AuthenticateReply) readMsg(bufferedReader);
			tgui.logDebug(authenticateReply.toString());
			String split = System.getProperty("os.name").toLowerCase().contains("windows") ? "\\\\": "/";
			String[] splitStr = relativePathname.split(split);
			String filename = splitStr[splitStr.length-1];
			String fileMd5 = shareRecord.fileMgr.getFileDescr().getFileMd5();
			String sharerSecret = shareRecord.sharerSecret;
			DropShareRequest dropShareRequest = new DropShareRequest(filename, fileMd5, sharerSecret, serverPort);
			writeMsg(bufferedWriter, dropShareRequest);
			DropShareReply dropShareReply = (DropShareReply) readMsg(bufferedReader);
			tgui.logDebug(dropShareReply.toString() );
			return dropShareReply.success;


		}catch (Exception e){
			tgui.logError("drop failed...");
			e.printStackTrace();
		}finally {
			tgui.logInfo("drop file done");
		}
		return false;
	}

	@Override
	public void downloadFromPeers(String relativePathname, SearchRecord searchRecord) {
		tgui.logError("downloadFromPeers implemented");
		String targetIP = searchRecord.idxSrvAddress.toString();
		int targetPort = searchRecord.idxSrvPort;
		FileDescr fileDescr = searchRecord.fileDescr;
		String fileMd5 = fileDescr.getFileMd5();
		String idxSrvSecret = searchRecord.idxSrvSecret;

		tgui.logInfo(targetIP+" "+targetPort+" "+fileMd5+" "+relativePathname);

		P2PClientThread client = null;
		try {
			client = new P2PClientThread(targetIP, targetPort, timeout, tgui, relativePathname,
					fileMd5);
		} catch (IOException e) {
			tgui.logInfo("P2P client: "+e.toString());
		}
		client.start();
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
