package manager.packets;

import controller.PacketController;
import javafx.scene.layout.Pane;
import model.entity.packets.MassivePacket;
import model.entity.packets.Packet;
import model.entity.ports.Port;
import model.wire.Wire;
import view.components.ports.HexagonPortView;
import view.components.ports.PortView;
import view.components.ports.SquarePortView;
import view.components.ports.TrianglePortView;
import view.components.systems.SystemView;

public final class MassivePacketManager {
	private MassivePacketManager() {}

	public static void changeInputPort(Wire wire, PacketController packetController) {
		if (wire == null || wire.getDest() == null) return;
		if (wire.getDest().getType() != model.entity.ports.PortType.INPUT) return;
		java.util.Random rng = new java.util.Random();
		double r = rng.nextDouble();
		model.entity.ports.Port.ShapeKind newKind;
		if (r < 1.0 / 3.0) {
			newKind = model.entity.ports.Port.ShapeKind.SQUARE;
		} else if (r < 2.0 / 3.0) {
			newKind = model.entity.ports.Port.ShapeKind.TRIANGLE;
		} else {
			newKind = model.entity.ports.Port.ShapeKind.HEXAGON;
		}
		wire.getDest().setShapeKind(newKind);
		try {
			changeInputPortView(wire.getDest(), packetController);
		} catch (Throwable ignored) {}
	}

	public static void onComplete(Wire wire, Packet packet, PacketController packetController) {
		if (!(packet instanceof MassivePacket)) return;
		if (wire == null) return;
		wire.incrementMassivePacketRunCount();
		if (wire.hasReachedMassiveRunLimit()) {
			wire.detachAndDeactivate();
			try {
				view.components.wires.WireView.markDisabled(wire);
			} catch (Throwable ignored) {}
			try {
				wireDeattachedIndicator(wire, packetController);
			} catch (Throwable ignored) {}
		}
	}

	private static void changeInputPortView(Port inputPort, PacketController packetController) {
		if (packetController == null || inputPort == null) return;
		try {
			Pane pane = packetController.getPacketLayer();
			if (pane == null) return;

			PortView oldView = null;
			SystemView targetSystemView = null;
			for (javafx.scene.Node node : new java.util.ArrayList<>(pane.getChildren())) {
				if (node instanceof PortView) {
					PortView pv = (PortView) node;
					if (pv.getModelPort() == inputPort) {
						oldView = pv;
					}
				} else if (node instanceof SystemView) {
					SystemView sv = (SystemView) node;
					if (sv.getSystem() == inputPort.getSystem()) {
						targetSystemView = sv;
					}
				}
			}

			double x = 0.0, y = 0.0;
			if (oldView != null) {
				x = oldView.getLayoutX();
				y = oldView.getLayoutY();
				pane.getChildren().remove(oldView);
			}

			PortView newView;
			switch (inputPort.getShapeKind()) {
				case SQUARE:
                    newView = new SquarePortView(inputPort);
					break;
				case TRIANGLE:
                    newView = new TrianglePortView(inputPort);
					break;
				case HEXAGON:
				default:
                    newView = new HexagonPortView(inputPort);
					break;
			}
			newView.setLayoutX(x);
			newView.setLayoutY(y);
			pane.getChildren().add(newView);

			if (targetSystemView != null) {
				java.util.List<PortView> inputs = targetSystemView.getInputPortViews();
				for (int i = 0; i < inputs.size(); i++) {
					if (inputs.get(i).getModelPort() == inputPort) {
						inputs.set(i, newView);
						break;
					}
				}
			}
		} catch (Throwable t) {
		}
	}

	private static void wireDeattachedIndicator(Wire wire, PacketController packetController) {
		if (wire == null || packetController == null) return;
		model.entity.systems.System srcSys = wire.getSource().getSystem();
		model.entity.systems.System dstSys = wire.getDest().getSystem();
		Pane pane = packetController.getPacketLayer();
		if (pane == null) return;
		for (javafx.scene.Node node : pane.getChildren()) {
			if (node instanceof view.components.systems.SystemView) {
				view.components.systems.SystemView sv = (view.components.systems.SystemView) node;
				if (sv.getSystem() == srcSys || sv.getSystem() == dstSys) {
					sv.setIndicatorWarning();
				}
			}
		}
	}
}


