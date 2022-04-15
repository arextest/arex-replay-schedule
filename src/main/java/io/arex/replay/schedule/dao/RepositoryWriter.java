package io.arex.replay.schedule.dao;

import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

/**
 * Created by wang_yc on 2022/1/20
 */
public interface RepositoryWriter<T> {
    default boolean save(List<T> itemList) {
        if (CollectionUtils.isEmpty(itemList)) {
            return false;
        }
        for (T item : itemList) {
            this.save(item);
        }
        return true;
    }

    boolean save(T item);
}
