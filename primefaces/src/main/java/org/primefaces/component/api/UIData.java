/*
 * The MIT License
 *
 * Copyright (c) 2009-2021 PrimeTek
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.primefaces.component.api;

import java.io.IOException;
import java.sql.ResultSet;
import java.util.*;

import javax.faces.FacesException;
import javax.faces.application.Application;
import javax.faces.application.FacesMessage;
import javax.faces.application.StateManager;
import javax.faces.component.*;
import javax.faces.component.visit.VisitCallback;
import javax.faces.component.visit.VisitContext;
import javax.faces.component.visit.VisitResult;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseId;
import javax.faces.event.PostValidateEvent;
import javax.faces.event.PreRenderComponentEvent;
import javax.faces.event.PreValidateEvent;
import javax.faces.model.*;
import javax.faces.render.Renderer;

import org.primefaces.component.column.Column;
import org.primefaces.component.columngroup.ColumnGroup;
import org.primefaces.component.columns.Columns;
import org.primefaces.model.CollectionDataModel;
import org.primefaces.model.IterableDataModel;
import org.primefaces.model.LazyDataModel;
import org.primefaces.util.ComponentTraversalUtils;
import org.primefaces.util.ComponentUtils;
import org.primefaces.util.ELUtils;
import org.primefaces.util.SharedStringBuilder;

/**
 * Enhanced version of the JSF UIData.
 * It also contains some methods of the Mojarra impl (e.g. setRowIndexRowStatePreserved), maybe can remove it in the future.
 */
@SuppressWarnings("unchecked")
public class UIData extends javax.faces.component.UIData {

    private static final String SB_ID = UIData.class.getName() + "#id";

    private final Map<String, Object> _rowTransientStates = new HashMap<>();
    private Map<String, Object> _rowDeltaStates = new HashMap<>();
    private Object _initialDescendantFullComponentState;

    private String clientId;
    private DataModel model;
    private Boolean isNested;
    private Object oldVar;

    public enum PropertyKeys {
        rowIndex,
        rowIndexVar,
        saved,
        lazy,
        rowStatePreserved
    }

    public boolean isLazy() {
        return ComponentUtils.eval(getStateHelper(), PropertyKeys.lazy, () -> {
            // if not set by xhtml, we need to check the type of the value binding
            Class<?> type = ELUtils.getType(getFacesContext(),
                    getValueExpression("value"),
                    () -> getValue());
            boolean lazy = LazyDataModel.class.isAssignableFrom(type);

            // remember in ViewState, to not do the same check again
            setLazy(lazy);

            return lazy;
        });
    }

    public void setLazy(boolean lazy) {
        getStateHelper().put(PropertyKeys.lazy, lazy);
    }

    public String getRowIndexVar() {
        return (String) getStateHelper().eval(PropertyKeys.rowIndexVar, null);
    }

    public void setRowIndexVar(String rowIndexVar) {
        getStateHelper().put(PropertyKeys.rowIndexVar, rowIndexVar);
    }

    @Override
    public boolean isRowStatePreserved() {
        return (Boolean) getStateHelper().eval(PropertyKeys.rowStatePreserved, false);
    }

    @Override
    public void setRowStatePreserved(boolean rowStatePreserved) {
        getStateHelper().put(PropertyKeys.rowStatePreserved, rowStatePreserved);
    }

    @Override
    public void processDecodes(FacesContext context) {
        if (!isRendered()) {
            return;
        }

        pushComponentToEL(context, this);
        preDecode(context);
        processPhase(context, PhaseId.APPLY_REQUEST_VALUES);
        decode(context);
        popComponentFromEL(context);
    }

    @Override
    public void processValidators(FacesContext context) {
        if (!isRendered()) {
            return;
        }

        pushComponentToEL(context, this);
        Application app = context.getApplication();
        app.publishEvent(context, PreValidateEvent.class, this);
        preValidate(context);
        processPhase(context, PhaseId.PROCESS_VALIDATIONS);
        app.publishEvent(context, PostValidateEvent.class, this);
        popComponentFromEL(context);
    }

    @Override
    public void processUpdates(FacesContext context) {
        if (!isRendered()) {
            return;
        }

        pushComponentToEL(context, this);
        preUpdate(context);
        processPhase(context, PhaseId.UPDATE_MODEL_VALUES);
        popComponentFromEL(context);
    }

    protected void processPhase(FacesContext context, PhaseId phaseId) {
        processFacets(context, phaseId);
        if (requiresColumns()) {
            processColumnFacets(context, phaseId);
        }

        if (shouldSkipChildren(context)) {
            return;
        }

        setRowIndex(-1);
        processChildren(context, phaseId);
        setRowIndex(-1);
    }

