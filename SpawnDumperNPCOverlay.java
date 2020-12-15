package net.runelite.client.plugins.spawndumper;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.CollisionData;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.OverlayUtil;

public class SpawnDumperNPCOverlay extends Overlay
{

    private final Client client;
    private final SpawnDumperPlugin plugin;

    @Inject
    private SpawnDumperNPCOverlay(Client client, SpawnDumperPlugin plugin)
    {
        this.client = client;
        this.plugin = plugin;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
        setPriority(OverlayPriority.MED);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (plugin.getSelectedNPCIndex() == -1)
        {
            return null;
        }

        NPCSpawn spawn = plugin.getSpawns().get(plugin.getSelectedNPCIndex());

        if (spawn == null)
        {
            return null;
        }

        Set<WorldPoint> points = spawn.getPoints();

        for (WorldPoint point : points)
        {
            renderTile(graphics, point, Color.BLUE);
        }

//        for (int x = spawn.getMinX(); x <= spawn.getMaxX(); x++)
//        {
//            for (int y = spawn.getMinY(); y <= spawn.getMaxY(); y++)
//            {
//                WorldPoint point = new WorldPoint(x, y, spawn.getMinZ());
//
//                if (!points.contains(point))
//                {
//                    renderTile(graphics, point, Color.CYAN);
//                }
//            }
//        }

        int spawnX = spawn.getMinX() + ((int) Math.ceil((spawn.getMaxX() - spawn.getMinX()) / 2D));
        int spawnY = spawn.getMinY() + ((int) Math.ceil((spawn.getMaxY() - spawn.getMinY()) / 2D));
        renderTile(graphics, new WorldPoint(spawnX, spawnY, spawn.getMinZ()), Color.RED);

        return null;
    }

    private void renderTile(final Graphics2D graphics, final WorldPoint dest, final Color color)
    {
        if (dest == null)
        {
            return;
        }

        LocalPoint localPoint = LocalPoint.fromWorld(client, dest);

        if (localPoint == null)
        {
            return;
        }

        final Polygon poly = Perspective.getCanvasTilePoly(client, localPoint);

        if (poly == null)
        {
            return;
        }

        OverlayUtil.renderPolygon(graphics, poly, color);
    }
}
