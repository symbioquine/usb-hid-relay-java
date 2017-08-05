package net.symbioquine.usbhidrelay;

/**
 * Interface used to refer to a single switch on a relay.
 */
public interface UsbHidRelaySwitchId {
  /**
   * @return the zero-indexed id of a switch on a relay.
   */
  int id();
}
