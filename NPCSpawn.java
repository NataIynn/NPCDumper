package net.runelite.client.plugins.spawndumper;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Data;
import net.runelite.api.coords.WorldPoint;

@Data
public class NPCSpawn {

    public static void main(String[] args) throws FileNotFoundException
    {
        Map<Integer, String> names = new Gson().fromJson(new InputStreamReader(new FileInputStream("npc_names.json")), new TypeToken<Map<Integer, String>>() {}.getType());
        List<NPCSpawn> spawns = new Gson().fromJson(new InputStreamReader(new FileInputStream("spawns.json")), new TypeToken<List<NPCSpawn>>() {}.getType());
        spawns.sort(Comparator.comparingInt(NPCSpawn::getNpc));

        System.out.println("[");
        for (NPCSpawn spawn : spawns)
        {
            int deltaX = spawn.getMaxX() - spawn.getMinX();
            int deltaY = spawn.getMaxY() - spawn.getMinY();
            int walkRadius = Math.max(deltaX, deltaY);

            int spawnX = spawn.getMinX() + (deltaX / 2);
            int spawnY = spawn.getMinY() + (deltaY / 2);

            StringBuilder builder = new StringBuilder();
            builder.append("  { ");
            builder.append("\"id\": ").append(spawn.getNpc());
            builder.append(", \"x\": ").append(spawnX);
            builder.append(", \"y\": ").append(spawnY);
            builder.append(", \"z\": ").append(spawn.getMinZ());
            if (walkRadius != 0)
            {
                builder.append(", \"walkRange\": ").append(walkRadius);
            }
            if (spawn.getOrientation() != -1)
            {
                builder.append(", \"direction\": ").append('"').append(orientationToString(spawn.getOrientation())).append('"');
            }
            builder.append(" },");
            builder.append(" // ").append(names.get(spawn.getNpc()));
            System.out.println(builder);
        }
        System.out.println("]");
    }

    private static String orientationToString(int orientation)
    {
        switch (orientation)
        {
            case 768:
                return "NW";
            case 1024:
                return "N";
            case 1280:
                return "NE";
            case 512:
                return "W";
            case 1536:
                return "E";
            case 256:
                return "SW";
            case 0:
                return "S";
            case 1792:
                return "SE";
        }

        return "S";
    }

    private final int npc;
    private final int index;
    private final Set<WorldPoint> points = new HashSet<>();
    private int orientation;

    public int getMinX()
    {
        int minX = Integer.MAX_VALUE;

        for (WorldPoint point : points)
        {
            minX = Math.min(minX, point.getX());
        }

        return minX;
    }

    public int getMaxX()
    {
        int maxX = Integer.MIN_VALUE;

        for (WorldPoint point : points)
        {
            maxX = Math.max(maxX, point.getX());
        }

        return maxX;
    }

    public int getMinY()
    {
        int minY = Integer.MAX_VALUE;

        for (WorldPoint point : points)
        {
            minY = Math.min(minY, point.getY());
        }

        return minY;
    }

    public int getMaxY()
    {
        int maxY = Integer.MIN_VALUE;

        for (WorldPoint point : points)
        {
            maxY = Math.max(maxY, point.getY());
        }

        return maxY;
    }

    public int getMinZ()
    {
        int minZ = Integer.MAX_VALUE;

        for (WorldPoint point : points)
        {
            minZ = Math.min(minZ, point.getPlane());
        }

        return minZ;
    }

    public int getMaxZ()
    {
        int maxZ = Integer.MIN_VALUE;

        for (WorldPoint point : points)
        {
            maxZ = Math.max(maxZ, point.getPlane());
        }

        return maxZ;
    }

}
