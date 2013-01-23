import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DataElement;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;

import java.io.*;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

public class ClienteBT implements Runnable, DiscoveryListener
{
	
	private Thread  thread_cliente;
		
	private static final UUID BT_GAME_SERVER_UUID = 
        new UUID("F0E0D0C0B0A000908070605040302010", false);
        
    /** Shows the engine is ready to work. */
    private static final int READY = 0;

    /** Shows the engine is searching bluetooth devices. */
    private static final int DEVICE_SEARCH = 1;

    /** Shows the engine is searching bluetooth services. */
    private static final int SERVICE_SEARCH = 2;

    /** Keeps the current state of engine. */
    private int state = READY;

    /** Keeps the discovery agent reference. */
    private DiscoveryAgent discoveryAgent;

    /** Keeps the parent reference to process specific actions. */
    private nPongGame parent;

    /** Becomes 'true' when this component is finilized. */
    private boolean isClosed;

    /** Proccess the search/download requests. */
    private Thread processorThread;

    /** Collects the remote devices found during a search. */
    private Vector /* RemoteDevice */ devices = new Vector();

    /** Collects the services found during a search. */
    private Vector /* ServiceRecord */ records = new Vector();

    /** Keeps the device discovery return code. */
    private int discType;

    /** Keeps the services search IDs (just to be able to cancel them). */
    private int[] searchIDs;

    /** Keeps the image name to be load. */
    private String imageNameToLoad;

    /** Keeps the table of {name, Service} to process the user choice. */
    private Hashtable base = new Hashtable();

    /** Informs the thread the download should be canceled. */
    private boolean isDownloadCanceled;

    /** Optimization: keeps service search patern. */
    private UUID[] uuidSet;

	StreamConnection conexion; 
	InputStream in=null;
	OutputStream out=null;
	
	public ClienteBT(nPongGame parent)
	{
		this.parent=parent;
		thread_cliente = new Thread(this);
        thread_cliente.start();
	}
	
	public void run()
	{
		boolean isBTReady = false;

        try
        {

            LocalDevice ld = LocalDevice.getLocalDevice();
            discoveryAgent = ld.getDiscoveryAgent();

            isBTReady = true;
        } catch (Exception e) {
            System.err.println("Bluetooth no se puede iniciar: " + e);
        }
        
        if (!isBTReady)
        {
            return;
        }

        
        uuidSet = new UUID[2];

        
        uuidSet[0] = new UUID(0x1101);

        
        uuidSet[1] = BT_GAME_SERVER_UUID;
       
        nPongServerSearch();
	}
	
    private synchronized void nPongServerSearch()
    {
        //while (!isClosed)
        //{

            // wait for new search request from user
            state = READY;

            /*try
            {
            	System.out.println("aki");
                wait();
                System.out.println("aki2");
            }
            catch (InterruptedException e)
            {
                System.err.println("Unexpected interuption: " + e);
                return;
            }*/

            // check the component is destroyed
            if (isClosed)
            {
                return;
            }

            // search for devices
            if (!searchDevices())
            {
                return;
            }
            //else if (devices.size() == 0)
            //{
                //continue;
            //}
            
            if (!searchServices())
            {
                return;
            }
            
            if(records.size()==0)
            	return;
            	
            String url=((ServiceRecord)records.elementAt(0)).getConnectionURL(
                        ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false);
            try
            {  
            	conexion = (StreamConnection) Connector.open(url);
            	/*System.out.println("Conectado...");
            	DataOutputStream out=new DataOutputStream(conn.openOutputStream());
                out.writeInt(1);
                out.flush();
            	out.close();*/
            	out=conexion.openOutputStream();
            	in=conexion.openInputStream();
            	parent.conexionLista();
        	}
        	catch(IOException ioe)
        	{
        		System.err.println(ioe);
        	}
            
            //System.err.println(url);
            
            
        //}
    }
	
	public void deviceDiscovered(RemoteDevice btDevice, DeviceClass cod)
	{
        if (devices.indexOf(btDevice) == -1)
        {
            devices.addElement(btDevice);
        }
    }
    
    public void inquiryCompleted(int discType)
    {
        this.discType = discType;

        synchronized (this)
        {
            notify();
        }
    }
    
    public void servicesDiscovered(int transID, ServiceRecord[] servRecord)
    {
        for (int i = 0; i < servRecord.length; i++)
        {
            records.addElement(servRecord[i]);
        }
    }
    
