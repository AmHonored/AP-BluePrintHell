package config.levels;

import javafx.geometry.Point2D;
import model.levels.Level;
import model.entity.ports.HexagonPort;
import model.entity.ports.PortType;
import model.entity.ports.SquarePort;
import model.entity.ports.TrianglePort;
import model.entity.systems.AntiVirusSystem;
import model.entity.systems.DDosSystem;
import model.entity.systems.DistributorSystem;
import model.entity.systems.EndSystem;
import model.entity.systems.IntermediateSystem;
import model.entity.systems.MergeSystem;
import model.entity.systems.SpySystem;
import model.entity.systems.StartSystem;
import model.entity.systems.System;

public class LevelFactory {

    public Level createLevel(LevelDefinition definition) {
        if (definition == null) throw new IllegalArgumentException("definition must not be null");

        Level level = new Level(definition.getModel().getWireLength());
        int currentCoins = level.getCoins();
        int targetCoins = definition.getModel().getInitialCoins();
        if (targetCoins > currentCoins) {
            level.addCoins(targetCoins - currentCoins);
        }

        for (SystemDefinition sysDef : definition.getSystems()) {
            System system = instantiateSystem(sysDef);
            system.setId(sysDef.getId());
            for (PortDefinition portDef : sysDef.getPorts()) {
                addPortToSystem(system, portDef);
            }
            level.addSystem(system);
        }
        return level;
    }

    private System instantiateSystem(SystemDefinition sysDef) {
        Point2D pos = new Point2D(sysDef.getPosition().getX(), sysDef.getPosition().getY());
        switch (sysDef.getType()) {
            case START: {
                StartSystem s = new StartSystem(pos);
                s.setMaxPacketsToGenerate(8);
                return s;
            }
            case INTERMEDIATE: return new IntermediateSystem(pos);
            case END: return new EndSystem(pos);
            case DDOS: return new DDosSystem(pos);
            case SPY: return new SpySystem(pos);
            case VPN: return new model.entity.systems.VPNSystem(pos);
            case DISTRIBUTOR: return new DistributorSystem(pos);
            case MERGE: return new MergeSystem(pos);
            case ANTIVIRUS: return new AntiVirusSystem(pos);
            default:
                throw new IllegalArgumentException("Unsupported system type: " + sysDef.getType());
        }
    }

    private void addPortToSystem(System system, PortDefinition portDef) {
        Point2D p = new Point2D(portDef.getPosition().getX(), portDef.getPosition().getY());
        PortType role = portDef.getRole() == PortDefinition.PortRole.INPUT ? PortType.INPUT : PortType.OUTPUT;
        switch (portDef.getShape()) {
            case SQUARE:
                system.addPort(new SquarePort(portDef.getId(), system, role, p));
                break;
            case TRIANGLE:
                system.addPort(new TrianglePort(portDef.getId(), system, role, p));
                break;
            case HEXAGON:
                system.addPort(new HexagonPort(portDef.getId(), system, role, p));
                break;
            default:
                throw new IllegalArgumentException("Unsupported port shape: " + portDef.getShape());
        }
    }
}


