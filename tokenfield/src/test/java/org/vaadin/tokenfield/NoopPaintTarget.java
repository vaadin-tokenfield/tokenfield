package org.vaadin.tokenfield;

import com.vaadin.server.ClientConnector;
import com.vaadin.server.PaintTarget;
import com.vaadin.server.Resource;
import com.vaadin.server.StreamVariable;
import com.vaadin.server.VariableOwner;
import com.vaadin.ui.Component;

import java.util.Map;

/**
 * A {@link PaintTarget} that discards everything written to it.
 *
 * <p>Painting is normally driven by {@code UidlWriter}, which needs a servlet
 * request, a deployment configuration and a communication manager. Tests that
 * only care about what a component <em>does</em> while it paints — not about
 * the UIDL it produces — can call {@code paintContent} directly with this
 * target instead of standing all of that up.</p>
 */
class NoopPaintTarget implements PaintTarget {

    @Override
    public void addSection(String sectionTagName, String sectionData) {
    }

    @Override
    public void startTag(String tagName) {
    }

    @Override
    public void endTag(String tagName) {
    }

    @Override
    public void addAttribute(String name, boolean value) {
    }

    @Override
    public void addAttribute(String name, int value) {
    }

    @Override
    public void addAttribute(String name, long value) {
    }

    @Override
    public void addAttribute(String name, float value) {
    }

    @Override
    public void addAttribute(String name, double value) {
    }

    @Override
    public void addAttribute(String name, String value) {
    }

    @Override
    public void addAttribute(String name, Resource value) {
    }

    @Override
    public void addAttribute(String name, Map<?, ?> value) {
    }

    @Override
    public void addAttribute(String name, Component value) {
    }

    @Override
    public void addAttribute(String name, Object[] values) {
    }

    @Override
    public void addVariable(VariableOwner owner, String name, String value) {
    }

    @Override
    public void addVariable(VariableOwner owner, String name, int value) {
    }

    @Override
    public void addVariable(VariableOwner owner, String name, long value) {
    }

    @Override
    public void addVariable(VariableOwner owner, String name, float value) {
    }

    @Override
    public void addVariable(VariableOwner owner, String name, double value) {
    }

    @Override
    public void addVariable(VariableOwner owner, String name, boolean value) {
    }

    @Override
    public void addVariable(VariableOwner owner, String name, String[] value) {
    }

    @Override
    public void addVariable(VariableOwner owner, String name, Component value) {
    }

    @Override
    public void addVariable(VariableOwner owner, String name, StreamVariable value) {
    }

    @Override
    public void addUploadStreamVariable(VariableOwner owner, String name) {
    }

    @Override
    public void addXMLSection(String sectionTagName, String sectionData, String namespace) {
    }

    @Override
    public void addUIDL(String uidl) {
    }

    @Override
    public void addText(String text) {
    }

    @Override
    public void addCharacterData(String text) {
    }

    @Override
    public String getTag(ClientConnector connector) {
        return "noop";
    }

    @Override
    public boolean isFullRepaint() {
        return false;
    }

    @Override
    public PaintStatus startPaintable(Component connector, String tagName) {
        return PaintStatus.PAINTING;
    }

    @Override
    public void endPaintable(Component connector) {
    }
}
