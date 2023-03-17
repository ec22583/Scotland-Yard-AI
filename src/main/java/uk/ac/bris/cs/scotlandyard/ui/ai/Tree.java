package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import uk.ac.bris.cs.scotlandyard.model.Move;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

//Our own implementation of a Tree data structure
//Children are ConcurrentHashMap to prevent potential concurrency issues
public class Tree<T> {
    private T value;
//  Integer is hash code of T.
    private ConcurrentHashMap<Integer, Tree<T>> children;

    public Tree () {
        this.children = new ConcurrentHashMap<>();
    }

    public Tree (T value) {
        this.value = value;
        this.children = new ConcurrentHashMap<>();
    }

    public void addChildNode (Tree<T> child) {
        this.children.put(child.hashCode(), child);
    }

    public void addChildNodes (Collection<Tree<T>> children) {
        Map<Integer, Tree<T>> childMap = new HashMap();
        for (Tree<T> child : children) {
            childMap.put(child.getValue().hashCode(), child);
        }

        this.children.putAll(childMap);
    }

    public Tree<T> addChildValue (T value) {
        Tree<T> newTree = new Tree<>(value);
        this.children.put(value.hashCode(), newTree);
        return newTree;
    }

    public void addChildValues (List<T> values) {
        this.addChildNodes(values
                .stream()
                .map(v -> new Tree<>(v))
                .toList()
        );
    }

    public List<Tree<T>> getChildNodes () {
        return ImmutableList.copyOf(this.children.values());
    }

    public List<T> getChildValues () {
        return this.children.values().stream().map((v -> v.getValue())).toList();
    }

    public Optional<Tree<T>> getChildNodeEqualling (T other) {
        if (this.children.containsKey(other.hashCode())) {
            return Optional.of(this.children.get(other.hashCode()));
        } else{
            return Optional.empty();
        }
    }

    public T getValue() {
        return this.value;
    }

    public void setValue (T value) {
        this.value = value;
    }

    public void removeChildNode (Tree<T> child) {
        this.children.remove(child);
    }
}
