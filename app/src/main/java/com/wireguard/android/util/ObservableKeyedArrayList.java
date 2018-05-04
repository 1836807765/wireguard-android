/*
 * Copyright © 2018 Samuel Holland <samuel@sholland.org>
 * Copyright © 2018 Jason A. Donenfeld <Jason@zx2c4.com>. All Rights Reserved.
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package com.wireguard.android.util;

import android.databinding.ObservableArrayList;
import android.support.annotation.NonNull;

import java.util.Collection;
import java.util.ListIterator;
import java.util.Objects;

/**
 * ArrayList that allows looking up elements by some key property. As the key property must always
 * be retrievable, this list cannot hold {@code null} elements. Because this class places no
 * restrictions on the order or duplication of keys, lookup by key, as well as all list modification
 * operations, require O(n) time.
 */

public class ObservableKeyedArrayList<K, E extends Keyed<? extends K>>
        extends ObservableArrayList<E> implements ObservableKeyedList<K, E> {
    @Override
    public boolean add(final E e) {
        if (e == null)
            throw new NullPointerException();
        return super.add(e);
    }

    @Override
    public void add(final int index, final E e) {
        if (e == null)
            throw new NullPointerException();
        super.add(index, e);
    }

    @Override
    public boolean addAll(@NonNull final Collection<? extends E> c) {
        if (c.contains(null))
            throw new NullPointerException();
        return super.addAll(c);
    }

    @Override
    public boolean addAll(final int index, @NonNull final Collection<? extends E> c) {
        if (c.contains(null))
            throw new NullPointerException();
        return super.addAll(index, c);
    }

    @Override
    public boolean containsAllKeys(final Collection<K> keys) {
        for (final K key : keys)
            if (!containsKey(key))
                return false;
        return true;
    }

    @Override
    public boolean containsKey(final K key) {
        return indexOfKey(key) >= 0;
    }

    @Override
    public E get(final K key) {
        final int index = indexOfKey(key);
        return index >= 0 ? get(index) : null;
    }

    @Override
    public E getLast(final K key) {
        final int index = lastIndexOfKey(key);
        return index >= 0 ? get(index) : null;
    }

    @Override
    public int indexOfKey(final K key) {
        final ListIterator<E> iterator = listIterator();
        while (iterator.hasNext()) {
            final int index = iterator.nextIndex();
            if (Objects.equals(iterator.next().getKey(), key))
                return index;
        }
        return -1;
    }

    @Override
    public int lastIndexOfKey(final K key) {
        final ListIterator<E> iterator = listIterator(size());
        while (iterator.hasPrevious()) {
            final int index = iterator.previousIndex();
            if (Objects.equals(iterator.previous().getKey(), key))
                return index;
        }
        return -1;
    }

    @Override
    public E set(final int index, final E e) {
        if (e == null)
            throw new NullPointerException();
        return super.set(index, e);
    }
}