    protected void processFacets(FacesContext context, PhaseId phaseId) {
        if (getFacetCount() > 0) {
            for (UIComponent facet : getFacets().values()) {
                process(context, facet, phaseId);
            }
        }
    }

    protected void processColumnFacets(FacesContext context, PhaseId phaseId) {
        for (int i = 0; i < getChildCount(); i++) {
            UIComponent child = getChildren().get(i);
            if (child.isRendered() && (child.getFacetCount() > 0)) {
                for (UIComponent facet : child.getFacets().values()) {
                    process(context, facet, phaseId);
                }
            }
        }
    }

    protected boolean shouldProcessChild(FacesContext context, int rowIndex, PhaseId phaseId ) {
        return true;
    }

    protected void processChildren(FacesContext context, PhaseId phaseId) {
        int first = getFirst();
        int rows = getRows();
        int last = rows == 0 ? getRowCount() : (first + rows);

        List<UIComponent> iterableChildren = null;

        for (int rowIndex = first; rowIndex < last; rowIndex++) {
            setRowIndex(rowIndex);

            if (!isRowAvailable()) {
                break;
            }

            if (!shouldProcessChild(context, rowIndex, phaseId)) {
                continue;
            }

            if (iterableChildren == null) {
                iterableChildren = getIterableChildren();
            }

            for (int i = 0; i < iterableChildren.size(); i++) {
                UIComponent child = iterableChildren.get(i);
                if (child.isRendered()) {
                    if (child instanceof Column) {
                        for (UIComponent grandkid : child.getChildren()) {
                            process(context, grandkid, phaseId);
                        }
                    }
                    else {
                        process(context, child, phaseId);
                    }
                }
            }
        }
    }

    protected void process(FacesContext context, UIComponent component, PhaseId phaseId) {
        if (phaseId == PhaseId.APPLY_REQUEST_VALUES) {
            component.processDecodes(context);
        }
        else if (phaseId == PhaseId.PROCESS_VALIDATIONS) {
            component.processValidators(context);
        }
        else if (phaseId == PhaseId.UPDATE_MODEL_VALUES) {
            component.processUpdates(context);
        }

    }

    @Override
    public String getClientId(FacesContext context) {
        if (clientId != null) {
            return clientId;
        }

        String id = getId();
        if (id == null) {
            UniqueIdVendor parentUniqueIdVendor = ComponentTraversalUtils.closestUniqueIdVendor(this);

            if (parentUniqueIdVendor == null) {
                UIViewRoot viewRoot = context.getViewRoot();

                if (viewRoot != null) {
                    id = viewRoot.createUniqueId();
                }
                else {
                    throw new FacesException("Cannot create clientId for " + getClass().getCanonicalName());
                }
            }
            else {
                id = parentUniqueIdVendor.createUniqueId(context, null);
            }

            setId(id);
        }

        UIComponent namingContainer = ComponentTraversalUtils.closestNamingContainer(this);
        if (namingContainer != null) {
            String containerClientId = namingContainer.getContainerClientId(context);

            if (containerClientId != null) {
                StringBuilder sb = SharedStringBuilder.get(context, SB_ID, containerClientId.length() + 10);
                clientId = sb.append(containerClientId).append(UINamingContainer.getSeparatorChar(context)).append(id).toString();
            }
            else {
                clientId = id;
            }
        }
        else {
            clientId = id;
        }

        Renderer renderer = getRenderer(context);
        if (renderer != null) {
            clientId = renderer.convertClientId(context, clientId);
        }

        return clientId;
    }

    @Override
    public String getContainerClientId(FacesContext context) {
        //clientId is without rowIndex
        String componentClientId = getClientId(context);

        int rowIndex = getRowIndex();
        if (rowIndex == -1) {
            return componentClientId;
        }

        StringBuilder sb = SharedStringBuilder.get(context, SB_ID, componentClientId.length() + 4);
        String containerClientId = sb.append(componentClientId).append(UINamingContainer.getSeparatorChar(context)).append(rowIndex).toString();

        return containerClientId;
    }

    @Override
    public void setId(String id) {
        super.setId(id);

        //clear
        clientId = null;
    }

