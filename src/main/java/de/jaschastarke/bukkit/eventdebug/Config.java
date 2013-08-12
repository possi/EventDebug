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
     *  - CreatureSpawnEvent
     *  - ChunkUnloadEvent
     */
    @IsConfigurationNode(order = 200, name = "supress")
    public StringList getSupressList() {
        if (supress == null) {
            if (!config.contains("surpess")) {
                supress = new StringList(Arrays.asList(new String[]{
                    "VehicleUpdateEvent",
                    "VehicleBlockCollisionEvent",
                    "BlockPhysicsEvents",
                    "CreatureSpawnEvent",
                    "ChunkUnloadEvent"
                }));
            } else {
                supress = new StringList(config.getStringList("supress"));
            }
        }
        return supress;
    }

    
    /**
     * Debug
     * 
     * The debug modus spams much details about the plugin to the server-log (console) which can help to solve issues.
     * 
     * default: true
     */
    @IsConfigurationNode(order = 9999)
    public boolean getDebug() {
        return config.getBoolean("debug", true);
    }
}
