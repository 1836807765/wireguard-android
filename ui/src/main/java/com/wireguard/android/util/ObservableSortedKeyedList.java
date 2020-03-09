/*
 * Copyright © 2017-2019 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.wireguard.android.util;

import com.wireguard.util.Keyed;
import com.wireguard.util.NonNullForAll;
import com.wireguard.util.SortedKeyedList;

/**
 * A list that is both sorted/keyed and observable.
 */

@NonNullForAll
public interface ObservableSortedKeyedList<K, E extends Keyed<? extends K>>
        extends ObservableKeyedList<K, E>, SortedKeyedList<K, E> {
}
