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

public abstract class TokenComboBox extends ComboBox {

    private static final long serialVersionUID = 8382983756053298383L;

    protected TokenField.InsertPosition insertPosition;

    private TokenFieldServerRpc rpc = new TokenFieldServerRpc() {
        public void deleteToken() {
            onDelete();
        }
    };

    public TokenComboBox(TokenField.InsertPosition insertPosition) {
        this.insertPosition = insertPosition;
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
        requestRepaint();
    }

    abstract protected void onDelete();

}
