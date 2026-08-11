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

import java.util.logging.Level;
import java.util.logging.Logger;

import org.vaadin.tokenfield.client.ui.TokenFieldServerRpc;

import com.vaadin.data.Property;
import com.vaadin.server.PaintException;
import com.vaadin.server.PaintTarget;
import com.vaadin.ui.ComboBox;

public abstract class TokenComboBox extends ComboBox {

    private static final long serialVersionUID = 8382983756053298383L;

    protected TokenField.InsertPosition insertPosition;

    protected TokenComboBox(TokenField.InsertPosition insertPosition) {
        this.insertPosition = insertPosition;
        TokenFieldServerRpc rpc = this::onDelete;
        registerRpc(rpc);
    }

    @Override
    public void paintContent(PaintTarget target) throws PaintException {
        super.paintContent(target);
        target.addVariable(this, "del", false);
        if (insertPosition == TokenField.InsertPosition.AFTER) {
            target.addAttribute("after", true);
        }
    }

    public void setTokenInsertPosition(TokenField.InsertPosition insertPosition) {
        this.insertPosition = insertPosition;
        markAsDirty();
    }

    /*
     * A select is normally only ever asked about ids that came out of its own
     * container. A TokenField deliberately holds tokens the container does not
     * contain, and resolving their icon asks anyway. A container keyed by a
     * specific type answers such a question by throwing rather than by
     * reporting the id as absent - a JPAContainer keyed by Long, asked about a
     * String, fails converting it. The lookup below therefore reads "no such
     * item" out of that refusal, which is what the caller means to ask.
     * <p>
     * Overridden rather than wrapped at the call site because AbstractSelect
     * reaches it from inside getItemIcon, where TokenField cannot intercept.
     */

    @Override
    public Property getContainerProperty(Object itemId, Object propertyId) {
        try {
            return super.getContainerProperty(itemId, propertyId);
        } catch (RuntimeException e) {
            logForeignId(itemId, e);
            return null;
        }
    }

    private static void logForeignId(Object itemId, RuntimeException e) {
        Logger.getLogger(TokenComboBox.class.getName()).log(Level.FINE,
                e, () -> "Container rejected the token id " + itemId
                        + "; treating it as not contained");
    }

    protected abstract void onDelete();

}
