package net.runelite.client.plugins.spawndumper;

import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;

public class SpawnDumperOverlay extends Overlay
{

    private final SpawnDumperPlugin plugin;
    private final PanelComponent panelComponent = new PanelComponent();

    @Inject
    private SpawnDumperOverlay(SpawnDumperPlugin plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        panelComponent.getChildren().clear();

        panelComponent.getChildren().add(LineComponent.builder()
            .left("Spawn Dumper")
            .build());

        panelComponent.getChildren().add(LineComponent.builder()
            .left("Total Spawns")
            .right(String.valueOf(plugin.getSpawns().size()))
            .build());

        panelComponent.getChildren().add(LineComponent.builder()
			.left("NPC Updates")
			.right(String.valueOf(plugin.getUpdatedThisTick()))
			.build());

        return panelComponent.render(graphics);
    }
}
