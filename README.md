# Network Game

A JavaFX game where you manage a network of systems and packets.

## Requirements

- Java 17 or higher
- Maven

## Running the Game

### Windows
Double-click on the `run.bat` file or run the following command in the terminal:
```
mvn clean javafx:run
```

### Linux/Mac
Run the following command in the terminal:
```
mvn clean javafx:run
```

## Game Controls

- **Arrow Keys (Left/Right)**: Control temporal progress when in temporal control mode
- **Space**: Toggle play/pause
- **Tab**: Toggle temporal control mode
- **Escape**: Return to main menu
- **S**: Open shop

## Sound Files

Place sound files in the `src/main/resources/sounds` directory:
- background_music.mp3
- connection_success.mp3
- level_complete.mp3
- packet_damage.mp3
- button_click.mp3
- shop_purchase.mp3

If these files are not present, the game will run without sounds.

## Game Structure

- **Main Menu**: Start game, level selection, settings, and exit
- **Game Screen**: Network systems, packets, and connections
- **Shop**: Purchase abilities to help manage the network
- **Settings**: Adjust volume

## Project Structure

- **Model**: Game state, packets, systems, connections
- **View**: UI scenes (main menu, game, settings, shop)
- **Controller**: Game logic and user interaction 