    //Row State preserved implementation is taken from Mojarra
    private void setRowIndexRowStatePreserved(int rowIndex) {
        if (rowIndex < -1) {
            throw new IllegalArgumentException("rowIndex is less than -1");
        }

        if (getRowIndex() == rowIndex) {
            return;
        }

        FacesContext facesContext = getFacesContext();

        if (_initialDescendantFullComponentState != null) {
            //Just save the row
            Map<String, Object> sm = saveFullDescendantComponentStates(facesContext, null, getChildren().iterator(), false);
            if (sm != null && !sm.isEmpty()) {
                _rowDeltaStates.put(getContainerClientId(facesContext), sm);
            }

            if (getRowIndex() != -1) {
                _rowTransientStates.put(getContainerClientId(facesContext),
                               saveTransientDescendantComponentStates(facesContext, null, getChildren().iterator(), false));
            }
        }

        // Update to the new row index
        //this.rowIndex = rowIndex;
        getStateHelper().put(PropertyKeys.rowIndex, rowIndex);
        DataModel localModel = getDataModel();
        localModel.setRowIndex(rowIndex);

        // if rowIndex is -1, clear the cache
        if (rowIndex == -1) {
            setDataModel(null);
        }

        // Clear or expose the current row data as a request scope attribute
        String var = getVar();
        if (var != null) {
            Map<String, Object> requestMap
                    = getFacesContext().getExternalContext().getRequestMap();
            if (rowIndex == -1) {
                oldVar = requestMap.remove(var);
            }
            else if (isRowAvailable()) {
                requestMap.put(var, getRowData());
            }
            else {
                requestMap.remove(var);
                if (null != oldVar) {
                    requestMap.put(var, oldVar);
                    oldVar = null;
                }
            }
        }

        if (_initialDescendantFullComponentState != null) {
            Object rowState = _rowDeltaStates.get(getContainerClientId(facesContext));
            if (rowState == null) {
                //Restore as original
                restoreFullDescendantComponentStates(facesContext, getChildren().iterator(), _initialDescendantFullComponentState, false);
            }
            else {
                //Restore first original and then delta
                restoreFullDescendantComponentDeltaStates(facesContext, getChildren().iterator(), rowState, _initialDescendantFullComponentState, false);
            }
            if (getRowIndex() == -1) {
                restoreTransientDescendantComponentStates(facesContext, getChildren().iterator(), null, false);
            }
            else {
                rowState = _rowTransientStates.get(getContainerClientId(facesContext));
                if (rowState == null) {
                    restoreTransientDescendantComponentStates(facesContext, getChildren().iterator(), null, false);
                }
                else {
                    restoreTransientDescendantComponentStates(facesContext, getChildren().iterator(), (Map<String, Object>) rowState, false);
                }
            }
        }
    }

    private void setRowIndexWithoutRowStatePreserved(int rowIndex) {
        saveDescendantState();
        setRowModel(rowIndex);
        restoreDescendantState();
    }

    public void setRowModel(int rowIndex) {
        //update rowIndex
        getStateHelper().put(PropertyKeys.rowIndex, rowIndex);
        getDataModel().setRowIndex(rowIndex);

        //clear datamodel
        if (rowIndex == -1) {
            setDataModel(null);
        }

        //update var
        String var = getVar();
        if (var != null) {
            String rowIndexVar = getRowIndexVar();
            Map<String, Object> requestMap = getFacesContext().getExternalContext().getRequestMap();

            if (rowIndex == -1) {
                oldVar = requestMap.remove(var);

                if (rowIndexVar != null) {
                    requestMap.remove(rowIndexVar);
                }
            }
            else if (isRowAvailable()) {
                requestMap.put(var, getRowData());

                if (rowIndexVar != null) {
                    requestMap.put(rowIndexVar, rowIndex);
                }
            }
            else {
                requestMap.remove(var);

                if (rowIndexVar != null) {
                    requestMap.put(rowIndexVar, rowIndex);
                }

                if (oldVar != null) {
                    requestMap.put(var, oldVar);
                    oldVar = null;
                }
            }
        }
    }

    @Override
    public int getRowIndex() {
        return (Integer) getStateHelper().eval(PropertyKeys.rowIndex, -1);
    }

    @Override
    public void setRowIndex(int rowIndex) {
        if (isRowStatePreserved()) {
            setRowIndexRowStatePreserved(rowIndex);
        }
        else {
            setRowIndexWithoutRowStatePreserved(rowIndex);
        }
    }

    protected void saveDescendantState() {
        FacesContext context = getFacesContext();

        if (getChildCount() > 0) {
            for (int i = 0; i < getChildCount(); i++) {
                UIComponent kid = getChildren().get(i);
                saveDescendantState(kid, context);
            }
        }

        if (getFacetCount() > 0) {
            for (UIComponent facet : getFacets().values()) {
                saveDescendantState(facet, context);
            }
        }
    }

