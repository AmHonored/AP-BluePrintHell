# Trojan Packet Feature

## Overview

The Trojan packet feature allows packets to be converted into malicious "trojan" packets when they pass through a DDoS system. This adds a new layer of complexity and strategy to the network game.

## Implementation Details

### Core Functionality

1. **Trojan Conversion**: Any packet that enters a DDoS system has a 30% chance of being converted to a trojan packet
2. **Visual Effects**: Trojan packets have distinctive visual effects including:
   - Dark red glow effect
   - Dark red stroke/border
   - Pulsing animation (opacity changes)
3. **Property Tracking**: Trojan packets maintain information about their original type and conversion time

### Key Methods Added to Packet Class

- `isTrojan()` - Check if a packet is a trojan
- `convertToTrojan()` - Convert a packet to trojan status
- `removeTrojanStatus()` - Remove trojan status from a packet
- `getOriginalType()` - Get the original packet type before trojan conversion
- `getTrojanConversionTime()` - Get the timestamp when the packet was converted

### DDoS System Integration

The DDoS system (`DdosSystem.java`) has been updated to:

1. **Automatic Conversion**: 30% probability of converting incoming packets to trojan
2. **Duplicate Prevention**: Won't convert packets that are already trojan
3. **Enhanced Logging**: Detailed console output for debugging and monitoring
4. **Property Tracking**: Adds DDoS-specific properties to trojan packets

### Visual Effects

Trojan packets are visually distinct with:
- **Glow Effect**: Dark red glow (0.8 intensity)
- **Stroke**: Dark red border (2.0 width)
- **Animation**: Continuous pulsing effect (opacity 0.7 ↔ 1.0 over 2 seconds)

## Usage Example

```java
// Create a packet
SquarePacket packet = new SquarePacket(new Point2D(100, 100));

// Check if it's a trojan (initially false)
System.out.println("Is Trojan: " + packet.isTrojan()); // false

// Convert to trojan
packet.convertToTrojan();

// Now it's a trojan
System.out.println("Is Trojan: " + packet.isTrojan()); // true
System.out.println("Original Type: " + packet.getOriginalType()); // SQUARE

// Remove trojan status
packet.removeTrojanStatus();
System.out.println("Is Trojan: " + packet.isTrojan()); // false
```

## Testing

To test the Trojan feature:

1. **Run the Game**: Use `.\run.bat` to start the game
2. **Load DDoS Test Level**: The game includes a DDoS test level (Level 3)
3. **Connect Systems**: Connect START → DDoS → END systems
4. **Observe**: Watch packets as they pass through the DDoS system
5. **Look for**: Packets with red glow and pulsing animation (trojan packets)

## Future Enhancements

The Trojan feature is designed to be extensible for future enhancements:

- **Trojan Packet Types**: Special trojan packet classes with unique behaviors
- **Infection Spread**: Trojan packets could infect other packets
- **Detection Systems**: Systems that can detect and remove trojan status
- **Advanced Effects**: More sophisticated visual and behavioral effects

## Technical Notes

- Trojan status is stored as a property in the packet's property map
- Visual effects are applied using JavaFX animations and effects
- The feature is backward compatible with existing packet types
- All trojan-related methods are thread-safe 