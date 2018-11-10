package org.terasology.rendering.nui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.context.Context;
import org.terasology.context.internal.ContextImpl;
import org.terasology.engine.SimpleUri;
import org.terasology.engine.subsystem.config.BindsManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.input.BindButtonEvent;
import org.terasology.input.BindButtonSubscriber;
import org.terasology.input.BindableButton;
import org.terasology.input.Keyboard;
import org.terasology.input.internal.BindableButtonImpl;
import org.terasology.logic.players.event.OnPlayerSpawnedEvent;
import org.terasology.registry.In;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

@RegisterSystem(RegisterMode.ALWAYS)
public class SortOrder extends BaseComponentSystem {
    private static ArrayList<Integer> id;
    private static int current;
    private static ArrayList<Integer[]> layersFilled; //arg1 of the Integer[] is the layer depth, arg2 is the number of things on that layer
    private static int index;
    private static int uiIndex;
    private static int first;
    private static final Logger logger = LoggerFactory.getLogger(SortOrder.class);
    //private static ArrayList<Class<AbstractWidget>> widgetList;
    //private static ArrayList<AbstractWidget> enabledWidgets;
    private static ArrayList<CoreScreenLayer> enabledWidgets;
    private static boolean initialized = false;
    private Canvas canvas;
    private Context context;

    @In
    private BindsManager bindsManager;

    /**
     * Initializes sort order
     * @param event
     * @param player
     */
    @ReceiveEvent
    public void onPlayerSpawnedEvent(OnPlayerSpawnedEvent event, EntityRef player) {
        initialized = true;
        logger.info("initializing...");
        Map<Integer, BindableButton> keys = bindsManager.getKeyBinds();
        if (keys.containsKey(Keyboard.Key.SEMICOLON.getId())) {
            logger.info("contains key!");
            keys.get(Keyboard.Key.SEMICOLON.getId()).subscribe(new BindButtonSubscriber() {
                @Override
                public boolean onPress(float delta, EntityRef target) {
                    target.send(new FocusChangedEvent());
                    return false;
                }

                @Override
                public boolean onRepeat(float delta, EntityRef target) {
                    target.send(new FocusChangedEvent());
                    return false;
                }

                @Override
                public boolean onRelease(float delta, EntityRef target) {
                    return false;
                }
            });
        } else {
            logger.info("doesn't contain key");
            BindButtonSubscriber bindButtonSubscriber = new BindButtonSubscriber() {
                @Override
                public boolean onPress(float delta, EntityRef target) {
                    target.send(new FocusChangedEvent());
                    return false;
                }

                @Override
                public boolean onRepeat(float delta, EntityRef target) {
                    target.send(new FocusChangedEvent());
                    return false;
                }

                @Override
                public boolean onRelease(float delta, EntityRef target) {
                    return false;
                }
            };
            keys.put(Keyboard.Key.SEMICOLON.getId(), new BindableButtonImpl(new SimpleUri("changeFocus"), "Change Focus", new BindButtonEvent()));
            logger.info("made key!");
            keys.get(Keyboard.Key.SEMICOLON.getId()).subscribe(bindButtonSubscriber);
            logger.info("added subscriber!");
        }
        current = 0;
        first = 0;
        index = 0;
        uiIndex = -1;
        layersFilled = new ArrayList<Integer[]>();
        //widgetList = new ArrayList<Class<AbstractWidget>>();
        //enabledWidgets = new ArrayList<AbstractWidget>();
        enabledWidgets = new ArrayList<CoreScreenLayer>();
        context = new ContextImpl();
    }

    @ReceiveEvent
    public void changeFocus(FocusChangedEvent event, EntityRef ref) {
        rotateOrder(true);
    }

    /**
     * rotates through the elements
     * @param increment
     */
    public static void rotateOrder(boolean increment) {
        if (layersFilled.size()>0) {
            Collections.sort(layersFilled, (a, b) -> Math.max(a[0], b[0]));
            logger.info("changing focus");
            if (increment) {
                index++;
            }
            int iterator;
            if (index < layersFilled.size()) {
                iterator = layersFilled.get(index)[0];
            } else {
                index = 0;
                iterator = layersFilled.get(index)[0];
            }
            boolean loopThroughDone = false;


            int tempIndex = index;
            int timesLooping = 0;
            ArrayList<CoreScreenLayer> widgetsCopy = new ArrayList<CoreScreenLayer>(enabledWidgets);
            while (!loopThroughDone) {
                for (CoreScreenLayer widget : widgetsCopy) {
                    widget.setInSortOrder(true);
                    if (widget.getDepth() == iterator) {
                        String id = widget.getId();
                        logger.info("drawn: " + widget.getId() + " ~~~ iterator: " + iterator);

                        widget.getManager().pushScreen(widget.getId());
                        widget.getManager().render();
                    }
                    widget.setInSortOrder(false);
                }
                if (tempIndex < layersFilled.size()) {
                    iterator = layersFilled.get(tempIndex)[0];
                    tempIndex++;
                } else {
                    tempIndex = 0;
                }
                if (timesLooping > layersFilled.size()) {
                    loopThroughDone = true;
                }
                timesLooping++;
            }
        }
    }

    /**
     * for sorting layersFilled
     * @param a
     * @param b
     * @return
     */
    private Integer forSort(Integer[] a, Integer[] b) {
        return Math.max(a[0], b[0]);
    }

    /**
     * increments current (for depth)
     * @return the new value of current
     */
    public static int getCurrent() {
        current++;
        return current;
    }

    /**
     * adds another occurrence of a certain depth to layers filled
     * @param layer the depth
     */
    public static void addAnother(int layer) {
        try {
            layersFilled.get(layer)[1]++;
        } catch (Exception e) {
            Integer[] toAdd = new Integer[2];
            toAdd[0] = layer;
            toAdd[1] = 1;
            layersFilled.add(toAdd);
        }
    }

    /**
     * removes an occurance of a certain depth to layers filled
     * @param layer the depth
     */
    public static void removeOne(int layer) {
        for (int i=0; i<layersFilled.size(); i++) {
            if (layersFilled.get(i)[0] == layer) {
                layersFilled.get(i)[1]--;
                return;
            }
        }
    }

    public static void setEnabledWidgets(ArrayList<CoreScreenLayer> widgetList) {
        if (initialized) {
            enabledWidgets = widgetList;
        }
    }
    public static ArrayList<CoreScreenLayer> getEnabledWidgets() {
        return enabledWidgets;
    }
    public static int makeIndex() {
        uiIndex++;
        return uiIndex;
    }

    public static boolean isInitialized() {
        return initialized;
    }
}