## Introduction

usb-hid-relay-java is a simple API for controlling USB HID relays. (Currently only tested with the SainSmart 16-channel USB-HID programmable relay.)

The following projects were used as a reference for understanding the protocol;
* https://github.com/mvines/relay
* https://github.com/tatobari/hidrelaypy

## Getting started

```Java
import net.symbioquine.usbhidrelay.UsbHidRelay;
import static net.symbioquine.usbhidrelay.UsbHidRelaySize.SIXTEEN;
```

```Java
UsbHidRelay relay = UsbHidRelay.findAndAcquireFirstRelay(SIXTEEN);

relay.setSwitchOn(0);
```

### Enumerated switches

```Java
import net.symbioquine.usbhidrelay.EnumeratedUsbHidRelay;
import net.symbioquine.usbhidrelay.UsbHidRelaySwitchId;
```

```Java
public enum AwesomeMovieRelaySwitch implements UsbHidRelaySwitchId {
    LIGHTS(0),
    CAMERA(10),
    ACTION(3);

    private final int switchId;

    MyProjectRelaySwitch(int switchId) {
        this.switchId = switchId;
    }

    @Override
    int id() {
        return switchId;
    }
}
```

```Java
import static AwesomeMovieRelaySwitch.*;
```

```Java
EnumeratedUsbHidRelay<AwesomeMovieRelaySwitch> relay = EnumeratedUsbHidRelay.findAndAcquireFirstRelay(SIXTEEN, AwesomeMovieRelaySwitch.class);

relay.setSwitchOn(LIGHTS);
relay.setSwitchOn(CAMERA);
relay.setSwitchOn(ACTION);
```