    protected void saveDescendantState(UIComponent component, FacesContext context) {
        Map<String, SavedState> saved = (Map<String, SavedState>) getStateHelper().get(PropertyKeys.saved);

        if (component instanceof EditableValueHolder) {
            EditableValueHolder input = (EditableValueHolder) component;
            SavedState state = null;
            String componentClientId = component.getClientId(context);

            if (saved == null) {
                state = new SavedState();
                getStateHelper().put(PropertyKeys.saved, componentClientId, state);
            }

            if (state == null) {
                state = saved.get(componentClientId);

                if (state == null) {
                    state = new SavedState();
                    getStateHelper().put(PropertyKeys.saved, componentClientId, state);
                }
            }

            state.setValue(input.getLocalValue());
            state.setValid(input.isValid());
            state.setSubmittedValue(input.getSubmittedValue());
            state.setLocalValueSet(input.isLocalValueSet());
        }
        else if (component instanceof UIForm) {
            UIForm form = (UIForm) component;
            String componentClientId = component.getClientId(context);
            SavedState state = null;
            if (saved == null) {
                state = new SavedState();
                getStateHelper().put(PropertyKeys.saved, componentClientId, state);
            }

            if (state == null) {
                state = saved.get(componentClientId);
                if (state == null) {
                    state = new SavedState();
                    //saved.put(clientId, state);
                    getStateHelper().put(PropertyKeys.saved, componentClientId, state);
                }
            }
            state.setSubmitted(form.isSubmitted());
        }

        //save state for children
        if (component.getChildCount() > 0) {
            for (int i = 0; i < component.getChildCount(); i++) {
                UIComponent kid = component.getChildren().get(i);
                saveDescendantState(kid, context);
            }
        }

        //save state for facets
        if (component.getFacetCount() > 0) {
            for (UIComponent facet : component.getFacets().values()) {
                saveDescendantState(facet, context);
            }
        }

    }

    protected void restoreDescendantState() {
        FacesContext context = getFacesContext();

        if (getChildCount() > 0) {
            for (int i = 0; i < getChildCount(); i++) {
                UIComponent kid = getChildren().get(i);
                restoreDescendantState(kid, context);
            }
        }

        if (getFacetCount() > 0) {
            for (UIComponent facet : getFacets().values()) {
                restoreDescendantState(facet, context);
            }
        }
    }

    protected void restoreDescendantState(UIComponent component, FacesContext context) {
        String id = component.getId();
        component.setId(id); //reset the client id
        Map<String, SavedState> saved = (Map<String, SavedState>) getStateHelper().get(PropertyKeys.saved);

        if (component instanceof EditableValueHolder) {
            EditableValueHolder input = (EditableValueHolder) component;
            String componentClientId = component.getClientId(context);

            SavedState state = saved.get(componentClientId);
            if (state == null) {
                state = new SavedState();
            }

            input.setValue(state.getValue());
            input.setValid(state.isValid());
            input.setSubmittedValue(state.getSubmittedValue());
            input.setLocalValueSet(state.isLocalValueSet());
        }
        else if (component instanceof UIForm) {
            UIForm form = (UIForm) component;
            String componentClientId = component.getClientId(context);
            SavedState state = saved.get(componentClientId);
            if (state == null) {
                state = new SavedState();
            }

            form.setSubmitted(state.getSubmitted());
            state.setSubmitted(form.isSubmitted());
        }

        //restore state of children
        if (component.getChildCount() > 0) {
            for (int i = 0; i < component.getChildCount(); i++) {
                UIComponent kid = component.getChildren().get(i);
                restoreDescendantState(kid, context);
            }
        }

        //restore state of facets
        if (component.getFacetCount() > 0) {
            for (UIComponent facet : component.getFacets().values()) {
                restoreDescendantState(facet, context);
            }
        }

    }

    @Override
    protected DataModel getDataModel() {
        if (model != null) {
            return (model);
        }

        Object current = getValue();
        if (current == null) {
            setDataModel(new ListDataModel(Collections.emptyList()));
        }
        else if (current instanceof DataModel) {
            setDataModel((DataModel) current);
        }
        else if (current instanceof List) {
            setDataModel(new ListDataModel((List) current));
        }
        else if (Object[].class.isAssignableFrom(current.getClass())) {
            setDataModel(new ArrayDataModel((Object[]) current));
        }
        else if (current instanceof ResultSet) {
            setDataModel(new ResultSetDataModel((ResultSet) current));
        }
        else if (current instanceof Collection) {
            setDataModel(new CollectionDataModel((Collection) current));
        }
        else if (current instanceof Iterable) {
            setDataModel(new IterableDataModel((Iterable<?>) current));
        }
        else if (current instanceof Map) {
            setDataModel(new IterableDataModel(((Map<?, ?>) current).entrySet()));
        }
        else {
            setDataModel(new ScalarDataModel(current));
        }

        return model;
    }

