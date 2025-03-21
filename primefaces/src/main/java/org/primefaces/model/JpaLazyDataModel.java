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
package org.primefaces.model;

import org.primefaces.util.LangUtils;

import javax.faces.context.FacesContext;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.faces.FacesException;
import javax.faces.component.UIComponent;
import javax.faces.convert.Converter;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import org.primefaces.util.BeanUtils;
import org.primefaces.util.Lazy;
import org.primefaces.util.SerializableSupplier;

/**
 * Basic {@link LazyDataModel} implementation with JPA and Criteria API.
 *
 * @param <T> The model class.
 */
public class JpaLazyDataModel<T> extends LazyDataModel<T> implements Serializable {

    protected Class<T> entityClass;
    protected SerializableSupplier<EntityManager> entityManager;
    protected String rowKeyField;

    private transient Lazy<Method> rowKeyGetter;

    /**
     * For serialization only
     */
    public JpaLazyDataModel() {

    }

    /**
     * Constructs a JpaLazyDataModel for usage without enabled selection.
     *
     * @param entityClass The entity class
     * @param entityManager The {@link EntityManager}
     */
    public JpaLazyDataModel(Class<T> entityClass, SerializableSupplier<EntityManager> entityManager) {
        this.entityClass = entityClass;
        this.entityManager = entityManager;
    }

    /**
     * Constructs a JpaLazyDataModel with selection support.
     *
     * @param entityClass The entity class
     * @param entityManager The {@link EntityManager}
     * @param rowKeyField The name of the rowKey property (e.g. "id")
     */
    public JpaLazyDataModel(Class<T> entityClass, SerializableSupplier<EntityManager> entityManager, String rowKeyField) {
        this(entityClass, entityManager);
        this.rowKeyField = rowKeyField;
    }

    /**
     * Constructs a JpaLazyDataModel with selection support, with an already existing {@link Converter}.
     *
     * @param entityClass The entity class
     * @param entityManager The {@link EntityManager}
     * @param converter The converter, which will be used for converting the entity to a rowKey and vice versa
     */
    public JpaLazyDataModel(Class<T> entityClass, SerializableSupplier<EntityManager> entityManager, Converter converter) {
        super(converter);
        this.entityClass = entityClass;
        this.entityManager = entityManager;
    }

    @Override
    public int count(Map<String, FilterMeta> filterBy) {
        EntityManager em = this.entityManager.get();

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<T> root = cq.from(entityClass);
        cq = cq.select(cb.count(root));

        applyFilters(cb, cq, root, filterBy);

        return em.createQuery(cq).getSingleResult().intValue();
    }

    @Override
    public List<T> load(int first, int pageSize, Map<String, SortMeta> sortBy, Map<String, FilterMeta> filterBy) {
        EntityManager em = this.entityManager.get();

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<T> cq = cb.createQuery(entityClass);
        Root<T> root = cq.from(entityClass);
        cq = cq.select(root);

        applyFilters(cb, cq, root, filterBy);
        applySort(cb, cq, root, sortBy);

        TypedQuery<T> query = em.createQuery(cq);
        query.setFirstResult(first);
        query.setMaxResults(pageSize);

        List<T> result = query.getResultList();

        return result;
    }

    protected void applyFilters(CriteriaBuilder cb,
                                CriteriaQuery<?> cq,
                                Root<T> root,
                                Map<String, FilterMeta> filterBy) {

        List<Predicate> predicates = new ArrayList<>();

        applyGlobalFilters(cb, cq, root, predicates);

        if (filterBy != null) {
            for (FilterMeta filter : filterBy.values()) {
                if (filter.getField() == null || filter.getFilterValue() == null) {
                    continue;
                }

                String filterValue = filter.getFilterValue().toString();
                Field filterField = LangUtils.getFieldRecursive(entityClass, filter.getField());
                Object convertedFilterValue = convertToType(filterValue, filterField.getType());
                Expression fieldExpression = resolveFieldExpression(root, filter.getField());

                Predicate predicate = createPredicate(filter, filterField, root, cb, fieldExpression, (Comparable) convertedFilterValue);
                predicates.add(predicate);
            }
        }

        if (!predicates.isEmpty()) {
            cq.where(
                cb.and(predicates.toArray(new Predicate[predicates.size()])));
        }
    }

    protected void applyGlobalFilters(CriteriaBuilder cb, CriteriaQuery<?> cq, Root<T> root, List<Predicate> predicates) {

    }

