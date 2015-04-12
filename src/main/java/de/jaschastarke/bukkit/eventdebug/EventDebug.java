package de.jaschastarke.bukkit.eventdebug;

import de.jaschastarke.bukkit.lib.Core;
import de.jaschastarke.bukkit.lib.PluginLang;
import de.jaschastarke.bukkit.lib.commands.BukkitCommand;
import de.jaschastarke.bukkit.lib.configuration.Configuration;
import de.jaschastarke.bukkit.lib.configuration.command.ConfigCommand;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.world.ChunkEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.bukkit.plugin.Plugin;
import org.reflections.Reflections;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class EventDebug extends Core {
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

        Reflections reflections = new Reflections("org.bukkit");

        Set<Class<? extends Event>> events = reflections.getSubTypesOf(Event.class);

        EventExecutor executer = new SimpleEventExecuter();
        DynamicListener listener = new DynamicListener(); 

        int count = 0;
        for (Class<? extends Event> event : events) {
            if (!Modifier.isAbstract(event.getModifiers())) {
                getLog().debug("Found Event-Type: " + event.getName());
                try {
                    getServer().getPluginManager().registerEvent(event, listener, EventPriority.MONITOR, executer, this);
                    count++;
                } catch (IllegalPluginAccessException e1) {
                    getLog().warn("Can't listen to Event: " + event.getName() + ": " + e1.getMessage());
                }
            }
        }
        getLog().debug("Done registering for "+count+" found Events");
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

    static class EventData {
        public String Name;
        public String Value;
        public boolean Important;

        static EventData create(String name, String val) {
            EventData d = new EventData();
            d.Name = name;
            d.Value = val;
            return d;
        }
        public static EventData create(String name, String sval, boolean important) {
            EventData d = create(name, sval);
            d.Important = d.Important || important;
            return d;
        }
        static boolean hasImportant(List<EventData> data) {
            for (EventData eventData : data) {
                if (eventData.Important)
                    return true;
            }
            return false;
        }
    }

    class DynamicListener implements Listener {
        public void execute(Event event) {
            if (!config.getQuiet() && !config.getSupressList().contains(event.getEventName())) {
                Location loc = null;

                if (event instanceof BlockEvent) {
                    loc = ((BlockEvent) event).getBlock().getLocation();
                } else if (event instanceof PlayerEvent) {
                    loc = ((PlayerEvent) event).getPlayer().getLocation();
                } else if (event instanceof EntityEvent) {
                    loc = ((EntityEvent) event).getEntity().getLocation();
                }
                boolean inrange = true;
                if (loc != null && config.getRangeToPlayer() > 0) {
                    inrange = false;
                    for (Player p : getServer().getOnlinePlayers()) {
                        if (p.isOp() && p.getLocation().getWorld().equals(loc.getWorld()) && p.getLocation().distance(loc) <= config.getRangeToPlayer()) {
                            inrange = true;
                            break;
                        }
                    }
                }
                if (!inrange)
                    return;

                ArrayList<EventData> data = new ArrayList<EventData>();
                for (Method method : event.getClass().getMethods()) {
                    if (Modifier.isPublic(method.getModifiers()) && !Modifier.isStatic(method.getModifiers())) {
                        if (method.getName().equalsIgnoreCase("getHandlers") ||
                                method.getName().equalsIgnoreCase("getClass") ||
                                method.getName().equalsIgnoreCase("isAsynchronous") ||
                                method.getName().equalsIgnoreCase("getEventName"))
                            continue;
                        if (method.getParameterTypes().length > 0)
                            continue;
                        if (method.getName().startsWith("get") || method.getName().startsWith("is")) {
                            try {
                                Object val = method.invoke(event);
                                String sval;

                                boolean important = false;
                                if (val instanceof Player) {
                                    sval = ((Player) val).getName();
                                    important = true;
                                } else if (val instanceof Block) {
                                    sval = ((Block) val).getType() + " at " + ((Block) val).getLocation().toString();
                                } else if (val instanceof ItemStack) {
                                    sval = ((ItemStack) val).getData() + " x" + ((ItemStack) val).getAmount();
                                } else if (val instanceof Plugin) {
                                    sval = val.toString(); // Prevent NoClassDefFoundError on below toString-check
                                } else if (val instanceof Enum) {
                                    sval = val.toString();
                                    important = true;
                                } else if (val == null) {
                                    continue;
                                } else {
                                    try {
                                        if (val.getClass().getMethod("toString").getDeclaringClass() == Object.class)
                                            continue; // We don't need no information that this is an object.
                                    } catch (NoSuchMethodException e) {
                                        // Well if there is an Object without toString, the World is doomed.
                                        // The whole world, not just this minecraft world!
                                    }
                                    sval = val.toString();
                                }

                                data.add(EventData.create(method.getName(), sval, important));
                            } catch (IllegalAccessException e) {
                                getLog().debug("IAE: "+e.getMessage());
                            } catch (InvocationTargetException e) {
                                getLog().debug("ITE: "+e.getMessage());
                            }
                        }
                    }
                }


                StringBuilder s = new StringBuilder();
                s.append("Event fired: ");
                s.append(event.getEventName());

                if (config.getShort() && EventData.hasImportant(data)) {
                    s.append(" (");

                    boolean first = true;
                    for (EventData ed : data) {
                        if (ed.Important) {
                            if (first)
                                first = false;
                            else
                                s.append(", ");

                            s.append(ed.Name);
                            s.append(": ");
                            s.append(ed.Value);
                        }
                    }
                    s.append(")");
                } else {
                    for (EventData ed : data) {
                        s.append("\n");
                        s.append(ed.Name);
                        s.append(": ");
                        s.append(ed.Value);
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
