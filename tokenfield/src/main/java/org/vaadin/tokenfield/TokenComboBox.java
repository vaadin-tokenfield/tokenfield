/*
 * Copyright 2010-2013 Marc Englund
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.vaadin.tokenfield;

import static java.util.Objects.requireNonNull;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.vaadin.tokenfield.client.ui.TokenFieldServerRpc;

import com.vaadin.server.PaintException;
import com.vaadin.server.PaintTarget;
import com.vaadin.server.Resource;
import com.vaadin.ui.ComboBox;

@NullMarked
public abstract class TokenComboBox extends ComboBox {

    private static final long serialVersionUID = 8382983756053298383L;

    protected TokenField.InsertPosition insertPosition;

    /**
     * Explicit caption override for the {@code null} item id.
     * <p>
     * {@link #setItemCaption(Object, String)} silently ignores a
     * {@code null} itemId (see {@code AbstractSelect}'s own implementation),
     * so it cannot back this the way it backs every other item's explicit
     * caption; {@link #setItemCaption}/{@link #getItemCaption} are
     * overridden here to manage it instead, applied under the same modes
     * ({@link ItemCaptionMode#EXPLICIT}/{@link ItemCaptionMode#EXPLICIT_DEFAULTS_ID})
     * an explicit caption applies under for any other item id.
     * </p>
     */
    private @Nullable String nullItemCaption;

    /**
     * Explicit icon override for the {@code null} item id, for the same
     * reason as {@link #nullItemCaption} - see {@link #setItemIcon(Object,
     * Resource)}. Icons are not caption-mode-gated, so this applies
     * unconditionally, matching {@code AbstractSelect#getItemIcon(Object)}.
     */
    private @Nullable Resource nullItemIcon;

    protected TokenComboBox(TokenField.InsertPosition insertPosition) {
        this.insertPosition = requireNonNull(insertPosition);
        TokenFieldServerRpc rpc = this::onDelete;
        registerRpc(rpc);
    }

    @Override
    public void paintContent(PaintTarget target) throws PaintException {
        super.paintContent(requireNonNull(target));
        target.addVariable(this, "del", false);
        if (insertPosition == TokenField.InsertPosition.AFTER) {
            target.addAttribute("after", true);
        }
    }

    public void setTokenInsertPosition(TokenField.InsertPosition insertPosition) {
        this.insertPosition = requireNonNull(insertPosition);
        markAsDirty();
    }

    @Override
    public @Nullable String getItemCaption(@Nullable Object itemId) {
        if (itemId == null && nullItemCaption != null
                && (getItemCaptionMode() == ItemCaptionMode.EXPLICIT
                        || getItemCaptionMode() == ItemCaptionMode.EXPLICIT_DEFAULTS_ID)) {
            return nullItemCaption;
        }
        return super.getItemCaption(itemId);
    }

    @Override
    public void setItemCaption(@Nullable Object itemId, @Nullable String caption) {
        if (itemId == null) {
            nullItemCaption = caption;
        } else {
            super.setItemCaption(itemId, caption);
        }
    }

    @Override
    public @Nullable Resource getItemIcon(@Nullable Object itemId) {
        if (itemId == null && nullItemIcon != null) {
            return nullItemIcon;
        }
        return super.getItemIcon(itemId);
    }

    @Override
    public void setItemIcon(@Nullable Object itemId, @Nullable Resource icon) {
        if (itemId == null) {
            nullItemIcon = icon;
        } else {
            super.setItemIcon(itemId, icon);
        }
    }

    protected abstract void onDelete();

}
