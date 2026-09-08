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
package org.vaadin.tokenfield.client.ui;

import org.vaadin.tokenfield.TokenComboBox;
import org.vaadin.tokenfield.client.ui.VTokenField.DeleteListener;

import com.google.gwt.core.client.GWT;
import com.vaadin.client.communication.RpcProxy;
import com.vaadin.client.communication.StateChangeEvent;
import com.vaadin.client.ui.combobox.ComboBoxConnector;
import com.vaadin.shared.ui.Connect;

@Connect(TokenComboBox.class)
public class TokenFieldConnector extends ComboBoxConnector {

    private final TokenFieldServerRpc rpc = RpcProxy.create(
            TokenFieldServerRpc.class, this);

    @Override
    protected void init() {
        // GWT 2.7 (Vaadin 7.7) compiles this package at source level 1.7 -- no lambdas here.
        //noinspection Anonymous2MethodRef,Convert2Lambda
        getWidget().addListener(new DeleteListener() {
            @Override
            public void onDelete() {
                rpc.deleteToken();
            }
        });

    }

    @Override
    public TokenFieldState getState() {
        return (TokenFieldState) super.getState();
    }

    @Override
    public void onStateChanged(StateChangeEvent stateChangeEvent) {
        super.onStateChanged(stateChangeEvent);
        // Without this the widget never learns the insert position, and its
        // delete-key branch (DELETE for AFTER, BACKSPACE for BEFORE) is stuck
        // on the BEFORE half.
        getWidget().after = getState().after;
    }

    @Override
    public VTokenField getWidget() {
        return (VTokenField) super.getWidget();
    }

    @Override
    protected VTokenField createWidget() {
        // TODO Auto-generated method stub
        return GWT.create(VTokenField.class);
    }

}
