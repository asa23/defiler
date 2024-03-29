package tests;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import virtualdisk.MyVirtualDisk;
import dfs.DFS;
import dfs.MyDFS;
import common.DFileID;

public class TestClient implements Runnable {

    private static final int NUM_WORKERS = 2;

    DFS dfiler;
    DFileID conc;
    int clientID;
    static PrintWriter writer;

    /**
     * @param args
     */

    public TestClient (DFS d, DFileID c, int client) {
        dfiler = d;
        // File to try concurrent access on
        conc = c;
        clientID = client;
        try{
        	writer = new PrintWriter("the-file-name.txt");
        }
        catch(FileNotFoundException e){
        	
        }
    }

    private void Print (String op, String mes) {
        System.out.println("Client #" + clientID + "\t Op: " + op + "\t \t "
                + mes);
    }

    private void WriteTest (DFileID f, String t) {
        byte[] data = t.getBytes();
        dfiler.write(f, data, 0, data.length);
        writer.println(new String(data));
    }

    private void WriteLong (DFileID f) {
        byte[] data = new byte[2048];
        for (int i = 0; i < 2048; i++) {
            data[i] = (byte) ('a' + (i % 26));
        }
        dfiler.write(f, data, 0, 2048);
    }

    private String ReadLong (DFileID f) {
        byte[] data = new byte[2048];
        dfiler.read(f, data, 0, 2048);
        return new String(data).trim();
    }

    private String ReadTest (DFileID f) {
        byte[] read = new byte[100];
        dfiler.read(f, read, 0, 50);
        writer.println(new String(read));
        // Print("Read bytes", Integer.toString(bytes));
        return new String(read).trim();
    }

    private String ReadTestPartial (DFileID f, int index, int count) {
        byte[] read = new byte[100];
        dfiler.read(f, read, 0, 100);
        System.out.println("100");
        dfiler.read(f, read, index, count);
        // Print("Read bytes", Integer.toString(bytes));
        return new String(read).trim();
    }

    private void extTest () {
        Print("Started", "Running");

        Print("Write INITIAL", "Concurrent " + conc.getID());
        WriteTest(conc, "INTIAL");
        Print("Read Concurrent", ReadTest(conc));
        Print("Write INITIALS", "Concurrent " + conc.getID());
        WriteTest(conc, "INTIALS");
        Print("Read Concurrent", ReadTest(conc));

        DFileID nf = dfiler.createDFile();

        Print("Created DFile", Integer.toString(nf.getID()));
        Print("Writing", "Test Two");
        WriteTest(nf, "TEST TWO");
        Print("Read", ReadTest(nf));
        
        Print("Writing", "Test Part");
        WriteTest(nf, "TEST PART");
        System.out.println("WROTE PART");
        Print("Read", ReadTestPartial(nf, 5, 4)); // Should be TEST TEST
        System.out.println("READ PART");
        
        // Test concurrent access 3 times
        for (int i = 0; i < 3; i++) {
            Print("Write SHUTDOWN " + clientID + "" + i, "Concurrent " + i);
            WriteTest(conc, "SHUT DOWN " + clientID + "" + i);
            Print("Read Concurrent " + i, ReadTest(conc));
        }

        WriteLong(nf);
        Print("Read Long", ReadLong(nf));

        WriteLong(conc);
        Print("Read Long Concurrent", ReadLong(conc));
    }

    private void concTest() {
        DFileID file = new DFileID(clientID);
        // DFileID file = dfiler.createDFile();
        WriteTest(file, "CLIENT " + clientID + 1);

        Print("Read", ReadTest(file));

    }

    @Override
    public void run () {
        // concTest();
        extTest();
        dfiler.sync();
    }

    public static void main (String[] args) throws Exception {
        System.out.println("Initializing DFS");
		MyVirtualDisk vd;
		vd = null;
		try {
			vd = new MyVirtualDisk();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Thread t = new Thread(vd);
		t.start();
		
		MyDFS dfiler = new MyDFS(vd);
        dfiler.init();
//        dfiler.createDFile();
        System.out.println("Initialized");
         DFileID file = dfiler.createDFile();
//        DFileID file = new DFileID(4);

        ArrayList<Thread> clients = new ArrayList<Thread>();
        // Run NUM_WORKERS threads
        for (int i = 0; i < NUM_WORKERS; i++) {
            TestClient tc = new TestClient(dfiler, file, i);
            Thread f = new Thread(tc);
            clients.add(f);
        }
        for(Thread f: clients){
        	f.start();
        }
        // Sync files to disk
        for (Thread tc : clients) {
            tc.join();
        }
        System.out.println("SHUTTING DOWN");
        //dfiler.shutdown();
        writer.close();
    }
}