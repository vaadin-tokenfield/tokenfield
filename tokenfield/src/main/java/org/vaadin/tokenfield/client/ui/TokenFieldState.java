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

import com.vaadin.shared.ui.combobox.ComboBoxState;

/**
 * Shared state of the token input, adding the token insert position to what a
 * ComboBox already carries.
 */
public class TokenFieldState extends ComboBoxState {

    private static final long serialVersionUID = 1L;

    /**
     * True when the token buttons are placed after the input
     * ({@code TokenField.InsertPosition.AFTER}), which is what decides whether
     * the delete key or the backspace key removes the last token.
     */
    public boolean after = false;
}
