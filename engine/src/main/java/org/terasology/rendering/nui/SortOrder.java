package org.terasology.rendering.nui;

//TODO: figure out why so many are added to enabledWidgets, and so few removed

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static int current;
    private static ArrayList<Integer[]> layersFilled; //arg1 of the Integer[] is the layer depth, arg2 is the number of things on that layer
    private static int index;
    private static int focusLayer;
    private static final Logger logger = LoggerFactory.getLogger(SortOrder.class);
    private static ArrayList<AbstractWidget> widgetList;
    private static ArrayList<AbstractWidget> enabledWidgets;
    private static boolean initialized = false;
    private static ArrayList<String> widgetIDs;

    @In
    private BindsManager bindsManager;

    @ReceiveEvent
    public void onPlayerSpawnedEvent(OnPlayerSpawnedEvent event, EntityRef player) {
        initialized = true;
        logger.info("initializing");
        Map<Integer, BindableButton> keys = bindsManager.getKeyBinds();
        if (keys.containsKey(Keyboard.Key.N.getId())) {
            logger.info("contains key!");
            keys.get(Keyboard.Key.N.getId()).subscribe(new BindButtonSubscriber() {
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
            keys.put(Keyboard.Key.N.getId(), new BindableButtonImpl(new SimpleUri("changeFocus"), "Change Focus", new BindButtonEvent()));
            logger.info("made key!");
            keys.get(Keyboard.Key.N.getId()).subscribe(bindButtonSubscriber);
            logger.info("added subscriber!");
        }
        current = 0;
        focusLayer = 0;
        index = 0;
        layersFilled = new ArrayList<Integer[]>();
        widgetList = new ArrayList<AbstractWidget>();
        enabledWidgets = new ArrayList<AbstractWidget>();
}

    @ReceiveEvent
    public void changeFocus(FocusChangedEvent event, EntityRef ref) {
        Collections.sort(layersFilled, (a, b) -> Math.max(a[0], b[0]));
        logger.info("changing focus");
        index++;
        logger.info("index: "+index);
        if (index < layersFilled.size()){
            focusLayer = layersFilled.get(index)[0];
        } else {
            logger.info("resetting index...");
            index = 0;
        }

        logger.info("enablesWidgets length: "+enabledWidgets.size());
        for(AbstractWidget widget:enabledWidgets) {
            if (widget.getDepth()==focusLayer) {
                logger.info("gained focus");
                widget.onGainFocus();
                widget.setEnabled(true);
            } else {
                logger.info("lost focus");
                widget.onLoseFocus();
                widget.setEnabled(false);
            }
        }
    }

    private Integer forSort(Integer[] a, Integer[] b) {
        return Math.max(a[0], b[0]);
    }

    public static int getCurrent() {
        return current;
    }
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
    public static void removeOne(int layer) {
        for (int i=0; i<layersFilled.size(); i++) {
            if (layersFilled.get(i)[0] == layer) {
                layersFilled.get(i)[1]--;
                return;
            }
        }
    }
    public static void addToWidgetList(AbstractWidget widget) {
        if (initialized) {
            widgetList.add(widget);
            addAnother(widget.getDepth());
            current++;
        }
    }
    public static void setFocusLayer(int toSet) {
        focusLayer = toSet;
    }
    public static void setEnabledWidgets(ArrayList<AbstractWidget> widgetList) {
        enabledWidgets = widgetList;
    }
    public static ArrayList<AbstractWidget> getEnabledWidgets() {
        return enabledWidgets;
    }
    public static ArrayList<String> getWidgetIDs() {
        return widgetIDs;
    }
}