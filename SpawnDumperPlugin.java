package net.runelite.client.plugins.spawndumper;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.swing.JButton;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import static net.runelite.api.MenuAction.MENU_ACTION_DEPRIORITIZE_OFFSET;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.NpcSpawned;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.devtools.DevToolsPlugin;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;

@PluginDescriptor(
    name = "Spawn Dumper"
)
@Slf4j
public class SpawnDumperPlugin extends Plugin
{
    // Option added to NPC menu
    private static final String TAG = "Spawn Info";
    private static final String UNTAG = "Remove Spawn Info";

    @Inject
    private Client client;

    @Inject
    private ClientToolbar clientToolbar;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private SpawnDumperOverlay spawnDumperOverlay;

    @Inject
    private SpawnDumperNPCOverlay spawnDumperNPCOverlay;

    @Getter
    private SpawnDumperButton enabled;
    @Getter
    private JButton saveSpawns;
    @Getter
    private JButton loadSpawns;
    @Getter
    private JButton clearSpawns;
    private NavigationButton navButton;

    /**
     * A map of {@link NPC#getIndex()} to a {@link NPCSpawn}.
     */
    @Getter(AccessLevel.PACKAGE)
    private Map<Integer, NPCSpawn> spawns = new HashMap<>();

    /**
     * Resets every tick, used to show the amount of updates in a single tick.
     */
    @Getter(AccessLevel.PACKAGE)
    private int updatedThisTick = 0;

    /**
     * The npc that is set to be highlighted and show information for.
     */
    @Getter(AccessLevel.PACKAGE)
    private int selectedNPCIndex = -1;

    @Override
    protected void startUp() throws Exception
    {
        enabled = new SpawnDumperButton("Enabled");

        saveSpawns = new JButton("Save Spawns");
        saveSpawns.addActionListener(e ->
        {
            try
            {
                Path spawnsPath = Paths.get("spawns.json");
                Files.write(spawnsPath, new Gson().toJson(spawns.values()).getBytes());
                int points = spawns.values().stream().mapToInt(value -> value.getPoints().size()).sum();
                System.out.println("Saved " + spawns.size() + " spawns with " + points + " points to " + spawnsPath + ".");
            }
            catch (IOException ex)
            {
                ex.printStackTrace();
            }
        });

        loadSpawns = new JButton("Load Spawns");
        loadSpawns.addActionListener(e ->
        {
            try
            {
                List<NPCSpawn> tempSpawns = new Gson().fromJson(new InputStreamReader(new FileInputStream("spawns.json")), new TypeToken<List<NPCSpawn>>() {}.getType());

                int points = 0;
                spawns.clear();
                for (NPCSpawn spawn : tempSpawns)
                {
                    points += spawn.getPoints().size();
                    spawns.put(spawn.getIndex(), spawn);
                }
                System.out.println("Loaded " + spawns.size() + " spawns with " + points + " points.");
            }
            catch (FileNotFoundException ex)
            {
                ex.printStackTrace();
            }
        });

        clearSpawns = new JButton("Clear Spawns");
        clearSpawns.addActionListener(e -> spawns.clear());

        final SpawnDumperPanel panel = injector.getInstance(SpawnDumperPanel.class);

        final BufferedImage icon = ImageUtil.getResourceStreamFromClass(DevToolsPlugin.class, "devtools_icon.png");

        overlayManager.add(spawnDumperOverlay);
        overlayManager.add(spawnDumperNPCOverlay);

        navButton = NavigationButton.builder()
            .tooltip("Spawn Dumper")
            .icon(icon)
            .priority(1)
            .panel(panel)
            .build();

        clientToolbar.addNavigation(navButton);
    }

    @Override
    protected void shutDown() throws Exception
    {
        overlayManager.remove(spawnDumperOverlay);
        overlayManager.remove(spawnDumperNPCOverlay);
        clientToolbar.removeNavigation(navButton);
    }


    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded event)
    {
        int type = event.getType();

        if (type >= MENU_ACTION_DEPRIORITIZE_OFFSET)
        {
            type -= MENU_ACTION_DEPRIORITIZE_OFFSET;
        }

        if (type == MenuAction.EXAMINE_NPC.getId())
        {
            // Add tag option
            MenuEntry[] menuEntries = client.getMenuEntries();
            menuEntries = Arrays.copyOf(menuEntries, menuEntries.length + 1);
            final MenuEntry tagEntry = menuEntries[menuEntries.length - 1] = new MenuEntry();
            tagEntry.setOption(event.getIdentifier() == selectedNPCIndex ? UNTAG : TAG);
            tagEntry.setTarget(event.getTarget());
            tagEntry.setParam0(event.getActionParam0());
            tagEntry.setParam1(event.getActionParam1());
            tagEntry.setIdentifier(event.getIdentifier());
            tagEntry.setType(MenuAction.RUNELITE.getId());
            client.setMenuEntries(menuEntries);
        }
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked click)
    {
        if (click.getMenuAction() != MenuAction.RUNELITE ||
            !(click.getMenuOption().equals(TAG) || click.getMenuOption().equals(UNTAG)))
        {
            return;
        }

        final int id = click.getId();

        if (selectedNPCIndex == id)
        {
            selectedNPCIndex = -1;
        }
        else
        {
            selectedNPCIndex = id;
        }

        click.consume();
    }

    @Subscribe
    public void onNpcSpawned(NpcSpawned event)
    {
        // check if the dumper is active
        if (!enabled.isActive())
        {
            return;
        }

        NPC npc = event.getNpc();
        NPCComposition def = client.getNpcDefinition(event.getNpc().getId());
        NPCSpawn existSpawn = spawns.get(npc.getIndex());

        if (existSpawn != null && existSpawn.getNpc() == npc.getId())
        {
            // spawn exists and npc ids are the same
            return;
        }

        NPCSpawn spawn = new NPCSpawn(npc.getId(), npc.getIndex());
        spawn.setOrientation(npc.getOrientation());
        spawn.getPoints().add(npc.getWorldLocation());

        spawns.put(npc.getIndex(), spawn);

        if (existSpawn != null)
        {
            log.debug("Replaced " + existSpawn.getNpc() + " with " + spawn.getNpc() + " due to same index but different ids.");
        }
        else
        {
            log.debug("Added new NPC to spawns: index=" + npc.getIndex() + ", id=" + npc.getId() + ", name=" + def.getName() + "");
        }
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        updatedThisTick = 0;

        // check if the dumper is active
        if (!enabled.isActive())
        {
            return;
        }

        for (NPC npc : client.getNpcs())
        {
            NPCSpawn spawn = spawns.get(npc.getIndex());

            if (spawn == null)
            {
                continue;
            }

            if (spawn.getOrientation() != -1 && npc.getOrientation() != spawn.getOrientation())
            {
                spawn.setOrientation(-1);
            }

            if (spawn.getPoints().add(npc.getWorldLocation()))
            {
                updatedThisTick++;
            }
        }
    }
}