    @Override
    protected void setDataModel(DataModel dataModel) {
        model = dataModel;
    }

    protected boolean shouldSkipChildren(FacesContext context) {
        return false;
    }

    protected boolean shouldVisitChildren(VisitContext context, boolean visitRows) {
        if (visitRows) {
            setRowIndex(-1);
        }

        Collection<String> idsToVisit = context.getSubtreeIdsToVisit(this);

        return (!idsToVisit.isEmpty());
    }

    @Override
    public boolean invokeOnComponent(FacesContext context, String clientId, ContextCallback callback)
            throws FacesException {

        // skip if the component is not a children of the UIData
        if (!clientId.startsWith(getClientId(context))) {
            return false;
        }

        return super.invokeOnComponent(context, clientId, callback);
    }

    @Override
    public boolean visitTree(VisitContext context, VisitCallback callback) {
        if (!isVisitable(context)) {
            return false;
        }

        FacesContext facesContext = context.getFacesContext();
        boolean visitRows = !ComponentUtils.isSkipIteration(context, facesContext);

        int rowIndex = -1;
        if (visitRows) {
            rowIndex = getRowIndex();
            setRowIndex(-1);
        }

        pushComponentToEL(facesContext, null);

        try {
            VisitResult result = context.invokeVisitCallback(this, callback);

            if (result == VisitResult.COMPLETE) {
                return true;
            }

            if ((result == VisitResult.ACCEPT) && shouldVisitChildren(context, visitRows)) {
                if (visitFacets(context, callback, visitRows)) {
                    return true;
                }

                if (requiresColumns() && visitColumnsAndColumnFacets(context, callback, visitRows)) {
                    return true;
                }

                if (visitRows(context, callback, visitRows)) {
                    return true;
                }

            }
        }
        finally {
            popComponentFromEL(facesContext);

            if (visitRows) {
                setRowIndex(rowIndex);
            }
        }

        return false;
    }

    protected boolean visitFacets(VisitContext context, VisitCallback callback, boolean visitRows) {
        if (visitRows) {
            setRowIndex(-1);
        }

        if (getFacetCount() > 0) {
            for (UIComponent facet : getFacets().values()) {
                if (facet.visitTree(context, callback)) {
                    return true;
                }
            }
        }

        return false;
    }

    protected boolean visitColumnsAndColumnFacets(VisitContext context, VisitCallback callback, boolean visitRows) {
        if (visitRows) {
            setRowIndex(-1);
        }

        if (getChildCount() > 0) {
            for (int i = 0; i < getChildCount(); i++) {
                UIComponent child = getChildren().get(i);
                VisitResult result = context.invokeVisitCallback(child, callback); // visit the column directly
                if (result == VisitResult.COMPLETE) {
                    return true;
                }

                if (child instanceof UIColumn) {
                    if (child.getFacetCount() > 0) {
                        if (child instanceof Columns) {
                            Columns columns = (Columns) child;
                            for (int j = 0; j < columns.getRowCount(); j++) {
                                columns.setRowIndex(j);
                                boolean value = visitColumnFacets(context, callback, child);
                                if (value) {
                                    return true;
                                }
                            }
                            columns.setRowIndex(-1);
                        }
                        else {
                            boolean value = visitColumnFacets(context, callback, child);
                            if (value) {
                                return true;
                            }
                        }

                    }
                }
                else if (child instanceof ColumnGroup) {
                    visitColumnGroup(context, callback, (ColumnGroup) child);
                }
            }
        }

        return false;
    }

    protected boolean visitColumnGroup(VisitContext context, VisitCallback callback, ColumnGroup group) {
        if (group.getChildCount() > 0) {
            for (int i = 0; i < group.getChildCount(); i++) {
                UIComponent row = group.getChildren().get(i);
                if (row.getChildCount() > 0) {
                    for (int j = 0; j < row.getChildCount(); j++) {
                        UIComponent col = row.getChildren().get(j);
                        if (col instanceof Column) {
                            boolean value = visitColumnFacets(context, callback, col);
                            if (value) {
                                return true;
                            }
                        }
                        else if (col instanceof Columns) {
                            if (col.getFacetCount() > 0) {
                                Columns columns = (Columns) col;
                                for (int k = 0; k < columns.getRowCount(); k++) {
                                    columns.setRowIndex(k);

                                    boolean value = visitColumnFacets(context, callback, columns);
                                    if (value) {
                                        columns.setRowIndex(-1);
                                        return true;
                                    }
                                }

                                columns.setRowIndex(-1);
                            }
                        }
                    }
                }
            }
        }

        return false;
    }

