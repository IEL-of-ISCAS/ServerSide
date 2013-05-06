/*
 * Copyright (C) 2013 Void Main Studio 
 * Project:ServerSide
 * Author: voidmain
 * Create Date: Apr 16, 20134:45:36 PM
 */
package me.voidmain.iel.socket_test;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Sample server for socket communication
 * 
 * @Project ServerSide
 * @Package me.voidmain.iel.socket_test
 * @Class TestServer
 * @Date Apr 16, 2013 4:45:36 PM
 * @author voidmain
 */
public class TestServer {

	public static ConcurrentLinkedQueue<QueueData> sForwardQueue;
	public static ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> sResponseQueue;
	public static ForwardThread sForwardThread;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		sForwardQueue = new ConcurrentLinkedQueue<QueueData>();
		sResponseQueue = new ConcurrentHashMap<String, ConcurrentLinkedQueue<String>>();

		Thread client = new PhoneThread();
		Thread computer = new ComputerThread();
		Thread computerReceive = new ComputerReceiveThread();

		client.start();
		computer.start();
		computerReceive.start();

		try {
			client.join();
			computer.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}
}

class ComputerThread extends Thread {
	@Override
	public void run() {
		ServerSocket serverSocket;
		try {
			serverSocket = new ServerSocket(7777);
			System.out.println("服务已启动,绑定7777端口!");

			while (true) {
				Socket socket = serverSocket.accept();
				if(TestServer.sForwardThread != null) {
					TestServer.sForwardThread = null;
				}
				System.out.println("Sender socket connected!");
				socket.setKeepAlive(true);
				socket.setTcpNoDelay(true);
				TestServer.sForwardThread = new ForwardThread(socket);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

class ComputerReceiveThread extends Thread {
	@Override
	public void run() {
		ServerSocket serverSocket;
		try {
			serverSocket = new ServerSocket(8888);
			System.out.println("服务已启动,绑定8888端口!");

			while (true) {
				Socket socket = serverSocket.accept();
				System.out.println("Computer Receive CONNETEDDDD!");
				while (TestServer.sForwardThread == null || TestServer.sForwardThread.isAlive()) {
					Thread.sleep(1000);
				}
				TestServer.sForwardThread.setReceiveSocket(socket);
				TestServer.sForwardThread.start();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}

class PhoneThread extends Thread {
	@Override
	public void run() {
		ServerSocket serverSocket;
		try {
			serverSocket = new ServerSocket(6666);
			System.out.println("服务已启动,绑定6666端口!");

			while (true) {
				Socket socket = serverSocket.accept();
				System.out.println("MOBILE CONNETEDDDD!");
				socket.setKeepAlive(true);
				socket.setTcpNoDelay(true);
				ServerThread st = new ServerThread(socket);
				st.start();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

class ForwardThread extends Thread {
	Socket mSendSocket;
	Socket mReceiveSocket;

	public ForwardThread(Socket socket) {
		this.mSendSocket = socket;
	}

	public void setReceiveSocket(Socket socket) {
		mReceiveSocket = socket;
	}

	public void run() {
		try {
			DataOutputStream out = new DataOutputStream(
					mSendSocket.getOutputStream());
			DataInputStream in = new DataInputStream(
					mReceiveSocket.getInputStream());

			QueueData data = null;
			while (true) {
				data = TestServer.sForwardQueue.poll();
				if (data == null)
					continue;
				String uid = data.uid;
				if (data.content != null) {
					out.write(data.content.getBytes());
					out.flush();
				}

				System.out.println("SENT   " + data.content + "   " + data.uid);

				String resp = in.readLine();
				System.out.println("resp   " + resp);
				ConcurrentLinkedQueue<String> queue = TestServer.sResponseQueue
						.get(uid);
				if (queue == null) {
					queue = new ConcurrentLinkedQueue<String>();
				}
				queue.add(resp);

				TestServer.sResponseQueue.put(uid, queue);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				mSendSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}

class ServerThread extends Thread {
	Socket socket;
	String uid;

	public ServerThread(Socket socket) {
		this.socket = socket;
		uid = UUID.randomUUID().toString();
	}

	public void run() {
		try {
			DataInputStream in = new DataInputStream(socket.getInputStream());
			DataOutputStream out = new DataOutputStream(
					socket.getOutputStream());

			String content = "";
			while (true) {
				content = in.readUTF();
				if (content != null) {
					System.out.println(content);
					System.out.println(TestServer.sForwardThread != null);
					System.out.println(TestServer.sForwardThread != null
							&& TestServer.sForwardThread.isAlive());
					if (TestServer.sForwardThread != null
							&& TestServer.sForwardThread.isAlive()) {
						QueueData data = new QueueData();
						data.content = content;
						data.uid = uid;
						TestServer.sForwardQueue.add(data);

						System.out
								.println("waiting for response   " + data.uid);

						while (true) {
							ConcurrentLinkedQueue<String> responQueue = TestServer.sResponseQueue
									.get(uid);
							if (responQueue != null
									&& responQueue.peek() != null) {
								String resp = responQueue.poll();
								System.out.println("asdasdfas   " + resp);
								out.writeUTF(resp);
								out.flush();
								break;
							}
						}

					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}

class QueueData {
	public String uid;
	public String content;
}