    protected <F extends Comparable> Predicate createPredicate(FilterMeta filter, Field filterField,
            Root<T> root, CriteriaBuilder cb, Expression fieldExpression, F filterValue) {

        Lazy<Expression<String>> fieldExpressionAsString = new Lazy(() -> fieldExpression.as(String.class));

        switch (filter.getMatchMode()) {
            case STARTS_WITH:
                return cb.like(fieldExpressionAsString.get(), filterValue + "%");
            case NOT_STARTS_WITH:
                return cb.notLike(fieldExpressionAsString.get(), filterValue + "%");
            case ENDS_WITH:
                return cb.like(fieldExpressionAsString.get(), "%" + filterValue);
            case NOT_ENDS_WITH:
                return cb.notLike(fieldExpressionAsString.get(), "%" + filterValue);
            case CONTAINS:
                return cb.like(fieldExpressionAsString.get(), "%" + filterValue + "%");
            case NOT_CONTAINS:
                return cb.notLike(fieldExpressionAsString.get(), "%" + filterValue + "%");
            case EXACT:
            case EQUALS:
                return cb.equal(fieldExpression, filterValue);
            case NOT_EXACT:
            case NOT_EQUALS:
                return cb.notEqual(fieldExpression, filterValue);
            case LESS_THAN:
                return cb.lessThan(fieldExpression, filterValue);
            case LESS_THAN_EQUALS:
                return cb.lessThanOrEqualTo(fieldExpression, filterValue);
            case GREATER_THAN:
                return cb.greaterThan(fieldExpression, filterValue);
            case GREATER_THAN_EQUALS:
                return cb.greaterThanOrEqualTo(fieldExpression, filterValue);
            case IN:
                throw new UnsupportedOperationException("MatchMode.IN currently not supported!");
            case NOT_IN:
                throw new UnsupportedOperationException("MatchMode.NOT_IN currently not supported!");
            case RANGE:
                throw new UnsupportedOperationException("MatchMode.RANGE currently not supported!");
            case BETWEEN:
                throw new UnsupportedOperationException("MatchMode.BETWEEN currently not supported!");
            case NOT_BETWEEN:
                throw new UnsupportedOperationException("MatchMode.NOT_BETWEEN currently not supported!");
            case GLOBAL:
                throw new UnsupportedOperationException("MatchMode.GLOBAL currently not supported!");
        }

        return null;
    }

    protected void applySort(CriteriaBuilder cb,
                             CriteriaQuery<T> cq,
                             Root<T> root,
                             Map<String, SortMeta> sortBy) {

        if (sortBy != null) {
            for (SortMeta sort : sortBy.values()) {
                if (sort.getField() == null || sort.getOrder() == SortOrder.UNSORTED) {
                    continue;
                }

                Expression<?> fieldExpression = resolveFieldExpression(root, sort.getField());
                cq.orderBy(sort.getOrder() == SortOrder.ASCENDING ? cb.asc(fieldExpression) : cb.desc(fieldExpression));
            }
        }
    }

    protected Expression resolveFieldExpression(Root<T> root, String fieldName) {
        Join<?, ?> join = null;

        // join if required; e.g. company.name -> join to company and get "name" field from the joined table
        while (fieldName.contains(".")) {
            String currentName = fieldName.substring(0, fieldName.indexOf("."));
            fieldName = fieldName.substring(currentName.length() + 1);

            if (join == null) {
                join = root.join(currentName, JoinType.INNER);
            }
            else {
                join = join.join(currentName, JoinType.INNER);
            }
        }

        return join == null ? root.get(fieldName) : join.get(fieldName);
    }

    @Override
    public T getRowData(String rowKey) {
        Converter converter = getConverter();
        if (converter != null) {
            return super.getRowData(rowKey);
        }

        if (rowKeyField != null) {
            Object convertedRowKey = convertToType(rowKey, getRowKeyGetter().getReturnType());

            EntityManager em = this.entityManager.get();

            CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
            CriteriaQuery<T> cq = criteriaBuilder.createQuery(entityClass);
            Root<T> root = cq.from(entityClass);
            cq.select(root).where(criteriaBuilder.equal(root.get(rowKeyField), convertedRowKey));

            TypedQuery<T> query = em.createQuery(cq);
            return query.getSingleResult();
        }

        throw new UnsupportedOperationException(
                getMessage("Provide a Converter or rowKeyField via constructor or implement getRowData(String rowKey) in %s"
                        + ", when basic rowKey algorithm is not used [component=%s,view=%s]."));
    }

    @Override
    public String getRowKey(T object) {
        Converter converter = getConverter();
        if (converter != null) {
            return super.getRowKey(object);
        }

        if (rowKeyField != null) {
            try {
                Object rowKey = getRowKeyGetter().invoke(object);
                return rowKey == null ? null : rowKey.toString();
            }
            catch (InvocationTargetException | IllegalAccessException e) {
                throw new FacesException("Could not invoke getter for " + rowKeyField + " on " + entityClass.getName(), e);
            }
        }

        throw new UnsupportedOperationException(
                getMessage("Provide a Converter or rowKeyField via constructor or implement getRowKey(T object) in %s"
                        + ", when basic rowKey algorithm is not used [component=%s,view=%s]."));
    }

    protected <V> V convertToType(String value, Class<V> valueType) {
        FacesContext context = FacesContext.getCurrentInstance();
        Converter<V> converter = context.getApplication().createConverter(valueType);
        if (converter != null) {
            return converter.getAsObject(context, UIComponent.getCurrentComponent(context), value);
        }

        if (valueType == String.class) {
            return (V) value;
        }

        if (BeanUtils.isPrimitiveOrPrimitiveWrapper(valueType)) {
            return (V) FacesContext.getCurrentInstance().getELContext().convertToType(value, valueType);
        }

        throw new FacesException("Can not convert " + value + " to type " + valueType
            + "; please create a JSF Converter for it or overwrite Object convertToType(String value, Class<?> valueType)!");
    }

    protected Method getRowKeyGetter() {
        if (rowKeyGetter == null) {
            rowKeyGetter = new Lazy<>(() -> {
                try {
                    return new PropertyDescriptor(rowKeyField, entityClass).getReadMethod();
                }
                catch (IntrospectionException e) {
                    throw new FacesException("Could not access " + rowKeyField + " on " + entityClass.getName(), e);
                }
            });
        }
        return rowKeyGetter.get();
    }
}
