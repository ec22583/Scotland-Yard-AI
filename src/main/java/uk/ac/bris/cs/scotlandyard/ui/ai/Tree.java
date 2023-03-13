package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

//Our own tree data structure
public class Tree<T> implements Iterable<T> {
    private Node<T> root;

    public Node<T> getRoot() {
        return root;
    }

    //Wrapper for value and its node's children
    class Node<T> {
        private T value;
        private List<Node<T>> children;

        public Node () {}

        public Node (T item) {
            this.value = item;
        }

        public T getValue() {
            return value;
        }

        public void setValue(T value) {
            this.value = value;
        }

        public boolean isLeaf () {
            return (children.isEmpty());
        }

        public ImmutableList<Node<T>> getAllChildren() {
            return ImmutableList.copyOf(children);
        }

        public void addChild (T child) {
            this.children.add(new Node<T>());
        }

        public void addChildren (Collection<T> children) {
            children
                    .forEach(c -> addChild(c)); //iterates through collection and adds child
        }

        public void pruneChild (Node<T> child) {
            if (this.children.contains(child)) {
                this.children.remove(child);
            } else {
                throw new IllegalArgumentException("Child does not exist");
            }
        }
    }

    //Implementation of the Iterator Pattern
    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            List<T> nodeValues;
            int index;
            
            private void addChildrenToList (Node<T> node) {
                //Adds the values into the List of node values
                nodeValues.add(node.getValue());

                for (Node<T> child : node.getAllChildren()) {
                    addChildrenToList(node);
                }
            }

            void Iterator () {
                nodeValues = new LinkedList<>();
                addChildrenToList(getRoot());
            }

            @Override
            public boolean hasNext() {
                return (index <= nodeValues.size());
            }

            @Override
            public T next() {
                index += 1;
                return nodeValues.get(index - 1);
            }
        };
    }

//  Default constructor
    Tree () {
        this.root = new Node<>();
    }

}
