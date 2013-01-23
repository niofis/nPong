import javax.bluetooth.DataElement;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.ServiceRegistrationException;
import javax.bluetooth.UUID;

// midp/cldc API
import java.io.*;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.StreamConnectionNotifier;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Vector;
import java.util.Hashtable;

class ServidorBT implements Runnable
{
	
	/** Describes this server */
    private static final UUID BT_GAME_SERVER_UUID = 
        new UUID("F0E0D0C0B0A000908070605040302010", false);

    /** The attribute id of the record item with images names. */
    private static final int IMAGES_NAMES_ATTRIBUTE_ID = 0x4321;

    /** Keeps the local device reference. */
    private LocalDevice localDevice;

    /** Accepts new connections. */
    private StreamConnectionNotifier notifier;

    /** Keeps the information about this server. */
    private ServiceRecord record;

    /** Keeps the parent reference to process specific actions. */
    private nPongGame parent;

    /** Becomes 'true' when this component is finilized. */
    private boolean isClosed;

    /** Creates notifier and accepts clients to be processed. */
    private Thread accepterThread;

    /** Optimization: keeps the table of data elements to be published. */
    private final Hashtable dataElements = new Hashtable();

	private StreamConnection conexion;

	private InputStream in=null;
	OutputStream out=null;
	
    /**
     * Constructs the bluetooth server, but it is initialized
     * in the different thread to "avoid dead lock".
     */
    public ServidorBT(nPongGame parent)
    {
        this.parent = parent;

        // we have to initialize a system in different thread...
        accepterThread = new Thread(this);
        accepterThread.start();
    }

    /**
     * Accepts a new client and send him/her a requested image.
     */
    public void run() {
        boolean isBTReady = false;

        try {

            // create/get a local device
            localDevice = LocalDevice.getLocalDevice();

            // set we are discoverable
            if (!localDevice.setDiscoverable(DiscoveryAgent.GIAC)) {
                // Some implementations always return false, even if 
                // setDiscoverable successful
                // throw new IOException("Can't set discoverable mode...");
            }

            // prepare a URL to create a notifier
            StringBuffer url = new StringBuffer("btspp://");

            // indicate this is a server
            url.append("localhost").append(':');

            // add the UUID to identify this service
            url.append(BT_GAME_SERVER_UUID.toString());

            // add the name for our service
            url.append(";name=nPong Server");

            // request all of the client not to be authorized
            // some devices fail on authorize=true
            url.append(";authorize=false");

            // create notifier now
            notifier = (StreamConnectionNotifier) Connector.open(
                    url.toString());

            // and remember the service record for the later updates
            //record = localDevice.getRecord(notifier);

            // create a special attribute with images names
            //DataElement base = new DataElement(DataElement.DATSEQ);
            //record.setAttributeValue(IMAGES_NAMES_ATTRIBUTE_ID, base);

            // remember we've reached this point.
            isBTReady = true;
        } catch (Exception e) {
            System.err.println("Can't initialize bluetooth: " + e);
        }
        
        //parent.completeInitialization(isBTReady);

        // nothing to do if no bluetooth available
        if (!isBTReady) {
            return;
        }

        // ok, start accepting connections then
        //while (!isClosed) {
            conexion = null;

            try {
                conexion = notifier.acceptAndOpen();
                
               /* System.err.println("Conexion...");
                DataInputStream in=new DataInputStream(conexion.openInputStream());
                System.err.println("Leido " + in.readInt());
                in.close();*/
                out=conexion.openOutputStream();
            	in=conexion.openInputStream();
                parent.conexionLista();
            } catch (IOException e) {
				System.err.println(e);
                // wrong client or interrupted - continue anyway
                //continue;
            }
        //}
    }


    /**
     * Destroy a work with bluetooth - exits the accepting
     * thread and close notifier.
     */
    void destroy() {
        isClosed = true;
		
		
		
        // finilize notifier work
        if (notifier != null) {
            try {
            	in.close();
            	in=null;
            	out.close();
            	out=null;
            	conexion.close();
				conexion=null;
                notifier.close();
            } catch (IOException e) {} // ignore
        }

        // wait for acceptor thread is done
        try {
            accepterThread.join();
        } catch (InterruptedException e) {} // ignore

    }

    /**
     * Reads the image name from the specified connection
     * and sends this image through this connection, then
     * close it after all.
     */
    private void processConnection(StreamConnection conn) {

        OutputStream out = null;

        try {
            out = conn.openOutputStream();
            out.write(0);
            out.flush();
        } catch (IOException e) {
            System.err.println("Can't send image data: " + e);
        }

        // close connection and good-bye
        try {
            conn.close();
        } catch (IOException e) {} // ignore
    }
    
    public byte enviarInfo(Info info)
    {
    	if(conexion==null)
    		return 1;
    	
    	try
    	{
    		return info.writeInfo(out);
    	}
    	catch(Exception ioe)
    	{
    		System.err.println("Servidor: " + ioe);
    		return 1;
    	}	
    	
    }
    
    public byte recibirInfo(Info info)
    {
    	if(conexion==null)
    		return 1;
    	
    	try
    	{
    		return info.readInfo(in);
    	}
    	catch(Exception ioe)
    	{
    		System.err.println("Servidor: " + ioe);
    		return 1;
    	}   		
    		
    }
    
} // end of class 'BTImageServer' definition
