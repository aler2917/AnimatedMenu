package me.megamichiel.animatedmenu.menu;

import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animationlib.Nagger;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

public abstract class AbstractMenu {

    private static final ItemStack[] EMPTY_ITEM_ARRAY = new ItemStack[0];

    protected final Nagger nagger;
    protected final String name;
    protected final MenuGrid menuGrid;

    protected AnimatedMenuPlugin plugin;
    private boolean dynamicSlots;

    protected AbstractMenu(Nagger nagger, String name, int size) {
        this.nagger = nagger;
        this.name = name;
        menuGrid = new MenuGrid(this, size);
    }

    public void init(AnimatedMenuPlugin plugin) {
        if (this.plugin == null)
            this.plugin = plugin;
        for (int i = 0; i < menuGrid.getSize(); i++) {
            if (menuGrid.getItems()[i].hasDynamicSlot()) {
                dynamicSlots = true;
                break;
            }
        }
    }

    public String getName() {
        return name;
    }

    public MenuGrid getMenuGrid() {
        return menuGrid;
    }

    public AnimatedMenuPlugin getPlugin() {
        return plugin;
    }

    protected abstract Iterator<Map.Entry<Player, Inventory>> getViewers();

    public void tick() {
        if (plugin == null) return;
        MenuItem[] items = menuGrid.getItems();
        int size = menuGrid.getSize();
        if (dynamicSlots) {
            for (int i = 0; i < size; i++)
                items[i].tick();
            ItemStack[] contents = new ItemStack[items.length];
            Iterator<Map.Entry<Player, Inventory>> viewers = getViewers();
            while (viewers.hasNext()) {
                Map.Entry<Player, Inventory> entry = viewers.next();
                Player player = entry.getKey();
                Inventory inv = entry.getValue();
                MenuItem item;
                for (int i = 0; i < size; i++)
                    if (!(item = items[i]).getSettings().isHidden(plugin, player))
                        contents[item.getSlot(player, contents)]
                                = item.getSettings().getItem(nagger, player);
                for (int i = 0; i < contents.length; i++) {
                    inv.setItem(i, contents[i]);
                    contents[i] = null;
                }
            }
        } else for (int index = 0; index < size; index++) {
            MenuItem item = items[index];
            if (item.tick()) {
                Iterator<Map.Entry<Player, Inventory>> viewers = getViewers();
                while (viewers.hasNext()) {
                    Map.Entry<Player, Inventory> entry = viewers.next();
                    Player player = entry.getKey();
                    Inventory inv = entry.getValue();
                    int slot = item.getSlot(player, EMPTY_ITEM_ARRAY);
                    boolean hidden = item.getSettings().isHidden(plugin, player);
                    ItemStack is = inv.getItem(slot);
                    if (hidden && is != null) {
                        inv.setItem(slot, null);
                        continue;
                    } else if (!hidden && is == null) {
                        inv.setItem(slot, item.load(nagger, player));
                        is = inv.getItem(slot);
                    }
                    if (!hidden) item.apply(nagger, player, is);
                }
            }
        }
    }
}