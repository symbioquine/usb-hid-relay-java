package net.symbioquine.usbhidrelay;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;

/**
 * Convenience wrapper for {@link UsbHidRelay} which allows specific relay switches to be
 * manipulated using enumeration instances instead of integer switch ids.
 *
 * @param <T> the enumeration type
 */
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EnumeratedUsbHidRelay<T extends Enum<T> & UsbHidRelaySwitchId> {
  UsbHidRelay hidRelay;
  List<T> switchIds;
  Map<Integer, T> switchIdsByValue;

  private EnumeratedUsbHidRelay(@NonNull UsbHidRelay hidRelay, @NonNull Class<? extends T> clazz) {
    this.hidRelay = hidRelay;
    this.switchIds = unmodifiableList(asList(clazz.getEnumConstants()));
    this.switchIdsByValue = unmodifiableMap(switchIds.stream().collect(toMap(UsbHidRelaySwitchId::id, identity())));

    if (switchIdsByValue.size() > relaySize().asInt()) {
      throw new IllegalArgumentException(
          String.format("Expected the enum values be to be less than or equal to %s. Instead found %s values in %s", relaySize(),
              switchIdsByValue.size(), clazz));
    }

    switchIdsByValue.forEach((switchIdValue, switchId) -> {
      if (switchIdValue < 0 || switchIdValue >= relaySize().asInt()) {
        throw new IllegalArgumentException(String.format(
            "Expected all entries in enum to have a id value greater than or equal to zero and less than %s. Instead got a value of %s for %s.",
            relaySize(), switchIdValue, switchId));
      }
    });

  }

  /**
   * Create an instance of {@link EnumeratedUsbHidRelay} wrapping a specified instance of
   * {@link UsbHidRelay}.
   * 
   * @param hidRelay an instance of {@link UsbHidRelay} to be wrapped
   * @param clazz a reference to an enumeration implementing the {@link UsbHidRelaySwitchId} interface
   *        which will be used to refer to specific relay switches.
   * @return an instance of {@link EnumeratedUsbHidRelay} wrapping a specified instance of
   *         {@link UsbHidRelay}.
   */
  public static <T extends Enum<T> & UsbHidRelaySwitchId> EnumeratedUsbHidRelay<T> create(@NonNull UsbHidRelay hidRelay,
      @NonNull Class<? extends T> clazz) {
    return new EnumeratedUsbHidRelay<T>(hidRelay, clazz);
  }

  /**
   * Create an instance of {@link EnumeratedUsbHidRelay} by acquiring the first {@link UsbHidRelay}
   * and wrapping it.
   * 
   * @param relaySize the size of the relay which is expected to be found.
   * @param clazz a reference to an enumeration implementing the {@link UsbHidRelaySwitchId} interface
   *        which will be used to refer to specific relay switches.
   * @return an instance of {@link EnumeratedUsbHidRelay} wrapping the acquired {@link UsbHidRelay}.
   */
  public static <T extends Enum<T> & UsbHidRelaySwitchId> EnumeratedUsbHidRelay<T> findAndAcquireFirstRelay(@NonNull UsbHidRelaySize relaySize,
      Class<? extends T> clazz) {
    return new EnumeratedUsbHidRelay<T>(UsbHidRelay.findAndAcquireFirstRelay(relaySize), clazz);
  }

  /**
   * @return an object representing the number of switches on the relay.
   */
  public UsbHidRelaySize relaySize() {
    return hidRelay.relaySize();
  }

  /**
   * @return a map of the enumerated relay switches to their respective states where true indicates
   *         a given switch is on (circuit closed) and false indicates a given switch is off
   *         (circuit open).
   */
  public Map<T, Boolean> getSwitchStates() {
    List<Boolean> switchStates = hidRelay.getSwitchStates();

    return switchIds.stream().collect(toMap(identity(), switchId -> switchStates.get(switchId.id())));
  }

  /**
   * @param switchId the id of the switch to get the state of.
   * @return the state of the switch where true indicates the switch is on (circuit closed) and
   *         false indicates the switch is off (circuit open).
   */
  public boolean getSwitchState(@NonNull T switchId) {
    return getSwitchStates().get(switchId);
  }

  /**
   * Reset the HID interface. Note this does not necessarily change the states of any switch on the
   * relay.
   */
  public void reset() {
    hidRelay.reset();
  }

  /**
   * Set the states of multiple switches simultaneously. Switches which are already in a specified
   * states will not be changed. Unspecified switches will not be changed.
   * 
   * @param switchStates a map of the desired switch states where true indicates the switch should
   *        be on (circuit closed) and false indicates the switch should be off (circuit open).
   */
  public void setSwitchStates(@NonNull Map<T, Boolean> switchStates) {
    List<Boolean> currentSwitchStates = hidRelay.getSwitchStates();

    List<Boolean> newSwitchStates = IntStream.range(0, currentSwitchStates.size()).mapToObj(switchIdValue -> {
      Optional<T> switchId = Optional.ofNullable(switchIdsByValue.get(switchIdValue));

      Optional<Boolean> newSwitchState = switchId.map(switchStates::get);

      return newSwitchState.orElseGet(() -> currentSwitchStates.get(switchIdValue));
    }).collect(toList());

    hidRelay.setSwitchStates(newSwitchStates);
  }

  /**
   * Set the state of a single switch. If the switch is already in the specified state it will not
   * be changed.
   * 
   * @param switchId the id of the switch to set the state of.
   * @param state the desired switch state where true indicates the switch should be on (circuit
   *        closed) and false indicates the switch should be off (circuit open).
   */
  public void setSwitchState(T switchId, boolean state) {
    hidRelay.setSwitchState(switchId.id(), state);
  }

  /**
   * Set the state of a single switch to on (circuit closed). If the switch is already on it will
   * not be changed.
   * 
   * @param switchId the id of the switch to be turned on.
   */
  public void setSwitchOn(T switchId) {
    setSwitchState(switchId, true);
  }

  /**
   * Set the state of a single switch to off (circuit open). If the switch is already off it will
   * not be changed.
   * 
   * @param switchId the id of the switch to be turned off.
   */
  public void setSwitchOff(T switchId) {
    setSwitchState(switchId, false);
  }

  /**
   * Set the states of all the switches in the enumeration to on (circuit closed). Switches which
   * are already in a specified states will not be changed. Switches not represented in the
   * enumeration will not be changed.
   */
  public void setAllSwitchesOn() {
    setSwitchStates(switchIds.stream().collect(toMap(identity(), s -> true)));
  }

  /**
   * Set the states of all the switches in the enumeration to off (circuit open). Switches which are
   * already in a specified states will not be changed. Switches not represented in the enumeration
   * will not be changed.
   */
  public void setAllSwitchesOff() {
    setSwitchStates(switchIds.stream().collect(toMap(identity(), s -> false)));
  }
}