    protected boolean visitColumnFacets(VisitContext context, VisitCallback callback, UIComponent component) {
        if (component.getFacetCount() > 0) {
            for (UIComponent columnFacet : component.getFacets().values()) {
                if (columnFacet.visitTree(context, callback)) {
                    return true;
                }
            }
        }

        return false;
    }

    protected boolean visitRows(VisitContext context, VisitCallback callback, boolean visitRows) {
        boolean requiresColumns = requiresColumns();
        int processed = 0;
        int rowIndex = 0;
        int rows = 0;
        if (visitRows) {
            rowIndex = getFirst() - 1;
            rows = getRows();
        }

        while (true) {
            if (visitRows) {
                if ((rows > 0) && (++processed > rows)) {
                    break;
                }

                setRowIndex(++rowIndex);
                if (!isRowAvailable()) {
                    break;
                }
            }

            if (getChildCount() > 0) {
                for (int i = 0; i < getChildCount(); i++) {
                    UIComponent kid = getChildren().get(i);
                    if (requiresColumns) {
                        if (kid instanceof Columns) {
                            Columns columns = (Columns) kid;
                            for (int j = 0; j < columns.getRowCount(); j++) {
                                columns.setRowIndex(j);

                                boolean value = visitColumnContent(context, callback, columns);
                                if (value) {
                                    columns.setRowIndex(-1);
                                    return true;
                                }
                            }

                            columns.setRowIndex(-1);
                        }
                        else {
                            boolean value = visitColumnContent(context, callback, kid);
                            if (value) {
                                return true;
                            }
                        }
                    }
                    else {
                        if (kid.visitTree(context, callback)) {
                            return true;
                        }
                    }
                }
            }

            if (!visitRows) {
                break;
            }

        }

        return false;
    }

    protected boolean visitColumnContent(VisitContext context, VisitCallback callback, UIComponent component) {
        if (component.getChildCount() > 0) {
            for (int i = 0; i < component.getChildCount(); i++) {
                UIComponent grandkid = component.getChildren().get(i);
                if (grandkid.visitTree(context, callback)) {
                    return true;
                }
            }
        }

        return false;
    }

    protected boolean requiresColumns() {
        return false;
    }

    protected List<UIComponent> getIterableChildren() {
        return getChildren();
    }

    @Override
    public void markInitialState() {
        if (isRowStatePreserved()) {
            if (getFacesContext().getAttributes().containsKey(StateManager.IS_BUILDING_INITIAL_STATE)) {
                _initialDescendantFullComponentState = saveDescendantInitialComponentStates(getFacesContext(), getChildren().iterator(), false);
            }
        }
        super.markInitialState();
    }

    private void restoreFullDescendantComponentStates(FacesContext facesContext,
                                                      Iterator<UIComponent> childIterator, Object state,
                                                      boolean restoreChildFacets) {
        Iterator<? extends Object[]> descendantStateIterator = null;
        while (childIterator.hasNext()) {
            if (descendantStateIterator == null && state != null) {
                descendantStateIterator = ((Collection<? extends Object[]>) state)
                        .iterator();
            }
            UIComponent component = childIterator.next();

            // reset the client id (see spec 3.1.6)
            component.setId(component.getId());
            if (!component.isTransient()) {
                Object childState = null;
                Object descendantState = null;
                if (descendantStateIterator != null
                        && descendantStateIterator.hasNext()) {
                    Object[] object = descendantStateIterator.next();
                    childState = object[0];
                    descendantState = object[1];
                }

                component.clearInitialState();
                component.restoreState(facesContext, childState);
                component.markInitialState();

                Iterator<UIComponent> childsIterator;
                if (restoreChildFacets) {
                    childsIterator = component.getFacetsAndChildren();
                }
                else {
                    childsIterator = component.getChildren().iterator();
                }
                restoreFullDescendantComponentStates(facesContext, childsIterator,
                        descendantState, true);
            }
        }
    }

