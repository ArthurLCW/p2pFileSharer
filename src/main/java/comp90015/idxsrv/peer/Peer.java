package comp90015.idxsrv.peer;


import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.LinkedBlockingDeque;

import comp90015.idxsrv.message.*;
import comp90015.idxsrv.server.IOThread;
import comp90015.idxsrv.textgui.ISharerGUI;
import comp90015.idxsrv.filemgr.FileDescr;
import comp90015.idxsrv.filemgr.FileMgr;
///


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

	/////
	private Socket client;

	private String ip;

	private String secret;

	public Peer(int port, String basedir, int socketTimeout, ISharerGUI tgui) throws IOException {
		this.tgui=tgui;
		this.port=port;
		this.timeout=socketTimeout;
		this.basedir=new File(basedir).getCanonicalPath();
		/////
		incomingConnections=new LinkedBlockingDeque<Socket>();

		ioThread = new IOThread(port,incomingConnections,socketTimeout,tgui);
		ioThread.start();

		////
		// tgui.logInfo("SIZE:: "+incomingConnections.size());
		InitSocketToServer connector = new InitSocketToServer("localhost", 3200, timeout, tgui);//TODO: cwli: ip/port
		try {
			client = connector.GenerateSocket();
		} catch (JsonSerializationException e) {
			tgui.logWarn(e.toString());
			// throw new RuntimeException(e);
		}
		testClientConnections(); /////////////////////////////////// TODO: Delete later
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
		tgui.logError("shareFileWithIdxServer unimplemented");
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

	public void testClientConnections(){
		boolean upload = false; // TODO: how to know client is requesting
		boolean download = true;

		String targetIP = "localhost"; // TODO: how to know ip port
		int targetPort = 3201; // local port for sharer uploading
		int sharerPort = 3201;

		int blockLength=16*1024*1024;
//		int blockLength=1024;

		String filename = "Distributed Systems -s1-full.mp4";
//		String filename = "course plan.jpg";
//		String filename = "testShort.txt";
//		String filename = "test.jpg";
		String fileMd5 = "m";
		int blockIdx = 0;

		// get MD5
		RandomAccessFile file ;
		FileDescr fileDescr = null;
		try {
			file = new RandomAccessFile("onlyUsedForMD5/"+filename, "r");
			fileDescr = new FileDescr(file, blockLength);
		} catch (NoSuchAlgorithmException | IOException e) {
			tgui.logWarn(e.toString());
			// throw new RuntimeException(e);
		}
		fileMd5 = fileDescr.getFileMd5();



		if (download){ // activator wxh
			tgui.logInfo("I am downloading!");
			try {
				P2PServerRequestThread requestThread = new P2PServerRequestThread(incomingConnections, tgui, sharerPort,
						timeout, blockLength);
				requestThread.start();

				P2PClientThread client = new P2PClientThread(targetIP, targetPort, timeout, tgui, filename,
						fileMd5, blockIdx, blockLength);
				client.start();
			} catch (IOException e) {
				tgui.logWarn(e.toString());
				// throw new RuntimeException(e);
			}
		}

		if (upload){
			tgui.logInfo("I am uploading!");

			P2PServerRequestThread requestThread = new P2PServerRequestThread(incomingConnections, tgui, sharerPort,
					timeout, blockLength);
			requestThread.start();
		}
	}




}
