package de.jaschastarke.bukkit.eventdebug;

import java.util.Arrays;

import de.jaschastarke.bukkit.lib.Core;
import de.jaschastarke.bukkit.lib.configuration.PluginConfiguration;
import de.jaschastarke.bukkit.lib.configuration.StringList;
import de.jaschastarke.configuration.annotations.IsConfigurationNode;
import de.jaschastarke.maven.ArchiveDocComments;

@ArchiveDocComments
public class Config extends PluginConfiguration {
    public Config(Core plugin) {
        super(plugin);
    }

    /**
     * Quiet
     * 
     * Suppress all Events
     * 
     * default: false
     */
    @IsConfigurationNode(order = 100)
    public boolean getQuiet() {
        return config.getBoolean("quiet", false);
    }
    
    private StringList supress = null;
    /**
     * SupressEvents
     * 
     * A list of Event-Types that won't be shown.
     * 
     * default:
     *  - VehicleUpdateEvent
     *  - VehicleBlockCollisionEvent
     *  - BlockPhysicsEvents
     *  - ChunkUnloadEvent
     *  - ChunkLoadEvent
     *  - PlayerMoveEvent
     */
    @IsConfigurationNode(order = 200, name = "supress")
    public StringList getSupressList() {
        if (supress == null) {
            if (!config.contains("supress")) {
                supress = new StringList(Arrays.asList(
                        "VehicleUpdateEvent",
                        "VehicleBlockCollisionEvent",
                        "BlockPhysicsEvents",
                        "ChunkUnloadEvent",
                        "ChunkLoadEvent",
                        "PlayerMoveEvent"));
            } else {
                supress = new StringList(config.getStringList("supress"));
            }
        }
        return supress;
    }

    /**
     * Filter PlayerRange
     *
     * Only shows Events that happens in range of X blocks to an OP. Events that can not be associated with a location
     * (like module load or such) are always shown.
     *
     * default: 3
     */
    @IsConfigurationNode(order = 300, name = "playerRange")
    public int getRangeToPlayer() {
        return config.getInt("playerRange", 3);
    }

    /**
     * Short Output
     *
     * Only shows one line per Event
     *
     * default: false
     */
    @IsConfigurationNode(order = 400)
    public boolean getShort() {
        return config.getBoolean("short", false);
    }
    
    /**
     * Debug
     * 
     * The debug modus spams some details about the plugin to the server-log (console) which can help to solve issues.
     * 
     * default: false
     */
    @IsConfigurationNode(order = 9999)
    public boolean getDebug() {
        return config.getBoolean("debug", false);
    }
}