    private Collection<Object[]> saveDescendantInitialComponentStates(FacesContext facesContext,
                                                                      Iterator<UIComponent> childIterator, boolean saveChildFacets) {
        Collection<Object[]> childStates = null;
        while (childIterator.hasNext()) {
            if (childStates == null) {
                childStates = new ArrayList<>();
            }

            UIComponent child = childIterator.next();
            if (!child.isTransient()) {
                // Add an entry to the collection, being an array of two
                // elements. The first element is the state of the children
                // of this component; the second is the state of the current
                // child itself.

                Iterator<UIComponent> childsIterator;
                if (saveChildFacets) {
                    childsIterator = child.getFacetsAndChildren();
                }
                else {
                    childsIterator = child.getChildren().iterator();
                }
                Object descendantState = saveDescendantInitialComponentStates(
                        facesContext, childsIterator, true);
                Object state = null;
                if (child.initialStateMarked()) {
                    child.clearInitialState();
                    state = child.saveState(facesContext);
                    child.markInitialState();
                }
                else {
                    state = child.saveState(facesContext);
                }
                childStates.add(new Object[]{state, descendantState});
            }
        }
        return childStates;
    }

    private Map<String, Object> saveFullDescendantComponentStates(FacesContext facesContext, Map<String, Object> stateMap,
                                                                  Iterator<UIComponent> childIterator, boolean saveChildFacets) {
        while (childIterator.hasNext()) {
            UIComponent child = childIterator.next();
            if (!child.isTransient()) {
                Iterator<UIComponent> childsIterator;
                if (saveChildFacets) {
                    childsIterator = child.getFacetsAndChildren();
                }
                else {
                    childsIterator = child.getChildren().iterator();
                }
                stateMap = saveFullDescendantComponentStates(facesContext, stateMap,
                        childsIterator, true);
                Object state = child.saveState(facesContext);
                if (state != null) {
                    if (stateMap == null) {
                        stateMap = new HashMap<>();
                    }
                    stateMap.put(child.getClientId(facesContext), state);
                }
            }
        }
        return stateMap;
    }

    private void restoreFullDescendantComponentDeltaStates(FacesContext facesContext,
                                                           Iterator<UIComponent> childIterator, Object state, Object initialState,
                                                           boolean restoreChildFacets) {
        Map<String, Object> descendantStateIterator = null;
        Iterator<? extends Object[]> descendantFullStateIterator = null;
        while (childIterator.hasNext()) {
            if (descendantStateIterator == null && state != null) {
                descendantStateIterator = (Map<String, Object>) state;
            }
            if (descendantFullStateIterator == null && initialState != null) {
                descendantFullStateIterator = ((Collection<? extends Object[]>) initialState).iterator();
            }
            UIComponent component = childIterator.next();

            // reset the client id (see spec 3.1.6)
            component.setId(component.getId());
            if (!component.isTransient()) {
                Object childInitialState = null;
                Object descendantInitialState = null;
                Object childState = null;
                if (descendantStateIterator != null
                        && descendantStateIterator.containsKey(component.getClientId(facesContext))) {
                    //Object[] object = (Object[]) descendantStateIterator.get(component.getClientId(facesContext));
                    //childState = object[0];
                    childState = descendantStateIterator.get(component.getClientId(facesContext));
                }
                if (descendantFullStateIterator != null
                        && descendantFullStateIterator.hasNext()) {
                    Object[] object = descendantFullStateIterator.next();
                    childInitialState = object[0];
                    descendantInitialState = object[1];
                }

                component.clearInitialState();
                if (childInitialState != null) {
                    component.restoreState(facesContext, childInitialState);
                    component.markInitialState();
                    component.restoreState(facesContext, childState);
                }
                else {
                    component.restoreState(facesContext, childState);
                    component.markInitialState();
                }

                Iterator<UIComponent> childsIterator;
                if (restoreChildFacets) {
                    childsIterator = component.getFacetsAndChildren();
                }
                else {
                    childsIterator = component.getChildren().iterator();
                }
                restoreFullDescendantComponentDeltaStates(facesContext, childsIterator,
                        state, descendantInitialState, true);
            }
        }
    }

    private void restoreTransientDescendantComponentStates(FacesContext facesContext, Iterator<UIComponent> childIterator, Map<String, Object> state,
                                                           boolean restoreChildFacets) {
        while (childIterator.hasNext()) {
            UIComponent component = childIterator.next();

            // reset the client id (see spec 3.1.6)
            component.setId(component.getId());
            if (!component.isTransient()) {
                component.restoreTransientState(facesContext, (state == null) ? null : state.get(component.getClientId(facesContext)));

                Iterator<UIComponent> childsIterator;
                if (restoreChildFacets) {
                    childsIterator = component.getFacetsAndChildren();
                }
                else {
                    childsIterator = component.getChildren().iterator();
                }
                restoreTransientDescendantComponentStates(facesContext, childsIterator, state, true);
            }
        }

    }

