package net.symbioquine.usbhidrelay;

import static java.util.stream.Collectors.toList;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.usb.UsbConfiguration;
import javax.usb.UsbDevice;
import javax.usb.UsbDeviceDescriptor;
import javax.usb.UsbDisconnectedException;
import javax.usb.UsbException;
import javax.usb.UsbHostManager;
import javax.usb.UsbHub;
import javax.usb.UsbInterface;
import javax.usb.UsbNotActiveException;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;

/**
 * Controls USB HID relays.
 */
@Accessors(fluent = true)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UsbHidRelay {

  private static final int CMD_READ = 0xD2;
  private static final int CMD_WRITE = 0xC3;
  private static final int CMD_RESET = 0x71;

  private static final int CMD_DATA_LEN = 14;

  private static final int VENDOR_ID = 0x416;
  private static final int PRODUCT_ID = 0x5020;

  private static final int ENDPOINT_IN_ADDRESS = 0x84;
  private static final int ENDPOINT_OUT_ADDRESS = 0x05;

  private static final int PACKET_LENGTH = 64;

  private static final int[] RELAY_BITMAP = {128, 256, 64, 512, 32, 1024, 16, 2048, 8, 4096, 4, 8192, 2, 16384, 1, 32768};

  UncheckedUsbInterface usbInterface;
  @Getter
  UsbHidRelaySize relaySize;

  private UsbHidRelay(@NonNull UsbInterface iface, @NonNull UsbHidRelaySize relaySize) {
    this.usbInterface = new UncheckedUsbInterface(iface);
    this.relaySize = relaySize;
  }

  /**
   * @return a {@link UsbHidRelay} instance wrapping the specified {@link UsbInterface} with the
   *         given size.
   */
  public static UsbHidRelay create(@NonNull UsbInterface usbInterface, @NonNull UsbHidRelaySize relaySize) {
    return new UsbHidRelay(usbInterface, relaySize);
  }

  /**
   * @return a {@link UsbHidRelay} instance with the given size wrapping the first USB HID relay
   *         device.
   */
  public static UsbHidRelay findAndAcquireFirstRelay(@NonNull UsbHidRelaySize relaySize) {
    Optional<UsbDevice> usbDevice;
    try {
      usbDevice = findDevice(UsbHostManager.getUsbServices().getRootUsbHub(), VENDOR_ID, PRODUCT_ID);
    } catch (SecurityException | UsbException e) {
      throw new RuntimeException("Failed to get relay usb device.", e);
    }

    if (!usbDevice.isPresent()) {
      throw new RuntimeException("Could not get relay usb device.");
    }

    UsbConfiguration configuration = usbDevice.get().getUsbConfiguration((byte) 1);
    Optional<UsbInterface> usbInterface = Optional.ofNullable(configuration.getUsbInterface((byte) 0));

    if (!usbInterface.isPresent()) {
      throw new RuntimeException("Could not get relay usb interface.");
    }

    try {
      usbInterface.get().claim(iface -> true);
    } catch (UsbNotActiveException | UsbDisconnectedException | UsbException e) {
      throw new RuntimeException("Failed to claim relay usb device.", e);
    }

    return new UsbHidRelay(usbInterface.get(), relaySize);
  }

  /**
   * @return a list of the states of all the switches on the relay. The states are in order of the
   *         switch id. The list will be same length as the number of relay switches.
   */
  public List<Boolean> getSwitchStates() {
    sendCommand(command(CMD_READ, 0x1111, 0x1111, 0x1111, 0x1111));

    ByteBuffer buffer = ByteBuffer.wrap(readResponse());

    buffer.order(ByteOrder.LITTLE_ENDIAN);

    int mask = buffer.getInt(2);

    List<Boolean> switchStates = new ArrayList<>(relaySize.asInt());

    IntStream.range(0, relaySize.asInt()).forEach(idx -> {
      int bitmap = RELAY_BITMAP[idx];

      switchStates.add((mask & bitmap) != 0);
    });

    return switchStates;
  }

  /**
   * Get the state of a switch on the relay by zero-indexed id.
   * 
   * @param relayId the zero-indexed switch id to get the state of.
   * @return the state of the switch where true indicates the switch is on (circuit closed) and
   *         false indicates the switch is off (circuit open).
   */
  public boolean getSwitchState(int relayId) {
    return getSwitchStates().get(relayId);
  }

  /**
   * Reset the HID interface. Note this does not necessarily change the states of any switch on the
   * relay.
   */
  public void reset() {
    sendCommand(command(CMD_RESET, CMD_RESET, 0x00, 0x1111, 0x00));
  }

  /**
   * Set the states of all the relay switches.
   * 
   * @param switchStates a list of the desired switch states in the order of the switch id.
   * @throws IllegalArgumentException if the number of switch states does not match the number of
   *         switches on the relay.
   */
  public void setSwitchStates(List<Boolean> switchStates) {
    if (switchStates.size() != relaySize.asInt()) {
      throw new IllegalArgumentException("Number of switch states must match relay size.");
    }

    int mask = 0;
    for (int switchId = 0; switchId < relaySize.asInt(); switchId++) {
      if (switchStates.get(switchId)) {
        mask |= (1 << switchId);
      }
    }

    writeMask(mask);
  }

  /**
   * Set the state of a relay switch by id. If the switch is already in the specified state it will
   * not be changed.
   *
   * @param switchId the zero-indexed id of the switch to set the state of.
   * @param state the desired state of the switch where true indicates the switch should be on
   *        (circuit closed) and false indicates the switch should be off (circuit open).
   * @throws IllegalArgumentException if the switch id is less than zero or greater than the size of
   *         the relay minus one.
   */
  public void setSwitchState(int switchId, boolean state) {
    if (switchId < 0 || switchId >= relaySize.asInt()) {
      throw new IllegalArgumentException("Switch id must be in the range 0 to " + (relaySize.asInt() - 1) + " instead got: " + switchId);
    }

    List<Boolean> switchStates = getSwitchStates();

    switchStates.set(switchId, state);

    setSwitchStates(switchStates);
  }

  /**
   * Set the state of a single switch to on (circuit closed). If the switch is already on it will
   * not be changed.
   * 
   * @param switchId the zero-indexed id of the switch to be turned on.
   * @throws IllegalArgumentException if the switch id is less than zero or greater than the size of
   *         the relay minus one.
   */
  public void setSwitchOn(int switchId) {
    setSwitchState(switchId, true);
  }

  /**
   * Set the state of a single switch to off (circuit open). If the switch is already on it will not
   * be changed.
   * 
   * @param switchId the zero-indexed id of the switch to be turned off.
   * @throws IllegalArgumentException if the switch id is less than zero or greater than the size of
   *         the relay minus one.
   */
  public void setSwitchOff(int switchId) {
    setSwitchState(switchId, false);
  }

  /**
   * Set the states of all switches on the relay to on (circuit closed). Any switches which are
   * already on will not be changed.
   */
  public void setAllSwitchesOn() {
    setSwitchStates(IntStream.range(0, relaySize.asInt()).mapToObj(i -> true).collect(toList()));
  }

  /**
   * Set the states of all switches on the relay to off (circuit open). Any switches which are
   * already off will not be changed.
   */
  public void setAllSwitchesOff() {
    setSwitchStates(IntStream.range(0, relaySize.asInt()).mapToObj(i -> false).collect(toList()));
  }

  private void writeMask(int mask) {
    sendCommand(command(CMD_WRITE, mask, 0x00, 0x00, 0x00));
  }

  private byte[] readResponse() {
    byte[] data = new byte[PACKET_LENGTH];

    try (AutoCloseableUncheckedUsbPipe pipe = usbInterface.openPipeForEndpoint(ENDPOINT_IN_ADDRESS)) {
      pipe.syncSubmit(data);
    }
    return data;
  }

  private void sendCommand(byte[] command) {
    try (AutoCloseableUncheckedUsbPipe pipe = usbInterface.openPipeForEndpoint(ENDPOINT_OUT_ADDRESS)) {
      pipe.syncSubmit(command);
    }
  }

  private byte[] command(int cmd, int... data) {
    assert data.length == 4;

    ByteBuffer buffer = ByteBuffer.allocate(PACKET_LENGTH);

    buffer.order(ByteOrder.LITTLE_ENDIAN);

    buffer.put((byte) cmd);
    buffer.put((byte) CMD_DATA_LEN);

    buffer.putShort((short) data[0]);
    buffer.putShort((short) data[1]);
    buffer.putShort((short) data[2]);
    buffer.putShort((short) data[3]);

    buffer.put((byte) 'H');
    buffer.put((byte) 'I');
    buffer.put((byte) 'D');
    buffer.put((byte) 'C');

    buffer.rewind();

    int checksum = IntStream.range(0, CMD_DATA_LEN).map(i -> 0xFF & buffer.get()).sum();

    buffer.putInt(checksum);

    return buffer.array();
  }

  private static Optional<UsbDevice> findDevice(UsbHub hub, int vendorId, int productId) {
    return toDevicesStream(hub).filter(device -> {
      UsbDeviceDescriptor desc = device.getUsbDeviceDescriptor();

      return desc.idVendor() == vendorId && desc.idProduct() == productId;
    }).findFirst();
  }

  private static Stream<UsbDevice> toDevicesStream(UsbHub hub) {
    @SuppressWarnings("unchecked")
    List<UsbDevice> usbDevices = (List<UsbDevice>) hub.getAttachedUsbDevices();

    return usbDevices.stream().flatMap(device -> {
      if (device.isUsbHub()) {
        return toDevicesStream((UsbHub) device);
      }
      return Stream.of(device);
    });
  }
}
