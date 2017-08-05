package net.symbioquine.usbhidrelay;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

/**
 * Represents a relay size in terms of the number of switches it has.
 */
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UsbHidRelaySize {
  int size;

  public static final UsbHidRelaySize TWO = new UsbHidRelaySize(2);
  public static final UsbHidRelaySize FOUR = new UsbHidRelaySize(4);
  public static final UsbHidRelaySize EIGHT = new UsbHidRelaySize(8);
  public static final UsbHidRelaySize SIXTEEN = new UsbHidRelaySize(16);

  private UsbHidRelaySize(int size) {
    this.size = validateRelaySize(size);
  }

  public int asInt() {
    return size;
  }

  private static int validateRelaySize(int relaySize) {
    if (relaySize <= 0 || relaySize > 16 || (relaySize & (relaySize - 1)) != 0) {
      throw new IllegalArgumentException("Relay size must be a power of two between 1 and 16 (inclusive). Instead got: " + relaySize);
    }
    return relaySize;
  }
}
