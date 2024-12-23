package com.arextest.storage.service;


import java.util.AbstractList;
import java.util.Iterator;
import jakarta.validation.constraints.NotNull;

/**
 * avoid create a new array List,supported serialize is our goal.
 *
 * @param <E> Type of instance
 * @author jmo
 * @since 2021/11/11
 */
public final class IterableListWrapper<E> extends AbstractList<E> {

  private final static int SIZE_NOT_GIVEN = 0;
  private final Iterable<E> iterable;

  IterableListWrapper(@NotNull Iterable<E> iterable) {
    this.iterable = iterable;
  }

  @Override
  public E get(int index) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Iterator<E> iterator() {
    return iterable.iterator();
  }

  @Override
  public int size() {
    return SIZE_NOT_GIVEN;
  }
}