## Introduction

usb-hid-relay-java is a simple API for controlling USB HID relays. (Currently only tested with the SainSmart 16-channel USB-HID programmable relay.)

The following projects were used as a reference for understanding the protocol;
* https://github.com/mvines/relay
* https://github.com/tatobari/hidrelaypy

## Getting started

### Dependency

* Integrate with JitPack; https://jitpack.io/
* Add dependency;

pom.xml;
```xml
<dependencies>
    <dependency>
        <groupId>com.github.symbioquine</groupId>
        <artifactId>usb-hid-relay-java</artifactId>
        <version>v1.0.0</version>
    </dependency>
</dependencies>
```

build.gradle;
```
dependencies {
    compile 'com.github.symbioquine:usb-hid-relay-java:v1.0.0'
}
```

### Simple use

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