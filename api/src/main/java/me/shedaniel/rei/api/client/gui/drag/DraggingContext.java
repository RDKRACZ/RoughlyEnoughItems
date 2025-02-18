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

package me.shedaniel.rei.api.client.gui.drag;

import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.REIRuntime;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * The context of the current dragged stack on the overlay.
 * <p>
 * Widgets should implement {@link DraggableStackProviderWidget} to submit applicable stacks to drag.
 * Widgets should implement {@link DraggableStackVisitorWidget} to accept incoming dragged stacks.
 * <p>
 * External providers should use {@link me.shedaniel.rei.api.client.registry.screen.ScreenRegistry#registerDraggableStackProvider(DraggableStackProvider)},
 * and {@link me.shedaniel.rei.api.client.registry.screen.ScreenRegistry#registerDraggableStackVisitor(DraggableStackVisitor)}.
 */
public interface DraggingContext<S extends Screen> {
    static DraggingContext<?> getInstance() {
        return REIRuntime.getInstance().getOverlay().get().getDraggingContext();
    }
    
    /**
     * Returns whether a draggable stack is present.
     *
     * @return whether a draggable stack is present
     */
    default boolean isDraggingStack() {
        return getCurrentStack() != null;
    }
    
    S getScreen();
    
    /**
     * Returns the current dragged stack, may be null.
     *
     * @return the current dragged stack, may be null
     */
    @Nullable
    DraggableStack getCurrentStack();
    
    /**
     * Returns the current position of the dragged stack, this is usually the position of the mouse pointer,
     * but you should use this regardless to account for future changes.
     *
     * @return the current position of the dragged stack
     */
    @Nullable
    Point getCurrentPosition();
    
    /**
     * Renders the draggable stack back to the position {@code position}.
     * This may be used to animate an unaccepted draggable stack returning to its initial position.
     *
     * @param stack           the stack to use for render
     * @param initialPosition the initial position of the stack
     * @param position        the position supplier of the destination
     */
    void renderBackToPosition(DraggableStack stack, Point initialPosition, Supplier<Point> position);
    
    /**
     * Renders the draggable stack back to the bounds {@code bounds}.
     * This may be used to animate an unaccepted draggable stack returning to its initial position.
     *
     * @param stack           the stack to use for render
     * @param initialPosition the initial bounds of the stack
     * @param bounds          the boundary supplier of the destination
     */
    void renderBackToPosition(DraggableStack stack, Rectangle initialPosition, Supplier<Rectangle> bounds);
    
    default void renderToVoid(DraggableStack stack) {
        Point currentPosition = getCurrentPosition();
        Rectangle targetBounds = new Rectangle(currentPosition.x, currentPosition.y, 1, 1);
        renderBackToPosition(stack, new Rectangle(currentPosition.x - 8, currentPosition.y - 8, 16, 16), () -> targetBounds);
    }
    
    default <T extends Screen> DraggingContext<T> cast() {
        return (DraggingContext<T>) this;
    }
}
