/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */
package com.microsoft.protection.controller.model;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.springframework.hateoas.Link;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.config.EnableHypermediaSupport.HypermediaType;

/**
 * {@link List} that extends {@link ResourceSupport} to ensure that
 * {@link Link}s in content are in {@link HypermediaType#HAL} format.
 * 
 * @param <T>
 *            of the response content
 */
public class ResponseList<T> extends ResourceSupport implements List<T> {

    private final List<T> content;

    /**
     * @param content
     *            to delegate
     */
    public ResponseList(final List<T> content) {
        this.content = content;
    }

    @Override
    public int size() {
        return content.size();
    }

    @Override
    public boolean isEmpty() {
        return content.isEmpty();
    }

    @Override
    public boolean contains(final Object o) {
        return content.contains(o);
    }

    @Override
    public Iterator<T> iterator() {
        return content.iterator();
    }

    @Override
    public Object[] toArray() {
        return content.toArray();
    }

    @Override
    public <T> T[] toArray(final T[] a) {
        return content.toArray(a);
    }

    @Override
    public boolean add(final T e) {
        return content.add(e);
    }

    @Override
    public boolean remove(final Object o) {
        return content.remove(o);
    }

    @Override
    public boolean containsAll(final Collection<?> c) {
        return content.containsAll(c);
    }

    @Override
    public boolean addAll(final Collection<? extends T> c) {
        return content.addAll(c);
    }

    @Override
    public boolean addAll(final int index, final Collection<? extends T> c) {
        return content.addAll(index, c);
    }

    @Override
    public boolean removeAll(final Collection<?> c) {
        return content.removeAll(c);
    }

    @Override
    public boolean retainAll(final Collection<?> c) {
        return content.retainAll(c);
    }

    @Override
    public void clear() {
        content.clear();
    }

    @Override
    public boolean equals(final Object o) {
        return content.equals(o);
    }

    @Override
    public int hashCode() {
        return content.hashCode();
    }

    @Override
    public T get(final int index) {
        return content.get(index);
    }

    @Override
    public T set(final int index, final T element) {
        return content.set(index, element);
    }

    @Override
    public void add(final int index, final T element) {
        content.add(index, element);
    }

    @Override
    public T remove(final int index) {
        return content.remove(index);
    }

    @Override
    public int indexOf(final Object o) {
        return content.indexOf(o);
    }

    @Override
    public int lastIndexOf(final Object o) {
        return content.lastIndexOf(o);
    }

    @Override
    public ListIterator<T> listIterator() {
        return content.listIterator();
    }

    @Override
    public ListIterator<T> listIterator(final int index) {
        return content.listIterator(index);
    }

    @Override
    public List<T> subList(final int fromIndex, final int toIndex) {
        return content.subList(fromIndex, toIndex);
    }

    @Override
    public String toString() {
        return "ResponseList [content=" + content + "]";
    }

}
