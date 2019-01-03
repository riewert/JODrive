package odrive;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.usb.UsbDevice;
import javax.usb.UsbHostManager;
import javax.usb.UsbHub;
import javax.usb.UsbServices;

public class ODriveUsbTest {

	public static ArrayList<short[]> POSSIBLE_ODRIVE_IDS = new ArrayList<short[]>(
			Arrays.asList(
					new short[]{4617, 3377},
					new short[]{4617, 3378},
					new short[]{4617, 3379}
					)
			);
	
	
	public static void main(String[] args) throws Exception {
		UsbServices services = UsbHostManager.getUsbServices();
		UsbHub rootHub = services.getRootUsbHub();
		
		ArrayList<UsbDevice> devices = UsbHighLevel.findDevices(rootHub, POSSIBLE_ODRIVE_IDS);
	    
//		System.out.println(devices.size()+" devices found.");
		
		ArrayList<ODrive> oDrives = new ArrayList<ODrive>();
		for (UsbDevice odriveUsbDevice : devices) {
			
//			System.out.println("Product: "+device.getProductString());
//			System.out.println("Device info: "+device);
//			System.out.println("Manufacturer: "+device.getManufacturerString());
//			System.out.println("Serial number: "+device.getSerialNumberString());
//			System.out.println();
			
		    ODrive odrive = new ODrive(odriveUsbDevice);
		    try{
		    	odrive.loadConfiguration();
		    } catch (IOException io) {
		    	odrive.readConfiguration();
			    odrive.saveConfiguration();
		    }
		    oDrives.add(odrive);
		}
	}
}