    private Map<String, Object> saveTransientDescendantComponentStates(FacesContext facesContext, Map<String,
                                    Object> childStates, Iterator<UIComponent> childIterator, boolean saveChildFacets) {
        while (childIterator.hasNext()) {
            UIComponent child = childIterator.next();
            if (!child.isTransient()) {
                Iterator<UIComponent> childsIterator;
                if (saveChildFacets) {
                    childsIterator = child.getFacetsAndChildren();
                }
                else {
                    childsIterator = child.getChildren().iterator();
                }
                childStates = saveTransientDescendantComponentStates(facesContext, childStates, childsIterator, true);
                Object state = child.saveTransientState(facesContext);
                if (state != null) {
                    if (childStates == null) {
                        childStates = new HashMap<>();
                    }
                    childStates.put(child.getClientId(facesContext), state);
                }
            }
        }
        return childStates;
    }

    @Override
    public void restoreState(FacesContext context, Object state) {
        if (state == null) {
            return;
        }

        Object[] values = (Object[]) state;
        super.restoreState(context, values[0]);
        Object restoredRowStates = UIComponentBase.restoreAttachedState(context, values[1]);
        if (restoredRowStates == null) {
            if (!_rowDeltaStates.isEmpty()) {
                _rowDeltaStates.clear();
            }
        }
        else {
            _rowDeltaStates = (Map<String, Object>) restoredRowStates;
        }
    }

    @Override
    public Object saveState(FacesContext context) {
        // See MyFaces UIData
        ComponentUtils.ViewPoolingResetMode viewPoolingResetMode = ComponentUtils.isViewPooling(context);
        if (viewPoolingResetMode == ComponentUtils.ViewPoolingResetMode.SOFT) {
            _rowTransientStates.clear();
            _initialDescendantFullComponentState = null;

            clientId = null;
            model = null;
            isNested = null;
            oldVar = null;
        }
        else if (viewPoolingResetMode == ComponentUtils.ViewPoolingResetMode.HARD) {
            _rowTransientStates.clear();
            _rowDeltaStates.clear();
            _initialDescendantFullComponentState = null;

            clientId = null;
            model = null;
            isNested = null;
            oldVar = null;
        }

        if (initialStateMarked()) {
            Object superState = super.saveState(context);

            if (superState == null && _rowDeltaStates.isEmpty()) {
                return null;
            }
            else {
                Object[] values = null;
                Object attachedState = UIComponentBase.saveAttachedState(context, _rowDeltaStates);
                if (superState != null || attachedState != null) {
                    values = new Object[]{superState, attachedState};
                }
                return values;
            }
        }
        else {
            Object[] values = new Object[2];
            values[0] = super.saveState(context);
            values[1] = UIComponentBase.saveAttachedState(context, _rowDeltaStates);
            return values;
        }
    }

    protected boolean isNestedWithinIterator() {
        if (isNested == null) {
            isNested = ComponentUtils.isNestedWithinIterator(this);
        }

        return isNested;
    }

    protected void preDecode(FacesContext context) {
        setDataModel(null);
        Map<String, SavedState> saved = (Map<String, SavedState>) getStateHelper().get(PropertyKeys.saved);
        if (null == saved || !keepSaved(context)) {
            getStateHelper().remove(PropertyKeys.saved);
        }
    }

    protected void preValidate(FacesContext context) {
        if (isNestedWithinIterator()) {
            setDataModel(null);
        }
    }

    protected void preUpdate(FacesContext context) {
        if (isNestedWithinIterator()) {
            setDataModel(null);
        }
    }

    protected void preEncode(FacesContext context) {
        setDataModel(null);
        if (!keepSaved(context)) {

            getStateHelper().remove(PropertyKeys.saved);
        }
    }

    private boolean keepSaved(FacesContext context) {
        return (contextHasErrorMessages(context) || isNestedWithinIterator());
    }

    private boolean contextHasErrorMessages(FacesContext context) {
        FacesMessage.Severity sev = context.getMaximumSeverity();
        return (sev != null && (FacesMessage.SEVERITY_ERROR.compareTo(sev) >= 0));
    }

    @Override
    public void encodeBegin(FacesContext context) throws IOException {

        preEncode(context);

        if (context == null) {
            throw new NullPointerException();
        }

        pushComponentToEL(context, null);

        if (!isRendered()) {
            return;
        }

        context.getApplication().publishEvent(context, PreRenderComponentEvent.class, this);

        String rendererType = getRendererType();
        if (rendererType != null) {
            Renderer renderer = getRenderer(context);
            if (renderer != null) {
                renderer.encodeBegin(context, this);
            }
        }
    }
}
