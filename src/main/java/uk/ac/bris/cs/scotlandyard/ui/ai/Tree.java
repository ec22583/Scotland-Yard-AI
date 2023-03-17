package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Optional;

//Our own implementation of a Tree data structure
//Children are ConcurrentLinkedQueues to prevent potential concurrency issues
public class Tree<T> {
    private T value;
    private ConcurrentLinkedQueue<Tree<T>> children;

    public Tree () {
        this.children = new ConcurrentLinkedQueue<>();
    }

    public Tree (T value) {
        this.value = value;
        this.children = new ConcurrentLinkedQueue<>();
    }

    public void addChildNode (Tree<T> child) {
        this.children.add(child);
    }

    public void addChildNodes (Collection<Tree<T>> children) {
        this.children.addAll(children);
    }

    public Tree<T> addChildValue (T value) {
        Tree<T> newTree = new Tree<>(value);
        this.children.add(newTree);
        return newTree;
    }

    public void addChildValues (List<T> values) {
        List<Tree<T>> newTrees =values
                .stream()
                .parallel()
                .map(v -> new Tree<T>(v))
                .toList();
        this.children.addAll(newTrees);
    }

    public List<Tree<T>> getChildNodes () {
        return ImmutableList.copyOf(this.children);
    }

    public List<T> getChildValues () {
        return this.children
                .stream()
                .parallel()
                .map(n -> n.getValue())
                .toList();
    }

    public Optional<Tree<T>> getChildNodeEqualling (T other) {
        for (Tree<T> child : this.children) {
            if (child.getValue().equals(other)) {
                return Optional.of(child);
            }
        }
        return Optional.empty();

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
