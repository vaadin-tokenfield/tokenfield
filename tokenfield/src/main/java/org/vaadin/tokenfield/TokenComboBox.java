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

import org.vaadin.tokenfield.client.ui.TokenFieldServerRpc;

import com.vaadin.server.PaintException;
import com.vaadin.server.PaintTarget;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.UI;

public abstract class TokenComboBox extends ComboBox {

    private static final long serialVersionUID = 8382983756053298383L;

    protected TokenField.InsertPosition insertPosition;

    /** Set when a dirty mark had to wait for the response to finish. */
    private boolean dirtyMarkPending;

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
        markAsDirtyWhenPossible();
    }

    /**
     * Marks this connector dirty, or arranges for it as soon as that is legal
     * again.
     * <p>
     * {@code ConnectorTracker.markDirty} throws outright once the response has
     * started being written, and this component is painted from application
     * code that can reach it at exactly that moment — see
     * {@code TokenFieldMarkAsDirtyWhileWritingResponseTest}. Holding the mark
     * back to {@link #beforeClientResponse(boolean)} keeps the change instead
     * of losing it, which is what Vaadin's own guarded state writes do
     * ({@code AbstractClientConnector.getState(boolean)}).
     * </p>
     */
    private void markAsDirtyWhenPossible() {
        UI ui = getUI();
        if (ui != null && ui.getConnectorTracker().isWritingResponse()) {
            dirtyMarkPending = true;
            return;
        }
        markAsDirty();
    }

    @Override
    public void beforeClientResponse(boolean initial) {
        if (dirtyMarkPending) {
            dirtyMarkPending = false;
            markAsDirty();
        }
        super.beforeClientResponse(initial);
    }

    protected abstract void onDelete();

}
