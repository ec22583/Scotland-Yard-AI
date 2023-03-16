package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;

import java.util.LinkedList;
import java.util.List;

public class Tree<T> {
    private T value;
    private List<Tree<T>> children;

    public Tree () {
        this.children = new LinkedList<>();
    }

    public Tree (T value) {
        this.value = value;
        this.children = new LinkedList<>();
    }

    public void addChildNode (Tree<T> child) {
        this.children.add(child);
    }

    public void removeChildNode (Tree<T> child) {
        this.children.remove(child);
    }

    public List<Tree<T>> getChildNodes () {
        return ImmutableList.copyOf(this.children);
    }

    public void addChildNodes (List<Tree<T>> children) {
        this.children.addAll(children);
    }

    public void addChildValue (T value) {
        this.children.add(new Tree<>(value));
    }

    public void addChildValues (List<T> values) {
        this.children.addAll(values
                .stream()
                .parallel()
                .map(v -> new Tree<T>(v))
                .toList());
    }

    public T getValue() {
        return this.value;
    }
}
