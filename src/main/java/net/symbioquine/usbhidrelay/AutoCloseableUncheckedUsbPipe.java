package net.symbioquine.usbhidrelay;

import static java.util.Objects.requireNonNull;

import javax.usb.UsbDisconnectedException;
import javax.usb.UsbException;
import javax.usb.UsbNotActiveException;
import javax.usb.UsbNotOpenException;
import javax.usb.UsbPipe;

/**
 * Wrap a {@link UsbPipe} to make it {@link AutoCloseable} and avoid checked exceptions.
 */
class AutoCloseableUncheckedUsbPipe implements AutoCloseable {
  private final UsbPipe pipe;

  public AutoCloseableUncheckedUsbPipe(UsbPipe pipe) {
    this.pipe = requireNonNull(pipe);
  }

  public void syncSubmit(byte[] data) {
    try {
      int result = pipe.syncSubmit(data);
      if (result != data.length) {
        throw new RuntimeException("Failed to read or write data on pipe. Expected " + data.length + " bytes to be transferred. Instead "
            + result + " were transferred.");
      }
    } catch (UsbNotActiveException | UsbNotOpenException | IllegalArgumentException | UsbDisconnectedException | UsbException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void close() {
    try {
      pipe.close();
    } catch (UsbNotActiveException | UsbNotOpenException | IllegalArgumentException | UsbDisconnectedException | UsbException e) {
      throw new RuntimeException(e);
    }
  }
}
