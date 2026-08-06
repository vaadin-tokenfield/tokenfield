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
import org.vaadin.tokenfield.client.ui.TokenFieldState;

import com.vaadin.ui.ComboBox;

public abstract class TokenComboBox extends ComboBox {

    private static final long serialVersionUID = 8382983756053298383L;

    protected TokenField.InsertPosition insertPosition;

    protected TokenComboBox(TokenField.InsertPosition insertPosition) {
        this.insertPosition = insertPosition;
        getState().after = isAfter();
        TokenFieldServerRpc rpc = this::onDelete;
        registerRpc(rpc);
    }

    @Override
    protected TokenFieldState getState() {
        return (TokenFieldState) super.getState();
    }

    @Override
    protected TokenFieldState getState(boolean markAsDirty) {
        return (TokenFieldState) super.getState(markAsDirty);
    }

    public void setTokenInsertPosition(TokenField.InsertPosition insertPosition) {
        this.insertPosition = insertPosition;
        // Writing shared state already marks the connector dirty.
        getState().after = isAfter();
    }

    private boolean isAfter() {
        return insertPosition == TokenField.InsertPosition.AFTER;
    }

    protected abstract void onDelete();

}
