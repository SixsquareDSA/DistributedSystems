package gr.aueb.cs.ds.worker;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import gr.aueb.cs.ds.ConfigReader;
import gr.aueb.cs.ds.network.Address;
import gr.aueb.cs.ds.network.Message;
import gr.aueb.cs.ds.network.Network;
import gr.aueb.cs.ds.network.NetworkHandler;
import gr.aueb.cs.ds.worker.database.Insert;
import gr.aueb.cs.ds.worker.map.Checkin;
import gr.aueb.cs.ds.worker.map.MapWorker;
import gr.aueb.cs.ds.worker.reduce.ReduceWorker;
import gr.aueb.cs.ds.network.Message.MessageType;

public class WorkerSpawner {

	public static void main(String[] args) {

		ArrayList<Thread> threads = new ArrayList<Thread>();
		Map<String, ArrayList<ArrayList<Checkin>>> mapper_data = new HashMap<String, ArrayList<ArrayList<Checkin>>>();

		ConfigReader conf = new ConfigReader();
		
		Scanner in = new Scanner(System.in);
		String nextLine;

		System.out.println("Give me address:ip i should listen to.");
		nextLine = in.nextLine();
		Address addr;
		try {
			addr = new Address(nextLine);
		} catch (Exception fu) {
			System.out.println("Nope. Wrong address formar");
			return;
		}
		System.out.println("Thank you.");


		try {
			ServerSocket providerSocket = new ServerSocket(addr.getPort(), 10, InetAddress.getByName(addr.getIp()));

			while (true) {
				Socket connection = providerSocket.accept();

				System.out.println("Worker Spawner: Got connection.");

//				ObjectOutputStream out = new ObjectOutputStream(connection.getOutputStream());
//				ObjectInputStream in = new ObjectInputStream(connection.getInputStream());
//				Message msg = (Message)in.readObject();

				NetworkHandler net = new NetworkHandler(connection);
				Message msg = net.readMessage();

				System.out.println("Worker Spawner: Got message.");

				switch (msg.getMsgType()) {
				case MAP:

					System.out.println("Worker Spawner: Message is MAP.");
					Address reducer = new Address(((ArrayList<String>)msg.getData()).get(6));
					ConfigReader conf2 = new ConfigReader();
					conf2.setReducer(reducer);
					Thread map = new MapWorker(net, msg, conf2);
					threads.add(map);
					map.start();
					break;
				case REDUCE:

					System.out.println("Worker Spawner: Message is REDUCE.");
					Thread reduce = new ReduceWorker(net, msg, conf, mapper_data.get(msg.getClientId()));
					threads.add(reduce);
					reduce.start();
					break;
				case MAPPER_DATA:

					System.out.println("Worker Spawner: Got data from MAPPER.");

					if (!mapper_data.containsKey(msg.getClientId())) {
						mapper_data.put(msg.getClientId(), ((ArrayList<ArrayList<Checkin>>)msg.getData()));
					} else {
						mapper_data.get(msg.getClientId()).addAll( ((ArrayList<ArrayList<Checkin>>)msg.getData()) );
					}

					net.sendMessage(new Message(msg.getClientId(), MessageType.ACK, new String("GOT DATA.")));
					net.close();
					break;
				case INSERT:
					System.out.println("Worker Spawner: Message is INSERT.");
					Thread insert = new Insert(net, msg, conf);
					threads.add(insert);
					insert.start();
					break;
				}
			}
		} catch (UnknownHostException e2) {
			System.err.println("You are trying to connect to an unknown host");
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			waitForTasksThread(threads);
		}
	}

	public static void waitForTasksThread(ArrayList<Thread> threads) {
		try {
//			threads.stream().forEach(t -> t.join());
			for (Thread t : threads) {
				t.join();
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}
