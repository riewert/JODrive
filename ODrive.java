package odrive;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

import javax.usb.UsbDevice;
import javax.usb.UsbEndpoint;
import javax.usb.UsbInterface;
import javax.usb.UsbPipe;

import org.json.JSONArray;

public class ODrive {
	
	private static short MESSAGE_LENGTH = Short.MAX_VALUE;
	private static short PROTOCOL_VERSION = 1;
	
	private HashMap<Byte, UsbEndpoint> endPoints = new HashMap<Byte, UsbEndpoint>();
	private short sequenceNumber = 129;
	private short endPointAddress = (short) 32768;
	private ByteBuffer twoByteBuffer = ByteBuffer.allocate(2);
	private ByteBuffer fourByteBuffer = ByteBuffer.allocate(4);
	private String configLocation = "ODRIVE_CONFIG_";
	private JSONArray configuration = null;
	
	public ODrive(UsbDevice device) throws Exception {
		twoByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		fourByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		configLocation = configLocation+device.getSerialNumberString()+".cfg";
		
		// Get the first USB interface
	    //UsbHighLevel.listInterfaces(device);
	    UsbInterface usbInterface = (UsbInterface) device.getActiveUsbConfiguration().getUsbInterfaces().get(2);
	    if (usbInterface==null) {
	    	throw new Exception("No interface found on device.");
	    } else {
	    	//System.out.println("Device interface found.\n");
	    }	    
	    //System.out.println(usbInterface.getUsbInterfaceDescriptor());
	    
	    usbInterface.claim();

	    // Get the first endpoint of the interface
	    //UsbHighLevel.listEndpoints(usbInterface);
	    
	    HashMap<Byte, UsbEndpoint> endPoints = new HashMap<Byte, UsbEndpoint>();
	    @SuppressWarnings("unchecked")
		Iterator<UsbEndpoint> it = usbInterface.getUsbEndpoints().iterator();
	    while (it.hasNext()) {
	    	UsbEndpoint endPoint = it.next();
	    	endPoints.put(endPoint.getDirection(), endPoint);
	    }
	    
	    if (endPoints.size() != 2) 
			throw new Exception("Two endpoints expected");
		
		this.endPoints = endPoints;
	}
	
	private void addConfigurationPrefix(ByteBuffer message) {
		message.put(twoByteBuffer.putShort(sequenceNumber).array());
		twoByteBuffer.clear();
		message.put(twoByteBuffer.putShort(endPointAddress).array());
		twoByteBuffer.clear();
		//message.put(endPoints.get(javax.usb.UsbConst.ENDPOINT_DIRECTION_OUT).getUsbEndpointDescriptor().bEndpointAddress());
		//message.put((byte) 0x00);
		message.put(twoByteBuffer.putShort(MESSAGE_LENGTH).array());
		twoByteBuffer.clear();
		sequenceNumber++;
	}
	
	private void addConfigurationSuffix(ByteBuffer message) {
		message.put(twoByteBuffer.putShort(PROTOCOL_VERSION).array());
		twoByteBuffer.clear();
	}
	
	public void readConfiguration() throws Exception {
		ByteBuffer message = ByteBuffer.allocate(12);
		
		addConfigurationPrefix(message);
		
		// Configuration request
		message.put((byte) 0x00);
		message.put((byte) 0x00);
		message.put((byte) 0x00);
		message.put((byte) 0x00);
		
		addConfigurationSuffix(message);
		
		//System.out.println(bytesToHex(message.array()));
		
		syncWritePacket(endPoints.get(javax.usb.UsbConst.ENDPOINT_DIRECTION_OUT), message.array());
		
		ByteBuffer configuration = ByteBuffer.allocate(MESSAGE_LENGTH);
		byte[] buffer = null;
		while (true) {
			buffer = syncReadPacket(endPoints.get(javax.usb.UsbConst.ENDPOINT_DIRECTION_IN));
			if (buffer == null || containsOnlyZeroes(buffer))
				break;
			configuration.put(buffer);
			
			message.clear();
			addConfigurationPrefix(message);
			
			message.put(fourByteBuffer.putInt(configuration.position()).array());
			fourByteBuffer.clear();
			
			addConfigurationSuffix(message);
			
			syncWritePacket(endPoints.get(javax.usb.UsbConst.ENDPOINT_DIRECTION_OUT), message.array());
		}
		
		configuration.position(0);
		byte[] trimmedMessage = trimZeroesOffMessage(configuration);
		String messageString = new String(trimmedMessage, 0, trimmedMessage.length, "ASCII");
		System.out.println("Configuration: "+messageString);
		
		//CharBuffer stringBuff = ByteBuffer.wrap(trimmedMessage).asCharBuffer();
		this.configuration = new JSONArray(messageString);
		
	}
	
	public void saveConfiguration() throws IOException {
		FileWriter fileWriter = new FileWriter(configLocation);
	    PrintWriter printWriter = new PrintWriter(fileWriter);
	    printWriter.print(configuration.toString());
	    printWriter.close();
	}
	
	public void loadConfiguration() throws IOException {
		FileReader fileReader = new FileReader(configLocation);
		CharBuffer buffer = CharBuffer.allocate(MESSAGE_LENGTH);
		fileReader.read(buffer);
		fileReader.close();
		String messageString = new String(buffer.array());
		
		this.configuration = new JSONArray(messageString);
		System.out.println("Configuration: "+messageString);
	}
	
	private static byte[] syncReadPacket(UsbEndpoint ep) throws Exception{
		UsbPipe pipe = ep.getUsbPipe();
		int size = ep.getUsbEndpointDescriptor().wMaxPacketSize() / 2;
		byte[] buffer = new byte[size];
		pipe.open();
		pipe.syncSubmit(buffer);
		pipe.close();
		//System.out.println("IN: "+bytesToHex(buffer));
		return Arrays.copyOfRange(buffer, 2, size);
	}

	private static void syncWritePacket(UsbEndpoint ep, byte[] message) throws Exception{
		UsbPipe pipe = ep.getUsbPipe();
		pipe.open();
		pipe.syncSubmit(message);
		pipe.close();
		//System.out.println("OUT: "+bytesToHex(message));
	}

	private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

	private static String bytesToHex(byte[] bytes) {
	    char[] hexChars = new char[bytes.length * 2];
	    for ( int j = 0; j < bytes.length; j++ ) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	}
	
	private boolean containsOnlyZeroes(final byte[] array) {
        int sum = 0;
        for (byte b : array) {
            sum |= b;
        }
        return (sum == 0);
    }
	
	private byte[] trimZeroesOffMessage(ByteBuffer byteBuffer) {
		
		for (int i = byteBuffer.limit(); i > 0; i--)
			if (byteBuffer.get(i-1) != 0)
				return Arrays.copyOfRange(byteBuffer.array(), 0, i);
		return null;
	}
}