    public void serviceSearchCompleted(int transID, int respCode)
    {

        int index = -1;

        for (int i = 0; i < searchIDs.length; i++)
        {
            if (searchIDs[i] == transID)
            {
                index = i;
                break;
            }
        }

        // error - unexpected transaction index
        if (index == -1)
        {
            System.err.println("Unexpected transaction index: " + transID);

            // FIXME: process the error case
        }
        else
        {
            searchIDs[index] = -1;
        }

        /*
         * Actually, we do not care about the response code -
         * if device is not reachable or no records, etc.
         */

        // make sure it was the last transaction
        for (int i = 0; i < searchIDs.length; i++)
        {
            if (searchIDs[i] != -1)
            {
                return;
            }
        }

        // ok, all of the transactions are completed
        synchronized (this)
        {
            notify();
        }
    }
    
    void requestSearch()
    {
        synchronized (this)
        {
            notify();
        }
    }
    
    void cancelSearch()
    {
        synchronized (this)
        {
            if (state == DEVICE_SEARCH)
            {
                discoveryAgent.cancelInquiry(this);
            }
            else if (state == SERVICE_SEARCH)
            {
                for (int i = 0; i < searchIDs.length; i++)
                {
                    discoveryAgent.cancelServiceSearch(searchIDs[i]);
                }
            }
        }
    }
    
    void requestLoad(String name)
    {
        synchronized (this)
        {
            imageNameToLoad = name;
            notify();
        }
    }
        
    void destroy()
    {
        synchronized (this)
        {
        	try
        	{
				in.close();
	           	in=null;
	           	out.close();
	           	out=null;
	           	conexion.close();
				conexion=null;
			}
			catch(IOException ioe)
			{
				System.err.println(ioe);
			}
			isClosed = true;
            isDownloadCanceled = true;
            notify();
            // FIXME: implement me
        }

        // wait for acceptor thread is done
        try
        {
            processorThread.join();
        }
        catch (InterruptedException e)
        {} // ignore
    }
    
    
    private boolean searchDevices()
    {
		//System.err.println("Inicia searchDevices");
        // ok, start a new search then
        state = DEVICE_SEARCH;
        devices.removeAllElements();

        try
        {
            discoveryAgent.startInquiry(DiscoveryAgent.GIAC, this);
        }
        catch (BluetoothStateException e)
        {
            System.err.println("Can't start inquiry now: " + e);
            //parent.informSearchError("Can't start device search");
            return true;
        }

        try
        {
            wait(); // until devices are found
        }
        catch (InterruptedException e)
        {
            System.err.println("Unexpected interuption: " + e);
            return false;
        }

        // this "wake up" may be caused by 'destroy' call
        if (isClosed)
        {
            return false;
        }

        // no?, ok, let's check the return code then
        switch (discType)
        {
        case INQUIRY_ERROR:
            //parent.informSearchError("Device discovering error...");

            // fall through

        case INQUIRY_TERMINATED:

            // make sure no garbage in found devices list
            devices.removeAllElements();

            // nothing to report - go to next request
            break;

        case INQUIRY_COMPLETED:
            if (devices.size() == 0)
            {
                //parent.informSearchError("No devices in range");
            }

            // go to service search now
            break;
        default:

            // what kind of system you are?... :(
            System.err.println("system error:"
                    + " unexpected device discovery code: " + discType);
            destroy();
            return false;
        }
        return true;
    }
    
    private boolean searchServices()
    {
        state = SERVICE_SEARCH;
        records.removeAllElements();
        searchIDs = new int[devices.size()];
        boolean isSearchStarted = false;

        for (int i = 0; i < devices.size(); i++)
        {
            RemoteDevice rd = (RemoteDevice) devices.elementAt(i);

            try
            {
                searchIDs[i] = discoveryAgent.searchServices(null, uuidSet,
                        rd, this);
                System.out.println("Servicio: " + searchIDs[i]);
            }
            catch (BluetoothStateException e)
            {
                System.err.println("Can't search services for: "
                        + rd.getBluetoothAddress() + " due to " + e);
                searchIDs[i] = -1;
                continue;
            }
            isSearchStarted = true;
        }

        // at least one of the services search should be found
        if (!isSearchStarted)
        {
            //parent.informSearchError("Can't search services.");
            return true;
        }

        try
        {
                wait(); // until services are found
        }
        catch (InterruptedException e)
        {
            System.err.println("Unexpected interuption: " + e);
            return false;
        }

        // this "wake up" may be caused by 'destroy' call
        if (isClosed)
        {
            return false;
        }

        // actually, no services were found
        if (records.size() == 0)
        {
        	System.err.println("No services...");
        	return false;
            //parent.informSearchError("No proper services were found");
        }    	
        return true;
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
    		System.err.println("Cliente: " + ioe);
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
    		System.err.println("Cliente: " + ioe);
    		return 1;
    	}   		
       
   
   				
    }

}