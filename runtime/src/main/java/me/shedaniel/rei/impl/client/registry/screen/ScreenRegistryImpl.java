/*
 * This file is licensed under the MIT License, part of Roughly Enough Items.
 * Copyright (c) 2018, 2019, 2020, 2021 shedaniel
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package me.shedaniel.rei.impl.client.registry.screen;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.mojang.blaze3d.platform.Window;
import dev.architectury.event.CompoundEventResult;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.config.DisplayPanelLocation;
import me.shedaniel.rei.api.client.gui.drag.DraggableStackProvider;
import me.shedaniel.rei.api.client.gui.drag.DraggableStackProviderWidget;
import me.shedaniel.rei.api.client.gui.drag.DraggableStackVisitor;
import me.shedaniel.rei.api.client.gui.drag.DraggableStackVisitorWidget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.screen.*;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.registry.ReloadStage;
import me.shedaniel.rei.api.common.util.CollectionUtils;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

@ApiStatus.Internal
@Environment(EnvType.CLIENT)
public class ScreenRegistryImpl implements ScreenRegistry {
    private Multimap<Class<? extends Screen>, ClickArea<?>> clickAreas = HashMultimap.create();
    private List<DraggableStackProvider<Screen>> draggableStacksProviders = new ArrayList<>();
    private List<DraggableStackVisitor<Screen>> draggableStacksVisitors = new ArrayList<>();
    private List<FocusedStackProvider> focusedStackProviders = new ArrayList<>();
    private List<OverlayDecider> deciders = new ArrayList<>();
    private Map<Class<?>, List<OverlayDecider>> cache = new HashMap<>();
    private ExclusionZones exclusionZones;
    private Class<? extends Screen> tmpScreen;
    
    @Override
    public ReloadStage getStage() {
        return ReloadStage.START;
    }
    
    @Override
    public void acceptPlugin(REIClientPlugin plugin) {
        plugin.registerScreens(this);
        plugin.registerExclusionZones(exclusionZones());
    }
    
    @Override
    public <R extends Screen> List<OverlayDecider> getDeciders(R screen) {
        if (screen == null) return Collections.emptyList();
        Class<? extends Screen> screenClass = screen.getClass();
        List<OverlayDecider> possibleCached = cache.get(screenClass);
        if (possibleCached != null) {
            return possibleCached;
        }
        
        tmpScreen = screenClass;
        List<OverlayDecider> deciders = CollectionUtils.filterToList(this.deciders, this::filterResponsible);
        cache.put(screenClass, deciders);
        tmpScreen = null;
        return deciders;
    }
    
    private boolean filterResponsible(OverlayDecider handler) {
        return handler.isHandingScreen(tmpScreen);
    }
    
    @Override
    public List<OverlayDecider> getDeciders() {
        return Collections.unmodifiableList(deciders);
    }
    
    @Override
    public <T extends Screen> Rectangle getScreenBounds(T screen) {
        for (OverlayDecider decider : getDeciders(screen)) {
            if (decider instanceof DisplayBoundsProvider) {
                return ((DisplayBoundsProvider<T>) decider).getScreenBounds(screen);
            }
        }
        return new Rectangle();
    }
    
    @Override
    public <T extends Screen> Rectangle getOverlayBounds(DisplayPanelLocation location, T screen) {
        Window window = Minecraft.getInstance().getWindow();
        int scaledWidth = window.getGuiScaledWidth();
        int scaledHeight = window.getGuiScaledHeight();
        Rectangle screenBounds = getScreenBounds(screen);
        if (screenBounds.isEmpty()) return new Rectangle();
        if (location == DisplayPanelLocation.LEFT) {
            if (screenBounds.x < 10) return new Rectangle();
            return new Rectangle(2, 0, screenBounds.x - 2, scaledHeight);
        } else {
            if (scaledWidth - screenBounds.getMaxX() < 10) return new Rectangle();
            return new Rectangle(screenBounds.getMaxX() + 2, 0, scaledWidth - screenBounds.getMaxX() - 4, scaledHeight);
        }
    }
    
    @Nullable
    @Override
    public <T extends Screen> EntryStack<?> getFocusedStack(T screen, Point mouse) {
        for (FocusedStackProvider provider : focusedStackProviders) {
            CompoundEventResult<EntryStack<?>> result = Objects.requireNonNull(provider.provide(screen, mouse));
            if (result.isTrue()) {
                if (result != null && !result.object().isEmpty())
                    return result.object();
                return null;
            } else if (result.isFalse())
                return null;
        }
        
        return null;
    }
    
    @Override
    public void registerDecider(OverlayDecider decider) {
        deciders.add(decider);
        deciders.sort(Comparator.reverseOrder());
        cache.clear();
        tmpScreen = null;
        registerDraggableStackProvider(DraggableStackProviderWidget.from(context ->
                Widgets.walk(context.getScreen().children(), DraggableStackProviderWidget.class::isInstance)));
        registerDraggableStackVisitor(DraggableStackVisitorWidget.from(context ->
                Widgets.walk(context.getScreen().children(), DraggableStackVisitorWidget.class::isInstance)));
    }
    
    @Override
    public void registerFocusedStack(FocusedStackProvider provider) {
        focusedStackProviders.add(provider);
        focusedStackProviders.sort(Comparator.reverseOrder());
    }
    
    @Override
    public <T extends Screen> void registerDraggableStackProvider(DraggableStackProvider<T> provider) {
        draggableStacksProviders.add((DraggableStackProvider<Screen>) provider);
        draggableStacksProviders.sort(Comparator.reverseOrder());
    }
    
    @Override
    public <T extends Screen> void registerDraggableStackVisitor(DraggableStackVisitor<T> visitor) {
        draggableStacksVisitors.add((DraggableStackVisitor<Screen>) visitor);
        draggableStacksVisitors.sort(Comparator.reverseOrder());
    }
    
    @Override
    public Iterable<DraggableStackProvider<Screen>> getDraggableProviders() {
        return Collections.unmodifiableList(draggableStacksProviders);
    }
    
    @Override
    public Iterable<DraggableStackVisitor<Screen>> getDraggableVisitors() {
        return Collections.unmodifiableList(draggableStacksVisitors);
    }
    
    @Override
    public ExclusionZones exclusionZones() {
        return exclusionZones;
    }
    
    @Override
    public <C extends AbstractContainerMenu, T extends AbstractContainerScreen<C>> void registerContainerClickArea(SimpleClickArea<T> area, Class<? extends T> screenClass, CategoryIdentifier<?>... categories) {
        registerClickArea(screen -> {
            Rectangle rectangle = area.provide(screen).clone();
            rectangle.translate(screen.leftPos, screen.topPos);
            return rectangle;
        }, screenClass, categories);
    }
    
    @Override
    public <T extends Screen> void registerClickArea(Class<? extends T> screenClass, ClickArea<T> area) {
        clickAreas.put(screenClass, area);
    }
    
    @Override
    @Nullable
    public <T extends Screen> Set<CategoryIdentifier<?>> handleClickArea(Class<T> screenClass, ClickArea.ClickAreaContext<T> context) {
        Mutable<Set<CategoryIdentifier<?>>> categories = new MutableObject<>(null);
        for (ClickArea<?> area : this.clickAreas.get(screenClass)) {
            ClickArea.Result result = ((ClickArea<T>) area).handle(context);
            
            if (result.isSuccessful()) {
                if (categories.getValue() == null) {
                    categories.setValue(new LinkedHashSet<>());
                }
                result.getCategories().collect(Collectors.toCollection(categories::getValue));
            }
        }
        return categories.getValue();
    }
    
    @Override
    public void startReload() {
        clickAreas.clear();
        deciders.clear();
        cache.clear();
        focusedStackProviders.clear();
        draggableStacksProviders.clear();
        draggableStacksVisitors.clear();
        tmpScreen = null;
        
        registerDefault();
    }
    
    private void registerDefault() {
        registerDecider(this.exclusionZones = new ExclusionZonesImpl());
        registerDecider(new OverlayDecider() {
            @Override
            public <R extends Screen> boolean isHandingScreen(Class<R> screen) {
                return true;
            }
            
            @Override
            public InteractionResult shouldScreenBeOverlaid(Class<?> screen) {
                return AbstractContainerScreen.class.isAssignableFrom(screen) ? InteractionResult.SUCCESS : InteractionResult.PASS;
            }
            
            @Override
            public double getPriority() {
                return -10.0;
            }
        });
        registerFocusedStack(new FocusedStackProvider() {
            @Override
            public CompoundEventResult<EntryStack<?>> provide(Screen screen, Point mouse) {
                if (screen instanceof AbstractContainerScreen<?> containerScreen) {
                    if (containerScreen.hoveredSlot != null && !containerScreen.hoveredSlot.getItem().isEmpty())
                        return CompoundEventResult.interruptTrue(EntryStacks.of(containerScreen.hoveredSlot.getItem()));
                }
                return CompoundEventResult.pass();
            }
            
            @Override
            public double getPriority() {
                return -10.0;
            }
        });
    }
}
