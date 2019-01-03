package odrive;

import java.util.ArrayList;
import java.util.List;

import javax.usb.UsbDevice;
import javax.usb.UsbDeviceDescriptor;
import javax.usb.UsbEndpoint;
import javax.usb.UsbHub;
import javax.usb.UsbInterface;

public class UsbHighLevel {
	
	@SuppressWarnings("unchecked")
	public static ArrayList<UsbDevice> findDevices(UsbHub hub, ArrayList<short[]> possibleIds) {
		
		ArrayList<UsbDevice> devices = new ArrayList<UsbDevice>();
		
		for (UsbDevice device : (List<UsbDevice>) hub.getAttachedUsbDevices())
	    {
	    	if (device.isUsbHub()) {
	    		ArrayList<UsbDevice> temp = findDevices((UsbHub) device, possibleIds);
	    		if (temp != null) devices.addAll(temp);
	    	} else {
		        UsbDeviceDescriptor desc = device.getUsbDeviceDescriptor();
		        System.out.println("Vendor: "+desc.idVendor()+", Product: "+desc.idProduct()+".");
		        for (short[] combo : possibleIds) {
			        if (desc.idVendor() == combo[0] && desc.idProduct() == combo[1]) devices.add(device);
			        if (device.isUsbHub())
			        {
			        	ArrayList<UsbDevice> temp = findDevices((UsbHub) device, possibleIds);
			    		if (temp != null) devices.addAll(temp);
			        }
		        }
	    	}
	    }
		
	    return devices;
	}
	
	@SuppressWarnings("unchecked")
	public static UsbDevice findDevice(UsbHub hub, short vendorId, short productId)
	{
	    for (UsbDevice device : (List<UsbDevice>) hub.getAttachedUsbDevices())
	    {
	    	if (device.isUsbHub()) {
	    		UsbDevice temp = findDevice((UsbHub) device, vendorId, productId);
	    		if (temp != null) return temp;
	    	} else {
		        UsbDeviceDescriptor desc = device.getUsbDeviceDescriptor();
		        System.out.println("Vendor: "+desc.idVendor()+", Product: "+desc.idProduct()+".");
		        if (desc.idVendor() == vendorId && desc.idProduct() == productId) return device;
		        if (device.isUsbHub())
		        {
		            device = findDevice((UsbHub) device, vendorId, productId);
		            if (device != null) return device;
		        }
	    	}
	    }
	    return null;
	}
	
	@SuppressWarnings("unchecked")
	public static void listInterfaces(UsbDevice device) {
		
		System.out.println("Listing "+device.getActiveUsbConfiguration().getUsbInterfaces().size()+" interface(s).");
		for (UsbInterface usbInterface : (List<UsbInterface>) device.getActiveUsbConfiguration().getUsbInterfaces()) {
			try {
				System.out.println("InterfaceString: "+usbInterface.getInterfaceString());
				System.out.println(usbInterface.getUsbInterfaceDescriptor());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return;
	}

	@SuppressWarnings("unchecked")
	public static void listEndpoints(UsbInterface usbInterface) {
		System.out.println("Listing "+usbInterface.getUsbEndpoints().size()+" endpoints(s).");
		for (UsbEndpoint usbEndpoint : (List<UsbEndpoint>) usbInterface.getUsbEndpoints()) {
			try {
				System.out.println("Endpoint: "+usbEndpoint.toString());
				System.out.println(usbEndpoint.getUsbEndpointDescriptor());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return;
	}
}
