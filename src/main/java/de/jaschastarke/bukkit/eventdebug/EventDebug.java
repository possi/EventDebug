package de.jaschastarke.bukkit.eventdebug;

import java.lang.reflect.Modifier;
import java.util.Set;

import org.bukkit.entity.HumanEntity;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.reflections.Reflections;

import de.jaschastarke.bukkit.lib.Core;
import de.jaschastarke.bukkit.lib.PluginLang;
import de.jaschastarke.bukkit.lib.commands.BukkitCommand;
import de.jaschastarke.bukkit.lib.configuration.Configuration;
import de.jaschastarke.bukkit.lib.configuration.command.ConfigCommand;

public class EventDebug extends Core {
    private Reflections reflections;
    private Set<Class<? extends Event>> events;
    private Config config;
    
    @Override
    public void onInitialize() {
        super.onInitialize();
        config = new Config(this);
        
        setLang(new PluginLang(null, this));
        
        ConfigCommand cc = new ConfCommand(config);
        cc.setPackageName(this.getName());
        commands.registerCommand(cc);
        
        config.saveDefault();
    }
    
    
    @Override
    public void onEnable() {
        super.onEnable();
        
        reflections = new Reflections("org.bukkit");
        
        events = reflections.getSubTypesOf(Event.class);

        EventExecutor executer = new SimpleEventExecuter();
        DynamicListener listener = new DynamicListener(); 
        
        for (Class<? extends Event> event : events) {
            if (!Modifier.isAbstract(event.getModifiers())) {
                getLog().debug("Found Event-Type: " + event.getName());
                try {
                    getServer().getPluginManager().registerEvent(event, listener, EventPriority.MONITOR, executer, this);
                } catch (IllegalPluginAccessException e1) {
                    getLog().warn("Can't listen to Event: " + event.getName() + ": " + e1.getMessage());
                }
            }
        }
    }


    @Override
    public boolean isDebug() {
        return !config.getQuiet() && config.getDebug();
    }

    public static class DummyCommand  extends BukkitCommand {
        @Override
        public String getName() {
            return "ed";
        }
    }
    class ConfCommand extends ConfigCommand {
        public ConfCommand(Configuration config) {
            super(config);
        }
        
        @Override
        public String getName() {
            return "ed";
        }
        
        @Override
        public String[] getAliases() {
            return new String[]{};
        }
    }

    class DynamicListener implements Listener {
        public void execute(Event event) {
            if (!config.getQuiet() && !config.getSupressList().contains(event.getEventName())) {
                StringBuilder s = new StringBuilder();
                s.append("Event fired: ");
                s.append(event.getEventName());
                s.append("\n");
                
                if (event instanceof PlayerEvent) {
                    s.append("  Player: ");
                    s.append(((PlayerEvent) event).getPlayer().getName());
                }
                if (event instanceof BlockEvent) {
                    s.append("  Block: ");
                    s.append(((BlockEvent) event).getBlock().getType().toString());
                    if (((BlockEvent) event).getBlock().getData() != 0) {
                        s.append(":");
                        s.append(((BlockEvent) event).getBlock().getData());
                    }
                    s.append(" at ");
                    s.append(((BlockEvent) event).getBlock().getLocation().toString());
                }
                if (event instanceof EntityEvent) {
                    s.append("  Entity: ");
                    s.append(((EntityEvent) event).getEntityType().toString());
                    if (((EntityEvent) event).getEntity() instanceof HumanEntity) {
                        s.append(" ");
                        s.append(((HumanEntity) ((EntityEvent) event).getEntity()).getName());
                    }
                }
                
                getLog().info(s.toString());
            }
        }
    }
    
    static class SimpleEventExecuter implements EventExecutor {
        @Override
        public void execute(Listener listener, Event event) throws EventException {
            if (!(listener instanceof DynamicListener))
                throw new EventException("Invalid Listener-Object provided");
            ((DynamicListener) listener).execute(event);
        }
    }
}
