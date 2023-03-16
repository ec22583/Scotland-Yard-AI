package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import org.checkerframework.checker.nullness.Opt;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Optional;

public class Tree<T> {
    private T value;
    private List<Tree<T>> children;
    private ConcurrentLinkedQueue<Tree<T>> queue;

    public Tree () {
//        this.children = new LinkedList<>();
        this.queue = new ConcurrentLinkedQueue<>();
    }

    public Tree (T value) {
        this.value = value;
//        this.children = new LinkedList<>();
        this.queue = new ConcurrentLinkedQueue<>();
    }

    public void addChildNode (Tree<T> child) {
//        this.children.add(child);
        this.queue.add(child);
    }

    public void addChildNodes (Collection<Tree<T>> children) {
//        this.children.addAll(children);
        this.queue.addAll(children);
    }

    public Tree<T> addChildValue (T value) {
        Tree<T> newTree = new Tree<>(value);
//        this.children.add(newTree);
        this.queue.add(newTree);
        return newTree;
    }

    public void addChildValues (List<T> values) {
        List<Tree<T>> newTrees =values
                .stream()
                .parallel()
                .map(v -> new Tree<T>(v))
                .toList();

//        this.children.addAll(newTrees);
        this.queue.addAll(newTrees);
    }

    public List<Tree<T>> getChildNodes () {
//        return ImmutableList.copyOf(this.children);
        return ImmutableList.copyOf(this.queue);
    }

    public List<T> getChildValues () {
        return this.queue
                .stream()
                .parallel()
                .map(n -> n.getValue())
                .toList();
    }

    public Optional<Tree<T>> getChildNodeEqualling (T other) {
        for (Tree<T> child : this.queue) {
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
//        this.children.remove(child);
        this.queue.remove(child);
    }
}
