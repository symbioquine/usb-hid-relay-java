package net.symbioquine.usbhidrelay;

import static java.util.Objects.requireNonNull;

import javax.usb.UsbDisconnectedException;
import javax.usb.UsbEndpoint;
import javax.usb.UsbException;
import javax.usb.UsbInterface;
import javax.usb.UsbNotActiveException;
import javax.usb.UsbNotClaimedException;
import javax.usb.UsbPipe;

/**
 * Wrap a {@link UsbInterface} to make it avoid checked exceptions.
 */
class UncheckedUsbInterface {
  private final UsbInterface usbInterface;

  public UncheckedUsbInterface(UsbInterface usbInterface) {
    this.usbInterface = requireNonNull(usbInterface);
  }

  public AutoCloseableUncheckedUsbPipe openPipeForEndpoint(int endpointAddress) {
    UsbEndpoint endpoint = usbInterface.getUsbEndpoint((byte) endpointAddress);
    UsbPipe pipe = endpoint.getUsbPipe();
    try {
      pipe.open();
    } catch (UsbNotActiveException | UsbNotClaimedException | UsbDisconnectedException | UsbException e) {
      throw new RuntimeException("Failed to open pipe for reading.", e);
    }
    return new AutoCloseableUncheckedUsbPipe(pipe);
  }
}
